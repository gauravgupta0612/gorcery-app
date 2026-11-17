import { useState } from 'react';
import { api } from '../api/client';
import { Coupon } from '../api/types';
import { Badge, Busy, confirmAction, Empty, Field, Loadable, Modal, toast, useAsync } from '../components/ui';
import { date, money } from '../utils/format';

type Form = Omit<Coupon, 'id' | 'usedCount'> & { id?: number };
const blank: Form = { code: '', description: '', discountType: 'FLAT', discountValue: 50, maxDiscount: null, minOrderAmount: 199, usageLimit: null, perUserLimit: 1, validFrom: '', validTo: '', firstOrderOnly: false, active: true };
const toLocal = (iso?: string | null) => (iso ? new Date(iso).toISOString().slice(0, 16) : '');

export default function Coupons() {
  const state = useAsync(() => api.get<Coupon[]>('/api/v1/admin/coupons'), []);
  const [f, setF] = useState<Form | null>(null);
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!f) return; setBusy(true);
    try {
      const body = { ...f, maxDiscount: f.maxDiscount || null, usageLimit: f.usageLimit || null,
        validFrom: f.validFrom ? new Date(f.validFrom).toISOString() : null, validTo: f.validTo ? new Date(f.validTo).toISOString() : null };
      if (f.id) await api.put(`/api/v1/admin/coupons/${f.id}`, body); else await api.post('/api/v1/admin/coupons', body);
      toast.ok('Coupon saved'); setF(null); state.reload();
    } catch (e) { toast.err(e); } finally { setBusy(false); }
  }
  async function remove(c: Coupon) {
    if (!confirmAction(`Delete coupon ${c.code}? Used coupons are deactivated instead.`)) return;
    try { await api.del(`/api/v1/admin/coupons/${c.id}`); toast.ok('Coupon removed'); state.reload(); } catch (e) { toast.err(e); }
  }

  return (
    <>
      <div className="page-head"><h1>Coupons & offers</h1><button className="btn primary" onClick={() => setF({ ...blank })}>+ New coupon</button></div>
      <Loadable state={state} empty={(d) => (d.length === 0 ? <Empty title="No coupons yet" /> : null)}>{(list) => (
        <div className="card flush"><table className="table">
          <thead><tr><th>Code</th><th>Offer</th><th>Min order</th><th>Used</th><th>Valid</th><th>Status</th><th></th></tr></thead>
          <tbody>{list.map((c) => (
            <tr key={c.id}><td><code className="coupon">{c.code}</code>{c.firstOrderOnly && <><br /><small className="muted">first order only</small></>}</td>
              <td>{c.discountType === 'FLAT' ? `${money(c.discountValue)} off` : `${c.discountValue}% off${c.maxDiscount ? ` up to ${money(c.maxDiscount)}` : ''}`}<br /><small className="muted">{c.description}</small></td>
              <td>{money(c.minOrderAmount)}</td><td>{c.usedCount}{c.usageLimit ? ` / ${c.usageLimit}` : ''}</td>
              <td><small>{date(c.validFrom)} → {c.validTo ? date(c.validTo) : 'no end'}</small></td>
              <td><Badge value={c.active ? 'ACTIVE' : 'INACTIVE'} tone={c.active ? 'green' : 'grey'} /></td>
              <td className="actions"><button className="btn small" onClick={() => setF({ ...c, validFrom: toLocal(c.validFrom), validTo: toLocal(c.validTo) })}>Edit</button><button className="btn small danger" onClick={() => remove(c)}>Delete</button></td></tr>
          ))}</tbody></table></div>
      )}</Loadable>
      {f && (
        <Modal title={f.id ? `Edit ${f.code}` : 'New coupon'} onClose={() => setF(null)} footer={<Busy busy={busy} disabled={!/^[A-Za-z0-9]{3,30}$/.test(f.code) || f.discountValue <= 0} onClick={save}>Save</Busy>}>
          <div className="row">
            <Field label="Code *"><input value={f.code} onChange={(e) => setF({ ...f, code: e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '') })} /></Field>
            <Field label="Type"><select value={f.discountType} onChange={(e) => setF({ ...f, discountType: e.target.value as 'FLAT' | 'PERCENT' })}><option value="FLAT">Flat ₹</option><option value="PERCENT">Percent %</option></select></Field>
            <Field label={f.discountType === 'FLAT' ? 'Amount ₹ *' : 'Percent *'}><input type="number" min="1" value={f.discountValue} onChange={(e) => setF({ ...f, discountValue: Number(e.target.value) })} /></Field>
          </div>
          <Field label="Description (shown to customers)"><input value={f.description ?? ''} onChange={(e) => setF({ ...f, description: e.target.value })} /></Field>
          <div className="row">
            {f.discountType === 'PERCENT' && <Field label="Max discount ₹"><input type="number" min="1" value={f.maxDiscount ?? ''} onChange={(e) => setF({ ...f, maxDiscount: e.target.value ? Number(e.target.value) : null })} /></Field>}
            <Field label="Min order ₹"><input type="number" min="0" value={f.minOrderAmount} onChange={(e) => setF({ ...f, minOrderAmount: Number(e.target.value) })} /></Field>
            <Field label="Total uses (blank = unlimited)"><input type="number" min="1" value={f.usageLimit ?? ''} onChange={(e) => setF({ ...f, usageLimit: e.target.value ? Number(e.target.value) : null })} /></Field>
            <Field label="Uses per customer"><input type="number" min="1" value={f.perUserLimit} onChange={(e) => setF({ ...f, perUserLimit: Number(e.target.value) })} /></Field>
          </div>
          <div className="row">
            <Field label="Valid from"><input type="datetime-local" value={f.validFrom ?? ''} onChange={(e) => setF({ ...f, validFrom: e.target.value })} /></Field>
            <Field label="Valid to"><input type="datetime-local" value={f.validTo ?? ''} onChange={(e) => setF({ ...f, validTo: e.target.value })} /></Field>
          </div>
          <label className="check"><input type="checkbox" checked={f.firstOrderOnly} onChange={(e) => setF({ ...f, firstOrderOnly: e.target.checked })} /> First order only</label>
          <label className="check"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} /> Active</label>
        </Modal>
      )}
    </>
  );
}
