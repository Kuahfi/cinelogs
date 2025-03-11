package com.z0diac.tesapi.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.z0diac.tesapi.databinding.ActivityDashboardBinding
import com.z0diac.tesapi.ui.auth.AuthViewModel
import com.z0diac.tesapi.ui.auth.LoginActivity

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.user.observe(this) { user ->
            if (user != null) {
                binding.tvWelcome.text = "Selamat datang, ${user.email}!"
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
