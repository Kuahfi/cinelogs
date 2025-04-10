package com.z0diac.tesapi.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.model.Movie1

class FavoritesAdapter(
    private var movies: MutableList<Movie1>,
    private val onMovieClick: (Movie1) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.MovieViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun getItemCount(): Int {
        return movies.size
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView = itemView.findViewById(R.id.ivPoster)

        fun bind(movie: Movie1) {
            Glide.with(itemView.context)
                .load("https://image.tmdb.org/t/p/w500" + movie.posterPath)
                .placeholder(R.drawable.placeholder_image)
                .into(poster)

            itemView.setOnClickListener {
                onMovieClick(movie)
            }
        }
    }

    fun updateMovies(newMovies: List<Movie1>) {
        movies.clear()
        movies.addAll(newMovies)
        notifyDataSetChanged()
    }
}
