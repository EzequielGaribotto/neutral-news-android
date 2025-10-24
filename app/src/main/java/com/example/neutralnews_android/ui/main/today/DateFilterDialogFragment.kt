package com.example.neutralnews_android.ui.main.today

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.setFragmentResult
import com.example.neutralnews_android.R
import com.example.neutralnews_android.ui.main.filter.DateGridAdapter
import com.example.neutralnews_android.ui.main.filter.DayItem
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.flow.firstOrNull
import com.example.neutralnews_android.util.Prefs

/**
 * Diálogo que muestra un calendario mensual y permite seleccionar múltiples días.
 * Reutiliza `DateGridAdapter` del paquete filter.
 */
class DateFilterDialogFragment : DialogFragment() {

    var onApply: ((List<Date>) -> Unit)? = null
    private var appliedFired = false

    private lateinit var rvCalendar: RecyclerView
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrev: ImageView
    private lateinit var btnNext: ImageView
    private lateinit var btnApply: TextView

    private var adapter: DateGridAdapter? = null
    private var currentYear = 0
    private var currentMonth = 0 // 1..12
    private val items = mutableListOf<DayItem>()
    private val tempSelectedDates = mutableSetOf<Date>()

    private val vm: TodayFragmentVM by activityViewModels()

    companion object {
        private const val ARG_INITIAL = "arg_initial"

        fun newInstance(initialDatesMillis: LongArray): DateFilterDialogFragment {
            val dlg = DateFilterDialogFragment()
            val b = Bundle()
            b.putLongArray(ARG_INITIAL, initialDatesMillis)
            dlg.arguments = b
            return dlg
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cal = Calendar.getInstance()
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH) + 1
        // cargar iniciales
        val arr = arguments?.getLongArray(ARG_INITIAL) ?: longArrayOf()
        for (m in arr) tempSelectedDates.add(Date(m))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val d = super.onCreateDialog(savedInstanceState)
        d.setCanceledOnTouchOutside(true)
        return d
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.dialog_date_filter, container, false)
        rvCalendar = v.findViewById(R.id.rvCalendar)
        tvMonthYear = v.findViewById(R.id.tvMonthYear)
        btnPrev = v.findViewById(R.id.btnPrevMonth)
        btnNext = v.findViewById(R.id.btnNextMonth)
        btnApply = v.findViewById(R.id.btnApplyDates)

        // Ensure dialog corners rounded and width fixed later in onStart
        dialog?.window?.setBackgroundDrawableResource(R.drawable.popup_background)

        adapter = DateGridAdapter(items) { day ->
            val date = day.toDate() ?: return@DateGridAdapter
            if (!day.isAvailable) return@DateGridAdapter
            // Adapter already toggles day.isSelected and calls notifyItemChanged
            // Only update the temporary selection set here
            if (tempSelectedDates.any { sameDay(it, date) }) {
                // remove same day
                val toRemove = tempSelectedDates.firstOrNull { sameDay(it, date) }
                if (toRemove != null) tempSelectedDates.remove(toRemove)
            } else {
                tempSelectedDates.add(date)
            }
        }

        rvCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        rvCalendar.adapter = adapter

        btnPrev.setOnClickListener {
            val prev = com.example.neutralnews_android.util.AppUtils.getPreviousMonthAndYear(currentYear, currentMonth)
            currentYear = prev.first
            currentMonth = prev.second
            refreshCalendar()
        }
        btnNext.setOnClickListener {
            val next = com.example.neutralnews_android.util.AppUtils.getNextMonthAndYear(currentYear, currentMonth)
            currentYear = next.first
            currentMonth = next.second
            refreshCalendar()
        }

        btnApply.setOnClickListener {
            val selectedList = tempSelectedDates.toList()
            onApply?.invoke(selectedList)
            appliedFired = true
            // also set fragment result with dates
            val arr = selectedList.map { it.time }.toLongArray()
            val bundle = Bundle()
            bundle.putBoolean("applied", true)
            bundle.putLongArray("dates", arr)
            setFragmentResult("date_filter_result", bundle)
            dismiss()
        }

        refreshCalendar()
        return v
    }

    override fun onStart() {
        super.onStart()
        // Set dialog width to 80% of screen and rounded corners
        dialog?.window?.let { w ->
            val metrics = resources.displayMetrics
            val width = (metrics.widthPixels * 0.8).toInt()
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (!appliedFired) {
            val bundle = Bundle()
            bundle.putBoolean("applied", false)
            setFragmentResult("date_filter_result", bundle)
        }
    }

    private fun refreshCalendar() {
        tvMonthYear.text = getMonthYearText(currentYear, currentMonth)
        items.clear()

        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offset = (firstDayOfWeek - cal.firstDayOfWeek + 7) % 7

        repeat(offset) { items.add(DayItem(currentYear, currentMonth, 0, isAvailable = false)) }

        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Initially mark all days as unavailable; will update after DB query
        for (d in 1..maxDay) {
            val it = DayItem(currentYear, currentMonth, d, isAvailable = false, isSelected = false)
            val dt = it.toDate()
            if (dt != null && tempSelectedDates.any { sel -> sameDay(sel, dt) }) {
                it.isSelected = true
                it.isAvailable = true // keep previously selected dates selectable
            }
            items.add(it)
        }

        while (items.size % 7 != 0) items.add(DayItem(currentYear, currentMonth, 0, isAvailable = false))

        // Update adapter immediately so UI shows the grid; days will appear disabled until DB check finishes
        adapter?.updateItems(items)

        // Now fetch available days from local DB asynchronously and update the items' availability
        lifecycleScope.launch {
            val available = withContext(Dispatchers.IO) { fetchAvailableDaysForMonth(currentYear, currentMonth) }
            if (available.isNotEmpty()) {
                for (i in items.indices) {
                    val dayItem = items[i]
                    if (dayItem.day != 0) {
                        // Keep selected days selectable even if they aren't in the newly computed available set
                        dayItem.isAvailable = dayItem.isSelected || available.contains(dayItem.day)
                    }
                }
                adapter?.updateItems(items)
            } else {
                // Check if DB is empty; if DB has no news at all, mark all days available to allow selection
                val dbHasAny = withContext(Dispatchers.IO) {
                    try {
                        val db = com.example.neutralnews_android.data.room.AppDatabase.getDatabase(requireContext())
                        val newsCount = db.newsDao().getAllNews().size
                        val neutralCount = db.neutralNewsDao().getAllNews().firstOrNull()?.size ?: 0
                        (newsCount + neutralCount) > 0
                    } catch (_: Exception) {
                        false
                    }
                }

                if (!dbHasAny) {
                    for (i in items.indices) {
                        val dayItem = items[i]
                        if (dayItem.day != 0) dayItem.isAvailable = true
                    }
                    adapter?.updateItems(items)
                } else {
                    // DB has entries but none in this month -> leave disabled; still set gray visuals
                    adapter?.updateItems(items)
                    // Also attach observers to retry when VM data updates (as before)
                    val obs1 = vm.neutralNewsList
                    val obs2 = vm.newsList
                    val listener = { _: Any? ->
                        lifecycleScope.launch {
                            val avail2 = withContext(Dispatchers.IO) { fetchAvailableDaysForMonth(currentYear, currentMonth) }
                            if (avail2.isNotEmpty()) {
                                for (i in items.indices) {
                                    val dayItem = items[i]
                                    if (dayItem.day != 0) {
                                        dayItem.isAvailable = dayItem.isSelected || avail2.contains(dayItem.day)
                                    }
                                }
                                adapter?.updateItems(items)
                            }
                        }
                    }
                    obs1.observe(viewLifecycleOwner) { listener(it) }
                    obs2.observe(viewLifecycleOwner) { listener(it) }
                }
            }
        }
    }

    // Query local DB (Room) for news timestamps in the given month/year, but prefer cached values in Prefs.
    private fun fetchAvailableDaysForMonth(year: Int, month: Int): Set<Int> {
        // Try cache first (fast)
        try {
            val key = "available_days_${year}_${month}"
            val csv = Prefs.getString(key, null)
            if (!csv.isNullOrEmpty()) {
                val parsed = csv.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
                if (parsed.isNotEmpty()) return parsed
            }
        } catch (_: Exception) {
            // ignore and fallback to DB
        }

        val set = mutableSetOf<Int>()
        try {
            val db = com.example.neutralnews_android.data.room.AppDatabase.getDatabase(requireContext())
            val newsDao = db.newsDao()
            val neutralDao = db.neutralNewsDao()

            // compute month start / end in millis (local timezone)
            val calStart = Calendar.getInstance()
            calStart.set(Calendar.YEAR, year)
            calStart.set(Calendar.MONTH, month - 1)
            calStart.set(Calendar.DAY_OF_MONTH, 1)
            calStart.set(Calendar.HOUR_OF_DAY, 0)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)
            val fromMillis = calStart.timeInMillis

            val calEnd = Calendar.getInstance()
            calEnd.set(Calendar.YEAR, year)
            calEnd.set(Calendar.MONTH, month - 1)
            calEnd.set(Calendar.DAY_OF_MONTH, calStart.getActualMaximum(Calendar.DAY_OF_MONTH))
            calEnd.set(Calendar.HOUR_OF_DAY, 23)
            calEnd.set(Calendar.MINUTE, 59)
            calEnd.set(Calendar.SECOND, 59)
            calEnd.set(Calendar.MILLISECOND, 999)
            val toMillis = calEnd.timeInMillis

            Log.d("DateFilterDlg", "query range from=$fromMillis to=$toMillis for $month/$year")

            val newsInRange = newsDao.getNewsBetween(fromMillis, toMillis)
            val neutralInRange = neutralDao.getNewsBetween(fromMillis, toMillis)

            Log.d("DateFilterDlg", "newsInRange=${newsInRange.size}, neutralInRange=${neutralInRange.size}")

            for (n in newsInRange) {
                val ts = if (n.pubDate > 0L) n.pubDate else if (n.createdAt > 0L) n.createdAt else n.updatedAt
                if (ts > 0L) {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    val d = cal.get(Calendar.DAY_OF_MONTH)
                    set.add(d)
                }
            }

            for (ne in neutralInRange) {
                // NeutralNewsEntity likely has 'date' or 'createdAt' fields; prefer 'date' then 'createdAt' then 'updatedAt'
                try {
                    val cls = ne::class
                    val dateField = cls.java.getDeclaredField("date")
                    dateField.isAccessible = true
                    val dateVal = dateField.getLong(ne)
                    if (dateVal > 0L) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = if (dateVal in 1..9999999999L) dateVal * 1000L else dateVal
                        set.add(cal.get(Calendar.DAY_OF_MONTH))
                    }
                } catch (_: Exception) {
                    // fallback try createdAt/updatedAt via reflection if present
                    try {
                        val createdField = ne::class.java.getDeclaredField("createdAt")
                        createdField.isAccessible = true
                        val createdVal = createdField.getLong(ne)
                        if (createdVal > 0L) {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = if (createdVal in 1..9999999999L) createdVal * 1000L else createdVal
                            set.add(cal.get(Calendar.DAY_OF_MONTH))
                        }
                    } catch (_: Exception) { }
                }
            }

            // cache result for future dialog openings (best-effort)
            try {
                val key = "available_days_${year}_${month}"
                val csv = set.sorted().joinToString(",")
                Prefs.putString(key, csv)
            } catch (_: Exception) { }

        } catch (_: Exception) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (d in 1..maxDay) set.add(d)
        }
        return set
    }

    private fun sameDay(a: Date, b: Date): Boolean {
        val ca = Calendar.getInstance().apply { time = a }
        val cb = Calendar.getInstance().apply { time = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH) && ca.get(Calendar.DAY_OF_MONTH) == cb.get(Calendar.DAY_OF_MONTH)
    }

    private fun getMonthYearText(year: Int, month: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        val fmt = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        // capitalize first letter (Spanish month names are lowercase by default)
        val raw = fmt.format(cal.time)
        return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
    }
}
