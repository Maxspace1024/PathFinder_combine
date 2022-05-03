package com.example.bt_combine_rw

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore.Images.Media.getBitmap
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.bt_combine_rw.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var pathtext: TextView
    private lateinit var rebtn: Button
    private lateinit var thisView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        supportActionBar?.hide()

        thisView = window.decorView
        rebtn = findViewById(R.id.Returnbutton)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        rebtn.setOnClickListener {
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        lateinit var originloc: LatLng
        lateinit var destloc: LatLng

        val intent = intent
        val locList = intent.getSerializableExtra("path") as ArrayList<LatLng>
        val result = intent.getSerializableExtra("result") as ArrayList<List<LatLng>>

        val lineoption = PolylineOptions()
        for (i in result.indices) {
            lineoption.addAll(result[i])
            lineoption.width(15f)
            lineoption.color(Color.GREEN)
            lineoption.geodesic(true)
        }

        val startpoint = LatLng(locList[0].latitude,locList[0].longitude)
        val destination = LatLng(locList[locList.size-1].latitude,locList[locList.size-1].longitude)

        for (i in 1 .. (locList.size-2)) {
            mMap.addMarker(MarkerOptions().position(locList[i]).title("Marker in path").icon(BitmapDescriptorFactory.fromResource(R.drawable.dot)).anchor(0.5f,0.5f))
        }
        mMap.addMarker(MarkerOptions().position(startpoint).title("Marker in start point"))
        mMap.addMarker(MarkerOptions().position(destination).title("Marker in Destination"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startpoint, 15.0f))
        mMap.addPolyline(lineoption)

        Toast.makeText(applicationContext,startpoint.latitude.toString()+" "+startpoint.longitude.toString(),
            Toast.LENGTH_LONG).show()
        Toast.makeText(applicationContext,destination.latitude.toString()+" "+destination.longitude.toString(),
            Toast.LENGTH_LONG).show()
    }
}