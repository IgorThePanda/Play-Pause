package com.igorthepadna.play_pause.utils

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import kotlin.random.Random

@UnstableApi
class CustomShuffleOrder(
    private val length: Int,
    private val currentIndex: Int,
    private val playNextIndices: List<Int>,
    private val normalIndices: List<Int>,
    private val random: Random = Random(System.currentTimeMillis())
) : ShuffleOrder {

    private val shuffledNormalIndices: List<Int> = normalIndices.shuffled(random)

    // The order should be:
    // 1. Current index
    // 2. All "Play Next" indices in their relative order
    // 3. All other normal indices shuffled
    private val fullOrder: List<Int> = run {
        val order = mutableListOf<Int>()
        if (currentIndex != -1 && currentIndex < length) {
            order.add(currentIndex)
        }
        order.addAll(playNextIndices)
        order.addAll(shuffledNormalIndices)
        order
    }

    override fun getNextIndex(index: Int): Int {
        val pos = fullOrder.indexOf(index)
        return if (pos != -1 && pos < fullOrder.size - 1) fullOrder[pos + 1] else -1
    }

    override fun getPreviousIndex(index: Int): Int {
        val pos = fullOrder.indexOf(index)
        return if (pos != -1 && pos > 0) fullOrder[pos - 1] else -1
    }

    override fun getLastIndex(): Int = fullOrder.lastOrNull() ?: -1

    override fun getFirstIndex(): Int = fullOrder.firstOrNull() ?: -1

    override fun getLength(): Int = length

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        // Simple implementation for cloning, ideally we should re-evaluate playNext
        return CustomShuffleOrder(length + insertionCount, currentIndex, playNextIndices, normalIndices)
    }

    override fun cloneAndRemove(indexFrom: Int, indexTo: Int): ShuffleOrder {
        return CustomShuffleOrder(length - (indexTo - indexFrom), currentIndex, playNextIndices, normalIndices)
    }

    override fun cloneAndClear(): ShuffleOrder {
        return CustomShuffleOrder(0, -1, emptyList(), emptyList())
    }
}
