package com.z0diac.tesapi.ui.dashboard

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.api.RetrofitInstance
import com.z0diac.tesapi.data.model.Movie1
import com.z0diac.tesapi.data.model.Review
import com.z0diac.tesapi.data.model.User
import com.z0diac.tesapi.data.model.UserMovieList
import com.z0diac.tesapi.data.repository.user.UserRepository
import com.z0diac.tesapi.ui.adapters.CastAdapter
import com.z0diac.tesapi.ui.adapters.ReviewAdapter
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MovieDetailsActivity : AppCompatActivity() {
    private lateinit var apiKey: String
    private lateinit var loadingContainer: FrameLayout
    private lateinit var contentScrollView: NestedScrollView
    private lateinit var blurOverlay: View
    private lateinit var btnFavorite: MaterialButton
    private lateinit var btnWatchlist: MaterialButton
    private lateinit var userRepository: UserRepository
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var rvReviews: RecyclerView
    private lateinit var db: FirebaseFirestore
    private var isInFavorites = false
    private var isInWatchlist = false
    private var currentUserId: String? = null
    private lateinit var currentMovie: Movie1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_details)
        apiKey = getString(R.string.tmdb_api_key)
        db = FirebaseFirestore.getInstance()

        userRepository = UserRepository()
        currentUserId = userRepository.getCurrentUserId()

        loadingContainer = findViewById(R.id.loadingContainer)
        contentScrollView = findViewById(R.id.contentScrollView)
        blurOverlay = findViewById(R.id.blurOverlay)
        btnFavorite = findViewById(R.id.btnFavorite)
        btnWatchlist = findViewById(R.id.btnWatchlist)
        showLoadingState(true)

        val btnBack: ImageButton = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val btnWriteReview: MaterialButton = findViewById(R.id.btnWriteReview)
        btnWriteReview.setOnClickListener {
            if (currentUserId == null) {
                Toast.makeText(this, "Please log in to write a review", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val existingReview = firestore.collection("movies")
                        .document(currentMovie.id.toString())
                        .collection("reviews")
                        .whereEqualTo("userId", currentUserId)
                        .get()
                        .await()

                    if (!existingReview.isEmpty) {
                        Toast.makeText(this@MovieDetailsActivity, "You have already reviewed this movie.", Toast.LENGTH_SHORT).show()
                    } else {
                        showReviewDialog()
                    }
                } catch (e: Exception) {
                    Log.e("MovieDetails", "Error checking existing review", e)
                    showReviewDialog()
                }
            }
        }

        rvReviews = findViewById(R.id.rvReviews)
        reviewAdapter = ReviewAdapter(emptyList(), isProfileView = false)
        rvReviews.layoutManager = LinearLayoutManager(this)
        rvReviews.adapter = reviewAdapter

        // ✅ Ambil data dari intent SECARA MANUAL
        val movieId = intent.getIntExtra("movie_id", 0)
        val movieTitle = intent.getStringExtra("movie_title") ?: ""
        val moviePoster = intent.getStringExtra("movie_poster") ?: ""
        val movieBackdrop = intent.getStringExtra("movie_backdrop")
        val movieReleaseDate = intent.getStringExtra("movie_release_date") ?: ""
        val movieRating = intent.getFloatExtra("movie_rating", 0f)
        val movieOverview = intent.getStringExtra("movie_overview") ?: ""
        Log.d("MovieDetails", "movieId=$movieId, moviePoster=$moviePoster")

        if (movieId == 0 || moviePoster.isEmpty()) {
            Toast.makeText(this, "Error: Movie data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val movie = Movie1(
            id = intent.getIntExtra("movie_id", 0),
            title = intent.getStringExtra("movie_title") ?: "",
            posterPath = intent.getStringExtra("movie_poster") ?: "",
            backdropPath = intent.getStringExtra("movie_backdrop"),
            releaseDate = intent.getStringExtra("movie_release_date") ?: "",
            rating = intent.getFloatExtra("movie_rating", 0f),
            overview = intent.getStringExtra("movie_overview") ?: "",
            genres = emptyList(), // Nggak dikirim, nanti diisi dari API
            runtime = 0,
            credits = null
        )

        currentMovie = movie
        fetchReviews(currentMovie.id)

        val ivBackdrop: ImageView = findViewById(R.id.ivMoviePoster)
        val tvTitle: TextView = findViewById(R.id.tvMovieTitle)
        val tvFormattedReleaseDate: TextView = findViewById(R.id.tvReleaseYear)
        val tvRating: TextView = findViewById(R.id.tvRating)
        val tvRuntime: TextView = findViewById(R.id.tvDuration)
        val tvSynopsis: TextView = findViewById(R.id.tvOverview)
        val btnWatchTrailer: FloatingActionButton = findViewById(R.id.fabPlayTrailer)
        val rvCast: RecyclerView = findViewById(R.id.rvCast)

        rvCast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        tvTitle.text = movie.title
        loadBlurredBackdrop(movie, ivBackdrop)

        fetchMovieDetails(
            movie, ivBackdrop, tvFormattedReleaseDate, tvRating,
            tvRuntime, tvSynopsis, btnWatchTrailer, rvCast
        )

        setupFavoriteAndWatchlistButtons(movie)
    }


    private fun setupFavoriteAndWatchlistButtons(movie: Movie1) {
        // Initialize buttons with default states
        updateFavoriteButtonUI(false)
        updateWatchlistButtonUI(false)

        // Check if user is logged in
        currentUserId?.let { userId ->
            // Fetch current states
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    isInFavorites = userRepository.isInFavorites(userId, movie.id)
                    isInWatchlist = userRepository.isInWatchlist(userId, movie.id)

                    withContext(Dispatchers.Main) {
                        updateFavoriteButtonUI(isInFavorites)
                        updateWatchlistButtonUI(isInWatchlist)

                        // Set up button click listeners
                        setupFavoriteButton(movie, userId)
                        setupWatchlistButton(movie, userId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } ?: run {
            // User not logged in - set up buttons to prompt login
            btnFavorite.setOnClickListener {
                Toast.makeText(this, "Please log in to add to favorites", Toast.LENGTH_SHORT).show()
                // Could add navigation to login screen here
            }

            btnWatchlist.setOnClickListener {
                Toast.makeText(this, "Please log in to add to watchlist", Toast.LENGTH_SHORT).show()
                // Could add navigation to login screen here
            }
        }
    }

    private fun setupFavoriteButton(movie: Movie1, userId: String) {
        btnFavorite.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (isInFavorites) {
                        // Remove from favorites
                        userRepository.removeFromFavorites(userId, movie.id)
                        isInFavorites = false
                    } else {
                        // Add to favorites
                        val userMovie = UserMovieList(
                            movieId = movie.id,
                            posterPath = movie.backdropPath,
                            title = movie.title
                        )
                        userRepository.addToFavorites(userId, userMovie)
                        isInFavorites = true
                    }

                    withContext(Dispatchers.Main) {
                        updateFavoriteButtonUI(isInFavorites)
                        val message = if (isInFavorites) "Added to favorites" else "Removed from favorites"
                        Toast.makeText(this@MovieDetailsActivity, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MovieDetailsActivity,
                            "Error updating favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupWatchlistButton(movie: Movie1, userId: String) {
        btnWatchlist.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (isInWatchlist) {
                        // Remove from watchlist
                        userRepository.removeFromWatchlist(userId, movie.id)
                        isInWatchlist = false
                    } else {
                        // Add to watchlist
                        val userMovie = UserMovieList(
                            movieId = movie.id,
                            posterPath = movie.posterPath,
                            backdropPath = movie.backdropPath,
                            title = movie.title
                        )
                        userRepository.addToWatchlist(userId, userMovie)
                        isInWatchlist = true
                    }

                    withContext(Dispatchers.Main) {
                        updateWatchlistButtonUI(isInWatchlist)
                        val message = if (isInWatchlist) "Added to watchlist" else "Removed from watchlist"
                        Toast.makeText(this@MovieDetailsActivity, message, Toast.LENGTH_SHORT).show()

                        // ✅ Tambahkan ini
                        setResult(RESULT_OK)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MovieDetailsActivity,
                            "Error updating watchlist",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }


    private fun updateFavoriteButtonUI(isFavorite: Boolean) {
        // Update icon for MaterialButton
        val iconRes = if (isFavorite) R.drawable.indicator_active else R.drawable.indicator_inactive
        btnFavorite.setIconResource(iconRes)

        // Optionally update button text
        btnFavorite.text = if (isFavorite) "In\nFavorites" else "Add to Favorites"
    }

    private fun updateWatchlistButtonUI(isInWatchlist: Boolean) {
        // Update icon for MaterialButton
        val iconRes = if (isInWatchlist) R.drawable.indicator_active else R.drawable.indicator_inactive
        btnWatchlist.setIconResource(iconRes)

        // Optionally update button text
        btnWatchlist.text = if (isInWatchlist) "In\nWatchlist" else "Add to Watchlist"
    }

    private fun loadBlurredBackdrop(movie: Movie1, ivBackdrop: ImageView) {
        val backdropUrl = movie.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }

        if (backdropUrl != null) {
            // Load blurred version during loading state
            Glide.with(this)
                .load(backdropUrl)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
                .placeholder(R.drawable.placeholder_image)
                .into(ivBackdrop)
        } else {
            // Use placeholder if no backdrop available
            ivBackdrop.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun fetchMovieDetails(
        movie: Movie1,
        ivBackdrop: ImageView,
        tvFormattedReleaseDate: TextView,
        tvRating: TextView,
        tvRuntime: TextView,
        tvSynopsis: TextView,
        btnWatchTrailer: FloatingActionButton,
        rvCast: RecyclerView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get movie details
                val response = RetrofitInstance.api.getMovieDetails(movie.id, apiKey).execute()
                val movieDetails = response.body()

                // Get trailer
                val videoResponse = RetrofitInstance.api.getMovieVideos(movie.id, apiKey)
                val trailer = videoResponse.results.firstOrNull { video -> video.type == "Trailer" && video.site == "YouTube" }
                val trailerUrl = trailer?.let { "https://www.youtube.com/watch?v=${it.key}" }

                // Get cast information
                val creditsResponse = RetrofitInstance.api.getMovieCredits(movie.id, apiKey).execute()
                val credits = creditsResponse.body()

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    // Load the full quality backdrop image
                    loadFullBackdrop(movie, ivBackdrop)

                    // Set movie details
                    val releaseYear = movieDetails?.releaseDate?.split("-")?.firstOrNull() ?: "N/A"
                    tvFormattedReleaseDate.text = releaseYear

                    val ratingValue = movieDetails?.rating ?: 0.0f
                    val formattedRating = String.format("%.1f", ratingValue)
                    tvRating.text = getString(R.string.rating, formattedRating)

                    // Set rating bar
                    val ratingBar: RatingBar? = findViewById(R.id.ratingBar)
                    ratingBar?.rating = ratingValue / 2  // Convert from scale 10 to scale 5

                    val castList = credits?.cast?.take(10) ?: emptyList()

                    tvRuntime.text = getString(R.string.runtime_placeholder, movieDetails?.runtime ?: 0)

                    val tvDirector: TextView? = findViewById(R.id.tvDirector)
                    val director = credits?.crew?.find { it.job == "Director" }?.name ?: "N/A"
                    tvDirector?.text = getString(R.string.director_placeholder, director)

                    // Synopsis with expand/collapse
                    var isExpanded = false
                    tvSynopsis.text = movieDetails?.overview ?: "No synopsis available"
                    tvSynopsis.maxLines = 4
                    tvSynopsis.ellipsize = TextUtils.TruncateAt.END

                    tvSynopsis.setOnClickListener {
                        isExpanded = !isExpanded
                        tvSynopsis.maxLines = if (isExpanded) Integer.MAX_VALUE else 4
                        tvSynopsis.ellipsize = if (isExpanded) null else TextUtils.TruncateAt.END
                    }

                    // Set cast adapter
                    if (castList.isNotEmpty()) {
                        rvCast.adapter = CastAdapter(castList)
                    }

                    // Set trailer button
                    if (trailerUrl != null) {
                        btnWatchTrailer.visibility = View.VISIBLE
                        btnWatchTrailer.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.setPackage("com.google.android.youtube") // Try to open in YouTube app
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                // If YouTube isn't available, open in browser
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl)))
                            }
                        }
                    } else {
                        btnWatchTrailer.visibility = View.GONE
                    }

                    // Hide loading state and show content
                    showLoadingState(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    // Show error state or fallback
                    showError()
                }
            }
        }
    }

    private fun loadFullBackdrop(movie: Movie1, ivBackdrop: ImageView) {
        val backdropUrl = movie.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }

        if (backdropUrl != null) {
            // Load full quality image
            Glide.with(this)
                .load(backdropUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Keep using the blurred version on failure
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hide blur overlay when full image is loaded
                        blurOverlay.visibility = View.GONE
                        return false
                    }
                })
                .into(ivBackdrop)
        }
    }

    private fun showReviewDialog() {
        val dialogView = layoutInflater.inflate(R.layout.review_dialog, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent closing when clicking outside
            .create()

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBarReview)
        val tvRatingDescription = dialogView.findViewById<TextView>(R.id.tvRatingDescription)
        val etReviewContent = dialogView.findViewById<EditText>(R.id.etReviewContent)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<MaterialButton>(R.id.btnSubmitReview)

        // Rating bar listener to change rating description text
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            tvRatingDescription.text = when {
                rating <= 1 -> "Very Bad"
                rating <= 2 -> "Bad"
                rating <= 3 -> "Okay"
                rating <= 4 -> "Good"
                else -> "Excellent"
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            val reviewText = etReviewContent.text.toString().trim()
            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentUserId = currentUser?.uid ?: return@setOnClickListener

            if (rating == 0f) {
                Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (reviewText.isEmpty()) {
                etReviewContent.error = "Please write a review"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val userRepo = UserRepository()
                    val user = userRepo.getUser(currentUserId) ?: User(currentUserId, "Anonymous")

                    // Cek apakah user sudah pernah review film ini
                    // Check if user has already reviewed this movie
                    val firestore = FirebaseFirestore.getInstance()
                    val existingReview = firestore.collection("movies")
                        .document(currentMovie.id.toString())
                        .collection("reviews")
                        .whereEqualTo("userId", currentUserId)
                        .get()
                        .await()

                    if (!existingReview.isEmpty) {
                        Toast.makeText(this@MovieDetailsActivity, "You have already reviewed this movie.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val review = Review(
                        id = "",
                        userId = currentUserId,
                        username = user.username,
                        movieId = currentMovie.id,
                        movieTitle = currentMovie.title,
                        rating = rating,
                        reviewText = reviewText,
                        timestamp = System.currentTimeMillis(),
                        posterPath = currentMovie.posterPath
                    )

                    val reviewId = userRepo.addReview(review)

                    Toast.makeText(this@MovieDetailsActivity, "Review submitted!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    fetchReviews(currentMovie.id)
                } catch (e: Exception) {
                    Log.e("MovieDetails", "Error submitting review", e)
                    Toast.makeText(this@MovieDetailsActivity, "Failed to submit review", Toast.LENGTH_SHORT).show()
                }
            }
        }




        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun fetchReviews(movieId: Int) {
        Log.d("MovieDetails", "Fetching reviews for movie ID: $movieId")

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("movies")
            .document(movieId.toString())
            .collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("MovieDetails", "Got ${documents.size()} reviews")

                val reviews = mutableListOf<Review>()
                for (doc in documents) {
                    val data = doc.data
                    val review = Review(
                        id = doc.id,
                        userId = data["userId"] as? String ?: "",
                        username = data["username"] as? String ?: "Unknown",
                        movieId = (data["movieId"] as? Number)?.toInt() ?: 0,
                        movieTitle = data["movieTitle"] as? String ?: "",
                        rating = (data["rating"] as? Number)?.toFloat() ?: 0f,
                        reviewText = data["reviewText"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                    )
                    reviews.add(review)
                }

                reviewAdapter.updateReviews(reviews)
            }
            .addOnFailureListener { e ->
                Log.e("MovieDetails", "Failed to load reviews: ${e.message}", e)
            }
    }



    private fun showLoadingState(isLoading: Boolean) {
        if (isLoading) {
            loadingContainer.visibility = View.VISIBLE
            contentScrollView.visibility = View.GONE
            blurOverlay.visibility = View.VISIBLE
        } else {
            loadingContainer.visibility = View.GONE
            contentScrollView.visibility = View.VISIBLE
        }
    }

    private fun showError() {
        // Show some error UI
        loadingContainer.visibility = View.GONE
        contentScrollView.visibility = View.VISIBLE
        Toast.makeText(this, "Error loading movie details", Toast.LENGTH_SHORT).show()
    }
}