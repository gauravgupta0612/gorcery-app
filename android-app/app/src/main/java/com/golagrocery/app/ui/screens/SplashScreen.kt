package com.golagrocery.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.ui.components.appContainer
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.navigation.goRoot
import com.golagrocery.app.ui.screens.auth.routeSignedIn
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(nav: NavHostController) {
    val container = appContainer()
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(400))
    }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        container.session.load()
        delay(900)
        val s = container.session.session.value
        when {
            s != null -> {
                val user = (container.auth.refreshProfile() as? ApiResult.Ok)?.data ?: s.user
                if (container.session.session.value == null) nav.goRoot(Routes.LOGIN) else routeSignedIn(container, nav, user)
            }
            !container.session.onboardingDone -> nav.goRoot(Routes.ONBOARDING)
            else -> nav.goRoot(Routes.LOGIN)
        }
    }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GolaColors.Green, GolaColors.GreenDark))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale.value).alpha(alpha.value)) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(160.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyLarge, color = GolaColors.Yellow)
        }
    }
}
