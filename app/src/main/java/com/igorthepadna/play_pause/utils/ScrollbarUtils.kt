package com.igorthepadna.play_pause.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch

/**
 * An expressive Material 3 scrollbar that supports touch scrubbing.
 * Updated to "stick" to the edge by using a half-pill design.
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    activeWidth: Dp = 10.dp,
    color: Color? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    visible: Boolean = true
): Modifier = composed {
    if (!visible) return@composed this
    
    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary
    var isDragging by remember { mutableStateOf(false) }
    val isScrolling = state.isScrollInProgress || isDragging
    
    val alpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scrollbar_alpha"
    )

    val currentWidth by animateDpAsState(
        targetValue = if (isDragging) activeWidth else width,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "scrollbar_width"
    )

    val density = LocalDensity.current
    val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
    val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }
    val gutterWidthPx = with(density) { 32.dp.toPx() } // Hitbox for grabbing

    val scrollbarValues by remember(state) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val totalItems = layoutInfo.totalItemsCount
            
            if (visibleItems.isEmpty() || totalItems <= visibleItems.size) {
                return@derivedStateOf null
            }
            
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()
            
            val visibleItemsHeight = lastItem.offset + lastItem.size - firstItem.offset
            val avgItemSize = visibleItemsHeight.toFloat() / visibleItems.size
            if (avgItemSize <= 0f) return@derivedStateOf null
            
            val estimatedTotalHeight = avgItemSize * totalItems + layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding
            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
            
            val currentScroll = state.firstVisibleItemIndex * avgItemSize + state.firstVisibleItemScrollOffset
            val totalScrollRange = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)
            val scrollProgress = (currentScroll / totalScrollRange).coerceIn(0f, 1f)
            
            val heightFraction = (viewportHeight / estimatedTotalHeight).coerceIn(0f, 1f)
            
            Pair(scrollProgress, heightFraction)
        }
    }

    val scope = rememberCoroutineScope()

    this
        .pointerInput(state, topPaddingPx, bottomPaddingPx, gutterWidthPx) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > size.width - gutterWidthPx) {
                        isDragging = true
                        down.consume()
                        
                        drag(down.id) { change ->
                            val scrollbarAreaHeight = size.height.toFloat() - topPaddingPx - bottomPaddingPx
                            val dragY = (change.position.y - topPaddingPx).coerceIn(0f, scrollbarAreaHeight)
                            val newProgress = (dragY / scrollbarAreaHeight).coerceIn(0f, 1f)
                            
                            val totalItems = state.layoutInfo.totalItemsCount
                            if (totalItems > 0) {
                                val targetIndex = (newProgress * (totalItems - 1)).toInt().coerceIn(0, totalItems - 1)
                                scope.launch {
                                    state.scrollToItem(targetIndex)
                                }
                            }
                            change.consume()
                        }
                        isDragging = false
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            val values = scrollbarValues
            if (alpha > 0.01f && values != null) {
                val scrollbarAreaHeight = size.height - topPaddingPx - bottomPaddingPx
                if (scrollbarAreaHeight > 0) {
                    val scrollbarHeight = (scrollbarAreaHeight * values.second).coerceAtLeast(48.dp.toPx())
                    val scrollbarOffset = topPaddingPx + (values.first * (scrollbarAreaHeight - scrollbarHeight))
                    
                    if (isDragging) {
                        drawRect(
                            color = scrollbarColor.copy(alpha = 0.03f * alpha),
                            topLeft = Offset(size.width - gutterWidthPx, topPaddingPx),
                            size = Size(gutterWidthPx, scrollbarAreaHeight)
                        )
                    }

                    drawScrollbar(
                        color = scrollbarColor,
                        alpha = alpha,
                        width = currentWidth,
                        scrollbarOffset = scrollbarOffset,
                        scrollbarHeight = scrollbarHeight,
                        isDragging = isDragging
                    )
                }
            }
        }
}

fun Modifier.verticalScrollbar(
    state: LazyGridState,
    width: Dp = 4.dp,
    activeWidth: Dp = 10.dp,
    color: Color? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    visible: Boolean = true
): Modifier = composed {
    if (!visible) return@composed this

    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary
    var isDragging by remember { mutableStateOf(false) }
    val isScrolling = state.isScrollInProgress || isDragging
    
    val alpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scrollbar_alpha"
    )

    val currentWidth by animateDpAsState(
        targetValue = if (isDragging) activeWidth else width,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "scrollbar_width"
    )

    val density = LocalDensity.current
    val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
    val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }
    val gutterWidthPx = with(density) { 32.dp.toPx() }

    val scrollbarValues by remember(state) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val totalItems = layoutInfo.totalItemsCount
            
            if (visibleItems.isEmpty() || totalItems <= visibleItems.size) {
                return@derivedStateOf null
            }
            
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()
            
            var numColumns = 0
            val firstY = firstItem.offset.y
            for (i in visibleItems.indices) {
                if (visibleItems[i].offset.y == firstY) numColumns++ else break
            }
            if (numColumns == 0) numColumns = 1
            
            val totalRows = (totalItems + numColumns - 1) / numColumns
            
            var visibleRowsCount = 0
            var lastRowY = -1
            for (i in visibleItems.indices) {
                val itemY = visibleItems[i].offset.y
                if (itemY != lastRowY) {
                    visibleRowsCount++
                    lastRowY = itemY
                }
            }
            
            if (visibleRowsCount == 0) return@derivedStateOf null
            
            val visibleRowsHeight = lastItem.offset.y + lastItem.size.height - firstItem.offset.y
            val avgRowSize = visibleRowsHeight.toFloat() / visibleRowsCount
            if (avgRowSize <= 0f) return@derivedStateOf null
            
            val estimatedTotalHeight = avgRowSize * totalRows + layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding
            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
            
            val currentScroll = (state.firstVisibleItemIndex / numColumns) * avgRowSize + state.firstVisibleItemScrollOffset
            val totalScrollRange = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)
            val scrollProgress = (currentScroll / totalScrollRange).coerceIn(0f, 1f)
            
            val heightFraction = (viewportHeight / estimatedTotalHeight).coerceIn(0f, 1f)
            
            Pair(scrollProgress, heightFraction)
        }
    }

    val scope = rememberCoroutineScope()

    this
        .pointerInput(state, topPaddingPx, bottomPaddingPx, gutterWidthPx) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > size.width - gutterWidthPx) {
                        isDragging = true
                        down.consume()
                        
                        drag(down.id) { change ->
                            val scrollbarAreaHeight = size.height.toFloat() - topPaddingPx - bottomPaddingPx
                            val dragY = (change.position.y - topPaddingPx).coerceIn(0f, scrollbarAreaHeight)
                            val newProgress = (dragY / scrollbarAreaHeight).coerceIn(0f, 1f)
                            
                            val totalItems = state.layoutInfo.totalItemsCount
                            if (totalItems > 0) {
                                val targetIndex = (newProgress * (totalItems - 1)).toInt().coerceIn(0, totalItems - 1)
                                scope.launch {
                                    state.scrollToItem(targetIndex)
                                }
                            }
                            change.consume()
                        }
                        isDragging = false
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            val values = scrollbarValues
            if (alpha > 0.01f && values != null) {
                val scrollbarAreaHeight = size.height - topPaddingPx - bottomPaddingPx
                if (scrollbarAreaHeight > 0) {
                    val scrollbarHeight = (scrollbarAreaHeight * values.second).coerceAtLeast(48.dp.toPx())
                    val scrollbarOffset = topPaddingPx + (values.first * (scrollbarAreaHeight - scrollbarHeight))
                    
                    if (isDragging) {
                        drawRect(
                            color = scrollbarColor.copy(alpha = 0.03f * alpha),
                            topLeft = Offset(size.width - gutterWidthPx, topPaddingPx),
                            size = Size(gutterWidthPx, scrollbarAreaHeight)
                        )
                    }

                    drawScrollbar(
                        color = scrollbarColor,
                        alpha = alpha,
                        width = currentWidth,
                        scrollbarOffset = scrollbarOffset,
                        scrollbarHeight = scrollbarHeight,
                        isDragging = isDragging
                    )
                }
            }
        }
}

@Composable
fun ScrollbarLabel(
    state: LazyListState,
    padding: PaddingValues,
    labelProvider: (Int) -> String
) {
    val firstVisibleIndex by remember { derivedStateOf { state.firstVisibleItemIndex } }
    val label = remember(firstVisibleIndex) { labelProvider(firstVisibleIndex) }
    
    val scrollbarValues by remember(state) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf null
            
            val totalItems = layoutInfo.totalItemsCount
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()
            val visibleItemsHeight = lastItem.offset + lastItem.size - firstItem.offset
            val avgItemSize = visibleItemsHeight.toFloat() / visibleItems.size
            if (avgItemSize <= 0f) return@derivedStateOf null
            
            val estimatedTotalHeight = avgItemSize * totalItems + layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding
            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
            val currentScroll = state.firstVisibleItemIndex * avgItemSize + state.firstVisibleItemScrollOffset
            val totalScrollRange = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)
            val scrollProgress = (currentScroll / totalScrollRange).coerceIn(0f, 1f)
            val heightFraction = (viewportHeight / estimatedTotalHeight).coerceIn(0f, 1f)
            
            Pair(scrollProgress, heightFraction)
        }
    }

    AnimatedVisibility(
        visible = label.isNotEmpty() && state.isScrollInProgress,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxSize()
    ) {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
        val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }
        
        Box(modifier = Modifier.fillMaxSize()) {
            scrollbarValues?.let { values ->
                val scrollbarAreaHeight = with(density) { (configuration.screenHeightDp.dp).toPx() } - topPaddingPx - bottomPaddingPx
                val scrollbarHeight = (scrollbarAreaHeight * values.second).coerceAtLeast(with(density) { 48.dp.toPx() })
                val scrollbarOffset = topPaddingPx + (values.first * (scrollbarAreaHeight - scrollbarHeight))
                
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, (scrollbarOffset + scrollbarHeight / 2 - with(density) { 36.dp.toPx() }).toInt()) }
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp)
                ) {
                    ScrollbarLabelContainer(label)
                }
            }
        }
    }
}

@Composable
fun ScrollbarLabel(
    state: LazyGridState,
    padding: PaddingValues,
    labelProvider: (Int) -> String
) {
    val firstVisibleIndex by remember { derivedStateOf { state.firstVisibleItemIndex } }
    val label = remember(firstVisibleIndex) { labelProvider(firstVisibleIndex) }
    
    val scrollbarValues by remember(state) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf null
            
            val totalItems = layoutInfo.totalItemsCount
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()
            
            var numColumns = 0
            val firstY = firstItem.offset.y
            for (i in visibleItems.indices) {
                if (visibleItems[i].offset.y == firstY) numColumns++ else break
            }
            if (numColumns == 0) numColumns = 1
            
            val totalRows = (totalItems + numColumns - 1) / numColumns
            
            var visibleRowsCount = 0
            var lastRowY = -1
            for (i in visibleItems.indices) {
                val itemY = visibleItems[i].offset.y
                if (itemY != lastRowY) {
                    visibleRowsCount++
                    lastRowY = itemY
                }
            }
            if (visibleRowsCount == 0) return@derivedStateOf null
            
            val visibleRowsHeight = lastItem.offset.y + lastItem.size.height - firstItem.offset.y
            val avgRowSize = visibleRowsHeight.toFloat() / visibleRowsCount
            if (avgRowSize <= 0f) return@derivedStateOf null
            
            val estimatedTotalHeight = avgRowSize * totalRows + layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding
            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
            val currentScroll = (state.firstVisibleItemIndex / numColumns) * avgRowSize + state.firstVisibleItemScrollOffset
            val totalScrollRange = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)
            val scrollProgress = (currentScroll / totalScrollRange).coerceIn(0f, 1f)
            val heightFraction = (viewportHeight / estimatedTotalHeight).coerceIn(0f, 1f)
            
            Pair(scrollProgress, heightFraction)
        }
    }

    AnimatedVisibility(
        visible = label.isNotEmpty() && state.isScrollInProgress,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxSize()
    ) {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
        val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }
        
        Box(modifier = Modifier.fillMaxSize()) {
            scrollbarValues?.let { values ->
                val scrollbarAreaHeight = with(density) { (configuration.screenHeightDp.dp).toPx() } - topPaddingPx - bottomPaddingPx
                val scrollbarHeight = (scrollbarAreaHeight * values.second).coerceAtLeast(with(density) { 48.dp.toPx() })
                val scrollbarOffset = topPaddingPx + (values.first * (scrollbarAreaHeight - scrollbarHeight))
                
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, (scrollbarOffset + scrollbarHeight / 2 - with(density) { 36.dp.toPx() }).toInt()) }
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp)
                ) {
                    ScrollbarLabelContainer(label)
                }
            }
        }
    }
}

@Composable
private fun ScrollbarLabelContainer(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
        }
    }
}

fun Modifier.verticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    activeWidth: Dp = 10.dp,
    color: Color? = null,
    padding: PaddingValues = PaddingValues(0.dp)
): Modifier = composed {
    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary
    var isDragging by remember { mutableStateOf(false) }
    val isScrolling = state.isScrollInProgress || isDragging
    
    val alpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scrollbar_alpha"
    )

    val currentWidth by animateDpAsState(
        targetValue = if (isDragging) activeWidth else width,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "scrollbar_width"
    )

    if (state.maxValue == 0) return@composed this

    val density = LocalDensity.current
    val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
    val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }
    val gutterWidthPx = with(density) { 32.dp.toPx() }

    val scope = rememberCoroutineScope()

    this
        .pointerInput(state, topPaddingPx, bottomPaddingPx, gutterWidthPx) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > size.width - gutterWidthPx) {
                        isDragging = true
                        down.consume()
                        
                        drag(down.id) { change ->
                            val scrollbarAreaHeight = size.height.toFloat() - topPaddingPx - bottomPaddingPx
                            val dragY = (change.position.y - topPaddingPx).coerceIn(0f, scrollbarAreaHeight)
                            val newProgress = (dragY / scrollbarAreaHeight).coerceIn(0f, 1f)
                            
                            scope.launch {
                                state.scrollTo((newProgress * state.maxValue).toInt())
                            }
                            change.consume()
                        }
                        isDragging = false
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (alpha > 0.01f) {
                val scrollbarAreaHeight = size.height - topPaddingPx - bottomPaddingPx
                if (scrollbarAreaHeight > 0) {
                    val viewportHeight = size.height
                    val estimatedTotalHeight = state.maxValue + viewportHeight
                    val scrollbarHeight = (viewportHeight / estimatedTotalHeight * scrollbarAreaHeight).coerceAtLeast(48.dp.toPx())
                    val scrollProgress = state.value.toFloat() / state.maxValue
                    val scrollbarOffset = topPaddingPx + (scrollProgress * (scrollbarAreaHeight - scrollbarHeight))
                    
                    if (isDragging) {
                        drawRect(
                            color = scrollbarColor.copy(alpha = 0.03f * alpha),
                            topLeft = Offset(size.width - gutterWidthPx, topPaddingPx),
                            size = Size(gutterWidthPx, scrollbarAreaHeight)
                        )
                    }

                    drawScrollbar(
                        color = scrollbarColor,
                        alpha = alpha,
                        width = currentWidth,
                        scrollbarOffset = scrollbarOffset,
                        scrollbarHeight = scrollbarHeight,
                        isDragging = isDragging
                    )
                }
            }
        }
}

private fun ContentDrawScope.drawScrollbar(
    color: Color,
    alpha: Float,
    width: Dp,
    scrollbarOffset: Float,
    scrollbarHeight: Float,
    isDragging: Boolean
) {
    val thickness = width.toPx()
    // We draw the pill wider but offset it so the right side is cut off by the edge
    // This creates a "sticky" half-pill effect.
    val xOffset = size.width - (thickness * 0.8f) 
    
    drawRoundRect(
        color = color.copy(alpha = (if (isDragging) 0.95f else 0.5f) * alpha),
        topLeft = Offset(xOffset, scrollbarOffset),
        size = Size(thickness * 1.5f, scrollbarHeight),
        cornerRadius = CornerRadius(x = thickness, y = thickness)
    )
}
