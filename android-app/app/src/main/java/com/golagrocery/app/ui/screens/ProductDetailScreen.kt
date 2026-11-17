package com.golagrocery.app.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.ProductDetailDto
import com.golagrocery.app.data.remote.ReviewDto
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.ui.components.*
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.launch

class ProductDetailViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    var state by mutableStateOf<Load<ProductDetailDto>>(Load.Loading); private set
    var reviews by mutableStateOf<List<ReviewDto>>(emptyList()); private set
    var selectedVariant by mutableLongStateOf(-1L)
    var wishlisted by mutableStateOf(false); private set
    init { load() }
    fun load() {
        state = Load.Loading
        viewModelScope.launch {
            when (val r = c.catalog.product(id)) {
                is ApiResult.Ok -> {
                    state = Load.Ready(r.data); wishlisted = r.data.inWishlist
                    if (selectedVariant < 0) selectedVariant = r.data.variants.firstOrNull { it.inStock }?.id ?: r.data.variants.firstOrNull()?.id ?: -1
                }
                is ApiResult.Err -> state = Load.Failed(r.message, r.isNetwork)
            }
            (c.catalog.reviews(id) as? ApiResult.Ok)?.let { reviews = it.data.items }
        }
    }
    fun toggleWishlist(onError: (String) -> Unit) {
        val target = !wishlisted
        wishlisted = target
        viewModelScope.launch {
            val r = c.catalog.setWishlisted(id, target)
            if (r is ApiResult.Err) { wishlisted = !target; onError(r.message) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(nav: NavHostController, id: Long, showMessage: (String) -> Unit) {
    val vm = golaViewModel(key = "product-$id") { ProductDetailViewModel(it, id) }
    val cart = rememberCartActions(showMessage) { nav.navigate(Routes.LOGIN) }
    val quote by appContainer().cart.quote.collectAsState()
    Column(Modifier.fillMaxSize().background(Color.White)) {
        GolaTopBar(stringResource(R.string.product_details), { nav.popBackStack() }) {
            IconButton(onClick = { vm.toggleWishlist(showMessage) }) {
                Icon(if (vm.wishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder, stringResource(R.string.wishlist),
                    tint = if (vm.wishlisted) GolaColors.Red else Color.Unspecified)
            }
        }
        LoadContent(vm.state, vm::load, Modifier.weight(1f)) { p ->
            val variant = p.variants.firstOrNull { it.id == vm.selectedVariant } ?: p.variants.firstOrNull()
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    val images = p.images.ifEmpty { listOf("") }
                    val pager = rememberPagerState { images.size }
                    Box {
                        HorizontalPager(pager) { i ->
                            NetImage(images[i], p.name, Modifier.fillMaxWidth().aspectRatio(1f).padding(24.dp).clip(RoundedCornerShape(20.dp)), ContentScale.Fit)
                        }
                        if (variant != null) DiscountTag(variant.discountPercent, Modifier.padding(24.dp))
                        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) {
                            repeat(images.size) { i -> Box(Modifier.padding(3.dp).size(7.dp).clip(CircleShape).background(if (pager.currentPage == i) GolaColors.Green else GolaColors.Grey.copy(alpha = .3f))) }
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        p.brand?.let { Text(it, color = GolaColors.Green, style = MaterialTheme.typography.labelLarge) }
                        Text(localizedName(p.name, p.nameHi), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        RatingPill(p.avgRating, p.ratingCount)
                        Spacer(Modifier.height(8.dp))
                        if (variant != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PriceRow(variant.price, variant.mrp, big = true)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.incl_taxes), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                            }
                            if (variant.mrp > variant.price) Text("${stringResource(R.string.mrp)} ${rupees(variant.mrp)}", style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.select_variant), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.variants.forEach { v ->
                                val sel = v.id == variant?.id
                                Column(
                                    Modifier.clip(RoundedCornerShape(12.dp)).border(if (sel) 2.dp else 1.dp, if (sel) GolaColors.Green else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                        .background(if (sel) GolaColors.GreenLight else Color.White).clickable { vm.selectedVariant = v.id }.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(v.label, style = MaterialTheme.typography.titleSmall)
                                    Text(rupees(v.price), style = MaterialTheme.typography.bodySmall)
                                    if (!v.inStock) Text(stringResource(R.string.out_of_stock), color = GolaColors.Red, style = MaterialTheme.typography.labelSmall)
                                    else if (v.discountPercent > 0) Text(stringResource(R.string.off, v.discountPercent), color = GolaColors.Discount, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (!p.description.isNullOrBlank()) {
                            var expanded by remember { mutableStateOf(false) }
                            Text(stringResource(R.string.description), style = MaterialTheme.typography.titleSmall)
                            Text(p.description, style = MaterialTheme.typography.bodyMedium, color = GolaColors.Grey, maxLines = if (expanded) Int.MAX_VALUE else 3,
                                modifier = Modifier.animateContentSize().clickable { expanded = !expanded })
                            Spacer(Modifier.height(16.dp))
                        }
                        Text(stringResource(R.string.ratings_reviews), style = MaterialTheme.typography.titleSmall)
                        if (vm.reviews.isEmpty()) Text(stringResource(R.string.no_reviews), color = GolaColors.Grey, style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(vm.reviews.take(5), key = { it.id }) { r ->
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { i -> Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = if (i < r.rating) GolaColors.Yellow else GolaColors.Grey.copy(alpha = .3f)) }
                            Spacer(Modifier.width(8.dp)); Text(r.userName, style = MaterialTheme.typography.labelLarge)
                        }
                        r.title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                        r.comment?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                if (p.similar.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.similar_products)) }
                    item { ProductRow(p.similar, cart) { nav.navigate(Routes.product(it)) } }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
        // Bottom add-to-cart bar
        val ready = vm.state as? Load.Ready
        val v = ready?.data?.variants?.firstOrNull { it.id == vm.selectedVariant }
        if (v != null) {
            Surface(shadowElevation = 12.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(v.label, style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey)
                        PriceRow(v.price, v.mrp)
                    }
                    if (!v.inStock) {
                        Text(stringResource(R.string.out_of_stock), color = GolaColors.Red, style = MaterialTheme.typography.titleSmall)
                    } else {
                        val inCart = cart.qtyOf(v.id)
                        QuantityStepper(inCart, cart.isBusy(v.id), true, { cart.setQty(v.id, it) }, v.availableQty, Modifier.width(140.dp), wide = true)
                        if (inCart > 0 && (quote?.itemCount ?: 0) > 0) {
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { nav.navigate(Routes.CART) }) { Text(stringResource(R.string.go_to_cart)) }
                        }
                    }
                }
            }
        }
    }
}
