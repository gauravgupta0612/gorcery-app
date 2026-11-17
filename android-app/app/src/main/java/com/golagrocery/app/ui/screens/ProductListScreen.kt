package com.golagrocery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.BrandDto
import com.golagrocery.app.data.remote.ProductCardDto
import com.golagrocery.app.data.repository.ProductFilter
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.ui.components.*
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProductListViewModel(private val c: AppContainer, categoryId: Long?, query: String) : ViewModel() {
    var filter by mutableStateOf(ProductFilter(query = query.ifBlank { null }, categoryId = categoryId)); private set
    var items by mutableStateOf<List<ProductCardDto>>(emptyList()); private set
    var state by mutableStateOf<Load<Unit>>(Load.Loading); private set
    var loadingMore by mutableStateOf(false); private set
    var brands by mutableStateOf<List<BrandDto>>(emptyList()); private set
    private var page = 0
    private var totalPages = 1
    private var job: Job? = null

    init {
        if (categoryId != null || query.isNotBlank()) reload() else state = Load.Ready(Unit)
        viewModelScope.launch { (c.catalog.brands(categoryId) as? ApiResult.Ok)?.let { brands = it.data } }
    }

    fun setQuery(q: String) {
        filter = filter.copy(query = q)
        job?.cancel()
        job = viewModelScope.launch {
            delay(350) // debounce typing
            if (q.trim().length >= 2 || filter.categoryId != null) doLoad(reset = true) else { items = emptyList(); state = Load.Ready(Unit) }
        }
    }

    fun apply(f: ProductFilter) { filter = f; reload() }

    fun reload() { job?.cancel(); job = viewModelScope.launch { doLoad(reset = true) } }

    fun loadMore() {
        if (loadingMore || page + 1 >= totalPages || state !is Load.Ready) return
        loadingMore = true
        viewModelScope.launch {
            when (val r = c.catalog.search(filter, page + 1)) {
                is ApiResult.Ok -> { page = r.data.page; totalPages = r.data.totalPages; items = items + r.data.items }
                is ApiResult.Err -> Unit
            }
            loadingMore = false
        }
    }

    private suspend fun doLoad(reset: Boolean) {
        if (reset) { state = Load.Loading; page = 0 }
        when (val r = c.catalog.search(filter, 0)) {
            is ApiResult.Ok -> { items = r.data.items; totalPages = r.data.totalPages; page = 0; state = Load.Ready(Unit) }
            is ApiResult.Err -> state = Load.Failed(r.message, r.isNetwork)
        }
    }
}

private val SORTS = listOf("relevance" to R.string.sort_relevance, "price_asc" to R.string.sort_price_asc, "price_desc" to R.string.sort_price_desc,
    "discount" to R.string.sort_discount, "rating" to R.string.sort_rating, "newest" to R.string.sort_newest)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(nav: NavHostController, categoryId: Long?, title: String, query: String, searchMode: Boolean, showMessage: (String) -> Unit) {
    val vm = golaViewModel(key = "plist-$categoryId-$query-$searchMode") { ProductListViewModel(it, categoryId, query) }
    val cart = rememberCartActions(showMessage) { nav.navigate(Routes.LOGIN) }
    var showFilters by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    val grid = rememberLazyGridState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(searchMode) { if (searchMode) runCatching { focus.requestFocus() } }
    val nearEnd by remember { derivedStateOf { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 >= vm.items.size - 4 } }
    LaunchedEffect(nearEnd) { if (nearEnd) vm.loadMore() }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        if (searchMode) {
            Row(Modifier.statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                OutlinedTextField(
                    value = vm.filter.query.orEmpty(), onValueChange = { vm.setQuery(it.take(100)) }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_hint)) }, modifier = Modifier.weight(1f).focusRequester(focus),
                    trailingIcon = { if (!vm.filter.query.isNullOrEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Default.Close, null) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { vm.reload() }),
                )
            }
        } else {
            GolaTopBar(title.ifBlank { stringResource(R.string.categories) }, { nav.popBackStack() }) {
                IconButton(onClick = { nav.navigate(Routes.SEARCH) }) { Icon(Icons.Default.Search, null) }
            }
        }
        // Sort & filter bar
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = vm.filter.activeCount > 0, onClick = { showFilters = true },
                label = { Text(stringResource(R.string.filters) + if (vm.filter.activeCount > 0) " (${vm.filter.activeCount})" else "") },
                leadingIcon = { Icon(Icons.Default.FilterList, null, Modifier.size(18.dp)) })
            FilterChip(selected = vm.filter.sort != "relevance", onClick = { showSort = true },
                label = { Text(stringResource(SORTS.first { it.first == vm.filter.sort }.second)) }, leadingIcon = { Icon(Icons.Default.Sort, null, Modifier.size(18.dp)) })
            FilterChip(selected = vm.filter.inStockOnly, onClick = { vm.apply(vm.filter.copy(inStockOnly = !vm.filter.inStockOnly)) }, label = { Text(stringResource(R.string.in_stock_only)) })
            FilterChip(selected = vm.filter.discountedOnly, onClick = { vm.apply(vm.filter.copy(discountedOnly = !vm.filter.discountedOnly)) }, label = { Text(stringResource(R.string.discounted_only)) })
        }
        LoadContent(vm.state, vm::reload) {
            if (vm.items.isEmpty()) {
                if (searchMode && vm.filter.query.isNullOrBlank()) Spacer(Modifier.fillMaxSize())
                else EmptyView(Icons.Default.SearchOff, R.string.no_results, stringResource(R.string.try_different))
            } else {
                LazyVerticalGrid(GridCells.Adaptive(156.dp), state = grid, contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(vm.items, key = { it.id }) { p -> ProductCard(p, cart, { nav.navigate(Routes.product(p.id)) }, Modifier.fillMaxWidth()) }
                    if (vm.loadingMore) item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp)) }
                }
            }
        }
    }

    if (showSort) {
        ModalBottomSheet(onDismissRequest = { showSort = false }) {
            Text(stringResource(R.string.sort), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            SORTS.forEach { (key, label) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = vm.filter.sort == key, onClick = { vm.apply(vm.filter.copy(sort = key)); showSort = false })
                    Text(stringResource(label))
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
    }
    if (showFilters) {
        FilterSheet(vm.filter, vm.brands, onDismiss = { showFilters = false }) { vm.apply(it); showFilters = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(initial: ProductFilter, brands: List<BrandDto>, onDismiss: () -> Unit, onApply: (ProductFilter) -> Unit) {
    var f by remember { mutableStateOf(initial) }
    var min by remember { mutableStateOf(initial.minPrice?.let { plain(it) } ?: "") }
    var max by remember { mutableStateOf(initial.maxPrice?.let { plain(it) } ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.filters), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.price_range), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(min, { min = it.filter { ch -> ch.isDigit() || ch == '.' }.take(7) }, label = { Text(stringResource(R.string.min)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(max, { max = it.filter { ch -> ch.isDigit() || ch == '.' }.take(7) }, label = { Text(stringResource(R.string.max)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            if (brands.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.brands), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 220.dp)) {
                    brands.forEach { b ->
                        val on = b.id in f.brandIds
                        FilterChip(selected = on, onClick = { f = f.copy(brandIds = if (on) f.brandIds - b.id else f.brandIds + b.id) }, label = { Text(b.name) })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(f.inStockOnly, { f = f.copy(inStockOnly = it) }); Text(stringResource(R.string.in_stock_only))
                Spacer(Modifier.width(16.dp))
                Checkbox(f.discountedOnly, { f = f.copy(discountedOnly = it) }); Text(stringResource(R.string.discounted_only))
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onApply(ProductFilter(query = initial.query, categoryId = initial.categoryId, sort = initial.sort)) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.clear))
                }
                Button(onClick = { onApply(f.copy(minPrice = min.toDoubleOrNull(), maxPrice = max.toDoubleOrNull())) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
    }
}
