package com.golagrocery.app.data.repository

import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.local.DeliveryLocation
import com.golagrocery.app.data.local.Session
import com.golagrocery.app.data.local.SessionStore
import com.golagrocery.app.data.remote.*
import com.golagrocery.app.data.safeCall

/** Authentication & profile. */
class AuthRepository(private val api: ApiService, private val session: SessionStore) {
    suspend fun register(name: String, phone: String, email: String?, password: String) =
        safeCall { api.register(RegisterRequest(name.trim(), phone, email?.trim()?.ifBlank { null }, password)) }
    suspend fun requestOtp(phone: String, purpose: String) = safeCall { api.requestOtp(OtpRequest(phone, purpose)) }
    suspend fun verifyOtp(phone: String, code: String, purpose: String): ApiResult<UserDto> =
        persist(safeCall { api.verifyOtp(VerifyOtpRequest(phone, code, purpose)) })
    suspend fun login(identifier: String, password: String): ApiResult<UserDto> =
        persist(safeCall { api.login(LoginRequest(identifier.trim(), password)) })

    private suspend fun persist(r: ApiResult<TokenResponse>): ApiResult<UserDto> = when (r) {
        is ApiResult.Ok -> {
            session.saveSession(Session(r.data.accessToken, r.data.refreshToken, r.data.user))
            ApiResult.Ok(r.data.user)
        }
        is ApiResult.Err -> r
    }

    suspend fun logout() {
        session.session.value?.let { s -> safeCall { api.logout(RefreshRequest(s.refreshToken)) } }
        session.clearSession()
    }

    suspend fun refreshProfile() = safeCall { api.me() }.also { if (it is ApiResult.Ok) session.updateUser(it.data) }
    suspend fun updateProfile(name: String, email: String?, lang: String?) =
        safeCall { api.updateMe(UpdateProfile(name.trim(), email?.trim()?.ifBlank { null }, lang)) }.also { if (it is ApiResult.Ok) session.updateUser(it.data) }
}

class CatalogRepository(private val api: ApiService) {
    suspend fun home() = safeCall { api.home() }
    suspend fun categories() = safeCall { api.categories() }
    suspend fun brands(categoryId: Long?) = safeCall { api.brands(categoryId) }
    suspend fun search(f: ProductFilter, page: Int) = safeCall {
        api.products(q = f.query?.ifBlank { null }, categoryId = f.categoryId, brandIds = f.brandIds.ifEmpty { null },
            minPrice = f.minPrice, maxPrice = f.maxPrice, inStock = if (f.inStockOnly) true else null,
            discounted = if (f.discountedOnly) true else null, sort = f.sort, page = page)
    }
    suspend fun product(id: Long) = safeCall { api.product(id) }
    suspend fun reviews(id: Long) = safeCall { api.reviews(id) }
    suspend fun createReview(productId: Long, rating: Int, title: String?, comment: String?) =
        safeCall { api.createReview(CreateReview(productId, rating, title?.ifBlank { null }, comment?.ifBlank { null })) }
    suspend fun wishlist() = safeCall { api.wishlist() }
    suspend fun setWishlisted(productId: Long, on: Boolean) =
        safeCall { if (on) api.addToWishlist(productId) else api.removeFromWishlist(productId) }
    suspend fun settings() = safeCall { api.settings() }
}

data class ProductFilter(
    val query: String? = null, val categoryId: Long? = null, val brandIds: List<Long> = emptyList(), val minPrice: Double? = null,
    val maxPrice: Double? = null, val inStockOnly: Boolean = false, val discountedOnly: Boolean = false, val sort: String = "relevance",
) {
    val activeCount get() = listOf(brandIds.isNotEmpty(), minPrice != null || maxPrice != null, inStockOnly, discountedOnly).count { it }
}

class LocationRepository(private val api: ApiService, private val session: SessionStore) {
    suspend fun check(pincode: String) = safeCall { api.checkPincode(pincode) }
    suspend fun select(check: PincodeCheck) {
        session.saveLocation(DeliveryLocation(check.pincode, listOfNotNull(check.area, check.city).joinToString(", ").ifBlank { check.pincode }, check.etaMinutes ?: 30))
    }
}

class AddressRepository(private val api: ApiService) {
    suspend fun list() = safeCall { api.addresses() }
    suspend fun save(id: Long?, req: AddressRequest) = safeCall { if (id == null) api.addAddress(req) else api.updateAddress(id, req) }
    suspend fun delete(id: Long) = safeCall { api.deleteAddress(id) }
}

class OrderRepository(private val api: ApiService) {
    suspend fun checkout(addressId: Long, method: String, instructions: String?) =
        safeCall { api.checkout(CheckoutRequest(addressId, method, instructions?.ifBlank { null })) }
    suspend fun orders(page: Int) = safeCall { api.orders(page) }
    suspend fun order(id: Long) = safeCall { api.order(id) }
    suspend fun cancel(id: Long, reason: String) = safeCall { api.cancelOrder(id, CancelRequest(reason)) }
    suspend fun reorder(id: Long) = safeCall { api.reorder(id) }
    suspend fun retryPayment(id: Long) = safeCall { api.retryPayment(id) }
    suspend fun verifyRazorpay(orderId: Long, rzpOrderId: String, paymentId: String, signature: String) =
        safeCall { api.verifyPayment(VerifyPayment(orderId, rzpOrderId, paymentId, signature)) }
    suspend fun completeMock(orderId: Long, success: Boolean) = safeCall { api.completeMockPayment(MockComplete(orderId, success)) }
    suspend fun reportFailure(orderId: Long, reason: String?) = safeCall { api.reportPaymentFailure(PaymentFailure(orderId, reason)) }
}

class EngagementRepository(private val api: ApiService) {
    suspend fun notifications() = safeCall { api.notifications() }
    suspend fun unreadCount() = safeCall { api.unreadCount() }
    suspend fun markRead(id: Long) = safeCall { api.markRead(id) }
    suspend fun markAllRead() = safeCall { api.markAllRead() }
    suspend fun registerDevice(token: String) = safeCall { api.registerDevice(RegisterDevice(token)) }
    suspend fun contact() = safeCall { api.supportContact() }
    suspend fun tickets() = safeCall { api.tickets() }
    suspend fun createTicket(orderId: Long?, subject: String, message: String) = safeCall { api.createTicket(CreateTicket(orderId, subject.trim(), message.trim())) }
}

class DeliveryRepository(private val api: ApiService) {
    suspend fun me() = safeCall { api.partnerMe() }
    suspend fun setAvailable(on: Boolean) = safeCall { api.setAvailability(Availability(on)) }
    suspend fun updateLocation(lat: Double, lng: Double) = safeCall { api.updateLocation(LocationUpdate(lat, lng)) }
    suspend fun active() = safeCall { api.assignments() }
    suspend fun history(page: Int) = safeCall { api.deliveryHistory(page) }
    suspend fun get(id: Long) = safeCall { api.assignment(id) }
    suspend fun pickup(id: Long) = safeCall { api.pickup(id) }
    suspend fun outForDelivery(id: Long) = safeCall { api.outForDelivery(id) }
    suspend fun deliver(id: Long, otp: String, cash: Double?) = safeCall { api.deliver(id, DeliverRequest(otp, cash)) }
}
