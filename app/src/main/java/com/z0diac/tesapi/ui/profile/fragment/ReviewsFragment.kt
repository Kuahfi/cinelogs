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
import com.z0diac.tesapi.data.repository.user.ReviewRepository
import com.z0diac.tesapi.ui.adapters.ReviewAdapter
import kotlinx.coroutines.launch

class ReviewsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var reviewsAdapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reviews, container, false)

        Log.d("ReviewsFragment", "onCreateView")

        recyclerView = view.findViewById(R.id.recyclerViewReviews)
        emptyView = view.findViewById(R.id.tvEmptyReviews)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        reviewsAdapter = ReviewAdapter(emptyList(), isProfileView = true)
        recyclerView.adapter = reviewsAdapter

        loadUserReviews()

        return view
    }

    private fun loadUserReviews() {
        val userId = arguments?.getString("USER_ID")
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
                Log.d("ReviewsFragment", "Fetching reviews from repository")
                val reviewRepository = ReviewRepository()
                val reviews = reviewRepository.getUserReviews(userId)
                Log.d("ReviewsFragment", "Fetched ${reviews.size} reviews")

                activity?.runOnUiThread {
                    if (reviews.isEmpty()) {
                        Log.d("ReviewsFragment", "No reviews found")
                        recyclerView.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                    } else {
                        Log.d("ReviewsFragment", "Displaying ${reviews.size} reviews")
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