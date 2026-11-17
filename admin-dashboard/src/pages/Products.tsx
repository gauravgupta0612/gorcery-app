import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, img } from '../api/client';
import { AdminProduct, Category, Page } from '../api/types';
import { Badge, confirmAction, Empty, Loadable, Pager, toast, useAsync } from '../components/ui';
import { money } from '../utils/format';

export function flattenCategories(tree: Category[], depth = 0): { id: number; label: string }[] {
  return tree.flatMap((c) => [{ id: c.id, label: `${'— '.repeat(depth)}${c.name}` }, ...flattenCategories(c.children ?? [], depth + 1)]);
}

export default function Products() {
  const [params, setParams] = useSearchParams();
  const [q, setQ] = useState(params.get('q') ?? '');
  const categoryId = params.get('categoryId') ?? '';
  const page = Number(params.get('page') ?? 0);
  const sort = params.get('sort') ?? 'newest';
  const set = (k: string, v: string) => { const p = new URLSearchParams(params); if (v) p.set(k, v); else p.delete(k); if (k !== 'page') p.delete('page'); setParams(p); };
  const cats = useAsync(() => api.get<Category[]>('/api/v1/admin/categories'), []);
  const state = useAsync(() => api.get<Page<AdminProduct>>('/api/v1/admin/products', { q: params.get('q') || undefined, categoryId: categoryId || undefined, sort, page }),
    [params.get('q'), categoryId, page, sort]);

  async function toggle(p: AdminProduct) {
    try {
      const updated = await api.patch<AdminProduct>(`/api/v1/admin/products/${p.id}/active`, { active: !p.active });
      state.setData((d) => d && { ...d, items: d.items.map((x) => (x.id === p.id ? updated : x)) });
      toast.ok(updated.active ? 'Product activated' : 'Product hidden from the app');
    } catch (e) { toast.err(e); }
  }
  async function remove(p: AdminProduct) {
    if (!confirmAction(`Delete "${p.name}"? Products with past orders are deactivated instead.`)) return;
    try { await api.del(`/api/v1/admin/products/${p.id}`); toast.ok('Product removed'); state.reload(); } catch (e) { toast.err(e); }
  }

  return (
    <>
      <div className="page-head"><h1>Products</h1><div className="actions"><Link className="btn" to="/import">Bulk import</Link><Link className="btn primary" to="/products/new">+ Add product</Link></div></div>
      <div className="toolbar">
        <form onSubmit={(e) => { e.preventDefault(); set('q', q.trim()); }}>
          <input placeholder="Search name, brand, SKU or barcode" value={q} onChange={(e) => setQ(e.target.value)} />
          <button className="btn">Search</button>
        </form>
        <select value={categoryId} onChange={(e) => set('categoryId', e.target.value)} aria-label="Category">
          <option value="">All categories</option>
          {flattenCategories(cats.data ?? []).map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
        </select>
        <select value={sort} onChange={(e) => set('sort', e.target.value)} aria-label="Sort">
          <option value="newest">Newest</option><option value="name">Name</option><option value="price_asc">Price ↑</option><option value="price_desc">Price ↓</option><option value="discount">Discount</option>
        </select>
      </div>
      <Loadable state={state} empty={(d) => (d.items.length === 0 ? <Empty title="No products match" action={<Link className="btn primary" to="/products/new">Add a product</Link>} /> : null)}>
        {(d) => (
          <>
            <div className="card flush">
              <table className="table">
                <thead><tr><th></th><th>Product</th><th>Category</th><th>Variants · price · stock</th><th>GST</th><th>Status</th><th></th></tr></thead>
                <tbody>{d.items.map((p) => (
                  <tr key={p.id} className={p.active ? '' : 'dim'}>
                    <td>{p.images[0] && <img className="thumb" src={img(p.images[0].url)} alt="" />}</td>
                    <td><Link to={`/products/${p.id}`}><strong>{p.name}</strong></Link><br /><small className="muted">{p.brandName}{p.isFeatured ? ' · ⭐ featured' : ''}</small></td>
                    <td>{p.categoryName}</td>
                    <td>{p.variants.filter((v) => v.active).map((v) => (
                      <div key={v.id} className="variant-line"><code>{v.sku}</code> {v.label} · {money(v.sellingPrice)} {v.mrp > v.sellingPrice && <s className="muted">{money(v.mrp)}</s>} · <Badge value={v.stockStatus ?? ''} /> {v.stock}</div>
                    ))}</td>
                    <td>{p.gstRate}%</td>
                    <td><Badge value={p.active ? 'ACTIVE' : 'INACTIVE'} tone={p.active ? 'green' : 'grey'} /></td>
                    <td className="actions"><Link className="btn small" to={`/products/${p.id}`}>Edit</Link><button className="btn small" onClick={() => toggle(p)}>{p.active ? 'Hide' : 'Show'}</button><button className="btn small danger" onClick={() => remove(p)}>Delete</button></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            <Pager page={d.page} totalPages={d.totalPages} onPage={(p) => set('page', String(p))} />
          </>
        )}
      </Loadable>
    </>
  );
}
