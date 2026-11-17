package com.golagrocery.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.PincodeCheck
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.ui.components.golaViewModel
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.navigation.goRoot
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.launch

class LocationViewModel(private val c: AppContainer) : ViewModel() {
    var pincode by mutableStateOf(c.session.location.value?.pincode ?: "")
    var busy by mutableStateOf(false); private set
    var detecting by mutableStateOf(false); private set
    var result by mutableStateOf<PincodeCheck?>(null); private set
    var error by mutableStateOf<String?>(null); private set

    fun check() {
        if (!Regex("^[1-9]\\d{5}$").matches(pincode)) { error = "Please enter a valid 6-digit pincode"; return }
        busy = true; error = null; result = null
        viewModelScope.launch {
            when (val r = c.locationRepo.check(pincode)) {
                is ApiResult.Ok -> result = r.data
                is ApiResult.Err -> error = r.message
            }
            busy = false
        }
    }

    fun detect(onUnavailable: () -> Unit) {
        detecting = true
        viewModelScope.launch {
            val d = c.location.detect()
            detecting = false
            if (d?.pincode == null) onUnavailable() else { pincode = d.pincode; check() }
        }
    }

    fun confirm(nav: NavHostController) {
        val r = result ?: return
        viewModelScope.launch {
            c.locationRepo.select(r)
            c.cart.refresh()
            if (nav.previousBackStackEntry != null) nav.popBackStack() else nav.goRoot(Routes.HOME)
        }
    }
}

@Composable
fun LocationScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { LocationViewModel(it) }
    val denied = stringResource(R.string.location_permission_denied)
    val unavailable = stringResource(R.string.location_unavailable)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) vm.detect { showMessage(unavailable) } else showMessage(denied)
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(GolaColors.GreenLight), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Place, null, tint = GolaColors.Green, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.select_location), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
            enabled = !vm.detecting, modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (vm.detecting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else { Icon(Icons.Default.MyLocation, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.use_current_location)) }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = vm.pincode, onValueChange = { v -> vm.pincode = v.filter(Char::isDigit).take(6) },
                label = { Text(stringResource(R.string.enter_pincode)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = vm::check, enabled = vm.pincode.length == 6 && !vm.busy, modifier = Modifier.height(56.dp)) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color.White)
                else Text(stringResource(R.string.check))
            }
        }
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        AnimatedVisibility(vm.result != null) {
            val r = vm.result
            if (r != null) Card(Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(
                containerColor = if (r.serviceable) GolaColors.GreenLight else GolaColors.YellowLight)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (r.serviceable) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null,
                        tint = if (r.serviceable) GolaColors.Green else GolaColors.Red)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(if (r.serviceable) listOfNotNull(r.area, r.city).joinToString(", ") else stringResource(R.string.not_serviceable),
                            style = MaterialTheme.typography.titleSmall)
                        if (r.serviceable) Text(stringResource(R.string.delivery_in, r.etaMinutes ?: 30), color = GolaColors.GreenDark)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = { vm.confirm(nav) }, enabled = vm.result?.serviceable == true, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.continue_))
        }
    }
}
