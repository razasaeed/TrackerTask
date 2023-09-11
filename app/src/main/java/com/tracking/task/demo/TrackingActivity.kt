package com.tracking.task.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.SimulatedLocationDataSource
import com.arcgismaps.location.SimulationParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.google.android.material.snackbar.Snackbar
import com.tracking.task.databinding.ActivityMainBinding
import com.tracking.task.showToast
import kotlinx.coroutines.launch
import java.time.Instant

class TrackingActivity : AppCompatActivity() {

    private var isTrackLocation: Boolean = false
    private val permissionRequestCode = 101
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

        viewModel.sendEvent(DemoEvent.GetLocationsData)

        lifecycle.addObserver(mapView)

        if (ContextCompat.checkSelfPermission(this@TrackingActivity, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@TrackingActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                permissionRequestCode
            )
        }

    }

    private fun showNavigation(locations: String?) {
        // create a center point for the data in West Los Angeles, California
        val center = Point(-13185535.98, 4037766.28, SpatialReference(102100))

        // create a graphics overlay for the points and use a red circle for the symbols
        val locationHistoryOverlay = GraphicsOverlay()
        val locationSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 10f)
        locationHistoryOverlay.renderer = SimpleRenderer(locationSymbol)

        // create a graphics overlay for the lines connecting the points and use a blue line for the symbol
        val locationHistoryLineOverlay = GraphicsOverlay()
        val locationLineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.green, 2.0f)
        locationHistoryLineOverlay.renderer = SimpleRenderer(locationLineSymbol)

        mapView.apply {
            // create and add a map with a navigation night basemap style
            map = ArcGISMap(BasemapStyle.ArcGISNavigationNight)
            setViewpoint(Viewpoint(center, 7000.0))
            graphicsOverlays.addAll(listOf(locationHistoryOverlay, locationHistoryLineOverlay))
        }

        // create a polyline builder to connect the location points
        val polylineBuilder = PolylineBuilder(SpatialReference(102100))

        // create a simulated location data source from json data with simulation parameters to set
        // a consistent velocity
        val simulatedLocationDataSource = SimulatedLocationDataSource(
            Geometry.fromJsonOrNull(locations.toString()) as Polyline,
            SimulationParameters(Instant.now(), 30.0, 0.0, 0.0)
        )

        // coroutine scope to collect data source location changes
        lifecycleScope.launch {
            simulatedLocationDataSource.locationChanged.collect { location ->
                // if location tracking is turned off, do not add to the polyline
                if (!isTrackLocation) {
                    return@collect
                }
                // get the point from the location
                val nextPoint = location.position
                // add the new point to the polyline builder
                polylineBuilder.addPoint(nextPoint)
                // add the new point to the two graphics overlays and reset the line connecting the points
                locationHistoryOverlay.graphics.add(Graphic(nextPoint))
                locationHistoryLineOverlay.graphics.apply {
                    clear()
                    add((Graphic(polylineBuilder.toGeometry())))
                }
            }
        }

        // configure the map view's location display to follow the simulated location data source
        mapView.locationDisplay.apply {
            dataSource = simulatedLocationDataSource
            setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
            initialZoomScale = 7000.0
        }

        // coroutine scope to set a tap event on the map view
        lifecycleScope.launch {
            mapView.onSingleTapConfirmed.collect {
                if (mapView.locationDisplay.autoPanMode.value == LocationDisplayAutoPanMode.Off) {
                    mapView.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
                }
                if (isTrackLocation) {
                    isTrackLocation = false
                    Snackbar.make(mapView, "Tracking has stopped", Snackbar.LENGTH_INDEFINITE).show()
                } else {
                    isTrackLocation = true
                    Snackbar.make(mapView, "Tracking has started", Snackbar.LENGTH_INDEFINITE ).show()
                }
            }
        }

        // coroutine scope to start the simulated location data source
        lifecycleScope.launch {
            simulatedLocationDataSource.start()
        }
    }

    private fun collectState() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect { state ->
                binding.progressBar.isVisible = state.isLoading
                if (!state.isLoading) {
                    state.locationsResult.let {
                        showNavigation(it)
                    }
                }
            }
        }
    }

    private fun collectEffect() {
        lifecycleScope.launchWhenStarted {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is DemoEffect.ErrorWithGetLocations -> {
                        showToast(effect.message)
                        binding.progressBar.isVisible = false
                    }
                }
            }
        }
    }

}