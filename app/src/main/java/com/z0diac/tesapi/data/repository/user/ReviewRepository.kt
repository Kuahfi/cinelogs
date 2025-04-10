package com.z0diac.tesapi.data.repository.user

import com.google.firebase.firestore.FirebaseFirestore
import com.z0diac.tesapi.data.model.Review
import kotlinx.coroutines.tasks.await

class ReviewRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun getUserReviews(userId: String): List<Review> {
        val snapshot = usersCollection
            .document(userId)
            .collection("reviews")
            .get()
            .await()
        return snapshot.toObjects(Review::class.java)
    }

    suspend fun updateReview(userId: String, review: Review) {
        val userReviewRef = db.collection("users").document(userId)
            .collection("reviews").document(review.id)
        val movieReviewRef = db.collection("movies").document(review.movieId.toString())
            .collection("reviews").document(review.id)

        val reviewMap = mapOf(
            "rating" to review.rating,
            "reviewText" to review.reviewText,
            "timestamp" to review.timestamp
        )

        userReviewRef.update(reviewMap).await()
        movieReviewRef.update(reviewMap).await()
    }

    suspend fun deleteReview(userId: String, reviewId: String, movieId: String) {
        val userReviewRef = db.collection("users").document(userId)
            .collection("reviews").document(reviewId)
        val movieReviewRef = db.collection("movies").document(movieId)
            .collection("reviews").document(reviewId)

        userReviewRef.delete().await()
        movieReviewRef.delete().await()
    }
}
