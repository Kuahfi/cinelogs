package com.z0diac.tesapi.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.z0diac.tesapi.data.model.User
import com.z0diac.tesapi.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository(FirebaseAuth.getInstance())

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                _user.value = repository.register(email, password)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _user.value = repository.login(email, password)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun logout() {
        repository.logout()
        _user.value = null
    }

    fun checkAutoLogin() {
        _user.value = repository.getCurrentUser()
    }
}
