import { ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '../api/client';
import { statusLabel, statusTone } from '../utils/format';

export function useAsync<T>(fn: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const seq = useRef(0);
  const reload = useCallback(() => {
    const id = ++seq.current;
    setLoading(true);
    setError(null);
    fn().then((d) => { if (id === seq.current) setData(d); })
      .catch((e) => { if (id === seq.current) setError(errorMessage(e)); })
      .finally(() => { if (id === seq.current) setLoading(false); });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  useEffect(() => { reload(); }, [reload]);
  return { data, error, loading, reload, setData };
}

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.body.fields && Object.keys(e.body.fields).length) {
      return e.message + ': ' + Object.entries(e.body.fields).map(([k, v]) => `${k} - ${v}`).join('; ');
    }
    return e.message;
  }
  return e instanceof Error ? e.message : 'Something went wrong';
}

type Toast = { id: number; text: string; kind: 'ok' | 'err' };
let pushToast: (t: Omit<Toast, 'id'>) => void = () => undefined;
export const toast = {
  ok: (text: string) => pushToast({ text, kind: 'ok' }),
  err: (e: unknown) => pushToast({ text: typeof e === 'string' ? e : errorMessage(e), kind: 'err' }),
};
export function Toaster() {
  const [items, setItems] = useState<Toast[]>([]);
  useEffect(() => {
    pushToast = (t) => {
      const id = Date.now() + Math.random();
      setItems((x) => [...x, { ...t, id }]);
      setTimeout(() => setItems((x) => x.filter((i) => i.id !== id)), 4500);
    };
  }, []);
  return <div className="toaster">{items.map((t) => <div key={t.id} className={`toast ${t.kind}`} role="status">{t.text}</div>)}</div>;
}

export function Badge({ value, tone }: { value: string; tone?: ReturnType<typeof statusTone> }) {
  return <span className={`badge ${tone ?? statusTone(value)}`}>{statusLabel(value)}</span>;
}

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return <div className="state"><div className="spinner" aria-hidden /> <span>{label}</span></div>;
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="state error">
      <strong>Couldn't load this</strong>
      <span>{message}</span>
      {onRetry && <button className="btn" onClick={onRetry}>Try again</button>}
    </div>
  );
}

export function Empty({ title, hint, action }: { title: string; hint?: string; action?: ReactNode }) {
  return <div className="state empty"><div className="empty-icon">🧺</div><strong>{title}</strong>{hint && <span>{hint}</span>}{action}</div>;
}

export function Loadable<T>({ state, children, empty }: {
  state: { data: T | null; error: string | null; loading: boolean; reload: () => void };
  children: (d: T) => ReactNode; empty?: (d: T) => ReactNode | null;
}) {
  if (state.loading && !state.data) return <Spinner />;
  if (state.error) return <ErrorState message={state.error} onRetry={state.reload} />;
  if (!state.data) return null;
  const e = empty?.(state.data);
  return <>{e ?? children(state.data)}</>;
}

export function Modal({ title, onClose, children, footer, wide }: { title: string; onClose: () => void; children: ReactNode; footer?: ReactNode; wide?: boolean }) {
  useEffect(() => {
    const h = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal ${wide ? 'wide' : ''}`} role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-head"><h3>{title}</h3><button className="icon-btn" onClick={onClose} aria-label="Close">✕</button></div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  );
}

export function Field({ label, children, hint, error }: { label: string; children: ReactNode; hint?: string; error?: string }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {hint && !error && <span className="field-hint">{hint}</span>}
      {error && <span className="field-error">{error}</span>}
    </label>
  );
}

export function Pager({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (p: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <div className="pager">
      <button className="btn" disabled={page <= 0} onClick={() => onPage(page - 1)}>← Prev</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button className="btn" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next →</button>
    </div>
  );
}

export function Busy({ busy, children, className = 'btn primary', ...rest }: React.ButtonHTMLAttributes<HTMLButtonElement> & { busy?: boolean }) {
  return <button className={className} disabled={busy || rest.disabled} {...rest}>{busy ? 'Please wait…' : children}</button>;
}

export function Stat({ label, value, sub, tone }: { label: string; value: ReactNode; sub?: ReactNode; tone?: string }) {
  return <div className={`stat ${tone ?? ''}`}><span className="stat-label">{label}</span><span className="stat-value">{value}</span>{sub && <span className="stat-sub">{sub}</span>}</div>;
}

export function confirmAction(message: string) { return window.confirm(message); }
