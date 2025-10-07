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
        
        initializeComponents()
        setupUI()
        setupClickListeners()
        loadBookings()
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
            adapter = BookingAdapter { booking ->
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
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Bookings"
        
        // Setup tabs
        tabLayout.addTab(tabLayout.newTab().setText("Upcoming"))
        tabLayout.addTab(tabLayout.newTab().setText("History"))
    }
    
    private fun setupClickListeners() {
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
            startActivity(Intent(this, BookingFormActivity::class.java))
        }
    }
    
    private fun loadBookings() {
        loadingView.show()
        loadingView.setMessage("Loading bookings...")
        
        val ownerNic = prefs.getNIC()
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Load upcoming bookings
                val upcomingResult = bookingRepository.getUpcomingBookings(ownerNic)
                if (upcomingResult.isSuccess()) {
                    val bookings = upcomingResult.getDataOrNull() ?: emptyList()
                    android.util.Log.d("BookingListActivity", "Raw upcoming bookings count: ${bookings.size}")
                    // Enhance bookings with station details
                    upcomingBookings = enhanceBookingsWithStationDetails(bookings)
                    android.util.Log.d("BookingListActivity", "Enhanced upcoming bookings count: ${upcomingBookings.size}")
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toasts.showInfo(this@BookingListActivity, "Loaded ${upcomingBookings.size} upcoming bookings")
                    }
                } else {
                    val error = upcomingResult.getErrorOrNull()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toasts.showWarning(this@BookingListActivity, "Failed to load upcoming: ${error?.message}")
                    }
                }
                
                // Load history bookings
                val historyResult = bookingRepository.getBookingHistory(ownerNic)
                if (historyResult.isSuccess()) {
                    val bookings = historyResult.getDataOrNull() ?: emptyList()
                    android.util.Log.d("BookingListActivity", "Raw history bookings count: ${bookings.size}")
                    // Enhance bookings with station details
                    historyBookings = enhanceBookingsWithStationDetails(bookings)
                    android.util.Log.d("BookingListActivity", "Enhanced history bookings count: ${historyBookings.size}")
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toasts.showInfo(this@BookingListActivity, "Loaded ${historyBookings.size} history bookings")
                        updateDisplay()
                    }
                } else {
                    val error = historyResult.getErrorOrNull()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toasts.showWarning(this@BookingListActivity, "Failed to load history: ${error?.message}")
                        updateDisplay()
                    }
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
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
    
    /**
     * Enhance booking data with station details
     */
    private suspend fun enhanceBookingsWithStationDetails(bookings: List<Booking>): List<Booking> {
        return try {
            val enhancedBookings = mutableListOf<Booking>()
            
            for (booking in bookings) {
                if (booking.stationId.isNotEmpty()) {
                    try {
                        val stationResult = stationRepository.getStation(booking.stationId)
                        if (stationResult.isSuccess()) {
                            val station = stationResult.getDataOrNull()
                            if (station != null) {
                                // Create enhanced booking with station details
                                val enhancedBooking = booking.copy(
                                    stationName = station.name,
                                    stationId = station.id
                                )
                                enhancedBookings.add(enhancedBooking)
                                android.util.Log.d("BookingListActivity", "Enhanced booking ${booking.id} with station: ${station.name}")
                            } else {
                                // Station not found, use original booking with fallback name
                                val enhancedBooking = booking.copy(
                                    stationName = "Station ${booking.stationId.take(8)}..."
                                )
                                enhancedBookings.add(enhancedBooking)
                                android.util.Log.w("BookingListActivity", "Station not found for ${booking.stationId}, using fallback name")
                            }
                        } else {
                            // Station API failed, use original booking with fallback name
                            val enhancedBooking = booking.copy(
                                stationName = "Station ${booking.stationId.take(8)}..."
                            )
                            enhancedBookings.add(enhancedBooking)
                            android.util.Log.w("BookingListActivity", "Station API failed for ${booking.stationId}, using fallback name")
                        }
                    } catch (e: Exception) {
                        // Network error, use original booking with fallback name
                        val enhancedBooking = booking.copy(
                            stationName = "Station ${booking.stationId.take(8)}..."
                        )
                        enhancedBookings.add(enhancedBooking)
                        android.util.Log.w("BookingListActivity", "Network error loading station ${booking.stationId}: ${e.message}")
                    }
                } else {
                    // No station ID, use original booking
                    enhancedBookings.add(booking)
                }
            }
            
            enhancedBookings
        } catch (e: Exception) {
            android.util.Log.e("BookingListActivity", "Failed to enhance bookings with station details: ${e.message}")
            bookings
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
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {
    
    private var bookings: List<Booking> = emptyList()
    
    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): BookingViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position], onBookingClick)
    }
    
    override fun getItemCount(): Int = bookings.size
    
    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStationName: MaterialTextView = itemView.findViewById(R.id.tv_station_name)
        private val tvDateTime: MaterialTextView = itemView.findViewById(R.id.tv_date_time)
        private val tvStatus: com.google.android.material.chip.Chip = itemView.findViewById(R.id.tv_status)
        private val tvDuration: MaterialTextView = itemView.findViewById(R.id.tv_duration)
        
        fun bind(booking: Booking, onBookingClick: (Booking) -> Unit) {
            tvStationName.text = booking.stationName ?: "Unknown Station"
            tvDateTime.text = formatDateTime(booking.startTime)
            tvStatus.text = booking.status.name
            tvDuration.text = formatDuration(booking.startTime, booking.endTime)
            
            // Set status color and style
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
