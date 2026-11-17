import { FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, img } from '../api/client';
import { AdminProduct, AdminVariant, Brand, Category, Supplier, Unit } from '../api/types';
import { Busy, errorMessage, Field, Spinner, toast, useAsync } from '../components/ui';
import { flattenCategories } from './Products';

const UNITS: Unit[] = ['KG', 'G', 'L', 'ML', 'PCS'];
const emptyVariant = (): AdminVariant => ({ sku: '', barcode: '', label: '', quantityValue: 1, unit: 'PCS', mrp: 0, sellingPrice: 0, maxPerOrder: 10, isDefault: false, active: true, stock: 0, lowStockThreshold: 10 });

interface FormState {
  name: string; nameHi: string; description: string; brandName: string; categoryId: number | ''; supplierId: number | ''; hsnCode: string;
  gstRate: number; isFeatured: boolean; active: boolean; variants: AdminVariant[]; imageUrls: string[];
}

export default function ProductForm() {
  const { id } = useParams();
  const isNew = !id;
  const nav = useNavigate();
  const cats = useAsync(() => api.get<Category[]>('/api/v1/admin/categories'), []);
  const brands = useAsync(() => api.get<Brand[]>('/api/v1/admin/brands'), []);
  const suppliers = useAsync(() => api.get<Supplier[]>('/api/v1/admin/suppliers'), []);
  const [f, setF] = useState<FormState | null>(isNew ? { name: '', nameHi: '', description: '', brandName: '', categoryId: '', supplierId: '', hsnCode: '', gstRate: 0, isFeatured: false, active: true, variants: [{ ...emptyVariant(), isDefault: true }], imageUrls: [] } : null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isNew) return;
    api.get<AdminProduct>(`/api/v1/admin/products/${id}`).then((p) => setF({
      name: p.name, nameHi: p.nameHi ?? '', description: p.description ?? '', brandName: p.brandName ?? '', categoryId: p.categoryId,
      supplierId: p.supplierId ?? '', hsnCode: p.hsnCode ?? '', gstRate: Number(p.gstRate), isFeatured: p.isFeatured, active: p.active,
      variants: p.variants.filter((v) => v.active).map((v) => ({ ...v, barcode: v.barcode ?? '' })), imageUrls: p.images.map((i) => i.url),
    })).catch((e) => setLoadError(errorMessage(e)));
  }, [id, isNew]);

  if (loadError) return <div className="alert err">{loadError}</div>;
  if (!f) return <Spinner />;

  const up = (patch: Partial<FormState>) => setF({ ...f, ...patch });
  const upV = (i: number, patch: Partial<AdminVariant>) => up({ variants: f.variants.map((v, j) => (j === i ? { ...v, ...patch } : patch.isDefault ? { ...v, isDefault: false } : v)) });

  async function uploadImage(file?: File) {
    if (!file || !f) return;
    setUploading(true);
    try {
      const r = await api.upload<{ url: string }>('/api/v1/admin/uploads', file);
      setF((cur) => cur && { ...cur, imageUrls: [...cur.imageUrls, r.url] });
    } catch (e) { toast.err(e); } finally { setUploading(false); }
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!f) return;
    setError(null);
    if (f.variants.some((v) => Number(v.sellingPrice) > Number(v.mrp))) { setError('Selling price cannot be higher than MRP'); return; }
    setBusy(true);
    const body = {
      ...f, categoryId: Number(f.categoryId), supplierId: f.supplierId === '' ? null : Number(f.supplierId), gstRate: Number(f.gstRate),
      brandName: f.brandName.trim() || null, nameHi: f.nameHi || null, hsnCode: f.hsnCode || null,
      variants: f.variants.map((v) => ({ ...v, barcode: v.barcode || null, mrp: Number(v.mrp), sellingPrice: Number(v.sellingPrice), quantityValue: Number(v.quantityValue), stock: Number(v.stock), lowStockThreshold: Number(v.lowStockThreshold), maxPerOrder: Number(v.maxPerOrder) })),
    };
    try {
      const saved = isNew ? await api.post<AdminProduct>('/api/v1/admin/products', body) : await api.put<AdminProduct>(`/api/v1/admin/products/${id}`, body);
      toast.ok('Product saved');
      nav(`/products/${saved.id}`, { replace: true });
      if (!isNew) setF({ ...f, variants: saved.variants.filter((v) => v.active).map((v) => ({ ...v, barcode: v.barcode ?? '' })) });
    } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  }

  return (
    <form onSubmit={submit}>
      <div className="page-head"><h1>{isNew ? 'Add product' : `Edit: ${f.name}`}</h1><div className="actions"><Link className="btn" to="/products">Cancel</Link><Busy busy={busy} type="submit">Save product</Busy></div></div>
      {error && <div className="alert err">{error}</div>}
      <div className="grid-2">
        <section className="card">
          <h3>Details</h3>
          <Field label="Product name *"><input required maxLength={160} value={f.name} onChange={(e) => up({ name: e.target.value })} /></Field>
          <Field label="Name in Hindi"><input maxLength={160} value={f.nameHi} onChange={(e) => up({ nameHi: e.target.value })} placeholder="हिंदी नाम" /></Field>
          <Field label="Description"><textarea rows={4} maxLength={5000} value={f.description} onChange={(e) => up({ description: e.target.value })} /></Field>
          <div className="row">
            <Field label="Category *">
              <select required value={f.categoryId} onChange={(e) => up({ categoryId: Number(e.target.value) })}>
                <option value="">Select…</option>
                {flattenCategories(cats.data ?? []).map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
              </select>
            </Field>
            <Field label="Brand" hint="Type a new name to create a brand">
              <input list="brands" value={f.brandName} onChange={(e) => up({ brandName: e.target.value })} />
              <datalist id="brands">{(brands.data ?? []).map((b) => <option key={b.id} value={b.name} />)}</datalist>
            </Field>
          </div>
          <div className="row">
            <Field label="Supplier">
              <select value={f.supplierId} onChange={(e) => up({ supplierId: e.target.value ? Number(e.target.value) : '' })}>
                <option value="">—</option>{(suppliers.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
            <Field label="GST rate *">
              <select value={f.gstRate} onChange={(e) => up({ gstRate: Number(e.target.value) })}>{[0, 5, 12, 18, 28].map((r) => <option key={r} value={r}>{r}%</option>)}</select>
            </Field>
            <Field label="HSN code"><input maxLength={10} value={f.hsnCode} onChange={(e) => up({ hsnCode: e.target.value })} /></Field>
          </div>
          <label className="check"><input type="checkbox" checked={f.active} onChange={(e) => up({ active: e.target.checked })} /> Active (visible in app)</label>
          <label className="check"><input type="checkbox" checked={f.isFeatured} onChange={(e) => up({ isFeatured: e.target.checked })} /> Featured on home screen</label>
        </section>
        <section className="card">
          <h3>Images</h3>
          <div className="image-grid">
            {f.imageUrls.map((u, i) => (
              <div key={u + i} className="image-tile">
                <img src={img(u)} alt="" />
                <div className="image-actions">
                  {i > 0 && <button type="button" className="btn small" onClick={() => { const a = [...f.imageUrls]; [a[i - 1], a[i]] = [a[i], a[i - 1]]; up({ imageUrls: a }); }}>←</button>}
                  <button type="button" className="btn small danger" onClick={() => up({ imageUrls: f.imageUrls.filter((_, j) => j !== i) })}>Remove</button>
                </div>
                {i === 0 && <span className="badge green primary-tag">Main</span>}
              </div>
            ))}
            <label className="image-tile add">
              {uploading ? 'Uploading…' : '+ Upload image'}
              <input type="file" accept="image/png,image/jpeg,image/webp" hidden onChange={(e) => { uploadImage(e.target.files?.[0]); e.target.value = ''; }} />
            </label>
          </div>
          <p className="muted small">JPG, PNG or WEBP up to 10 MB. The first image is shown in listings.</p>
        </section>
      </div>
      <section className="card">
        <div className="page-head"><h3>Variants, pricing & stock</h3><button type="button" className="btn" onClick={() => up({ variants: [...f.variants, emptyVariant()] })}>+ Add variant</button></div>
        <div className="table-scroll">
          <table className="table form-table">
            <thead><tr><th>Default</th><th>SKU *</th><th>Barcode</th><th>Label *</th><th>Qty</th><th>Unit</th><th>MRP ₹ *</th><th>Price ₹ *</th><th>Stock</th><th>Low at</th><th>Max/order</th><th></th></tr></thead>
            <tbody>{f.variants.map((v, i) => (
              <tr key={v.id ?? `new-${i}`}>
                <td><input type="radio" name="def" checked={v.isDefault} onChange={() => upV(i, { isDefault: true })} aria-label="Default variant" /></td>
                <td><input required value={v.sku} pattern="[A-Za-z0-9_\-]+" onChange={(e) => upV(i, { sku: e.target.value.toUpperCase() })} /></td>
                <td><input value={v.barcode ?? ''} pattern="\d{8,14}" title="8-14 digits" onChange={(e) => upV(i, { barcode: e.target.value })} /></td>
                <td><input required value={v.label} placeholder="1 kg" onChange={(e) => upV(i, { label: e.target.value })} /></td>
                <td><input type="number" min="0.001" step="0.001" required value={v.quantityValue} onChange={(e) => upV(i, { quantityValue: Number(e.target.value) })} /></td>
                <td><select value={v.unit} onChange={(e) => upV(i, { unit: e.target.value as Unit })}>{UNITS.map((u) => <option key={u}>{u}</option>)}</select></td>
                <td><input type="number" min="0.01" step="0.01" required value={v.mrp} onChange={(e) => upV(i, { mrp: Number(e.target.value) })} /></td>
                <td><input type="number" min="0.01" step="0.01" required value={v.sellingPrice} onChange={(e) => upV(i, { sellingPrice: Number(e.target.value) })} />
                  {v.mrp > 0 && v.sellingPrice > 0 && v.sellingPrice < v.mrp && <small className="muted">{Math.floor(((v.mrp - v.sellingPrice) * 100) / v.mrp)}% off</small>}</td>
                <td><input type="number" min="0" step="1" value={v.stock} onChange={(e) => upV(i, { stock: Number(e.target.value) })} /></td>
                <td><input type="number" min="0" step="1" value={v.lowStockThreshold} onChange={(e) => upV(i, { lowStockThreshold: Number(e.target.value) })} /></td>
                <td><input type="number" min="1" max="100" step="1" value={v.maxPerOrder} onChange={(e) => upV(i, { maxPerOrder: Number(e.target.value) })} /></td>
                <td>{f.variants.length > 1 && <button type="button" className="btn small danger" onClick={() => up({ variants: f.variants.filter((_, j) => j !== i) })}>✕</button>}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <p className="muted small">Changing stock here records an inventory adjustment. Removed variants are deactivated (kept for order history).</p>
      </section>
    </form>
  );
}
