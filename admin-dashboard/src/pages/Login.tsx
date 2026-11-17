import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth } from '../api/client';
import { Busy, errorMessage, Field } from '../components/ui';

export default function Login() {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const nav = useNavigate();

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await auth.login(identifier.trim(), password);
      nav('/');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={submit}>
        <img src="/logo.svg" alt="" width={64} height={64} />
        <h1>Gola Grocery</h1>
        <p className="muted">Admin console · Freshness Delivered to Your Doorstep.</p>
        <Field label="Mobile number or email">
          <input autoFocus required value={identifier} onChange={(e) => setIdentifier(e.target.value)} autoComplete="username" />
        </Field>
        <Field label="Password">
          <input required type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
        </Field>
        {error && <div className="alert err">{error}</div>}
        <Busy busy={busy} type="submit" className="btn primary block">Sign in</Busy>
        <p className="muted small">The first admin account is created from ADMIN_PHONE / ADMIN_PASSWORD when the backend starts.</p>
      </form>
    </div>
  );
}
