package com.z0diac.tesapi.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.model.Movie

class MovieAdapter(private var movies: List<Movie>) :
    RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    fun updateMovies(newMovies: List<Movie>) {
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

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val poster: ImageView = itemView.findViewById(R.id.ivPoster)

        fun bind(movie: Movie) {
            title.text = movie.title
            Glide.with(itemView.context)
                .load("https://image.tmdb.org/t/p/w500" + movie.posterPath)
                .placeholder(R.drawable.placeholder_image)
                .into(poster)
        }
    }
}
