import { describe, expect, it } from 'vitest';
import { money, NEXT_STATUS, statusLabel, statusTone } from '../utils/format';
import { buildUrl } from '../api/client';

describe('format helpers', () => {
  it('formats INR', () => {
    expect(money(1299.5)).toContain('1,299.50');
    expect(money(null)).toContain('0');
  });
  it('labels statuses', () => {
    expect(statusLabel('OUT_FOR_DELIVERY')).toBe('Out For Delivery');
    expect(statusTone('DELIVERED')).toBe('green');
    expect(statusTone('CANCELLED')).toBe('red');
  });
  it('mirrors backend admin workflow (no skipping steps)', () => {
    expect(NEXT_STATUS.PLACED).toEqual(['CONFIRMED', 'CANCELLED']);
    expect(NEXT_STATUS.DELIVERED).toBeUndefined();
  });
});

describe('buildUrl', () => {
  it('drops empty params and repeats arrays', () => {
    expect(buildUrl('/x', { a: 1, b: '', c: undefined, d: [1, 2] })).toBe('/x?a=1&d=1&d=2');
  });
});
