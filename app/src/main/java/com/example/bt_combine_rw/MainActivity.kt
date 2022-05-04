package com.example.bt_combine_rw

import  android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.app.TaskStackBuilder.create
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.*
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat.create
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.internal.UnsafeAllocator.create
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.ByteString.Companion.encode
import java.io.*
import java.lang.Exception
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt
import kotlinx.coroutines.launch as launch

class MainActivity : AppCompatActivity() {
    private lateinit var serverThread : ServerThread
    private lateinit var clientThread : ClientThread
    private var sendReceive : SendReceive? = null

    private lateinit var deamonQueueInspecter : DeamonQueueInspector
    private val deamonQ = ConcurrentLinkedQueue<UserInfo>()

    // name , rot degree
    private var resultString : String = ""

    private var delayTime : Long = 200
    private var direction_info_dealyTime : Long = 6000
    private var SERVER_NAME = "maxspace"
    private var terminalType : String? = ""

    lateinit var btManager : BluetoothManager
    lateinit var btAdapter : BluetoothAdapter
    lateinit var btDeviceSet: Set<BluetoothDevice>

    enum class STATEE{
        LISTENING,CONNECTING,CONNECTED,CONNECTION_FAIL,MSG_RECV,
        STREAM_DONE,UPDATE_DIRECTION_INFO
    }

    private val APP_NAME : String = "BT_CHAT"
    private val MY_UUID : UUID = UUID.fromString("6bb9c936-27e4-4042-b247-3e4566900da1")

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        //etServName.setText(SERVER_NAME)

        btManager = getSystemService(BluetoothManager::class.java)
        btAdapter = btManager.adapter
        if(btAdapter==null){
            Log.d("info","no support bt")
        }
        else{
            if(!btAdapter.isEnabled) {
                //if bt is closed
                btAdapter.enable()
                Toast.makeText(this,"BT is opening!!",Toast.LENGTH_SHORT).show()
            }

            terminalType = intent.extras?.getString("type")
            if(terminalType == getString(R.string.TYPE_SERVER)){
                //server side
                //llllClientSide.visibility = View.GONE
                llllClientSide2.visibility = View.GONE
                llllServerSide.visibility = View.VISIBLE

                serverThread = ServerThread()
                serverThread.start()

                deamonQueueInspecter = DeamonQueueInspector()
                deamonQueueInspecter.start()
            }
            else if(terminalType == getString(R.string.TYPE_CLIENT)){
                //client side
                //llllClientSide.visibility = View.GONE
                llllClientSide2.visibility = View.VISIBLE
                llllServerSide.visibility = View.GONE

            }
        }

        btnSendInfo.setOnClickListener {
            btDeviceSet = btAdapter.bondedDevices
            btDeviceSet.forEach {
                Log.d("dev",it.name)
            }
            var btDevice = btDeviceSet.find{it.name == SERVER_NAME}

            // get the rotate degree
            val locList = ArrayList<LatLng>()
            val result = ArrayList<List<LatLng>>()
            val path = ArrayList<LatLng>()
            val joint = ArrayList<LatLng>()
            val gpsloc = locationManager()
            var flag = true

            val destStr = dest.text.toString()
            if (destStr == null || destStr == "") {
                Toast.makeText(applicationContext, "provide location", Toast.LENGTH_SHORT).show()
            }
            else if (gpsloc == null) {
                Toast.makeText(applicationContext,"Please open the GPS", Toast.LENGTH_SHORT).show()
            }
            else {
                val gpslng = LatLng(gpsloc!!.latitude,gpsloc.longitude)

                val client = OkHttpClient().newBuilder().build()
                val request = Request.Builder().url(getDirectionURL(gpslng, destStr)).build()
                runBlocking {
                    val job = CoroutineScope(Dispatchers.IO).launch {
                        val response = client.newCall(request).execute()
                        response.body?.run {
                            val data = string()
                            val respObj = Gson().fromJson(data, GoogleMapDTO::class.java)
                            val status = respObj.status
                            Log.i("sssss",status)
                            if (status != "OK"){
                                flag = false
                            }
                            else {
                                val path = ArrayList<LatLng>()

                                val originloc = LatLng(
                                    respObj.routes[0].legs[0].start_location.lat.toDouble(),
                                    respObj.routes[0].legs[0].start_location.lng.toDouble()
                                )
                                val destloc = LatLng(
                                    respObj.routes[0].legs[0].end_location.lat.toDouble(),
                                    respObj.routes[0].legs[0].end_location.lng.toDouble()
                                )

                                joint.add(originloc)
                                for (i in 0 until (respObj.routes[0].legs[0].steps.size)) {

                                    joint.add(LatLng(
                                        respObj.routes[0].legs[0].steps[i].end_location.lat.toDouble(),
                                        respObj.routes[0].legs[0].steps[i].end_location.lng.toDouble()
                                    ))
                                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                                }
                                result.add(path)

                                joint.add(destloc)

                                resultString =
                                    etUsername.text.toString() + "," + angle(path[0], path[1])
                                Log.i("result", resultString)
                            }
                        }
                    }
                    job.join()

                    val job2 = launch{
                        // create a thread to send string
                        if(btDevice!=null){
                            try {
                                clientThread = ClientThread(btDevice)
                                clientThread.start()
                            }catch (e : Exception){
                                Log.d("exp",e.toString())
                            }
                        }
                    }
                    job2.join()
                }

                if(btDevice == null) Toast.makeText(this,"There is not a bounded device.",Toast.LENGTH_SHORT).show()
                if(!flag) Toast.makeText(this,"未搜尋到目的地",Toast.LENGTH_SHORT).show()
                else {
                    val intent2 = Intent(this, MapsActivity::class.java)
                    intent2.putExtra("path", joint)
                    intent2.putExtra("result", result)
                    startActivity(intent2)
                }
            }


        }
        ibtnSetServGPS.setOnClickListener {
            val intentx = Intent(this,MapsActivityGPS::class.java)
            startActivity(intentx)
        }
        /*
        btnRunAsServ.setOnClickListener{
            btnSend.isEnabled = false
            btnRunAsServ.isEnabled = false
            btnCloseServ.isEnabled = true

            serverThread = ServerThread()
            serverThread.start()
        }
        btnCloseServ.setOnClickListener {
            btnSend.isEnabled = true
            btnRunAsServ.isEnabled = true
            btnCloseServ.isEnabled = false

            closeServerThread()
        }
        btnSend.setOnClickListener {
            SERVER_NAME = etServName.text.toString()

            btDeviceSet = btAdapter.bondedDevices
            btDeviceSet.forEach {
                Log.d("dev",it.name)
            }
            var btDevice = btDeviceSet.find{it.name == SERVER_NAME}

            if(btDevice!=null){
                try {
                    clientThread = ClientThread(btDevice)
                    clientThread.start()
                }catch (e : Exception){
                    Log.d("exp",e.toString())
                }
            }
            else{
                Toast.makeText(this,"There is not a bounded device.",Toast.LENGTH_SHORT).show()
            }
        }
         */
    }

    var mainHandler = Handler(Looper.myLooper()!!,Handler.Callback {
        when(it.what){
            STATEE.LISTENING.ordinal        -> {}//tvStat2.setText("STATUS:"+"Listening...")
            STATEE.CONNECTING.ordinal       -> {}//tvStat2.setText("STATUS:"+"Connecting...")
            STATEE.CONNECTED.ordinal        -> {}//tvStat2.setText("STATUS:"+"Connected")
            STATEE.CONNECTION_FAIL.ordinal  -> {}//tvStat2.setText("STATUS:"+"Connection Failed")
            STATEE.STREAM_DONE.ordinal      ->{
                //var s = etMsg.text.toString()
                sendReceive?.write(resultString.encodeToByteArray())
//                sendReceive?.write(resultString)

                Thread{
                    Thread.sleep(delayTime)
                    try{
                        clientThread.cancel()
                    }catch (e:Exception){
                        Log.d("exp",e.toString())
                    }
                }.start()
            }
            STATEE.MSG_RECV.ordinal         -> {
                var readbuf : ByteArray = it.obj as ByteArray
                var tempMsg = readbuf.decodeToString()//String(readbuf,0,it.arg1)
                val s = tempMsg.split(",")

                /* deal with the incoming data "tempMsg"*/
                try {
                    Log.d("ssss","${s[0]}->${s[1]}")
                    deamonQ.add(
                        UserInfo(
                            s[0],s[1].toFloat()
                        )
                    )
                }catch (e : Exception){
                    e.printStackTrace()
                }
                /* deal with the incoming data "d"*/

                Thread{
                    // after server recv data
                    // we will close serverThread
                    Thread.sleep(delayTime)
                    try{
                        serverThread.cancel()
                    }catch (e:Exception){
                        Log.d("exp",e.toString())
                    }
                    Thread.sleep(delayTime)
                    serverThread = ServerThread()
                    serverThread.start()
                }.start()
            }
            STATEE.UPDATE_DIRECTION_INFO.ordinal ->{
                if(terminalType == getString(R.string.TYPE_SERVER)){
                    var anim = AnimationUtils.loadAnimation(this,R.anim.slide_up)
                    llllServerSide.startAnimation(anim)

                    // dequeue
                    var q = try{
                        deamonQ.remove()
                    }
                    catch (e : Exception){
                        e.printStackTrace()
                        null
                    }

                    // prevent get the null object
                    if(q != null){
                        tvUsernameField.text = q.name
                        ivArrowField.rotation = q.rotDeg
                        tvStat2.text = "Wating in the Queue:${deamonQ.size}"
                    }
                    else{
                        tvUsernameField.text = "N/A"
                        ivArrowField.rotation = 0.0f
                        tvStat2.text = "Wating in the Queue:0"
                    }

                }
            }
        }
        true
    })

    data class UserInfo(
        val name : String = "",
        val rotDeg : Float = 0.0f
    )
    private inner class DeamonQueueInspector : Thread() {
        private var isRunning = true

        override fun run() {
            super.run()
            while(isRunning){
                if(deamonQ.size > 0){
                    mainHandler.sendMessage(
                        Message().also {
                            it.what = STATEE.UPDATE_DIRECTION_INFO.ordinal
                        }
                    )
                    Thread.sleep(direction_info_dealyTime)
                }
                else{
                    Thread.sleep(1000)
                }
            }
        }

        fun cancel(){
            isRunning = false
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ServerThread() : Thread(){
        //AcceptThread

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            btAdapter?.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID)
        }

        override fun run(){
            var socket: BluetoothSocket? = null
            // Keep listening until exception occurs or a socket is returned.
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                socket = try {
                    mainHandler.sendMessage(
                        Message().also{it.what = MainActivity.STATEE.CONNECTING.ordinal}
                    )

                    mmServerSocket!!.accept()
                } catch (e: IOException) {
                    Log.e("info", "Socket's accept() method failed", e)
                    mainHandler.sendMessage(
                        Message().also{it.what = STATEE.CONNECTION_FAIL.ordinal}
                    )
                    break
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    //manageMyConnectedSocket(socket)
                    mainHandler.sendMessage(
                        Message().also{it.what = STATEE.CONNECTED.ordinal}
                    )

                    //recv
                    Log.d("info","communication here")
                    sendReceive = SendReceive(socket)
                    sendReceive?.start()

                    //break
                    //tricky here for server side
                }
            }

        }

        fun cancel() {
            try {
                sendReceive?.cancel()
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e("info", "Could not close the connect socket", e)
            }
        }

    }

    @SuppressLint("MissingPermission")
    private inner class ClientThread(d : BluetoothDevice) : Thread(){
        private var device : BluetoothDevice = d
        private val socket : BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        override fun run(){
            btAdapter.cancelDiscovery()

            try{
                socket?.connect()
                mainHandler.sendMessage(
                    Message().also{it.what = STATEE.CONNECTING.ordinal}
                )

                sendReceive = SendReceive(socket)
                sendReceive?.start()

                mainHandler.sendMessage(
                    Message().also{it.what = STATEE.STREAM_DONE.ordinal}
                )
            }catch (e : IOException){
                e.printStackTrace()
                mainHandler.sendMessage(
                    Message().also{it.what = STATEE.CONNECTION_FAIL.ordinal}
                )
            }
        }

        fun cancel() {
            try {
                sendReceive?.cancel()
                socket?.close()
            } catch (e: IOException) {
                Log.e("info", "Could not close the client socket", e)
            }
        }

    }

    private inner class SendReceive(s : BluetoothSocket?) : Thread(){
        private val socket = s
        private val input : InputStream? = s?.inputStream
        private val output : OutputStream? = s?.outputStream
//        private val reader : BufferedReader? = BufferedReader(InputStreamReader(s?.inputStream,"UTF-8"))
//        private val writer : PrintWriter? = PrintWriter(s?.outputStream)
        private var isRunning = true

        override fun run(){
            while(isRunning){
                try {
                    //var buf = reader!!.readLine().toCharArray()
                    var buf = ByteArray(1024)
                    var bytes =input!!.read(buf)

                    mainHandler
                        .obtainMessage(STATEE.MSG_RECV.ordinal,bytes,-1,buf)
//                        .obtainMessage(STATEE.MSG_RECV.ordinal,buf.size,-1,buf)
                        .sendToTarget()
                }catch (e : IOException){
                    e.printStackTrace()
                    //isRunning=false
                }
                isRunning=false //important here for server side
            }
        }

//        public fun write( str : String){
        public fun write( bytes : ByteArray){
            try{
                output!!.write(bytes)
//                writer!!.print(str)
            }catch (e: IOException){
                e.printStackTrace()
            }
        }

        fun cancel() {
            try {
//                input?.close()
//                output?.close()
                isRunning = false
                socket?.close()
            } catch (e: IOException) {
                Log.e("info", "Could not close the connect socket", e)
            }
        }
    }

    private fun closeServerThread(){
        if(btnCloseServ.isEnabled && serverThread!=null && serverThread.isAlive==true){
            try {
                serverThread.cancel()
            }catch (e : Exception){
                Log.d("exp",e.toString())
            }
        }
    }

    private fun closeDeamonQueue(){
        if(terminalType == getString(R.string.TYPE_SERVER)){
            deamonQueueInspecter.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        closeServerThread()
        closeDeamonQueue()
    }


    private fun getDirectionURL(origin: LatLng, dest:String):String{
        return "https://maps.googleapis.com/maps/api/directions/json?language=zh-TW&origin=${origin.latitude},${origin.longitude}&destination=${dest}&key=AIzaSyBme10Ce7rnKYnAYQnWlHb-JJ-byyEcRq0&mode=walking"
    }

    // dot product
    private fun dot(vector:List<Double>,vector2:List<Double>) = vector[0]*vector2[0] + vector[1]*vector2[1]

    // calculate the length of vector
    private fun len(vector:List<Double>):Double = sqrt(vector[0]*vector[0] + vector[1]*vector[1])

    // get the vector of two coordinate
    private fun getVector(coordinate: LatLng, coordinate2: LatLng):List<Double>{
        val x = coordinate2.longitude - coordinate.longitude
        val y = coordinate2.latitude - coordinate.latitude
        return listOf(x,y)
    }

    // calculate the angle with given vector and north
    private fun angle(coordinate: LatLng, coordinate2: LatLng):Double{
        val vector = getVector(coordinate,coordinate2)
        val north = listOf(0.0,1.0)
        val angle = acos(dot(north,vector)/(1*len(vector))) * 180 / PI
        if (vector[0] < 0) return (360.0 - angle)
        return angle
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

    public fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }

        return poly
    }
}