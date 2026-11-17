import { useState } from 'react';
import { api, img } from '../api/client';
import { Category } from '../api/types';
import { Busy, confirmAction, Field, Loadable, Modal, toast, useAsync } from '../components/ui';

type Edit = { id?: number; parentId: number | null; name: string; nameHi: string; imageUrl: string; sortOrder: number; active: boolean };

export default function Categories() {
  const state = useAsync(() => api.get<Category[]>('/api/v1/admin/categories'), []);
  const [edit, setEdit] = useState<Edit | null>(null);
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!edit) return;
    setBusy(true);
    try {
      const body = { ...edit, nameHi: edit.nameHi || null, imageUrl: edit.imageUrl || null };
      if (edit.id) await api.put(`/api/v1/admin/categories/${edit.id}`, body); else await api.post('/api/v1/admin/categories', body);
      toast.ok('Category saved'); setEdit(null); state.reload();
    } catch (e) { toast.err(e); } finally { setBusy(false); }
  }
  async function remove(c: Category) {
    if (!confirmAction(`Delete category "${c.name}"?`)) return;
    try { await api.del(`/api/v1/admin/categories/${c.id}`); toast.ok('Category deleted'); state.reload(); } catch (e) { toast.err(e); }
  }
  const open = (c?: Category, parentId: number | null = null) => setEdit(c
    ? { id: c.id, parentId: c.parentId ?? null, name: c.name, nameHi: c.nameHi ?? '', imageUrl: c.imageUrl ?? '', sortOrder: 0, active: true }
    : { parentId, name: '', nameHi: '', imageUrl: '', sortOrder: 0, active: true });

  return (
    <>
      <div className="page-head"><h1>Categories</h1><button className="btn primary" onClick={() => open()}>+ Add category</button></div>
      <p className="muted">Create unlimited categories and subcategories. Inactive categories are hidden in the app.</p>
      <Loadable state={state}>{(tree) => (
        <div className="cat-grid">
          {tree.map((c) => (
            <section key={c.id} className="card cat-card">
              <div className="cat-head">
                {c.imageUrl && <img className="thumb" src={img(c.imageUrl)} alt="" />}
                <div><strong>{c.name}</strong>{c.nameHi && <div className="muted">{c.nameHi}</div>}</div>
                <div className="actions"><button className="btn small" onClick={() => open(c)}>Edit</button><button className="btn small danger" onClick={() => remove(c)}>Delete</button></div>
              </div>
              <ul className="sub-list">
                {c.children.map((s) => (
                  <li key={s.id}><span>{s.name} {s.nameHi && <small className="muted">· {s.nameHi}</small>}</span>
                    <span><button className="link" onClick={() => open(s)}>Edit</button> <button className="link danger" onClick={() => remove(s)}>Delete</button></span></li>
                ))}
              </ul>
              <button className="btn small" onClick={() => open(undefined, c.id)}>+ Subcategory</button>
            </section>
          ))}
        </div>
      )}</Loadable>
      {edit && (
        <Modal title={edit.id ? 'Edit category' : 'New category'} onClose={() => setEdit(null)} footer={<Busy busy={busy} disabled={!edit.name.trim()} onClick={save}>Save</Busy>}>
          <Field label="Parent">
            <select value={edit.parentId ?? ''} onChange={(e) => setEdit({ ...edit, parentId: e.target.value ? Number(e.target.value) : null })}>
              <option value="">— Top level —</option>
              {(state.data ?? []).filter((c) => c.id !== edit.id).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
          <Field label="Name *"><input value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })} /></Field>
          <Field label="Name in Hindi"><input value={edit.nameHi} onChange={(e) => setEdit({ ...edit, nameHi: e.target.value })} /></Field>
          <Field label="Image">
            <div className="row">
              <input value={edit.imageUrl} onChange={(e) => setEdit({ ...edit, imageUrl: e.target.value })} placeholder="/files/uploads/…" />
              <label className="btn">Upload<input type="file" hidden accept="image/*" onChange={async (e) => {
                const file = e.target.files?.[0]; if (!file) return;
                try { const r = await api.upload<{ url: string }>('/api/v1/admin/uploads', file); setEdit((x) => x && { ...x, imageUrl: r.url }); } catch (err) { toast.err(err); }
              }} /></label>
            </div>
          </Field>
          <div className="row">
            <Field label="Sort order"><input type="number" value={edit.sortOrder} onChange={(e) => setEdit({ ...edit, sortOrder: Number(e.target.value) })} /></Field>
            <label className="check"><input type="checkbox" checked={edit.active} onChange={(e) => setEdit({ ...edit, active: e.target.checked })} /> Active</label>
          </div>
        </Modal>
      )}
    </>
  );
}
