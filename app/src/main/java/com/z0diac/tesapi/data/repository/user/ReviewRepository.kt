package com.z0diac.tesapi.data.repository.user

import com.google.firebase.firestore.FirebaseFirestore
import com.z0diac.tesapi.data.model.Review
import kotlinx.coroutines.tasks.await

class ReviewRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val moviesCollection = db.collection("movies")

    // Tambah review ke user & movie collection
    suspend fun addReview(review: Review, movieId: Int) {
        val timestamp = review.timestamp.toString()

        // Simpan di collection user
        usersCollection
            .document(review.userId)
            .collection("reviews")
            .document(timestamp)
            .set(review)
            .await()

        // Simpan di collection film
        moviesCollection
            .document(movieId.toString())
            .collection("reviews")
            .document(timestamp)
            .set(review)
            .await()
    }

    // Ambil semua review berdasarkan movie
    suspend fun getMovieReviews(movieId: Int): List<Review> {
        val snapshot = moviesCollection
            .document(movieId.toString())
            .collection("reviews")
            .get()
            .await()
        return snapshot.toObjects(Review::class.java)
    }

    // Ambil semua review milik user
    suspend fun getUserReviews(userId: String): List<Review> {
        val snapshot = usersCollection
            .document(userId)
            .collection("reviews")
            .get()
            .await()
        return snapshot.toObjects(Review::class.java)
    }

    suspend fun getUserReviewCount(userId: String): Int {
        val snapshot = usersCollection
            .document(userId)
            .collection("reviews")
            .get()
            .await()
        return snapshot.size()
    }

}
