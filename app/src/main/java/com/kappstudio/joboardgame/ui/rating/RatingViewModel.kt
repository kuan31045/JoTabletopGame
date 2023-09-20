package com.kappstudio.joboardgame.ui.rating

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kappstudio.joboardgame.R
import com.kappstudio.joboardgame.appInstance
import com.kappstudio.joboardgame.data.NewRating
import com.kappstudio.joboardgame.data.Rating
import com.kappstudio.joboardgame.data.remote.FirebaseService
import kotlinx.coroutines.launch

class RatingViewModel(private val rating: Rating) : ViewModel() {

    private var _myRating = MutableLiveData(rating)
    val myRating: LiveData<Rating> = _myRating

    private var _msg = MutableLiveData<String>()
    val msg: LiveData<String> = _msg

    val hasRating = MutableLiveData(rating.id.isNotEmpty())

    var score = MutableLiveData(rating.score)

    fun sendRating() {
        var newRating = NewRating(
            id = rating.id,
            gameId = rating.gameId,
            game = rating.game.toGameMap(),
            score = score.value ?: 0
        )
        viewModelScope.launch {
            if (FirebaseService.sendRating(newRating)) {
                _msg.value = appInstance.getString(R.string.rating_ok)
            }
            FirebaseService.updateRating(rating, (score.value ?: 0) - rating.score)
        }
    }

    fun removeRating() {
        viewModelScope.launch {
            if (FirebaseService.removeRating(rating)
            ) {
                _msg.value = appInstance.getString(R.string.remove_ok)

            } else {
                _msg.value = appInstance.getString(R.string.error)
            }
        }
    }
}