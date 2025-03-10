package com.kappstudio.joboardgame.ui.map

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.dylanc.activityresult.launcher.EnableLocationLauncher
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.kappstudio.joboardgame.databinding.FragmentMapBinding
import com.kappstudio.joboardgame.util.ToastUtil
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.kappstudio.joboardgame.R
import com.kappstudio.joboardgame.appInstance
import com.kappstudio.joboardgame.data.Party
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

const val taipeiLatitude = 25.03870345095808
const val taipeiLongitude = 121.53238092570203

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private lateinit var binding: FragmentMapBinding
    private var locationPermissionOk = false
    private lateinit var mMap: GoogleMap
    private lateinit var mLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallBack: LocationCallback
    private var locationIsUpdate = false
    private lateinit var enableLocationLauncher: EnableLocationLauncher
    private val viewModel: MapViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = FragmentMapBinding.inflate(inflater)

        enableLocationLauncher = EnableLocationLauncher(this)
        mLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context ?: appInstance)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnGetLocation.setOnClickListener {
            getPermission()
        }

        return binding.root
    }

    private fun getPermission() {
        PermissionX.init(this)
            .permissions(Manifest.permission.ACCESS_FINE_LOCATION)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    getString(R.string.permission_msg),
                    getString(R.string.ok),
                    getString(R.string.cancel)
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    getString(R.string.permission_setting_msg),
                    getString(R.string.ok),
                    getString(R.string.cancel)
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    locationPermissionOk = true
                    ToastUtil.show(getString(R.string.geting_location))
                    checkGPS()
                }
            }
    }

    private fun checkGPS() {
        val locationManager =
            context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            context?.let {
                AlertDialog.Builder(it)
                    .setTitle(getString(R.string.no_gps))
                    .setMessage(getString(R.string.need_gps))
                    .setPositiveButton(getString(R.string.go_to_open)) { _, _ ->
                        enableLocationLauncher.launch { enabled ->
                            if (enabled) {
                                checkGPS()
                            }
                        }
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        try {
            if (!locationPermissionOk) {
                getPermission()
                return
            }

            ToastUtil.show(getString(R.string.geting_location))

            binding.btnGetLocation.visibility = View.GONE

            mMap.isMyLocationEnabled = true
            var currentLocationCount = 0
            val locationRequest = LocationRequest()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 1000

            mLocationProviderClient.requestLocationUpdates(
                locationRequest,

                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationCallBack = this
                        locationIsUpdate = true

                        currentLocationCount++
                        locationResult.lastLocation?.let {
                            val currentLocation = LatLng(
                                it.latitude,
                                it.longitude
                            )

                            if (currentLocationCount == 1) {
                                mMap.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        currentLocation, 15f
                                    )
                                )
                            }
                        }
                    }
                },
                null
            )

        } catch (e: SecurityException) {
            ToastUtil.show(getString(R.string.cant_get_location))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        viewModel.parties.observe(viewLifecycleOwner) {
            addPartiesMark()
            val selectedPartyId: String? = MapFragmentArgs.fromBundle(requireArguments()).partyId
            if (selectedPartyId == null) {
                moveToLocation(
                    LatLng(taipeiLatitude, taipeiLongitude)
                )
            } else {
                val party = viewModel.parties.value?.filter { it.id == selectedPartyId }?.get(0)
                if (party != null) {
                    addMark(party)
                    moveToLocation(
                        LatLng(
                            party.location.lat,
                            party.location.lng
                        )
                    )
                }
            }
        }

        mMap.setInfoWindowAdapter(context?.let { PartyInfoWindowAdapter(it, viewModel) })
        mMap.setOnInfoWindowClickListener(this)
    }

    private fun moveToLocation(latLng: LatLng) {
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng, 15f
            )
        )
    }

    private fun addPartiesMark() {
        viewModel.parties.value?.forEach { party ->
            if (party.partyTime + 3600000 >= Calendar.getInstance().timeInMillis) {
                addMark(party)
            }
        }
    }

    private fun addMark(party: Party) {
        CoroutineScope(Dispatchers.IO).launch {
            val partyLocation = LatLng(
                party.location.lat,
                party.location.lng
            )
            activity?.runOnUiThread {
                mMap.addMarker(
                    MarkerOptions()
                        .position(partyLocation)
                        .title(party.title)
                        .snippet(party.id)
                )?.apply {
                    showInfoWindow()
                }
            }
        }
    }

    override fun onInfoWindowClick(marker: Marker) {
        marker.snippet?.let {
            findNavController().navigate(
                MapFragmentDirections.navToPartyDetailFragment(it)
            )
        }
    }
}