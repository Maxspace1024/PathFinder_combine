package com.example.bt_combine_rw

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class MainActivitySplash : AppCompatActivity() {
    private val DELAY_TIME : Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_splash)

        supportActionBar?.hide()

        Handler().postDelayed({
            val intent = Intent(this@MainActivitySplash,MainActivitySelection::class.java)
            startActivity(intent)
            finish()
        },DELAY_TIME)
    }
}