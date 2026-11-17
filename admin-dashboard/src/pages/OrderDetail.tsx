import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, img } from '../api/client';
import { OrderDetail, Partner } from '../api/types';
import { Badge, Busy, confirmAction, Field, Loadable, Modal, toast, useAsync } from '../components/ui';
import { dateTime, money, NEXT_STATUS, statusLabel, WORKFLOW } from '../utils/format';

export default function OrderDetailPage() {
  const { id } = useParams();
  const state = useAsync(() => api.get<OrderDetail>(`/api/v1/admin/orders/${id}`), [id]);
  const [busy, setBusy] = useState(false);
  const [assignOpen, setAssignOpen] = useState(false);
  const [refundOpen, setRefundOpen] = useState(false);

  async function move(next: string) {
    let note: string | null = '';
    if (next === 'CANCELLED') {
      note = window.prompt('Reason for cancellation (shown to the customer):');
      if (!note) return;
    }
    setBusy(true);
    try {
      state.setData(await api.patch<OrderDetail>(`/api/v1/admin/orders/${id}/status`, { status: next, note }));
      toast.ok(`Order moved to ${statusLabel(next)}`);
    } catch (e) { toast.err(e); } finally { setBusy(false); }
  }

  return (
    <Loadable state={state}>{(o) => {
      const step = WORKFLOW.indexOf(o.status);
      return (
        <>
          <div className="page-head">
            <h1>Order {o.orderNumber} <Badge value={o.status} /></h1>
            <div className="actions">
              <Link className="btn" to="/orders">← All orders</Link>
              {!['PENDING_PAYMENT', 'PAYMENT_FAILED'].includes(o.status) && <Link className="btn" to={`/orders/${o.id}/invoice`} target="_blank">Invoice</Link>}
            </div>
          </div>
          {o.testPayment && <div className="alert warn">TEST payment (mock gateway) — no money was collected for this order.</div>}
          {step >= 0 && (
            <ol className="stepper">
              {WORKFLOW.map((s, i) => <li key={s} className={i < step ? 'done' : i === step ? 'current' : ''}>{statusLabel(s)}</li>)}
            </ol>
          )}
          <div className="grid-2">
            <section className="card">
              <h3>Actions</h3>
              <div className="actions wrap">
                {(NEXT_STATUS[o.status] ?? []).map((n) => (
                  <Busy key={n} busy={busy} className={n === 'CANCELLED' ? 'btn danger' : 'btn primary'}
                    onClick={() => (n !== 'CANCELLED' || confirmAction('Cancel this order? Stock is restored and paid orders are refunded.')) && move(n)}>
                    {n === 'CANCELLED' ? 'Cancel order' : `Mark ${statusLabel(n)}`}
                  </Busy>
                ))}
                {(o.status === 'PACKED' || o.status === 'ASSIGNED') && (
                  <button className="btn primary" onClick={() => setAssignOpen(true)}>{o.status === 'ASSIGNED' ? 'Reassign partner' : 'Assign delivery partner'}</button>
                )}
                {(o.paymentStatus === 'PAID' || o.paymentStatus === 'PARTIALLY_REFUNDED') && (
                  <button className="btn" onClick={() => setRefundOpen(true)}>Issue refund</button>
                )}
                {!(NEXT_STATUS[o.status]?.length) && o.status !== 'PACKED' && o.paymentStatus !== 'PAID' && <span className="muted">No actions available for this status.</span>}
              </div>
              {o.status === 'ASSIGNED' && <p className="muted small">The delivery partner confirms pickup, out-for-delivery and delivery (with the customer's OTP) in the app.</p>}
              <h3>Customer & delivery</h3>
              <p><strong>{o.address.name}</strong> · <a href={`tel:${o.address.phone}`}>{o.address.phone}</a><br />
                {o.address.line1}{o.address.line2 ? `, ${o.address.line2}` : ''}{o.address.landmark ? ` (near ${o.address.landmark})` : ''}<br />
                {o.address.city}, {o.address.state} – {o.address.pincode}</p>
              {o.deliveryInstructions && <p className="note">📝 {o.deliveryInstructions}</p>}
              {o.customerId && <p><Link to={`/customers?id=${o.customerId}`}>View customer profile</Link></p>}
              {o.deliveryPartner && <p>🛵 <strong>{o.deliveryPartner.name}</strong> · {o.deliveryPartner.phone} · {o.deliveryPartner.vehicleType} {o.deliveryPartner.vehicleNumber}</p>}
              {o.cancelReason && <div className="alert err">Cancelled: {o.cancelReason}</div>}
            </section>
            <section className="card">
              <h3>Bill</h3>
              <table className="table compact"><tbody>
                <tr><td>Item total (MRP {money(o.mrpTotal)})</td><td className="num">{money(o.itemTotal)}</td></tr>
                {o.couponDiscount > 0 && <tr><td>Coupon {o.couponCode}</td><td className="num">− {money(o.couponDiscount)}</td></tr>}
                <tr><td>Delivery fee</td><td className="num">{money(o.deliveryFee)}</td></tr>
                <tr><td>Handling fee</td><td className="num">{money(o.handlingFee)}</td></tr>
                <tr className="total"><td>Grand total</td><td className="num">{money(o.grandTotal)}</td></tr>
                <tr><td className="muted">GST included</td><td className="num muted">{money(o.taxTotal)}</td></tr>
              </tbody></table>
              <p>Payment: <strong>{o.paymentMethod}</strong> {o.paymentProvider && `via ${o.paymentProvider}`} · <Badge value={o.paymentStatus} /></p>
              {o.refunds.length > 0 && (<>
                <h4>Refunds</h4>
                <table className="table compact"><tbody>
                  {o.refunds.map((r) => <tr key={r.id}><td>{money(r.amount)}</td><td>{r.reason}</td><td><Badge value={r.status} /></td></tr>)}
                </tbody></table>
              </>)}
            </section>
          </div>
          <section className="card flush">
            <table className="table">
              <thead><tr><th></th><th>Product</th><th>SKU</th><th>Price</th><th>Qty</th><th>GST</th><th>Total</th></tr></thead>
              <tbody>
                {o.items.map((i) => (
                  <tr key={i.variantId}>
                    <td>{i.imageUrl && <img className="thumb" src={img(i.imageUrl)} alt="" />}</td>
                    <td>{i.productName}<br /><small className="muted">{i.variantLabel}</small></td>
                    <td><code>{i.sku}</code></td>
                    <td className="num">{money(i.unitPrice)}{i.unitMrp > i.unitPrice && <><br /><s className="muted small">{money(i.unitMrp)}</s></>}</td>
                    <td>{i.quantity}</td>
                    <td>{i.gstRate}%</td>
                    <td className="num">{money(i.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
          <section className="card">
            <h3>Timeline</h3>
            <ul className="timeline">
              {o.history.map((h, i) => <li key={i}><Badge value={h.status} /> <small>{dateTime(h.at)}</small>{h.note && <div className="muted">{h.note}</div>}</li>)}
            </ul>
          </section>
          {assignOpen && <AssignModal orderId={o.id} onClose={() => setAssignOpen(false)} onDone={(d) => { state.setData(d); setAssignOpen(false); }} />}
          {refundOpen && <RefundModal order={o} onClose={() => setRefundOpen(false)} onDone={() => { setRefundOpen(false); state.reload(); }} />}
        </>
      );
    }}</Loadable>
  );
}

function AssignModal({ orderId, onClose, onDone }: { orderId: number; onClose: () => void; onDone: (o: OrderDetail) => void }) {
  const partners = useAsync(() => api.get<Partner[]>('/api/v1/admin/delivery-partners'), []);
  const [pid, setPid] = useState<number | ''>('');
  const [busy, setBusy] = useState(false);
  async function assign() {
    if (!pid) return;
    setBusy(true);
    try { onDone(await api.post<OrderDetail>(`/api/v1/admin/orders/${orderId}/assign`, { partnerId: pid })); toast.ok('Delivery partner assigned'); }
    catch (e) { toast.err(e); } finally { setBusy(false); }
  }
  return (
    <Modal title="Assign delivery partner" onClose={onClose} footer={<Busy busy={busy} disabled={!pid} onClick={assign}>Assign</Busy>}>
      <Loadable state={partners} empty={(d) => (d.filter((p) => p.active).length === 0 ? <p>No active delivery partners. <Link to="/delivery-partners">Add one</Link>.</p> : null)}>
        {(list) => (
          <div className="radio-list">
            {list.filter((p) => p.active).map((p) => (
              <label key={p.id} className={`radio-row ${pid === p.id ? 'sel' : ''}`}>
                <input type="radio" name="p" checked={pid === p.id} onChange={() => setPid(p.id)} />
                <span><strong>{p.fullName}</strong> · {p.phone}<br /><small className="muted">{p.vehicleType} {p.vehicleNumber} · {p.activeOrders} active · {p.available ? '🟢 available' : '⚪ offline'}</small></span>
              </label>
            ))}
          </div>
        )}
      </Loadable>
    </Modal>
  );
}

function RefundModal({ order, onClose, onDone }: { order: OrderDetail; onClose: () => void; onDone: () => void }) {
  const already = order.refunds.filter((r) => ['REQUESTED', 'APPROVED', 'PROCESSED'].includes(r.status)).reduce((s, r) => s + Number(r.amount), 0);
  const max = Math.max(0, Number(order.grandTotal) - already);
  const [amount, setAmount] = useState(max.toFixed(2));
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit() {
    setBusy(true);
    try {
      await api.post(`/api/v1/admin/orders/${order.id}/refunds`, { amount: Number(amount), reason });
      toast.ok('Refund requested. Approve it in Refunds to send it to the payment gateway.');
      onDone();
    } catch (e) { toast.err(e); } finally { setBusy(false); }
  }
  return (
    <Modal title={`Refund for ${order.orderNumber}`} onClose={onClose}
      footer={<Busy busy={busy} disabled={!reason.trim() || Number(amount) <= 0 || Number(amount) > max} onClick={submit}>Create refund request</Busy>}>
      <Field label={`Amount (max ${money(max)})`}><input type="number" step="0.01" min="0.01" max={max} value={amount} onChange={(e) => setAmount(e.target.value)} /></Field>
      <Field label="Reason"><input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="e.g. Damaged item – 1 × Amul Butter" /></Field>
    </Modal>
  );
}
