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
        usersCollection
            .document(userId)
            .collection("reviews")
            .document(review.id)
            .set(review)
            .await()
    }

    suspend fun deleteReview(userId: String, reviewId: String) {
        usersCollection
            .document(userId)
            .collection("reviews")
            .document(reviewId)
            .delete()
            .await()
    }



}
