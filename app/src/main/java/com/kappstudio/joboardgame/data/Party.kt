package com.kappstudio.joboardgame.data

import android.os.Parcelable
import com.kappstudio.joboardgame.login.UserManager
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Party(
    val id: String = "",
    var hostId: String = "",
    var host: User = User(),
    var title: String = "",
    var cover: String = "https://firebasestorage.googleapis.com/v0/b/jo-tabletop-game.appspot.com/o/cover1.png?alt=media&token=f3144faf-1e81-4d84-b25e-46e32b64b8f1",
    var partyTime: Long = 0,
    var location:  Location =  Location(),
    var note: String = "",
    var requirePlayerQty: Int = 0,
    var gameList: MutableList<Game> = mutableListOf(),
    var playerIdList: MutableList<String> = mutableListOf(),
    var playerList: MutableList<User>? = null,
    var photos: MutableList<String> = mutableListOf(),

    ) : Parcelable

 @Parcelize
data class NewParty(

    var id: String = "",
    var hostId: String = UserManager.user.value?.id?:"",
    var host: HashMap<String, String> = toUserMap(UserManager.user.value?:User()),
    var title: String = "",
    var cover: String = "https://firebasestorage.googleapis.com/v0/b/jo-tabletop-game.appspot.com/o/cover1.png?alt=media&token=f3144faf-1e81-4d84-b25e-46e32b64b8f1",
    var partyTime: Long = 0,
    var location:  Location =  Location(),
    var note: String = "",
    var requirePlayerQty: Int = 0,
    var gameList: @RawValue  MutableList<HashMap<String, Any>> = mutableListOf(),
    var playerIdList: MutableList<String> = mutableListOf(UserManager.user.value?.id?:""),
    var playerList: MutableList<HashMap<String, String>> = mutableListOf(
        toUserMap(UserManager.user.value?:User())
    ),
    var photos: MutableList<String> = mutableListOf(),


    ) : Parcelable
