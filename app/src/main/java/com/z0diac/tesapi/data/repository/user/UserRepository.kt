package com.z0diac.tesapi.data.repository.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.z0diac.tesapi.data.model.Review
import com.z0diac.tesapi.data.model.User
import com.z0diac.tesapi.data.model.UserMovieList
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // Get current user ID or null if not logged in
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Save user data to Firestore
    suspend fun saveUser(user: User): Boolean {
        return try {
            usersCollection.document(user.uid).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Get user by ID
    suspend fun getUser(uid: String): User? {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                document.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Add movie to watchlist
    suspend fun addToWatchlist(userId: String, movie: UserMovieList) {
        usersCollection
            .document(userId)
            .collection("watchlist")
            .document(movie.movieId.toString())
            .set(movie)
            .await()
    }

    // Remove movie from watchlist
    suspend fun removeFromWatchlist(userId: String, movieId: Int) {
        usersCollection
            .document(userId)
            .collection("watchlist")
            .document(movieId.toString())
            .delete()
            .await()
    }

    // Check if movie is in watchlist
    suspend fun isInWatchlist(userId: String, movieId: Int): Boolean {
        val document = usersCollection
            .document(userId)
            .collection("watchlist")
            .document(movieId.toString())
            .get()
            .await()
        return document.exists()
    }

    // Get user's watchlist
    suspend fun getWatchlist(userId: String): List<UserMovieList> {
        val snapshot = usersCollection
            .document(userId)
            .collection("watchlist")
            .get()
            .await()
        return snapshot.toObjects(UserMovieList::class.java)
    }

    // Similar functions for favorites
    suspend fun addToFavorites(userId: String, movie: UserMovieList) {
        usersCollection
            .document(userId)
            .collection("favorites")
            .document(movie.movieId.toString())
            .set(movie)
            .await()
    }

    suspend fun removeFromFavorites(userId: String, movieId: Int) {
        usersCollection
            .document(userId)
            .collection("favorites")
            .document(movieId.toString())
            .delete()
            .await()
    }

    suspend fun isInFavorites(userId: String, movieId: Int): Boolean {
        val document = usersCollection
            .document(userId)
            .collection("favorites")
            .document(movieId.toString())
            .get()
            .await()
        return document.exists()
    }

    suspend fun getFavorites(userId: String): List<UserMovieList> {
        val snapshot = usersCollection
            .document(userId)
            .collection("favorites")
            .get()
            .await()
        return snapshot.toObjects(UserMovieList::class.java)
    }

    // Functions for reviews
    suspend fun addReview(review: Review): String {
        val reviewId = UUID.randomUUID().toString()

        // Ambil data user untuk username & profilePictureUrl
        val userData = getUser(review.userId)

        val reviewWithId = review.copy(
            id = reviewId,
            username = userData?.username ?: review.username,
            profilePictureUrl = userData?.profilePictureUrl // <-- Tambahan field baru
        )

        // Simpan ke subcollection users/{uid}/reviews
        usersCollection
            .document(review.userId)
            .collection("reviews")
            .document(reviewId)
            .set(reviewWithId)
            .await()

        // Simpan juga ke movies/{movieId}/reviews
        db.collection("movies")
            .document(review.movieId.toString())
            .collection("reviews")
            .document(reviewId)
            .set(reviewWithId)
            .await()

        return reviewId
    }

}