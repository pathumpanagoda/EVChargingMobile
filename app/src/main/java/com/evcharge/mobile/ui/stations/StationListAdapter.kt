package com.evcharge.mobile.ui.stations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.evcharge.mobile.R
import com.evcharge.mobile.data.dto.Station
import com.evcharge.mobile.data.dto.StationStatus
import com.google.android.material.card.MaterialCardView

/**
 * Adapter for displaying charging stations in a list
 */
class StationListAdapter(
    private val onStationClick: (Station) -> Unit
) : RecyclerView.Adapter<StationListAdapter.StationViewHolder>() {
    
    private var stations: List<Station> = emptyList()
    
    fun updateStations(newStations: List<Station>) {
        stations = newStations
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_station, parent, false)
        return StationViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.bind(stations[position])
    }
    
    override fun getItemCount(): Int = stations.size
    
    inner class StationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_view)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvAddress: TextView = itemView.findViewById(R.id.tv_address)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvPorts: TextView = itemView.findViewById(R.id.tv_ports)
        private val tvDistance: TextView = itemView.findViewById(R.id.tv_distance)
        
        fun bind(station: Station) {
            tvName.text = station.name
            tvAddress.text = station.address
            tvStatus.text = station.status.name
            tvPorts.text = "Ports: ${station.maxCapacity - station.currentOccupancy}/${station.maxCapacity}"
            tvDistance.text = "Rate: ${station.chargingRate}kW"
            
            // Set status color
            val statusColor = when (station.status) {
                StationStatus.AVAILABLE -> android.graphics.Color.GREEN
                StationStatus.OCCUPIED -> android.graphics.Color.RED
                StationStatus.MAINTENANCE -> android.graphics.Color.parseColor("#FF9800") // Orange
                StationStatus.OFFLINE -> android.graphics.Color.GRAY
            }
            tvStatus.setTextColor(statusColor)
            
            // Set card click listener
            cardView.setOnClickListener {
                onStationClick(station)
            }
        }
    }
}
