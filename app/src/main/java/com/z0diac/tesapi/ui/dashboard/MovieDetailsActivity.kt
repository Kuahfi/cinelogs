package com.z0diac.tesapi.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.api.RetrofitInstance
import com.z0diac.tesapi.data.model.Movie1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MovieDetailsActivity : AppCompatActivity() {
    private lateinit var apiKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_details)
        apiKey = getString(R.string.tmdb_api_key)

        val movie = intent.getParcelableExtra<Movie1>("MOVIE_DATA")
        val ivPoster: ImageView = findViewById(R.id.ivDetailPoster)
        val ivBackdrop: ImageView = findViewById(R.id.ivBackdrop)
        val tvTitle: TextView = findViewById(R.id.tvTitle)
        val tvHeaderTitle: TextView = findViewById(R.id.tvHeaderTitle) // Header title in the sticky container
        val tvFormattedReleaseDate: TextView = findViewById(R.id.tvFormattedReleaseDate) // New formatted release date TextView
        val tvGenre: TextView = findViewById(R.id.tvGenre)
        val tvRating: TextView = findViewById(R.id.tvRating)
        val tvRuntime: TextView = findViewById(R.id.tvRuntime)
        val tvDirector: TextView = findViewById(R.id.tvDirector)
        val tvSynopsis: TextView = findViewById(R.id.tvSynopsis)
        val btnWatchTrailer: TextView = findViewById(R.id.btnWatchTrailer)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val overlayView: View = findViewById(R.id.overlayView)
        val rvCast: RecyclerView = findViewById(R.id.rvCast)
        val backButton: ImageView = findViewById(R.id.btnBack)
        val nestedScrollView: NestedScrollView = findViewById(R.id.contentScrollView)
        val titleContainer: View = findViewById(R.id.titleContainer)

        // Initially make the title container background transparent, it will only become visible on scroll
        titleContainer.alpha = 0f

        backButton.setOnClickListener {
            onBackPressed()
        }

        // Setup RecyclerView with vertical orientation for cast
        rvCast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        movie?.let {
            tvTitle.text = it.title
            tvHeaderTitle.text = it.title // Set the title for the header too

            // Scroll listener to control header visibility
            nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                // Make the header visible after scrolling past a certain point
                val scrollThreshold = 150
                if (scrollY > scrollThreshold) {
                    // Fade in title container
                    val alpha = (scrollY - scrollThreshold).coerceAtMost(100) / 100f
                    titleContainer.alpha = alpha
                } else {
                    // Hide title container
                    titleContainer.alpha = 0f
                }
            })

            Glide.with(this)
                .load("https://image.tmdb.org/t/p/w500" + it.posterPath)
                .placeholder(R.drawable.placeholder_image)
                .into(ivPoster)

            Glide.with(this)
                .load(movie.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" } ?: R.drawable.placeholder_image)
                .placeholder(R.drawable.placeholder_image)
                .into(ivBackdrop)

            // Fetch movie details
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Get movie details
                    val response = RetrofitInstance.api.getMovieDetails(it.id, apiKey).execute()
                    val movieDetails = response.body()

                    // Get trailer
                    val videoResponse = RetrofitInstance.api.getMovieVideos(it.id, apiKey)
                    val trailer = videoResponse.results.firstOrNull { video -> video.type == "Trailer" && video.site == "YouTube" }
                    val trailerUrl = trailer?.let { "https://www.youtube.com/watch?v=${it.key}" }

                    // Get cast information
                    val creditsResponse = RetrofitInstance.api.getMovieCredits(it.id, apiKey).execute()
                    val credits = creditsResponse.body()
                    val castList = credits?.cast?.take(10) ?: emptyList()

                    // Update UI on main thread
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        overlayView.visibility = View.GONE

                        // Set movie details
                        val releaseYear = movieDetails?.releaseDate?.split("-")?.firstOrNull() ?: "N/A"

                        // Format the release date with bullet point
                        tvFormattedReleaseDate.text = "• $releaseYear"

                        // Also update the header title to include the year
                        tvHeaderTitle.text = it.title

                        tvGenre.text = getString(
                            R.string.genre,
                            movieDetails?.genres?.take(3)?.joinToString { genre -> genre.name } ?: "N/A"
                        )
                        val ratingValue = movieDetails?.rating ?: 0.0f
                        val formattedRating = String.format("%.1f", ratingValue)
                        tvRating.text = getString(R.string.rating, formattedRating)

                        tvRuntime.text = getString(R.string.runtime_placeholder, movieDetails?.runtime ?: 0)

                        // Find director
                        val director = movieDetails?.credits?.crew?.find { it.job == "Director" }?.name ?: "N/A"
                        tvDirector.text = getString(R.string.director_placeholder, director)

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
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}