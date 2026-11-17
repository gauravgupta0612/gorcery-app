import { useState } from 'react';
import { api } from '../api/client';
import { Brand, Supplier } from '../api/types';
import { Badge, Busy, Field, Loadable, Modal, toast, useAsync } from '../components/ui';

export default function Brands() {
  const brands = useAsync(() => api.get<Brand[]>('/api/v1/admin/brands'), []);
  const suppliers = useAsync(() => api.get<Supplier[]>('/api/v1/admin/suppliers'), []);
  const [b, setB] = useState<Partial<Brand> | null>(null);
  const [s, setS] = useState<Partial<Supplier> | null>(null);
  const [busy, setBusy] = useState(false);

  async function saveBrand() {
    if (!b) return; setBusy(true);
    try { const body = { name: b.name, logoUrl: b.logoUrl || null, active: b.active ?? true };
      if (b.id) await api.put(`/api/v1/admin/brands/${b.id}`, body); else await api.post('/api/v1/admin/brands', body);
      toast.ok('Brand saved'); setB(null); brands.reload(); } catch (e) { toast.err(e); } finally { setBusy(false); }
  }
  async function saveSupplier() {
    if (!s) return; setBusy(true);
    try { const body = { name: s.name, contactName: s.contactName || null, phone: s.phone || '', email: s.email || null, gstin: s.gstin || '', address: s.address || null, active: s.active ?? true };
      if (s.id) await api.put(`/api/v1/admin/suppliers/${s.id}`, body); else await api.post('/api/v1/admin/suppliers', body);
      toast.ok('Supplier saved'); setS(null); suppliers.reload(); } catch (e) { toast.err(e); } finally { setBusy(false); }
  }

  return (
    <>
      <div className="grid-2">
        <section>
          <div className="page-head"><h1>Brands</h1><button className="btn primary" onClick={() => setB({ name: '', active: true })}>+ Brand</button></div>
          <Loadable state={brands}>{(list) => (
            <div className="card flush"><table className="table"><tbody>
              {list.map((x) => <tr key={x.id}><td>{x.name}</td><td><Badge value={x.active ? 'ACTIVE' : 'INACTIVE'} tone={x.active ? 'green' : 'grey'} /></td><td><button className="btn small" onClick={() => setB(x)}>Edit</button></td></tr>)}
            </tbody></table></div>
          )}</Loadable>
        </section>
        <section>
          <div className="page-head"><h1>Suppliers</h1><button className="btn primary" onClick={() => setS({ name: '', active: true })}>+ Supplier</button></div>
          <Loadable state={suppliers}>{(list) => (
            <div className="card flush"><table className="table"><tbody>
              {list.map((x) => <tr key={x.id}><td><strong>{x.name}</strong><br /><small className="muted">{x.contactName} {x.phone} {x.gstin && `· GSTIN ${x.gstin}`}</small></td><td><button className="btn small" onClick={() => setS(x)}>Edit</button></td></tr>)}
            </tbody></table></div>
          )}</Loadable>
        </section>
      </div>
      {b && <Modal title={b.id ? 'Edit brand' : 'New brand'} onClose={() => setB(null)} footer={<Busy busy={busy} disabled={!b.name?.trim()} onClick={saveBrand}>Save</Busy>}>
        <Field label="Name"><input value={b.name ?? ''} onChange={(e) => setB({ ...b, name: e.target.value })} /></Field>
        <Field label="Logo URL"><input value={b.logoUrl ?? ''} onChange={(e) => setB({ ...b, logoUrl: e.target.value })} /></Field>
        <label className="check"><input type="checkbox" checked={b.active ?? true} onChange={(e) => setB({ ...b, active: e.target.checked })} /> Active</label>
      </Modal>}
      {s && <Modal title={s.id ? 'Edit supplier' : 'New supplier'} onClose={() => setS(null)} footer={<Busy busy={busy} disabled={!s.name?.trim()} onClick={saveSupplier}>Save</Busy>}>
        <Field label="Name"><input value={s.name ?? ''} onChange={(e) => setS({ ...s, name: e.target.value })} /></Field>
        <div className="row"><Field label="Contact person"><input value={s.contactName ?? ''} onChange={(e) => setS({ ...s, contactName: e.target.value })} /></Field>
          <Field label="Phone"><input value={s.phone ?? ''} onChange={(e) => setS({ ...s, phone: e.target.value })} /></Field></div>
        <div className="row"><Field label="Email"><input type="email" value={s.email ?? ''} onChange={(e) => setS({ ...s, email: e.target.value })} /></Field>
          <Field label="GSTIN"><input value={s.gstin ?? ''} onChange={(e) => setS({ ...s, gstin: e.target.value.toUpperCase() })} /></Field></div>
        <Field label="Address"><input value={s.address ?? ''} onChange={(e) => setS({ ...s, address: e.target.value })} /></Field>
        <label className="check"><input type="checkbox" checked={s.active ?? true} onChange={(e) => setS({ ...s, active: e.target.checked })} /> Active</label>
      </Modal>}
    </>
  );
}
