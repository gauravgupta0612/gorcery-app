package com.golagrocery.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.CategoryDto
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.ui.components.*
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.launch

class CategoriesViewModel(private val c: AppContainer) : ViewModel() {
    var state by mutableStateOf<Load<List<CategoryDto>>>(Load.Loading); private set
    var selected by mutableIntStateOf(0)
    init { load() }
    fun load() {
        state = Load.Loading
        viewModelScope.launch {
            state = when (val r = c.catalog.categories()) {
                is ApiResult.Ok -> Load.Ready(r.data)
                is ApiResult.Err -> Load.Failed(r.message, r.isNetwork)
            }
        }
    }
}

@Composable
fun CategoriesScreen(nav: NavHostController) {
    val vm = golaViewModel { CategoriesViewModel(it) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        GolaTopBar(stringResource(R.string.categories), null)
        LoadContent(vm.state, vm::load) { cats ->
            if (cats.isEmpty()) return@LoadContent
            val sel = cats[vm.selected.coerceIn(0, cats.lastIndex)]
            Row(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.width(96.dp).fillMaxHeight().background(GolaColors.Surface)) {
                    items(cats.size) { i ->
                        val c = cats[i]
                        val bg by animateColorAsState(if (i == vm.selected) Color.White else Color.Transparent, label = "cat")
                        Column(
                            Modifier.fillMaxWidth().background(bg).clickable { vm.selected = i }.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            NetImage(c.imageUrl, c.name, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
                            Text(localizedName(c.name, c.nameHi), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2,
                                color = if (i == vm.selected) GolaColors.Green else Color.Unspecified)
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(localizedName(sel.name, sel.nameHi), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.see_all), color = GolaColors.Green, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable { nav.navigate(Routes.products(sel.id, sel.name)) })
                    }
                    LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(8.dp)) {
                        items(sel.children, key = { it.id }) { sub ->
                            Column(Modifier.padding(4.dp).clip(RoundedCornerShape(12.dp)).clickable { nav.navigate(Routes.products(sub.id, sub.name)) }
                                .background(GolaColors.GreenLight).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                NetImage(sub.imageUrl ?: sel.imageUrl, sub.name, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)))
                                Text(localizedName(sub.name, sub.nameHi), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}
