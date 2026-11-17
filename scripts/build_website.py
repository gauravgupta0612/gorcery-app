#!/usr/bin/env python3
"""Builds the public Gola Grocery website (website/) from the seed SQL.
Run:  python scripts/build_website.py
Edit the BUSINESS block below to change contact details shown on every page."""
import html, os, re
from collections import OrderedDict

BUSINESS = {
    "name": "Gola Grocery",
    "tagline": "Freshness Delivered to Your Doorstep.",
    "owner": "Gaurav Gupta",
    "email": "ggupta865@gmail.com",
    "phone": "+91 88699 99358",
    "address": "Gola Gokarannath, Lakhimpur Kheri, Uttar Pradesh 262802, India",
    "hours": "6:00 AM - 11:30 PM, all days",
    "api": "https://gola-grocery-api.onrender.com",
    "apk": "https://github.com/gauravgupta0612/gorcery-app/releases/download/apk-latest/GolaGrocery-debug.apk",
    "min_order": 99, "delivery_fee": 25, "free_above": 299, "handling_fee": 4, "cod_max": 3000,
    "updated": "17 November 2026",
}
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MIG = os.path.join(ROOT, "database", "migrations")
OUT = os.path.join(ROOT, "website")

def read(n): return open(os.path.join(MIG, n), encoding="utf-8").read()
def unq(s): return s.replace("''", "'")
v2 = read("V2__seed_catalog.sql")
cats = {}
for m in re.finditer(r"INSERT INTO categories\(parent_id, name, name_hi, slug(?:, image_url)?, sort_order\) (?:VALUES \(NULL|SELECT id), '((?:[^']|'')*)', '((?:[^']|'')*)', '([^']*)'", v2):
    cats[m.group(3)] = unq(m.group(1))
products = OrderedDict()
for m in re.finditer(r"INSERT INTO products\([^)]*\) SELECT '((?:[^']|'')*)', '([^']*)', '((?:[^']|'')*)', \(SELECT id FROM brands WHERE name = '((?:[^']|'')*)'\), c\.id, \d+, \d+, \w+ FROM categories c WHERE c\.slug = '([^']*)'", v2):
    name, slug, desc, brand, cslug = m.groups()
    products[slug] = dict(name=unq(name), desc=unq(desc), brand=unq(brand), top=cats.get(cslug.split("--")[0], "Other"),
                          img=f"{BUSINESS['api']}/files/catalog/{slug}-1.png", variant=None)
for m in re.finditer(r"INSERT INTO product_variants\([^)]*\) SELECT id, '[^']*', '[^']*', '((?:[^']|'')*)', [\d.]+, '\w+', ([\d.]+), ([\d.]+), (TRUE|FALSE) FROM products WHERE slug = '([^']*)'", v2):
    label, mrp, price, default, slug = m.groups()
    p = products.get(slug)
    if p and (p["variant"] is None or default == "TRUE"):
        p["variant"] = (unq(label), float(mrp), float(price))
v5 = os.path.join(MIG, "V5__real_product_images.sql")
if os.path.exists(v5):
    for m in re.finditer(r"SELECT id, '([^']+)', 0 FROM products WHERE slug = '([^']*)'", open(v5, encoding="utf-8").read()):
        if m.group(2) in products: products[m.group(2)]["img"] = m.group(1)
pins = []
for f in sorted(os.listdir(MIG)):
    for m in re.finditer(r"\('(\d{6})', '((?:[^']|'')*)', '((?:[^']|'')*)'", read(f)):
        pins.append((m.group(1), unq(m.group(2)), unq(m.group(3))))
seen = set(); pins = [p for p in pins if not (p[0] in seen or seen.add(p[0]))]

E = html.escape
B = BUSINESS
NAV = [("index.html", "Shop"), ("about.html", "About"), ("shipping.html", "Delivery"), ("refund.html", "Refunds"), ("contact.html", "Contact")]
CSS = """*{box-sizing:border-box}body{margin:0;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;color:#1f2937;background:#f7f8f5;line-height:1.55}
a{color:#0c831f}header{background:#0c831f;color:#fff}header .w{display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap;padding:14px 20px}
header a{color:#fff;text-decoration:none;font-weight:600;margin-left:16px}header .logo{font-size:22px;font-weight:800;margin:0}.w{max-width:1120px;margin:0 auto;padding:0 20px}
.hero{background:linear-gradient(135deg,#0c831f,#3fae4f);color:#fff;padding:48px 0}.hero h1{font-size:36px;margin:0 0 8px}.hero p{font-size:18px;margin:0 0 20px;opacity:.95}
.btn{display:inline-block;background:#f8cb46;color:#1f2937;padding:12px 20px;border-radius:10px;font-weight:700;text-decoration:none}
.pills{display:flex;flex-wrap:wrap;gap:8px;margin:24px 0}.pills a{background:#fff;border:1px solid #dfe5da;border-radius:999px;padding:6px 14px;text-decoration:none;color:#1f2937;font-size:14px}
h2{margin:32px 0 12px}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:14px}
.card{background:#fff;border:1px solid #e5e9e1;border-radius:12px;padding:12px;display:flex;flex-direction:column}.card img{width:100%;height:130px;object-fit:contain;background:#fafafa;border-radius:8px}
.card .n{font-weight:600;font-size:14px;margin:8px 0 2px}.card .l{color:#6b7280;font-size:13px}.card .p{margin-top:auto;padding-top:6px;font-weight:700}.card s{color:#9ca3af;font-weight:400;font-size:13px;margin-left:6px}
.doc{background:#fff;border:1px solid #e5e9e1;border-radius:12px;padding:28px;margin:28px 0}.doc h1{margin-top:0}table{border-collapse:collapse;width:100%}td,th{border:1px solid #e5e9e1;padding:6px 10px;text-align:left;font-size:14px}
footer{background:#14321b;color:#cfe3d2;margin-top:40px;padding:28px 0;font-size:14px}footer a{color:#fff;margin-right:14px}"""

def page(fname, title, body, desc=""):
    nav = "".join(f'<a href="{h}">{t}</a>' for h, t in NAV)
    foot = " ".join(f'<a href="{h}">{t}</a>' for h, t in [("about.html", "About us"), ("contact.html", "Contact us"), ("privacy.html", "Privacy Policy"), ("terms.html", "Terms & Conditions"), ("refund.html", "Cancellation & Refund Policy"), ("shipping.html", "Shipping & Delivery Policy")])
    doc = f"""<!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{E(title)} | {E(B['name'])}</title><meta name="description" content="{E(desc or B['tagline'])}"><link rel="stylesheet" href="style.css"></head><body>
<header><div class="w"><p class="logo">🛒 {E(B['name'])}</p><nav>{nav}</nav></div></header>
{body}
<footer><div class="w"><p>{foot}</p><p>{E(B['name'])} · {E(B['address'])} · {E(B['phone'])} · <a href="mailto:{B['email']}">{E(B['email'])}</a></p>
<p>Payments are processed securely by Razorpay. All prices are in Indian Rupees (INR) and include applicable taxes. © 2026 {E(B['name'])}.</p></div></footer></body></html>"""
    open(os.path.join(OUT, fname), "w", encoding="utf-8").write(doc)

def doc(fname, title, inner): page(fname, title, f'<main class="w"><article class="doc"><h1>{E(title)}</h1><p><em>Last updated: {B["updated"]}</em></p>{inner}</article></main>')

os.makedirs(OUT, exist_ok=True)
open(os.path.join(OUT, "style.css"), "w").write(CSS)
groups = OrderedDict()
for slug, p in products.items():
    if p["variant"]: groups.setdefault(p["top"], []).append((slug, p))
def gid(t): return re.sub(r"[^a-z0-9]+", "-", t.lower()).strip("-")
pills = "".join(f'<a href="#{gid(t)}">{E(t)}</a>' for t in groups)
secs = ""
for t, items in groups.items():
    cards = ""
    for slug, p in items:
        label, mrp, price = p["variant"]
        strike = f"<s>₹{mrp:,.0f}</s>" if mrp > price else ""
        cards += f'<div class="card"><img loading="lazy" src="{E(p["img"])}" alt="{E(p["name"])}"><div class="n">{E(p["name"])}</div><div class="l">{E(label)}</div><div class="p">₹{price:,.0f}{strike}</div></div>'
    secs += f'<h2 id="{gid(t)}">{E(t)}</h2><div class="grid">{cards}</div>'
page("index.html", "Online grocery delivery", f"""<section class="hero"><div class="w"><h1>{E(B['name'])}</h1><p>{E(B['tagline'])} Fruits, vegetables, dairy, staples, snacks, household and personal care - delivered in about 30 minutes.</p>
<a class="btn" href="{B['apk']}">Download the Android app</a></div></section>
<main class="w"><p class="pills">{pills}</p><p>Order on our Android app. Pay by UPI, debit/credit card, net banking (via Razorpay) or Cash on Delivery. Minimum order ₹{B['min_order']}; delivery ₹{B['delivery_fee']} (free above ₹{B['free_above']}).</p>{secs}
<p style="color:#6b7280;font-size:13px">Product images: Open Food Facts / Open Beauty Facts contributors (CC BY-SA) and Wikimedia Commons. Brand names belong to their owners. Prices may change; the price shown in the app at checkout applies.</p></main>""")
doc("about.html", "About us", f"""<p><b>{E(B['name'])}</b> is an online grocery store run by {E(B['owner'])} from {E(B['address'])}. We sell everyday groceries - fresh fruits and vegetables, dairy and bakery, atta, rice and dals, oils and spices, snacks and beverages, personal care, household cleaning, baby care, pet supplies and stationery - and deliver them to your doorstep.</p>
<p>Customers place orders through the {E(B['name'])} Android app, track them live and pay online or on delivery. Our goal: <i>{E(B['tagline'])}</i></p>""")
doc("contact.html", "Contact us", f"""<table><tr><th>Business name</th><td>{E(B['name'])} (proprietor: {E(B['owner'])})</td></tr><tr><th>Address</th><td>{E(B['address'])}</td></tr>
<tr><th>Phone / WhatsApp</th><td><a href="tel:{B['phone'].replace(' ','')}">{E(B['phone'])}</a></td></tr><tr><th>Email</th><td><a href="mailto:{B['email']}">{E(B['email'])}</a></td></tr><tr><th>Support hours</th><td>{E(B['hours'])}</td></tr></table>
<p>For order issues please include your order number (e.g. GG100123). We reply within 24 hours.</p>""")
doc("privacy.html", "Privacy Policy", f"""<p>This policy explains how {E(B['name'])} ("we") collects and uses your information when you use our app and website.</p>
<h3>Information we collect</h3><ul><li>Name, mobile number, email address and password (stored only as a secure hash) when you register.</li><li>Delivery addresses and, if you allow it, your location to place the delivery pin.</li><li>Order history, cart, wishlist, reviews and support messages.</li><li>Payment status and transaction IDs. <b>We never see or store your card, UPI PIN or bank details</b> - payments are handled by Razorpay under its own privacy policy and PCI-DSS standards.</li><li>Device token for order notifications.</li></ul>
<h3>How we use it</h3><ul><li>To create your account, verify it with a one-time password (OTP) sent by email, process and deliver orders and process refunds.</li><li>To send order updates and, only if you agree, offers.</li><li>To prevent fraud and meet legal and tax obligations.</li></ul>
<h3>Sharing</h3><p>We share only what is needed: your name, phone and address with our delivery partner for your order; payment details with Razorpay; and data with authorities when required by law. We do not sell your data.</p>
<h3>Storage and security</h3><p>Data is stored on secured cloud servers with encrypted connections (HTTPS). Access is limited to authorised staff.</p>
<h3>Your choices</h3><p>You can view and edit your profile and addresses in the app. To delete your account and data, email <a href="mailto:{B['email']}">{E(B['email'])}</a>; we will do so within 30 days except records we must keep by law (e.g. invoices).</p>
<h3>Contact</h3><p>Questions: <a href="mailto:{B['email']}">{E(B['email'])}</a>, {E(B['phone'])}.</p>""")
doc("terms.html", "Terms & Conditions", f"""<p>By using the {E(B['name'])} app or website you agree to these terms. The service is operated by {E(B['owner'])}, {E(B['address'])}.</p>
<h3>Accounts</h3><p>You must give correct details and keep your password safe. You are responsible for orders placed from your account.</p>
<h3>Orders and prices</h3><ul><li>All prices are in INR and include GST where applicable. Prices and stock may change; the price shown at checkout applies.</li><li>Minimum order value is ₹{B['min_order']}. A delivery fee of ₹{B['delivery_fee']} applies to orders below ₹{B['free_above']}, and a handling fee of ₹{B['handling_fee']} may apply.</li><li>We may cancel an order if an item is out of stock, the address is outside our delivery area, or the order looks fraudulent; any amount paid is refunded in full.</li></ul>
<h3>Payment</h3><p>We accept UPI, debit/credit cards and net banking through Razorpay, and Cash on Delivery (up to ₹{B['cod_max']:,}). Online orders are confirmed only after successful payment.</p>
<h3>Delivery, cancellation and refunds</h3><p>See our <a href="shipping.html">Shipping & Delivery Policy</a> and <a href="refund.html">Cancellation & Refund Policy</a>.</p>
<h3>Liability</h3><p>Our liability for any order is limited to the amount paid for that order. Product information comes from manufacturers; please read the label before use.</p>
<h3>Governing law</h3><p>These terms are governed by the laws of India; courts at Lakhimpur Kheri, Uttar Pradesh have jurisdiction.</p>
<h3>Contact</h3><p><a href="mailto:{B['email']}">{E(B['email'])}</a> · {E(B['phone'])}</p>""")
doc("refund.html", "Cancellation & Refund Policy", f"""<h3>Cancellation</h3><ul><li>You can cancel an order free of charge in the app until it is packed (status <i>Placed</i> or <i>Confirmed</i>).</li><li>After the order is packed or out for delivery it cannot be cancelled; you may refuse perishable items at the door if they are damaged.</li><li>We may cancel orders for stock or delivery reasons; you will be informed and refunded in full.</li></ul>
<h3>Returns and replacements</h3><ul><li>Damaged, expired, wrong or missing items: report within <b>24 hours</b> of delivery from the order screen or by email/phone with a photo. We will replace the item or refund it.</li><li>Fresh fruits, vegetables, dairy and bakery items can be returned only at the time of delivery.</li><li>Opened personal care, baby care and food items cannot be returned unless defective.</li></ul>
<h3>Refunds</h3><ul><li>Approved refunds for online payments go back to the original payment method (UPI, card or bank) through Razorpay within <b>5-7 working days</b>.</li><li>For Cash on Delivery orders, refunds are made by UPI/bank transfer within 5-7 working days after you share your details.</li><li>If money was debited but the order failed, it is refunded automatically within 5-7 working days.</li></ul>
<p>Contact: <a href="mailto:{B['email']}">{E(B['email'])}</a> · {E(B['phone'])}</p>""")
rows = "".join(f"<tr><td>{a}</td><td>{E(b)}</td><td>{E(c)}</td></tr>" for a, b, c in pins)
doc("shipping.html", "Shipping & Delivery Policy", f"""<ul><li>We deliver groceries only within our service areas listed below; the app checks your pincode before you order.</li><li>Typical delivery time is <b>20-60 minutes</b> after the order is confirmed, between {E(B['hours'])}. Orders placed outside these hours are delivered the next morning.</li>
<li>Delivery fee: ₹{B['delivery_fee']} for orders below ₹{B['free_above']}; free above ₹{B['free_above']}. Minimum order ₹{B['min_order']}.</li><li>A delivery OTP is shown in the app; please share it with the delivery partner to receive your order.</li><li>We do not ship outside India or by courier.</li></ul>
<h3>Service areas</h3><table><tr><th>Pincode</th><th>City</th><th>Area</th></tr>{rows}</table>""")
print(f"Built {len(products)} products, {len(pins)} pincodes -> {OUT}")
