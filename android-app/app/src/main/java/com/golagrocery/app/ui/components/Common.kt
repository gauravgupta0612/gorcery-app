package com.golagrocery.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.golagrocery.app.R
import com.golagrocery.app.data.remote.Network
import com.golagrocery.app.ui.theme.GolaColors
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Generic async UI state. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val data: T) : Load<T>
    data class Failed(val message: String, val network: Boolean = false) : Load<Nothing>
}

private val inr: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
fun rupees(v: Double): String = inr.format(v).replace(".00", "")
fun plain(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.2f", v)
private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale("en", "IN")).withZone(ZoneId.of("Asia/Kolkata"))
fun formatDate(iso: String?): String = iso?.let { runCatching { dateFmt.format(Instant.parse(it)) }.getOrDefault(it) } ?: ""

@Composable
fun <T> LoadContent(state: Load<T>, onRetry: () -> Unit, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    AnimatedContent(targetState = state, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
        contentKey = { it::class }, label = "load", modifier = modifier) { s ->
        when (s) {
            is Load.Loading -> LoadingView()
            is Load.Failed -> ErrorView(if (s.network) stringResource(R.string.no_internet) else s.message, onRetry)
            is Load.Ready -> content(s.data)
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CloudOff, null, tint = GolaColors.Grey, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.something_wrong), style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = GolaColors.Grey, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
fun EmptyView(icon: ImageVector, @StringRes title: Int, hint: String? = null, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(GolaColors.GreenLight), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = GolaColors.Green, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = GolaColors.Grey, textAlign = TextAlign.Center) }
        action?.let { Spacer(Modifier.height(16.dp)); it() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GolaTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1) },
        navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
    )
}

@Composable
fun NetImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    SubcomposeAsyncImage(
        model = Network.imageUrl(url),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { Box(Modifier.fillMaxSize().background(GolaColors.Surface)) },
        error = {
            Box(Modifier.fillMaxSize().background(GolaColors.GreenLight), contentAlignment = Alignment.Center) {
                Text("🥬", style = MaterialTheme.typography.headlineMedium)
            }
        },
    )
}

@Composable
fun PriceRow(price: Double, mrp: Double, modifier: Modifier = Modifier, big: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(rupees(price), style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (mrp > price) {
            Spacer(Modifier.width(6.dp))
            Text(rupees(mrp), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey, textDecoration = TextDecoration.LineThrough)
        }
    }
}

@Composable
fun DiscountTag(percent: Int, modifier: Modifier = Modifier) {
    if (percent <= 0) return
    Box(modifier.clip(RoundedCornerShape(bottomEnd = 10.dp, topStart = 10.dp)).background(GolaColors.Discount).padding(horizontal = 6.dp, vertical = 3.dp)) {
        Text(stringResource(R.string.off, percent), color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun RatingPill(rating: Double, count: Int) {
    if (count <= 0) return
    Row(Modifier.clip(RoundedCornerShape(6.dp)).background(GolaColors.GreenLight).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, null, tint = GolaColors.Green, modifier = Modifier.size(12.dp))
        Text(" ${"%.1f".format(Locale.US, rating)} ($count)", style = MaterialTheme.typography.labelSmall, color = GolaColors.GreenDark)
    }
}

/** ADD button that turns into a − qty + stepper; disabled while a request for this item is in flight. */
@Composable
fun QuantityStepper(qty: Int, busy: Boolean, enabled: Boolean, onChange: (Int) -> Unit, max: Int, modifier: Modifier = Modifier, wide: Boolean = false) {
    val alpha by animateFloatAsState(if (busy) 0.6f else 1f, label = "busy")
    val width = if (wide) Modifier.fillMaxWidth() else Modifier.width(84.dp)
    AnimatedContent(targetState = qty == 0, label = "stepper", modifier = modifier.alpha(alpha)) { isZero ->
        if (isZero) {
            OutlinedButton(
                onClick = { onChange(1) }, enabled = enabled && !busy, modifier = width.height(36.dp),
                contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = GolaColors.GreenLight, contentColor = GolaColors.Green),
                border = androidx.compose.foundation.BorderStroke(1.dp, GolaColors.Green),
            ) { Text(stringResource(R.string.add), fontWeight = FontWeight.ExtraBold) }
        } else {
            Row(width.height(36.dp).clip(RoundedCornerShape(8.dp)).background(GolaColors.Green), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onChange(qty - 1) }, enabled = !busy, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(30.dp)) {
                    Text("−", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Text("$qty", color = Color.White, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onChange(qty + 1) }, enabled = !busy && qty < max, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(30.dp)) {
                    Text("+", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun StatusChip(status: String) {
    val (bg, fg) = when (status) {
        "DELIVERED", "PAID", "PROCESSED" -> GolaColors.GreenLight to GolaColors.GreenDark
        "CANCELLED", "PAYMENT_FAILED", "FAILED" -> Color(0xFFFDECEA) to GolaColors.Red
        "OUT_FOR_DELIVERY", "ASSIGNED", "PICKED_UP" -> Color(0xFFE8F0FE) to GolaColors.Blue
        else -> GolaColors.YellowLight to Color(0xFF6B5400)
    }
    Text(orderStatusLabel(status), color = fg, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
fun orderStatusLabel(status: String): String = when (status) {
    "PENDING_PAYMENT" -> stringResource(R.string.st_PENDING_PAYMENT)
    "PLACED" -> stringResource(R.string.st_PLACED)
    "CONFIRMED" -> stringResource(R.string.st_CONFIRMED)
    "PREPARING" -> stringResource(R.string.st_PREPARING)
    "PACKED" -> stringResource(R.string.st_PACKED)
    "ASSIGNED" -> stringResource(R.string.st_ASSIGNED)
    "OUT_FOR_DELIVERY" -> stringResource(R.string.st_OUT_FOR_DELIVERY)
    "DELIVERED" -> stringResource(R.string.st_DELIVERED)
    "CANCELLED" -> stringResource(R.string.st_CANCELLED)
    "PAYMENT_FAILED" -> stringResource(R.string.st_PAYMENT_FAILED)
    else -> status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
fun GolaCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(14.dp))
        .background(Color.White).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)).padding(14.dp), content = content)
}

@Composable
fun BillRow(label: String, value: String, bold: Boolean = false, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.weight(1f), style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
        Text(value, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
