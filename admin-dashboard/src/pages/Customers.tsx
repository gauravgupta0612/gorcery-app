import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, auth } from '../api/client';
import { Customer, Page } from '../api/types';
import { Badge, confirmAction, Empty, Loadable, Modal, Pager, toast, useAsync } from '../components/ui';
import { date, dateTime, money } from '../utils/format';

interface Detail {
  customer: Customer;
  addresses: { id: number; label: string; contactName: string; contactPhone: string; line1: string; line2?: string; city: string; state: string; pincode: string; isDefault: boolean }[];
  recentOrders: { id: number; orderNumber: string; status: string; grandTotal: number; placedAt: string }[];
}

export default function Customers() {
  const [params, setParams] = useSearchParams();
  const [q, setQ] = useState(params.get('q') ?? '');
  const page = Number(params.get('page') ?? 0);
  const openId = params.get('id');
  const state = useAsync(() => api.get<Page<Customer>>('/api/v1/admin/customers', { q: params.get('q') || undefined, page }), [params.get('q'), page]);
  const detail = useAsync(() => (openId ? api.get<Detail>(`/api/v1/admin/customers/${openId}`) : Promise.resolve(null)), [openId]);
  const setParam = (k: string, v: string | null) => { const p = new URLSearchParams(params); if (v) p.set(k, v); else p.delete(k); setParams(p); };

  async function toggle(c: Customer) {
    if (!confirmAction(`${c.enabled ? 'Disable' : 'Enable'} ${c.fullName}? Disabled customers are logged out and cannot sign in.`)) return;
    try { await api.patch(`/api/v1/admin/customers/${c.id}/enabled`, { enabled: !c.enabled }); toast.ok('Customer updated'); state.reload(); detail.reload(); }
    catch (e) { toast.err(e); }
  }

  return (
    <>
      <div className="page-head"><h1>Customers</h1></div>
      <div className="toolbar"><form onSubmit={(e) => { e.preventDefault(); setParam('q', q.trim() || null); }}>
        <input placeholder="Name, phone or email" value={q} onChange={(e) => setQ(e.target.value)} /><button className="btn">Search</button></form></div>
      <Loadable state={state} empty={(d) => (d.items.length === 0 ? <Empty title="No customers yet" /> : null)}>{(d) => (
        <>
          <div className="card flush"><table className="table">
            <thead><tr><th>Name</th><th>Phone</th><th>Email</th><th>Orders</th><th>Delivered spend</th><th>Joined</th><th>Status</th><th></th></tr></thead>
            <tbody>{d.items.map((c) => (
              <tr key={c.id}><td><button className="link" onClick={() => setParam('id', String(c.id))}>{c.fullName}</button></td><td>{c.phone}</td><td>{c.email}</td>
                <td>{c.orderCount}</td><td className="num">{money(c.totalSpent)}</td><td>{date(c.createdAt)}</td>
                <td><Badge value={c.enabled ? (c.verified ? 'ACTIVE' : 'UNVERIFIED') : 'DISABLED'} tone={c.enabled ? (c.verified ? 'green' : 'yellow') : 'red'} /></td>
                <td>{auth.isAdmin() && <button className="btn small" onClick={() => toggle(c)}>{c.enabled ? 'Disable' : 'Enable'}</button>}</td></tr>
            ))}</tbody></table></div>
          <Pager page={d.page} totalPages={d.totalPages} onPage={(p) => setParam('page', String(p))} />
        </>
      )}</Loadable>
      {openId && (
        <Modal wide title="Customer" onClose={() => setParam('id', null)}>
          <Loadable state={detail}>{(dt) => dt && (
            <div className="grid-2">
              <div>
                <h3>{dt.customer.fullName}</h3>
                <p>📞 {dt.customer.phone}<br />✉️ {dt.customer.email ?? '—'}<br />Joined {date(dt.customer.createdAt)}</p>
                <h4>Saved addresses</h4>
                {dt.addresses.length === 0 ? <p className="muted">None</p> : dt.addresses.map((a) => (
                  <p key={a.id} className="note"><strong>{a.label}</strong>{a.isDefault && ' · default'}<br />{a.contactName}, {a.line1}{a.line2 ? `, ${a.line2}` : ''}, {a.city} {a.pincode}</p>
                ))}
              </div>
              <div>
                <h4>Recent orders</h4>
                {dt.recentOrders.length === 0 ? <p className="muted">No orders</p> : (
                  <table className="table compact"><tbody>{dt.recentOrders.map((o) => (
                    <tr key={o.id}><td><Link to={`/orders/${o.id}`}>{o.orderNumber}</Link></td><td><Badge value={o.status} /></td><td className="num">{money(o.grandTotal)}</td><td><small>{dateTime(o.placedAt)}</small></td></tr>
                  ))}</tbody></table>
                )}
              </div>
            </div>
          )}</Loadable>
        </Modal>
      )}
    </>
  );
}
