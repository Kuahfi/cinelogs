package com.z0diac.tesapi.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.api.RetrofitInstance
import com.z0diac.tesapi.data.model.Genre
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
        val tvReleaseDate: TextView = findViewById(R.id.tvReleaseDate)
        val tvGenre: TextView = findViewById(R.id.tvGenre)
        val tvRating: TextView = findViewById(R.id.tvRating)
        val tvRuntime: TextView = findViewById(R.id.tvRuntime)
        val tvDirector: TextView = findViewById(R.id.tvDirector)
        val tvSynopsis: TextView = findViewById(R.id.tvSynopsis)
        val btnWatchTrailer: TextView = findViewById(R.id.btnWatchTrailer)

        movie?.let {
            tvTitle.text = it.title

            Glide.with(this)
                .load("https://image.tmdb.org/t/p/w500" + it.posterPath)
                .placeholder(R.drawable.placeholder_image)
                .into(ivPoster)

            Glide.with(this)
                .load(movie.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" } ?: R.drawable.placeholder_image)
                .placeholder(R.drawable.placeholder_image)
                .into(ivBackdrop)

            // Ambil detail film dari TMDB
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitInstance.api.getMovieDetails(it.id, apiKey).execute()
                    val movieDetails = response.body()

                    val videoResponse = RetrofitInstance.api.getMovieVideos(it.id, apiKey)
                    val trailer = videoResponse.results.firstOrNull { video -> video.type == "Trailer" && video.site == "YouTube" }
                    val trailerUrl = trailer?.let { "https://www.youtube.com/watch?v=${it.key}" }

                    withContext(Dispatchers.Main) {
                        // Ambil hanya tahun dari releaseDate
                        val releaseYear = movieDetails?.releaseDate?.split("-")?.firstOrNull() ?: "N/A"
                        tvReleaseDate.text = " • $releaseYear"

                        tvGenre.text = getString(
                            R.string.genre,
                            movieDetails?.genres?.take(3)?.joinToString { genre -> genre.name } ?: "N/A"
                        )
                        tvRating.text = getString(R.string.rating, movieDetails?.rating ?: "N/A")
                        tvRuntime.text = getString(R.string.runtime_placeholder, movieDetails?.runtime ?: 0)

                        // Cari sutradara
                        val director = movieDetails?.credits?.crew?.find { it.job == "Director" }?.name ?: "N/A"
                        tvDirector.text = getString(R.string.director_placeholder, director)

                        // Sinopsis dengan expand/collapse
                        var isExpanded = false
                        tvSynopsis.text = movieDetails?.overview ?: "No synopsis available"
                        tvSynopsis.maxLines = 3
                        tvSynopsis.ellipsize = TextUtils.TruncateAt.END

                        tvSynopsis.setOnClickListener {
                            isExpanded = !isExpanded
                            tvSynopsis.maxLines = if (isExpanded) Integer.MAX_VALUE else 3
                            tvSynopsis.ellipsize = if (isExpanded) null else TextUtils.TruncateAt.END
                        }

                        if (trailerUrl != null) {
                            btnWatchTrailer.visibility = View.VISIBLE
                            btnWatchTrailer.setOnClickListener {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.setPackage("com.google.android.youtube") // Coba buka di YouTube app
                                try {
                                    it.context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Jika YouTube tidak ada, buka di browser
                                    it.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl)))
                                }
                            }
                        } else {
                            btnWatchTrailer.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}


