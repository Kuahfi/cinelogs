package com.z0diac.tesapi.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.z0diac.tesapi.R
import com.z0diac.tesapi.ui.auth.LoginActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvEmail: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
//        tvEmail = findViewById(R.id.tvEmail)
//        btnLogout = findViewById(R.id.btnLogout)

        val user = auth.currentUser
        tvEmail.text = user?.email ?: "No email (Anonymous)"

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
