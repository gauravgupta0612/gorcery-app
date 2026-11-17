import { useState } from 'react';
import { api, img } from '../api/client';
import { Banner } from '../api/types';
import { Badge, Busy, confirmAction, Empty, Field, Loadable, Modal, toast, useAsync } from '../components/ui';

type Form = Omit<Banner, 'id'> & { id?: number };
const blank: Form = { title: '', subtitle: '', imageUrl: '', linkType: 'NONE', linkValue: '', bgColor: '#0C831F', sortOrder: 0, active: true, startsAt: null, endsAt: null };

export default function Banners() {
  const state = useAsync(() => api.get<Banner[]>('/api/v1/admin/banners'), []);
  const [f, setF] = useState<Form | null>(null);
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!f) return; setBusy(true);
    try {
      const body = { ...f, linkValue: f.linkValue || null, subtitle: f.subtitle || null };
      if (f.id) await api.put(`/api/v1/admin/banners/${f.id}`, body); else await api.post('/api/v1/admin/banners', body);
      toast.ok('Banner saved'); setF(null); state.reload();
    } catch (e) { toast.err(e); } finally { setBusy(false); }
  }
  async function remove(b: Banner) {
    if (!confirmAction(`Delete banner "${b.title}"?`)) return;
    try { await api.del(`/api/v1/admin/banners/${b.id}`); state.reload(); } catch (e) { toast.err(e); }
  }

  return (
    <>
      <div className="page-head"><h1>Promotional banners</h1><button className="btn primary" onClick={() => setF({ ...blank })}>+ New banner</button></div>
      <Loadable state={state} empty={(d) => (d.length === 0 ? <Empty title="No banners" /> : null)}>{(list) => (
        <div className="banner-grid">{list.map((b) => (
          <div key={b.id} className="card banner-card">
            <img src={img(b.imageUrl)} alt={b.title} />
            <div className="row between"><div><strong>{b.title}</strong><br /><small className="muted">{b.subtitle}</small><br /><small>→ {b.linkType} {b.linkValue}</small></div>
              <Badge value={b.active ? 'ACTIVE' : 'INACTIVE'} tone={b.active ? 'green' : 'grey'} /></div>
            <div className="actions"><button className="btn small" onClick={() => setF(b)}>Edit</button><button className="btn small danger" onClick={() => remove(b)}>Delete</button></div>
          </div>
        ))}</div>
      )}</Loadable>
      {f && (
        <Modal title={f.id ? 'Edit banner' : 'New banner'} onClose={() => setF(null)} footer={<Busy busy={busy} disabled={!f.title || !f.imageUrl} onClick={save}>Save</Busy>}>
          <Field label="Title *"><input value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} /></Field>
          <Field label="Subtitle"><input value={f.subtitle ?? ''} onChange={(e) => setF({ ...f, subtitle: e.target.value })} /></Field>
          <Field label="Image * (recommended 1200×500)">
            <div className="row">
              <input value={f.imageUrl} onChange={(e) => setF({ ...f, imageUrl: e.target.value })} />
              <label className="btn">Upload<input type="file" hidden accept="image/*" onChange={async (e) => {
                const file = e.target.files?.[0]; if (!file) return;
                try { const r = await api.upload<{ url: string }>('/api/v1/admin/uploads', file); setF((x) => x && { ...x, imageUrl: r.url }); } catch (err) { toast.err(err); }
              }} /></label>
            </div>
            {f.imageUrl && <img className="preview" src={img(f.imageUrl)} alt="" />}
          </Field>
          <div className="row">
            <Field label="Opens"><select value={f.linkType} onChange={(e) => setF({ ...f, linkType: e.target.value })}><option value="NONE">Nothing</option><option value="CATEGORY">Category (slug)</option><option value="PRODUCT">Product (id)</option><option value="COUPON">Coupon (code)</option></select></Field>
            <Field label="Link value"><input value={f.linkValue ?? ''} onChange={(e) => setF({ ...f, linkValue: e.target.value })} /></Field>
          </div>
          <div className="row">
            <Field label="Background"><input type="color" value={f.bgColor ?? '#0C831F'} onChange={(e) => setF({ ...f, bgColor: e.target.value })} /></Field>
            <Field label="Sort order"><input type="number" value={f.sortOrder} onChange={(e) => setF({ ...f, sortOrder: Number(e.target.value) })} /></Field>
            <label className="check"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} /> Active</label>
          </div>
        </Modal>
      )}
    </>
  );
}
