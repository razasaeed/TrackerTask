package com.example.contractsdemo.demo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.internal.jni.CoreArcGISRuntimeEnvironment
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.SimulatedLocationDataSource
import com.arcgismaps.location.SimulationParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.portal.Portal
import com.example.contractsdemo.R
import com.example.contractsdemo.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.time.Instant


class TrackingActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private var isTrackLocation: Boolean = false

    var latitude = 33.56535838687976
    var longitude = 73.0192457433643
    private val permissionRequestCode = 101
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    lateinit var binding: ActivityMainBinding

    private val mapView: MapView by lazy {
        binding.mapView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        setApiKey()
        lifecycle.addObserver(mapView)

        // create a center point for the data in West Los Angeles, California
        val center = Point(-13185535.98, 4037766.28, SpatialReference(102100))

        binding.apply {

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

        startLocationUpdates()

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

        // create a simulated location data source from json data with simulation parameters to set a consistent velocity
        val simulatedLocationDataSource = SimulatedLocationDataSource(
            Geometry.fromJsonOrNull(getString(R.string.polyline_data)) as Polyline,
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

    private fun startLocationUpdates() {
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d("cgecklloca", "$latitude and $longitude")
                // Do something with the latitude and longitude
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // Update interval in milliseconds
                10f, // Minimum distance to trigger updates in meters
                locationListener
            )
        }
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
                /*setupMap()
                addGraphics()*/
            }
        })
    }

}