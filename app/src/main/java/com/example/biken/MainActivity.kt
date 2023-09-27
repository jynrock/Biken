package com.example.biken

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private var score = 0
    private var distanceTravelled = 0f
    private var lastLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private val recentLocations = mutableListOf<Location>()

    private val sharedPref by lazy {
        getSharedPreferences("BikenPrefs", Context.MODE_PRIVATE)
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.accuracy > 5) {
                return
            }

            recentLocations.add(location)
            recentLocations.retainAll { loc -> loc.time >= location.time - 5000 }

            if (lastLocation != null) {
                val distanceInMeters = lastLocation!!.distanceTo(location)
                distanceTravelled += distanceInMeters

                if (distanceTravelled >= 200) {
                    score++
                    distanceTravelled = 0f
                    findViewById<TextView>(R.id.tv_score).text = "Score: $score"
                }

                findViewById<TextView>(R.id.tv_distance).text = "Distance: ${distanceTravelled.toInt()} m"
            }

            lastLocation = location
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        score = sharedPref.getInt("score", 0)
        findViewById<TextView>(R.id.tv_score).text = "Score: $score"

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val btnStartStop = findViewById<Button>(R.id.btn_start_stop)
        btnStartStop.setOnClickListener {
            if (btnStartStop.text == "Start") {
                startTracking()
                btnStartStop.text = "Stop"
            } else {
                stopTracking()
                btnStartStop.text = "Start"
            }
        }

        val btnReset = findViewById<Button>(R.id.btn_reset)
        btnReset.setOnClickListener {
            score = 0
            distanceTravelled = 0f
            findViewById<TextView>(R.id.tv_score).text = "Score: 0"
            findViewById<TextView>(R.id.tv_distance).text = "Distance: 0 m"
        }
    }

    private fun startTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }

        distanceTravelled = sharedPref.getFloat("distanceTravelled", 0f)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 5f, locationListener)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 5f, locationListener)
    }

    private fun stopTracking() {
        locationManager.removeUpdates(locationListener)
    }

    override fun onPause() {
        super.onPause()
        with(sharedPref.edit()) {
            putInt("score", score)
            putFloat("distanceTravelled", distanceTravelled)
            apply()
        }
    }

    private fun truncateTo1DecimalPlace(value: Float): Float {
        return (value * 10).toInt() / 10f
    }
}
