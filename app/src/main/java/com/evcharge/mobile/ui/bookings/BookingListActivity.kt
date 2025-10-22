package com.evcharge.mobile.ui.bookings

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evcharge.mobile.R
import com.evcharge.mobile.common.Prefs
import com.evcharge.mobile.common.Toasts
import com.evcharge.mobile.common.getDataOrNull
import com.evcharge.mobile.common.getErrorOrNull
import com.evcharge.mobile.common.isSuccess
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.BookingApi
import com.evcharge.mobile.data.api.StationApi
import com.evcharge.mobile.data.dto.Booking
import com.evcharge.mobile.data.dto.BookingStatus
import com.evcharge.mobile.data.repo.BookingRepository
import com.evcharge.mobile.data.repo.StationRepository
import com.evcharge.mobile.ui.widgets.LoadingView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * Booking list activity showing upcoming and history
 */
class BookingListActivity : AppCompatActivity() {
    
    private lateinit var prefs: Prefs
    private lateinit var bookingRepository: BookingRepository
    private lateinit var stationRepository: StationRepository
    
    // UI Components
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: MaterialTextView
    private lateinit var fabNewBooking: FloatingActionButton
    private lateinit var loadingView: LoadingView
    
    private lateinit var adapter: BookingAdapter
    private var currentTab = 0 // 0 = Upcoming, 1 = History
    private var upcomingBookings: List<Booking> = emptyList()
    private var historyBookings: List<Booking> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_list)
        
        try {
            initializeComponents()
            setupUI()
            setupClickListeners()
            loadBookings()
        } catch (e: Exception) {
            Toasts.showError(this, "Booking list initialization failed: ${e.message}")
            finish()
        }
    }
    
    private fun initializeComponents() {
        try {
            prefs = Prefs(this)
            val apiClient = ApiClient(prefs)
            val bookingApi = BookingApi(apiClient)
            val stationApi = StationApi(apiClient)
            bookingRepository = BookingRepository(bookingApi)
            stationRepository = StationRepository(stationApi)
            
            // Initialize UI components
            tabLayout = findViewById(R.id.tab_layout)
            recyclerView = findViewById(R.id.recycler_view)
            emptyView = findViewById(R.id.empty_view)
            fabNewBooking = findViewById(R.id.fab_new_booking)
            loadingView = findViewById(R.id.loading_view)
            
            // Setup adapter
            adapter = BookingAdapter(stationRepository) { booking ->
                openBookingDetail(booking)
            }
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter
        } catch (e: Exception) {
            Toasts.showError(this, "Component initialization failed: ${e.message}")
            throw e
        }
    }
    
    private fun setupUI() {
        try {
            // Set up toolbar with error handling for action bar conflict
            try {
                setSupportActionBar(findViewById(R.id.toolbar))
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "My Bookings"
            } catch (e: Exception) {
                // Handle action bar conflict gracefully
                Toasts.showWarning(this, "Toolbar setup skipped: ${e.message}")
            }
            
            // Setup tabs
            tabLayout.addTab(tabLayout.newTab().setText("Upcoming"))
            tabLayout.addTab(tabLayout.newTab().setText("History"))
        } catch (e: Exception) {
            Toasts.showError(this, "UI setup failed: ${e.message}")
            throw e
        }
    }
    
    private fun setupClickListeners() {
        try {
            // Tab selection
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    currentTab = tab?.position ?: 0
                    updateDisplay()
                }
                
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            
            // New booking FAB
            fabNewBooking.setOnClickListener {
                try {
                    Toasts.showInfo(this, "Opening New Booking...")
                    startActivity(Intent(this, BookingFormActivity::class.java))
                } catch (e: Exception) {
                    Toasts.showError(this, "Failed to open New Booking: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Toasts.showError(this, "Click listeners setup failed: ${e.message}")
            throw e
        }
    }
    
    private fun loadBookings() {
        loadingView.show()
        loadingView.setMessage("Loading bookings...")
        
        val ownerNic = prefs.getNIC()
        
        android.util.Log.d("BookingListActivity", "Loading bookings for NIC: $ownerNic")
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Load upcoming bookings
                val upcomingResult = bookingRepository.getUpcomingBookings(ownerNic)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (upcomingResult.isSuccess()) {
                        upcomingBookings = upcomingResult.getDataOrNull() ?: emptyList()
                        Toasts.showInfo(this@BookingListActivity, "Loaded ${upcomingBookings.size} upcoming bookings")
                    } else {
                        val error = upcomingResult.getErrorOrNull()
                        Toasts.showWarning(this@BookingListActivity, "Failed to load upcoming: ${error?.message}")
                    }
                }
                
                // Load history bookings
                val historyResult = bookingRepository.getBookingHistory(ownerNic)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (historyResult.isSuccess()) {
                        historyBookings = historyResult.getDataOrNull() ?: emptyList()
                        Toasts.showInfo(this@BookingListActivity, "Loaded ${historyBookings.size} history bookings")
                    } else {
                        val error = historyResult.getErrorOrNull()
                        Toasts.showWarning(this@BookingListActivity, "Failed to load history: ${error?.message}")
                    }
                    
                    updateDisplay()
                }
                
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toasts.showError(this@BookingListActivity, "Failed to load bookings: ${e.message}")
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    loadingView.hide()
                }
            }
        }
    }
    
    private fun updateDisplay() {
        val bookings = if (currentTab == 0) upcomingBookings else historyBookings
        
        if (bookings.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.text = if (currentTab == 0) "No upcoming bookings" else "No booking history"
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            adapter.updateBookings(bookings)
        }
    }
    
    private fun openBookingDetail(booking: Booking) {
        val intent = Intent(this, BookingDetailActivity::class.java)
        intent.putExtra("booking_id", booking.id)
        startActivity(intent)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_booking_list, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_refresh -> {
                loadBookings()
                true
            }
            R.id.action_new_booking -> {
                startActivity(Intent(this, BookingFormActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh bookings when returning to this activity
        loadBookings()
    }
}

/**
 * RecyclerView adapter for booking list
 */
class BookingAdapter(
    private val stationRepository: StationRepository,
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {
    
    private var bookings: List<Booking> = emptyList()
    private val stationCache = mutableMapOf<String, String>() // stationId -> stationName
    
    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        // Pre-fetch station names for all bookings
        fetchStationNames()
        notifyDataSetChanged()
    }
    
    private fun fetchStationNames() {
        val uniqueStationIds = bookings.map { it.stationId }.distinct()
        uniqueStationIds.forEach { stationId ->
            if (!stationCache.containsKey(stationId)) {
                // Fetch station name asynchronously
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        val result = stationRepository.getStation(stationId)
                        if (result.isSuccess()) {
                            val station = result.getDataOrNull()
                            station?.let {
                                stationCache[stationId] = it.name
                                // Notify adapter to refresh the view
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    notifyDataSetChanged()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("BookingAdapter", "Failed to fetch station name for $stationId: ${e.message}")
                    }
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): BookingViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        val stationName = stationCache[booking.stationId]
        holder.bind(booking, stationName, onBookingClick)
    }
    
    override fun getItemCount(): Int = bookings.size
    
    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStationName: MaterialTextView = itemView.findViewById(R.id.tv_station_name)
        private val tvDateTime: MaterialTextView = itemView.findViewById(R.id.tv_date_time)
        private val tvStatus: com.google.android.material.chip.Chip = itemView.findViewById(R.id.tv_status)
        private val tvDuration: MaterialTextView = itemView.findViewById(R.id.tv_duration)
        
        fun bind(booking: Booking, stationName: String?, onBookingClick: (Booking) -> Unit) {
            // Display station name and ID
            val displayName = if (!stationName.isNullOrEmpty()) {
                "$stationName (ID: ${booking.stationId.take(8)}...)"
            } else {
                "Station ID: ${booking.stationId.take(8)}..."
            }
            tvStationName.text = displayName
            
            tvDateTime.text = formatDateTime(booking.startTime)
            tvDuration.text = formatDuration(booking.startTime, booking.endTime)
            
            // Set status text and style
            val statusText = booking.status.name.uppercase()
            tvStatus.text = statusText
            android.util.Log.d("BookingAdapter", "Setting status: $statusText for booking ${booking.id}")
            when (booking.status) {
                BookingStatus.PENDING -> {
                    tvStatus.setChipBackgroundColorResource(R.color.status_pending)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.black))
                }
                BookingStatus.APPROVED -> {
                    tvStatus.setChipBackgroundColorResource(R.color.status_approved)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.white))
                }
                BookingStatus.COMPLETED -> {
                    tvStatus.setChipBackgroundColorResource(R.color.status_completed)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.white))
                }
                BookingStatus.CANCELLED -> {
                    tvStatus.setChipBackgroundColorResource(R.color.status_cancelled)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.white))
                }
            }
            
            itemView.setOnClickListener {
                onBookingClick(booking)
            }
        }
        
        private fun formatDateTime(timestamp: Long): String {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
        
        private fun formatDuration(startTime: Long, endTime: Long): String {
            val duration = endTime - startTime
            val hours = duration / (1000 * 60 * 60)
            val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
            return "${hours}h ${minutes}m"
        }
    }
}
