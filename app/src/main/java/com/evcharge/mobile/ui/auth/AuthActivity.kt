package com.evcharge.mobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evcharge.mobile.App
import com.evcharge.mobile.BuildConfig
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.Toasts
import com.evcharge.mobile.common.Validators
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.getErrorOrNull
import com.evcharge.mobile.common.isSuccess
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.AuthApi
import com.evcharge.mobile.data.db.OwnerDao
import com.evcharge.mobile.data.dto.LoginRequest
import com.evcharge.mobile.data.repo.AuthRepository
import com.evcharge.mobile.ui.dashboard.OwnerDashboardActivity
import com.evcharge.mobile.ui.dashboard.OperatorHomeActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Authentication activity for login
 */
class AuthActivity : AppCompatActivity() {
    
    private lateinit var prefs: Prefs
    private lateinit var authRepository: AuthRepository
    
    // UI Components
    private lateinit var userTypeToggle: MaterialButtonToggleGroup
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnFakeLogin: MaterialButton
    
    private var isOwner = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        
        initializeComponents()
        setupUI()
        setupClickListeners()
        
        // Check if user is already logged in
        if (prefs.isLoggedIn()) {
            navigateToDashboard()
        }
    }
    
    private fun initializeComponents() {
        prefs = Prefs(this)
        val apiClient = ApiClient(prefs)
        val authApi = AuthApi(apiClient)
        val ownerDao = OwnerDao(App.instance.dbHelper)
        authRepository = AuthRepository(authApi, ownerDao, prefs)
        
        // Initialize UI components
        userTypeToggle = findViewById(R.id.user_type_toggle)
        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnRegister = findViewById(R.id.btn_register)
        btnFakeLogin = findViewById(R.id.btn_fake_login)
    }
    
    private fun setupUI() {
        // Set default selection
        userTypeToggle.check(R.id.btn_owner)
        
        // Show debug login in debug builds
        if (BuildConfig.DEBUG) {
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.debug_card).visibility = android.view.View.VISIBLE
        }
    }
    
    private fun setupClickListeners() {
        // User type toggle
        userTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isOwner = checkedId == R.id.btn_owner
            }
        }
        
        // Login button
        btnLogin.setOnClickListener {
            performLogin()
        }
        
        // Register button
        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        // Fake login button (debug only)
        btnFakeLogin.setOnClickListener {
            fillDemoData()
        }
    }
    
    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        // Validate input
        if (username.isEmpty()) {
            Toasts.showValidationError(this, "Username or NIC is required")
            return
        }
        
        if (password.isEmpty()) {
            Toasts.showValidationError(this, "Password is required")
            return
        }
        
        // Show loading
        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."
        
        val request = LoginRequest(username, password)
        
        lifecycleScope.launch {
            try {
                val result = authRepository.login(request)
                
                if (result.isSuccess()) {
                    val loginResponse = result.getDataOrNull()
                    if (loginResponse != null) {
                        // Check if account is deactivated (for owners)
                        if (isOwner && loginResponse.role == "Owner") {
                            val statusResult = authRepository.checkAccountStatus(loginResponse.nic ?: "")
                            if (statusResult.isSuccess()) {
                                val isActive = statusResult.getDataOrNull() ?: true
                                if (!isActive) {
                                    Toasts.showError(this@AuthActivity, "Your account is deactivated. Contact Backoffice for reactivation.")
                                    return@launch
                                }
                            }
                        }
                        
                        Toasts.showSuccess(this@AuthActivity, "Login successful")
                        navigateToDashboard()
                    }
                } else {
                    val error = result.getErrorOrNull()
                    val message = error?.message ?: "Login failed"
                    Toasts.showError(this@AuthActivity, message)
                }
            } catch (e: Exception) {
                Toasts.showError(this@AuthActivity, "Login failed: ${e.message}")
            } finally {
                // Reset button
                btnLogin.isEnabled = true
                btnLogin.text = getString(R.string.login_button)
            }
        }
    }
    
    private fun navigateToDashboard() {
        val intent = if (prefs.isOwner()) {
            Intent(this, OwnerDashboardActivity::class.java)
        } else {
            Intent(this, OperatorHomeActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
    
    private fun fillDemoData() {
        etUsername.setText("123456789V")
        etPassword.setText("password123")
    }
}
