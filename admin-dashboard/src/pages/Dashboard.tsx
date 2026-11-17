import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api/client';
import { Dashboard as D } from '../api/types';
import { Loadable, Stat, useAsync } from '../components/ui';
import { money, statusLabel } from '../utils/format';

export default function DashboardPage() {
  const state = useAsync(() => api.get<D>('/api/v1/admin/dashboard'), []);
  return (
    <>
      <div className="page-head"><h1>Dashboard</h1><button className="btn" onClick={state.reload}>Refresh</button></div>
      <Loadable state={state}>{(d) => (
        <>
          <div className="stats">
            <Stat label="Orders today" value={d.todayOrders} sub={money(d.todayRevenue) + ' revenue'} />
            <Stat label="To process" value={<Link to="/orders?status=PLACED">{d.pendingOrders}</Link>} sub="placed → assigned" tone="yellow" />
            <Stat label="Out for delivery" value={d.outForDelivery} tone="blue" />
            <Stat label="Customers" value={d.totalCustomers} sub={`+${d.newCustomers7d} this week`} />
            <Stat label="Low-stock items" value={<Link to="/inventory?low=1">{d.lowStockItems}</Link>} tone={d.lowStockItems ? 'red' : ''} />
            <Stat label="Open tickets" value={<Link to="/support">{d.openTickets}</Link>} />
          </div>
          {d.lowStockItems > 0 && (
            <div className="alert warn">⚠️ {d.lowStockItems} item(s) are at or below their low-stock threshold. <Link to="/inventory?low=1">Restock now</Link></div>
          )}
          <div className="grid-2">
            <section className="card">
              <h3>Revenue · last 14 days</h3>
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={d.last14Days.map((p) => ({ ...p, revenue: Number(p.revenue), day: p.date.slice(5) }))}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
                  <XAxis dataKey="day" fontSize={12} />
                  <YAxis fontSize={12} tickFormatter={(v) => '₹' + v} width={70} />
                  <Tooltip formatter={(v: number) => money(v)} />
                  <Line type="monotone" dataKey="revenue" stroke="#0C831F" strokeWidth={2.5} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </section>
            <section className="card">
              <h3>Orders · last 14 days</h3>
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={d.last14Days.map((p) => ({ ...p, day: p.date.slice(5) }))}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
                  <XAxis dataKey="day" fontSize={12} />
                  <YAxis allowDecimals={false} fontSize={12} width={40} />
                  <Tooltip />
                  <Bar dataKey="orders" fill="#F9C80E" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </section>
          </div>
          <div className="grid-3">
            <section className="card">
              <h3>Top products · 30 days</h3>
              {d.topProducts30d.length === 0 ? <p className="muted">No sales yet.</p> : (
                <table className="table compact"><tbody>
                  {d.topProducts30d.map((p) => <tr key={p.name}><td>{p.name}</td><td className="num">{p.count}</td><td className="num">{money(p.amount)}</td></tr>)}
                </tbody></table>
              )}
            </section>
            <section className="card">
              <h3>Orders by status</h3>
              {d.ordersByStatus.length === 0 ? <p className="muted">No orders yet.</p> : (
                <table className="table compact"><tbody>
                  {d.ordersByStatus.map((p) => <tr key={p.name}><td><Link to={`/orders?status=${p.name}`}>{statusLabel(p.name)}</Link></td><td className="num">{p.count}</td></tr>)}
                </tbody></table>
              )}
            </section>
            <section className="card">
              <h3>Payment methods · 30 days</h3>
              {d.paymentSplit30d.length === 0 ? <p className="muted">No payments yet.</p> : (
                <table className="table compact"><tbody>
                  {d.paymentSplit30d.map((p) => <tr key={p.name}><td>{p.name}</td><td className="num">{p.count}</td><td className="num">{money(p.amount)}</td></tr>)}
                </tbody></table>
              )}
            </section>
          </div>
        </>
      )}</Loadable>
    </>
  );
}
