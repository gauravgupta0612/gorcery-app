package com.golagrocery.app.data.remote

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------- common / auth
@Serializable data class ErrorBody(val code: String = "", val message: String = "", val fields: Map<String, String>? = null)
@Serializable data class PageDto<T>(val items: List<T> = emptyList(), val page: Int = 0, val size: Int = 0, val totalItems: Long = 0, val totalPages: Int = 0)

@Serializable data class UserDto(
    val id: Long, val fullName: String, val phone: String, val email: String? = null,
    val roles: List<String> = emptyList(), val preferredLanguage: String = "en",
) {
    val isDeliveryPartner get() = "DELIVERY_PARTNER" in roles
    val isCustomer get() = "CUSTOMER" in roles
}
@Serializable data class TokenResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long = 0, val user: UserDto)
@Serializable data class OtpSentResponse(val otpSent: Boolean = false, val phone: String = "", val devOtp: String? = null, val sentTo: String? = null)
@Serializable data class RegisterRequest(val fullName: String, val phone: String, val email: String?, val password: String)
@Serializable data class OtpRequest(val phone: String, val purpose: String)
@Serializable data class VerifyOtpRequest(val phone: String, val code: String, val purpose: String)
@Serializable data class LoginRequest(val identifier: String, val password: String)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class UpdateProfile(val fullName: String, val email: String?, val preferredLanguage: String?)

// ---------------------------------------------------------------- catalog
@Serializable data class VariantDto(
    val id: Long, val sku: String = "", val label: String, val mrp: Double, val price: Double, val discountPercent: Int = 0,
    val inStock: Boolean = true, val availableQty: Int = 0, val maxPerOrder: Int = 10, val unit: String = "PCS",
)
@Serializable data class ProductCardDto(
    val id: Long, val name: String, val nameHi: String? = null, val slug: String = "", val brand: String? = null,
    val imageUrl: String? = null, val categoryId: Long = 0, val avgRating: Double = 0.0, val ratingCount: Int = 0,
    val variants: List<VariantDto> = emptyList(),
)
@Serializable data class ProductDetailDto(
    val id: Long, val name: String, val nameHi: String? = null, val slug: String = "", val description: String? = null,
    val brand: String? = null, val categoryId: Long = 0, val categoryName: String = "", val parentCategoryId: Long? = null,
    val gstRate: Double = 0.0, val avgRating: Double = 0.0, val ratingCount: Int = 0, val images: List<String> = emptyList(),
    val variants: List<VariantDto> = emptyList(), val inWishlist: Boolean = false, val similar: List<ProductCardDto> = emptyList(),
)
@Serializable data class CategoryDto(
    val id: Long, val name: String, val nameHi: String? = null, val slug: String = "", val imageUrl: String? = null,
    val parentId: Long? = null, val children: List<CategoryDto> = emptyList(),
)
@Serializable data class BannerDto(
    val id: Long, val title: String, val subtitle: String? = null, val imageUrl: String, val linkType: String = "NONE",
    val linkValue: String? = null, val bgColor: String? = null,
)
@Serializable data class HomeSection(val title: String, val categoryId: Long, val products: List<ProductCardDto> = emptyList())
@Serializable data class HomeDto(
    val banners: List<BannerDto> = emptyList(), val categories: List<CategoryDto> = emptyList(),
    val featured: List<ProductCardDto> = emptyList(), val deals: List<ProductCardDto> = emptyList(), val sections: List<HomeSection> = emptyList(),
)
@Serializable data class BrandDto(val id: Long, val name: String)
@Serializable data class ReviewDto(val id: Long, val productId: Long, val userName: String = "", val rating: Int, val title: String? = null, val comment: String? = null, val createdAt: String = "")
@Serializable data class CreateReview(val productId: Long, val rating: Int, val title: String?, val comment: String?)
@Serializable data class PincodeCheck(val pincode: String, val serviceable: Boolean, val city: String? = null, val area: String? = null, val etaMinutes: Int? = null, val message: String? = null)
@Serializable data class PublicSettings(
    val storeName: String = "Gola Grocery", val tagline: String = "", val supportPhone: String? = null, val supportEmail: String? = null,
    val deliveryFee: Double = 0.0, val freeDeliveryAbove: Double = 0.0, val handlingFee: Double = 0.0, val minOrderAmount: Double = 0.0,
    val codEnabled: Boolean = true, val codMaxAmount: Double = 0.0, val openNow: Boolean = true, val openingTime: String = "", val closingTime: String = "",
)

// ---------------------------------------------------------------- cart
@Serializable data class QuoteLine(
    val variantId: Long, val productId: Long, val productName: String, val variantLabel: String, val imageUrl: String? = null,
    val unitMrp: Double, val unitPrice: Double, val quantity: Int, val lineTotal: Double, val lineMrp: Double = 0.0,
    val gstRate: Double = 0.0, val tax: Double = 0.0, val availableQty: Int = 0, val maxPerOrder: Int = 10, val problem: String? = null,
)
@Serializable data class Quote(
    val lines: List<QuoteLine> = emptyList(), val itemTotal: Double = 0.0, val mrpTotal: Double = 0.0, val productSavings: Double = 0.0,
    val couponCode: String? = null, val couponValid: Boolean = false, val couponMessage: String? = null, val couponDiscount: Double = 0.0,
    val deliveryFee: Double = 0.0, val handlingFee: Double = 0.0, val taxTotal: Double = 0.0, val grandTotal: Double = 0.0,
    val minOrderAmount: Double = 0.0, val meetsMinimum: Boolean = true, val freeDeliveryAbove: Double = 0.0,
    val amountForFreeDelivery: Double = 0.0, val hasProblems: Boolean = false,
) {
    val itemCount get() = lines.sumOf { it.quantity }
}
@Serializable data class SetQty(val variantId: Long, val quantity: Int)
@Serializable data class ApplyCoupon(val code: String)
@Serializable data class CouponDto(
    val code: String, val description: String? = null, val discountType: String = "FLAT", val discountValue: Double = 0.0,
    val maxDiscount: Double? = null, val minOrderAmount: Double = 0.0, val applicable: Boolean = false, val reason: String? = null,
)

// ---------------------------------------------------------------- addresses
@Serializable data class AddressDto(
    val id: Long, val label: String = "HOME", val contactName: String, val contactPhone: String, val line1: String,
    val line2: String? = null, val landmark: String? = null, val city: String, val state: String, val pincode: String,
    val latitude: Double? = null, val longitude: Double? = null, val isDefault: Boolean = false,
) {
    fun oneLine() = listOfNotNull(line1, line2?.takeIf { it.isNotBlank() }, landmark?.takeIf { it.isNotBlank() }, city, "$state - $pincode").joinToString(", ")
}
@Serializable data class AddressRequest(
    val label: String, val contactName: String, val contactPhone: String, val line1: String, val line2: String?,
    val landmark: String?, val city: String, val state: String, val pincode: String, val latitude: Double?, val longitude: Double?,
    val isDefault: Boolean,
)

// ---------------------------------------------------------------- orders & payments
@Serializable data class CheckoutRequest(val addressId: Long, val paymentMethod: String, val deliveryInstructions: String?)
@Serializable data class PaymentInit(
    val provider: String, val keyId: String = "", val providerOrderId: String, val amountPaise: Long, val currency: String = "INR",
    val orderNumber: String, val testMode: Boolean = false, val customerName: String = "", val customerPhone: String = "", val customerEmail: String? = null,
)
@Serializable data class OrderItemDto(
    val productId: Long, val variantId: Long, val productName: String, val variantLabel: String, val imageUrl: String? = null,
    val sku: String = "", val unitMrp: Double = 0.0, val unitPrice: Double, val quantity: Int, val lineTotal: Double,
    val gstRate: Double = 0.0, val taxAmount: Double = 0.0,
)
@Serializable data class AddressSnapshot(
    val name: String, val phone: String, val line1: String, val line2: String? = null, val landmark: String? = null,
    val city: String, val state: String, val pincode: String, val lat: Double? = null, val lng: Double? = null,
) {
    fun oneLine() = listOfNotNull(line1, line2?.takeIf { it.isNotBlank() }, landmark?.takeIf { it.isNotBlank() }, city, "$state - $pincode").joinToString(", ")
}
@Serializable data class HistoryDto(val status: String, val note: String? = null, val at: String)
@Serializable data class PartnerDto(val id: Long, val name: String, val phone: String, val vehicleType: String = "", val vehicleNumber: String? = null)
@Serializable data class RefundDto(val id: Long, val amount: Double, val reason: String, val status: String, val createdAt: String = "")
@Serializable data class OrderSummaryDto(
    val id: Long, val orderNumber: String, val status: String, val paymentMethod: String, val paymentStatus: String,
    val grandTotal: Double, val itemCount: Int, val placedAt: String, val images: List<String> = emptyList(),
)
@Serializable data class OrderDetailDto(
    val id: Long, val orderNumber: String, val status: String, val paymentMethod: String, val paymentStatus: String,
    val paymentProvider: String? = null, val testPayment: Boolean = false, val items: List<OrderItemDto> = emptyList(),
    val address: AddressSnapshot, val itemTotal: Double, val mrpTotal: Double = 0.0, val couponCode: String? = null,
    val couponDiscount: Double = 0.0, val deliveryFee: Double = 0.0, val handlingFee: Double = 0.0, val taxTotal: Double = 0.0,
    val grandTotal: Double, val deliveryInstructions: String? = null, val cancelReason: String? = null, val deliveryOtp: String? = null,
    val placedAt: String, val deliveredAt: String? = null, val history: List<HistoryDto> = emptyList(),
    val deliveryPartner: PartnerDto? = null, val canCancel: Boolean = false, val canRetryPayment: Boolean = false,
    val canReview: Boolean = false, val refunds: List<RefundDto> = emptyList(),
)
@Serializable data class CheckoutResponse(val order: OrderDetailDto, val payment: PaymentInit? = null)
@Serializable data class ReorderResponse(val added: Int, val skipped: List<String> = emptyList())
@Serializable data class CancelRequest(val reason: String)
@Serializable data class VerifyPayment(val orderId: Long, val razorpayOrderId: String, val razorpayPaymentId: String, val razorpaySignature: String)
@Serializable data class MockComplete(val orderId: Long, val success: Boolean)
@Serializable data class PaymentFailure(val orderId: Long, val reason: String?)

// ---------------------------------------------------------------- notifications & support
@Serializable data class NotificationDto(val id: Long, val title: String, val body: String, val type: String = "GENERAL", val reference: String? = null, val read: Boolean = false, val createdAt: String = "")
@Serializable data class RegisterDevice(val token: String)
@Serializable data class UnreadCount(val count: Long = 0)
@Serializable data class TicketDto(val id: Long, val orderId: Long? = null, val subject: String, val message: String, val status: String, val adminReply: String? = null, val createdAt: String = "", val updatedAt: String = "")
@Serializable data class CreateTicket(val orderId: Long?, val subject: String, val message: String)
@Serializable data class WishlistState(val inWishlist: Boolean = false)

// ---------------------------------------------------------------- delivery partner
@Serializable data class PartnerProfile(val id: Long, val name: String, val phone: String, val vehicleType: String = "", val vehicleNumber: String? = null, val available: Boolean = false, val activeCount: Long = 0, val deliveredCount: Long = 0)
@Serializable data class AssignmentDto(
    val assignmentId: Long, val assignmentStatus: String, val assignedAt: String = "", val pickedUpAt: String? = null, val deliveredAt: String? = null,
    val orderId: Long, val orderNumber: String, val orderStatus: String, val paymentMethod: String, val paymentStatus: String,
    val amountToCollect: Double = 0.0, val grandTotal: Double = 0.0, val itemCount: Int = 0, val items: List<OrderItemDto> = emptyList(),
    val address: AddressSnapshot, val deliveryInstructions: String? = null, val navigationUrl: String = "",
)
@Serializable data class Availability(val available: Boolean)
@Serializable data class LocationUpdate(val lat: Double, val lng: Double)
@Serializable data class DeliverRequest(val otp: String, val codCollected: Double?)
