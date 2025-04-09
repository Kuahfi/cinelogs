package com.z0diac.tesapi.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.model.Review
import java.text.SimpleDateFormat
import java.util.*

interface OnReviewOptionClickListener {
    fun onEditClicked(review: Review)
    fun onDeleteClicked(review: Review)
}

class ReviewAdapter(
    private var reviews: List<Review>,
    private val isProfileView: Boolean,
    private val optionClickListener: OnReviewOptionClickListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_PROFILE = 1
    private val VIEW_TYPE_SIMPLE = 2

    inner class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMovieTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)
        val tvReviewText: TextView = itemView.findViewById(R.id.tvReviewText)
        val tvReviewDate: TextView = itemView.findViewById(R.id.tvReviewDate)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val ivMoviePoster: ImageView = itemView.findViewById(R.id.ivMoviePoster)
        val ivMoreOptions: ImageView = itemView.findViewById(R.id.btnOptions)
    }

    inner class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivUserAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvReviewContent: TextView = itemView.findViewById(R.id.tvReviewContent)
        val tvReviewDate: TextView = itemView.findViewById(R.id.tvReviewDate)
        val rbUserRating: RatingBar = itemView.findViewById(R.id.rbUserRating)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isProfileView) VIEW_TYPE_PROFILE else VIEW_TYPE_SIMPLE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_PROFILE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_review_user, parent, false)
            ProfileViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_review, parent, false)
            SimpleViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val review = reviews[position]

        if (holder is ProfileViewHolder) {
            holder.tvMovieTitle.text = review.movieTitle
            holder.tvReviewText.text = review.reviewText
            holder.tvReviewDate.text = formatDate(review.timestamp)
            holder.ratingBar.rating = review.rating

            if (!review.posterPath.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load("https://image.tmdb.org/t/p/w200${review.posterPath}")
                    .placeholder(R.drawable.placeholder_poster)
                    .into(holder.ivMoviePoster)
            } else {
                holder.ivMoviePoster.setImageResource(R.drawable.placeholder_poster)
            }

            holder.ivMoreOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.inflate(R.menu.menu_review_options)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_edit -> {
                            optionClickListener?.onEditClicked(review)
                            true
                        }
                        R.id.menu_delete -> {
                            optionClickListener?.onDeleteClicked(review)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }

        } else if (holder is SimpleViewHolder) {
            holder.tvUsername.text = review.username
            holder.tvReviewContent.text = review.reviewText
            holder.tvReviewDate.text = formatDate(review.timestamp)
            holder.rbUserRating.rating = review.rating

            // ✅ Load foto profil user
            Glide.with(holder.itemView.context)
                .load(review.profilePictureUrl ?: R.drawable.default_profile)
                .placeholder(R.drawable.default_profile)
                .circleCrop()
                .into(holder.ivUserAvatar)
        }
    }

    override fun getItemCount(): Int = reviews.size

    fun updateReviews(newReviews: List<Review>) {
        this.reviews = newReviews
        notifyDataSetChanged()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
