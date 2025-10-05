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
        
        // Test backend connectivity
        testSimpleConnectivity()
        
        // Check if user is already logged in
        if (prefs.isLoggedIn()) {
            // Navigate to dashboard without making API calls immediately
            try {
                navigateToDashboard()
            } catch (e: Exception) {
                android.util.Log.e("AuthActivity", "Auto-navigation failed", e)
                // Continue with login screen if auto-navigation fails
            }
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
                        // Check if account is deactivated (for owners)
                        if (isOwner && loginResponse.role == "EVOwner") {
                            val statusResult = authRepository.checkAccountStatus(loginResponse.nic ?: "")
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
                            // Debug: Log the role and navigation decision
                            android.util.Log.d("AuthActivity", "User role: ${loginResponse.role}")
                            android.util.Log.d("AuthActivity", "Is owner: ${prefs.isOwner()}")
                            android.util.Log.d("AuthActivity", "Is operator: ${prefs.isOperator()}")
                            
                            // Add a small delay to ensure login is fully processed
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                navigateToDashboard()
                            }, 500)
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
            val isOwner = prefs.isOwner()
            val isOperator = prefs.isOperator()
            val role = prefs.getRole()
            
            android.util.Log.d("AuthActivity", "Navigation - Role: $role, IsOwner: $isOwner, IsOperator: $isOperator")
            
            // Always try OwnerDashboardActivity first as it's more stable
            val intent = Intent(this, OwnerDashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            android.util.Log.d("AuthActivity", "Starting OwnerDashboardActivity")
            startActivity(intent)
            finish()
            
        } catch (e: Exception) {
            android.util.Log.e("AuthActivity", "Navigation error", e)
            Toasts.showError(this, "Failed to navigate: ${e.message}")
            
            // If navigation fails, just show success message and stay on login screen
            Toasts.showSuccess(this, "Login successful! Please try again.")
        }
    }
    
    private fun fillDemoData() {
        etUsername.setText("123456789V")
        etPassword.setText("Password123")
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
                
                // Try multiple endpoints
                val endpoints = listOf(
                    "${BuildConfig.BASE_URL}/swagger",
                    "${BuildConfig.BASE_URL}/health",
                    "${BuildConfig.BASE_URL}/",
                    "http://10.0.2.2:5034/swagger",
                    "http://10.0.2.2:5034/health",
                    "http://10.0.2.2:5034/"
                )
                
                var connected = false
                var lastError: Exception? = null
                
                for (endpoint in endpoints) {
                    try {
                        android.util.Log.d("AuthActivity", "Trying endpoint: $endpoint")
                        val request = okhttp3.Request.Builder()
                            .url(endpoint)
                            .build()
                        
                        val response = client.newCall(request).execute()
                        android.util.Log.d("AuthActivity", "Response from $endpoint: ${response.code}")
                        
                        if (response.isSuccessful) {
                            connected = true
                            withContext(Dispatchers.Main) {
                                Toasts.showSuccess(this@AuthActivity, "✓ Backend connected to: $endpoint")
                            }
                            response.close()
                            break
                        } else {
                            android.util.Log.w("AuthActivity", "Endpoint $endpoint returned code: ${response.code}")
                            response.close()
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("AuthActivity", "Endpoint $endpoint failed: ${e.message}")
                        lastError = e
                    }
                }
                
                if (!connected) {
                    withContext(Dispatchers.Main) {
                        Toasts.showError(this@AuthActivity, "✗ Backend not reachable. Last error: ${lastError?.message}")
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toasts.showError(this@AuthActivity, "✗ Connection test failed: ${e.javaClass.simpleName}")
                }
            }
        }
    }
    
    private fun testSimpleConnectivitySilent() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Test basic HTTP connectivity using OkHttp directly (silent)
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val request = okhttp3.Request.Builder()
                    .url("${BuildConfig.BASE_URL}/swagger")
                    .build()
                
                val response = client.newCall(request).execute()
                android.util.Log.d("AuthActivity", "Backend connectivity test: ${response.code}")
                response.close()
            } catch (e: Exception) {
                android.util.Log.d("AuthActivity", "Backend not available: ${e.javaClass.simpleName}")
                // Don't show any messages to user
            }
        }
    }
}
