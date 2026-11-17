package com.golagrocery.app.di

import android.content.Context
import com.golagrocery.app.data.local.SessionStore
import com.golagrocery.app.data.remote.Network
import com.golagrocery.app.data.remote.PaymentInit
import com.golagrocery.app.data.repository.*
import com.golagrocery.app.location.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** Manual dependency injection: one instance per app process. */
class AppContainer(context: Context) {
    /** Process-wide scope for fire-and-forget work that must outlive a screen (e.g. push registration). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val session = SessionStore(context, Network.json)
    private val sessionExpired = Channel<Unit>(Channel.CONFLATED)
    val sessionExpiredEvents = sessionExpired.receiveAsFlow()
    val api = Network.create(session) { sessionExpired.trySend(Unit) }

    val auth = AuthRepository(api, session)
    val catalog = CatalogRepository(api)
    val locationRepo = LocationRepository(api, session)
    val addresses = AddressRepository(api)
    val orders = OrderRepository(api)
    val engagement = EngagementRepository(api)
    val delivery = DeliveryRepository(api)
    val cart = CartStore(api, session)
    val location = LocationHelper(context)

    /** Hand-off of a freshly created gateway order from checkout to the payment screen. */
    @Volatile var pendingPayment: Pair<Long, PaymentInit>? = null
}
