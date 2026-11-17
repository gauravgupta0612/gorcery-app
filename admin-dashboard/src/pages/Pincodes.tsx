import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { Pincode } from '../api/types';
import { Badge, Busy, confirmAction, Empty, Field, Loadable, Modal, toast, useAsync } from '../components/ui';
import { money } from '../utils/format';

export default function Pincodes() {
  const state = useAsync(() => api.get<Pincode[]>('/api/v1/admin/pincodes'), []);
  const [f, setF] = useState<(Pincode & { isNew?: boolean }) | null>(null);
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!f) return; setBusy(true);
    try { await api.put(`/api/v1/admin/pincodes/${f.pincode}`, { ...f, deliveryFee: f.deliveryFee === null || (f.deliveryFee as unknown) === '' ? null : Number(f.deliveryFee) });
      toast.ok('Pincode saved'); setF(null); state.reload(); } catch (e) { toast.err(e); } finally { setBusy(false); }
  }
  async function remove(p: Pincode) {
    if (!confirmAction(`Stop delivering to ${p.pincode}?`)) return;
    try { await api.del(`/api/v1/admin/pincodes/${p.pincode}`); state.reload(); } catch (e) { toast.err(e); }
  }

  return (
    <>
      <div className="page-head"><h1>Serviceable pincodes & delivery charges</h1><button className="btn primary" onClick={() => setF({ pincode: '', city: '', area: '', deliveryFee: null, etaMinutes: 30, active: true, isNew: true })}>+ Add pincode</button></div>
      <p className="muted">The default delivery fee, free-delivery threshold and handling fee are in <Link to="/settings">Store settings</Link>. A per-pincode fee overrides the default.</p>
      <Loadable state={state} empty={(d) => (d.length === 0 ? <Empty title="No serviceable pincodes" hint="Customers can't order until you add at least one." /> : null)}>{(list) => (
        <div className="card flush"><table className="table">
          <thead><tr><th>Pincode</th><th>City</th><th>Area</th><th>Delivery fee</th><th>ETA</th><th>Status</th><th></th></tr></thead>
          <tbody>{list.map((p) => (
            <tr key={p.pincode}><td><strong>{p.pincode}</strong></td><td>{p.city}</td><td>{p.area}</td><td>{p.deliveryFee == null ? <span className="muted">default</span> : money(p.deliveryFee)}</td>
              <td>{p.etaMinutes} min</td><td><Badge value={p.active ? 'ACTIVE' : 'PAUSED'} tone={p.active ? 'green' : 'grey'} /></td>
              <td className="actions"><button className="btn small" onClick={() => setF(p)}>Edit</button><button className="btn small danger" onClick={() => remove(p)}>Remove</button></td></tr>
          ))}</tbody></table></div>
      )}</Loadable>
      {f && (
        <Modal title={f.isNew ? 'Add pincode' : `Edit ${f.pincode}`} onClose={() => setF(null)} footer={<Busy busy={busy} disabled={!/^[1-9]\d{5}$/.test(f.pincode) || !f.city} onClick={save}>Save</Busy>}>
          <div className="row">
            <Field label="Pincode *"><input disabled={!f.isNew} maxLength={6} value={f.pincode} onChange={(e) => setF({ ...f, pincode: e.target.value.replace(/\D/g, '') })} /></Field>
            <Field label="City *"><input value={f.city} onChange={(e) => setF({ ...f, city: e.target.value })} /></Field>
          </div>
          <Field label="Area"><input value={f.area ?? ''} onChange={(e) => setF({ ...f, area: e.target.value })} /></Field>
          <div className="row">
            <Field label="Delivery fee ₹ (blank = default)"><input type="number" min="0" value={f.deliveryFee ?? ''} onChange={(e) => setF({ ...f, deliveryFee: e.target.value === '' ? null : Number(e.target.value) })} /></Field>
            <Field label="ETA (minutes)"><input type="number" min="5" max="240" value={f.etaMinutes} onChange={(e) => setF({ ...f, etaMinutes: Number(e.target.value) })} /></Field>
          </div>
          <label className="check"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} /> Accepting orders</label>
        </Modal>
      )}
    </>
  );
}
