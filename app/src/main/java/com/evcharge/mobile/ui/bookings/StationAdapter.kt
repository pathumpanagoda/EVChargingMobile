package com.evcharge.mobile.ui.bookings

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.evcharge.mobile.R
import com.evcharge.mobile.data.dto.Station
import com.evcharge.mobile.data.dto.StationStatus

/**
 * Custom adapter for station dropdown with enhanced display
 */
class StationAdapter(
    context: Context,
    private val stations: List<Station>
) : ArrayAdapter<Station>(context, 0, stations) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var filteredStations: List<Station> = stations

    override fun getCount(): Int = filteredStations.size

    override fun getItem(position: Int): Station? {
        return if (position < filteredStations.size) filteredStations[position] else null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_station_dropdown, parent, false)
        
        val station = getItem(position) ?: return view
        
        val tvStatus = view.findViewById<TextView>(R.id.tv_station_status)
        val tvName = view.findViewById<TextView>(R.id.tv_station_name)
        val tvAddress = view.findViewById<TextView>(R.id.tv_station_address)
        val tvPrice = view.findViewById<TextView>(R.id.tv_station_price)
        
        // Set status emoji
        val statusEmoji = when (station.status) {
            StationStatus.AVAILABLE -> "🟢"
            StationStatus.OCCUPIED -> "🟡"
            StationStatus.MAINTENANCE -> "🔧"
            StationStatus.OFFLINE -> "🔴"
        }
        tvStatus.text = statusEmoji
        
        // Set station name
        tvName.text = station.name
        
        // Set address
        tvAddress.text = station.address
        
        // Set price
        tvPrice.text = if (station.pricePerHour > 0) {
            "$${String.format("%.2f", station.pricePerHour)}/hr"
        } else {
            "Free"
        }
        
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                
                if (constraint.isNullOrEmpty()) {
                    filteredStations = stations
                } else {
                    val query = constraint.toString().lowercase()
                    filteredStations = stations.filter { station ->
                        station.name.lowercase().contains(query) ||
                        station.address.lowercase().contains(query)
                    }
                }
                
                results.values = filteredStations
                results.count = filteredStations.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                @Suppress("UNCHECKED_CAST")
                filteredStations = (results?.values as? List<Station>) ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}

