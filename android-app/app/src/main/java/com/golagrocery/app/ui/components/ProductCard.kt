package com.golagrocery.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.ProductCardDto
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.launch
import java.util.Locale

/** Callback bundle so cards can update the cart and report errors without each screen re-implementing it. */
class CartActions(val qtyOf: (Long) -> Int, val isBusy: (Long) -> Boolean, val setQty: (Long, Int) -> Unit)

@Composable
fun rememberCartActions(onError: (String) -> Unit, onLoginRequired: () -> Unit): CartActions {
    val container = appContainer()
    val quote by container.cart.quote.collectAsState()
    val pending by container.cart.pending.collectAsState()
    val scope = rememberCoroutineScope()
    val loggedIn = container.session.session.collectAsState().value != null
    return remember(quote, pending, loggedIn) {
        CartActions(
            qtyOf = { id -> quote?.lines?.firstOrNull { it.variantId == id }?.quantity ?: 0 },
            isBusy = { id -> id in pending },
            setQty = { id, q ->
                if (!loggedIn) onLoginRequired()
                else scope.launch {
                    val r = container.cart.setQuantity(id, q)
                    if (r is ApiResult.Err) onError(r.message)
                }
            },
        )
    }
}

@Composable
fun localizedName(name: String, nameHi: String?): String =
    if (!nameHi.isNullOrBlank() && Locale.getDefault().language == "hi") nameHi else name

@Composable
fun ProductCard(p: ProductCardDto, cart: CartActions, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val v = p.variants.firstOrNull()
    Column(
        modifier.width(156.dp).clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)).clickable(onClick = onOpen).padding(10.dp)
    ) {
        Box {
            NetImage(p.imageUrl, p.name, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)))
            if (v != null) DiscountTag(v.discountPercent, Modifier.align(Alignment.TopStart))
        }
        Spacer(Modifier.height(6.dp))
        RatingPill(p.avgRating, p.ratingCount)
        Text(localizedName(p.name, p.nameHi), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 40.dp).padding(top = 2.dp))
        Text(v?.label.orEmpty() + if (p.variants.size > 1) " · ${p.variants.size} options" else "", style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (v != null) {
                    Text(rupees(v.price), style = MaterialTheme.typography.titleSmall)
                    if (v.mrp > v.price) Text(rupees(v.mrp), style = MaterialTheme.typography.bodySmall, color = GolaColors.Grey,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                }
            }
            if (v == null || !v.inStock) {
                Text(stringResource(R.string.out_of_stock), style = MaterialTheme.typography.labelSmall, color = GolaColors.Red)
            } else if (p.variants.size > 1 && cart.qtyOf(v.id) == 0) {
                // Multiple pack sizes: open the product to choose.
                QuantityStepper(0, false, true, { onOpen() }, v.availableQty)
            } else {
                QuantityStepper(cart.qtyOf(v.id), cart.isBusy(v.id), v.inStock, { cart.setQty(v.id, it) }, v.availableQty)
            }
        }
    }
}

@Composable
fun ProductRow(products: List<ProductCardDto>, cart: CartActions, onOpen: (Long) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(products, key = { it.id }) { p -> ProductCard(p, cart, { onOpen(p.id) }) }
    }
}
