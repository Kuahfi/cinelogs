package com.z0diac.tesapi.ui.auth

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.auth.FirebaseAuth
import com.z0diac.tesapi.Movie
import com.z0diac.tesapi.MovieResponse
import com.z0diac.tesapi.R
import com.z0diac.tesapi.RetrofitInstance
import com.z0diac.tesapi.ui.dashboard.DashboardActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var posterImageView: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private var movieList: List<Movie> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        posterImageView = findViewById(R.id.ivPoster)

        val emailField = findViewById<EditText>(R.id.etEmail)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerText = findViewById<TextView>(R.id.btnRegister)

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null && user.isEmailVerified) {
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Please verify your email first!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        fetchMovies()
    }

    private fun fetchMovies() {
        RetrofitInstance.api.getPopularMovies("043cbbcb77cb0cae18791c2111db5c75")
            .enqueue(object : Callback<MovieResponse> {
                override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                    if (response.isSuccessful) {
                        movieList = response.body()?.results ?: emptyList()
                        if (movieList.isNotEmpty()) {
                            startImageSlideshow()
                        }
                    }
                }

                override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Failed to load movies", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun startImageSlideshow() {
        handler.post(object : Runnable {
            override fun run() {
                if (movieList.isNotEmpty()) {
                    val randomMovie = movieList.random()
                    val imageUrl = "https://image.tmdb.org/t/p/w500${randomMovie.posterPath}"

                    // Pastikan posterImageView masih ada sebelum memuat gambar
                    if (posterImageView != null) {
                        Glide.with(this@LoginActivity)
                            .load(imageUrl)
                            .placeholder(posterImageView.drawable ?: ColorDrawable(0xFF000000.toInt())) // Pakai gambar lama, kalau null pakai hitam
                            .transition(DrawableTransitionOptions.withCrossFade(1500)) // Smooth fade in 1.5 detik
                            .into(posterImageView)
                    }
                }
                handler.postDelayed(this, 5000) // Ganti setiap 5 detik
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Hentikan semua handler agar tidak crash
    }
}
