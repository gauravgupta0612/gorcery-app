import { FormEvent, useEffect, useState } from 'react';
import { api, auth } from '../api/client';
import { StoreSettings } from '../api/types';
import { Busy, Field, Loadable, toast, useAsync } from '../components/ui';

export default function Settings() {
  const state = useAsync(() => api.get<StoreSettings>('/api/v1/admin/settings'), []);
  const [f, setF] = useState<StoreSettings | null>(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => { if (state.data) setF({ ...state.data, openingTime: state.data.openingTime.slice(0, 5), closingTime: state.data.closingTime.slice(0, 5) }); }, [state.data]);
  const admin = auth.isAdmin();

  async function save(e: FormEvent) {
    e.preventDefault();
    if (!f) return; setBusy(true);
    try { const saved = await api.put<StoreSettings>('/api/v1/admin/settings', { ...f, gstin: f.gstin || '' }); state.setData(saved); toast.ok('Settings saved'); }
    catch (err) { toast.err(err); } finally { setBusy(false); }
  }
  const num = (k: keyof StoreSettings) => ({ type: 'number', min: 0, step: '0.01', value: String(f?.[k] ?? ''), onChange: (e: React.ChangeEvent<HTMLInputElement>) => setF((x) => x && { ...x, [k]: Number(e.target.value) }) });

  return (
    <Loadable state={state}>{() => f && (
      <form onSubmit={save}>
        <div className="page-head"><h1>Store settings</h1>{admin ? <Busy busy={busy} type="submit">Save settings</Busy> : <span className="muted">Only admins can change settings</span>}</div>
        <fieldset disabled={!admin} className="grid-2">
          <section className="card">
            <h3>Store</h3>
            <Field label="Store name"><input value={f.storeName} onChange={(e) => setF({ ...f, storeName: e.target.value })} /></Field>
            <Field label="Tagline"><input value={f.tagline} onChange={(e) => setF({ ...f, tagline: e.target.value })} /></Field>
            <div className="row">
              <Field label="Support phone"><input value={f.supportPhone ?? ''} onChange={(e) => setF({ ...f, supportPhone: e.target.value })} /></Field>
              <Field label="Support email"><input type="email" value={f.supportEmail ?? ''} onChange={(e) => setF({ ...f, supportEmail: e.target.value })} /></Field>
            </div>
            <Field label="GSTIN"><input maxLength={15} value={f.gstin ?? ''} onChange={(e) => setF({ ...f, gstin: e.target.value.toUpperCase() })} /></Field>
            <Field label="Store / dark-store address (printed on invoices)"><textarea rows={2} value={f.storeAddress ?? ''} onChange={(e) => setF({ ...f, storeAddress: e.target.value })} /></Field>
            <div className="row">
              <Field label="Opens at"><input type="time" value={f.openingTime} onChange={(e) => setF({ ...f, openingTime: e.target.value })} /></Field>
              <Field label="Closes at"><input type="time" value={f.closingTime} onChange={(e) => setF({ ...f, closingTime: e.target.value })} /></Field>
            </div>
            <label className="check"><input type="checkbox" checked={f.storeOpen} onChange={(e) => setF({ ...f, storeOpen: e.target.checked })} /> Accepting orders (turn off to pause the store)</label>
          </section>
          <section className="card">
            <h3>Delivery charges & checkout</h3>
            <div className="row"><Field label="Default delivery fee ₹"><input {...num('deliveryFee')} /></Field><Field label="Free delivery above ₹"><input {...num('freeDeliveryAbove')} /></Field></div>
            <div className="row"><Field label="Handling fee ₹"><input {...num('handlingFee')} /></Field><Field label="Minimum order ₹"><input {...num('minOrderAmount')} /></Field></div>
            <label className="check"><input type="checkbox" checked={f.codEnabled} onChange={(e) => setF({ ...f, codEnabled: e.target.checked })} /> Cash on Delivery enabled</label>
            <Field label="COD allowed up to ₹"><input {...num('codMaxAmount')} /></Field>
          </section>
        </fieldset>
      </form>
    )}</Loadable>
  );
}
