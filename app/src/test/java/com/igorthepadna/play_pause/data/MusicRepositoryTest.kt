package com.igorthepadna.play_pause.data

import org.junit.Test
import org.junit.Assert.*

class MusicRepositoryTest {

    @Test
    fun testSplitArtistsCaseInsensitive() {
        val input = "Artist A Feat Artist B"
        val expected = listOf("Artist A", "Artist B")
        val actual = MusicRepository.splitArtists(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testSplitArtistsWithVariousDelimiters() {
        val input = "Artist A & Artist B feat. Artist C, Artist D Featuring Artist E"
        val expected = listOf("Artist A", "Artist B", "Artist C", "Artist D", "Artist E")
        val actual = MusicRepository.splitArtists(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testSplitArtistsProtected() {
        val input = "Tyler, The Creator Feat. Marina and the Diamonds"
        val expected = listOf("Tyler, The Creator", "Marina and the Diamonds")
        val actual = MusicRepository.splitArtists(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testCleanAlbumTitle() {
        val input = "Greatest Hits (feat. Someone)"
        val expected = "Greatest Hits"
        val actual = MusicRepository.cleanTitle(input)
        assertEquals(expected, actual)
    }
}
