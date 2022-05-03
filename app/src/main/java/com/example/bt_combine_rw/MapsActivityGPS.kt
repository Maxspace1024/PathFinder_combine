package com.example.bt_combine_rw

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.bt_combine_rw.databinding.ActivityMapsGpsBinding
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import kotlinx.android.synthetic.main.activity_maps_gps.*
import androidx.lifecycle.Transformations.map




class MapsActivityGPS : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsGpsBinding

    public var marker : Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsGpsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnSetGPSLoc.setOnClickListener {
            var llat = marker?.position?.latitude
            var llng = marker?.position?.longitude
            Toast.makeText(this@MapsActivityGPS,"LAT:$llat \nLNG:$llng",Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val perthLocation = LatLng(-31.90, 115.86)
        marker = mMap.addMarker(
            MarkerOptions()
                .position(perthLocation)
                .draggable(true)
        )
        mMap.setOnMarkerClickListener {
            Toast.makeText(this,"asdf",Toast.LENGTH_SHORT).show()
            true
        }
        mMap.setOnMarkerDragListener(object : OnMarkerDragListener{
            override fun onMarkerDragEnd(p0: Marker) {
                var llat = p0.position.latitude
                var llng = p0.position.longitude
                Toast.makeText(this@MapsActivityGPS,"LAT:$llat \nLNG:$llng",Toast.LENGTH_SHORT).show()
            }

            override fun onMarkerDrag(p0: Marker) {

            }

            override fun onMarkerDragStart(p0: Marker) {

            }
        })

        mMap.moveCamera(CameraUpdateFactory.newLatLng(perthLocation))
    }


}




