package com.z0diac.tesapi.ui.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.model.Movie1
import com.z0diac.tesapi.ui.dashboard.MovieDetailsActivity
import java.text.SimpleDateFormat
import java.util.*

class SearchAdapter(private var movies: List<Movie1>) :
    RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    fun updateData(newMovies: List<Movie1>) {
        this.movies = newMovies
        notifyDataSetChanged()
    }

    inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView = itemView.findViewById(R.id.ivPoster)
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val releaseYear: TextView = itemView.findViewById(R.id.tvReleaseYear)

        fun bind(movie: Movie1) {
            title.text = movie.title

            // Format the release date to show only the year, with error handling
            if (!movie.releaseDate.isNullOrEmpty()) {
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(movie.releaseDate)
                    val year = SimpleDateFormat("yyyy", Locale.US).format(date)
                    releaseYear.text = year
                } catch (e: Exception) {
                    releaseYear.text = "----"
                }
            } else {
                releaseYear.text = "----"
            }

            Glide.with(itemView.context)
                .load("https://image.tmdb.org/t/p/w500" + movie.posterPath)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(poster)

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, MovieDetailsActivity::class.java).apply {
                    putExtra("movie_id", movie.id)
                    putExtra("movie_title", movie.title)
                    putExtra("movie_poster", movie.posterPath)
                    putExtra("movie_backdrop", movie.backdropPath ?: "")
                    putExtra("movie_release_date", movie.releaseDate)
                    putExtra("movie_rating", movie.rating ?: 0f)
                    putExtra("movie_overview", movie.overview)
                }
                itemView.context.startActivity(intent)
            }
        }
    }
}
