import { useState } from 'react';
import { api, auth } from '../api/client';
import { Partner } from '../api/types';
import { Badge, Busy, Empty, Field, Loadable, Modal, toast, useAsync } from '../components/ui';

type Form = { id?: number; fullName: string; phone: string; email: string; password: string; vehicleType: string; vehicleNumber: string; licenseNumber: string; active: boolean };
const blank: Form = { fullName: '', phone: '', email: '', password: '', vehicleType: 'BIKE', vehicleNumber: '', licenseNumber: '', active: true };

export default function DeliveryPartners() {
  const state = useAsync(() => api.get<Partner[]>('/api/v1/admin/delivery-partners'), []);
  const [f, setF] = useState<Form | null>(null);
  const [busy, setBusy] = useState(false);
  const admin = auth.isAdmin();

  async function save() {
    if (!f) return; setBusy(true);
    try {
      const body = { ...f, email: f.email || null, password: f.password || null };
      if (f.id) await api.put(`/api/v1/admin/delivery-partners/${f.id}`, body); else await api.post('/api/v1/admin/delivery-partners', body);
      toast.ok('Delivery partner saved'); setF(null); state.reload();
    } catch (e) { toast.err(e); } finally { setBusy(false); }
  }

  return (
    <>
      <div className="page-head"><h1>Delivery partners</h1>{admin && <button className="btn primary" onClick={() => setF({ ...blank })}>+ Add partner</button>}</div>
      <p className="muted">Partners sign in to the Gola Grocery Android app with their mobile number and password to see assigned orders.</p>
      <Loadable state={state} empty={(d) => (d.length === 0 ? <Empty title="No delivery partners yet" hint="Add a partner to start assigning packed orders." /> : null)}>{(list) => (
        <div className="card flush"><table className="table">
          <thead><tr><th>Name</th><th>Phone</th><th>Vehicle</th><th>Active orders</th><th>Delivered</th><th>Availability</th><th>Status</th><th></th></tr></thead>
          <tbody>{list.map((p) => (
            <tr key={p.id}><td><strong>{p.fullName}</strong><br /><small className="muted">{p.email}</small></td><td>{p.phone}</td>
              <td>{p.vehicleType} {p.vehicleNumber}<br /><small className="muted">{p.licenseNumber && `DL ${p.licenseNumber}`}</small></td>
              <td>{p.activeOrders}</td><td>{p.delivered}</td><td>{p.available ? '🟢 Online' : '⚪ Offline'}</td>
              <td><Badge value={p.active ? 'ACTIVE' : 'INACTIVE'} tone={p.active ? 'green' : 'grey'} /></td>
              <td>{admin && <button className="btn small" onClick={() => setF({ id: p.id, fullName: p.fullName, phone: p.phone, email: p.email ?? '', password: '', vehicleType: p.vehicleType, vehicleNumber: p.vehicleNumber ?? '', licenseNumber: p.licenseNumber ?? '', active: p.active })}>Edit</button>}</td></tr>
          ))}</tbody></table></div>
      )}</Loadable>
      {f && (
        <Modal title={f.id ? 'Edit delivery partner' : 'New delivery partner'} onClose={() => setF(null)}
          footer={<Busy busy={busy} disabled={!f.fullName || !/^[6-9]\d{9}$/.test(f.phone) || (!f.id && f.password.length < 8)} onClick={save}>Save</Busy>}>
          <Field label="Full name *"><input value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} /></Field>
          <div className="row">
            <Field label="Mobile number *" hint="10 digits, used to sign in"><input value={f.phone} maxLength={10} onChange={(e) => setF({ ...f, phone: e.target.value.replace(/\D/g, '') })} /></Field>
            <Field label="Email"><input type="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></Field>
          </div>
          <Field label={f.id ? 'New password (leave blank to keep)' : 'Password * (min 8 characters)'}><input type="password" autoComplete="new-password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></Field>
          <div className="row">
            <Field label="Vehicle"><select value={f.vehicleType} onChange={(e) => setF({ ...f, vehicleType: e.target.value })}>{['BIKE', 'SCOOTER', 'BICYCLE', 'CAR', 'VAN'].map((v) => <option key={v}>{v}</option>)}</select></Field>
            <Field label="Vehicle number"><input value={f.vehicleNumber} onChange={(e) => setF({ ...f, vehicleNumber: e.target.value.toUpperCase() })} /></Field>
            <Field label="Licence number"><input value={f.licenseNumber} onChange={(e) => setF({ ...f, licenseNumber: e.target.value.toUpperCase() })} /></Field>
          </div>
          <label className="check"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} /> Active</label>
        </Modal>
      )}
    </>
  );
}
