package com.golagrocery.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.BuildConfig
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.*
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.ui.components.*
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.navigation.goRoot
import com.golagrocery.app.ui.screens.auth.PHONE_REGEX
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ account
@Composable
fun AccountScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val c = appContainer()
    val session by c.session.session.collectAsState()
    val user = session?.user
    val scope = rememberCoroutineScope()
    val hindi = AppCompatDelegate.getApplicationLocales().toLanguageTags().startsWith("hi")
    Column(Modifier.fillMaxSize().background(GolaColors.Surface).verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(GolaColors.Green).statusBarsPadding().padding(20.dp)) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(GolaColors.Yellow), contentAlignment = Alignment.Center) {
                Text(user?.fullName?.take(1)?.uppercase() ?: "G", style = MaterialTheme.typography.headlineMedium, color = GolaColors.GreenDark)
            }
            Spacer(Modifier.height(8.dp))
            Text(user?.fullName ?: "", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("+91 ${user?.phone ?: ""}" + (user?.email?.let { " · $it" } ?: ""), color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { nav.navigate(Routes.PROFILE) }, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.edit_profile), color = GolaColors.Yellow) }
        }
        GolaCard {
            MenuRow(Icons.Outlined.ReceiptLong, stringResource(R.string.my_orders)) { nav.navigate(Routes.ORDERS) }
            MenuRow(Icons.Outlined.Place, stringResource(R.string.addresses)) { nav.navigate(Routes.ADDRESSES) }
            MenuRow(Icons.Outlined.FavoriteBorder, stringResource(R.string.wishlist)) { nav.navigate(Routes.WISHLIST) }
            MenuRow(Icons.Outlined.Notifications, stringResource(R.string.notifications)) { nav.navigate(Routes.NOTIFICATIONS) }
            MenuRow(Icons.Outlined.LocalOffer, stringResource(R.string.coupons)) { nav.navigate(Routes.COUPONS) }
            MenuRow(Icons.Outlined.SupportAgent, stringResource(R.string.support)) { nav.navigate(Routes.SUPPORT) }
            if (user?.isDeliveryPartner == true) MenuRow(Icons.Outlined.DeliveryDining, stringResource(R.string.delivery_mode)) { nav.navigate(Routes.DELIVERY_HOME) }
        }
        GolaCard {
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                FilterChip(selected = !hindi, onClick = { setLanguage(c, scope, "en") }, label = { Text("English") })
                FilterChip(selected = hindi, onClick = { setLanguage(c, scope, "hi") }, label = { Text("हिंदी") })
            }
        }
        GolaCard {
            MenuRow(Icons.AutoMirrored.Outlined.Logout, stringResource(R.string.logout), tint = GolaColors.Red) {
                scope.launch { c.auth.logout(); c.cart.clearLocal(); nav.goRoot(Routes.LOGIN) }
            }
        }
        Text("Gola Grocery v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey, modifier = Modifier.padding(16.dp))
    }
}

private fun setLanguage(c: AppContainer, scope: kotlinx.coroutines.CoroutineScope, lang: String) {
    // Persist on the server so push notifications arrive in the same language, then switch the UI (recreates the activity).
    c.session.session.value?.user?.let { u -> scope.launch { c.auth.updateProfile(u.fullName, u.email, lang) } }
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
}

@Composable
fun MenuRow(icon: ImageVector, label: String, tint: Color = GolaColors.GreenDark, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint)
        Spacer(Modifier.width(14.dp))
        Text(label, Modifier.weight(1f), color = if (tint == GolaColors.Red) tint else Color.Unspecified)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = GolaColors.Grey)
    }
}

@Composable
fun EditProfileScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val c = appContainer()
    val user = c.session.session.collectAsState().value?.user
    var name by remember { mutableStateOf(user?.fullName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.saved)
    Column(Modifier.fillMaxSize().background(Color.White)) {
        GolaTopBar(stringResource(R.string.edit_profile), { nav.popBackStack() })
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(name, { name = it.take(120) }, label = { Text(stringResource(R.string.full_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(email, { email = it.take(160) }, label = { Text(stringResource(R.string.email_optional)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField("+91 ${user?.phone ?: ""}", {}, enabled = false, label = { Text(stringResource(R.string.mobile_number)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(enabled = !busy && name.trim().length >= 2, modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {
                busy = true
                scope.launch {
                    when (val r = c.auth.updateProfile(name, email, user?.preferredLanguage)) {
                        is ApiResult.Ok -> { showMessage(savedMsg); nav.popBackStack() }
                        is ApiResult.Err -> showMessage(r.message)
                    }
                    busy = false
                }
            }) { Text(stringResource(R.string.save)) }
        }
    }
}

// ------------------------------------------------------------------ addresses
class AddressesViewModel(private val c: AppContainer) : ViewModel() {
    var state by mutableStateOf<Load<List<AddressDto>>>(Load.Loading); private set
    init { load() }
    fun load() { viewModelScope.launch { state = when (val r = c.addresses.list()) { is ApiResult.Ok -> Load.Ready(r.data); is ApiResult.Err -> Load.Failed(r.message, r.isNetwork) } } }
    fun delete(id: Long, onError: (String) -> Unit) = viewModelScope.launch {
        when (val r = c.addresses.delete(id)) { is ApiResult.Ok -> load(); is ApiResult.Err -> onError(r.message) }
    }
}

@Composable
fun AddressesScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { AddressesViewModel(it) }
    DisposableEffectOnResume { vm.load() }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.addresses), { nav.popBackStack() })
        TextButton(onClick = { nav.navigate(Routes.addressEdit()) }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("+ " + stringResource(R.string.add_address)) }
        LoadContent(vm.state, vm::load) { list ->
            if (list.isEmpty()) EmptyView(Icons.Outlined.Place, R.string.no_addresses)
            else LazyColumn {
                items(list, key = { it.id }) { a ->
                    GolaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(labelFor(a.label), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            if (a.isDefault) Text(stringResource(R.string.default_), color = GolaColors.Green, style = MaterialTheme.typography.labelSmall)
                        }
                        Text("${a.contactName} · ${a.contactPhone}", style = MaterialTheme.typography.bodyMedium)
                        Text(a.oneLine(), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                        Row {
                            TextButton(onClick = { nav.navigate(Routes.addressEdit(a.id)) }) { Text(stringResource(R.string.edit)) }
                            TextButton(onClick = { vm.delete(a.id, showMessage) }) { Text(stringResource(R.string.delete), color = GolaColors.Red) }
                        }
                    }
                }
            }
        }
    }
}

class AddressEditViewModel(private val c: AppContainer, private val id: Long?) : ViewModel() {
    var label by mutableStateOf("HOME"); var name by mutableStateOf(c.session.session.value?.user?.fullName ?: "")
    var phone by mutableStateOf(c.session.session.value?.user?.phone ?: ""); var line1 by mutableStateOf(""); var line2 by mutableStateOf("")
    var landmark by mutableStateOf(""); var city by mutableStateOf(""); var state by mutableStateOf("")
    var pincode by mutableStateOf(c.session.location.value?.pincode ?: ""); var isDefault by mutableStateOf(false)
    var lat: Double? = null; var lng: Double? = null
    var busy by mutableStateOf(false); private set
    var detecting by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)

    init {
        if (id != null) viewModelScope.launch {
            (c.addresses.list() as? ApiResult.Ok)?.data?.firstOrNull { it.id == id }?.let { a ->
                label = a.label; name = a.contactName; phone = a.contactPhone; line1 = a.line1; line2 = a.line2 ?: ""; landmark = a.landmark ?: ""
                city = a.city; state = a.state; pincode = a.pincode; isDefault = a.isDefault; lat = a.latitude; lng = a.longitude
            }
        }
    }

    val valid get() = name.isNotBlank() && PHONE_REGEX.matches(phone) && line1.isNotBlank() && city.isNotBlank() && state.isNotBlank() && Regex("^[1-9]\\d{5}$").matches(pincode)

    fun detect(onFail: () -> Unit) {
        detecting = true
        viewModelScope.launch {
            val d = c.location.detect()
            detecting = false
            if (d == null) { onFail(); return@launch }
            lat = d.lat; lng = d.lng
            d.pincode?.let { pincode = it }; d.city?.let { city = it }; d.state?.let { state = it }
            if (line2.isBlank()) d.line?.let { line2 = it.take(200) }
        }
    }

    fun save(onDone: () -> Unit) {
        busy = true; error = null
        viewModelScope.launch {
            val check = c.locationRepo.check(pincode)
            if (check is ApiResult.Ok && !check.data.serviceable) { error = check.data.message; busy = false; return@launch }
            val req = AddressRequest(label, name.trim(), phone, line1.trim(), line2.trim().ifBlank { null }, landmark.trim().ifBlank { null },
                city.trim(), state.trim(), pincode, lat, lng, isDefault)
            when (val r = c.addresses.save(id, req)) { is ApiResult.Ok -> onDone(); is ApiResult.Err -> error = r.message }
            busy = false
        }
    }
}

@Composable
fun AddressEditScreen(nav: NavHostController, id: Long?, showMessage: (String) -> Unit) {
    val vm = golaViewModel(key = "addr-$id") { AddressEditViewModel(it, id) }
    val unavailable = stringResource(R.string.location_unavailable)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { g ->
        if (g.values.any { it }) vm.detect { showMessage(unavailable) } else showMessage(unavailable)
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        GolaTopBar(stringResource(if (id == null) R.string.add_address else R.string.edit), { nav.popBackStack() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                enabled = !vm.detecting, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.MyLocation, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.use_current_location))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("HOME" to R.string.label_home, "WORK" to R.string.label_work, "OTHER" to R.string.label_other).forEach { (k, l) ->
                    FilterChip(selected = vm.label == k, onClick = { vm.label = k }, label = { Text(stringResource(l)) })
                }
            }
            Field(vm.line1, { vm.line1 = it.take(200) }, R.string.house_flat)
            Field(vm.line2, { vm.line2 = it.take(200) }, R.string.area_street)
            Field(vm.landmark, { vm.landmark = it.take(120) }, R.string.landmark)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { Field(vm.city, { vm.city = it.take(80) }, R.string.city) }
                Box(Modifier.weight(1f)) { Field(vm.pincode, { vm.pincode = it.filter(Char::isDigit).take(6) }, R.string.pincode, KeyboardType.Number) }
            }
            Field(vm.state, { vm.state = it.take(80) }, R.string.state)
            Field(vm.name, { vm.name = it.take(120) }, R.string.contact_name)
            Field(vm.phone, { vm.phone = it.filter(Char::isDigit).take(10) }, R.string.contact_phone, KeyboardType.Phone)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(vm.isDefault, { vm.isDefault = it }); Text(stringResource(R.string.set_default)) }
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Button(onClick = { vm.save { nav.popBackStack() } }, enabled = vm.valid && !vm.busy,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp)) { Text(stringResource(R.string.save)) }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: Int, type: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type), modifier = Modifier.fillMaxWidth())
}

// ------------------------------------------------------------------ wishlist
class WishlistViewModel(private val c: AppContainer) : ViewModel() {
    var state by mutableStateOf<Load<List<ProductCardDto>>>(Load.Loading); private set
    init { load() }
    fun load() { viewModelScope.launch { state = when (val r = c.catalog.wishlist()) { is ApiResult.Ok -> Load.Ready(r.data); is ApiResult.Err -> Load.Failed(r.message, r.isNetwork) } } }
}

@Composable
fun WishlistScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { WishlistViewModel(it) }
    DisposableEffectOnResume { vm.load() }
    val cart = rememberCartActions(showMessage) { nav.navigate(Routes.LOGIN) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        GolaTopBar(stringResource(R.string.wishlist), { nav.popBackStack() })
        LoadContent(vm.state, vm::load) { list ->
            if (list.isEmpty()) EmptyView(Icons.Outlined.FavoriteBorder, R.string.wishlist_empty)
            else LazyVerticalGrid(GridCells.Adaptive(156.dp), contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list.size) { i -> val p = list[i]; ProductCard(p, cart, { nav.navigate(Routes.product(p.id)) }, Modifier.fillMaxWidth()) }
            }
        }
    }
}

// ------------------------------------------------------------------ notifications
class NotificationsViewModel(private val c: AppContainer) : ViewModel() {
    var state by mutableStateOf<Load<List<NotificationDto>>>(Load.Loading); private set
    init { load() }
    fun load() { viewModelScope.launch { state = when (val r = c.engagement.notifications()) { is ApiResult.Ok -> Load.Ready(r.data.items); is ApiResult.Err -> Load.Failed(r.message, r.isNetwork) } } }
    fun open(n: NotificationDto) { if (!n.read) viewModelScope.launch { c.engagement.markRead(n.id); load() } }
    fun markAll() = viewModelScope.launch { c.engagement.markAllRead(); load() }
}

@Composable
fun NotificationsScreen(nav: NavHostController) {
    val vm = golaViewModel { NotificationsViewModel(it) }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.notifications), { nav.popBackStack() }) {
            TextButton(onClick = { vm.markAll() }) { Text(stringResource(R.string.mark_all_read)) }
        }
        LoadContent(vm.state, vm::load) { list ->
            if (list.isEmpty()) EmptyView(Icons.Outlined.NotificationsNone, R.string.no_notifications)
            else LazyColumn {
                items(list, key = { it.id }) { n ->
                    GolaCard(Modifier.clickable {
                        vm.open(n)
                        if (n.type == "ORDER") n.reference?.toLongOrNull()?.let { nav.navigate(Routes.order(it)) }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!n.read) Box(Modifier.size(8.dp).clip(CircleShape).background(GolaColors.Green))
                            Spacer(Modifier.width(if (n.read) 0.dp else 8.dp))
                            Text(n.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        }
                        Text(n.body, style = MaterialTheme.typography.bodyMedium)
                        Text(formatDate(n.createdAt), style = MaterialTheme.typography.labelSmall, color = GolaColors.Grey)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ support
class SupportViewModel(private val c: AppContainer) : ViewModel() {
    var tickets by mutableStateOf<Load<List<TicketDto>>>(Load.Loading); private set
    var contact by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var sending by mutableStateOf(false); private set
    init { load() }
    fun load() {
        viewModelScope.launch {
            (c.engagement.contact() as? ApiResult.Ok)?.let { contact = it.data }
            tickets = when (val r = c.engagement.tickets()) { is ApiResult.Ok -> Load.Ready(r.data); is ApiResult.Err -> Load.Failed(r.message, r.isNetwork) }
        }
    }
    fun send(subject: String, message: String, onDone: () -> Unit, onError: (String) -> Unit) {
        sending = true
        viewModelScope.launch {
            when (val r = c.engagement.createTicket(null, subject, message)) { is ApiResult.Ok -> { onDone(); load() }; is ApiResult.Err -> onError(r.message) }
            sending = false
        }
    }
}

@Composable
fun SupportScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { SupportViewModel(it) }
    val ctx = LocalContext.current
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(GolaColors.Surface)) {
        GolaTopBar(stringResource(R.string.support), { nav.popBackStack() })
        LazyColumn(Modifier.weight(1f)) {
            item {
                GolaCard {
                    Text(stringResource(R.string.contact_us), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        vm.contact["phone"]?.takeIf { it.isNotBlank() }?.let { p ->
                            OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$p"))) }) { Icon(Icons.Default.Call, null); Spacer(Modifier.width(4.dp)); Text(p) }
                        }
                        vm.contact["email"]?.takeIf { it.isNotBlank() }?.let { e ->
                            OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$e"))) }) { Icon(Icons.Default.Email, null) }
                        }
                    }
                }
            }
            item {
                GolaCard {
                    Text(stringResource(R.string.raise_ticket), style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(subject, { subject = it.take(150) }, label = { Text(stringResource(R.string.subject)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(message, { message = it.take(2000) }, label = { Text(stringResource(R.string.message)) }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { vm.send(subject, message, { subject = ""; message = ""; showMessage("Ticket submitted") }, showMessage) },
                        enabled = subject.isNotBlank() && message.isNotBlank() && !vm.sending, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.submit)) }
                }
            }
            item { SectionTitle(stringResource(R.string.my_tickets)) }
            when (val t = vm.tickets) {
                is Load.Ready -> items(t.data, key = { it.id }) { tk ->
                    GolaCard {
                        Row { Text("#${tk.id} ${tk.subject}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); StatusChip(tk.status) }
                        Text(tk.message, style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                        tk.adminReply?.let { Text("${stringResource(R.string.support_reply)}: $it", style = MaterialTheme.typography.bodyMedium, color = GolaColors.GreenDark) }
                    }
                }
                is Load.Failed -> item { Text(t.message, color = GolaColors.Red, modifier = Modifier.padding(16.dp)) }
                Load.Loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp)) }
            }
        }
    }
}
