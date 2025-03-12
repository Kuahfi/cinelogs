package com.z0diac.tesapi.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.z0diac.tesapi.data.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth) {

    suspend fun register(email: String, password: String): User? {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.let {
            User(it.uid, it.email ?: "")
        }
    }

    suspend fun login(email: String, password: String): User? {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.let {
            User(it.uid, it.email ?: "")
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUser(): User? {
        val user = auth.currentUser
        return user?.let { User(it.uid, it.email ?: "") }
    }
}
