package com.z0diac.tesapi.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.repository.user.ReviewRepository
import com.z0diac.tesapi.data.repository.user.UserRepository
import com.z0diac.tesapi.ui.adapters.ProfileViewPagerAdapter
import com.z0diac.tesapi.ui.auth.LoginActivity
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvEmail: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvReviewsCount: TextView
    private lateinit var tvWatchlistCount: TextView
    private lateinit var tvFavoritesCount: TextView
    private lateinit var btnLogout: Button
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var ivProfilePicture: CircleImageView
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        // Init Views
        tvEmail = findViewById(R.id.tvEmail)
        tvUsername = findViewById(R.id.tvUsername)
        tvReviewsCount = findViewById(R.id.tvReviewsCount)
        tvWatchlistCount = findViewById(R.id.tvWatchlistCount)
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount)
        btnLogout = findViewById(R.id.btnLogout)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        ivProfilePicture = findViewById(R.id.ivProfilePicture) // ✅ Tambah inisialisasi

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val user = auth.currentUser
        tvEmail.text = user?.email ?: "No email (Anonymous)"

        user?.uid?.let { uid ->
            userId = uid
            setupViewPager(uid)
            refreshUserData()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        supportFragmentManager.setFragmentResultListener("review_updated", this) { _, _ ->
            refreshUserData()
        }

        supportFragmentManager.setFragmentResultListener("watchlist_updated", this) { _, _ ->
            refreshUserData()
        }

        supportFragmentManager.setFragmentResultListener("favorites_updated", this) { _, _ ->
            refreshUserData()
        }

        supportFragmentManager.setFragmentResultListener("refresh_tabs", this) { _, _ ->
            userId?.let {
                setupViewPager(it)
            }
        }
    }

    private fun setupViewPager(userId: String) {
        val adapter = ProfileViewPagerAdapter(this, userId)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Reviews"
                1 -> "Watchlist"
                2 -> "Favorites"
                else -> ""
            }
        }.attach()
    }

    private fun refreshUserData() {
        val user = auth.currentUser ?: return

        lifecycleScope.launch {
            try {
                val userRepository = UserRepository()
                val reviewRepository = ReviewRepository()

                val userData = userRepository.getUser(user.uid)
                tvUsername.text = userData?.username ?: "No username"

                // ✅ Load foto profil
                val defaultProfileUrl = "https://i.imgur.com/3J9KcDo.png"
                Glide.with(this@ProfileActivity)
                    .load(userData?.profilePictureUrl ?: defaultProfileUrl)
                    .placeholder(R.drawable.default_profile)
                    .into(ivProfilePicture)

                val reviews = reviewRepository.getUserReviews(user.uid)
                tvReviewsCount.text = reviews.size.toString()

                val watchlist = userRepository.getWatchlist(user.uid)
                tvWatchlistCount.text = watchlist.size.toString()

                val favorites = userRepository.getFavorites(user.uid)
                tvFavoritesCount.text = favorites.size.toString()
            } catch (e: Exception) {
                tvUsername.text = "Error loading data"
                tvReviewsCount.text = "-"
                tvWatchlistCount.text = "-"
                tvFavoritesCount.text = "-"
            }
        }
    }
}
