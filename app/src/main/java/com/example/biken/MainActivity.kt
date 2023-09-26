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
    private var distanceTravelled = 0f // en mètres
    private var scoreThreshold = 200f  // Distance requise pour augmenter le score
    private var lastLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private val recentLocations = mutableListOf<Location>()

    private val sharedPref by lazy {
        getSharedPreferences("BikenPrefs", Context.MODE_PRIVATE)
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            location?.let {
                if (it.accuracy > 5) { // ignorez les mises à jour avec une précision inférieure à 5 mètres
                    return
                }

                recentLocations.add(it)
                recentLocations.retainAll { loc -> loc.time >= it.time - 5000 }

                if (lastLocation != null) {
                    val distanceInMeters = lastLocation!!.distanceTo(it)
                    distanceTravelled += distanceInMeters

                    val timeDifference = (it.time - lastLocation!!.time) / 1000
                    val speed: Float = if (timeDifference > 0) {
                        (distanceInMeters / timeDifference) * 3.6f
                    } else {
                        location.speed * 3.6f
                    }

                    val recentDistance = recentLocations.takeIf { it.size > 1 }?.mapIndexed { index, loc ->
                        if (index == 0) 0f else recentLocations[index - 1].distanceTo(loc)
                    }?.sum() ?: 0f

                    if (recentDistance < 5) {
                        findViewById<TextView>(R.id.tv_speed).text = "Vitesse: 0.0 Km/h"
                    } else {
                        findViewById<TextView>(R.id.tv_speed).text = "Vitesse: ${speed.toInt()} Km/h"
                    }

                    val distanceInKm = truncateTo1DecimalPlace(distanceTravelled / 1000)
                    findViewById<TextView>(R.id.tv_distance).text = "Distance: $distanceInKm Km"

                    if (distanceTravelled >= scoreThreshold) {
                        score += 1
                        scoreThreshold += 200f // Augmente le seuil pour le prochain point de score
                        findViewById<TextView>(R.id.tv_score).text = "Score: $score"
                    }
                }
                lastLocation = it
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) { }

        override fun onProviderEnabled(provider: String) { }

        override fun onProviderDisabled(provider: String) { }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        score = sharedPref.getInt("score", 0)
        distanceTravelled = sharedPref.getFloat("distanceTravelled", 0f)
        scoreThreshold = sharedPref.getFloat("scoreThreshold", 200f)
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
            scoreThreshold = 200f
            findViewById<TextView>(R.id.tv_score).text = "Score: $score"
            findViewById<TextView>(R.id.tv_distance).text = "Distance: 0 Km"
            with(sharedPref.edit()) {
                putInt("score", score)
                putFloat("distanceTravelled", distanceTravelled)
                putFloat("scoreThreshold", scoreThreshold)
                apply()
            }
        }
    }

    private fun startTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }

        distanceTravelled = 0f // réinitialiser à chaque démarrage du suivi
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
            putFloat("scoreThreshold", scoreThreshold)
            apply()
        }
    }

    private fun truncateTo1DecimalPlace(value: Float): Float {
        return (value * 10).toInt() / 10f
    }
}
