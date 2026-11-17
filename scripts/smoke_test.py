#!/usr/bin/env python3
"""
End-to-end API smoke test for Gola Grocery (standard library only).

Exercises the complete flow against a RUNNING backend:
  catalogue -> register + OTP -> address -> cart -> coupon -> COD checkout ->
  admin confirm/prepare/pack -> create delivery partner -> assign -> pickup ->
  out for delivery -> deliver with OTP + cash -> review -> reorder ->
  online (mock) payment -> cancel -> automatic refund.

Requirements on the backend:  OTP_DEV_ECHO=true  and  PAYMENTS_MODE=mock
Usage:
  python scripts/smoke_test.py --base http://localhost:8080 --admin-phone 9999999999 --admin-password '<ADMIN_PASSWORD>'
"""
import argparse, json, random, sys, urllib.error, urllib.request

PASS, FAIL = 0, 0

def call(base, method, path, body=None, token=None, expect=(200, 201, 204)):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(base.rstrip("/") + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode() or "null"
            status = r.status
    except urllib.error.HTTPError as e:
        raw, status = e.read().decode() or "null", e.code
    payload = json.loads(raw) if raw.strip().startswith(("{", "[")) or raw == "null" else raw
    if status not in expect:
        raise AssertionError(f"{method} {path} -> {status}: {raw[:400]}")
    return payload

def step(name, fn):
    global PASS, FAIL
    try:
        out = fn()
        PASS += 1
        print(f"  PASS  {name}")
        return out
    except Exception as e:  # noqa: BLE001 - report and continue
        FAIL += 1
        print(f"  FAIL  {name}: {e}")
        return None

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8080")
    ap.add_argument("--admin-phone", required=True)
    ap.add_argument("--admin-password", required=True)
    ap.add_argument("--pincode", default="201301")
    a = ap.parse_args()
    B = a.base
    rnd = random.randint(10_000_000, 99_999_999)
    phone = f"9{rnd}0"[:10]
    partner_phone = f"8{rnd}1"[:10]
    print(f"Gola Grocery smoke test against {B}\n")

    step("health", lambda: call(B, "GET", "/actuator/health"))
    home = step("catalog home", lambda: call(B, "GET", "/api/v1/catalog/home"))
    step("categories (>=16 top-level)", lambda: len(call(B, "GET", "/api/v1/catalog/categories")) >= 16 or (_ for _ in ()).throw(AssertionError("too few")))
    found = step("search 'dal'", lambda: call(B, "GET", "/api/v1/catalog/products?q=dal&inStock=true"))
    step("pincode serviceable", lambda: call(B, "GET", f"/api/v1/store/pincodes/{a.pincode}")["serviceable"] or (_ for _ in ()).throw(AssertionError("not serviceable")))

    reg = step("register customer", lambda: call(B, "POST", "/api/v1/auth/register",
              {"fullName": "Smoke Test", "phone": phone, "email": None, "password": "SmokeTest#123"}))
    if not reg or not reg.get("devOtp"):
        print("\nOTP not echoed - start the backend with OTP_DEV_ECHO=true for this test."); sys.exit(1)
    tok = step("verify OTP", lambda: call(B, "POST", "/api/v1/auth/otp/verify", {"phone": phone, "code": reg["devOtp"], "purpose": "REGISTER"}))
    T = tok["accessToken"]
    step("wrong password rejected", lambda: call(B, "POST", "/api/v1/auth/login", {"identifier": phone, "password": "nope-nope"}, expect=(401,)))
    step("customer blocked from admin API", lambda: call(B, "GET", "/api/v1/admin/dashboard", token=T, expect=(403,)))
    addr = step("add address", lambda: call(B, "POST", "/api/v1/me/addresses", {
        "label": "HOME", "contactName": "Smoke Test", "contactPhone": phone, "line1": "B-12, Tower 3", "line2": "Sector 18",
        "landmark": "Near metro", "city": "Noida", "state": "Uttar Pradesh", "pincode": a.pincode, "latitude": 28.57, "longitude": 77.32, "isDefault": True}, token=T))

    items = [p for p in (found or {}).get("items", []) if p["variants"] and p["variants"][0]["inStock"]]
    if (home or {}).get("featured"):
        items += [p for p in home["featured"] if p["variants"] and p["variants"][0]["inStock"]]
    v1, v2 = items[0]["variants"][0], items[1]["variants"][0]
    step("add to cart", lambda: call(B, "PUT", f"/api/v1/cart/items?pincode={a.pincode}", {"variantId": v1["id"], "quantity": 2}, token=T))
    q = step("add 2nd item", lambda: call(B, "PUT", f"/api/v1/cart/items?pincode={a.pincode}", {"variantId": v2["id"], "quantity": 3}, token=T))
    step("server-side totals", lambda: abs(q["grandTotal"] - (q["itemTotal"] - q["couponDiscount"] + q["deliveryFee"] + q["handlingFee"])) < 0.01 or (_ for _ in ()).throw(AssertionError(q)))
    step("invalid coupon rejected", lambda: call(B, "POST", "/api/v1/cart/coupon", {"code": "NOPE123"}, token=T, expect=(400,)))
    if q and q["itemTotal"] >= 299:
        step("apply GOLA75 (first order)", lambda: call(B, "POST", "/api/v1/cart/coupon", {"code": "GOLA75"}, token=T))
    order = step("checkout COD", lambda: call(B, "POST", "/api/v1/orders/checkout", {"addressId": addr["id"], "paymentMethod": "COD", "deliveryInstructions": "Ring the bell"}, token=T))
    o = order["order"]
    step("order is PLACED with OTP", lambda: (o["status"] == "PLACED" and o["deliveryOtp"]) or (_ for _ in ()).throw(AssertionError(o["status"])))
    step("cart emptied", lambda: len(call(B, "GET", "/api/v1/cart", token=T)["lines"]) == 0 or (_ for _ in ()).throw(AssertionError("cart not empty")))

    adm = step("admin login", lambda: call(B, "POST", "/api/v1/auth/login", {"identifier": a.admin_phone, "password": a.admin_password}))
    A = adm["accessToken"]
    step("dashboard", lambda: call(B, "GET", "/api/v1/admin/dashboard", token=A))
    step("cannot skip to DELIVERED", lambda: call(B, "PATCH", f"/api/v1/admin/orders/{o['id']}/status", {"status": "DELIVERED"}, token=A, expect=(400,)))
    for s in ("CONFIRMED", "PREPARING", "PACKED"):
        step(f"admin -> {s}", lambda s=s: call(B, "PATCH", f"/api/v1/admin/orders/{o['id']}/status", {"status": s}, token=A))
    partner = step("create delivery partner", lambda: call(B, "POST", "/api/v1/admin/delivery-partners", {
        "fullName": "Ravi Rider", "phone": partner_phone, "email": None, "password": "Rider#12345", "vehicleType": "BIKE",
        "vehicleNumber": "UP16AB1234", "licenseNumber": "DL123", "active": True}, token=A))
    step("assign partner", lambda: call(B, "POST", f"/api/v1/admin/orders/{o['id']}/assign", {"partnerId": partner["id"]}, token=A))
    P = step("partner login", lambda: call(B, "POST", "/api/v1/auth/login", {"identifier": partner_phone, "password": "Rider#12345"}))["accessToken"]
    asg = step("partner sees assignment", lambda: [x for x in call(B, "GET", "/api/v1/delivery/assignments", token=P) if x["orderId"] == o["id"]][0])
    step("pickup", lambda: call(B, "POST", f"/api/v1/delivery/assignments/{asg['assignmentId']}/pickup", token=P))
    step("out for delivery", lambda: call(B, "POST", f"/api/v1/delivery/assignments/{asg['assignmentId']}/out-for-delivery", token=P))
    step("wrong OTP rejected", lambda: call(B, "POST", f"/api/v1/delivery/assignments/{asg['assignmentId']}/deliver", {"otp": "0000" if o["deliveryOtp"] != "0000" else "1111", "codCollected": asg["amountToCollect"]}, token=P, expect=(400,)))
    step("deliver with OTP + cash", lambda: call(B, "POST", f"/api/v1/delivery/assignments/{asg['assignmentId']}/deliver", {"otp": o["deliveryOtp"], "codCollected": asg["amountToCollect"]}, token=P))
    done = step("customer sees DELIVERED + PAID", lambda: call(B, "GET", f"/api/v1/orders/{o['id']}", token=T))
    step("status check", lambda: (done["status"] == "DELIVERED" and done["paymentStatus"] == "PAID") or (_ for _ in ()).throw(AssertionError(done["status"])))
    step("invoice", lambda: call(B, "GET", f"/api/v1/orders/{o['id']}/invoice", token=T))
    step("review product", lambda: call(B, "POST", "/api/v1/reviews", {"productId": o["items"][0]["productId"], "rating": 5, "title": "Fresh!", "comment": "Great quality"}, token=T))
    step("reorder", lambda: call(B, "POST", f"/api/v1/orders/{o['id']}/reorder", token=T))

    o2 = step("checkout UPI (mock gateway)", lambda: call(B, "POST", "/api/v1/orders/checkout", {"addressId": addr["id"], "paymentMethod": "UPI"}, token=T))
    if o2:
        step("payment pending until confirmed", lambda: o2["order"]["status"] == "PENDING_PAYMENT" or (_ for _ in ()).throw(AssertionError(o2["order"]["status"])))
        paid = step("mock payment success", lambda: call(B, "POST", "/api/v1/payments/mock/complete", {"orderId": o2["order"]["id"], "success": True}, token=T))
        step("order PLACED + PAID", lambda: (paid["status"] == "PLACED" and paid["paymentStatus"] == "PAID") or (_ for _ in ()).throw(AssertionError(paid)))
        c = step("customer cancels", lambda: call(B, "POST", f"/api/v1/orders/{o2['order']['id']}/cancel", {"reason": "Ordered by mistake"}, token=T))
        step("auto refund processed", lambda: (c["status"] == "CANCELLED" and c["refunds"] and c["refunds"][0]["status"] == "PROCESSED") or (_ for _ in ()).throw(AssertionError(c.get("refunds"))))
    step("sales report", lambda: call(B, "GET", "/api/v1/admin/reports/sales", token=A))
    step("notifications delivered", lambda: call(B, "GET", "/api/v1/notifications", token=T)["totalItems"] > 0 or (_ for _ in ()).throw(AssertionError("none")))
    step("audit log", lambda: call(B, "GET", "/api/v1/admin/audit-logs", token=A))

    print(f"\n{PASS} passed, {FAIL} failed")
    sys.exit(1 if FAIL else 0)

if __name__ == "__main__":
    main()
