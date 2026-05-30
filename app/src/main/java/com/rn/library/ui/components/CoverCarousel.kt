package com.rn.library.ui.components

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rn.library.data.toCoverImageData
import java.io.File
import kotlinx.coroutines.flow.distinctUntilChanged

private fun modIndex(value: Int, size: Int): Int {
    val m = value % size
    return if (m < 0) m + size else m
}

private fun centerPageForCoverIndex(index: Int, coverCount: Int): Int {
    val half = Int.MAX_VALUE / 2
    return half - modIndex(half, coverCount) + index
}
private fun pageToCoverIndex(page: Int, coverCount: Int): Int = modIndex(page, coverCount)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoverCarousel(
    coverPaths: List<String>,
    currentPath: String?,
    onCurrentPathChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    cardShape: RoundedCornerShape = RoundedCornerShape(20.dp),
    cardContainerColor: Color = Color(0xFF2A2A2A),
    placeholder: @Composable () -> Unit = {
        Text(text = "📖", fontSize = 72.sp)
    },
    onCoverClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (coverPaths.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false, onClick = {}),
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = cardContainerColor)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    placeholder()
                }
            }
            return
        }

        if (coverPaths.size == 1) {
            val path = coverPaths.first()
            LaunchedEffect(path) {
                if (path != currentPath) onCurrentPathChange(path)
            }
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onCoverClick),
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = cardContainerColor)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(path.toCoverImageData())
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            return
        }

        val coverCount = coverPaths.size
        val initialCoverIndex = coverPaths.indexOf(currentPath).let { if (it >= 0) it else 0 }
        val pagerState = rememberPagerState(
            initialPage = centerPageForCoverIndex(initialCoverIndex, coverCount),
            pageCount = { Int.MAX_VALUE },
        )

        LaunchedEffect(pagerState, coverCount) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page ->
                    onCurrentPathChange(coverPaths[pageToCoverIndex(page, coverCount)])
                }
        }

        LaunchedEffect(currentPath, coverPaths) {
            val target = coverPaths.indexOf(currentPath)
            if (target < 0) return@LaunchedEffect
            val currentIndex = pageToCoverIndex(pagerState.currentPage, coverCount)
            if (target == currentIndex) return@LaunchedEffect
            val forward = modIndex(target - currentIndex, coverCount)
            val backward = modIndex(currentIndex - target, coverCount)
            val delta = if (forward <= backward) forward else -backward
            pagerState.animateScrollToPage(pagerState.currentPage + delta)
        }
        val displayIndex = pageToCoverIndex(pagerState.currentPage, coverCount)

        Card(
            modifier = Modifier.fillMaxSize(),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = cardContainerColor)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onCoverClick)
            ) { page ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverPaths[pageToCoverIndex(page, coverCount)].toCoverImageData())
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            coverPaths.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == displayIndex) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == displayIndex) Color.White
                            else Color.White.copy(alpha = 0.45f)
                        )
                )
            }
        }
    }
}

fun coverPathToUri(path: String): Uri =
    if (path.startsWith("/")) Uri.fromFile(File(path)) else Uri.parse(path)