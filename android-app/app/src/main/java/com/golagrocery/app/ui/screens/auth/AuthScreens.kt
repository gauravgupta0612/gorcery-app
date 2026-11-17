package com.golagrocery.app.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.ui.components.golaViewModel
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val PHONE_REGEX = Regex("^[6-9]\\d{9}$")

class AuthViewModel(private val c: AppContainer) : ViewModel() {
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)

    fun loginPassword(phone: String, password: String, nav: NavHostController) = launchOp {
        when (val r = c.auth.login(phone, password)) {
            is ApiResult.Ok -> routeSignedIn(c, nav, r.data)
            is ApiResult.Err -> error = r.message
        }
    }

    fun requestLoginOtp(phone: String, nav: NavHostController) = launchOp {
        when (val r = c.auth.requestOtp(phone, "LOGIN")) {
            is ApiResult.Ok -> nav.navigate(Routes.otp(phone, "LOGIN", r.data.devOtp))
            is ApiResult.Err -> error = if (r.status == 404) "No account found for this number. Please create one." else r.message
        }
    }

    fun register(name: String, phone: String, email: String, password: String, nav: NavHostController) = launchOp {
        when (val r = c.auth.register(name, phone, email, password)) {
            is ApiResult.Ok -> nav.navigate(Routes.otp(phone, "REGISTER", r.data.devOtp))
            is ApiResult.Err -> error = r.message
        }
    }

    fun verify(phone: String, code: String, purpose: String, nav: NavHostController) = launchOp {
        when (val r = c.auth.verifyOtp(phone, code, purpose)) {
            is ApiResult.Ok -> routeSignedIn(c, nav, r.data)
            is ApiResult.Err -> error = r.message
        }
    }

    fun resend(phone: String, purpose: String, onSent: (String?) -> Unit) = launchOp {
        when (val r = c.auth.requestOtp(phone, purpose)) {
            is ApiResult.Ok -> onSent(r.data.devOtp)
            is ApiResult.Err -> error = r.message
        }
    }

    private fun launchOp(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        viewModelScope.launch { try { block() } finally { busy = false } }
    }
}

@Composable
private fun AuthScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).imePadding()) {
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(Brush.linearGradient(listOf(GolaColors.Green, GolaColors.GreenDark))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.statusBarsPadding()) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(96.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium, color = GolaColors.Yellow)
            }
        }
        Column(Modifier.padding(24.dp), content = content)
    }
}

@Composable
fun PhoneField(value: String, onChange: (String) -> Unit, isError: Boolean) {
    OutlinedTextField(
        value = value, onValueChange = { v -> onChange(v.filter(Char::isDigit).take(10)) },
        label = { Text(stringResource(R.string.mobile_number)) }, prefix = { Text("+91 ") }, singleLine = true,
        isError = isError, supportingText = if (isError) ({ Text(stringResource(R.string.invalid_phone)) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun PasswordField(value: String, onChange: (String) -> Unit, label: String = stringResource(R.string.password), hint: String? = null) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
        supportingText = hint?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun LoginScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { AuthViewModel(it) }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var otpMode by rememberSaveable { mutableStateOf(true) }
    val phoneOk = PHONE_REGEX.matches(phone)
    AuthScaffold {
        Text(stringResource(R.string.login), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        PhoneField(phone, { phone = it }, phone.length == 10 && !phoneOk)
        AnimatedContent(otpMode, label = "mode") { otp ->
            if (!otp) Column { Spacer(Modifier.height(8.dp)); PasswordField(password, { password = it }) }
            else Spacer(Modifier.height(1.dp))
        }
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (otpMode) vm.requestLoginOtp(phone, nav) else vm.loginPassword(phone, password, nav) },
            enabled = phoneOk && (otpMode || password.isNotEmpty()) && !vm.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(stringResource(if (otpMode) R.string.send_otp else R.string.login))
        }
        TextButton(onClick = { otpMode = !otpMode; vm.error = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(if (otpMode) R.string.login_with_password else R.string.login_with_otp))
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        TextButton(onClick = { nav.navigate(Routes.REGISTER) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.no_account), fontWeight = FontWeight.Bold)
        }
        Text("Delivery partners: sign in with your mobile number and password.", style = MaterialTheme.typography.bodySmall,
            color = GolaColors.Grey, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun RegisterScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { AuthViewModel(it) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val valid = name.trim().length >= 2 && PHONE_REGEX.matches(phone) && password.length >= 8 &&
        (email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches())
    AuthScaffold {
        Text(stringResource(R.string.register), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(name, { name = it.take(120) }, label = { Text(stringResource(R.string.full_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        PhoneField(phone, { phone = it }, phone.length == 10 && !PHONE_REGEX.matches(phone))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(email, { email = it.take(160) }, label = { Text(stringResource(R.string.email_optional)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        PasswordField(password, { password = it.take(72) }, hint = stringResource(R.string.password_rule))
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.register(name, phone, email, password, nav) }, enabled = valid && !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text(stringResource(R.string.send_otp))
        }
        TextButton(onClick = { nav.popBackStack() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.have_account)) }
    }
}

@Composable
fun OtpScreen(nav: NavHostController, phone: String, purpose: String, devOtp: String?, showMessage: (String) -> Unit) {
    val vm = golaViewModel { AuthViewModel(it) }
    var code by rememberSaveable { mutableStateOf("") }
    var dev by rememberSaveable { mutableStateOf(devOtp) }
    var seconds by remember { mutableIntStateOf(30) }
    LaunchedEffect(seconds) { if (seconds > 0) { delay(1000); seconds-- } }
    LaunchedEffect(code) { if (code.length == 6 && !vm.busy) vm.verify(phone, code, purpose, nav) }
    AuthScaffold {
        Text(stringResource(R.string.verify), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.enter_otp, "+91 $phone"), color = GolaColors.Grey)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = code, onValueChange = { v -> code = v.filter(Char::isDigit).take(6) }, singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth(),
        )
        dev?.let {
            Spacer(Modifier.height(8.dp))
            Surface(color = GolaColors.YellowLight, shape = RoundedCornerShape(8.dp)) {
                Text(stringResource(R.string.dev_otp_hint, it), modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.verify(phone, code, purpose, nav) }, enabled = code.length == 6 && !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text(stringResource(R.string.verify))
        }
        TextButton(enabled = seconds == 0 && !vm.busy, onClick = { vm.resend(phone, purpose) { d -> dev = d; seconds = 30; code = "" } },
            modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (seconds > 0) stringResource(R.string.resend_in, seconds) else stringResource(R.string.resend_otp))
        }
    }
}
