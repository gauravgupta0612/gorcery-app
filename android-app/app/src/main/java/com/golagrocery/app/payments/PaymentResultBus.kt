package com.golagrocery.app.payments

import android.app.Activity
import com.golagrocery.app.data.remote.PaymentInit
import com.razorpay.Checkout
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

sealed interface GatewayResult {
    data class Success(val razorpayOrderId: String, val paymentId: String, val signature: String) : GatewayResult
    data class Failure(val code: Int, val description: String?) : GatewayResult
}

/** Bridges Razorpay's Activity callbacks into a flow the payment ViewModel can collect. */
object PaymentResultBus {
    private val _results = MutableSharedFlow<GatewayResult>(extraBufferCapacity = 1)
    val results: SharedFlow<GatewayResult> = _results.asSharedFlow()
    fun emit(r: GatewayResult) { _results.tryEmit(r) }
}

object RazorpayLauncher {
    /** Opens Razorpay Standard Checkout. [method] pre-selects upi / card / netbanking. */
    fun open(activity: Activity, init: PaymentInit, method: String?) {
        val checkout = Checkout()
        checkout.setKeyID(init.keyId)
        val options = JSONObject().apply {
            put("name", "Gola Grocery")
            put("description", "Order #${init.orderNumber}")
            put("order_id", init.providerOrderId)
            put("currency", init.currency)
            put("amount", init.amountPaise)
            put("theme", JSONObject().put("color", "#0C831F"))
            put("prefill", JSONObject().apply {
                put("name", init.customerName)
                put("contact", "+91" + init.customerPhone)
                init.customerEmail?.let { put("email", it) }
                method?.let { put("method", it) }
            })
            put("retry", JSONObject().put("enabled", true).put("max_count", 2))
        }
        checkout.open(activity, options)
    }
}
