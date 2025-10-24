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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        
        // Test backend connectivity (safely)
        try {
            testSimpleConnectivity()
        } catch (e: Exception) {
            // Don't crash the app if connectivity test fails
            android.util.Log.w("AuthActivity", "Connectivity test failed: ${e.message}")
        }
        
        // Check if user is already logged in with valid session
        try {
            if (prefs.isLoggedIn() && prefs.hasValidSession()) {
                // User is already logged in with valid session, navigate to dashboard
                navigateToDashboard()
            }
        } catch (e: Exception) {
            android.util.Log.w("AuthActivity", "Navigation check failed: ${e.message}")
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
        } catch (e: Exception) {
            Toasts.showError(this, "Initialization failed: ${e.message}")
            finish()
        }
        
        // Initialize UI components
        try {
            userTypeToggle = findViewById(R.id.user_type_toggle)
            etUsername = findViewById(R.id.et_username)
            etPassword = findViewById(R.id.et_password)
            btnLogin = findViewById(R.id.btn_login)
            btnRegister = findViewById(R.id.btn_register)
            btnFakeLogin = findViewById(R.id.btn_fake_login)
        } catch (e: Exception) {
            Toasts.showError(this, "UI initialization failed: ${e.message}")
            finish()
        }
    }
    
    private fun setupUI() {
        try {
            // Set default selection
            userTypeToggle.check(R.id.btn_owner)
            
            // Show debug login in debug builds
            if (BuildConfig.DEBUG) {
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.debug_card).visibility = android.view.View.VISIBLE
            }
        } catch (e: Exception) {
            Toasts.showError(this, "UI setup failed: ${e.message}")
        }
    }
    
    private fun setupClickListeners() {
        try {
            // User type toggle
            userTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    isOwner = checkedId == R.id.btn_owner
                    
                    // Clear username field when switching roles to prevent confusion
                    etUsername.text?.clear()
                }
            }
            
            // Login button
            btnLogin.setOnClickListener {
                performLogin()
            }
            
            // Register button
            btnRegister.setOnClickListener {
                try {
                    startActivity(Intent(this, RegisterActivity::class.java))
                } catch (e: Exception) {
                    Toasts.showError(this, "Failed to open registration: ${e.message}")
                }
            }
            
            // Fake login button (debug only)
            btnFakeLogin.setOnClickListener {
                fillDemoData()
            }
        } catch (e: Exception) {
            Toasts.showError(this, "Click listeners setup failed: ${e.message}")
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
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    Toasts.showInfo(this@AuthActivity, "Attempting login with: $username")
                }
                val result = authRepository.login(request)
                
                if (result.isSuccess()) {
                    val loginResponse = result.getDataOrNull()
                    if (loginResponse != null) {
                        // Validate selected role against actual user role
                        val isValidRole = validateUserRole(loginResponse.role)
                        if (!isValidRole) {
                            withContext(Dispatchers.Main) {
                                val expectedRole = if (isOwner) "EV Owner" else "Station Operator"
                                val actualRole = when (loginResponse.role) {
                                    "EVOwner" -> "EV Owner"
                                    "StationOperator" -> "Station Operator"
                                    "Backoffice" -> "Backoffice Admin"
                                    else -> loginResponse.role
                                }
                                Toasts.showError(this@AuthActivity, "Role mismatch! You selected '$expectedRole' but your account is '$actualRole'. Please select the correct role and try again.")
                            }
                            return@launch
                        }
                        
                        // Check if account is deactivated (for EVOwners only)
                        if (loginResponse.role == "EVOwner") {
                            val userId = loginResponse.userId // For EVOwner, userId is the NIC
                            val statusResult = authRepository.checkAccountStatus(userId)
                            if (statusResult.isSuccess()) {
                                val isActive = statusResult.getDataOrNull() ?: true
                                if (!isActive) {
                                    withContext(Dispatchers.Main) {
                                        Toasts.showError(this@AuthActivity, "Your account is deactivated. Contact Backoffice for reactivation.")
                                    }
                                    return@launch
                                }
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            Toasts.showSuccess(this@AuthActivity, "Login successful")
                            // Debug: Show role information
                            Toasts.showInfo(this@AuthActivity, "Role: ${loginResponse.role}, UserId: ${loginResponse.userId}")
                            navigateToDashboard()
                        }
                    }
                } else {
                    val error = result.getErrorOrNull()
                    val message = error?.message ?: "Login failed"
                    withContext(Dispatchers.Main) {
                        Toasts.showError(this@AuthActivity, "Login failed: $message")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toasts.showError(this@AuthActivity, "Login exception: ${e.javaClass.simpleName} - ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    // Reset button
                    btnLogin.isEnabled = true
                    btnLogin.text = getString(R.string.login_button)
                }
            }
        }
    }
    
    private fun navigateToDashboard() {
        try {
            val role = prefs.getRole()
            val isOwner = prefs.isOwner()
            val isStationOperator = prefs.isStationOperator()
            val isBackoffice = prefs.isBackoffice()
            
            Toasts.showInfo(this, "Navigating - Role: $role, IsOwner: $isOwner, IsStationOperator: $isStationOperator, IsBackoffice: $isBackoffice")
            
            val intent = when {
                isOwner -> {
                    // EV Owner - navigate to owner dashboard
                    Intent(this, OwnerDashboardActivity::class.java)
                }
                isStationOperator || isBackoffice -> {
                    // Station Operator or Backoffice - navigate to operator dashboard
                    Intent(this, OperatorHomeActivity::class.java)
                }
                else -> {
                    // Unknown role - default to owner dashboard
                    Toasts.showError(this, "Unknown role: $role. Navigating to owner dashboard.")
                    Intent(this, OwnerDashboardActivity::class.java)
                }
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toasts.showError(this, "Navigation failed: ${e.message}")
            // Fallback: try to navigate to owner dashboard
            try {
                startActivity(Intent(this, OwnerDashboardActivity::class.java))
                finish()
            } catch (e2: Exception) {
                Toasts.showError(this, "Fallback navigation also failed: ${e2.message}")
            }
        }
    }
    
    private fun fillDemoData() {
        etUsername.setText("123456789V")
        etPassword.setText("Password123")
    }
    
    /**
     * Validate if the selected role matches the actual user role from backend
     */
    private fun validateUserRole(actualRole: String): Boolean {
        return when {
            isOwner && actualRole == "EVOwner" -> true
            !isOwner && (actualRole == "StationOperator" || actualRole == "Backoffice") -> true
            else -> false
        }
    }
    
    private fun testSimpleConnectivity() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Show debug info about the BASE_URL being used
                withContext(Dispatchers.Main) {
                    Toasts.showInfo(this@AuthActivity, "Testing: ${BuildConfig.BASE_URL}")
                }
                
                // Test basic HTTP connectivity using OkHttp directly
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val request = okhttp3.Request.Builder()
                    .url("${BuildConfig.BASE_URL}/swagger")
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toasts.showSuccess(this@AuthActivity, "✓ Backend connected successfully")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toasts.showWarning(this@AuthActivity, "⚠ Backend responded with code: ${response.code}")
                    }
                }
                response.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toasts.showError(this@AuthActivity, "✗ Connection failed: ${e.javaClass.simpleName}")
                }
            }
        }
    }
}
