package com.evcharge.mobile.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.evcharge.mobile.App
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.Toasts
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.getErrorOrNull
import com.evcharge.mobile.common.isSuccess
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.AuthApi
import com.evcharge.mobile.data.api.BookingApi
import com.evcharge.mobile.data.api.OwnerApi
import com.evcharge.mobile.data.db.OwnerDao
import com.evcharge.mobile.data.dto.DashboardStats
import com.evcharge.mobile.data.repo.AuthRepository
import com.evcharge.mobile.data.repo.BookingRepository
import com.evcharge.mobile.data.repo.OwnerRepository
import com.evcharge.mobile.ui.auth.AuthActivity
import com.evcharge.mobile.ui.bookings.BookingFormActivity
import com.evcharge.mobile.ui.bookings.BookingListActivity
import com.evcharge.mobile.ui.owners.OwnerProfileActivity
import com.evcharge.mobile.ui.stations.StationMapActivity
import com.evcharge.mobile.ui.widgets.LoadingView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Owner dashboard activity
 */
class OwnerDashboardActivity : AppCompatActivity() {
    
    private lateinit var prefs: Prefs
    private lateinit var authRepository: AuthRepository
    private lateinit var bookingRepository: BookingRepository
    private lateinit var ownerRepository: OwnerRepository
    
    // UI Components
    private lateinit var tvWelcome: MaterialTextView
    private lateinit var tvOwnerName: MaterialTextView
    private lateinit var tvPendingCount: MaterialTextView
    private lateinit var tvApprovedCount: MaterialTextView
    private lateinit var btnNewReservation: MaterialButton
    private lateinit var btnMyBookings: MaterialButton
    private lateinit var btnProfile: MaterialButton
    private lateinit var btnViewMap: MaterialButton
    private lateinit var loadingView: LoadingView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_dashboard)
        
        try {
            // Initialize prefs first
            prefs = Prefs(this)
            
            // Check if user is properly authenticated
            android.util.Log.d("OwnerDashboard", "Checking authentication - isLoggedIn: ${prefs.isLoggedIn()}, hasValidSession: ${prefs.hasValidSession()}")
            
            if (!prefs.isLoggedIn()) {
                android.util.Log.w("OwnerDashboard", "User not logged in, redirecting to auth")
                Toasts.showError(this, "Please login first.")
                val intent = Intent(this, com.evcharge.mobile.ui.auth.AuthActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
                return
            }
            
            initializeComponents()
            setupUI()
            setupClickListeners()
            loadDashboardData()
        } catch (e: Exception) {
            android.util.Log.e("OwnerDashboard", "Error in onCreate", e)
            // Don't show error to user, just log it
            android.util.Log.e("OwnerDashboard", "onCreate error: ${e.message}")
        }
    }
    
    private fun initializeComponents() {
        try {
            val apiClient = ApiClient(prefs)
            val authApi = AuthApi(apiClient)
            val bookingApi = BookingApi(apiClient)
            val ownerApi = OwnerApi(apiClient)
            val ownerDao = OwnerDao(App.instance.dbHelper)
            
            authRepository = AuthRepository(authApi, ownerDao, prefs)
            bookingRepository = BookingRepository(bookingApi)
            ownerRepository = OwnerRepository(ownerApi, ownerDao)
            
            // Initialize UI components
            tvWelcome = findViewById(R.id.tv_welcome)
            tvOwnerName = findViewById(R.id.tv_owner_name)
            tvPendingCount = findViewById(R.id.tv_pending_count)
            tvApprovedCount = findViewById(R.id.tv_approved_count)
            btnNewReservation = findViewById(R.id.btn_new_reservation)
            btnMyBookings = findViewById(R.id.btn_my_bookings)
            btnProfile = findViewById(R.id.btn_profile)
            btnViewMap = findViewById(R.id.btn_view_map)
            loadingView = findViewById(R.id.loading_view)
            
            android.util.Log.d("OwnerDashboard", "Components initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("OwnerDashboard", "Failed to initialize components", e)
            // Don't show error message to user, just log it
            android.util.Log.e("OwnerDashboard", "Component initialization error: ${e.message}")
        }
    }
    
    private fun setupUI() {
        try {
            // Set up toolbar only if it doesn't already exist
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            if (toolbar != null && supportActionBar == null) {
                setSupportActionBar(toolbar)
                android.util.Log.d("OwnerDashboard", "Toolbar set successfully")
            } else {
                android.util.Log.d("OwnerDashboard", "Toolbar already exists or not found")
            }
            
            // Load owner name
            val ownerNic = prefs.getNIC()
            val localOwner = ownerRepository.getLocalOwner(ownerNic)
            if (localOwner != null && localOwner.name.isNotEmpty()) {
                tvOwnerName.text = localOwner.name
            } else {
                tvOwnerName.text = "EV Owner"
            }
            
            android.util.Log.d("OwnerDashboard", "UI setup completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("OwnerDashboard", "Failed to setup UI", e)
            // Don't show error message to user, just log it
            android.util.Log.e("OwnerDashboard", "UI setup error: ${e.message}")
        }
    }
    
    private fun setupClickListeners() {
        try {
            // New reservation button
            btnNewReservation.setOnClickListener {
                try {
                    android.util.Log.d("OwnerDashboard", "New reservation button clicked")
                    Toasts.showInfo(this, "Opening booking form...")
                    
                    // Test if we can create the intent first
                    val intent = Intent(this, BookingFormActivity::class.java)
                    android.util.Log.d("OwnerDashboard", "Intent created successfully")
                    
                    // Try to start the activity
                    startActivity(intent)
                    android.util.Log.d("OwnerDashboard", "BookingFormActivity started successfully")
                } catch (e: Exception) {
                    android.util.Log.e("OwnerDashboard", "Failed to start BookingFormActivity", e)
                    android.util.Log.e("OwnerDashboard", "Exception details: ${e.javaClass.simpleName} - ${e.message}")
                    android.util.Log.e("OwnerDashboard", "Stack trace: ${e.stackTraceToString()}")
                    Toasts.showError(this, "Failed to open booking form: ${e.message}")
                }
            }
            
            // My bookings button
            btnMyBookings.setOnClickListener {
                try {
                    android.util.Log.d("OwnerDashboard", "My bookings button clicked")
                    Toasts.showInfo(this, "Opening bookings list...")
                    val intent = Intent(this, BookingListActivity::class.java)
                    startActivity(intent)
                    android.util.Log.d("OwnerDashboard", "BookingListActivity started successfully")
                } catch (e: Exception) {
                    android.util.Log.e("OwnerDashboard", "Failed to start BookingListActivity", e)
                    Toasts.showError(this, "Failed to open bookings: ${e.message}")
                }
            }
            
            // Profile button
            btnProfile.setOnClickListener {
                try {
                    android.util.Log.d("OwnerDashboard", "Profile button clicked")
                    Toasts.showInfo(this, "Opening profile...")
                    val intent = Intent(this, OwnerProfileActivity::class.java)
                    startActivity(intent)
                    android.util.Log.d("OwnerDashboard", "OwnerProfileActivity started successfully")
                } catch (e: Exception) {
                    android.util.Log.e("OwnerDashboard", "Failed to start OwnerProfileActivity", e)
                    Toasts.showError(this, "Failed to open profile: ${e.message}")
                }
            }
            
            // View map button
            btnViewMap.setOnClickListener {
                try {
                    android.util.Log.d("OwnerDashboard", "View map button clicked")
                    Toasts.showInfo(this, "Opening station map...")
                    val intent = Intent(this, StationMapActivity::class.java)
                    startActivity(intent)
                    android.util.Log.d("OwnerDashboard", "StationMapActivity started successfully")
                } catch (e: Exception) {
                    android.util.Log.e("OwnerDashboard", "Failed to start StationMapActivity", e)
                    Toasts.showError(this, "Failed to open map: ${e.message}")
                }
            }
            
            android.util.Log.d("OwnerDashboard", "Click listeners setup completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("OwnerDashboard", "Failed to setup click listeners", e)
            Toasts.showError(this, "Failed to setup buttons: ${e.message}")
        }
    }
    
    private fun loadDashboardData() {
        loadingView.show()
        loadingView.setMessage("Loading dashboard...")
        
        val ownerNic = prefs.getNIC()
        
        lifecycleScope.launch {
            try {
                // Try to get data from backend first
                val result = bookingRepository.getDashboardStats(ownerNic)
                
                if (result.isSuccess()) {
                    val stats = result.getDataOrNull() ?: DashboardStats()
                    updateDashboardStats(stats)
                    Toasts.showSuccess(this@OwnerDashboardActivity, "Connected to backend successfully!")
                } else {
                    // If backend fails, use mock data instead of showing error
                    loadMockDashboardData()
                }
            } catch (e: Exception) {
                // If any exception occurs, use mock data
                loadMockDashboardData()
            } finally {
                loadingView.hide()
            }
        }
    }
    
    private fun loadMockDashboardData() {
        // Create mock data for offline mode
        val mockStats = DashboardStats(
            pendingCount = 2,
            approvedCount = 5
        )
        updateDashboardStats(mockStats)
        
        // Show info message about backend connectivity
        Toasts.showInfo(this, "Backend server not available. Using demo data.")
    }
    
    private fun updateDashboardStats(stats: DashboardStats) {
        tvPendingCount.text = stats.pendingCount.toString()
        tvApprovedCount.text = stats.approvedCount.toString()
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
            R.id.action_refresh -> {
                loadDashboardData()
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
    
    override fun onResume() {
        super.onResume()
        // Refresh dashboard data when returning to this activity
        loadDashboardData()
    }
}
