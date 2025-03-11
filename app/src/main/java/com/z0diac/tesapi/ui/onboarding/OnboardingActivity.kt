package com.z0diac.tesapi.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.z0diac.tesapi.R
import com.z0diac.tesapi.ui.auth.LoginActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var btnSkip: Button
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var indicatorLayout: LinearLayout

    private val titles = arrayOf(
        "Welcome to Cinelogs",
        "Discover Amazing Features",
        "Start Your Journey Today!"
    )

    private val descriptions = arrayOf(
        "Your personal space to track, review, and relive your favorite movies.",
        "Explore and enjoy various features designed for you.",
        "Sign up now and experience the best of our application!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)
        tvTitle = findViewById(R.id.tvTitle)
        tvDescription = findViewById(R.id.tvDescription)
        indicatorLayout = findViewById(R.id.indicatorLayout)

        // Set adapter untuk ViewPager2
        viewPager.adapter = OnboardingAdapter(this)

        // Setup indikator titik
        setupIndicator()
        setCurrentIndicator(0)

        // Listener untuk mengganti teks & indikator saat geser halaman
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tvTitle.text = titles[position]
                tvDescription.text = descriptions[position]
                setCurrentIndicator(position)

                if (position == titles.size - 1) {
                    btnNext.text = "Get Started"
                } else {
                    btnNext.text = "Next"
                }
            }
        })

        // Tombol Skip -> Langsung ke Login
        btnSkip.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Tombol Next -> Pindah halaman atau ke Login jika di halaman terakhir
        btnNext.setOnClickListener {
            if (viewPager.currentItem < titles.size - 1) {
                viewPager.currentItem += 1
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun setupIndicator() {
        val indicators = Array(titles.size) { ImageView(this) }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(8, 0, 8, 0)

        for (i in indicators.indices) {
            indicators[i].layoutParams = params
            indicators[i].setImageResource(R.drawable.indicator_inactive) // Titik default (putih)
            indicatorLayout.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(position: Int) {
        for (i in 0 until indicatorLayout.childCount) {
            val imageView = indicatorLayout.getChildAt(i) as ImageView
            if (i == position) {
                imageView.setImageResource(R.drawable.indicator_active) // Titik aktif (hitam)
            } else {
                imageView.setImageResource(R.drawable.indicator_inactive) // Titik non-aktif (putih)
            }
        }
    }
}
