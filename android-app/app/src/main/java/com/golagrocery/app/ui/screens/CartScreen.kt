package com.golagrocery.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.CouponDto
import com.golagrocery.app.data.remote.Quote
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.ui.components.*
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.launch

class CartViewModel(private val c: AppContainer) : ViewModel() {
    var state by mutableStateOf<Load<Unit>>(Load.Loading); private set
    init { load() }
    fun load() {
        if (c.cart.quote.value == null) state = Load.Loading
        viewModelScope.launch {
            state = when (val r = c.cart.refresh()) {
                is ApiResult.Ok -> Load.Ready(Unit)
                is ApiResult.Err -> if (c.cart.quote.value != null) Load.Ready(Unit) else Load.Failed(r.message, r.isNetwork)
            }
        }
    }
    fun removeCoupon(onError: (String) -> Unit) = viewModelScope.launch {
        (c.cart.removeCoupon() as? ApiResult.Err)?.let { onError(it.message) }
    }
}

@Composable
fun CartScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { CartViewModel(it) }
    val container = appContainer()
    val quote by container.cart.quote.collectAsState()
    val cart = rememberCartActions(showMessage) { nav.navigate(Routes.LOGIN) }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.cart), null)
        LoadContent(vm.state, vm::load, Modifier.weight(1f)) {
            val q = quote
            if (q == null || q.lines.isEmpty()) {
                EmptyView(Icons.Outlined.ShoppingCart, R.string.cart_empty, stringResource(R.string.cart_empty_hint)) {
                    Button(onClick = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }) { Text(stringResource(R.string.start_shopping)) }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    if (q.amountForFreeDelivery > 0) item {
                        GolaCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalShipping, null, tint = GolaColors.Green)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.add_more_free_delivery, plain(q.amountForFreeDelivery)), style = MaterialTheme.typography.bodyMedium)
                            }
                            LinearProgressIndicator(progress = { ((q.itemTotal - q.couponDiscount) / q.freeDeliveryAbove).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = GolaColors.Green)
                        }
                    }
                    item {
                        GolaCard(Modifier.animateContentSize()) {
                            q.lines.forEachIndexed { i, l ->
                                if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NetImage(l.imageUrl, l.productName, Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).clickable { nav.navigate(Routes.product(l.productId)) })
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(l.productName, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                        Text(l.variantLabel, style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                                        l.problem?.let { Text(it, color = GolaColors.Red, style = MaterialTheme.typography.labelSmall) }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        QuantityStepper(l.quantity, cart.isBusy(l.variantId), true, { cart.setQty(l.variantId, it) }, l.maxPerOrder.coerceAtMost(maxOf(l.availableQty, l.quantity)))
                                        Spacer(Modifier.height(4.dp))
                                        Text(rupees(l.lineTotal), style = MaterialTheme.typography.titleSmall)
                                        if (l.lineMrp > l.lineTotal) Text(rupees(l.lineMrp), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey, textDecoration = TextDecoration.LineThrough)
                                    }
                                }
                            }
                        }
                    }
                    item { CouponRow(q, { nav.navigate(Routes.COUPONS) }) { vm.removeCoupon(showMessage) } }
                    item { BillCard(q) }
                    if (q.productSavings + q.couponDiscount > 0) item {
                        Text(stringResource(R.string.you_save, plain(q.productSavings + q.couponDiscount)), color = GolaColors.Discount,
                            style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth().padding(16.dp))
                    }
                }
            }
        }
        val q = quote
        if (q != null && q.lines.isNotEmpty()) {
            Surface(shadowElevation = 12.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (!q.meetsMinimum) Text(stringResource(R.string.min_order, plain(q.minOrderAmount)), color = GolaColors.Red, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { nav.navigate(Routes.CHECKOUT) }, enabled = q.meetsMinimum && !q.hasProblems, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("${rupees(q.grandTotal)}  ·  ${stringResource(R.string.proceed_to_checkout)}")
                    }
                }
            }
        }
    }
}

@Composable
fun CouponRow(q: Quote, onOpen: () -> Unit, onRemove: () -> Unit) {
    GolaCard {
        Row(Modifier.clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalOffer, null, tint = GolaColors.Green)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                if (q.couponValid && q.couponCode != null) {
                    Text("${q.couponCode} · ${stringResource(R.string.applied)}", style = MaterialTheme.typography.titleSmall, color = GolaColors.Green)
                    q.couponMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                } else {
                    Text(stringResource(R.string.apply_coupon), style = MaterialTheme.typography.titleSmall)
                    if (q.couponCode != null && q.couponMessage != null) Text(q.couponMessage, style = MaterialTheme.typography.bodySmall, color = GolaColors.Red)
                }
            }
            if (q.couponCode != null) TextButton(onClick = onRemove) { Text(stringResource(R.string.remove)) }
            else Icon(Icons.Default.KeyboardArrowRight, null)
        }
    }
}

@Composable
fun BillCard(q: Quote) {
    GolaCard {
        Text(stringResource(R.string.bill_details), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        BillRow(stringResource(R.string.item_total), rupees(q.itemTotal))
        if (q.couponDiscount > 0) BillRow(stringResource(R.string.coupon_discount), "− " + rupees(q.couponDiscount), valueColor = GolaColors.Green)
        BillRow(stringResource(R.string.delivery_fee), if (q.deliveryFee == 0.0) stringResource(R.string.free) else rupees(q.deliveryFee),
            valueColor = if (q.deliveryFee == 0.0) GolaColors.Green else Color.Unspecified)
        BillRow(stringResource(R.string.handling_fee), rupees(q.handlingFee))
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        BillRow(stringResource(R.string.to_pay), rupees(q.grandTotal), bold = true)
    }
}

class CouponsViewModel(private val c: AppContainer) : ViewModel() {
    var state by mutableStateOf<Load<List<CouponDto>>>(Load.Loading); private set
    var applying by mutableStateOf(false); private set
    init { load() }
    fun load() {
        state = Load.Loading
        viewModelScope.launch {
            state = when (val r = c.cart.coupons()) { is ApiResult.Ok -> Load.Ready(r.data); is ApiResult.Err -> Load.Failed(r.message, r.isNetwork) }
        }
    }
    fun apply(code: String, onDone: () -> Unit, onError: (String) -> Unit) {
        applying = true
        viewModelScope.launch {
            when (val r = c.cart.applyCoupon(code)) { is ApiResult.Ok -> onDone(); is ApiResult.Err -> onError(r.message) }
            applying = false
        }
    }
}

@Composable
fun CouponsScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { CouponsViewModel(it) }
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.coupons), { nav.popBackStack() })
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(code, { code = it.uppercase().filter(Char::isLetterOrDigit).take(30) }, singleLine = true,
                placeholder = { Text(stringResource(R.string.enter_coupon)) }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.apply(code, { nav.popBackStack() }, showMessage) }, enabled = code.length >= 3 && !vm.applying) { Text(stringResource(R.string.apply)) }
        }
        LoadContent(vm.state, vm::load) { list ->
            LazyColumn {
                items(list, key = { it.code }) { c ->
                    GolaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.code, style = MaterialTheme.typography.titleMedium, color = GolaColors.GreenDark,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(GolaColors.YellowLight).padding(horizontal = 8.dp, vertical = 2.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(c.description ?: "", style = MaterialTheme.typography.bodyMedium)
                                if (!c.applicable && c.reason != null) Text(c.reason, style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                            }
                            TextButton(onClick = { vm.apply(c.code, { nav.popBackStack() }, showMessage) }, enabled = c.applicable && !vm.applying) {
                                Text(stringResource(R.string.apply))
                            }
                        }
                    }
                }
            }
        }
    }
}
