package com.rock.pixelplay.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VideoItem(
    val title: String,
    val path: String,
    val dateAdded: Long,
    val duration: String,
    val thumbnail: String,
    var lastPlayed: String,
    var playedPercentage : Float
)