package com.golagrocery.app.ui.screens

import android.app.Activity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.AddressDto
import com.golagrocery.app.data.remote.OrderDetailDto
import com.golagrocery.app.data.remote.PaymentInit
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.payments.GatewayResult
import com.golagrocery.app.payments.PaymentResultBus
import com.golagrocery.app.payments.RazorpayLauncher
import com.golagrocery.app.ui.components.*
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ checkout
class CheckoutViewModel(private val c: AppContainer) : ViewModel() {
    var addresses by mutableStateOf<Load<List<AddressDto>>>(Load.Loading); private set
    var selectedAddress by mutableLongStateOf(-1L)
    var method by mutableStateOf("UPI")
    var instructions by mutableStateOf("")
    var placing by mutableStateOf(false); private set
    var codEnabled by mutableStateOf(true); private set

    init { load() }
    fun load() {
        addresses = Load.Loading
        viewModelScope.launch {
            (c.catalog.settings() as? ApiResult.Ok)?.let { codEnabled = it.data.codEnabled }
            addresses = when (val r = c.addresses.list()) {
                is ApiResult.Ok -> {
                    val pin = c.session.location.value?.pincode
                    if (selectedAddress < 0 || r.data.none { it.id == selectedAddress }) {
                        selectedAddress = (r.data.firstOrNull { it.pincode == pin && it.isDefault } ?: r.data.firstOrNull { it.pincode == pin }
                            ?: r.data.firstOrNull { it.isDefault } ?: r.data.firstOrNull())?.id ?: -1
                    }
                    Load.Ready(r.data)
                }
                is ApiResult.Err -> Load.Failed(r.message, r.isNetwork)
            }
            c.cart.refresh()
        }
    }

    fun place(nav: NavHostController, onError: (String) -> Unit) {
        if (placing || selectedAddress < 0) return
        placing = true
        viewModelScope.launch {
            when (val r = c.orders.checkout(selectedAddress, method, instructions)) {
                is ApiResult.Ok -> {
                    c.cart.refresh()
                    val order = r.data.order
                    val pay = r.data.payment
                    if (pay == null) {
                        nav.navigate(Routes.orderPlaced(order.id)) { popUpTo(Routes.CART) }
                    } else {
                        c.pendingPayment = order.id to pay
                        nav.navigate(Routes.payment(order.id, method)) { popUpTo(Routes.CART) }
                    }
                }
                is ApiResult.Err -> { onError(r.message); c.cart.refresh() }
            }
            placing = false
        }
    }
}

private data class PayOption(val key: String, val label: Int, val icon: ImageVector)
private val payOptions = listOf(
    PayOption("UPI", R.string.upi, Icons.Default.QrCode), PayOption("CARD", R.string.card, Icons.Default.CreditCard),
    PayOption("NETBANKING", R.string.netbanking, Icons.Default.AccountBalance), PayOption("COD", R.string.cod, Icons.Default.Payments),
)

@Composable
fun CheckoutScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { CheckoutViewModel(it) }
    val quote by appContainer().cart.quote.collectAsState()
    // Reload addresses when returning from the address editor.
    DisposableEffectOnResume { vm.load() }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.checkout), { nav.popBackStack() })
        LoadContent(vm.addresses, vm::load, Modifier.weight(1f)) { list ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                GolaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.delivery_address), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { nav.navigate(Routes.addressEdit()) }) { Text("+ " + stringResource(R.string.add_address)) }
                    }
                    if (list.isEmpty()) Text(stringResource(R.string.no_addresses), color = GolaColors.Grey)
                    list.forEach { a ->
                        val sel = a.id == vm.selectedAddress
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp))
                                .border(if (sel) 2.dp else 1.dp, if (sel) GolaColors.Green else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                .clickable { vm.selectedAddress = a.id }.padding(10.dp),
                        ) {
                            RadioButton(sel, { vm.selectedAddress = a.id })
                            Column(Modifier.weight(1f)) {
                                Text("${labelFor(a.label)} · ${a.contactName}", style = MaterialTheme.typography.titleSmall)
                                Text(a.oneLine(), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                                Text(a.contactPhone, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                GolaCard {
                    Text(stringResource(R.string.payment_method), style = MaterialTheme.typography.titleSmall)
                    payOptions.filter { it.key != "COD" || vm.codEnabled }.forEach { o ->
                        Row(Modifier.fillMaxWidth().clickable { vm.method = o.key }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(o.icon, null, tint = GolaColors.Green)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(o.label), Modifier.weight(1f))
                            RadioButton(vm.method == o.key, { vm.method = o.key })
                        }
                    }
                }
                GolaCard {
                    OutlinedTextField(vm.instructions, { vm.instructions = it.take(300) }, label = { Text(stringResource(R.string.delivery_instructions)) },
                        modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
                quote?.let { BillCard(it) }
                Spacer(Modifier.height(16.dp))
            }
        }
        val total = quote?.grandTotal ?: 0.0
        Surface(shadowElevation = 12.dp) {
            Button(
                onClick = { vm.place(nav, showMessage) }, enabled = !vm.placing && vm.selectedAddress > 0 && (quote?.lines?.isNotEmpty() == true),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
            ) {
                if (vm.placing) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(stringResource(if (vm.method == "COD") R.string.place_order else R.string.pay_now, plain(total)))
            }
        }
    }
}

@Composable
fun labelFor(label: String) = when (label) {
    "WORK" -> stringResource(R.string.label_work); "OTHER" -> stringResource(R.string.label_other); else -> stringResource(R.string.label_home)
}

/** Runs [onResume] every time the screen becomes visible again (e.g. after returning from another screen). */
@Composable
fun DisposableEffectOnResume(onResume: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val latest by rememberUpdatedState(onResume)
    DisposableEffect(owner) {
        var first = true
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) { if (first) first = false else latest() }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

// ------------------------------------------------------------------ payment
sealed interface PayState {
    data object Launching : PayState
    data class AwaitingMock(val init: PaymentInit) : PayState
    data object Verifying : PayState
    data class Failed(val message: String) : PayState
}

class PaymentViewModel(private val c: AppContainer, private val orderId: Long) : ViewModel() {
    var state by mutableStateOf<PayState>(PayState.Launching); private set
    private var current: PaymentInit? = c.pendingPayment?.takeIf { it.first == orderId }?.second
    var launchRequest by mutableStateOf<PaymentInit?>(null); private set
    var paidOrder by mutableStateOf<OrderDetailDto?>(null); private set

    init {
        c.pendingPayment = null
        viewModelScope.launch {
            PaymentResultBus.results.collect { r -> onGateway(r) }
        }
        start()
    }

    private fun start() {
        val i = current
        if (i == null) { retry(); return }
        if (i.provider == "MOCK") state = PayState.AwaitingMock(i) else { state = PayState.Launching; launchRequest = i }
    }

    fun launched() { launchRequest = null }

    fun retry() {
        state = PayState.Launching
        viewModelScope.launch {
            when (val r = c.orders.retryPayment(orderId)) {
                is ApiResult.Ok -> { current = r.data; start() }
                is ApiResult.Err -> state = PayState.Failed(r.message)
            }
        }
    }

    private fun onGateway(r: GatewayResult) {
        val i = current ?: return
        state = PayState.Verifying
        viewModelScope.launch {
            when (r) {
                is GatewayResult.Success -> handle(c.orders.verifyRazorpay(orderId, r.razorpayOrderId.ifBlank { i.providerOrderId }, r.paymentId, r.signature))
                is GatewayResult.Failure -> {
                    c.orders.reportFailure(orderId, "code=${r.code}")
                    state = PayState.Failed(r.description?.take(200) ?: "Payment cancelled")
                }
            }
        }
    }

    fun mock(success: Boolean) {
        state = PayState.Verifying
        viewModelScope.launch {
            if (success) handle(c.orders.completeMock(orderId, true))
            else { c.orders.completeMock(orderId, false); state = PayState.Failed("Payment declined (test mode)") }
        }
    }

    private fun handle(r: ApiResult<OrderDetailDto>) {
        when (r) {
            is ApiResult.Ok -> if (r.data.paymentStatus == "PAID") paidOrder = r.data else state = PayState.Failed("Payment could not be confirmed yet. Check My Orders.")
            is ApiResult.Err -> state = PayState.Failed(r.message)
        }
    }
}

@Composable
fun PaymentScreen(nav: NavHostController, orderId: Long, method: String) {
    val vm = golaViewModel(key = "pay-$orderId") { PaymentViewModel(it, orderId) }
    val activity = LocalContext.current as? Activity
    val req = vm.launchRequest
    LaunchedEffect(req) {
        if (req != null && activity != null) {
            RazorpayLauncher.open(activity, req, when (method) { "UPI" -> "upi"; "CARD" -> "card"; "NETBANKING" -> "netbanking"; else -> null })
            vm.launched()
        }
    }
    LaunchedEffect(vm.paidOrder) { vm.paidOrder?.let { nav.navigate(Routes.orderPlaced(it.id)) { popUpTo(Routes.CART) } } }

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        when (val s = vm.state) {
            PayState.Launching, PayState.Verifying -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.payment_pending), style = MaterialTheme.typography.titleMedium)
            }
            is PayState.AwaitingMock -> {
                Icon(Icons.Default.Science, null, tint = GolaColors.Yellow, modifier = Modifier.size(64.dp))
                Text(stringResource(R.string.test_payment_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.test_payment_body), textAlign = TextAlign.Center, color = GolaColors.Grey)
                Spacer(Modifier.height(8.dp))
                Text(rupees(s.init.amountPaise / 100.0) + " · #" + s.init.orderNumber, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { vm.mock(true) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.simulate_success)) }
                OutlinedButton(onClick = { vm.mock(false) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.simulate_failure)) }
            }
            is PayState.Failed -> {
                Icon(Icons.Default.ErrorOutline, null, tint = GolaColors.Red, modifier = Modifier.size(64.dp))
                Text(stringResource(R.string.payment_failed), style = MaterialTheme.typography.titleLarge)
                Text(s.message, color = GolaColors.Grey, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.payment_failed_body), color = GolaColors.Grey, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(onClick = vm::retry, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.retry_payment)) }
                TextButton(onClick = { nav.navigate(Routes.order(orderId)) { popUpTo(Routes.CART) } }) { Text(stringResource(R.string.order_details)) }
            }
        }
    }
}

// ------------------------------------------------------------------ success
@Composable
fun OrderPlacedScreen(nav: NavHostController, orderId: Long) {
    var started by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (started) 1f else 0.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tick")
    LaunchedEffect(Unit) { started = true }
    var number by remember { mutableStateOf("") }
    val c = appContainer()
    LaunchedEffect(orderId) { (c.orders.order(orderId) as? ApiResult.Ok)?.let { number = it.data.orderNumber } }
    Column(Modifier.fillMaxSize().background(GolaColors.Green).statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(120.dp).scale(scale).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = GolaColors.Green, modifier = Modifier.size(72.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.order_placed), style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(stringResource(R.string.order_placed_body, number), color = GolaColors.Yellow, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { nav.navigate(Routes.order(orderId)) { popUpTo(Routes.HOME) } },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = GolaColors.Green), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.track_order))
        }
        TextButton(onClick = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }) {
            Text(stringResource(R.string.start_shopping), color = Color.White)
        }
    }
}
