package com.golagrocery.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.golagrocery.app.payments.GatewayResult
import com.golagrocery.app.payments.PaymentResultBus
import com.golagrocery.app.ui.navigation.GolaNavHost
import com.golagrocery.app.ui.theme.GolaTheme
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

class MainActivity : AppCompatActivity(), PaymentResultWithDataListener {
    companion object { const val EXTRA_ORDER_ID = "order_id" }

    /** Order id from a tapped push notification, consumed by the nav host. */
    private val openOrder = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Checkout.preload(applicationContext)
        handleIntent(intent)
        setContent {
            GolaTheme {
                GolaNavHost(openOrderId = openOrder.value, onOrderOpened = { openOrder.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_ORDER_ID, -1L) ?: -1L
        if (id > 0) openOrder.value = id
    }

    // Razorpay callbacks -> payment ViewModel (signature is verified by the backend, never trusted here)
    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        val orderId = data?.orderId
        val sig = data?.signature
        if (razorpayPaymentId != null && orderId != null && sig != null) {
            PaymentResultBus.emit(GatewayResult.Success(orderId, razorpayPaymentId, sig))
        } else {
            PaymentResultBus.emit(GatewayResult.Failure(-1, "Incomplete response from payment gateway"))
        }
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        PaymentResultBus.emit(GatewayResult.Failure(code, response))
    }
}
