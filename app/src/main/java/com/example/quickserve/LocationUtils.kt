package com.example.quickserve

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import java.util.Locale

object LocationUtils {

    suspend fun getCurrentLocation(context: Context): LatLng? {
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }

            val location = fusedLocationClient.lastLocation.await()
            location?.let {
                LatLng(it.latitude, it.longitude)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getAddressFromLocation(context: Context, lat: Double, lng: Double, onResult: (String) -> Unit) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressLine = address.getAddressLine(0)
                onResult(addressLine)
            } else {
                onResult("Selected location on map")
            }
        } catch (e: Exception) {
            onResult("Selected location on map")
        }
    }
}