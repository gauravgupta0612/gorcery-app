# Database (PostgreSQL)

| File | Purpose |
|---|---|
| `migrations/V1__schema.sql` | All tables, constraints, indexes, roles, default store settings |
| `migrations/V2__seed_catalog.sql` | **Generated** sample catalogue: 16 categories, 52 subcategories, 144 products, 164 variants/SKUs with stock, 74 brands, 3 suppliers, 4 banners, 3 coupons, 12 serviceable pincodes |
| `scripts/catalog_data.py` | Human-editable catalogue source |
| `scripts/generate_seed.py` | Regenerates V2 + the placeholder artwork in `seed-images/` (`python3 generate_seed.py`, needs Pillow) |
| `seed-images/` | Branded placeholder product/category/banner images served at `/files/catalog/*` |
| `scripts/create_database.sql`, `scripts/apply_migrations.sh` | One-time DB/role creation; optional manual migration |

Migrations are applied automatically by Flyway when the backend starts (the backend build copies this folder to `db/migration`).
**Never edit an applied migration** — add `V3__…sql` instead.

Tables: users, roles, user_roles, otp_codes, refresh_tokens, addresses, categories (self-referencing, unlimited depth), brands,
suppliers, products, product_variants (SKU, barcode, unit KG/G/L/ML/PCS, MRP, selling price), product_images, inventory,
inventory_movements, carts, cart_items, wishlist, coupons, coupon_redemptions, orders, order_items, order_status_history,
payments, refunds, delivery_partners, delivery_assignments, reviews, notifications, device_tokens, banners,
serviceable_pincodes, store_settings, support_tickets, audit_logs.

Sample prices are illustrative; barcodes are synthetic EAN-13 numbers (valid check digit, `890` prefix) for demo use.
Product images are generated placeholders — upload real photography from Admin → Products → Edit → Images.
