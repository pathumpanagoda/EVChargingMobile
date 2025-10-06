package com.evcharge.mobile.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.evcharge.mobile.App
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.AuthApi
import com.evcharge.mobile.data.db.OwnerDao
import com.evcharge.mobile.data.repo.AuthRepository
import com.evcharge.mobile.ui.auth.AuthActivity
import com.evcharge.mobile.ui.qr.QrScanActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView

/**
 * Operator home activity
 */
class OperatorHomeActivity : AppCompatActivity() {
    
    private lateinit var prefs: Prefs
    private lateinit var authRepository: AuthRepository
    
    // UI Components
    private lateinit var tvWelcome: MaterialTextView
    private lateinit var tvOperatorName: MaterialTextView
    private lateinit var btnScanQr: MaterialButton
    private lateinit var cardInstructions: MaterialCardView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_home)
        
        try {
            initializeComponents()
            setupUI()
            setupClickListeners()
        } catch (e: Exception) {
            com.evcharge.mobile.common.Toasts.showError(this, "Operator dashboard initialization failed: ${e.message}")
            finish()
        }
    }
    
    private fun initializeComponents() {
        try {
            prefs = Prefs(this)
            val apiClient = ApiClient(prefs)
            val authApi = AuthApi(apiClient)
            
            // Safely initialize database helper
            val dbHelper = try {
                App.instance.dbHelper
            } catch (e: Exception) {
                // Fallback: create a new instance if App.instance is not ready
                com.evcharge.mobile.data.db.UserDbHelper(this)
            }
            
            val ownerDao = OwnerDao(dbHelper)
            authRepository = AuthRepository(authApi, ownerDao, prefs)
            
            // Initialize UI components
            tvWelcome = findViewById(R.id.tv_welcome)
            tvOperatorName = findViewById(R.id.tv_operator_name)
            btnScanQr = findViewById(R.id.btn_scan_qr)
            cardInstructions = findViewById(R.id.card_instructions)
        } catch (e: Exception) {
            com.evcharge.mobile.common.Toasts.showError(this, "Component initialization failed: ${e.message}")
            throw e
        }
    }
    
    private fun setupUI() {
        try {
            // Set up toolbar with error handling
            try {
                setSupportActionBar(findViewById(R.id.toolbar))
            } catch (e: Exception) {
                com.evcharge.mobile.common.Toasts.showWarning(this, "Toolbar setup skipped: ${e.message}")
            }
            
            // Set welcome message with user name and NIC (similar to React frontend)
            val userName = prefs.getName()
            val userNIC = prefs.getNIC()
            val userRole = prefs.getRole()
            
            if (userName.isNotEmpty() && userNIC.isNotEmpty()) {
                tvWelcome.text = "Hello, $userName!"
            } else if (userName.isNotEmpty()) {
                tvWelcome.text = "Hello, $userName!"
            } else if (userNIC.isNotEmpty()) {
                tvWelcome.text = "Hello, Operator!"
            } else {
                tvWelcome.text = "Hello, Operator!"
            }
            
            // Set operator name and NIC (similar to React: username/name and (role))
            if (userName.isNotEmpty() && userNIC.isNotEmpty()) {
                tvOperatorName.text = "$userName ($userNIC)"
            } else if (userName.isNotEmpty()) {
                tvOperatorName.text = userName
            } else if (userNIC.isNotEmpty()) {
                tvOperatorName.text = "NIC: $userNIC"
            } else {
                tvOperatorName.text = "Operator"
            }
        } catch (e: Exception) {
            com.evcharge.mobile.common.Toasts.showError(this, "UI setup failed: ${e.message}")
            throw e
        }
    }
    
    private fun setupClickListeners() {
        try {
            // Scan QR button
            btnScanQr.setOnClickListener {
                try {
                    com.evcharge.mobile.common.Toasts.showInfo(this, "Opening QR Scanner...")
                    startActivity(Intent(this, QrScanActivity::class.java))
                } catch (e: Exception) {
                    com.evcharge.mobile.common.Toasts.showError(this, "Failed to open QR Scanner: ${e.message}")
                }
            }
        } catch (e: Exception) {
            com.evcharge.mobile.common.Toasts.showError(this, "Click listeners setup failed: ${e.message}")
            throw e
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun logout() {
        authRepository.logout()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
