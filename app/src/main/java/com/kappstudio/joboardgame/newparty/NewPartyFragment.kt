package com.kappstudio.joboardgame.newparty

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kappstudio.joboardgame.databinding.FragmentNewPartyBinding
import com.kappstudio.joboardgame.favorite.FavoriteFragmentDirections
import com.dylanc.activityresult.launcher.StartActivityLauncher
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.irozon.alertview.AlertActionStyle
import com.irozon.alertview.AlertStyle
import com.irozon.alertview.AlertView
import com.irozon.alertview.objects.AlertAction
import com.kappstudio.joboardgame.R

import com.kappstudio.joboardgame.appInstance
import com.kappstudio.joboardgame.data.source.remote.LoadApiStatus
import com.kappstudio.joboardgame.util.closeKeyBoard
import com.kappstudio.joboardgame.util.closeSoftKeyboard
import tech.gujin.toast.ToastUtil
import timber.log.Timber

class NewPartyFragment : Fragment() {

    lateinit var binding: FragmentNewPartyBinding
    val viewModel: NewPartyViewModel by viewModels()

    lateinit var startActivityLauncher: StartActivityLauncher



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNewPartyBinding.inflate(inflater)
        startActivityLauncher = StartActivityLauncher(this)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.btnAddCover.setOnClickListener {
            pickImage()
        }


        var isPickingTime = false

        var r = SingleDateAndTimePickerDialog.Builder(context)
            .bottomSheet()
            .curved()
            .backgroundColor(appInstance.getColor(R.color.white))
            .mainColor(appInstance.getColor(R.color.blue_8187ff))
            .titleTextColor(appInstance.getColor(R.color.blue_8187ff))
            .displayListener {  }
            .displayListener {
                isPickingTime = true
            }
            .title(getString(R.string.data_time))
            .listener { date ->
                binding.etTime.setText(date.toString())
                viewModel.time.value = date.time
                isPickingTime = false

            }


        binding.etTime.setOnClickListener {
            closeSoftKeyboard(binding.etTitle)
            r.display()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                     if (isPickingTime){
                        r.close()
                        isPickingTime = false
                    }else{
                        findNavController().popBackStack()
                    }

                }
            })

        binding.etLocation.setOnClickListener {
            startAutoCompleteIntent()
        }


        viewModel.invalidPublish.observe(viewLifecycleOwner, {
            it?.let {
                when(it){
                    InvalidInput.TITLE_EMPTY ->ToastUtil.show("請填寫標題!")
                    InvalidInput.TIME_EMPTY ->ToastUtil.show("請選擇時間!")
                    InvalidInput.LOCATION_EMPTY ->ToastUtil.show("請填寫地點!")
                    InvalidInput.QTY_EMPTY ->ToastUtil.show("請填寫徵求人數!")
                    InvalidInput.DESC_EMPTY ->ToastUtil.show("請填寫說明!")
                    InvalidInput.GAMES_EMPTY ->ToastUtil.show("請加入遊戲!")
                }
            }
        })



        viewModel.navToGameDetail.observe(viewLifecycleOwner, {
            it?.let {


                if (it.id == "notFound") {


                    val alert = AlertView(
                        getString(R.string.no_game) + it.name,
                        "",
                        AlertStyle.BOTTOM_SHEET
                    )
                    alert.addAction(
                        AlertAction(
                            getString(R.string.open_browser),
                            AlertActionStyle.POSITIVE
                        ) { _ ->
                            // Action 1 callback
                            val uri = Uri.parse(getString(R.string.google_search) + it.name)
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            activity?.startActivity(intent)
                        })
                    alert.addAction(
                        AlertAction(
                            getString(R.string.cancel),
                            AlertActionStyle.DEFAULT
                        ) { _ ->
                            // Action 2 callback
                        })

                    alert.show(activity as AppCompatActivity)


                } else {
                    findNavController().navigate(
                        FavoriteFragmentDirections.navToGameDetailFragment(it.id)
                    )
                }
                viewModel.onNavToGameDetail()
            }
        })
        viewModel.games.observe(viewLifecycleOwner, {
            Timber.d("games: $it")
            binding.rvGame.adapter = AddGameAdapter(viewModel).apply {
                submitList(it.reversed())
            }
        })

        viewModel.status.observe(viewLifecycleOwner, {
            when (it) {
                LoadApiStatus.DONE -> findNavController().popBackStack()
            }
        })

        return binding.root
    }

    private fun pickImage() {
        ImagePicker.with(this)
            .crop(
                2f,
                1f
            )                    //Crop image(Optional), Check Customization for more option
            .compress(1024)            //Final image size will be less than 1 MB(Optional)
            .maxResultSize(
                1080,
                540
            )    //Final image resolution will be less than 1080 x 1080(Optional)
            .createIntent { intent ->
                startActivityLauncher.launch(intent) { resultCode, data ->
                    when (resultCode) {
                        Activity.RESULT_OK -> {
                            data?.let {
                                val fileUri = data.data
                                binding.ivCover.setImageURI(fileUri)
                                viewModel.photoUri.value = fileUri
                            }
                        }
                        ImagePicker.RESULT_ERROR -> {
                            ToastUtil.show(ImagePicker.getError(data))
                        }
                    }
                }
            }
    }

    private fun startAutoCompleteIntent() {

        // Set the fields to specify which types of place data to
        // return after the user has made a selection.
        val fields = listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG)

        // Start the autocomplete intent.
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)

            .setTypeFilter(TypeFilter.ADDRESS)
            .build(activity)

        startActivityLauncher.launch(intent) { resultCode, data ->
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(it)
                        binding.etLocation.setText(place.address)
                        viewModel.lat.value = place.latLng.latitude
                        viewModel.lng.value = place.latLng.longitude

                        Timber.d("Place: ${place.name}, ${place.id}")
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Timber.d(status.statusMessage)
                    }
                }
            }
        }
    }





}