package com.z0diac.tesapi.ui.dashboard

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.model.Movie1
import com.z0diac.tesapi.ui.dashboard.MovieDetailsActivity

class MovieAdapter(private var movies: List<Movie1>) :
    RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    fun updateMovies(newMovies: List<Movie1>) {
        movies = newMovies
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
        holder.bind(movie)
    }

    override fun getItemCount(): Int = movies.size

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView = itemView.findViewById(R.id.ivPoster)

        fun bind(movie: Movie1) {
            Glide.with(itemView.context)
                .load("https://image.tmdb.org/t/p/w500" + movie.posterPath)
                .placeholder(R.drawable.placeholder_image)
                .into(poster)

            // Tambahkan OnClickListener untuk pindah ke MovieDetailsActivity
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, MovieDetailsActivity::class.java)
                intent.putExtra("MOVIE_DATA", movie.copy(genres = movie.genres ?: emptyList())) // Kirim data movie
                itemView.context.startActivity(intent)
            }
        }
    }
}
