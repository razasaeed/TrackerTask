package com.example.contractsdemo.demo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.internal.jni.CoreArcGISRuntimeEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.portal.Portal
import com.example.contractsdemo.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener


class MainActivity : AppCompatActivity() {

    var latitude = 33.56535838687976
    var longitude = 73.0192457433643
    private val permissionRequestCode = 101
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    lateinit var binding: ActivityMainBinding
    private val viewModel: DemoViewModel by viewModels()

    private val mapView: MapView by lazy {
        binding.mapView
    }

    init {
        collectState()
        collectEffect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycle.addObserver(mapView)
        setApiKey()
        setupMap()
        addGraphics()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
        getUserLocation()

        binding.apply {

            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    permissionRequestCode
                )
            }

        }

        binding.btnGetUsers.setOnClickListener {
            viewModel.sendEvent(DemoEvent.GetUsers)
        }
    }

    private fun setupMap() {
        val map = ArcGISMap(BasemapStyle.ArcGISNavigation)
        mapView.map = map
        mapView.setViewpoint(Viewpoint(latitude, longitude, 72000.0))
    }

    private fun setApiKey() {
        CoreArcGISRuntimeEnvironment.setAPIKey("AAPK3e33c87e49b949c4864fba74e7fc266ajhJadeeEjmft-F5cVu2cOdqRlz6mz4DynMR02A_vhh4ZJWKfyuXdJqxIl88ttwim")
        ArcGISEnvironment.apiKey = ApiKey.create("AAPK3e33c87e49b949c4864fba74e7fc266ajhJadeeEjmft-F5cVu2cOdqRlz6mz4DynMR02A_vhh4ZJWKfyuXdJqxIl88ttwim")
    }

    private fun addGraphics() {
        val graphicsOverlay = GraphicsOverlay()
        mapView.graphicsOverlays.add(graphicsOverlay)
        val point = Point(longitude, latitude, SpatialReference.wgs84())

        // create a point symbol that is an small red circle
        val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 10f)

        // create a blue outline symbol and assign it to the outline property of the simple marker symbol
        val blueOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(0, 0, 255), 2f)
        simpleMarkerSymbol.outline = blueOutlineSymbol

        // create a graphic with the point geometry and symbol
        val pointGraphic = Graphic(point, simpleMarkerSymbol)

        // add the point graphic to the graphics overlay
        graphicsOverlay.graphics.add(pointGraphic)

        // Create a polylineBuilder with a spatial reference and add three points to it.
        // Then get the polyline from the polyline builder

        // create a blue line symbol for the polyline
        val polylineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(0, 0, 255), 3f)
        val polylineBuilder = PolylineBuilder(SpatialReference.wgs84()) {
            addPoint(73.01974041264937, 33.565745963894315)
            addPoint(73.0213277842358, 33.56576441989921)
            addPoint(73.01990284136984, 33.5664903529637)
        }
        val polyline = polylineBuilder.toGeometry()

        // create a polyline graphic with the polyline geometry and symbol
        val polylineGraphic = Graphic(polyline, polylineSymbol)

        // add the polyline graphic to the graphics overlay
        graphicsOverlay.graphics.add(polylineGraphic)
    }

    private fun collectState() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect { state ->
                if (state.isLoading) {
                    // show loading
                } else {
                    state.usersResult.let {
                        Log.d("usersResult", it?.size.toString())
                    }
                }
            }
        }
    }

    private fun collectEffect() {
        lifecycleScope.launchWhenStarted {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is DemoEffect.ErrorWithGetUsers -> {
                        effect.message
                    }
                }
            }
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location> { location ->
            if (location != null) {
                Log.d("checklocatin", "${location.latitude} and ${location.longitude}")
                latitude = location.latitude
                longitude = location.longitude
                setupMap()
                addGraphics()
            }
        })
    }

}