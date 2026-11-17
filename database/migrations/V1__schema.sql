-- =====================================================================
-- Gola Grocery - core schema (PostgreSQL 14+)
-- =====================================================================

CREATE TABLE roles (
    id          SMALLSERIAL PRIMARY KEY,
    name        VARCHAR(32) NOT NULL UNIQUE           -- CUSTOMER, ADMIN, DELIVERY_PARTNER, STORE_MANAGER
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(15)  NOT NULL UNIQUE,
    email           VARCHAR(160) UNIQUE,
    password_hash   VARCHAR(100),
    phone_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    preferred_language VARCHAR(5) NOT NULL DEFAULT 'en',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id BIGINT   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE otp_codes (
    id          BIGSERIAL PRIMARY KEY,
    phone       VARCHAR(15)  NOT NULL,
    code_hash   VARCHAR(100) NOT NULL,
    purpose     VARCHAR(20)  NOT NULL,               -- REGISTER, LOGIN
    attempts    INT          NOT NULL DEFAULT 0,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_otp_phone ON otp_codes(phone, purpose, created_at DESC);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(100) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE addresses (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label         VARCHAR(20)  NOT NULL DEFAULT 'HOME',   -- HOME, WORK, OTHER
    contact_name  VARCHAR(120) NOT NULL,
    contact_phone VARCHAR(15)  NOT NULL,
    line1         VARCHAR(200) NOT NULL,
    line2         VARCHAR(200),
    landmark      VARCHAR(120),
    city          VARCHAR(80)  NOT NULL,
    state         VARCHAR(80)  NOT NULL,
    pincode       VARCHAR(6)   NOT NULL,
    latitude      DOUBLE PRECISION,
    longitude     DOUBLE PRECISION,
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_addresses_user ON addresses(user_id);

-- ---------------------------------------------------------------- catalog
CREATE TABLE brands (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL UNIQUE,
    logo_url  VARCHAR(300),
    active    BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT REFERENCES categories(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    name_hi     VARCHAR(100),
    slug        VARCHAR(120) NOT NULL UNIQUE,
    image_url   VARCHAR(300),
    sort_order  INT     NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_categories_parent ON categories(parent_id);

CREATE TABLE suppliers (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    contact_name  VARCHAR(120),
    phone         VARCHAR(15),
    email         VARCHAR(160),
    gstin         VARCHAR(15),
    address       VARCHAR(300),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(160) NOT NULL,
    name_hi       VARCHAR(160),
    slug          VARCHAR(180) NOT NULL UNIQUE,
    description   TEXT,
    brand_id      BIGINT REFERENCES brands(id),
    category_id   BIGINT NOT NULL REFERENCES categories(id),
    supplier_id   BIGINT REFERENCES suppliers(id),
    hsn_code      VARCHAR(10),
    gst_rate      NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (gst_rate IN (0, 5, 12, 18, 28)),
    tax_inclusive BOOLEAN NOT NULL DEFAULT TRUE,
    is_featured   BOOLEAN NOT NULL DEFAULT FALSE,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    avg_rating    NUMERIC(3,2) NOT NULL DEFAULT 0,
    rating_count  INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand_id);
CREATE INDEX idx_products_search ON products USING gin (to_tsvector('simple', name || ' ' || coalesce(description, '')));

CREATE TABLE product_variants (
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku            VARCHAR(40) NOT NULL UNIQUE,
    barcode        VARCHAR(20) UNIQUE,
    label          VARCHAR(40) NOT NULL,                 -- e.g. "1 kg", "500 ml", "Pack of 6"
    quantity_value NUMERIC(10,3) NOT NULL CHECK (quantity_value > 0),
    unit           VARCHAR(5)  NOT NULL CHECK (unit IN ('KG','G','L','ML','PCS')),
    mrp            NUMERIC(10,2) NOT NULL CHECK (mrp > 0),
    selling_price  NUMERIC(10,2) NOT NULL CHECK (selling_price > 0),
    max_per_order  INT NOT NULL DEFAULT 10,
    is_default     BOOLEAN NOT NULL DEFAULT FALSE,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_price_le_mrp CHECK (selling_price <= mrp)
);
CREATE INDEX idx_variants_product ON product_variants(product_id);

CREATE TABLE product_images (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url         VARCHAR(300) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_images_product ON product_images(product_id);

CREATE TABLE inventory (
    variant_id          BIGINT PRIMARY KEY REFERENCES product_variants(id) ON DELETE CASCADE,
    quantity            INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved            INT NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    low_stock_threshold INT NOT NULL DEFAULT 10,
    version             BIGINT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE inventory_movements (
    id          BIGSERIAL PRIMARY KEY,
    variant_id  BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    delta       INT NOT NULL,
    reason      VARCHAR(30) NOT NULL,       -- RESTOCK, ORDER, CANCEL, ADJUSTMENT, RETURN, IMPORT
    reference   VARCHAR(60),
    created_by  BIGINT REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- cart / wishlist
CREATE TABLE carts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    coupon_code VARCHAR(30),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id          BIGSERIAL PRIMARY KEY,
    cart_id     BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    variant_id  BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    quantity    INT NOT NULL CHECK (quantity > 0),
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (cart_id, variant_id)
);

CREATE TABLE wishlist (
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, product_id)
);

-- ---------------------------------------------------------------- coupons
CREATE TABLE coupons (
    id                BIGSERIAL PRIMARY KEY,
    code              VARCHAR(30) NOT NULL UNIQUE,
    description       VARCHAR(200),
    discount_type     VARCHAR(10) NOT NULL CHECK (discount_type IN ('PERCENT','FLAT')),
    discount_value    NUMERIC(10,2) NOT NULL CHECK (discount_value > 0),
    max_discount      NUMERIC(10,2),
    min_order_amount  NUMERIC(10,2) NOT NULL DEFAULT 0,
    usage_limit       INT,
    per_user_limit    INT NOT NULL DEFAULT 1,
    used_count        INT NOT NULL DEFAULT 0,
    valid_from        TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_to          TIMESTAMPTZ,
    first_order_only  BOOLEAN NOT NULL DEFAULT FALSE,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- delivery partners
CREATE TABLE delivery_partners (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    vehicle_type    VARCHAR(20) NOT NULL DEFAULT 'BIKE',
    vehicle_number  VARCHAR(20),
    license_number  VARCHAR(30),
    available       BOOLEAN NOT NULL DEFAULT TRUE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    current_lat     DOUBLE PRECISION,
    current_lng     DOUBLE PRECISION,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- orders
CREATE TABLE orders (
    id                BIGSERIAL PRIMARY KEY,
    order_number      VARCHAR(20) NOT NULL UNIQUE,
    user_id           BIGINT NOT NULL REFERENCES users(id),
    status            VARCHAR(20) NOT NULL CHECK (status IN
                        ('PENDING_PAYMENT','PLACED','CONFIRMED','PREPARING','PACKED','ASSIGNED',
                         'OUT_FOR_DELIVERY','DELIVERED','CANCELLED','PAYMENT_FAILED')),
    payment_method    VARCHAR(10) NOT NULL CHECK (payment_method IN ('UPI','CARD','NETBANKING','COD')),
    payment_status    VARCHAR(12) NOT NULL CHECK (payment_status IN ('PENDING','PAID','FAILED','REFUNDED','PARTIALLY_REFUNDED','COD_PENDING')),
    -- address snapshot
    ship_name         VARCHAR(120) NOT NULL,
    ship_phone        VARCHAR(15)  NOT NULL,
    ship_line1        VARCHAR(200) NOT NULL,
    ship_line2        VARCHAR(200),
    ship_landmark     VARCHAR(120),
    ship_city         VARCHAR(80)  NOT NULL,
    ship_state        VARCHAR(80)  NOT NULL,
    ship_pincode      VARCHAR(6)   NOT NULL,
    ship_lat          DOUBLE PRECISION,
    ship_lng          DOUBLE PRECISION,
    -- amounts
    item_total        NUMERIC(10,2) NOT NULL,     -- sum of selling price * qty
    mrp_total         NUMERIC(10,2) NOT NULL,
    coupon_code       VARCHAR(30),
    coupon_discount   NUMERIC(10,2) NOT NULL DEFAULT 0,
    delivery_fee      NUMERIC(10,2) NOT NULL DEFAULT 0,
    handling_fee      NUMERIC(10,2) NOT NULL DEFAULT 0,
    tax_total         NUMERIC(10,2) NOT NULL DEFAULT 0,   -- GST portion included in prices
    grand_total       NUMERIC(10,2) NOT NULL,
    delivery_instructions VARCHAR(300),
    cancel_reason     VARCHAR(300),
    delivery_otp      VARCHAR(6),           -- shown to the customer, verified server-side at hand-over
    placed_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_at      TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_user ON orders(user_id, placed_at DESC);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id     BIGINT NOT NULL REFERENCES products(id),
    variant_id     BIGINT NOT NULL REFERENCES product_variants(id),
    product_name   VARCHAR(160) NOT NULL,
    variant_label  VARCHAR(40)  NOT NULL,
    image_url      VARCHAR(300),
    sku            VARCHAR(40)  NOT NULL,
    unit_mrp       NUMERIC(10,2) NOT NULL,
    unit_price     NUMERIC(10,2) NOT NULL,
    quantity       INT NOT NULL CHECK (quantity > 0),
    gst_rate       NUMERIC(5,2) NOT NULL,
    tax_amount     NUMERIC(10,2) NOT NULL,
    line_total     NUMERIC(10,2) NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items(order_id);

CREATE TABLE order_status_history (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL,
    note        VARCHAR(300),
    changed_by  BIGINT REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_osh_order ON order_status_history(order_id);

CREATE TABLE coupon_redemptions (
    id          BIGSERIAL PRIMARY KEY,
    coupon_id   BIGINT NOT NULL REFERENCES coupons(id),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount      NUMERIC(10,2) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payments (
    id                   BIGSERIAL PRIMARY KEY,
    order_id             BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    provider             VARCHAR(20) NOT NULL,         -- RAZORPAY, COD, MOCK
    method               VARCHAR(10) NOT NULL,
    amount               NUMERIC(10,2) NOT NULL,
    currency             VARCHAR(3) NOT NULL DEFAULT 'INR',
    status               VARCHAR(12) NOT NULL CHECK (status IN ('CREATED','PAID','FAILED','REFUNDED','COD_PENDING','COD_COLLECTED')),
    provider_order_id    VARCHAR(60),
    provider_payment_id  VARCHAR(60),
    provider_signature   VARCHAR(200),
    failure_reason       VARCHAR(300),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE UNIQUE INDEX uq_payments_provider_order ON payments(provider_order_id) WHERE provider_order_id IS NOT NULL;

CREATE TABLE refunds (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_id         BIGINT REFERENCES payments(id),
    amount             NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    reason             VARCHAR(300) NOT NULL,
    status             VARCHAR(12) NOT NULL CHECK (status IN ('REQUESTED','APPROVED','PROCESSED','REJECTED','FAILED')),
    provider_refund_id VARCHAR(60),
    requested_by       BIGINT REFERENCES users(id),
    processed_by       BIGINT REFERENCES users(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE delivery_assignments (
    id                    BIGSERIAL PRIMARY KEY,
    order_id              BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    delivery_partner_id   BIGINT NOT NULL REFERENCES delivery_partners(id),
    status                VARCHAR(20) NOT NULL CHECK (status IN ('ASSIGNED','PICKED_UP','OUT_FOR_DELIVERY','DELIVERED','CANCELLED')),
    assigned_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    picked_up_at          TIMESTAMPTZ,
    delivered_at          TIMESTAMPTZ,
    cod_collected         NUMERIC(10,2)
);
CREATE INDEX idx_da_partner ON delivery_assignments(delivery_partner_id, status);
CREATE UNIQUE INDEX uq_da_active_order ON delivery_assignments(order_id) WHERE status <> 'CANCELLED';

-- ---------------------------------------------------------------- reviews
CREATE TABLE reviews (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_id    BIGINT REFERENCES orders(id),
    rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title       VARCHAR(120),
    comment     VARCHAR(1000),
    approved    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, user_id)
);

-- ---------------------------------------------------------------- notifications
CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id) ON DELETE CASCADE,   -- NULL = broadcast
    title       VARCHAR(150) NOT NULL,
    body        VARCHAR(500) NOT NULL,
    type        VARCHAR(20) NOT NULL DEFAULT 'GENERAL',           -- ORDER, OFFER, GENERAL
    reference   VARCHAR(60),
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);

CREATE TABLE device_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(300) NOT NULL UNIQUE,
    platform    VARCHAR(10) NOT NULL DEFAULT 'ANDROID',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- store
CREATE TABLE banners (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(120) NOT NULL,
    subtitle    VARCHAR(200),
    image_url   VARCHAR(300) NOT NULL,
    link_type   VARCHAR(12) NOT NULL DEFAULT 'NONE' CHECK (link_type IN ('NONE','CATEGORY','PRODUCT','COUPON')),
    link_value  VARCHAR(60),
    bg_color    VARCHAR(9) DEFAULT '#0C831F',
    sort_order  INT NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at   TIMESTAMPTZ,
    ends_at     TIMESTAMPTZ
);

CREATE TABLE serviceable_pincodes (
    pincode           VARCHAR(6) PRIMARY KEY,
    city              VARCHAR(80) NOT NULL,
    area              VARCHAR(120),
    delivery_fee      NUMERIC(10,2),          -- override; NULL = use store default
    eta_minutes       INT NOT NULL DEFAULT 30,
    active            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE store_settings (
    id                        SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    store_name                VARCHAR(100) NOT NULL DEFAULT 'Gola Grocery',
    tagline                   VARCHAR(150) NOT NULL DEFAULT 'Freshness Delivered to Your Doorstep.',
    support_phone             VARCHAR(15),
    support_email             VARCHAR(160),
    gstin                     VARCHAR(15),
    store_address             VARCHAR(300),
    delivery_fee              NUMERIC(10,2) NOT NULL DEFAULT 25,
    free_delivery_above       NUMERIC(10,2) NOT NULL DEFAULT 299,
    handling_fee              NUMERIC(10,2) NOT NULL DEFAULT 4,
    min_order_amount          NUMERIC(10,2) NOT NULL DEFAULT 99,
    cod_enabled               BOOLEAN NOT NULL DEFAULT TRUE,
    cod_max_amount            NUMERIC(10,2) NOT NULL DEFAULT 3000,
    store_open                BOOLEAN NOT NULL DEFAULT TRUE,
    opening_time              TIME NOT NULL DEFAULT '06:00',
    closing_time              TIME NOT NULL DEFAULT '23:30',
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE support_tickets (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_id    BIGINT REFERENCES orders(id),
    subject     VARCHAR(150) NOT NULL,
    message     VARCHAR(2000) NOT NULL,
    status      VARCHAR(12) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),
    admin_reply VARCHAR(2000),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    actor_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    actor_role   VARCHAR(32),
    action       VARCHAR(60) NOT NULL,
    entity_type  VARCHAR(40),
    entity_id    VARCHAR(40),
    details      TEXT,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);

CREATE SEQUENCE order_number_seq START 100001;

INSERT INTO roles(name) VALUES ('CUSTOMER'), ('ADMIN'), ('DELIVERY_PARTNER'), ('STORE_MANAGER');
INSERT INTO store_settings(id, support_phone, support_email, store_address)
VALUES (1, '1800-000-0000', 'support@golagrocery.example', 'Gola Grocery Dark Store, Sector 18, Noida, Uttar Pradesh 201301');
