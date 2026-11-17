export interface Page<T> { items: T[]; page: number; size: number; totalItems: number; totalPages: number }

export interface Category { id: number; name: string; nameHi?: string; slug: string; imageUrl?: string; parentId?: number | null; children: Category[] }
export interface Brand { id: number; name: string; logoUrl?: string; active: boolean }
export interface Supplier { id: number; name: string; contactName?: string; phone?: string; email?: string; gstin?: string; address?: string; active: boolean }

export type Unit = 'KG' | 'G' | 'L' | 'ML' | 'PCS';
export interface AdminVariant {
  id?: number; sku: string; barcode?: string | null; label: string; quantityValue: number; unit: Unit; mrp: number; sellingPrice: number;
  discountPercent?: number; maxPerOrder: number; isDefault: boolean; active: boolean; stock: number; lowStockThreshold: number; stockStatus?: string;
}
export interface AdminProduct {
  id: number; name: string; nameHi?: string; slug: string; description?: string; brandId?: number; brandName?: string; categoryId: number;
  categoryName: string; supplierId?: number; hsnCode?: string; gstRate: number; isFeatured: boolean; active: boolean; avgRating: number;
  ratingCount: number; images: { id: number; url: string; sortOrder: number }[]; variants: AdminVariant[];
}

export interface OrderRow {
  id: number; orderNumber: string; customerName: string; customerPhone: string; pincode: string; status: string; paymentMethod: string;
  paymentStatus: string; grandTotal: number; itemCount: number; placedAt: string;
}
export interface OrderItem {
  productId: number; variantId: number; productName: string; variantLabel: string; imageUrl?: string; sku: string; unitMrp: number;
  unitPrice: number; quantity: number; lineTotal: number; gstRate: number; taxAmount: number;
}
export interface OrderDetail {
  id: number; orderNumber: string; status: string; paymentMethod: string; paymentStatus: string; paymentProvider?: string; testPayment: boolean;
  items: OrderItem[];
  address: { name: string; phone: string; line1: string; line2?: string; landmark?: string; city: string; state: string; pincode: string; lat?: number; lng?: number };
  itemTotal: number; mrpTotal: number; couponCode?: string; couponDiscount: number; deliveryFee: number; handlingFee: number; taxTotal: number;
  grandTotal: number; deliveryInstructions?: string; cancelReason?: string; placedAt: string; deliveredAt?: string;
  history: { status: string; note?: string; at: string }[];
  deliveryPartner?: { id: number; name: string; phone: string; vehicleType: string; vehicleNumber?: string };
  refunds: { id: number; amount: number; reason: string; status: string; createdAt: string }[];
  customerId?: number;
}
export interface Invoice {
  invoiceNumber: string; invoiceDate: string; storeName: string; storeAddress?: string; gstin?: string; supportPhone?: string; orderNumber: string;
  customerName: string; customerPhone: string; shippingAddress: string; paymentMethod: string; paymentStatus: string; items: OrderItem[];
  taxes: { gstRate: number; taxableValue: number; cgst: number; sgst: number }[]; itemTotal: number; couponDiscount: number;
  deliveryFee: number; handlingFee: number; grandTotal: number; status: string;
}
export interface Partner {
  id: number; userId: number; fullName: string; phone: string; email?: string; vehicleType: string; vehicleNumber?: string;
  licenseNumber?: string; available: boolean; active: boolean; activeOrders: number; delivered: number;
}
export interface Customer { id: number; fullName: string; phone: string; email?: string; enabled: boolean; verified: boolean; createdAt: string; orderCount: number; totalSpent: number }
export interface Coupon {
  id: number; code: string; description?: string; discountType: 'PERCENT' | 'FLAT'; discountValue: number; maxDiscount?: number | null;
  minOrderAmount: number; usageLimit?: number | null; perUserLimit: number; usedCount: number; validFrom?: string; validTo?: string | null;
  firstOrderOnly: boolean; active: boolean;
}
export interface Banner {
  id: number; title: string; subtitle?: string; imageUrl: string; linkType: string; linkValue?: string; bgColor?: string; sortOrder: number;
  active: boolean; startsAt?: string | null; endsAt?: string | null;
}
export interface Pincode { pincode: string; city: string; area?: string; deliveryFee?: number | null; etaMinutes: number; active: boolean }
export interface StoreSettings {
  storeName: string; tagline: string; supportPhone?: string; supportEmail?: string; gstin?: string; storeAddress?: string; deliveryFee: number;
  freeDeliveryAbove: number; handlingFee: number; minOrderAmount: number; codEnabled: boolean; codMaxAmount: number; storeOpen: boolean;
  openingTime: string; closingTime: string;
}
export interface InventoryRow {
  variantId: number; productId: number; productName: string; brand?: string; category: string; sku: string; barcode?: string; label: string;
  quantity: number; lowStockThreshold: number; status: string; active: boolean; sellingPrice: number;
}
export interface Named { name: string; count: number; amount: number }
export interface Dashboard {
  todayOrders: number; todayRevenue: number; pendingOrders: number; outForDelivery: number; totalCustomers: number; newCustomers7d: number;
  lowStockItems: number; openTickets: number; last14Days: { date: string; orders: number; revenue: number }[]; ordersByStatus: Named[];
  topProducts30d: Named[]; paymentSplit30d: Named[];
}
export interface SalesRow { date: string; orders: number; itemTotal: number; discounts: number; deliveryFees: number; handlingFees: number; tax: number; netRevenue: number; refunds: number }
export interface SalesReport { from: string; to: string; rows: SalesRow[]; totals: SalesRow; topProducts: Named[]; categorySales: Named[]; paymentSplit: Named[] }
export interface RefundRow { id: number; orderId: number; orderNumber: string; amount: number; reason: string; status: string; providerRefundId?: string; createdAt: string }
export interface Ticket { id: number; orderId?: number; subject: string; message: string; status: string; adminReply?: string; createdAt: string; updatedAt: string }
export interface AuditEntry { id: number; actorId?: number; actorRole?: string; action: string; entityType?: string; entityId?: string; details?: string; ipAddress?: string; createdAt: string }
