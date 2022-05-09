package com.example.bt_combine_rw

import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
            //Toast.makeText(this@MapsActivityGPS,"LAT:$llat \nLNG:$llng",Toast.LENGTH_SHORT).show()
            // back latlng
            val intentxx = Intent()
            intentxx.putExtra("llat","$llat")
            intentxx.putExtra("llng","$llng")
            setResult(1111,intentxx)

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

        val gpsloc = locationManager()
        var perthLocation = LatLng(24.123552,120.6731373)

        if(gpsloc == null){
            Toast.makeText(applicationContext,"Please open the GPS", Toast.LENGTH_SHORT).show()
        }
        else{
            perthLocation = LatLng( gpsloc!!.latitude, gpsloc.longitude)
        }

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

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(perthLocation,18.0f))
    }


    fun locationManager(): Location? {
        var oriLocation: Location? = null
        val oriLocation1: Location?
        val oriLocation2: Location?
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        val isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if(oriLocation == null) {
                    oriLocation = location
                }
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

        }
        try{
            if (isGPSEnabled && isNetworkEnabled){
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000, 0f, locationListener)
                oriLocation1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000, 0f, locationListener)
                oriLocation2 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (oriLocation1 != oriLocation2) oriLocation = oriLocation2
            }
            else if (isGPSEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000, 0f, locationListener)
                oriLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000, 0f, locationListener)
                oriLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
        }catch(ex: SecurityException) {
            Log.d("myTag", "Security Exception, no location available")
        }
        return oriLocation
    }

}




