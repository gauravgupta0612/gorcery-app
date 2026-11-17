package com.golagrocery.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.OrderDetailDto
import com.golagrocery.app.data.remote.OrderSummaryDto
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.ui.components.*
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OrdersViewModel(private val c: AppContainer) : ViewModel() {
    var state by mutableStateOf<Load<List<OrderSummaryDto>>>(Load.Loading); private set
    init { load() }
    fun load() {
        viewModelScope.launch {
            state = when (val r = c.orders.orders(0)) { is ApiResult.Ok -> Load.Ready(r.data.items); is ApiResult.Err -> Load.Failed(r.message, r.isNetwork) }
        }
    }
}

@Composable
fun OrdersScreen(nav: NavHostController) {
    val vm = golaViewModel { OrdersViewModel(it) }
    DisposableEffectOnResume { vm.load() }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.my_orders), { nav.popBackStack() })
        LoadContent(vm.state, vm::load) { list ->
            if (list.isEmpty()) EmptyView(Icons.Outlined.ReceiptLong, R.string.no_orders, null) {
                Button(onClick = { nav.navigate(Routes.HOME) }) { Text(stringResource(R.string.start_shopping)) }
            } else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(list, key = { it.id }) { o ->
                    GolaCard(Modifier.clickable { nav.navigate(Routes.order(o.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("#${o.orderNumber}", style = MaterialTheme.typography.titleSmall)
                                Text(formatDate(o.placedAt), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                            }
                            StatusChip(o.status)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            o.images.take(4).forEach { NetImage(it, null, Modifier.padding(end = 6.dp).size(44.dp).clip(RoundedCornerShape(8.dp))) }
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(rupees(o.grandTotal), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.items_count, o.itemCount) + " · " + o.paymentMethod, style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                            }
                        }
                    }
                }
            }
        }
    }
}

class OrderDetailViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    var state by mutableStateOf<Load<OrderDetailDto>>(Load.Loading); private set
    var busy by mutableStateOf(false); private set
    init {
        load()
        // Live tracking: poll while the order is active.
        viewModelScope.launch {
            while (isActive) {
                delay(20_000)
                val s = (state as? Load.Ready)?.data?.status
                if (s != null && s !in setOf("DELIVERED", "CANCELLED", "PAYMENT_FAILED")) refresh()
            }
        }
    }
    fun load() { state = Load.Loading; viewModelScope.launch { refresh() } }
    private suspend fun refresh() {
        when (val r = c.orders.order(id)) {
            is ApiResult.Ok -> state = Load.Ready(r.data)
            is ApiResult.Err -> if (state !is Load.Ready) state = Load.Failed(r.message, r.isNetwork)
        }
    }
    fun cancel(reason: String, onError: (String) -> Unit) = act {
        when (val r = c.orders.cancel(id, reason)) { is ApiResult.Ok -> state = Load.Ready(r.data); is ApiResult.Err -> onError(r.message) }
    }
    fun reorder(onDone: (Int, List<String>) -> Unit, onError: (String) -> Unit) = act {
        when (val r = c.orders.reorder(id)) { is ApiResult.Ok -> { c.cart.refresh(); onDone(r.data.added, r.data.skipped) }; is ApiResult.Err -> onError(r.message) }
    }
    private fun act(block: suspend () -> Unit) { if (busy) return; busy = true; viewModelScope.launch { try { block() } finally { busy = false } } }
}

private val TRACK = listOf("PLACED", "CONFIRMED", "PREPARING", "PACKED", "ASSIGNED", "OUT_FOR_DELIVERY", "DELIVERED")

@Composable
fun OrderDetailScreen(nav: NavHostController, id: Long, showMessage: (String) -> Unit) {
    val vm = golaViewModel(key = "order-$id") { OrderDetailViewModel(it, id) }
    val ctx = LocalContext.current
    var cancelOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.order_details), { nav.popBackStack() })
        LoadContent(vm.state, vm::load) { o ->
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                item {
                    GolaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("#${o.orderNumber}", style = MaterialTheme.typography.titleMedium)
                                Text(formatDate(o.placedAt), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                            }
                            StatusChip(o.status)
                        }
                        if (o.testPayment) Text("TEST payment – no money was collected", color = GolaColors.Red, style = MaterialTheme.typography.labelSmall)
                        o.cancelReason?.let { Text(it, color = GolaColors.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                    }
                }
                if (o.deliveryOtp != null) item {
                    GolaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = GolaColors.Green)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(stringResource(R.string.delivery_otp, o.deliveryOtp.orEmpty()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Text(stringResource(R.string.delivery_otp_hint), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                            }
                        }
                    }
                }
                if (o.status in TRACK) item { TrackingCard(o) }
                o.deliveryPartner?.let { p ->
                    item {
                        GolaCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(44.dp).clip(CircleShape).background(GolaColors.GreenLight), contentAlignment = Alignment.Center) { Icon(Icons.Default.DeliveryDining, null, tint = GolaColors.Green) }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.delivery_partner), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                                    Text(p.name, style = MaterialTheme.typography.titleSmall)
                                    Text("${p.vehicleType} ${p.vehicleNumber ?: ""}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (o.status != "DELIVERED") OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${p.phone}"))) }) {
                                    Icon(Icons.Default.Call, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.call))
                                }
                            }
                        }
                    }
                }
                item {
                    GolaCard {
                        Text(stringResource(R.string.items_count, o.items.sumOf { it.quantity }), style = MaterialTheme.typography.titleSmall)
                        o.items.forEach { i ->
                            Row(Modifier.padding(vertical = 6.dp).clickable { nav.navigate(Routes.product(i.productId)) }, verticalAlignment = Alignment.CenterVertically) {
                                NetImage(i.imageUrl, i.productName, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(i.productName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text("${i.variantLabel} × ${i.quantity}", style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                                }
                                Text(rupees(i.lineTotal), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
                item {
                    GolaCard {
                        Text(stringResource(R.string.bill_details), style = MaterialTheme.typography.titleSmall)
                        BillRow(stringResource(R.string.item_total), rupees(o.itemTotal))
                        if (o.couponDiscount > 0) BillRow("${stringResource(R.string.coupon_discount)} (${o.couponCode})", "− " + rupees(o.couponDiscount), valueColor = GolaColors.Green)
                        BillRow(stringResource(R.string.delivery_fee), if (o.deliveryFee == 0.0) stringResource(R.string.free) else rupees(o.deliveryFee))
                        BillRow(stringResource(R.string.handling_fee), rupees(o.handlingFee))
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        BillRow(stringResource(R.string.to_pay), rupees(o.grandTotal), bold = true)
                        Text("${o.paymentMethod} · ${o.paymentStatus.replace('_', ' ')}", style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                        o.refunds.forEach { r -> Text("Refund ${rupees(r.amount)} – ${r.status}", style = MaterialTheme.typography.bodySmall, color = GolaColors.Blue) }
                    }
                }
                item {
                    GolaCard {
                        Text(stringResource(R.string.delivery_address), style = MaterialTheme.typography.titleSmall)
                        Text("${o.address.name} · ${o.address.phone}", style = MaterialTheme.typography.bodyMedium)
                        Text(o.address.oneLine(), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                    }
                }
                item {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (o.canRetryPayment) Button(onClick = { nav.navigate(Routes.payment(o.id, o.paymentMethod)) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.retry_payment))
                        }
                        if (o.canReview) Button(onClick = { nav.navigate(Routes.review(o.id)) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.rate_products)) }
                        if (o.status == "DELIVERED" || o.status == "CANCELLED" || o.status == "PAYMENT_FAILED") {
                            OutlinedButton(enabled = !vm.busy, onClick = {
                                vm.reorder({ added, skipped ->
                                    if (skipped.isNotEmpty()) showMessage("Added $added. Skipped: " + skipped.joinToString())
                                    nav.navigate(Routes.CART)
                                }, showMessage)
                            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Replay, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.reorder)) }
                        }
                        OutlinedButton(onClick = { nav.navigate(Routes.SUPPORT) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.support)) }
                        if (o.canCancel) TextButton(onClick = { cancelOpen = true }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.cancel_order), color = GolaColors.Red)
                        }
                    }
                }
            }
        }
    }
    if (cancelOpen) {
        val reasons = listOf("Ordered by mistake", "Delivery time too long", "Want to change items", "Found a better price", "Other")
        var reason by remember { mutableStateOf(reasons.first()) }
        AlertDialog(
            onDismissRequest = { cancelOpen = false },
            title = { Text(stringResource(R.string.cancel_reason)) },
            text = {
                Column { reasons.forEach { r -> Row(Modifier.fillMaxWidth().clickable { reason = r }, verticalAlignment = Alignment.CenterVertically) { RadioButton(reason == r, { reason = r }); Text(r) } } }
            },
            confirmButton = { TextButton(onClick = { cancelOpen = false; vm.cancel(reason, showMessage) }) { Text(stringResource(R.string.confirm), color = GolaColors.Red) } },
            dismissButton = { TextButton(onClick = { cancelOpen = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun TrackingCard(o: OrderDetailDto) {
    val current = TRACK.indexOf(o.status)
    val times = o.history.associate { it.status to it.at }
    GolaCard {
        TRACK.forEachIndexed { i, s ->
            val done = i <= current
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(18.dp).clip(CircleShape).background(if (done) GolaColors.Green else GolaColors.Grey.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
                        if (done) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                    if (i < TRACK.lastIndex) Box(Modifier.width(2.dp).height(26.dp).background(if (i < current) GolaColors.Green else GolaColors.Grey.copy(alpha = .25f)))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(orderStatusLabel(s), style = MaterialTheme.typography.bodyMedium, fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal,
                        color = if (done) Color.Unspecified else GolaColors.Grey)
                    times[s]?.let { Text(formatDate(it), style = MaterialTheme.typography.labelSmall, color = GolaColors.Grey) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ reviews
class ReviewViewModel(private val c: AppContainer, private val orderId: Long) : ViewModel() {
    var state by mutableStateOf<Load<OrderDetailDto>>(Load.Loading); private set
    val ratings = mutableStateMapOf<Long, Int>()
    val comments = mutableStateMapOf<Long, String>()
    var submitting by mutableStateOf(false); private set
    init { load() }
    fun load() { viewModelScope.launch { state = when (val r = c.orders.order(orderId)) { is ApiResult.Ok -> Load.Ready(r.data); is ApiResult.Err -> Load.Failed(r.message, r.isNetwork) } } }
    fun submit(onDone: (String) -> Unit) {
        submitting = true
        viewModelScope.launch {
            var ok = 0; var lastError: String? = null
            ratings.filter { it.value > 0 }.forEach { (pid, stars) ->
                when (val r = c.catalog.createReview(pid, stars, null, comments[pid])) { is ApiResult.Ok -> ok++; is ApiResult.Err -> lastError = r.message }
            }
            submitting = false
            onDone(if (lastError != null && ok == 0) lastError!! else "Thanks! $ok review(s) submitted")
        }
    }
}

@Composable
fun ReviewScreen(nav: NavHostController, orderId: Long, showMessage: (String) -> Unit) {
    val vm = golaViewModel(key = "review-$orderId") { ReviewViewModel(it, orderId) }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.write_review), { nav.popBackStack() })
        LoadContent(vm.state, vm::load, Modifier.weight(1f)) { o ->
            LazyColumn {
                items(o.items.distinctBy { it.productId }, key = { it.productId }) { i ->
                    GolaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NetImage(i.imageUrl, null, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
                            Spacer(Modifier.width(10.dp)); Text(i.productName, style = MaterialTheme.typography.titleSmall)
                        }
                        Row {
                            (1..5).forEach { n ->
                                IconButton(onClick = { vm.ratings[i.productId] = n }) {
                                    Icon(Icons.Default.Star, "$n", tint = if ((vm.ratings[i.productId] ?: 0) >= n) GolaColors.Yellow else GolaColors.Grey.copy(alpha = .3f))
                                }
                            }
                        }
                        if ((vm.ratings[i.productId] ?: 0) > 0) OutlinedTextField(vm.comments[i.productId] ?: "", { vm.comments[i.productId] = it.take(1000) },
                            placeholder = { Text(stringResource(R.string.review_comment)) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Button(onClick = { vm.submit { msg -> showMessage(msg); nav.popBackStack() } }, enabled = !vm.submitting && vm.ratings.values.any { it > 0 },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp)) { Text(stringResource(R.string.submit)) }
    }
}
