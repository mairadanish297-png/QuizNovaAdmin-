package com.quiznova.admin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            checkLogin()
        }, 1500)
    }

    private fun checkLogin() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        checkAdminAccess(user.email ?: "")
    }

    private fun checkAdminAccess(email: String) {
        Log.d("AdminSplash", "Checking access for: $email")

        FirebaseFirestore.getInstance()
            .collection("admin")
            .document("config")
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // Config nahi hai — login pe jao (auto-setup hoga)
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    return@addOnSuccessListener
                }

                val allowedEmails = doc.get("allowedEmails") as? List<String> ?: listOf()
                val isAllowed = allowedEmails.any {
                    it.equals(email, ignoreCase = true)
                }

                if (isAllowed) {
                    startActivity(Intent(this, DashboardActivity::class.java))
                } else {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        putExtra("error", "This email is not authorized as admin")
                    })
                }
                finish()
            }
            .addOnFailureListener {
                // Error — login pe jao
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
    }
}