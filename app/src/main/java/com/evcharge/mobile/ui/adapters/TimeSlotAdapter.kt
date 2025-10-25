package com.evcharge.mobile.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.evcharge.mobile.R
import com.evcharge.mobile.R.color.material_dynamic_neutral95
import com.evcharge.mobile.data.dto.TimeSlot
import com.evcharge.mobile.databinding.ItemTimeSlotBinding

/**
 * Adapter for displaying time slots in a grid layout
 */
class TimeSlotAdapter(
    private var timeSlots: List<TimeSlot> = emptyList(),
    private var onSlotSelected: ((TimeSlot) -> Unit)? = null
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private var selectedSlot: TimeSlot? = null

    class TimeSlotViewHolder(private val binding: ItemTimeSlotBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(
            timeSlot: TimeSlot, 
            isSelected: Boolean, 
            onSlotClick: (TimeSlot) -> Unit
        ) {
            binding.tvTime.text = timeSlot.time
            binding.tvAvailability.text = timeSlot.getAvailabilityText()
            
            // Show/hide selection indicator
            binding.tvSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            // Show/hide full indicator
            binding.tvFull.visibility = if (timeSlot.isFullyBooked()) View.VISIBLE else View.GONE
            
            // Update card appearance based on availability and selection
            val cardView = binding.root
            when {
                isSelected -> {
                    // Selected state
                    cardView.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.material_dynamic_primary95)
                    )
                    cardView.strokeColor = ContextCompat.getColor(binding.root.context, R.color.material_dynamic_primary70)
                    cardView.strokeWidth = 2
                }
                timeSlot.isFullyBooked() -> {
                    // Fully booked state
                    cardView.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, material_dynamic_neutral95)
                    )
                    cardView.strokeColor = ContextCompat.getColor(binding.root.context, R.color.material_dynamic_neutral80)
                    cardView.strokeWidth = 1
                }
                else -> {
                    // Available state
                    cardView.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.material_dynamic_neutral99)
                    )
                    cardView.strokeColor = ContextCompat.getColor(binding.root.context, R.color.material_dynamic_neutral90)
                    cardView.strokeWidth = 1
                }
            }
            
            // Set click listener
            cardView.setOnClickListener {
                if (timeSlot.available && !timeSlot.isFullyBooked()) {
                    onSlotClick(timeSlot)
                }
            }
            
            // Disable interaction for fully booked slots
            cardView.isClickable = timeSlot.available && !timeSlot.isFullyBooked()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val binding = ItemTimeSlotBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return TimeSlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        val isSelected = selectedSlot?.time == timeSlot.time
        holder.bind(timeSlot, isSelected) { slot ->
            selectSlot(slot)
        }
    }

    override fun getItemCount(): Int = timeSlots.size

    /**
     * Update the list of time slots
     */
    fun updateTimeSlots(newTimeSlots: List<TimeSlot>) {
        timeSlots = newTimeSlots
        notifyDataSetChanged()
    }

    /**
     * Select a time slot
     */
    private fun selectSlot(slot: TimeSlot) {
        if (slot.available && !slot.isFullyBooked()) {
            selectedSlot = slot
            onSlotSelected?.invoke(slot)
            notifyDataSetChanged()
        }
    }

    /**
     * Get the currently selected slot
     */
    fun getSelectedSlot(): TimeSlot? = selectedSlot

    /**
     * Clear selection
     */
    fun clearSelection() {
        selectedSlot = null
        notifyDataSetChanged()
    }
}
