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
    private lateinit var btnScanQr: MaterialButton
    private lateinit var cardInstructions: MaterialCardView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_home)
        
        initializeComponents()
        setupUI()
        setupClickListeners()
    }
    
    private fun initializeComponents() {
        prefs = Prefs(this)
        val apiClient = ApiClient(prefs)
        val authApi = AuthApi(apiClient)
        val ownerDao = OwnerDao(App.instance.dbHelper)
        authRepository = AuthRepository(authApi, ownerDao, prefs)
        
        // Initialize UI components
        tvWelcome = findViewById(R.id.tv_welcome)
        btnScanQr = findViewById(R.id.btn_scan_qr)
        cardInstructions = findViewById(R.id.card_instructions)
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        
        tvWelcome.text = "Welcome, Operator!"
    }
    
    private fun setupClickListeners() {
        // Scan QR button
        btnScanQr.setOnClickListener {
            startActivity(Intent(this, QrScanActivity::class.java))
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
