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
        
        initializeComponents()
        setupUI()
        setupClickListeners()
        loadDashboardData()
    }
    
    private fun initializeComponents() {
        prefs = Prefs(this)
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
    }
    
    private fun setupUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        
        // Load owner name
        val ownerNic = prefs.getNIC()
        val localOwner = ownerRepository.getLocalOwner(ownerNic)
        if (localOwner != null && localOwner.name.isNotEmpty()) {
            tvOwnerName.text = localOwner.name
        } else {
            tvOwnerName.text = "EV Owner"
        }
    }
    
    private fun setupClickListeners() {
        // New reservation button
        btnNewReservation.setOnClickListener {
            startActivity(Intent(this, BookingFormActivity::class.java))
        }
        
        // My bookings button
        btnMyBookings.setOnClickListener {
            startActivity(Intent(this, BookingListActivity::class.java))
        }
        
        // Profile button
        btnProfile.setOnClickListener {
            startActivity(Intent(this, OwnerProfileActivity::class.java))
        }
        
        // View map button
        btnViewMap.setOnClickListener {
            startActivity(Intent(this, StationMapActivity::class.java))
        }
    }
    
    private fun loadDashboardData() {
        loadingView.show()
        loadingView.setMessage("Loading dashboard...")
        
        val ownerNic = prefs.getNIC()
        
        lifecycleScope.launch {
            try {
                val result = bookingRepository.getDashboardStats(ownerNic)
                
                if (result.isSuccess()) {
                    val stats = result.getDataOrNull() ?: DashboardStats()
                    updateDashboardStats(stats)
                } else {
                    val error = result.getErrorOrNull()
                    Toasts.showError(this@OwnerDashboardActivity, error?.message ?: "Failed to load dashboard")
                }
            } catch (e: Exception) {
                Toasts.showError(this@OwnerDashboardActivity, "Failed to load dashboard: ${e.message}")
            } finally {
                loadingView.hide()
            }
        }
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
