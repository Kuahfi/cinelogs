package com.z0diac.tesapi.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.z0diac.tesapi.R
import com.z0diac.tesapi.ui.onboarding.OnboardingActivity

class splashScreen : AppCompatActivity() {

    private val SPLASH_TIME: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler().postDelayed({
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }, SPLASH_TIME)
    }
}
