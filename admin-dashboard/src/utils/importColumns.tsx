export const IMPORT_COLUMNS: [string, string][] = [
  ['product_name *', 'Product name'], ['category *', 'Top-level category'], ['subcategory', 'Subcategory'], ['brand', 'Brand (auto-created)'],
  ['description', 'Description'], ['sku *', 'Unique SKU'], ['barcode', '8–14 digit barcode'], ['variant_label *', 'e.g. 1 kg, 500 ml'],
  ['quantity_value *', 'Numeric quantity'], ['unit *', 'KG, G, L, ML or PCS'], ['mrp *', 'MRP in ₹'], ['selling_price *', 'Selling price in ₹ (≤ MRP)'],
  ['gst_rate', '0, 5, 12, 18 or 28'], ['hsn_code', 'HSN code'], ['stock', 'Units on hand'], ['low_stock_threshold', 'Alert level'],
  ['image_url', 'Image URL'], ['active', 'true / false'],
];
export function ImportColumns() {
  return (
    <details><summary>Column reference</summary>
      <table className="table compact"><tbody>{IMPORT_COLUMNS.map(([c, d]) => <tr key={c}><td><code>{c}</code></td><td>{d}</td></tr>)}</tbody></table>
    </details>
  );
}
