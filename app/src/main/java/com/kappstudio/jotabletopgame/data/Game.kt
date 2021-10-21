package com.kappstudio.jotabletopgame.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Game(
    val id:String = "",
    var name: String = "",
    val image: String = "",
    val type: MutableList<String>? = null,
    val time: Long = 0,
    val minPlayerQty: Int = 0,
    val maxPlayerQty: Int = 0,
    val desc: String = "",
    var totalRating: Long = -1,
    var ratingQty: Long = -1,
    var createdTime: Long = 0,
): Parcelable