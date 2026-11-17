import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api/client';
import { SalesReport } from '../api/types';
import { Loadable, Stat, toast, useAsync } from '../components/ui';
import { money } from '../utils/format';

const iso = (d: Date) => d.toISOString().slice(0, 10);

export default function Reports() {
  const today = new Date();
  const [from, setFrom] = useState(iso(new Date(today.getTime() - 29 * 86400000)));
  const [to, setTo] = useState(iso(today));
  const state = useAsync(() => api.get<SalesReport>('/api/v1/admin/reports/sales', { from, to }), [from, to]);
  return (
    <>
      <div className="page-head"><h1>Sales reports</h1>
        <button className="btn" onClick={() => api.download('/api/v1/admin/reports/sales.csv', `gola-sales-${from}-to-${to}.csv`, { from, to }).catch(toast.err)}>Export CSV</button></div>
      <div className="toolbar">
        <label className="inline">From <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
        <label className="inline">To <input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
      </div>
      <Loadable state={state}>{(r) => (
        <>
          <div className="stats">
            <Stat label="Orders" value={r.totals.orders} />
            <Stat label="Net revenue" value={money(r.totals.netRevenue)} />
            <Stat label="Coupon discounts" value={money(r.totals.discounts)} />
            <Stat label="Delivery + handling" value={money(Number(r.totals.deliveryFees) + Number(r.totals.handlingFees))} />
            <Stat label="GST included" value={money(r.totals.tax)} />
            <Stat label="Refunds" value={money(r.totals.refunds)} tone={Number(r.totals.refunds) > 0 ? 'red' : ''} />
          </div>
          <section className="card">
            <h3>Daily revenue</h3>
            {r.rows.length === 0 ? <p className="muted">No sales in this period.</p> : (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={r.rows.map((x) => ({ day: x.date.slice(5), revenue: Number(x.netRevenue) }))}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#eee" /><XAxis dataKey="day" fontSize={12} /><YAxis fontSize={12} width={70} tickFormatter={(v) => '₹' + v} />
                  <Tooltip formatter={(v: number) => money(v)} /><Bar dataKey="revenue" fill="#0C831F" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </section>
          <div className="grid-3">
            <section className="card"><h3>Top products</h3><table className="table compact"><tbody>{r.topProducts.map((p) => <tr key={p.name}><td>{p.name}</td><td className="num">{p.count}</td><td className="num">{money(p.amount)}</td></tr>)}</tbody></table></section>
            <section className="card"><h3>By category</h3><table className="table compact"><tbody>{r.categorySales.map((p) => <tr key={p.name}><td>{p.name}</td><td className="num">{p.count}</td><td className="num">{money(p.amount)}</td></tr>)}</tbody></table></section>
            <section className="card"><h3>Payment methods</h3><table className="table compact"><tbody>{r.paymentSplit.map((p) => <tr key={p.name}><td>{p.name}</td><td className="num">{p.count}</td><td className="num">{money(p.amount)}</td></tr>)}</tbody></table></section>
          </div>
          <section className="card flush"><table className="table">
            <thead><tr><th>Date</th><th>Orders</th><th>Item total</th><th>Discounts</th><th>Delivery</th><th>Handling</th><th>GST</th><th>Net</th><th>Refunds</th></tr></thead>
            <tbody>{[...r.rows, r.totals].map((x) => (
              <tr key={x.date} className={x.date === 'TOTAL' ? 'total' : ''}><td>{x.date}</td><td>{x.orders}</td><td className="num">{money(x.itemTotal)}</td><td className="num">{money(x.discounts)}</td>
                <td className="num">{money(x.deliveryFees)}</td><td className="num">{money(x.handlingFees)}</td><td className="num">{money(x.tax)}</td><td className="num">{money(x.netRevenue)}</td><td className="num">{money(x.refunds)}</td></tr>
            ))}</tbody></table></section>
        </>
      )}</Loadable>
    </>
  );
}
