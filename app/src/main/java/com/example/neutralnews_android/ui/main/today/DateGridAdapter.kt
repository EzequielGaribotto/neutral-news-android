package com.example.neutralnews_android.ui.main.today

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.neutralnews_android.R
import java.util.*

/**
 * Adapter para mostrar una cuadrícula de días en un mes.
 * Cada celda puede estar habilitada (hay noticias) o deshabilitada (sin noticias).
 * Permite selección múltiple de días y muestra un fondo circular naranja cuando está seleccionado.
 */
class DateGridAdapter(
    private var items: List<DayItem>,
    private val onDayClick: (DayItem) -> Unit
) : RecyclerView.Adapter<DateGridAdapter.DayViewHolder>() {

    fun updateItems(newItems: List<DayItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_day, parent, false)
        return DayViewHolder(v)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            if (!item.isAvailable) return@setOnClickListener
            // Toggle selection without causing full layout recalculation
            item.isSelected = !item.isSelected
            notifyItemChanged(position)
            onDayClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val flContainer: View = itemView.findViewById(R.id.flContainer)

        fun bind(item: DayItem) {
            if (item.day == 0) {
                tvDay.text = ""
                tvDay.isEnabled = false
                flContainer.background = null
                return
            }
            tvDay.text = item.day.toString()

            if (!item.isAvailable) {
                tvDay.alpha = 1f
                tvDay.setTextColor(itemView.context.resources.getColor(R.color.black, null))
                flContainer.setBackgroundResource(R.drawable.ic_bg_circle_gray_border)
            } else {
                tvDay.alpha = 1f
                if (item.isSelected) {
                    flContainer.setBackgroundResource(R.drawable.ic_bg_circle_orange_filled)
                    tvDay.setTextColor(itemView.context.resources.getColor(R.color.white, null))
                } else {
                    flContainer.setBackgroundResource(R.drawable.ic_bg_circle_orange_border)
                    tvDay.setTextColor(itemView.context.resources.getColor(R.color.black10, null))
                }
            }
        }
    }
}

// Modelo para cada día en la grilla
data class DayItem(
    val year: Int,
    val month: Int,
    val day: Int, // 0 significa placeholder (día vacío al inicio del mes)
    var isAvailable: Boolean = true, // si hay noticias
    var isSelected: Boolean = false
) {
    fun toDate(): Date? {
        if (day == 0) return null
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}
