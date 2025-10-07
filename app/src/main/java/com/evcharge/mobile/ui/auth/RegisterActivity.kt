package com.evcharge.mobile.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evcharge.mobile.App
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
import com.evcharge.mobile.data.dto.RegisterRequest
import com.evcharge.mobile.data.repo.AuthRepository
import com.evcharge.mobile.ui.dashboard.OwnerDashboardActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Registration activity for EV owners
 */
class RegisterActivity : AppCompatActivity() {
    
    private lateinit var prefs: Prefs
    private lateinit var authRepository: AuthRepository
    
    // UI Components
    private lateinit var etNic: TextInputEditText
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnLogin: MaterialButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        initializeComponents()
        setupClickListeners()
    }
    
    private fun initializeComponents() {
        prefs = Prefs(this)
        val apiClient = ApiClient(prefs)
        val authApi = AuthApi(apiClient)
        val ownerDao = OwnerDao(App.instance.dbHelper)
        authRepository = AuthRepository(authApi, ownerDao, prefs)
        
        // Initialize UI components
        etNic = findViewById(R.id.et_nic)
        etName = findViewById(R.id.et_name)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        btnLogin = findViewById(R.id.btn_login)
    }
    
    private fun setupClickListeners() {
        // Register button
        btnRegister.setOnClickListener {
            performRegistration()
        }
        
        // Login button
        btnLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun performRegistration() {
        val nic = etNic.text.toString().trim()
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        
        // Validate input
        if (!validateInput(nic, name, email, phone, password, confirmPassword)) {
            return
        }
        
        // Show loading
        btnRegister.isEnabled = false
        btnRegister.text = "Registering..."
        
        val request = RegisterRequest(nic, name, email, phone, password)
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val result = authRepository.registerOwner(request)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess()) {
                        val registerResponse = result.getDataOrNull()
                        if (registerResponse?.data != null) {
                            Toasts.showSuccess(this@RegisterActivity, "Registration successful")
                            navigateToDashboard()
                        } else {
                            Toasts.showError(this@RegisterActivity, "Registration failed")
                        }
                    } else {
                        val error = result.getErrorOrNull()
                        val message = error?.message ?: "Registration failed"
                        Toasts.showError(this@RegisterActivity, message)
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showError(this@RegisterActivity, "Registration failed: ${e.message}")
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    // Reset button
                    btnRegister.isEnabled = true
                    btnRegister.text = getString(R.string.register_button)
                }
            }
        }
    }
    
    private fun validateInput(
        nic: String,
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        // Validate NIC
        if (!Validators.isValidNIC(nic)) {
            Toasts.showValidationError(this, Validators.getNICErrorMessage())
            etNic.requestFocus()
            return false
        }
        
        // Validate name
        if (!Validators.isValidName(name)) {
            Toasts.showValidationError(this, Validators.getNameErrorMessage())
            etName.requestFocus()
            return false
        }
        
        // Validate email
        if (!Validators.isValidEmail(email)) {
            Toasts.showValidationError(this, Validators.getEmailErrorMessage())
            etEmail.requestFocus()
            return false
        }
        
        // Validate phone
        if (!Validators.isValidPhone(phone)) {
            Toasts.showValidationError(this, Validators.getPhoneErrorMessage())
            etPhone.requestFocus()
            return false
        }
        
        // Validate password
        if (!Validators.isValidPassword(password)) {
            Toasts.showValidationError(this, Validators.getPasswordErrorMessage())
            etPassword.requestFocus()
            return false
        }
        
        // Validate password confirmation
        if (!Validators.doPasswordsMatch(password, confirmPassword)) {
            Toasts.showValidationError(this, Validators.getPasswordMismatchErrorMessage())
            etConfirmPassword.requestFocus()
            return false
        }
        
        return true
    }
    
    private fun navigateToDashboard() {
        val intent = Intent(this, OwnerDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}
