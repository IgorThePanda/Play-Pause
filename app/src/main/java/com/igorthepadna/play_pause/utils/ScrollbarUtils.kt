package com.igorthepadna.play_pause.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    visible: Boolean = true
): Modifier {
    if (!visible) return this
    
    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scrollbar_alpha"
    )

    val density = LocalDensity.current
    val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
    val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }

    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    
    val scrollbarValues by remember(state, layoutInfo) {
        derivedStateOf {
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || totalItems <= visibleItems.size || totalItems > 10000) {
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

    val animProgress by animateFloatAsState(
        targetValue = scrollbarValues?.first ?: 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scrollbar_progress"
    )
    
    val animHeightFraction by animateFloatAsState(
        targetValue = scrollbarValues?.second ?: 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scrollbar_height_fraction"
    )

    return drawWithContent {
        drawContent()
        if (alpha > 0.01f && scrollbarValues != null) {
            val scrollbarAreaHeight = size.height - topPaddingPx - bottomPaddingPx
            if (scrollbarAreaHeight > 0) {
                val scrollbarHeight = (scrollbarAreaHeight * animHeightFraction).coerceAtLeast(40.dp.toPx())
                val scrollbarOffset = topPaddingPx + (animProgress * (scrollbarAreaHeight - scrollbarHeight))
                
                drawScrollbar(scrollbarColor, alpha, width, scrollbarOffset, scrollbarHeight)
            }
        }
    }
}

@Composable
fun Modifier.verticalScrollbar(
    state: LazyGridState,
    width: Dp = 4.dp,
    color: Color? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    visible: Boolean = true
): Modifier {
    if (!visible) return this

    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scrollbar_alpha"
    )

    val density = LocalDensity.current
    val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
    val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }

    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    
    val scrollbarValues by remember(state, layoutInfo) {
        derivedStateOf {
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || totalItems <= visibleItems.size || totalItems > 10000) {
                return@derivedStateOf null
            }
            
            val numColumns = visibleItems.groupBy { it.offset.y }.maxOfOrNull { it.value.size } ?: 1
            val totalRows = (totalItems + numColumns - 1) / numColumns
            
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()
            
            val visibleRowsList = visibleItems.groupBy { it.offset.y }
            val visibleRowsCount = visibleRowsList.size
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

    val animProgress by animateFloatAsState(
        targetValue = scrollbarValues?.first ?: 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scrollbar_progress"
    )
    
    val animHeightFraction by animateFloatAsState(
        targetValue = scrollbarValues?.second ?: 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scrollbar_height_fraction"
    )

    return drawWithContent {
        drawContent()
        if (alpha > 0.01f && scrollbarValues != null) {
            val scrollbarAreaHeight = size.height - topPaddingPx - bottomPaddingPx
            if (scrollbarAreaHeight > 0) {
                val scrollbarHeight = (scrollbarAreaHeight * animHeightFraction).coerceAtLeast(40.dp.toPx())
                val scrollbarOffset = topPaddingPx + (animProgress * (scrollbarAreaHeight - scrollbarHeight))
                
                drawScrollbar(scrollbarColor, alpha, width, scrollbarOffset, scrollbarHeight)
            }
        }
    }
}

@Composable
fun Modifier.verticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    color: Color? = null,
    padding: PaddingValues = PaddingValues(0.dp)
): Modifier {
    val scrollbarColor = color ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scrollbar_alpha"
    )

    if (state.maxValue == 0) return this

    val density = LocalDensity.current
    val topPaddingPx = with(density) { padding.calculateTopPadding().toPx() }
    val bottomPaddingPx = with(density) { padding.calculateBottomPadding().toPx() }

    val scrollProgress = state.value.toFloat() / state.maxValue
    
    val animProgress by animateFloatAsState(
        targetValue = scrollProgress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scrollbar_progress"
    )

    return drawWithContent {
        drawContent()
        if (alpha > 0.01f) {
            val scrollbarAreaHeight = size.height - topPaddingPx - bottomPaddingPx
            if (scrollbarAreaHeight > 0) {
                val viewportHeight = size.height
                val estimatedTotalHeight = state.maxValue + viewportHeight
                val scrollbarHeight = (viewportHeight / estimatedTotalHeight * scrollbarAreaHeight).coerceAtLeast(40.dp.toPx())
                val scrollbarOffset = topPaddingPx + (animProgress * (scrollbarAreaHeight - scrollbarHeight))
                
                drawScrollbar(scrollbarColor, alpha, width, scrollbarOffset, scrollbarHeight)
            }
        }
    }
}

private fun ContentDrawScope.drawScrollbar(
    color: Color,
    alpha: Float,
    width: Dp,
    scrollbarOffset: Float,
    scrollbarHeight: Float
) {
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset(size.width - width.toPx() - 4.dp.toPx(), scrollbarOffset),
        size = Size(width.toPx(), scrollbarHeight),
        cornerRadius = CornerRadius(x = width.toPx() / 2, y = width.toPx() / 2)
    )
}
