package com.z0diac.tesapi.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.z0diac.tesapi.R
import com.z0diac.tesapi.ui.auth.LoginActivity
import androidx.lifecycle.lifecycleScope
import com.z0diac.tesapi.data.repository.user.UserRepository
import com.z0diac.tesapi.data.repository.user.ReviewRepository
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvEmail: TextView
    private lateinit var btnLogout: Button
    private lateinit var tvUsername: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        tvEmail = findViewById(R.id.tvEmail)
        tvUsername = findViewById(R.id.tvUsername)
        btnLogout = findViewById(R.id.btnLogout)

        val tvReviewsCount: TextView = findViewById(R.id.tvReviewsCount)
        val tvWatchlistCount: TextView = findViewById(R.id.tvWatchlistCount)
        val tvFavoritesCount: TextView = findViewById(R.id.tvFavoritesCount)

        val user = auth.currentUser
        tvEmail.text = user?.email ?: "No email (Anonymous)"

        val userRepository = UserRepository()
        val reviewRepository = ReviewRepository()

        user?.uid?.let { uid ->
            lifecycleScope.launch {
                try {
                    // Fetch username
                    val userData = userRepository.getUser(uid)
                    tvUsername.text = userData?.username ?: "No username"

                    // Fetch reviews count
                    val reviews = reviewRepository.getUserReviews(uid)
                    tvReviewsCount.text = reviews.size.toString()

                    // Fetch watchlist count
                    val watchlist = userRepository.getWatchlist(uid)
                    tvWatchlistCount.text = watchlist.size.toString()

                    // Fetch favorites count
                    val favorites = userRepository.getFavorites(uid)
                    tvFavoritesCount.text = favorites.size.toString()

                } catch (e: Exception) {
                    tvUsername.text = "Error loading data"
                    tvReviewsCount.text = "-"
                    tvWatchlistCount.text = "-"
                    tvFavoritesCount.text = "-"
                }
            }
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }



}
