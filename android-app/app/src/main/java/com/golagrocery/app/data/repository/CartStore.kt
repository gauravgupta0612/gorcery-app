package com.golagrocery.app.data.repository

import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.local.SessionStore
import com.golagrocery.app.data.remote.ApiService
import com.golagrocery.app.data.remote.ApplyCoupon
import com.golagrocery.app.data.remote.Quote
import com.golagrocery.app.data.remote.SetQty
import com.golagrocery.app.data.safeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * App-wide cart state backed by the server cart (the server is the source of truth for prices,
 * stock and totals). Screens observe [quote] for badges and quantity steppers.
 */
class CartStore(private val api: ApiService, private val session: SessionStore) {
    private val _quote = MutableStateFlow<Quote?>(null)
    val quote: StateFlow<Quote?> = _quote.asStateFlow()
    private val _pending = MutableStateFlow<Set<Long>>(emptySet())
    val pending: StateFlow<Set<Long>> = _pending.asStateFlow()
    private val mutex = Mutex()

    private val pin get() = session.location.value?.pincode

    fun qty(variantId: Long): Int = _quote.value?.lines?.firstOrNull { it.variantId == variantId }?.quantity ?: 0

    suspend fun refresh(): ApiResult<Quote> {
        if (session.session.value == null) { _quote.value = null; return ApiResult.Err("Please log in", status = 401) }
        return safeCall { api.cart(pin) }.also { if (it is ApiResult.Ok) _quote.value = it.data }
    }

    suspend fun setQuantity(variantId: Long, quantity: Int): ApiResult<Quote> {
        _pending.value = _pending.value + variantId
        return try {
            mutex.withLock { safeCall { api.setCartItem(SetQty(variantId, quantity), pin) } }.also { if (it is ApiResult.Ok) _quote.value = it.data }
        } finally {
            _pending.value = _pending.value - variantId
        }
    }

    suspend fun applyCoupon(code: String) = safeCall { api.applyCoupon(ApplyCoupon(code.trim().uppercase()), pin) }.also { if (it is ApiResult.Ok) _quote.value = it.data }
    suspend fun removeCoupon() = safeCall { api.removeCoupon(pin) }.also { if (it is ApiResult.Ok) _quote.value = it.data }
    suspend fun coupons() = safeCall { api.coupons() }
    fun clearLocal() { _quote.value = null }
}
