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

    private var displayedMovies = mutableListOf<Movie1>()
    private var popularMovies = mutableListOf<Movie1>()
    private var currentPopularPage = 1
    private var isPopularLoading = false

    private var currentPage = 1  // Halaman pertama
    private var isLoading = false // Cegah double fetch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        ivProfile = binding.ivProfile

        ivProfile.setOnClickListener {
            try {
                val user = auth.currentUser
                if (user != null) {
                    if (user.isAnonymous) {
                        showRegisterDialog()
                    } else {
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    showRegisterDialog()
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error handling profile click", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }


        currentPage = 1
        currentPopularPage = 1
        displayedMovies.clear()
        popularMovies.clear()

        setupImageSlider()
        setupRecyclerView()
        fetchTrendingMovies() // This should now load page 1
        fetchPopularMovies() // Load first page of popular movies

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.logout()
                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@DashboardActivity, LoginActivity::class.java))
                    finish()
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

    private fun setupRecyclerView() {
        // Set up trending movies RecyclerView
        movieAdapter = MovieAdapter(displayedMovies) {
            fetchTrendingMovies()
        }

        binding.rvTrendingMovies.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = movieAdapter
            setHasFixedSize(true)
        }

        // Set up popular movies RecyclerView
        popularMoviesAdapter = MovieAdapter(popularMovies) {
            fetchPopularMovies()
        }

        binding.rvPopularMovies.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = popularMoviesAdapter
            setHasFixedSize(true)
        }
    }

    private fun fetchTrendingMovies() {
        if (isLoading) return
        isLoading = true
        binding.loadingOverlay.visibility = View.VISIBLE

        val apiKey = getString(R.string.tmdb_api_key)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getTopRatedMovies(apiKey, currentPage).execute()

                withContext(Dispatchers.Main) {
                    isLoading = false
                    binding.loadingOverlay.visibility = View.GONE

                    if (response.isSuccessful) {
                        response.body()?.let { movieResponse ->
                            val newMovies = movieResponse.results
                            if (newMovies.isNotEmpty()) {
                                movieAdapter.updateMovies(newMovies)

                                // Scroll to the first item (position 0) after loading data
                                // Only do this on the first page
                                if (currentPage == 1) {
                                    binding.rvTrendingMovies.scrollToPosition(0)
                                }

                                currentPage++
                                Log.d("DashboardActivity", "Loaded page $currentPage with ${newMovies.size} movies")
                            } else {
                                Toast.makeText(this@DashboardActivity, "Tidak ada film lagi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Log.e("DashboardActivity", "API error: ${response.code()} - ${response.message()}")
                        Toast.makeText(this@DashboardActivity, "Gagal memuat film: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    binding.loadingOverlay.visibility = View.GONE
                    Log.e("DashboardActivity", "Error fetching movies", e)
                    Toast.makeText(this@DashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    isPopularLoading = false
                    binding.loadingOverlay.visibility = View.GONE

                    if (response.isSuccessful) {
                        response.body()?.let { movieResponse ->
                            val newMovies = movieResponse.results

                            if (newMovies.isNotEmpty()) {
                                popularMoviesAdapter.updateMovies(newMovies)

                                // Scroll to the first item (position 0) after loading data
                                // Only do this on the first page
                                if (currentPopularPage == 1) {
                                    binding.rvPopularMovies.scrollToPosition(0)
                                }

                                currentPopularPage++
                                Log.d("DashboardActivity", "Popular Movies Page $currentPopularPage loaded: ${newMovies.size} movies")
                            } else {
                                Toast.makeText(this@DashboardActivity, "Tidak ada film populer lagi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Log.e("DashboardActivity", "Popular Movies API error: ${response.code()} - ${response.message()}")
                        Toast.makeText(this@DashboardActivity, "Gagal memuat film populer: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isPopularLoading = false
                    binding.loadingOverlay.visibility = View.GONE
                    Log.e("DashboardActivity", "Error fetching popular movies", e)
                    Toast.makeText(this@DashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupIndicatorDots(size: Int) {
        val dots = mutableListOf<ImageView>()
        binding.indicatorLayout.removeAllViews()

        for (i in 0 until size) {
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.indicator_inactive)
                val params = LinearLayout.LayoutParams(20, 20).apply {
                    setMargins(8, 0, 8, 0)
                }
                layoutParams = params
            }
            dots.add(dot)
            binding.indicatorLayout.addView(dot)
        }

        if (dots.isNotEmpty()) dots[0].setImageResource(R.drawable.indicator_active)
    }

    private fun updateIndicatorDots(position: Int) {
        for (i in 0 until binding.indicatorLayout.childCount) {
            val dot = binding.indicatorLayout.getChildAt(i) as ImageView
            dot.setImageResource(if (i == position) R.drawable.indicator_active else R.drawable.indicator_inactive)
        }
    }

    private fun showRegisterDialog() {
        try {
            val dialog = BottomSheetDialog(this)
            val view = LayoutInflater.from(this).inflate(R.layout.register_dialog, null)

            // Find views safely with null checks
            val etName = view.findViewById<EditText>(R.id.etRegisterName)
            val etEmail = view.findViewById<EditText>(R.id.etRegisterEmail)
            val etPassword = view.findViewById<EditText>(R.id.etRegisterPassword)
            val etConfirmPassword = view.findViewById<EditText>(R.id.etRegisterConfirmPassword)
            val btnSubmit = view.findViewById<Button>(R.id.btnSubmitRegister)

            // Check if any required view is null
            if (etEmail == null || etPassword == null || etConfirmPassword == null || btnSubmit == null) {
                Log.e("DashboardActivity", "One or more views not found in register_dialog.xml")
                Toast.makeText(this, "Error loading registration form", Toast.LENGTH_SHORT).show()
                return
            }

            btnSubmit.setOnClickListener {
                try {
                    val email = etEmail.text.toString().trim()
                    val password = etPassword.text.toString().trim()
                    val confirmPassword = etConfirmPassword.text.toString().trim()

                    if (email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if(password != confirmPassword){
                        Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            try {
                                if (task.isSuccessful) {
                                    val user = FirebaseAuth.getInstance().currentUser
                                    user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                                        try {
                                            if (emailTask.isSuccessful) {
                                                Toast.makeText(this, "Verification email sent! Please login to continue.", Toast.LENGTH_LONG).show()
                                                dialog.dismiss()

                                                // Sign out the current user
                                                FirebaseAuth.getInstance().signOut()

                                                // Redirect to LoginActivity
                                                startActivity(Intent(this@DashboardActivity, LoginActivity::class.java))
                                                finish() // Close the current activity
                                            } else {
                                                Toast.makeText(this, "Failed to send verification email: ${emailTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("DashboardActivity", "Error in email verification", e)
                                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Log.e("DashboardActivity", "Error in registration completion", e)
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Error in registration button click", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.setContentView(view)
            dialog.show()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error showing register dialog", e)
            Toast.makeText(this, "Error showing registration dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}