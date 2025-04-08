package com.z0diac.tesapi.profile.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.model.Review
import com.z0diac.tesapi.data.repository.user.ReviewRepository
import com.z0diac.tesapi.ui.adapters.OnReviewOptionClickListener
import com.z0diac.tesapi.ui.adapters.ReviewAdapter
import kotlinx.coroutines.launch

class ReviewsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var reviewsAdapter: ReviewAdapter

    private var userId: String? = null // ✅ User ID disimpan sebagai property class

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reviews, container, false)

        Log.d("ReviewsFragment", "onCreateView")

        recyclerView = view.findViewById(R.id.recyclerViewReviews)
        emptyView = view.findViewById(R.id.tvEmptyReviews)

        recyclerView.layoutManager = LinearLayoutManager(context)

        reviewsAdapter = ReviewAdapter(
            emptyList(),
            isProfileView = true,
            optionClickListener = object : OnReviewOptionClickListener {
                override fun onEditClicked(review: Review) {
                    showEditDialog(review)
                }

                override fun onDeleteClicked(review: Review) {
                    deleteReview(review)
                }
            }
        )

        recyclerView.adapter = reviewsAdapter

        userId = arguments?.getString("USER_ID") // ✅ Ambil userId dari arguments
        loadUserReviews()

        return view
    }

    private fun loadUserReviews() {
        Log.d("ReviewsFragment", "Loading reviews for user: $userId")

        if (userId == null) {
            Log.e("ReviewsFragment", "User ID is null")
            emptyView.text = "Error: User ID not found"
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val reviewRepository = ReviewRepository()
                val reviews = reviewRepository.getUserReviews(userId!!)
                Log.d("ReviewsFragment", "Fetched ${reviews.size} reviews")

                activity?.runOnUiThread {
                    if (reviews.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                        reviewsAdapter.updateReviews(reviews)
                    }
                }
            } catch (e: Exception) {
                Log.e("ReviewsFragment", "Error loading reviews", e)
                activity?.runOnUiThread {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "Error loading reviews: ${e.message}"
                }
            }
        }
    }

    private fun showEditDialog(review: Review) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_review, null)
        val ratingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.ratingBar)
        val reviewText = dialogView.findViewById<android.widget.EditText>(R.id.etReviewText)

        ratingBar.rating = review.rating
        reviewText.setText(review.reviewText)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Review")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val updatedRating = ratingBar.rating
                val updatedText = reviewText.text.toString()
                updateReview(review, updatedRating, updatedText)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun updateReview(review: Review, newRating: Float, newText: String) {
        val updatedReview = review.copy(
            rating = newRating,
            reviewText = newText,
            timestamp = System.currentTimeMillis()
        )

        userId?.let { uid ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val reviewRepository = ReviewRepository()
                    reviewRepository.updateReview(uid, updatedReview)
                    loadUserReviews()
                } catch (e: Exception) {
                    Log.e("ReviewsFragment", "Failed to update review", e)
                }
            }
        }
    }

    private fun deleteReview(review: Review) {
        userId?.let { uid ->
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Review")
                .setMessage("Are you sure you want to delete this review?")
                .setPositiveButton("Delete") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val reviewRepository = ReviewRepository()
                            reviewRepository.deleteReview(uid, review.id)
                            loadUserReviews()
                        } catch (e: Exception) {
                            Log.e("ReviewsFragment", "Failed to delete review", e)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    companion object {
        fun newInstance(userId: String): ReviewsFragment {
            Log.d("ReviewsFragment", "Creating new instance for user: $userId")
            val fragment = ReviewsFragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            fragment.arguments = args
            return fragment
        }
    }
}
