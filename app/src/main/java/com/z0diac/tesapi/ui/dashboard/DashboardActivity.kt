package com.z0diac.tesapi.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.api.RetrofitInstance
import com.z0diac.tesapi.data.model.MovieResponse
import com.z0diac.tesapi.databinding.ActivityDashboardBinding
import com.z0diac.tesapi.ui.auth.LoginActivity
import com.z0diac.tesapi.ui.dashboard.adapter.MovieAdapter
import com.z0diac.tesapi.viewmodel.AuthViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var movieAdapter: MovieAdapter
    private val viewModel: AuthViewModel by viewModels()  // Inisialisasi ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        fetchTrendingMovies()
        setupLogoutButton()
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

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
