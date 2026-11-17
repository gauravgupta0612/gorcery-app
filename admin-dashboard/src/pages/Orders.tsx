import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { OrderRow, Page } from '../api/types';
import { Badge, Empty, Loadable, Pager, useAsync } from '../components/ui';
import { dateTime, money, statusLabel } from '../utils/format';

const STATUSES = ['PENDING_PAYMENT', 'PLACED', 'CONFIRMED', 'PREPARING', 'PACKED', 'ASSIGNED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'PAYMENT_FAILED'];

export default function Orders() {
  const [params, setParams] = useSearchParams();
  const status = params.get('status') ?? '';
  const [q, setQ] = useState(params.get('q') ?? '');
  const from = params.get('from') ?? '';
  const to = params.get('to') ?? '';
  const page = Number(params.get('page') ?? 0);
  const set = (k: string, v: string) => { const p = new URLSearchParams(params); if (v) p.set(k, v); else p.delete(k); if (k !== 'page') p.delete('page'); setParams(p); };
  const state = useAsync(() => api.get<Page<OrderRow>>('/api/v1/admin/orders', { status: status || undefined, q: params.get('q') || undefined, from: from || undefined, to: to || undefined, page }), [status, params.get('q'), from, to, page]);

  return (
    <>
      <div className="page-head"><h1>Orders</h1><button className="btn" onClick={state.reload}>Refresh</button></div>
      <div className="toolbar">
        <form onSubmit={(e) => { e.preventDefault(); set('q', q.trim()); }}>
          <input placeholder="Order no., phone or name" value={q} onChange={(e) => setQ(e.target.value)} />
          <button className="btn">Search</button>
        </form>
        <select value={status} onChange={(e) => set('status', e.target.value)} aria-label="Status">
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{statusLabel(s)}</option>)}
        </select>
        <label className="inline">From <input type="date" value={from} onChange={(e) => set('from', e.target.value)} /></label>
        <label className="inline">To <input type="date" value={to} onChange={(e) => set('to', e.target.value)} /></label>
      </div>
      <Loadable state={state} empty={(d) => (d.items.length === 0 ? <Empty title="No orders found" hint="Orders placed from the app appear here." /> : null)}>
        {(d) => (
          <>
            <div className="card flush">
              <table className="table">
                <thead><tr><th>Order</th><th>Customer</th><th>Pincode</th><th>Items</th><th>Total</th><th>Payment</th><th>Status</th><th>Placed</th></tr></thead>
                <tbody>
                  {d.items.map((o) => (
                    <tr key={o.id}>
                      <td><Link to={`/orders/${o.id}`}><strong>{o.orderNumber}</strong></Link></td>
                      <td>{o.customerName}<br /><small className="muted">{o.customerPhone}</small></td>
                      <td>{o.pincode}</td>
                      <td>{o.itemCount}</td>
                      <td className="num">{money(o.grandTotal)}</td>
                      <td>{o.paymentMethod}<br /><Badge value={o.paymentStatus} /></td>
                      <td><Badge value={o.status} /></td>
                      <td><small>{dateTime(o.placedAt)}</small></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pager page={d.page} totalPages={d.totalPages} onPage={(p) => set('page', String(p))} />
          </>
        )}
      </Loadable>
    </>
  );
}
