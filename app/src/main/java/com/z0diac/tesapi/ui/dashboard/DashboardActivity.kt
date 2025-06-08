package com.z0diac.tesapi.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.api.RetrofitInstance
import com.z0diac.tesapi.data.model.Movie1
import com.z0diac.tesapi.databinding.ActivityDashboardBinding
import com.z0diac.tesapi.ui.adapters.ImageSliderAdapter
import com.z0diac.tesapi.ui.adapters.MovieAdapter
import com.z0diac.tesapi.ui.auth.LoginActivity
import com.z0diac.tesapi.ui.profile.ProfileActivity
import com.z0diac.tesapi.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {
    private lateinit var ivProfile: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var popularMoviesAdapter: MovieAdapter
    private lateinit var sliderAdapter: ImageSliderAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val viewModel: AuthViewModel by viewModels()

    // These master lists hold the state and prevent data loss.
    private var trendingMoviesList = mutableListOf<Movie1>()
    private var popularMoviesList = mutableListOf<Movie1>()

    private var currentTrendingPage = 1
    private var isTrendingLoading = false
    private var currentPopularPage = 1
    private var isPopularLoading = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        ivProfile = binding.ivProfile

        setupProfileClickListener()
        setupRecyclerView()
        setupImageSlider()
        setupSearchListener()

        // Initial data fetch
        fetchTrendingMovies()
        fetchPopularMovies()
    }

    private fun setupSearchListener() {
        binding.etSearch.isFocusable = false
        binding.etSearch.isClickable = true

        binding.etSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupProfileClickListener() {
        ivProfile.setOnClickListener {
            val user = auth.currentUser
            if (user != null && !user.isAnonymous) {
                startActivity(Intent(this, ProfileActivity::class.java))
            } else {
                showRegisterDialog()
            }
        }
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter { fetchTrendingMovies() }
        binding.rvTrendingMovies.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = movieAdapter
        }

        popularMoviesAdapter = MovieAdapter { fetchPopularMovies() }
        binding.rvPopularMovies.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = popularMoviesAdapter
        }
    }

    private fun fetchTrendingMovies() {
        if (isTrendingLoading) return
        isTrendingLoading = true
        binding.loadingOverlay.visibility = View.VISIBLE

        val apiKey = getString(R.string.tmdb_api_key)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getTopRatedMovies(apiKey, currentTrendingPage).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val newMovies = response.body()?.results ?: emptyList()
                        if (currentTrendingPage == 1) {
                            trendingMoviesList.clear()
                            movieAdapter.setMovies(newMovies)
                        } else {
                            movieAdapter.addMovies(newMovies)
                        }
                        trendingMoviesList.addAll(newMovies)
                        currentTrendingPage++
                    } else {
                        Toast.makeText(this@DashboardActivity, "Failed to load movies: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isTrendingLoading = false
                    binding.loadingOverlay.visibility = View.GONE
                }
            }
        }
    }

    private fun fetchPopularMovies() {
        if (isPopularLoading) return
        isPopularLoading = true
        binding.loadingOverlay.visibility = View.VISIBLE

        val apiKey = getString(R.string.tmdb_api_key)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getPopularMovies(apiKey, currentPopularPage).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val newMovies = response.body()?.results ?: emptyList()
                        if (currentPopularPage == 1) {
                            popularMoviesList.clear()
                            popularMoviesAdapter.setMovies(newMovies)
                        } else {
                            popularMoviesAdapter.addMovies(newMovies)
                        }
                        popularMoviesList.addAll(newMovies)
                        currentPopularPage++
                    } else {
                        Toast.makeText(this@DashboardActivity, "Failed to load popular movies: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isPopularLoading = false
                    binding.loadingOverlay.visibility = View.GONE
                }
            }
        }
    }

    private fun setupImageSlider() {
        val sliderImages = listOf(
            R.drawable.deadpool,
            R.drawable.oppenheimer,
            R.drawable.croods,
            R.drawable.dune,
            R.drawable.joker
        )

        sliderAdapter = ImageSliderAdapter(sliderImages)
        binding.viewPagerSlider.adapter = sliderAdapter
        setupIndicatorDots(sliderImages.size)

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

    private fun setupIndicatorDots(size: Int) {
        binding.indicatorLayout.removeAllViews()
        for (i in 0 until size) {
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.indicator_inactive)
                layoutParams = LinearLayout.LayoutParams(20, 20).apply { setMargins(8, 0, 8, 0) }
            }
            binding.indicatorLayout.addView(dot)
        }
        if (size > 0) {
            updateIndicatorDots(0)
        }
    }

    private fun updateIndicatorDots(position: Int) {
        for (i in 0 until binding.indicatorLayout.childCount) {
            val dot = binding.indicatorLayout.getChildAt(i) as ImageView
            dot.setImageResource(if (i == position) R.drawable.indicator_active else R.drawable.indicator_inactive)
        }
    }

    private fun showRegisterDialog() {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.register_dialog, null)
            val dialog = BottomSheetDialog(this).apply { setContentView(dialogView) }

            val etEmail = dialogView.findViewById<EditText>(R.id.etRegisterEmail)
            val etPassword = dialogView.findViewById<EditText>(R.id.etRegisterPassword)
            val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etRegisterConfirmPassword)
            val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmitRegister)

            btnSubmit.setOnClickListener {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                val confirmPassword = etConfirmPassword.text.toString().trim()

                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (password != confirmPassword) {
                    Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                Toast.makeText(this, "Verification email sent! Please login.", Toast.LENGTH_LONG).show()
                                auth.signOut()
                                dialog.dismiss()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Failed to send verification email: ${emailTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            dialog.show()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error showing register dialog", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
