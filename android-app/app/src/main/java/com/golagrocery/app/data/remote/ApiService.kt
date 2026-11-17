package com.golagrocery.app.data.remote

import retrofit2.http.*

interface ApiService {
    // auth
    @POST("api/v1/auth/register") suspend fun register(@Body body: RegisterRequest): OtpSentResponse
    @POST("api/v1/auth/otp/request") suspend fun requestOtp(@Body body: OtpRequest): OtpSentResponse
    @POST("api/v1/auth/otp/verify") suspend fun verifyOtp(@Body body: VerifyOtpRequest): TokenResponse
    @POST("api/v1/auth/login") suspend fun login(@Body body: LoginRequest): TokenResponse
    @POST("api/v1/auth/logout") suspend fun logout(@Body body: RefreshRequest)

    // profile
    @GET("api/v1/me") suspend fun me(): UserDto
    @PUT("api/v1/me") suspend fun updateMe(@Body body: UpdateProfile): UserDto
    @GET("api/v1/me/addresses") suspend fun addresses(): List<AddressDto>
    @POST("api/v1/me/addresses") suspend fun addAddress(@Body body: AddressRequest): AddressDto
    @PUT("api/v1/me/addresses/{id}") suspend fun updateAddress(@Path("id") id: Long, @Body body: AddressRequest): AddressDto
    @DELETE("api/v1/me/addresses/{id}") suspend fun deleteAddress(@Path("id") id: Long)

    // store & catalog
    @GET("api/v1/store/settings") suspend fun settings(): PublicSettings
    @GET("api/v1/store/pincodes/{pincode}") suspend fun checkPincode(@Path("pincode") pincode: String): PincodeCheck
    @GET("api/v1/catalog/home") suspend fun home(): HomeDto
    @GET("api/v1/catalog/categories") suspend fun categories(): List<CategoryDto>
    @GET("api/v1/catalog/brands") suspend fun brands(@Query("categoryId") categoryId: Long?): List<BrandDto>
    @GET("api/v1/catalog/products") suspend fun products(
        @Query("q") q: String? = null, @Query("categoryId") categoryId: Long? = null, @Query("brandIds") brandIds: List<Long>? = null,
        @Query("minPrice") minPrice: Double? = null, @Query("maxPrice") maxPrice: Double? = null, @Query("inStock") inStock: Boolean? = null,
        @Query("discounted") discounted: Boolean? = null, @Query("sort") sort: String = "relevance", @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageDto<ProductCardDto>
    @GET("api/v1/catalog/products/{id}") suspend fun product(@Path("id") id: Long): ProductDetailDto
    @GET("api/v1/catalog/products/{id}/reviews") suspend fun reviews(@Path("id") id: Long, @Query("page") page: Int = 0): PageDto<ReviewDto>
    @POST("api/v1/reviews") suspend fun createReview(@Body body: CreateReview): ReviewDto

    // cart & wishlist
    @GET("api/v1/cart") suspend fun cart(@Query("pincode") pincode: String?): Quote
    @PUT("api/v1/cart/items") suspend fun setCartItem(@Body body: SetQty, @Query("pincode") pincode: String?): Quote
    @DELETE("api/v1/cart") suspend fun clearCart(@Query("pincode") pincode: String?): Quote
    @POST("api/v1/cart/coupon") suspend fun applyCoupon(@Body body: ApplyCoupon, @Query("pincode") pincode: String?): Quote
    @DELETE("api/v1/cart/coupon") suspend fun removeCoupon(@Query("pincode") pincode: String?): Quote
    @GET("api/v1/cart/coupons") suspend fun coupons(): List<CouponDto>
    @GET("api/v1/wishlist") suspend fun wishlist(): List<ProductCardDto>
    @POST("api/v1/wishlist/{productId}") suspend fun addToWishlist(@Path("productId") productId: Long): WishlistState
    @DELETE("api/v1/wishlist/{productId}") suspend fun removeFromWishlist(@Path("productId") productId: Long): WishlistState

    // orders & payments
    @POST("api/v1/orders/checkout") suspend fun checkout(@Body body: CheckoutRequest): CheckoutResponse
    @GET("api/v1/orders") suspend fun orders(@Query("page") page: Int = 0, @Query("size") size: Int = 20): PageDto<OrderSummaryDto>
    @GET("api/v1/orders/{id}") suspend fun order(@Path("id") id: Long): OrderDetailDto
    @POST("api/v1/orders/{id}/cancel") suspend fun cancelOrder(@Path("id") id: Long, @Body body: CancelRequest): OrderDetailDto
    @POST("api/v1/orders/{id}/reorder") suspend fun reorder(@Path("id") id: Long): ReorderResponse
    @POST("api/v1/orders/{id}/retry-payment") suspend fun retryPayment(@Path("id") id: Long): PaymentInit
    @POST("api/v1/payments/razorpay/verify") suspend fun verifyPayment(@Body body: VerifyPayment): OrderDetailDto
    @POST("api/v1/payments/mock/complete") suspend fun completeMockPayment(@Body body: MockComplete): OrderDetailDto
    @POST("api/v1/payments/failure") suspend fun reportPaymentFailure(@Body body: PaymentFailure): OrderDetailDto

    // notifications & support
    @GET("api/v1/notifications") suspend fun notifications(@Query("page") page: Int = 0): PageDto<NotificationDto>
    @GET("api/v1/notifications/unread-count") suspend fun unreadCount(): UnreadCount
    @POST("api/v1/notifications/{id}/read") suspend fun markRead(@Path("id") id: Long)
    @POST("api/v1/notifications/read-all") suspend fun markAllRead(): Map<String, Int>
    @POST("api/v1/devices") suspend fun registerDevice(@Body body: RegisterDevice)
    @GET("api/v1/support/contact") suspend fun supportContact(): Map<String, String>
    @GET("api/v1/support/tickets") suspend fun tickets(): List<TicketDto>
    @POST("api/v1/support/tickets") suspend fun createTicket(@Body body: CreateTicket): TicketDto

    // delivery partner
    @GET("api/v1/delivery/me") suspend fun partnerMe(): PartnerProfile
    @PUT("api/v1/delivery/me/availability") suspend fun setAvailability(@Body body: Availability): PartnerProfile
    @PUT("api/v1/delivery/me/location") suspend fun updateLocation(@Body body: LocationUpdate)
    @GET("api/v1/delivery/assignments") suspend fun assignments(): List<AssignmentDto>
    @GET("api/v1/delivery/assignments/history") suspend fun deliveryHistory(@Query("page") page: Int = 0): PageDto<AssignmentDto>
    @GET("api/v1/delivery/assignments/{id}") suspend fun assignment(@Path("id") id: Long): AssignmentDto
    @POST("api/v1/delivery/assignments/{id}/pickup") suspend fun pickup(@Path("id") id: Long): AssignmentDto
    @POST("api/v1/delivery/assignments/{id}/out-for-delivery") suspend fun outForDelivery(@Path("id") id: Long): AssignmentDto
    @POST("api/v1/delivery/assignments/{id}/deliver") suspend fun deliver(@Path("id") id: Long, @Body body: DeliverRequest): AssignmentDto
}
