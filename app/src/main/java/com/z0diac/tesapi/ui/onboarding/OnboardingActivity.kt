package com.z0diac.tesapi.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.z0diac.tesapi.R
import com.z0diac.tesapi.ui.auth.LoginActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var btnSkip: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek apakah user sudah pernah onboarding
//        if (isOnboardingCompleted()) {
//            navigateToLogin()
//            return
//        }

        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        val onboardingItems = listOf(
            OnboardingItem(R.drawable.onboarding1, "Welcome!", "This is the first page of onboarding."),
            OnboardingItem(R.drawable.onboarding2, "Explore", "Discover new features in our app."),
            OnboardingItem(R.drawable.onboarding3, "Get Started!", "Let's begin your journey with us.")
        )

        val adapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = adapter

        btnNext.setOnClickListener {
            if (viewPager.currentItem < onboardingItems.size - 1) {
                viewPager.currentItem += 1
            } else {
                markOnboardingCompleted()
                navigateToLogin()
            }
        }

        btnSkip.setOnClickListener {
            markOnboardingCompleted()
            navigateToLogin()
        }
    }

    private fun isOnboardingCompleted(): Boolean {
        val sharedPref = getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("onboarding_completed", false)
    }

    private fun markOnboardingCompleted() {
        val sharedPref = getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("onboarding_completed", true)
            apply()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
