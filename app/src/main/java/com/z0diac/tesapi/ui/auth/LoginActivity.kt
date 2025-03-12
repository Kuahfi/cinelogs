package com.z0diac.tesapi.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.z0diac.tesapi.data.model.Movie
import com.z0diac.tesapi.data.model.MovieResponse
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.api.RetrofitInstance
import com.z0diac.tesapi.ui.dashboard.DashboardActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent

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

        if (auth.currentUser != null && auth.currentUser!!.isEmailVerified) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        val btnAnonymousLogin = findViewById<Button>(R.id.btnAnonymousLogin)
        btnAnonymousLogin.setOnClickListener {
            signInAnonymously()
        }

        val emailField = findViewById<EditText>(R.id.etEmail)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerText = findViewById<TextView>(R.id.btnRegister)
        val forgotPasswordText = findViewById<TextView>(R.id.btnForgotPassword)

        registerText.setOnClickListener {
            showRegisterDialog()
        }

        forgotPasswordText.setOnClickListener { // 🔥 Tambahkan ini
            showResetPasswordDialog()
        }

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

        fetchMovies()
    }

    private fun signInAnonymously() {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val savedUid = sharedPref.getString("anonymous_uid", null)

        if (savedUid != null) {
            Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
        }

        auth.signInAnonymously().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val newUid = user?.uid

                // Simpan UID baru
                sharedPref.edit().putString("anonymous_uid", newUid).apply()

                Toast.makeText(this, "Signed in as Anonymous", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRegisterDialog() {
        val dialog = BottomSheetDialog(this)
        val view: View = LayoutInflater.from(this).inflate(R.layout.register_dialog, null)

        val etName: EditText = view.findViewById(R.id.etRegisterName) //buat nanti aja
        val etEmail: EditText = view.findViewById(R.id.etRegisterEmail)
        val etPassword: EditText = view.findViewById(R.id.etRegisterPassword)
        val etConfirmPassword: EditText = view.findViewById(R.id.etRegisterConfirmPassword)
        val btnSubmit: Button = view.findViewById(R.id.btnSubmitRegister)

        btnSubmit.setOnClickListener {
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

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                Toast.makeText(this, "Verification email sent!", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showResetPasswordDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.reset_password_dialog, null)

        val etEmail = view.findViewById<EditText>(R.id.etResetEmail)
        val btnSend = view.findViewById<Button>(R.id.btnSendResetLink)

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 Cek apakah email terdaftar sebelum reset password
            FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val signInMethods = task.result?.signInMethods
                        if (signInMethods.isNullOrEmpty()) {
                            Toast.makeText(this, "Email is not registered!", Toast.LENGTH_LONG).show()
                        } else {
                            // 🔹 Email ditemukan, lanjutkan reset password
                            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                .addOnCompleteListener { resetTask ->
                                    if (resetTask.isSuccessful) {
                                        Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                                        dialog.dismiss()
                                    } else {
                                        Toast.makeText(this, "Error: ${resetTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        dialog.setContentView(view)
        dialog.show()
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

                    if (posterImageView != null) {
                        Glide.with(this@LoginActivity)
                            .load(imageUrl)
                            .placeholder(R.drawable.img_1)
                            .into(posterImageView)
                    }
                }
                handler.postDelayed(this, 10000)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}