package com.golagrocery.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.BannerDto
import com.golagrocery.app.data.remote.CategoryDto
import com.golagrocery.app.data.remote.HomeDto
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.ui.components.*
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    var state by mutableStateOf<Load<HomeDto>>(Load.Loading); private set
    var unread by mutableLongStateOf(0L); private set
    init { load() }
    fun load() {
        if (state !is Load.Ready) state = Load.Loading
        viewModelScope.launch {
            state = when (val r = c.catalog.home()) {
                is ApiResult.Ok -> Load.Ready(r.data)
                is ApiResult.Err -> if (state is Load.Ready) state else Load.Failed(r.message, r.isNetwork)
            }
            (c.engagement.unreadCount() as? ApiResult.Ok)?.let { unread = it.data.count }
            c.cart.refresh()
        }
    }
}

@Composable
fun HomeScreen(nav: NavHostController, showMessage: (String) -> Unit) {
    val vm = golaViewModel { HomeViewModel(it) }
    val container = appContainer()
    val loc by container.session.location.collectAsState()
    val cart = rememberCartActions(showMessage) { nav.navigate(Routes.LOGIN) }
    val quote by container.cart.quote.collectAsState()
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column {
            // Header
            Column(Modifier.background(Brush.verticalGradient(listOf(GolaColors.YellowLight, Color.White))).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { nav.navigate(Routes.LOCATION) }) {
                        Text(stringResource(R.string.delivery_in, loc?.etaMinutes ?: 30), style = MaterialTheme.typography.titleLarge, color = GolaColors.GreenDark)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${stringResource(R.string.deliver_to)}: ${loc?.label ?: "—"} ${loc?.pincode ?: ""}", style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = { nav.navigate(Routes.NOTIFICATIONS) }) {
                        BadgedBox(badge = { if (vm.unread > 0) Badge { Text("${vm.unread}") } }) { Icon(Icons.Outlined.Notifications, stringResource(R.string.notifications)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).clickable { nav.navigate(Routes.SEARCH) }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Search, null, tint = GolaColors.Grey)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.search_hint), color = GolaColors.Grey)
                }
            }
            LoadContent(vm.state, vm::load) { home ->
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    if (home.banners.isNotEmpty()) item { BannerCarousel(home.banners) { b -> openBanner(b, home, nav) } }
                    item { SectionTitle(stringResource(R.string.shop_by_category)) }
                    item { CategoryGrid(home.categories) { c -> nav.navigate(Routes.products(c.id, c.name)) } }
                    if (home.deals.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.deals)) { TextButton(onClick = { nav.navigate(Routes.SEARCH) }) { Text(stringResource(R.string.see_all)) } } }
                        item { ProductRow(home.deals, cart) { nav.navigate(Routes.product(it)) } }
                    }
                    if (home.featured.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.featured)) }
                        item { ProductRow(home.featured, cart) { nav.navigate(Routes.product(it)) } }
                    }
                    items(home.sections, key = { "s" + it.categoryId }) { s ->
                        SectionTitle(s.title) { TextButton(onClick = { nav.navigate(Routes.products(s.categoryId, s.title)) }) { Text(stringResource(R.string.see_all)) } }
                        ProductRow(s.products, cart) { nav.navigate(Routes.product(it)) }
                    }
                    item {
                        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.titleMedium, color = GolaColors.Grey.copy(alpha = .5f),
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
                    }
                }
            }
        }
        // Floating cart bar
        val count = quote?.itemCount ?: 0
        AnimatedVisibility(count > 0, modifier = Modifier.align(Alignment.BottomCenter), enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            FloatingCartBar(count, quote?.grandTotal ?: 0.0) { nav.navigate(Routes.CART) }
        }
    }
}

private fun openBanner(b: BannerDto, home: HomeDto, nav: NavHostController) {
    when (b.linkType) {
        "CATEGORY" -> home.categories.firstOrNull { it.slug == b.linkValue }?.let { nav.navigate(Routes.products(it.id, it.name)) }
        "PRODUCT" -> b.linkValue?.toLongOrNull()?.let { nav.navigate(Routes.product(it)) }
        "COUPON" -> nav.navigate(Routes.CART)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarousel(banners: List<BannerDto>, onClick: (BannerDto) -> Unit) {
    val pager = rememberPagerState { banners.size }
    LaunchedEffect(banners.size) {
        while (banners.size > 1) {
            delay(3500)
            pager.animateScrollToPage((pager.currentPage + 1) % banners.size)
        }
    }
    Column {
        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 10.dp, modifier = Modifier.padding(top = 12.dp)) { i ->
            NetImage(banners[i].imageUrl, banners[i].title,
                Modifier.fillMaxWidth().aspectRatio(12f / 5f).clip(RoundedCornerShape(16.dp)).clickable { onClick(banners[i]) })
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
            repeat(banners.size) { i ->
                Box(Modifier.padding(3.dp).size(if (pager.currentPage == i) 8.dp else 6.dp).clip(CircleShape)
                    .background(if (pager.currentPage == i) GolaColors.Green else GolaColors.Grey.copy(alpha = .3f)))
            }
        }
    }
}

@Composable
fun CategoryGrid(categories: List<CategoryDto>, onClick: (CategoryDto) -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp)) {
        categories.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { c ->
                    Column(Modifier.weight(1f).padding(4.dp).clip(RoundedCornerShape(12.dp)).clickable { onClick(c) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        NetImage(c.imageUrl, c.name, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(GolaColors.GreenLight))
                        Text(localizedName(c.name, c.nameHi), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2,
                            minLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun FloatingCartBar(count: Int, total: Double, onClick: () -> Unit) {
    Row(
        Modifier.padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GolaColors.Green).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.items_count, count), color = Color.White, style = MaterialTheme.typography.labelLarge)
            Text(rupees(total), color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Text(stringResource(R.string.go_to_cart), color = Color.White, style = MaterialTheme.typography.titleSmall)
        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White)
    }
}
