package com.z0diac.tesapi.ui.dashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.api.RetrofitInstance
import com.z0diac.tesapi.data.model.MovieResponse
import com.z0diac.tesapi.databinding.ActivityDashboardBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import androidx.activity.viewModels
import android.content.Intent
import android.widget.ImageView
import android.widget.LinearLayout
import com.z0diac.tesapi.viewmodel.AuthViewModel
import com.z0diac.tesapi.ui.auth.LoginActivity

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var sliderAdapter: ImageSliderAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupImageSlider()
        setupRecyclerView()
        fetchTrendingMovies()

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupImageSlider() {
        val sliderImages = listOf(
            R.drawable.deadpool,  // Ganti dengan gambar di drawable
            R.drawable.oppenheimer,
            R.drawable.croods,
            R.drawable.dune,
            R.drawable.joker
        )

        sliderAdapter = ImageSliderAdapter(sliderImages)
        binding.viewPagerSlider.adapter = sliderAdapter

        setupIndicatorDots(sliderImages.size)

        // Auto-slide tiap 3 detik
        val sliderRunnable = object : Runnable {
            override fun run() {
                val nextItem = (binding.viewPagerSlider.currentItem + 1) % sliderImages.size
                binding.viewPagerSlider.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 3000)
            }
        }

        handler.postDelayed(sliderRunnable, 3000)

        binding.viewPagerSlider.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicatorDots(position)
            }
        })
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter(listOf())
        binding.rvTrendingMovies.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = movieAdapter
        }
    }

    private fun fetchTrendingMovies() {
        val apiKey = getString(R.string.tmdb_api_key)

        RetrofitInstance.api.getPopularMovies(apiKey).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { movieResponse ->
                        movieAdapter.updateMovies(movieResponse.results)
                    }
                } else {
                    Toast.makeText(this@DashboardActivity, "Failed to load movies", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                Log.e("DashboardActivity", "Error fetching movies", t)
                Toast.makeText(this@DashboardActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupIndicatorDots(size: Int) {
        val dots = mutableListOf<ImageView>()
        binding.indicatorLayout.removeAllViews()

        for (i in 0 until size) {
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.indicator_inactive) // Dot default
                val params = LinearLayout.LayoutParams(20, 20).apply {
                    setMargins(8, 0, 8, 0)
                }
                layoutParams = params
            }
            dots.add(dot)
            binding.indicatorLayout.addView(dot)
        }

        // Aktifkan dot pertama
        if (dots.isNotEmpty()) dots[0].setImageResource(R.drawable.indicator_active)
    }

    private fun updateIndicatorDots(position: Int) {
        for (i in 0 until binding.indicatorLayout.childCount) {
            val dot = binding.indicatorLayout.getChildAt(i) as ImageView
            dot.setImageResource(if (i == position) R.drawable.indicator_active else R.drawable.indicator_inactive)
        }
    }

}
