package com.evcharge.mobile.ui.owners

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.Toasts
import com.evcharge.mobile.common.Validators
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.AuthApi
import com.evcharge.mobile.data.api.OwnerApi
import com.evcharge.mobile.data.db.OwnerDao
import com.evcharge.mobile.data.dto.OwnerProfile
import com.evcharge.mobile.data.dto.OwnerUpdateRequest
import com.evcharge.mobile.data.repo.AuthRepository
import com.evcharge.mobile.data.repo.OwnerRepository
import com.evcharge.mobile.ui.auth.AuthActivity
import com.evcharge.mobile.ui.widgets.LoadingView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.evcharge.mobile.App
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.getErrorOrNull
import com.evcharge.mobile.common.isSuccess
import kotlinx.coroutines.launch

/**
 * Owner profile activity
 */
class OwnerProfileActivity : AppCompatActivity() {
    
    private lateinit var prefs: Prefs
    private lateinit var authRepository: AuthRepository
    private lateinit var ownerRepository: OwnerRepository
    
    // UI Components
    private lateinit var etNic: TextInputEditText
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnUpdate: MaterialButton
    private lateinit var btnDeactivate: MaterialButton
    private lateinit var loadingView: LoadingView
    
    private var isEditing = false
    private var ownerProfile: OwnerProfile? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_profile)
        
        try {
            initializeComponents()
            setupUI()
            setupClickListeners()
            loadProfile()
        } catch (e: Exception) {
            Toasts.showError(this, "Profile page initialization failed: ${e.message}")
            finish()
        }
    }
    
    private fun initializeComponents() {
        try {
            prefs = Prefs(this)
            val apiClient = ApiClient(prefs)
            val authApi = AuthApi(apiClient)
            val ownerApi = OwnerApi(apiClient)
            
            // Safely initialize database helper
            val dbHelper = try {
                App.instance.dbHelper
            } catch (e: Exception) {
                // Fallback: create a new instance if App.instance is not ready
                com.evcharge.mobile.data.db.UserDbHelper(this)
            }
            
            val ownerDao = OwnerDao(dbHelper)
            
            authRepository = AuthRepository(authApi, ownerDao, prefs)
            ownerRepository = OwnerRepository(ownerApi, ownerDao)
            
            // Initialize UI components
            etNic = findViewById(R.id.et_nic)
            etName = findViewById(R.id.et_name)
            etEmail = findViewById(R.id.et_email)
            etPhone = findViewById(R.id.et_phone)
            btnUpdate = findViewById(R.id.btn_update)
            btnDeactivate = findViewById(R.id.btn_deactivate)
            loadingView = findViewById(R.id.loading_view)
        } catch (e: Exception) {
            Toasts.showError(this, "Profile initialization failed: ${e.message}")
            finish()
        }
    }
    
    private fun setupUI() {
        try {
            // Set up toolbar with error handling for missing toolbar
            try {
                setSupportActionBar(findViewById(R.id.toolbar))
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Profile"
            } catch (e: Exception) {
                // Handle missing toolbar gracefully
                Toasts.showWarning(this, "Toolbar setup skipped: ${e.message}")
            }
            
            // Set NIC as non-editable
            etNic.isEnabled = false
        } catch (e: Exception) {
            Toasts.showError(this, "UI setup failed: ${e.message}")
            throw e
        }
    }
    
    private fun setupClickListeners() {
        try {
            // Update button
            btnUpdate.setOnClickListener {
                try {
                    if (isEditing) {
                        updateProfile()
                    } else {
                        enableEditing()
                    }
                } catch (e: Exception) {
                    Toasts.showError(this, "Update action failed: ${e.message}")
                }
            }
            
            // Deactivate button
            btnDeactivate.setOnClickListener {
                try {
                    showDeactivateConfirmation()
                } catch (e: Exception) {
                    Toasts.showError(this, "Deactivate action failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Toasts.showError(this, "Click listeners setup failed: ${e.message}")
            throw e
        }
    }
    
    private fun loadProfile() {
        loadingView.show()
        loadingView.setMessage("Loading profile...")
        
        val ownerNic = prefs.getNIC()
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Try to get from server first
                val result = ownerRepository.getOwner(ownerNic)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (result.isSuccess()) {
                        ownerProfile = result.getDataOrNull()
                        Toasts.showInfo(this@OwnerProfileActivity, "Profile loaded from server")
                    } else {
                        // Fallback to local database
                        Toasts.showWarning(this@OwnerProfileActivity, "Server unavailable, loading from local storage")
                        ownerProfile = ownerRepository.getLocalOwner(ownerNic)
                    }
                    
                    if (ownerProfile != null) {
                        updateUI()
                    } else {
                        // If no profile found anywhere, create a basic one from session data
                        createBasicProfile(ownerNic)
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showWarning(this@OwnerProfileActivity, "Backend error, trying local storage: ${e.message}")
                    try {
                        // Try local database as fallback
                        ownerProfile = ownerRepository.getLocalOwner(ownerNic)
                        if (ownerProfile != null) {
                            updateUI()
                        } else {
                            createBasicProfile(ownerNic)
                        }
                    } catch (e2: Exception) {
                        Toasts.showError(this@OwnerProfileActivity, "Failed to load profile: ${e2.message}")
                        createBasicProfile(ownerNic)
                    }
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loadingView.hide()
                }
            }
        }
    }
    
    private fun createBasicProfile(ownerNic: String) {
        try {
            // Create a basic profile with default values
            // Since we don't have name/email/phone in Prefs, we'll use defaults
            ownerProfile = com.evcharge.mobile.data.dto.OwnerProfile(
                nic = ownerNic,
                name = "User", // Default name
                email = "", // Empty email - user can fill it
                phone = "", // Empty phone - user can fill it
                active = true
            )
            
            updateUI()
            Toasts.showInfo(this, "Profile loaded with default values. Please update your information.")
        } catch (e: Exception) {
            Toasts.showError(this, "Failed to create basic profile: ${e.message}")
        }
    }
    
    private fun updateUI() {
        val profile = ownerProfile ?: return
        
        etNic.setText(profile.nic)
        etName.setText(profile.name)
        etEmail.setText(profile.email)
        etPhone.setText(profile.phone)
        
        updateButtonStates()
    }
    
    private fun enableEditing() {
        isEditing = true
        etName.isEnabled = true
        etEmail.isEnabled = true
        etPhone.isEnabled = true
        btnUpdate.text = "Save"
        btnDeactivate.visibility = android.view.View.GONE
    }
    
    private fun disableEditing() {
        isEditing = false
        etName.isEnabled = false
        etEmail.isEnabled = false
        etPhone.isEnabled = false
        btnUpdate.text = "Edit"
        btnDeactivate.visibility = android.view.View.VISIBLE
    }
    
    private fun updateButtonStates() {
        if (isEditing) {
            btnUpdate.text = "Save"
            btnDeactivate.visibility = android.view.View.GONE
        } else {
            btnUpdate.text = "Edit"
            btnDeactivate.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun updateProfile() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        
        // Validate input
        if (!Validators.isValidName(name)) {
            Toasts.showValidationError(this, Validators.getNameErrorMessage())
            etName.requestFocus()
            return
        }
        
        if (!Validators.isValidEmail(email)) {
            Toasts.showValidationError(this, Validators.getEmailErrorMessage())
            etEmail.requestFocus()
            return
        }
        
        if (!Validators.isValidPhone(phone)) {
            Toasts.showValidationError(this, Validators.getPhoneErrorMessage())
            etPhone.requestFocus()
            return
        }
        
        loadingView.show()
        loadingView.setMessage("Updating profile...")
        
        val request = OwnerUpdateRequest(name, email, phone)
        val ownerNic = prefs.getNIC()
        
        lifecycleScope.launch {
            try {
                val result = ownerRepository.updateOwner(ownerNic, request)
                
                if (result.isSuccess()) {
                    ownerProfile = result.getDataOrNull()
                    Toasts.showSuccess(this@OwnerProfileActivity, "Profile updated successfully")
                    disableEditing()
                } else {
                    val error = result.getErrorOrNull()
                    Toasts.showError(this@OwnerProfileActivity, error?.message ?: "Failed to update profile")
                }
            } catch (e: Exception) {
                Toasts.showError(this@OwnerProfileActivity, "Failed to update profile: ${e.message}")
            } finally {
                loadingView.hide()
            }
        }
    }
    
    private fun showDeactivateConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Deactivate Account")
            .setMessage("Are you sure you want to deactivate your account? You will not be able to make new bookings.")
            .setPositiveButton("Yes") { _, _ ->
                deactivateAccount()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun deactivateAccount() {
        loadingView.show()
        loadingView.setMessage("Deactivating account...")
        
        val ownerNic = prefs.getNIC()
        
        lifecycleScope.launch {
            try {
                val result = ownerRepository.deactivateOwner(ownerNic, "User requested deactivation")
                
                if (result.isSuccess()) {
                    Toasts.showSuccess(this@OwnerProfileActivity, "Account deactivated successfully")
                    
                    // Logout and return to login
                    authRepository.logout()
                    val intent = Intent(this@OwnerProfileActivity, AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val error = result.getErrorOrNull()
                    Toasts.showError(this@OwnerProfileActivity, error?.message ?: "Failed to deactivate account")
                }
            } catch (e: Exception) {
                Toasts.showError(this@OwnerProfileActivity, "Failed to deactivate account: ${e.message}")
            } finally {
                loadingView.hide()
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_owner_profile, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (isEditing) {
                    disableEditing()
                    loadProfile() // Reload original data
                } else {
                    finish()
                }
                true
            }
            R.id.action_refresh -> {
                loadProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        if (isEditing) {
            disableEditing()
            loadProfile() // Reload original data
        } else {
            super.onBackPressed()
        }
    }
}
