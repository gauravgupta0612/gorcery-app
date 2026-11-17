import { useState } from 'react';
import { api } from '../api/client';
import { Busy, toast } from '../components/ui';
import { ImportColumns } from '../utils/importColumns';

interface Result { created: number; updated: number; skipped: number; errors: string[] }

export default function ImportPage() {
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<Result | null>(null);

  async function run() {
    if (!file) return;
    setBusy(true); setResult(null);
    try { const r = await api.upload<Result>('/api/v1/admin/products/import', file); setResult(r); toast.ok(`Import finished: ${r.created} created, ${r.updated} updated`); }
    catch (e) { toast.err(e); } finally { setBusy(false); }
  }
  return (
    <>
      <div className="page-head"><h1>Bulk import (Excel / CSV)</h1>
        <button className="btn" onClick={() => api.download('/api/v1/admin/products/import/template', 'gola-products-template.csv').catch(toast.err)}>Download CSV template</button></div>
      <section className="card">
        <p>Upload a <strong>.csv</strong> or <strong>.xlsx</strong> file. One row = one variant. Rows with the same <code>product_name</code> + category become variants of one product.
          Existing SKUs are updated (price, stock, status); new SKUs are created. Missing categories are created automatically.</p>
        <ImportColumns />
        <div className="row">
          <input type="file" accept=".csv,.xlsx" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
          <Busy busy={busy} disabled={!file} onClick={run}>Import</Busy>
        </div>
      </section>
      {result && (
        <section className="card">
          <h3>Result</h3>
          <p>✅ {result.created} products created · 🔁 {result.updated} updated · ⚠️ {result.skipped} rows skipped</p>
          {result.errors.length > 0 && <ul className="errors">{result.errors.map((e, i) => <li key={i}>{e}</li>)}</ul>}
        </section>
      )}
    </>
  );
}
