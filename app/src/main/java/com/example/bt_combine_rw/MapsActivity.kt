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
        pathtext = findViewById(R.id.Result)
        rebtn = findViewById(R.id.Returnbutton)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        rebtn.setOnClickListener {
//            val intent= Intent(this,MainActivity::class.java)
//            startActivity(intent)
            finish()
        }
    }

//    private fun getDirectionURL(origin:String,dest:String):String{
//        return "https://maps.googleapis.com/maps/api/directions/json?language=zh-TW&origin=${origin}&destination=${dest}&key=AIzaSyBme10Ce7rnKYnAYQnWlHb-JJ-byyEcRq0&mode=walking"
//    }

    private fun getDirectionURL(origin:LatLng,dest:String):String{
        return "https://maps.googleapis.com/maps/api/directions/json?language=zh-TW&origin=${origin.latitude},${origin.longitude}&destination=${dest}&key=AIzaSyBme10Ce7rnKYnAYQnWlHb-JJ-byyEcRq0&mode=walking"
    }
//    fun decodePolyline(encoded: String): List<LatLng> {
//
//        val poly = ArrayList<LatLng>()
//        var index = 0
//        val len = encoded.length
//        var lat = 0
//        var lng = 0
//
//        while (index < len) {
//            var b: Int
//            var shift = 0
//            var result = 0
//            do {
//                b = encoded[index++].toInt() - 63
//                result = result or (b and 0x1f shl shift)
//                shift += 5
//            } while (b >= 0x20)
//            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
//            lat += dlat
//
//            shift = 0
//            result = 0
//            do {
//                b = encoded[index++].toInt() - 63
//                result = result or (b and 0x1f shl shift)
//                shift += 5
//            } while (b >= 0x20)
//            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
//            lng += dlng
//
//            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
//            poly.add(latLng)
//        }
//
//        return poly
//    }

    // dot product
    private fun dot(vector:List<Double>,vector2:List<Double>) = vector[0]*vector2[0] + vector[1]*vector2[1]

    // calculate the length of vector
    private fun len(vector:List<Double>):Double = sqrt(vector[0]*vector[0] + vector[1]*vector[1])

    // get the vector of two coordinate
    private fun getVector(coordinate:LatLng, coordinate2:LatLng):List<Double>{
        val x = coordinate2.longitude - coordinate.longitude
        val y = coordinate2.latitude - coordinate.latitude
        return listOf(x,y)
    }

    // calculate the angle with given vector and north
    private fun angle(coordinate:LatLng, coordinate2:LatLng):Double{
        val vector = getVector(coordinate,coordinate2)
        val north = listOf(0.0,1.0)
        val angle = acos(dot(north,vector)/(1*len(vector))) * 180 / PI
        if (vector[0] < 0) return (360.0 - angle)
        return angle
    }
    fun locationManager():Location? {
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
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12.0f))

            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

        }
        try{
            if (isGPSEnabled && isNetworkEnabled){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    5000, 0f, locationListener)
                oriLocation1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    5000, 0f, locationListener)
                oriLocation2 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (oriLocation1 != oriLocation2) oriLocation = oriLocation2
            }
            else if (isGPSEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    5000, 0f, locationListener)
                oriLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    5000, 0f, locationListener)
                oriLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
        }catch(ex: SecurityException) {
            Log.d("myTag", "Security Exception, no location available")
        }
        return oriLocation
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
        lateinit var originloc: LatLng
        lateinit var destloc: LatLng

        val intent = intent
        val locList = intent.getSerializableExtra("path") as ArrayList<LatLng>
        val result = intent.getSerializableExtra("result") as ArrayList<List<LatLng>>
        /*
//        val origin = intent.getStringExtra("origin")
//        val dest = intent.getStringExtra("dest")
//        var addressList: List<Address>? = null
//        var addressList2: List<Address>? = null
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
//            mMap.isMyLocationEnabled = true
//        }


        val gpsloc = locationManager()

//        if (dest == null || dest == ""){
//            Toast.makeText(applicationContext,"provide location", Toast.LENGTH_SHORT).show()
//            return
//        }
        if (gpsloc == null) {
            Toast.makeText(applicationContext,"Please open the GPS", Toast.LENGTH_SHORT).show()
            return
        }
        val gpslng = LatLng(gpsloc.latitude,gpsloc.longitude)



//        val geoCoder = Geocoder(this)
//        try{
//            addressList = geoCoder.getFromLocationName(origin,1)
//            addressList2 = geoCoder.getFromLocationName(dest,1)
//        }
//        catch(e: java.io.IOException){
//            e.printStackTrace()
//        }
        val lineoption = PolylineOptions()
        var pathAngle = ""
//        val locList = ArrayList<LatLng>()
//        val address = addressList!![0]
//        val address2 = addressList2!![0]
//        Log.i("addr",String.format("size %d",addressList.size))
//        originloc = LatLng(address.latitude,address.longitude)
//        destloc = LatLng(address2.latitude,address2.longitude)
        val client = OkHttpClient().newBuilder().build()
        val request = Request.Builder().url(getDirectionURL(gpslng,dest)).build()
        CoroutineScope(Dispatchers.IO).launch {
            val response = client.newCall(request).execute()
            response.body?.run{
                val data = string()
                val result =  ArrayList<List<LatLng>>()
                val respObj = Gson().fromJson(data,GoogleMapDTO::class.java)

                val path =  ArrayList<LatLng>()

                originloc = LatLng(respObj.routes[0].legs[0].start_location.lat.toDouble()
                    ,respObj.routes[0].legs[0].start_location.lng.toDouble())
                destloc = LatLng(respObj.routes[0].legs[0].end_location.lat.toDouble()
                    ,respObj.routes[0].legs[0].end_location.lng.toDouble())

                locList.add(originloc)

                for (i in 0 until (respObj.routes[0].legs[0].steps.size)){
                    val startLatLng = LatLng(respObj.routes[0].legs[0].steps[i].start_location.lat.toDouble()
                        ,respObj.routes[0].legs[0].steps[i].start_location.lng.toDouble())
                    path.add(startLatLng)
                    val endLatLng = LatLng(respObj.routes[0].legs[0].steps[i].end_location.lat.toDouble()
                        ,respObj.routes[0].legs[0].steps[i].end_location.lng.toDouble())
                    path.add(endLatLng)
                    locList.add(endLatLng)
//                            path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                locList.add(destloc)
                result.add(path)
                */
        val lineoption = PolylineOptions()
        for (i in result.indices) {
            lineoption.addAll(result[i])
            lineoption.width(15f)
            lineoption.color(Color.GREEN)
            lineoption.geodesic(true)
        }
//                pathAngle += "Angle: "
//                pathAngle += angle(locList[0],locList[1])


//                pathAngle += "startpoint->"
//                for (i in 1 until (locList.size-1)){
//                    pathAngle += String.format("%.1f",angle(locList[i-1],locList[i]))
//                    pathAngle += "->"
//                }
//                pathAngle += "destination"
//            }
//            withContext(Dispatchers.Main){
        val startpoint = LatLng(locList[0].latitude,locList[0].longitude)
        val destination = LatLng(locList[locList.size-1].latitude,locList[locList.size-1].longitude)
//                for (i in 1 .. (locList.size-2)) {
//                    mMap.addMarker(MarkerOptions().position(locList[i]).title("Marker in path").icon(BitmapDescriptorFactory.fromResource(R.drawable.dot)).anchor(0.5f,0.5f))
//                }
        mMap.addMarker(MarkerOptions().position(startpoint).title("Marker in start point"))
        mMap.addMarker(MarkerOptions().position(destination).title("Marker in Destination"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startpoint, 15.0f))
        mMap.addPolyline(lineoption)
//                pathtext.text = pathAngle
        pathtext.setTextSize(TypedValue.COMPLEX_UNIT_PX,40f)

        Toast.makeText(applicationContext,startpoint.latitude.toString()+" "+startpoint.longitude.toString(),
            Toast.LENGTH_LONG).show()
        Toast.makeText(applicationContext,destination.latitude.toString()+" "+destination.longitude.toString(),
            Toast.LENGTH_LONG).show()
    }
}
//
//    }
//}