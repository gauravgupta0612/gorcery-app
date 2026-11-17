package com.golagrocery.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.golagrocery.app.R
import com.golagrocery.app.ui.components.appContainer
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.navigation.goRoot
import com.golagrocery.app.ui.theme.GolaColors
import kotlinx.coroutines.launch

private data class Page(val emoji: String, val title: Int, val body: Int)
private val pages = listOf(
    Page("🥭🥬🍅", R.string.onb_title_1, R.string.onb_body_1),
    Page("🏷️💰🎉", R.string.onb_title_2, R.string.onb_body_2),
    Page("📱💳💵", R.string.onb_title_3, R.string.onb_body_3),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(nav: NavHostController) {
    val container = appContainer()
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val finish: () -> Unit = { scope.launch { container.session.setOnboardingDone(); nav.goRoot(Routes.LOGIN) } }
    Column(Modifier.fillMaxSize().background(GolaColors.GreenLight).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = finish) { Text(stringResource(R.string.skip)) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
            val p = pages[i]
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(220.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color.White), contentAlignment = Alignment.Center) {
                    Text(p.emoji, fontSize = 56.sp)
                }
                Spacer(Modifier.height(32.dp))
                Text(stringResource(p.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = GolaColors.GreenDark)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(p.body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = GolaColors.Grey)
            }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) {
                repeat(pages.size) { i ->
                    val w by animateDpAsState(if (pager.currentPage == i) 24.dp else 8.dp, label = "dot")
                    val c by animateColorAsState(if (pager.currentPage == i) GolaColors.Green else GolaColors.Grey.copy(alpha = .4f), label = "dotc")
                    Box(Modifier.padding(end = 6.dp).height(8.dp).width(w).clip(RoundedCornerShape(4.dp)).background(c))
                }
            }
            Button(onClick = {
                if (pager.currentPage < pages.lastIndex) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else finish()
            }) { Text(stringResource(if (pager.currentPage < pages.lastIndex) R.string.next else R.string.get_started)) }
        }
    }
}
