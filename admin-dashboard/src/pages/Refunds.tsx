import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, auth } from '../api/client';
import { Page, RefundRow } from '../api/types';
import { Badge, confirmAction, Empty, Loadable, Pager, toast, useAsync } from '../components/ui';
import { dateTime, money } from '../utils/format';

export default function Refunds() {
  const [status, setStatus] = useState('REQUESTED');
  const [page, setPage] = useState(0);
  const state = useAsync(() => api.get<Page<RefundRow>>('/api/v1/admin/refunds', { status: status || undefined, page }), [status, page]);
  const admin = auth.isAdmin();

  async function process(r: RefundRow) {
    if (!confirmAction(`Send refund of ${money(r.amount)} for ${r.orderNumber} to the payment gateway?`)) return;
    try { await api.post(`/api/v1/admin/refunds/${r.id}/process`); toast.ok('Refund processed'); state.reload(); } catch (e) { toast.err(e); }
  }
  async function reject(r: RefundRow) {
    const note = window.prompt('Reason for rejecting this refund?');
    if (note === null) return;
    try { await api.post(`/api/v1/admin/refunds/${r.id}/reject`, { note }); toast.ok('Refund rejected'); state.reload(); } catch (e) { toast.err(e); }
  }

  return (
    <>
      <div className="page-head"><h1>Refund management</h1></div>
      <div className="toolbar">
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} aria-label="Status">
          <option value="">All</option>{['REQUESTED', 'APPROVED', 'PROCESSED', 'FAILED', 'REJECTED'].map((s) => <option key={s}>{s}</option>)}
        </select>
      </div>
      <p className="muted small">Online payments are refunded through Razorpay. COD refunds are recorded as MANUAL — return the money by cash/UPI and then process here.</p>
      <Loadable state={state} empty={(d) => (d.items.length === 0 ? <Empty title="No refunds here" /> : null)}>{(d) => (
        <>
          <div className="card flush"><table className="table">
            <thead><tr><th>Order</th><th>Amount</th><th>Reason</th><th>Status</th><th>Gateway ref</th><th>Created</th><th></th></tr></thead>
            <tbody>{d.items.map((r) => (
              <tr key={r.id}><td><Link to={`/orders/${r.orderId}`}>{r.orderNumber}</Link></td><td className="num">{money(r.amount)}</td><td>{r.reason}</td>
                <td><Badge value={r.status} /></td><td><small>{r.providerRefundId}</small></td><td><small>{dateTime(r.createdAt)}</small></td>
                <td className="actions">{admin && (r.status === 'REQUESTED' || r.status === 'FAILED') && <><button className="btn small primary" onClick={() => process(r)}>Approve & process</button>
                  {r.status === 'REQUESTED' && <button className="btn small danger" onClick={() => reject(r)}>Reject</button>}</>}</td></tr>
            ))}</tbody></table></div>
          <Pager page={d.page} totalPages={d.totalPages} onPage={setPage} />
        </>
      )}</Loadable>
    </>
  );
}
