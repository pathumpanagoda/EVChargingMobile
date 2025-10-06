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
        
        initializeComponents()
        setupUI()
        setupClickListeners()
        loadProfile()
    }
    
    private fun initializeComponents() {
        prefs = Prefs.instance()
        authRepository = AuthRepository()
        ownerRepository = OwnerRepository()
        
        // Initialize UI components
        etNic = findViewById(R.id.et_nic)
        etName = findViewById(R.id.et_name)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        btnUpdate = findViewById(R.id.btn_update)
        btnDeactivate = findViewById(R.id.btn_deactivate)
        loadingView = findViewById(R.id.loading_view)
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"
        
        // Set NIC as non-editable
        etNic.isEnabled = false
    }
    
    private fun setupClickListeners() {
        // Update button
        btnUpdate.setOnClickListener {
            if (isEditing) {
                updateProfile()
            } else {
                enableEditing()
            }
        }
        
        // Deactivate button
        btnDeactivate.setOnClickListener {
            showDeactivateConfirmation()
        }
    }
    
    private fun loadProfile() {
        loadingView.show()
        loadingView.setMessage("Loading profile...")
        
        val ownerNic = prefs.getNic()
        
        lifecycleScope.launch {
            try {
                // Try to get from server first
                val result = ownerRepository.getOwner(ownerNic ?: "")
                
                if (result.isSuccess()) {
                    ownerProfile = result.getDataOrNull()
                } else {
                    // Fallback to local database
                    ownerProfile = ownerRepository.getLocalOwner(ownerNic ?: "")
                }
                
                if (ownerProfile != null) {
                    updateUI()
                } else {
                    Toasts.showError(this@OwnerProfileActivity, "Failed to load profile")
                }
            } catch (e: Exception) {
                Toasts.showError(this@OwnerProfileActivity, "Failed to load profile: ${e.message}")
            } finally {
                loadingView.hide()
            }
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
        val ownerNic = prefs.getNic()
        
        lifecycleScope.launch {
            try {
                val result = ownerRepository.updateOwner(ownerNic ?: "", request)
                
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
        
        val ownerNic = prefs.getNic()
        
        lifecycleScope.launch {
            try {
                val result = ownerRepository.deactivateOwner(ownerNic ?: "", "User requested deactivation")
                
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
