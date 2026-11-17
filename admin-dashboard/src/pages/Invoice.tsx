import { useParams } from 'react-router-dom';
import { api } from '../api/client';
import { Invoice } from '../api/types';
import { Loadable, useAsync } from '../components/ui';
import { dateTime, money } from '../utils/format';

export default function InvoicePage() {
  const { id } = useParams();
  const state = useAsync(() => api.get<Invoice>(`/api/v1/admin/orders/${id}/invoice`), [id]);
  return (
    <div className="invoice-wrap">
      <Loadable state={state}>{(inv) => (
        <div className="invoice">
          <div className="no-print actions"><button className="btn primary" onClick={() => window.print()}>Print / Save as PDF</button></div>
          <header>
            <div><h1>{inv.storeName}</h1><p>{inv.storeAddress}</p>{inv.gstin && <p>GSTIN: {inv.gstin}</p>}{inv.supportPhone && <p>Support: {inv.supportPhone}</p>}</div>
            <div className="right"><h2>TAX INVOICE</h2><p>Invoice #: <strong>{inv.invoiceNumber}</strong></p><p>Order #: {inv.orderNumber}</p><p>Date: {dateTime(inv.invoiceDate)}</p></div>
          </header>
          <section><strong>Bill to / Ship to</strong><p>{inv.customerName} · {inv.customerPhone}<br />{inv.shippingAddress}</p></section>
          <table className="table">
            <thead><tr><th>#</th><th>Item</th><th>SKU</th><th className="num">Qty</th><th className="num">Rate</th><th className="num">GST %</th><th className="num">Amount</th></tr></thead>
            <tbody>{inv.items.map((i, n) => (
              <tr key={i.variantId}><td>{n + 1}</td><td>{i.productName} – {i.variantLabel}</td><td>{i.sku}</td><td className="num">{i.quantity}</td>
                <td className="num">{money(i.unitPrice)}</td><td className="num">{i.gstRate}</td><td className="num">{money(i.lineTotal)}</td></tr>
            ))}</tbody>
          </table>
          <div className="inv-bottom">
            <table className="table compact">
              <thead><tr><th>GST rate</th><th className="num">Taxable value</th><th className="num">CGST</th><th className="num">SGST</th></tr></thead>
              <tbody>{inv.taxes.map((t) => <tr key={t.gstRate}><td>{t.gstRate}%</td><td className="num">{money(t.taxableValue)}</td><td className="num">{money(t.cgst)}</td><td className="num">{money(t.sgst)}</td></tr>)}</tbody>
            </table>
            <table className="table compact totals"><tbody>
              <tr><td>Item total</td><td className="num">{money(inv.itemTotal)}</td></tr>
              {Number(inv.couponDiscount) > 0 && <tr><td>Coupon discount</td><td className="num">− {money(inv.couponDiscount)}</td></tr>}
              <tr><td>Delivery fee</td><td className="num">{money(inv.deliveryFee)}</td></tr>
              <tr><td>Handling fee</td><td className="num">{money(inv.handlingFee)}</td></tr>
              <tr className="total"><td>Grand total</td><td className="num">{money(inv.grandTotal)}</td></tr>
              <tr><td>Payment</td><td className="num">{inv.paymentMethod} ({inv.paymentStatus})</td></tr>
            </tbody></table>
          </div>
          <p className="muted small">Prices are inclusive of GST. This is a computer-generated invoice.</p>
        </div>
      )}</Loadable>
    </div>
  );
}
