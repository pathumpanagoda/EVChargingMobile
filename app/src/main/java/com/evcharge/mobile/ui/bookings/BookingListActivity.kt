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
import com.evcharge.mobile.common.isSuccess
import com.evcharge.mobile.data.api.ApiClient
import com.evcharge.mobile.data.api.BookingApi
import com.evcharge.mobile.data.dto.Booking
import com.evcharge.mobile.data.model.BookingStatus
import com.evcharge.mobile.data.model.toBookingStatus
import com.evcharge.mobile.data.repo.BookingRepository
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
        prefs = Prefs.instance()
        bookingRepository = BookingRepository()
        
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
        
        val ownerNic = prefs.getNic()
        
        lifecycleScope.launch {
            try {
                // Load upcoming bookings
                val upcomingResult = bookingRepository.getUpcomingBookings(ownerNic ?: "")
                if (upcomingResult.isSuccess()) {
                    upcomingBookings = upcomingResult.getDataOrNull() ?: emptyList()
                }
                
                // Load history bookings
                val historyResult = bookingRepository.getBookingHistory(ownerNic ?: "")
                if (historyResult.isSuccess()) {
                    historyBookings = historyResult.getDataOrNull() ?: emptyList()
                }
                
                updateDisplay()
                
            } catch (e: Exception) {
                Toasts.showError(this@BookingListActivity, "Failed to load bookings: ${e.message}")
            } finally {
                loadingView.hide()
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
        private val tvStatus: MaterialTextView = itemView.findViewById(R.id.tv_status)
        private val tvDuration: MaterialTextView = itemView.findViewById(R.id.tv_duration)
        
        fun bind(booking: Booking, onBookingClick: (Booking) -> Unit) {
            tvStationName.text = booking.stationName ?: "Unknown Station"
            tvDateTime.text = booking.startTime ?: "N/A"
            tvStatus.text = booking.status
            tvDuration.text = formatDuration(booking.startTime, booking.endTime)
            
            // Set status color
            val statusColor = when (booking.status.toBookingStatus()) {
                BookingStatus.Pending -> R.color.status_pending
                BookingStatus.Approved -> R.color.status_approved
                BookingStatus.Completed -> R.color.status_completed
                BookingStatus.Cancelled -> R.color.status_cancelled
                BookingStatus.Unknown -> R.color.status_pending
            }
            tvStatus.setTextColor(itemView.context.getColor(statusColor))
            
            itemView.setOnClickListener {
                onBookingClick(booking)
            }
        }
        
        private fun formatDateTime(timestamp: Long): String {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
        
        private fun formatDuration(startTime: String?, endTime: String?): String {
            if (startTime == null || endTime == null) return "N/A"
            val start = com.evcharge.mobile.common.TimeExt.isoToMillisOrNull(startTime) ?: return "N/A"
            val end = com.evcharge.mobile.common.TimeExt.isoToMillisOrNull(endTime) ?: return "N/A"
            val duration = end - start
            val hours = duration / (1000 * 60 * 60)
            val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
            return "${hours}h ${minutes}m"
        }
    }
}
