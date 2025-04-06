package com.z0diac.tesapi.ui.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.z0diac.tesapi.data.model.Movie1
import com.z0diac.tesapi.data.model.MovieResponse
import com.z0diac.tesapi.data.model.User
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.api.RetrofitInstance
import com.z0diac.tesapi.data.repository.user.UserRepository
import com.z0diac.tesapi.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var posterImageView: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private var movieList: List<Movie1> = emptyList()
    private val userRepository = UserRepository()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        posterImageView = findViewById(R.id.ivLoginPoster)

        // Check if user is already logged in and verified
        if (auth.currentUser != null) {
            if (auth.currentUser!!.isEmailVerified || auth.currentUser!!.isAnonymous) {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
                return
            }
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

        forgotPasswordText.setOnClickListener {
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
                            // Use repository to get user info
                            lifecycleScope.launch {
                                try {
                                    val userData = userRepository.getUser(user.uid)
                                    if (userData != null) {
                                        Toast.makeText(this@LoginActivity,
                                            "Welcome, ${userData.username}!",
                                            Toast.LENGTH_SHORT).show()
                                    }
                                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                                    finish()
                                } catch (e: Exception) {
                                    Toast.makeText(this@LoginActivity,
                                        "Failed to fetch user data: ${e.message}",
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
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

        auth.signInAnonymously().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val newUid = user?.uid

                // Save the anonymous UID
                if (newUid != null) {
                    sharedPref.edit().putString("anonymous_uid", newUid).apply()

                    // Create anonymous user in database with a guest username
                    lifecycleScope.launch {
                        try {
                            val guestName = "Guest-${UUID.randomUUID().toString().substring(0, 6)}"
                            val anonymousUser = User(
                                uid = newUid,
                                username = guestName,
                                email = ""
                            )
                            userRepository.saveUser(anonymousUser)
                            Toast.makeText(this@LoginActivity,
                                "Welcome, $guestName!",
                                Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            // Non-critical error, just log it
                            e.printStackTrace()
                        }
                    }
                }

                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Authentication Failed: ${task.exception?.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRegisterDialog() {
        val dialog = BottomSheetDialog(this)
        val view: View = LayoutInflater.from(this).inflate(R.layout.register_dialog, null)

        val etName: EditText = view.findViewById(R.id.etRegisterName)
        val etEmail: EditText = view.findViewById(R.id.etRegisterEmail)
        val etPassword: EditText = view.findViewById(R.id.etRegisterPassword)
        val etConfirmPassword: EditText = view.findViewById(R.id.etRegisterConfirmPassword)
        val btnSubmit: Button = view.findViewById(R.id.btnSubmitRegister)

        btnSubmit.setOnClickListener {
            val username = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false

            // First check if username is already taken
            lifecycleScope.launch {
                try {
                    val querySnapshot = auth.currentUser?.uid?.let {
                        userRepository.getUser(it)
                    }

                    // Create the user after validation
                    createUserAccount(username, email, password, dialog)

                } catch (e: Exception) {
                    btnSubmit.isEnabled = true
                    Toast.makeText(this@LoginActivity,
                        "Error checking username: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun createUserAccount(
        username: String,
        email: String,
        password: String,
        dialog: BottomSheetDialog
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            // Create user in Firestore with our repository
                            lifecycleScope.launch {
                                try {
                                    val newUser = User(
                                        uid = user.uid,
                                        username = username,
                                        email = email
                                    )
                                    userRepository.saveUser(newUser)

                                    dialog.dismiss()

                                    Toast.makeText(this@LoginActivity,
                                        "Account created! Please verify your email.",
                                        Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this@LoginActivity,
                                        "Failed to save user data: ${e.message}",
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this@LoginActivity,
                                "Failed to send verification email.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@LoginActivity,
                        "Registration Failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
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

            btnSend.isEnabled = false

            FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val signInMethods = task.result?.signInMethods
                        if (signInMethods.isNullOrEmpty()) {
                            btnSend.isEnabled = true
                            Toast.makeText(this, "Email is not registered!", Toast.LENGTH_LONG).show()
                        } else {
                            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                .addOnCompleteListener { resetTask ->
                                    btnSend.isEnabled = true
                                    if (resetTask.isSuccessful) {
                                        Toast.makeText(this, "Reset link sent to $email",
                                            Toast.LENGTH_LONG).show()
                                        dialog.dismiss()
                                    } else {
                                        Toast.makeText(this, "Error: ${resetTask.exception?.message}",
                                            Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    } else {
                        btnSend.isEnabled = true
                        Toast.makeText(this, "Error: ${task.exception?.message}",
                            Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this@LoginActivity, "Failed to load movies",
                        Toast.LENGTH_SHORT).show()
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