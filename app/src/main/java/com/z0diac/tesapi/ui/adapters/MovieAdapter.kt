package com.z0diac.tesapi.ui.adapters

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

class MovieAdapter(
    private val onLoadMore: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val movies: MutableList<Movie1> = mutableListOf()
    private val movieViewType = 1
    private val loadMoreViewType = 2

    override fun getItemViewType(position: Int): Int {
        return if (position < movies.size) movieViewType else loadMoreViewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == movieViewType) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
            MovieViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_load_more, parent, false)
            LoadMoreViewHolder(view, onLoadMore)
        }
    }

    override fun getItemCount(): Int {
        return if (movies.isEmpty()) 0 else movies.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MovieViewHolder) {
            holder.bind(movies[position])
        }
    }

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView = itemView.findViewById(R.id.ivPoster)

        fun bind(movie: Movie1) {
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

    inner class LoadMoreViewHolder(itemView: View, onLoadMore: () -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        init {
            val btnLoadMore = itemView.findViewById<ImageView>(R.id.btnLoadMore)
            btnLoadMore.setOnClickListener {
                onLoadMore()
            }
        }
    }

    fun setMovies(newMovies: List<Movie1>) {
        movies.clear()
        movies.addAll(newMovies)
        notifyDataSetChanged()
    }

    fun addMovies(newMovies: List<Movie1>) {
        val startPosition = movies.size
        movies.addAll(newMovies)
        notifyItemRangeInserted(startPosition, newMovies.size)
    }
}
