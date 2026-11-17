import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { Page, Ticket } from '../api/types';
import { Badge, Busy, Empty, Field, Loadable, Modal, Pager, toast, useAsync } from '../components/ui';
import { dateTime } from '../utils/format';

export default function Support() {
  const [page, setPage] = useState(0);
  const state = useAsync(() => api.get<Page<Ticket>>('/api/v1/admin/support/tickets', { page }), [page]);
  const [t, setT] = useState<Ticket | null>(null);
  const [reply, setReply] = useState('');
  const [status, setStatus] = useState('IN_PROGRESS');
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!t) return; setBusy(true);
    try { await api.patch(`/api/v1/admin/support/tickets/${t.id}`, { status, reply: reply || null }); toast.ok('Ticket updated — customer notified'); setT(null); state.reload(); }
    catch (e) { toast.err(e); } finally { setBusy(false); }
  }
  return (
    <>
      <div className="page-head"><h1>Customer support</h1></div>
      <Loadable state={state} empty={(d) => (d.items.length === 0 ? <Empty title="No support tickets" /> : null)}>{(d) => (
        <>
          <div className="card flush"><table className="table">
            <thead><tr><th>#</th><th>Subject</th><th>Order</th><th>Status</th><th>Updated</th><th></th></tr></thead>
            <tbody>{d.items.map((x) => (
              <tr key={x.id}><td>{x.id}</td><td><strong>{x.subject}</strong><br /><small className="muted">{x.message.slice(0, 120)}</small></td>
                <td>{x.orderId && <Link to={`/orders/${x.orderId}`}>View order</Link>}</td><td><Badge value={x.status} /></td><td><small>{dateTime(x.updatedAt)}</small></td>
                <td><button className="btn small" onClick={() => { setT(x); setReply(x.adminReply ?? ''); setStatus(x.status === 'OPEN' ? 'IN_PROGRESS' : x.status); }}>Open</button></td></tr>
            ))}</tbody></table></div>
          <Pager page={d.page} totalPages={d.totalPages} onPage={setPage} />
        </>
      )}</Loadable>
      {t && (
        <Modal title={`Ticket #${t.id}: ${t.subject}`} onClose={() => setT(null)} footer={<Busy busy={busy} onClick={save}>Save & notify customer</Busy>}>
          <p className="note">{t.message}</p>
          <Field label="Reply"><textarea rows={5} maxLength={2000} value={reply} onChange={(e) => setReply(e.target.value)} /></Field>
          <Field label="Status"><select value={status} onChange={(e) => setStatus(e.target.value)}>{['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map((s) => <option key={s}>{s}</option>)}</select></Field>
        </Modal>
      )}
    </>
  );
}
