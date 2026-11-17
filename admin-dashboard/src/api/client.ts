// Thin fetch wrapper with JWT auth + automatic refresh-token rotation.
const BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

export interface ApiErrorBody { code: string; message: string; fields?: Record<string, string> }

export class ApiError extends Error {
  constructor(public status: number, public body: ApiErrorBody) {
    super(body.message || `Request failed (${status})`);
  }
}

export interface AuthUser { id: number; fullName: string; phone: string; email?: string; roles: string[] }
interface TokenResponse { accessToken: string; refreshToken: string; expiresIn: number; user: AuthUser }

const KEY = 'gola_admin_session';
type Session = { accessToken: string; refreshToken: string; user: AuthUser };

let session: Session | null = (() => {
  try { return JSON.parse(sessionStorage.getItem(KEY) ?? 'null'); } catch { return null; }
})();
const listeners = new Set<(u: AuthUser | null) => void>();

function save(s: Session | null) {
  session = s;
  try {
    if (s) sessionStorage.setItem(KEY, JSON.stringify(s)); else sessionStorage.removeItem(KEY);
  } catch { /* storage unavailable - keep in memory */ }
  listeners.forEach((l) => l(s?.user ?? null));
}

export const auth = {
  user: () => session?.user ?? null,
  subscribe(fn: (u: AuthUser | null) => void) { listeners.add(fn); return () => { listeners.delete(fn); }; },
  async login(identifier: string, password: string) {
    const res = await request<TokenResponse>('/api/v1/auth/login', { method: 'POST', body: { identifier, password }, anonymous: true });
    if (!res.user.roles.some((r) => r === 'ADMIN' || r === 'STORE_MANAGER')) {
      throw new ApiError(403, { code: 'FORBIDDEN', message: 'This account does not have admin access' });
    }
    save({ accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user });
    return res.user;
  },
  async logout() {
    const s = session;
    save(null);
    if (s) await fetch(BASE + '/api/v1/auth/logout', { method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: s.refreshToken }) }).catch(() => undefined);
  },
  isAdmin: () => session?.user.roles.includes('ADMIN') ?? false,
  token: () => session?.accessToken ?? null,
};

let refreshing: Promise<boolean> | null = null;
async function refresh(): Promise<boolean> {
  if (!session) return false;
  refreshing ??= (async () => {
    try {
      const r = await fetch(BASE + '/api/v1/auth/refresh', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken: session!.refreshToken }),
      });
      if (!r.ok) { save(null); return false; }
      const t = (await r.json()) as TokenResponse;
      save({ accessToken: t.accessToken, refreshToken: t.refreshToken, user: t.user });
      return true;
    } catch { return false; } finally { setTimeout(() => { refreshing = null; }, 0); }
  })();
  return refreshing;
}

export interface RequestOptions {
  method?: string;
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined | null | (string | number)[]>;
  anonymous?: boolean;
  raw?: boolean;
}

export function buildUrl(path: string, query?: RequestOptions['query']) {
  const params = new URLSearchParams();
  Object.entries(query ?? {}).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return;
    if (Array.isArray(v)) v.forEach((x) => params.append(k, String(x)));
    else params.append(k, String(v));
  });
  const qs = params.toString();
  return BASE + path + (qs ? `?${qs}` : '');
}

export async function request<T>(path: string, opts: RequestOptions = {}, retried = false): Promise<T> {
  const headers: Record<string, string> = {};
  const isForm = opts.body instanceof FormData;
  if (opts.body !== undefined && !isForm) headers['Content-Type'] = 'application/json';
  if (!opts.anonymous && session) headers.Authorization = `Bearer ${session.accessToken}`;
  let res: Response;
  try {
    res = await fetch(buildUrl(path, opts.query), {
      method: opts.method ?? 'GET', headers,
      body: opts.body === undefined ? undefined : isForm ? (opts.body as FormData) : JSON.stringify(opts.body),
    });
  } catch {
    throw new ApiError(0, { code: 'NETWORK', message: 'Cannot reach the server. Check that the backend is running.' });
  }
  if (res.status === 401 && !opts.anonymous && !retried && (await refresh())) return request<T>(path, opts, true);
  if (!res.ok) {
    let body: ApiErrorBody = { code: 'HTTP_' + res.status, message: `Request failed (${res.status})` };
    try { body = await res.json(); } catch { /* not JSON */ }
    if (res.status === 401) save(null);
    throw new ApiError(res.status, body);
  }
  if (opts.raw) return (await res.blob()) as T;
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  get: <T>(p: string, query?: RequestOptions['query']) => request<T>(p, { query }),
  post: <T>(p: string, body?: unknown) => request<T>(p, { method: 'POST', body }),
  put: <T>(p: string, body?: unknown) => request<T>(p, { method: 'PUT', body }),
  patch: <T>(p: string, body?: unknown) => request<T>(p, { method: 'PATCH', body }),
  del: <T>(p: string) => request<T>(p, { method: 'DELETE' }),
  upload: <T>(p: string, file: File) => { const f = new FormData(); f.append('file', file); return request<T>(p, { method: 'POST', body: f }); },
  download: async (p: string, filename: string, query?: RequestOptions['query']) => {
    const blob = await request<Blob>(p, { query, raw: true });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  },
};

/** Resolves backend-relative image paths (/files/...) against the API base. */
export const img = (url?: string | null) => (!url ? '' : url.startsWith('http') ? url : BASE + url);
