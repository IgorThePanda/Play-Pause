package com.igorthepadna.play_pause.data.remote

import com.igorthepadna.play_pause.data.*
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class LastfmFriendsResponse(val friends: LastfmFriendsList)

@Serializable
data class LastfmFriendsList(val user: List<LastfmUser>)

@Serializable
data class LastfmRecentTracksResponse(val recenttracks: LastfmRecentTracksList)

@Serializable
data class LastfmRecentTracksList(val track: List<LastfmTrack>)

interface LastfmService {
    @GET("?method=user.getfriends&format=json")
    suspend fun getFriends(
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 50
    ): LastfmFriendsResponse

    @GET("?method=user.getrecenttracks&format=json")
    suspend fun getRecentTracks(
        @Query("user") user: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 1
    ): LastfmRecentTracksResponse
}
