import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { InventoryRow } from '../api/types';
import { Badge, Busy, Empty, Field, Loadable, Modal, toast, useAsync } from '../components/ui';
import { money } from '../utils/format';

export default function Inventory() {
  const [params, setParams] = useSearchParams();
  const low = params.get('low') === '1';
  const [q, setQ] = useState(params.get('q') ?? '');
  const state = useAsync(() => api.get<InventoryRow[]>('/api/v1/admin/inventory', { q: params.get('q') || undefined, lowStock: low }), [params.get('q'), low]);
  const [adj, setAdj] = useState<InventoryRow | null>(null);
  const [delta, setDelta] = useState(0);
  const [reason, setReason] = useState('RESTOCK');
  const [ref, setRef] = useState('');
  const [threshold, setThreshold] = useState(10);
  const [busy, setBusy] = useState(false);

  function openAdj(r: InventoryRow) { setAdj(r); setDelta(0); setReason('RESTOCK'); setRef(''); setThreshold(r.lowStockThreshold); }
  async function save() {
    if (!adj) return;
    setBusy(true);
    try {
      if (delta !== 0) await api.post(`/api/v1/admin/inventory/${adj.variantId}/adjust`, { delta, reason, reference: ref || null });
      if (threshold !== adj.lowStockThreshold) await api.put(`/api/v1/admin/inventory/${adj.variantId}/settings`, { lowStockThreshold: threshold });
      toast.ok('Stock updated'); setAdj(null); state.reload();
    } catch (e) { toast.err(e); } finally { setBusy(false); }
  }

  return (
    <>
      <div className="page-head"><h1>Inventory</h1></div>
      <div className="toolbar">
        <form onSubmit={(e) => { e.preventDefault(); const p = new URLSearchParams(params); if (q.trim()) p.set('q', q.trim()); else p.delete('q'); setParams(p); }}>
          <input placeholder="Product, SKU or scan barcode" value={q} onChange={(e) => setQ(e.target.value)} />
          <button className="btn">Search</button>
        </form>
        <label className="check"><input type="checkbox" checked={low} onChange={(e) => { const p = new URLSearchParams(params); if (e.target.checked) p.set('low', '1'); else p.delete('low'); setParams(p); }} /> Low-stock alerts only</label>
      </div>
      <Loadable state={state} empty={(d) => (d.length === 0 ? <Empty title={low ? 'No low-stock items 🎉' : 'Nothing found'} /> : null)}>
        {(rows) => (
          <div className="card flush"><table className="table">
            <thead><tr><th>Product</th><th>SKU</th><th>Barcode</th><th>Category</th><th>Price</th><th>In stock</th><th>Alert at</th><th>Status</th><th></th></tr></thead>
            <tbody>{rows.map((r) => (
              <tr key={r.variantId} className={r.active ? '' : 'dim'}>
                <td><Link to={`/products/${r.productId}`}>{r.productName}</Link><br /><small className="muted">{r.label}{r.brand ? ` · ${r.brand}` : ''}</small></td>
                <td><code>{r.sku}</code></td><td><small>{r.barcode}</small></td><td>{r.category}</td><td className="num">{money(r.sellingPrice)}</td>
                <td className="num"><strong>{r.quantity}</strong></td><td className="num">{r.lowStockThreshold}</td><td><Badge value={r.status} /></td>
                <td><button className="btn small" onClick={() => openAdj(r)}>Adjust</button></td>
              </tr>
            ))}</tbody>
          </table></div>
        )}
      </Loadable>
      {adj && (
        <Modal title={`Adjust stock · ${adj.productName} (${adj.label})`} onClose={() => setAdj(null)} footer={<Busy busy={busy} onClick={save}>Save</Busy>}>
          <p>Current stock: <strong>{adj.quantity}</strong> → after change: <strong>{adj.quantity + delta}</strong></p>
          <div className="row">
            <Field label="Change (+ add / − remove)"><input type="number" step="1" value={delta} onChange={(e) => setDelta(Math.trunc(Number(e.target.value)))} /></Field>
            <Field label="Reason"><select value={reason} onChange={(e) => setReason(e.target.value)}><option>RESTOCK</option><option>ADJUSTMENT</option><option>RETURN</option><option>DAMAGED</option></select></Field>
          </div>
          <Field label="Reference (PO / note)"><input value={ref} maxLength={60} onChange={(e) => setRef(e.target.value)} /></Field>
          <Field label="Low-stock alert threshold"><input type="number" min={0} value={threshold} onChange={(e) => setThreshold(Number(e.target.value))} /></Field>
        </Modal>
      )}
    </>
  );
}
