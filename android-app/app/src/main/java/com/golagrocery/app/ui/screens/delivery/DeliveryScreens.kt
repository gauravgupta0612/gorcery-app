package com.golagrocery.app.ui.screens.delivery

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.AssignmentDto
import com.golagrocery.app.data.remote.PartnerProfile
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.ui.components.*
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.navigation.goRoot
import com.golagrocery.app.ui.screens.DisposableEffectOnResume
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DeliveryHomeViewModel(private val c: AppContainer) : ViewModel() {
    var profile by mutableStateOf<PartnerProfile?>(null); private set
    var state by mutableStateOf<Load<List<AssignmentDto>>>(Load.Loading); private set
    init {
        load()
        viewModelScope.launch { while (isActive) { delay(30_000); refresh() } } // new assignments appear automatically
    }
    fun load() { viewModelScope.launch { refresh() } }
    private suspend fun refresh() {
        (c.delivery.me() as? ApiResult.Ok)?.let { profile = it.data }
        when (val r = c.delivery.active()) {
            is ApiResult.Ok -> state = Load.Ready(r.data)
            is ApiResult.Err -> if (state !is Load.Ready) state = Load.Failed(r.message, r.isNetwork)
        }
    }
    fun toggle(onError: (String) -> Unit) {
        val p = profile ?: return
        viewModelScope.launch {
            when (val r = c.delivery.setAvailable(!p.available)) { is ApiResult.Ok -> profile = r.data; is ApiResult.Err -> onError(r.message) }
        }
    }
    fun shareLocation() = viewModelScope.launch { c.location.detect()?.let { c.delivery.updateLocation(it.lat, it.lng) } }
}

@Composable
fun DeliveryHomeScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { DeliveryHomeViewModel(it) }
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val isCustomer = c.session.session.collectAsState().value?.user?.isCustomer == true
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { g -> if (g.values.any { it }) vm.shareLocation() }
    LaunchedEffect(Unit) { perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    DisposableEffectOnResume { vm.load() }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        Column(Modifier.fillMaxWidth().background(GolaColors.GreenDark).statusBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.delivery_mode), color = GolaColors.Yellow, style = MaterialTheme.typography.labelLarge)
                    Text(vm.profile?.name ?: "", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    vm.profile?.let { Text("${it.vehicleType} ${it.vehicleNumber ?: ""} · ${it.deliveredCount} delivered", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(checked = vm.profile?.available == true, onCheckedChange = { vm.toggle(showMessage) },
                        colors = SwitchDefaults.colors(checkedTrackColor = GolaColors.Yellow, checkedThumbColor = GolaColors.GreenDark))
                    Text(stringResource(if (vm.profile?.available == true) R.string.online else R.string.offline), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { nav.navigate(Routes.DELIVERY_HISTORY) }, label = { Text(stringResource(R.string.delivery_history), color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.History, null, tint = Color.White) })
                if (isCustomer) AssistChip(onClick = { nav.goRoot(Routes.HOME) }, label = { Text(stringResource(R.string.home), color = Color.White) })
                AssistChip(onClick = { scope.launch { c.auth.logout(); nav.goRoot(Routes.LOGIN) } }, label = { Text(stringResource(R.string.logout), color = Color.White) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = Color.White) })
            }
        }
        SectionTitle(stringResource(R.string.assigned_orders)) { IconButton(onClick = vm::load) { Icon(Icons.Default.Refresh, null) } }
        LoadContent(vm.state, vm::load) { list ->
            if (list.isEmpty()) EmptyView(Icons.Outlined.Inventory2, R.string.no_assignments)
            else LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(list, key = { it.assignmentId }) { a -> AssignmentCard(a) { nav.navigate(Routes.deliveryAssignment(a.assignmentId)) } }
            }
        }
    }
}

@Composable
private fun AssignmentCard(a: AssignmentDto, onClick: () -> Unit) {
    GolaCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("#${a.orderNumber}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            StatusChip(a.assignmentStatus)
        }
        Text("${a.address.name} · ${a.address.phone}", style = MaterialTheme.typography.bodyMedium)
        Text(a.address.oneLine(), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey, maxLines = 2)
        Row(Modifier.padding(top = 6.dp)) {
            Text(stringResource(R.string.items_count, a.itemCount), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(if (a.amountToCollect > 0) stringResource(R.string.collect_cash, plain(a.amountToCollect)) else stringResource(R.string.prepaid),
                color = if (a.amountToCollect > 0) GolaColors.Red else GolaColors.Green, style = MaterialTheme.typography.labelLarge)
        }
    }
}

class AssignmentViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    var state by mutableStateOf<Load<AssignmentDto>>(Load.Loading); private set
    var busy by mutableStateOf(false); private set
    init { load() }
    fun load() { viewModelScope.launch { state = when (val r = c.delivery.get(id)) { is ApiResult.Ok -> Load.Ready(r.data); is ApiResult.Err -> Load.Failed(r.message, r.isNetwork) } } }
    fun act(block: suspend () -> ApiResult<AssignmentDto>, onError: (String) -> Unit, onOk: (AssignmentDto) -> Unit = {}) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            when (val r = block()) { is ApiResult.Ok -> { state = Load.Ready(r.data); onOk(r.data) }; is ApiResult.Err -> onError(r.message) }
            busy = false
        }
    }
    fun pickup(onError: (String) -> Unit) = act({ c.delivery.pickup(id) }, onError)
    fun out(onError: (String) -> Unit) = act({ c.delivery.outForDelivery(id) }, onError)
    fun deliver(otp: String, cash: Double?, onError: (String) -> Unit, onOk: () -> Unit) = act({ c.delivery.deliver(id, otp, cash) }, onError) { onOk() }
}

@Composable
fun AssignmentScreen(nav: NavHostController, id: Long, showMessage: (String) -> Unit) {
    val vm = golaViewModel(key = "assign-$id") { AssignmentViewModel(it, id) }
    val ctx = LocalContext.current
    var deliverOpen by remember { mutableStateOf(false) }
    val deliveredMsg = stringResource(R.string.delivered_success)
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.order_details), { nav.popBackStack() })
        LoadContent(vm.state, vm::load, Modifier.weight(1f)) { a ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                GolaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${a.orderNumber}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); StatusChip(a.assignmentStatus)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(a.address.name, style = MaterialTheme.typography.titleSmall)
                    Text(a.address.oneLine(), style = MaterialTheme.typography.bodyMedium)
                    a.deliveryInstructions?.let { Text("📝 $it", style = MaterialTheme.typography.bodySmall, color = GolaColors.GreenDark, modifier = Modifier.padding(top = 4.dp)) }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${a.address.phone}"))) }) {
                            Icon(Icons.Default.Call, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.call))
                        }
                        Button(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.navigationUrl))) }) {
                            Icon(Icons.Default.Navigation, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.navigate))
                        }
                    }
                }
                GolaCard {
                    Text(if (a.amountToCollect > 0) stringResource(R.string.collect_cash, plain(a.amountToCollect)) else stringResource(R.string.prepaid),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (a.amountToCollect > 0) GolaColors.Red else GolaColors.Green)
                    Text("${a.paymentMethod} · ${rupees(a.grandTotal)}", style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                }
                GolaCard {
                    Text(stringResource(R.string.items_count, a.itemCount), style = MaterialTheme.typography.titleSmall)
                    a.items.forEach { i ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${i.quantity} ×", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                            Text("${i.productName} (${i.variantLabel})", Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        (vm.state as? Load.Ready)?.data?.let { a ->
            Surface(shadowElevation = 12.dp) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    when (a.assignmentStatus) {
                        "ASSIGNED" -> Button(onClick = { vm.pickup(showMessage) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.confirm_pickup)) }
                        "PICKED_UP" -> Button(onClick = { vm.out(showMessage) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.start_delivery)) }
                        "OUT_FOR_DELIVERY" -> Button(onClick = { deliverOpen = true }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.mark_delivered)) }
                        else -> Text(orderStatusLabel(a.assignmentStatus), modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
            if (deliverOpen) {
                var otp by remember { mutableStateOf("") }
                var cash by remember { mutableStateOf(if (a.amountToCollect > 0) plain(a.amountToCollect) else "") }
                AlertDialog(
                    onDismissRequest = { deliverOpen = false },
                    title = { Text(stringResource(R.string.mark_delivered)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(otp, { otp = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.customer_otp)) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                            if (a.amountToCollect > 0) OutlinedTextField(cash, { cash = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text(stringResource(R.string.cash_collected)) },
                                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = otp.length == 4 && !vm.busy, onClick = {
                            vm.deliver(otp, if (a.amountToCollect > 0) cash.toDoubleOrNull() else null, showMessage) {
                                deliverOpen = false; showMessage(deliveredMsg); nav.popBackStack()
                            }
                        }) { Text(stringResource(R.string.confirm)) }
                    },
                    dismissButton = { TextButton(onClick = { deliverOpen = false }) { Text(stringResource(R.string.cancel)) } },
                )
            }
        }
    }
}

class DeliveryHistoryViewModel(private val c: AppContainer) : ViewModel() {
    var state by mutableStateOf<Load<List<AssignmentDto>>>(Load.Loading); private set
    init { load() }
    fun load() { viewModelScope.launch { state = when (val r = c.delivery.history(0)) { is ApiResult.Ok -> Load.Ready(r.data.items); is ApiResult.Err -> Load.Failed(r.message, r.isNetwork) } } }
}

@Composable
fun DeliveryHistoryScreen(nav: NavHostController) {
    val vm = golaViewModel { DeliveryHistoryViewModel(it) }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.delivery_history), { nav.popBackStack() })
        LoadContent(vm.state, vm::load) { list ->
            if (list.isEmpty()) EmptyView(Icons.Outlined.Inventory2, R.string.no_orders)
            else LazyColumn {
                items(list, key = { it.assignmentId }) { a ->
                    GolaCard {
                        Row { Text("#${a.orderNumber}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Text(rupees(a.grandTotal)) }
                        Text("${a.address.name} · ${a.address.city} ${a.address.pincode}", style = MaterialTheme.typography.bodySmall)
                        Text(formatDate(a.deliveredAt), style = MaterialTheme.typography.labelSmall, color = GolaColors.Grey)
                    }
                }
            }
        }
    }
}
