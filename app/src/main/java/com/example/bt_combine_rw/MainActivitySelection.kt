package com.example.bt_combine_rw

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bt_combine_rw.MainActivity
import com.example.bt_combine_rw.R
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main_selection.*

class MainActivitySelection : AppCompatActivity() {
    private val TAG : String = "debug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_selection)
        supportActionBar?.hide()

        btnSelectServ.setOnClickListener{
            //server need to show Arrow to point the direction
            var intentx = Intent(this, MainActivity::class.java)
            intentx.putExtra("type",getString(R.string.TYPE_SERVER))
            startActivity(intentx)
        }
        btnSelectCli.setOnClickListener {
            /*
            var intentx = Intent(this,MainActivity::class.java)
            intentx.putExtra("type",getString(R.string.TYPE_CLIENT))
            startActivity(intentx)
             */
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    0
                )
            }
            else {
                val locationRequest = LocationRequest.create().apply {
                    interval = 10000
                    fastestInterval = 5000
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }
// check settings
                val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
                val client : SettingsClient = LocationServices.getSettingsClient(this)
                val checkTask : Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
                checkTask.addOnSuccessListener {
                    Log.d(TAG, "checkLocationSettings is Successful: " + checkTask.isSuccessful)
                    Log.d(TAG, "isLocationUsable: " + it.locationSettingsStates!!.isLocationUsable)
                    permission()
                }
                checkTask.addOnFailureListener{ exception ->
                    Log.d(TAG, "checkLocationSettings's exception: " + exception.message)
                    if (exception is ResolvableApiException) {
                        try {
                            // require user to change the settings.
                            val REQUEST_CHECK_SETTINGS = 0x1
                            exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                        } catch (sendEx: IntentSender.SendIntentException) {
                        }
                    }
                }
            }

//            var intentx = Intent(this,MainActivityClient::class.java)
//            startActivity(intentx)
        }
    }
    private fun permission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        } else startIntent()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startIntent()
            } else {
                Toast.makeText(this, "需要定位權限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startIntent() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}