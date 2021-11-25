package com.kappstudio.joboardgame.gamedetail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kappstudio.joboardgame.*
import com.kappstudio.joboardgame.databinding.FragmentGameDetailBinding
import com.kappstudio.joboardgame.tools.ToolsFragmentDirections
import tech.gujin.toast.ToastUtil
import timber.log.Timber
 
import android.R
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import kotlinx.coroutines.NonDisposableHandle.parent


class GameDetailFragment : Fragment() {
    val viewModel: GameDetailViewModel by viewModels {
        VMFactory {
            GameDetailViewModel(
                GameDetailFragmentArgs.fromBundle(requireArguments()).clickedGameId,
                appInstance.provideJoRepository()
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentGameDetailBinding.inflate(inflater)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val transition: Transition = Fade()
        transition.setDuration(600)
        transition.addTarget(binding.ivGame)
        if (container != null) {
            TransitionManager.beginDelayedTransition(container, transition)
        }

        binding.tvDesc.setOnClickListener {
            binding.tvDesc.text = viewModel.game.value?.desc ?: ""
        }

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnToBottle.setOnClickListener {
            findNavController().navigate(GameDetailFragmentDirections.navToBottleFragment())
        }
        binding.btnToDice.setOnClickListener {
            findNavController().navigate(GameDetailFragmentDirections.navToDiceFragment())
        }
        binding.btnToTimer.setOnClickListener {
            findNavController().navigate(GameDetailFragmentDirections.navToTimerFragment())
        }

        viewModel.game.observe(viewLifecycleOwner, {
            it?.let {
                Timber.d("game= $it")
                bindImage(binding.ivGame, it.image)



                bindTextViewGameTypes(binding.tvType, it.type)
                if (it.desc.length > 100) {
                    binding.tvDesc.text = it.desc.substring(0..100) + "···"
                } else {
                    binding.tvDesc.text = it.desc
                }

                viewModel.addViewedGame() //加入瀏覽紀錄
                viewModel.checkFavorite()
                viewModel.checkRating()
                viewModel.calAvgRating()

                if (it.tools.isNotEmpty()) {
                    binding.tvTitleTools.visibility = View.VISIBLE
                } else {
                    binding.tvTitleTools.visibility = View.GONE

                }
            }
        })



        viewModel.navToRating.observe(viewLifecycleOwner, {
            it?.let { it ->
                findNavController().navigate(GameDetailFragmentDirections.navToRatingDialog(it))
                viewModel.onNavToRating()
            }
        })
        viewModel.isFavorite.observe(viewLifecycleOwner, {

            Timber.d("isFavorite: $it")
            binding.cbFavorite.isChecked = it
        })


        return binding.root
    }

    override fun onResume() {

        super.onResume()
        Timber.d("onResume")
    }


}