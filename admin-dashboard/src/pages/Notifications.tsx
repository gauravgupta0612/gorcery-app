import { FormEvent, useState } from 'react';
import { api } from '../api/client';
import { Busy, Field, toast } from '../components/ui';

export default function Notifications() {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [type, setType] = useState('OFFER');
  const [busy, setBusy] = useState(false);
  async function send(e: FormEvent) {
    e.preventDefault();
    if (!window.confirm('Send this notification to ALL customers?')) return;
    setBusy(true);
    try { const r = await api.post<{ recipients: number }>('/api/v1/admin/notifications/broadcast', { title, body, type }); toast.ok(`Sent to ${r.recipients} customers`); setTitle(''); setBody(''); }
    catch (err) { toast.err(err); } finally { setBusy(false); }
  }
  return (
    <>
      <div className="page-head"><h1>Push notifications</h1></div>
      <form className="card narrow" onSubmit={send}>
        <p className="muted">Saved to every customer's in-app inbox and sent via Firebase Cloud Messaging (topic <code>all-customers</code>) when FCM is configured.</p>
        <Field label="Title"><input required maxLength={150} value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Weekend mango fest 🥭" /></Field>
        <Field label="Message"><textarea required rows={4} maxLength={500} value={body} onChange={(e) => setBody(e.target.value)} placeholder="Alphonso mangoes at ₹429/kg. Order now!" /></Field>
        <Field label="Type"><select value={type} onChange={(e) => setType(e.target.value)}><option value="OFFER">Offer</option><option value="GENERAL">General</option></select></Field>
        <Busy busy={busy} type="submit">Send to all customers</Busy>
      </form>
    </>
  );
}
