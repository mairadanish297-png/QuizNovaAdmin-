package com.quiznova.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quiznova.admin.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val errorMsg = intent.getStringExtra("error")
        if (errorMsg != null) {
            showError(errorMsg)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                showError("Enter email address")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showError("Enter password")
                return@setOnClickListener
            }

            loginAdmin(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                showError("Enter email first")
                return@setOnClickListener
            }
            sendResetEmail(email)
        }
    }

    private fun loginAdmin(email: String, password: String) {
        showLoading(true)
        hideError()

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                verifyAdminAccess(email)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Login failed: ${e.message}")
            }
    }

    private fun verifyAdminAccess(email: String) {
        binding.tvStatus.text = "Verifying admin access..."
        Log.d("AdminLogin", "Checking access for: $email")

        db.collection("admin")
            .document("config")
            .get()
            .addOnSuccessListener { doc ->
                showLoading(false)

                if (!doc.exists()) {
                    // Document nahi hai — pehla admin banao
                    Log.d("AdminLogin", "Config not found, creating first admin")
                    createFirstAdmin(email)
                    return@addOnSuccessListener
                    return@addOnSuccessListener
                }

                val allowedEmails = doc.get("allowedEmails") as? List<String>

                Log.d("AdminLogin", "Allowed emails: $allowedEmails")

                if (allowedEmails == null || allowedEmails.isEmpty()) {
                    // allowedEmails field nahi hai — add karo
                    Log.d("AdminLogin", "No allowedEmails field, adding current email")
                    addEmailToConfig(email)
                    return@addOnSuccessListener
                }

                // Case-insensitive check
                val isAllowed = allowedEmails.any {
                    it.equals(email, ignoreCase = true)
                }

                if (isAllowed) {
                    Log.d("AdminLogin", "Access granted!")
                    Toast.makeText(this, "Welcome Admin!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Log.d("AdminLogin", "Access denied. Not in list.")
                    auth.signOut()
                    showError("Email not authorized.\nYour email: $email\nAllowed: $allowedEmails")
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e("AdminLogin", "Firestore error", e)

                // Error aaya — try to create admin config
                createFirstAdmin(email)
            }
    }

    private fun createFirstAdmin(email: String) {
        Log.d("AdminLogin", "Creating first admin for: $email")

        val config = hashMapOf(
            "allowedEmails" to listOf(email),
            "roles" to hashMapOf(email to "super_admin"),
            "coinToDollarRate" to 100,
            "minWithdrawal" to 500,
            "maxWithdrawal" to 5000,
            "maintenanceMode" to false
        )

        db.collection("admin")
            .document("config")
            .set(config)
            .addOnSuccessListener {
                Log.d("AdminLogin", "First admin created successfully!")
                Toast.makeText(this, "Admin setup complete!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("AdminLogin", "Failed to create admin config", e)
                showError("Setup failed: ${e.message}\n\nPlease add manually in Firebase Console:\nCollection: admin\nDocument: config\nField: allowedEmails (array)\nValue: $email")
            }
    }

    private fun addEmailToConfig(email: String) {
        Log.d("AdminLogin", "Adding $email to existing config")

        db.collection("admin")
            .document("config")
            .update(
                "allowedEmails", com.google.firebase.firestore.FieldValue.arrayUnion(email),
                "roles.$email", "super_admin"
            )
            .addOnSuccessListener {
                Log.d("AdminLogin", "Email added to config!")
                Toast.makeText(this, "Admin access granted!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("AdminLogin", "Failed to add email", e)
                showError("Failed: ${e.message}")
            }
    }

    private fun sendResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                showError("Failed: ${e.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvStatus.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnLogin.text = if (show) "" else "LOGIN"
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }
}