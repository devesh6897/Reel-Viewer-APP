package com.example.reelapp

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class VideoItem(
    val likeCount: Int,
    val url: String? = null,
    val profilePicUrl: String? = null,   // URL or resource ID for the profile picture
    val username: String? = null,     // Username
    val description: String? = null   // Description text
) : Parcelable
