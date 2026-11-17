const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
export const money = (v: number | string | null | undefined) => inr.format(Number(v ?? 0));
export const dateTime = (iso?: string | null) =>
  iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata' }) : '—';
export const date = (iso?: string | null) => (iso ? new Date(iso).toLocaleDateString('en-IN', { dateStyle: 'medium', timeZone: 'Asia/Kolkata' }) : '—');
export const statusLabel = (s: string) => s.split('_').map((w) => w[0] + w.slice(1).toLowerCase()).join(' ');
export const NEXT_STATUS: Record<string, string[]> = {
  PLACED: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PREPARING', 'CANCELLED'],
  PREPARING: ['PACKED', 'CANCELLED'],
  PACKED: ['CANCELLED'],
  ASSIGNED: ['CANCELLED'],
};
export const WORKFLOW = ['PLACED', 'CONFIRMED', 'PREPARING', 'PACKED', 'ASSIGNED', 'OUT_FOR_DELIVERY', 'DELIVERED'];
export function statusTone(s: string): 'green' | 'yellow' | 'red' | 'blue' | 'grey' {
  if (s === 'DELIVERED' || s === 'PAID' || s === 'PROCESSED' || s === 'IN_STOCK' || s === 'RESOLVED') return 'green';
  if (s === 'CANCELLED' || s === 'PAYMENT_FAILED' || s === 'FAILED' || s === 'OUT_OF_STOCK' || s === 'REJECTED') return 'red';
  if (s === 'OUT_FOR_DELIVERY' || s === 'ASSIGNED' || s === 'IN_PROGRESS') return 'blue';
  if (s === 'PENDING_PAYMENT' || s === 'LOW_STOCK' || s === 'REQUESTED' || s === 'COD_PENDING' || s === 'PENDING' || s === 'OPEN') return 'yellow';
  return 'grey';
}
