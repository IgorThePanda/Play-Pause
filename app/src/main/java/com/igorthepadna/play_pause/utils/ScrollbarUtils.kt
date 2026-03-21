package com.igorthepadna.play_pause.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch

private data class ScrollbarValues(val progress: Float, val viewportFraction: Float)

private fun calculateScrollbarValues(state: LazyListState): ScrollbarValues? {
    val layoutInfo = state.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val totalItems = layoutInfo.totalItemsCount
    if (visibleItems.isEmpty() || totalItems <= visibleItems.size) return null
    val firstItem = visibleItems.first()
    val lastItem = visibleItems.last()
    val visibleItemsHeight = (lastItem.offset + lastItem.size - firstItem.offset).toFloat()
    val avgItemSize = visibleItemsHeight / visibleItems.size
    if (avgItemSize <= 0f) return null
    val estimatedTotalHeight = avgItemSize * totalItems + layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding
    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
    val currentScroll = state.firstVisibleItemIndex * avgItemSize + state.firstVisibleItemScrollOffset
    val totalScrollRange = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)
    return ScrollbarValues(
        (currentScroll / totalScrollRange).coerceIn(0f, 1f),
        (viewportHeight / estimatedTotalHeight).coerceIn(0f, 1f)
    )
}

private fun calculateScrollbarValues(state: LazyGridState): ScrollbarValues? {
    val layoutInfo = state.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val totalItems = layoutInfo.totalItemsCount
    if (visibleItems.isEmpty() || totalItems <= visibleItems.size) return null
    val firstItem = visibleItems.first()
    var numColumns = 0
    val firstY = firstItem.offset.y
    for (i in visibleItems.indices) if (visibleItems[i].offset.y == firstY) numColumns++ else break
    if (numColumns == 0) numColumns = 1
    var visibleRowsCount = 0
    var lastRowY = -1
    for (i in visibleItems.indices) {
        if (visibleItems[i].offset.y != lastRowY) {
            visibleRowsCount++
            lastRowY = visibleItems[i].offset.y
        }
    }
    if (visibleRowsCount == 0) return null
    val lastItem = visibleItems.last()
    val visibleRowsHeight = (lastItem.offset.y + lastItem.size.height - firstItem.offset.y).toFloat()
    val avgRowSize = visibleRowsHeight / visibleRowsCount
    if (avgRowSize <= 0f) return null
    val totalRows = (totalItems + numColumns - 1) / numColumns
    val estimatedTotalHeight = avgRowSize * totalRows + layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding
    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
    val currentScroll = (state.firstVisibleItemIndex / numColumns) * avgRowSize + state.firstVisibleItemScrollOffset
    val totalScrollRange = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)
    return ScrollbarValues(
        (currentScroll / totalScrollRange).coerceIn(0f, 1f),
        (viewportHeight / estimatedTotalHeight).coerceIn(0f, 1f)
    )
}

fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 8.dp,
    activeWidth: Dp = 12.dp,
    color: Color? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    visible: Boolean = true
): Modifier = composed {
    if (!visible) return@composed this
    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary
    var isDragging by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (state.isScrollInProgress || isDragging) 1f else 0f, spring(stiffness = Spring.StiffnessLow), label = "alpha")
    val currentWidth by animateDpAsState(if (isDragging) activeWidth else width, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "width")
    val density = LocalDensity.current
    val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
    val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }
    val gutterWidthPx = with(density) { 24.dp.toPx() }
    val values by remember(state) { derivedStateOf { calculateScrollbarValues(state) } }
    val scope = rememberCoroutineScope()

    this.pointerInput(state, topPaddingPx, bottomPaddingPx, gutterWidthPx) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                if (change.changedToDown() && change.position.x > (size.width.toFloat() - gutterWidthPx)) {
                    isDragging = true
                    change.consume()
                    drag(change.id) { dragChange: PointerInputChange ->
                        val scrollbarAreaHeight = size.height.toFloat() - topPaddingPx - bottomPaddingPx
                        if (scrollbarAreaHeight > 0f) {
                            val dragY = (dragChange.position.y - topPaddingPx).coerceIn(0f, scrollbarAreaHeight)
                            val newProgress = (dragY / scrollbarAreaHeight).coerceIn(0f, 1f)
                            val totalItems = state.layoutInfo.totalItemsCount
                            if (totalItems > 0) {
                                val targetIndex = (newProgress * (totalItems - 1)).toInt().coerceIn(0, totalItems - 1)
                                scope.launch { state.scrollToItem(targetIndex) }
                            }
                        }
                        dragChange.consume()
                    }
                    isDragging = false
                }
            }
        }
    }.drawWithContent {
        drawContent()
        values?.let { v ->
            if (alpha > 0.01f) {
                val scrollbarAreaHeight = size.height - topPaddingPx - bottomPaddingPx
                if (scrollbarAreaHeight > 0f) {
                    val scrollbarHeight = (scrollbarAreaHeight * v.viewportFraction).coerceAtLeast(48.dp.toPx())
                    val scrollbarOffset = topPaddingPx + (v.progress * (scrollbarAreaHeight - scrollbarHeight))
                    if (isDragging) drawRect(scrollbarColor.copy(alpha = 0.03f * alpha), Offset(size.width - gutterWidthPx, topPaddingPx), Size(gutterWidthPx, scrollbarAreaHeight))
                    drawScrollbar(scrollbarColor, alpha, currentWidth, scrollbarOffset, scrollbarHeight)
                }
            }
        }
    }
}

fun Modifier.verticalScrollbar(
    state: LazyGridState,
    width: Dp = 8.dp,
    activeWidth: Dp = 12.dp,
    color: Color? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    visible: Boolean = true
): Modifier = composed {
    if (!visible) return@composed this
    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary
    var isDragging by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (state.isScrollInProgress || isDragging) 1f else 0f, spring(stiffness = Spring.StiffnessLow), label = "alpha")
    val currentWidth by animateDpAsState(if (isDragging) activeWidth else width, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "width")
    val density = LocalDensity.current
    val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
    val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }
    val gutterWidthPx = with(density) { 24.dp.toPx() }
    val values by remember(state) { derivedStateOf { calculateScrollbarValues(state) } }
    val scope = rememberCoroutineScope()

    this.pointerInput(state, topPaddingPx, bottomPaddingPx, gutterWidthPx) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                if (change.changedToDown() && change.position.x > (size.width.toFloat() - gutterWidthPx)) {
                    isDragging = true
                    change.consume()
                    drag(change.id) { dragChange: PointerInputChange ->
                        val scrollbarAreaHeight = size.height.toFloat() - topPaddingPx - bottomPaddingPx
                        if (scrollbarAreaHeight > 0f) {
                            val dragY = (dragChange.position.y - topPaddingPx).coerceIn(0f, scrollbarAreaHeight)
                            val newProgress = (dragY / scrollbarAreaHeight).coerceIn(0f, 1f)
                            val totalItems = state.layoutInfo.totalItemsCount
                            if (totalItems > 0) {
                                val targetIndex = (newProgress * (totalItems - 1)).toInt().coerceIn(0, totalItems - 1)
                                scope.launch { state.scrollToItem(targetIndex) }
                            }
                        }
                        dragChange.consume()
                    }
                    isDragging = false
                }
            }
        }
    }.drawWithContent {
        drawContent()
        values?.let { v ->
            if (alpha > 0.01f) {
                val scrollbarAreaHeight = size.height - topPaddingPx - bottomPaddingPx
                if (scrollbarAreaHeight > 0f) {
                    val scrollbarHeight = (scrollbarAreaHeight * v.viewportFraction).coerceAtLeast(48.dp.toPx())
                    val scrollbarOffset = topPaddingPx + (v.progress * (scrollbarAreaHeight - scrollbarHeight))
                    if (isDragging) drawRect(scrollbarColor.copy(alpha = 0.03f * alpha), Offset(size.width - gutterWidthPx, topPaddingPx), Size(gutterWidthPx, scrollbarAreaHeight))
                    drawScrollbar(scrollbarColor, alpha, currentWidth, scrollbarOffset, scrollbarHeight)
                }
            }
        }
    }
}

@Composable
fun ScrollbarLabel(state: LazyListState, padding: PaddingValues, labelProvider: (Int) -> String) {
    val values by remember(state) { derivedStateOf { calculateScrollbarValues(state) } }
    val firstVisibleIndex by remember { derivedStateOf { state.firstVisibleItemIndex } }
    val label = remember(firstVisibleIndex) { labelProvider(firstVisibleIndex) }
    val density = LocalDensity.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = label.isNotEmpty() && state.isScrollInProgress,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val topPx = with(density) { padding.calculateTopPadding().toPx() }
                val bottomPx = with(density) { padding.calculateBottomPadding().toPx() }
                val maxHeightPx = with(density) { maxHeight.toPx() }
                
                values?.let { v ->
                    val scrollbarAreaHeight = maxHeightPx - topPx - bottomPx
                    if (scrollbarAreaHeight > 0f) {
                        val scrollbarHeight = (scrollbarAreaHeight * v.viewportFraction).coerceAtLeast(with(density) { 48.dp.toPx() })
                        val scrollbarOffset = topPx + (v.progress * (scrollbarAreaHeight - scrollbarHeight))
                        val centerOffsetPx = scrollbarOffset + (scrollbarHeight / 2f)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 40.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            val centerOffsetDp = with(density) { centerOffsetPx.toDp() }
                            Box(modifier = Modifier.offset(y = centerOffsetDp - 36.dp)) {
                                ScrollbarLabelContainer(label)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScrollbarLabel(state: LazyGridState, padding: PaddingValues, labelProvider: (Int) -> String) {
    val values by remember(state) { derivedStateOf { calculateScrollbarValues(state) } }
    val firstVisibleIndex by remember { derivedStateOf { state.firstVisibleItemIndex } }
    val label = remember(firstVisibleIndex) { labelProvider(firstVisibleIndex) }
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = label.isNotEmpty() && state.isScrollInProgress,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val topPx = with(density) { padding.calculateTopPadding().toPx() }
                val bottomPx = with(density) { padding.calculateBottomPadding().toPx() }
                val maxHeightPx = with(density) { maxHeight.toPx() }
                
                values?.let { v ->
                    val scrollbarAreaHeight = maxHeightPx - topPx - bottomPx
                    if (scrollbarAreaHeight > 0f) {
                        val scrollbarHeight = (scrollbarAreaHeight * v.viewportFraction).coerceAtLeast(with(density) { 48.dp.toPx() })
                        val scrollbarOffset = topPx + (v.progress * (scrollbarAreaHeight - scrollbarHeight))
                        val centerOffsetPx = scrollbarOffset + (scrollbarHeight / 2f)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 40.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            val centerOffsetDp = with(density) { centerOffsetPx.toDp() }
                            Box(modifier = Modifier.offset(y = centerOffsetDp - 36.dp)) {
                                ScrollbarLabelContainer(label)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollbarLabelContainer(label: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, tonalElevation = 8.dp, shadowElevation = 4.dp) {
        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        }
    }
}

fun Modifier.verticalScrollbar(state: ScrollState, width: Dp = 8.dp, activeWidth: Dp = 12.dp, color: Color? = null, padding: PaddingValues = PaddingValues(0.dp)): Modifier = composed {
    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary
    var isDragging by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (state.isScrollInProgress || isDragging) 1f else 0f, spring(stiffness = Spring.StiffnessLow), label = "alpha")
    val currentWidth by animateDpAsState(if (isDragging) activeWidth else width, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "width")
    val density = LocalDensity.current
    val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
    val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }
    val gutterWidthPx = with(density) { 24.dp.toPx() }
    val scope = rememberCoroutineScope()

    this.pointerInput(state, topPaddingPx, bottomPaddingPx, gutterWidthPx) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                if (change.changedToDown() && change.position.x > (size.width.toFloat() - gutterWidthPx)) {
                    isDragging = true
                    change.consume()
                    drag(change.id) { dragChange: PointerInputChange ->
                        val scrollbarAreaHeight = size.height.toFloat() - topPaddingPx - bottomPaddingPx
                        if (scrollbarAreaHeight > 0f) {
                            val dragY = (dragChange.position.y - topPaddingPx).coerceIn(0f, scrollbarAreaHeight)
                            val newProgress = (dragY / scrollbarAreaHeight).coerceIn(0f, 1f)
                            scope.launch { state.scrollTo((newProgress * state.maxValue).toInt()) }
                        }
                        dragChange.consume()
                    }
                    isDragging = false
                }
            }
        }
    }.drawWithContent {
        drawContent()
        if (alpha > 0.01f) {
            val scrollbarAreaHeight = size.height - topPaddingPx - bottomPaddingPx
            if (scrollbarAreaHeight > 0f) {
                val viewportHeight = size.height
                val estimatedTotalHeight = state.maxValue + viewportHeight
                val scrollbarHeight = (viewportHeight / estimatedTotalHeight * scrollbarAreaHeight).coerceAtLeast(48.dp.toPx())
                val scrollProgress = state.value.toFloat() / (state.maxValue.takeIf { it > 0 } ?: 1)
                val scrollbarOffset = topPaddingPx + (scrollProgress * (scrollbarAreaHeight - scrollbarHeight))
                if (isDragging) drawRect(scrollbarColor.copy(alpha = 0.03f * alpha), Offset(size.width - gutterWidthPx, topPaddingPx), Size(gutterWidthPx, scrollbarAreaHeight))
                drawScrollbar(scrollbarColor, alpha, currentWidth, scrollbarOffset, scrollbarHeight)
            }
        }
    }
}

private fun ContentDrawScope.drawScrollbar(color: Color, alpha: Float, width: Dp, scrollbarOffset: Float, scrollbarHeight: Float) {
    val thickness = width.toPx()
    val xOffset = size.width - thickness
    drawRoundRect(color = color.copy(alpha = (if (width > 8.dp) 0.95f else 0.55f) * alpha), topLeft = Offset(xOffset, scrollbarOffset), size = Size(thickness, scrollbarHeight), cornerRadius = CornerRadius(thickness / 2, thickness / 2))
}
