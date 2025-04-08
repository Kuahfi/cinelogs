package com.z0diac.tesapi.ui.dashboard

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.model.Review
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter(private var reviews: List<Review>) :
    RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvReviewContent: TextView = itemView.findViewById(R.id.tvReviewContent)
        val tvReviewDate: TextView = itemView.findViewById(R.id.tvReviewDate)
        val rbUserRating: RatingBar = itemView.findViewById(R.id.rbUserRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        val context = holder.itemView.context

        holder.tvUsername.text = review.username
        holder.tvReviewContent.text = review.reviewText
        holder.tvReviewDate.text = formatDate(review.timestamp)
        holder.rbUserRating.rating = review.rating

        // Log for debugging
        Log.d("ReviewAdapter", "Bound review: ${review.username} - ${review.reviewText.take(20)}...")
    }

    override fun getItemCount(): Int {
        Log.d("ReviewAdapter", "Review count: ${reviews.size}")
        return reviews.size
    }

    fun updateReviews(newReviews: List<Review>) {
        Log.d("ReviewAdapter", "Updating reviews: ${newReviews.size}")
        this.reviews = newReviews
        notifyDataSetChanged()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}