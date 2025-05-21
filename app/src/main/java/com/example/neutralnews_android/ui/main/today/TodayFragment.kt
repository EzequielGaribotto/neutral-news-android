package com.example.neutralnews_android.ui.main.today

import android.animation.ValueAnimator
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isGone
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neutralnews_android.BR
import com.example.neutralnews_android.R
import com.example.neutralnews_android.data.Constants.ApiObject.FILTER_COUNT
import com.example.neutralnews_android.data.Constants.ApiObject.FILTER_DATA
import com.example.neutralnews_android.data.Constants.DateFormat.DATE_FORMAT
import com.example.neutralnews_android.data.bean.filter.LocalFilterBean
import com.example.neutralnews_android.data.bean.news.NewsBean
import com.example.neutralnews_android.data.bean.settings.SettingsBean
import com.example.neutralnews_android.databinding.FragmentTodayBinding
import com.example.neutralnews_android.databinding.RowNewsBinding
import com.example.neutralnews_android.di.adapter.SimpleRecyclerViewAdapter
import com.example.neutralnews_android.di.view.AppFragment
import com.example.neutralnews_android.ui.main.filter.FilterActivity
import com.example.neutralnews_android.ui.main.news.NewDetailActivity
import com.example.neutralnews_android.util.loggerE
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


/**
 * Fragment that displays today's news with filtering and search capabilities.
 */
@AndroidEntryPoint
class TodayFragment : AppFragment() {

    private lateinit var binding: FragmentTodayBinding
    private val vm: TodayFragmentVM by activityViewModels()
    private var filterData: LocalFilterBean = LocalFilterBean()
    private var settings: SettingsBean = SettingsBean()
    private lateinit var newsAdapter: SimpleRecyclerViewAdapter<NewsBean, RowNewsBinding>
    private var isSearchActive = false
    private var isSettingText = false
    private var currentSortType: SortType = SortType.DATE_DESC

    // Enum para definir los tipos de filtro por fecha
    enum class DateFilterType {
        ALL, MULTI_SELECT
    }

    // Variable para mantener el filtro de fecha actual
    private var currentDateFilter: DateFilterType = DateFilterType.ALL
    // Mapa para almacenar las fechas de los días de la semana pasados
    private val pastDays = mutableMapOf<String, Date>()

    // Conjunto para almacenar las fechas seleccionadas por el usuario
    private val selectedDates = mutableSetOf<String>()

    // Enum to define sort types
    enum class SortType {
        DATE_DESC, DATE_ASC,
        UPDATED_AT_DESC, UPDATED_AT_ASC,
        RELEVANCE_DESC, RELEVANCE_ASC,
        SOURCES_DESC, SOURCES_ASC
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        if (!::binding.isInitialized) {
            initializeBinding(inflater, container)
        }

        loadInitialData()
        setupUI()
        setupObservers()
        updateFilterButtonVisibility()
        calculatePastDays() // Calcular fechas para el filtro
        return binding.root
    }

    // Función para calcular y almacenar las fechas de los últimos 7 días
    private fun calculatePastDays() {
        val calendar = Calendar.getInstance()
        val today = calendar.time

        // Guardar fecha de hoy
        pastDays["Hoy"] = today

        // Retroceder un día para ayer
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        pastDays["Ayer"] = calendar.time

        // Guardar los últimos 5 días con nombres de día
        val dayFormat = SimpleDateFormat("EEEE", Locale("es", "ES"))
        for (i in 2..6) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val dayName = dayFormat.format(calendar.time).capitalize(Locale.ROOT)
            pastDays[dayName] = calendar.time
        }
    }

    /**
     * Muestra el menú desplegable de filtro por fecha con opciones de selección múltiple.
     */
    private fun showDateFilterMenu(view: View) {
        val wrapper = ContextThemeWrapper(context, R.style.AppTheme_PopupOverlay)
        val popup = PopupMenu(wrapper, view)
        popup.menuInflater.inflate(R.menu.menu_date_filter, popup.menu)

        // Formatear fecha para mostrar en el menú
        val dateFormat = SimpleDateFormat("dd/MM", Locale("es", "ES"))

        val headerItem = popup.menu.findItem(R.id.menu_date_filter_header)
        if (headerItem != null) {
            // Configurar el color directamente
            headerItem.title = SpannableString(headerItem.title).apply {
                setSpan(ForegroundColorSpan(resources.getColor(R.color.orange, null)),
                    0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // Cambiar el comportamiento del menú para soportar selección múltiple
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            Log.e("TodayFragment", "Error configurando el menú: ${e.message}")
        }

        // Configurar etiquetas dinámicas para las fechas
        val todayItem = popup.menu.findItem(R.id.menu_date_today)
        todayItem.title = SpannableString("${dateFormat.format(pastDays["Hoy"])} - Hoy").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        todayItem.isCheckable = true
        todayItem.isChecked = selectedDates.contains("Hoy")

        val yesterdayItem = popup.menu.findItem(R.id.menu_date_yesterday)
        yesterdayItem.title = SpannableString("${dateFormat.format(pastDays["Ayer"])}").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        yesterdayItem.isCheckable = true
        yesterdayItem.isChecked = selectedDates.contains("Ayer")

        // Configurar días de la semana con sus fechas
        val dayItems = listOf(
            R.id.menu_date_day1,
            R.id.menu_date_day2,
            R.id.menu_date_day3,
            R.id.menu_date_day4,
            R.id.menu_date_day5
        )

        // Obtener nombres de días de la semana ordenados (excluyendo Hoy y Ayer)
        val dayNames = pastDays.keys.filter { it != "Hoy" && it != "Ayer" }.sortedByDescending {
            pastDays[it]?.time ?: 0
        }

        // Asignar nombres y fechas a los elementos del menú
        dayNames.forEachIndexed { index, dayName ->
            if (index < dayItems.size) {
                val item = popup.menu.findItem(dayItems[index])
                item.isVisible = true
                item.title = SpannableString("${dateFormat.format(pastDays[dayName])}").apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                item.isCheckable = true
                item.isChecked = selectedDates.contains(dayName)
            }
        }

        // Ocultar elementos no utilizados
        for (i in dayNames.size until dayItems.size) {
            popup.menu.findItem(dayItems[i]).isVisible = false
        }

        // Configurar item "Todas las fechas"
        val allItem = popup.menu.findItem(R.id.menu_date_all)
        allItem.title = SpannableString(allItem.title).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        allItem.isCheckable = true
        allItem.isChecked = selectedDates.isEmpty()

        // Ocultar opción "Más antiguas" ya que no es compatible con la selección múltiple
        popup.menu.findItem(R.id.menu_date_older).isVisible = false

        // Agregar botón para aplicar filtros
        val applyItem = popup.menu.add(0, 9999, 9999, "Aplicar filtros")
        applyItem.title = SpannableString("APLICAR FILTROS").apply {
            setSpan(
                ForegroundColorSpan(resources.getColor(R.color.orange, null)),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Variable para controlar si debemos cerrar el menú
        var shouldCloseMenu = false

        // Usamos un listener personalizado para controlar exactamente cuándo cerrar el menú
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_date_all -> {
                    // Desmarcar todas las selecciones
                    selectedDates.clear()
                    currentDateFilter = DateFilterType.ALL
                    // NO aplicamos filtros aquí, esperamos al botón Aplicar
                    allItem.isChecked = true

                    // Desmarcar todos los demás
                    todayItem.isChecked = false
                    yesterdayItem.isChecked = false
                    dayItems.forEach { itemId ->
                        popup.menu.findItem(itemId)?.isChecked = false
                    }

                    false // No cerrar el menú
                }

                R.id.menu_date_today, R.id.menu_date_yesterday,
                R.id.menu_date_day1, R.id.menu_date_day2,
                R.id.menu_date_day3, R.id.menu_date_day4, R.id.menu_date_day5 -> {
                    // Selección múltiple: invertir estado
                    val dayName = when (menuItem.itemId) {
                        R.id.menu_date_today -> "Hoy"
                        R.id.menu_date_yesterday -> "Ayer"
                        else -> {
                            val index = dayItems.indexOf(menuItem.itemId)
                            if (index >= 0 && index < dayNames.size) dayNames[index] else ""
                        }
                    }

                    // Invertir selección
                    if (selectedDates.contains(dayName)) {
                        selectedDates.remove(dayName)
                    } else {
                        selectedDates.add(dayName)
                    }

                    // Actualizar estado visual manualmente
                    menuItem.isChecked = !menuItem.isChecked

                    // Si hay selecciones, desmarcar "Todas las fechas"
                    allItem.isChecked = selectedDates.isEmpty()

                    false // No cerrar el menú
                }

                9999 -> { // Botón Aplicar
                    if (selectedDates.isNotEmpty()) {
                        currentDateFilter = DateFilterType.MULTI_SELECT
                    } else {
                        currentDateFilter = DateFilterType.ALL
                    }
                    applyDateFilter()
                    true // Cerrar el menú después de hacer clic en Aplicar
                }

                else -> false // No cerrar el menú para otras opciones
            }
        }

        // Mostrar el menú
        popup.show()
    }

    /**
     * Aplica el filtro de fecha seleccionado a las noticias.
     */
    private fun applyDateFilter() {
        when (currentDateFilter) {
            DateFilterType.ALL -> {
                filterData.dateFilter = null
                filterData.selectedDates = null
                filterData.isOlderThan = false
            }

            DateFilterType.MULTI_SELECT -> {
                if (selectedDates.isEmpty()) {
                    // Si no hay fechas seleccionadas, mostrar todas
                    filterData.dateFilter = null
                    filterData.selectedDates = null
                } else {
                    // Configurar múltiples fechas seleccionadas
                    val selectedDatesList = selectedDates.mapNotNull { pastDays[it] }
                    filterData.selectedDates = selectedDatesList
                }
                filterData.isOlderThan = false
            }
        }

        // Actualizar la UI para reflejar las fechas seleccionadas
        val selectedCount = selectedDates.size
        if (selectedCount > 0) {
            binding.imgDateFilter.setColorFilter(resources.getColor(R.color.orange, null))
        } else {
            binding.imgDateFilter.clearColorFilter()
        }

        vm.updateFilterData(filterData)
        vm.applyFilters(filterData)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mStartForResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            when (result.resultCode) {
                RESULT_OK -> handleFilterResult(result)
            }
        }
        isLauncherReady = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
    private fun loadInitialData() {
        if (!vm.hasLoadedData) {
            Log.d("loadInitialData", "Cargando datos iniciales")
            vm.setInitialData()
        } else {
            vm.searchNews(vm.searchQuery.value.orEmpty())
        }
    }

    /**
     * Activity result launcher to handle filter selection results.
     */
    private lateinit var mStartForResult: ActivityResultLauncher<Intent>
    private var isLauncherReady = false


    private fun updateFilterButtonVisibility() {
        if (isLauncherReady) {
            binding.clFilter.visibility = View.VISIBLE
        }
    }
    /**
     * Initializes the data binding for the fragment.
     */
    private fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_today, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.vm = vm
        binding.filterCount = 0
    }

    override fun onResume() {
        super.onResume()
    }

    /**
     * Sets up the UI components and listeners.
     */
    private fun setupUI() {
        newsAdapter = setupNewsRvAdapter()
        setupRefreshListener()

        binding.tvNoResults.visibility = View.GONE
        binding.clSearch.visibility = View.VISIBLE
        binding.btnClearSearch.visibility = View.VISIBLE
        binding.clFilter.visibility = View.VISIBLE

        setupSearchFunctionality()

        // Direct setup of the sort button - this overrides any XML onclick attributes
        binding.imgSort.setOnClickListener {
            showSortMenu(it)
        }

        // Configurar el botón de filtro por fecha
        binding.imgDateFilter.setOnClickListener {
            showDateFilterMenu(it)
        }
    }

    /**
     * Muestra el menú de ordenación con selección y flecha de dirección.
     */
    private fun showSortMenu(view: View) {
        val popup = PopupMenu(ContextThemeWrapper(context, R.style.AppTheme_PopupOverlay), view)
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)

        // Necesario para que se muestren los iconos en el menú
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            Log.e("TodayFragment", "Error mostrando iconos de menú: ${e.message}")
        }

        // Estilo para el encabezado
        val headerItem = popup.menu.findItem(R.id.menu_sort_header)
        if (headerItem != null) {
            headerItem.title = SpannableString(headerItem.title).apply {
                setSpan(ForegroundColorSpan(resources.getColor(R.color.orange, null)),
                    0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // Limpiar selección previa
        for (i in 0 until popup.menu.size()) {
            val item = popup.menu.getItem(i)
            item.isChecked = false
            item.icon = null

            // Aplicar negrita a todos los elementos del menú
            if (i != 0) { // Saltar el encabezado que ya tiene su propio estilo
                item.title = SpannableString(item.title).apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        // Determinar ítem seleccionado y dirección
        val (selectedId, isAsc) = when (currentSortType) {
            SortType.DATE_DESC -> Pair(R.id.menu_sort_date, false)
            SortType.DATE_ASC -> Pair(R.id.menu_sort_date, true)
            SortType.UPDATED_AT_DESC -> Pair(R.id.menu_sort_updated_at, false)
            SortType.UPDATED_AT_ASC -> Pair(R.id.menu_sort_updated_at, true)
            SortType.RELEVANCE_DESC -> Pair(R.id.menu_sort_relevance, false)
            SortType.RELEVANCE_ASC -> Pair(R.id.menu_sort_relevance, true)
            SortType.SOURCES_DESC -> Pair(R.id.menu_sort_sources, false)
            SortType.SOURCES_ASC -> Pair(R.id.menu_sort_sources, true)
        }

        // Resaltar ítem seleccionado y poner icono de dirección
        val selectedItem = popup.menu.findItem(selectedId)
        selectedItem.isChecked = true
        selectedItem.title = SpannableString(selectedItem.title).apply {
            setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.dark_gray)),
                0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Asignar icono de flecha
        val arrowRes = if (isAsc) R.drawable.ic_sort_ascending else R.drawable.ic_sort_descending
        val arrowDrawable = ContextCompat.getDrawable(requireContext(), arrowRes)
        selectedItem.icon = arrowDrawable

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_sort_date -> {
                    currentSortType = if (currentSortType == SortType.DATE_DESC)
                        SortType.DATE_ASC else SortType.DATE_DESC
                    sortNewsList()
                    true
                }
                R.id.menu_sort_updated_at -> {
                    currentSortType = if (currentSortType == SortType.UPDATED_AT_DESC)
                        SortType.UPDATED_AT_ASC else SortType.UPDATED_AT_DESC
                    sortNewsList()
                    true
                }
                R.id.menu_sort_relevance -> {
                    currentSortType = if (currentSortType == SortType.RELEVANCE_DESC)
                        SortType.RELEVANCE_ASC else SortType.RELEVANCE_DESC
                    sortNewsList()
                    true
                }
                R.id.menu_sort_sources -> {
                    currentSortType = if (currentSortType == SortType.SOURCES_DESC)
                        SortType.SOURCES_ASC else SortType.SOURCES_DESC
                    sortNewsList()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    /**
     * Sorts the news list based on the current sort type.
     */
    private fun sortNewsList() {
        val currentList = newsAdapter.list.toMutableList()

        val sortedList = when (currentSortType) {
            SortType.DATE_DESC -> currentList.sortedByDescending { parseDateToTimestamp(it.date) }
            SortType.DATE_ASC -> currentList.sortedBy { parseDateToTimestamp(it.date) }
            SortType.UPDATED_AT_DESC -> currentList.sortedByDescending { parseDateToTimestamp(it.updatedAt) }
            SortType.UPDATED_AT_ASC -> currentList.sortedBy { parseDateToTimestamp(it.updatedAt) }
            SortType.RELEVANCE_DESC -> currentList.sortedByDescending { it.relevance ?: 0.0 }
            SortType.RELEVANCE_ASC -> currentList.sortedBy { it.relevance ?: 0.0 }
            SortType.SOURCES_DESC -> currentList.sortedByDescending { it.sourceIds?.size ?: 0 }
            SortType.SOURCES_ASC -> currentList.sortedBy { it.sourceIds?.size ?: 0 }
        }

        newsAdapter.list = sortedList
        newsAdapter.notifyDataSetChanged()

        // Scroll to top after sorting
        binding.rvTodayNews.scrollToPosition(0)
    }

    /**
     * Sets up the pull-to-refresh functionality.
     */
    private fun setupRefreshListener() {
        binding.swRefresh.setOnRefreshListener {
            vm.refreshNews()
            binding.swRefresh.isRefreshing = false
        }
    }

    /**
     * Sets up the search functionality with animations and event handlers.
     */
    private fun setupSearchFunctionality() {
        val cardView = binding.cardViewSearch
        val editText = binding.etSearch
        val clearButton = binding.btnClearSearch
        val searchIcon = binding.ivSearch

        val darkColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)
        val whiteColor = Color.WHITE
        val grayTextColor = ContextCompat.getColor(requireContext(), R.color.gray_light)
        val blackTextColor = Color.BLACK

        var isProgrammaticFocusChange = false

        setupSearchFocusListener(editText, cardView, searchIcon, clearButton, darkColor, whiteColor, grayTextColor, blackTextColor, isProgrammaticFocusChange)
        setupSearchTextWatcher(editText)
        setupSearchActionListener(editText)
        setupClearButtonListener(clearButton)
    }

    /**
     * Sets up focus listener for search with animations.
     */
    private fun setupSearchFocusListener(
        editText: android.widget.EditText,
        cardView: androidx.cardview.widget.CardView,
        searchIcon: android.widget.ImageView,
        clearButton: android.widget.ImageView,
        darkColor: Int,
        whiteColor: Int,
        grayTextColor: Int,
        blackTextColor: Int,
        isProgrammaticFocusChange: Boolean
    ) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (isProgrammaticFocusChange) return@setOnFocusChangeListener

            val colorAnimator = if (hasFocus) {
                ValueAnimator.ofArgb(darkColor, whiteColor).apply {
                    addUpdateListener {
                        cardView.setCardBackgroundColor(it.animatedValue as Int)
                    }
                }
            } else if (editText.text.isNullOrEmpty()) {
                ValueAnimator.ofArgb(whiteColor, darkColor).apply {
                    addUpdateListener {
                        cardView.setCardBackgroundColor(it.animatedValue as Int)
                    }
                }
            } else null

            val textAnimator = if (hasFocus) {
                ValueAnimator.ofArgb(grayTextColor, blackTextColor)
            } else if (editText.text.isNullOrEmpty()) {
                ValueAnimator.ofArgb(blackTextColor, grayTextColor)
            } else null

            textAnimator?.apply {
                duration = 300
                addUpdateListener { anim ->
                    val color = anim.animatedValue as Int
                    editText.setTextColor(color)
                    editText.setHintTextColor((color and 0xFFFFFF) or 0x80000000.toInt())
                    searchIcon.setColorFilter(color)
                    clearButton.setColorFilter(color)
                }
                start()
            }

            colorAnimator?.apply {
                duration = 300
                start()
            }

            if (hasFocus && editText.text.isNotEmpty()) {
                editText.post {
                    editText.setSelection(editText.text.length)
                }
            }
        }
    }

    /**
     * Sets up text watcher for search input with debounce.
     */
    private fun setupSearchTextWatcher(editText: android.widget.EditText) {
        editText.addTextChangedListener { text ->
            if (isSettingText) return@addTextChangedListener

            binding.etSearch.removeCallbacks(searchRunnable)
            val query = text?.toString()?.trim() ?: ""

            if (query.length >= 2) {
                binding.etSearch.postDelayed(searchRunnable, 1500)
            } else if (query.isEmpty() && isSearchActive) {
                binding.etSearch.post(searchRunnable)
            }
        }
    }

    /**
     * Sets up keyboard action listener for search.
     */
    private fun setupSearchActionListener(editText: android.widget.EditText) {
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.etSearch.removeCallbacks(searchRunnable)
                performSearch(true)
                return@setOnEditorActionListener true
            }
            false
        }
    }

    /**
     * Sets up clear button for search input.
     */
    private fun setupClearButtonListener(clearButton: android.widget.ImageView) {
        clearButton.setOnClickListener {
            binding.etSearch.removeCallbacks(searchRunnable)
            isSearchActive = false
            vm.searchNews("")
            clearSearchInput()
        }
    }

    /**
     * Runnable that performs search after debounce period.
     */
    private val searchRunnable = Runnable {
        performSearch(true)
    }

    /**
     * Performs search with the current query and manages keyboard visibility.
     *
     * @param hideKeyboardAfter Whether to hide keyboard after search
     */
    private fun performSearch(hideKeyboardAfter: Boolean) {
        val query = binding.etSearch.text.toString().trim()

        Log.d("User", "performSearch: $query")
        isSearchActive = query.length >= 2

        // Mostrar mensaje informativo para búsquedas especializadas
        if (query.startsWith("g", ignoreCase = true) && query.length > 1) {
            val groupId = query.substring(1).toIntOrNull()
            if (groupId != null) {
                vm.messageEvent.postValue("Buscando grupo: $groupId")
            }
        } else if (query.startsWith("s", ignoreCase = true) && query.length > 1) {
            val sourceId = query.substring(1)
            vm.messageEvent.postValue("Buscando fuente: $sourceId")
        }

        vm.searchNews(query)
        if (hideKeyboardAfter && query.isNotEmpty()) {
            hideKeyboard()
        }
    }

    /**
     * Clears search input text.
     */
    private fun clearSearchInput() {
        isSettingText = true
        binding.etSearch.setText("")
        isSettingText = false
    }

    /**
     * Sets up observers for live data from the view model.
     */
    private fun setupObservers() {
        observeLoading()
        observeSearchQuery()
        observeNewsList()
        observeClickEvents()
        observeMessages()
        observeNoResults()
        observePagination()
    }

    private fun observePagination() {
        vm.isPaginating.observe(viewLifecycleOwner) { isPaginating ->
            binding.paginationLoader.visibility = if (isPaginating) View.VISIBLE else View.GONE
        }
    }

    private fun observeLoading() {
        vm.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swRefresh.isRefreshing = isLoading
            binding.rvTodayNews.visibility = if (isLoading) View.GONE else View.VISIBLE

            if (!isLoading && binding.clSearch.isGone) {
                binding.clSearch.visibility = View.VISIBLE
                binding.btnClearSearch.visibility = View.VISIBLE
            }

            if (isLoading) {
                binding.tvNoResults.visibility = View.GONE
            }
        }
    }

    private fun observeSearchQuery() {
        vm.searchQuery.observe(viewLifecycleOwner) { query ->
            if (binding.etSearch.text.toString() != query) {
                isSettingText = true
                binding.etSearch.setText(query)
                binding.etSearch.setSelection(query.length)
                isSettingText = false
                isSearchActive = query.isNotEmpty()
            }
        }
    }

    private fun observeNewsList() {
        vm.neutralNewsList.observe(viewLifecycleOwner) { news ->
            val newsItems = news.map { neutralNews ->
                NewsBean(
                    title = neutralNews.neutralTitle,
                    description = neutralNews.neutralDescription,
                    category = neutralNews.category,
                    imageUrl = neutralNews.imageUrl,
                    group = neutralNews.group,
                    createdAt = neutralNews.createdAt,
                    date = neutralNews.date,
                    updatedAt = neutralNews.updatedAt,
                    relevance = neutralNews.relevance,
                    sourceIds = neutralNews.sourceIds
                )
            }

            newsAdapter.list = if (isSearchActive) {
                newsItems
            } else {
                when (currentSortType) {
                    SortType.DATE_DESC -> newsItems.sortedByDescending { parseDateToTimestamp(it.date) }
                    // Apply other sort types based on current selection
                    else -> {
                        sortNewsList()
                        newsItems
                    }
                }
            }
            newsAdapter.notifyDataSetChanged()

            if (!isSearchActive && newsItems.isEmpty()) {
                binding.tvNoResults.visibility = View.VISIBLE
                binding.tvNoResults.text = getString(R.string.no_news_found)
            }
        }
    }
    private fun parseDateToTimestamp(dateString: String?): Long {
        if (dateString.isNullOrEmpty()) return 0L

        // Intentar con múltiples formatos posibles
        val formats = listOf(
            DATE_FORMAT,
            "yyyy-MM-dd'T'HH:mm:ssX",
            "EEE, dd MMMM 'de' yyyy HH:mm:ss.SSS",
            // Agregar aquí todos los formatos posibles, incluyendo el nuevo formato
            settings.dateFormat // Formato configurado por el usuario
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale("es", "ES"))
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(dateString)?.time ?: 0L
            } catch (e: Exception) {
                // Intentar con el siguiente formato
            }
        }

        return 0L
    }
    private fun observeClickEvents() {
        vm.obrClick.observe(viewLifecycleOwner) { view ->
            when (view.id) {
                R.id.imgFilter -> {
                    Log.e("TAG", "onCreateView: ")
                    mStartForResult.launch(FilterActivity.newIntent(requireContext(), filterData))
                }
            }
        }
    }

    private fun observeMessages() {
        vm.messageEvent.observe(viewLifecycleOwner) { message ->
            msgNormal(message)
        }
    }

    private fun observeNoResults() {
        vm.showNoResults.observe(viewLifecycleOwner) { noResults ->
            if (noResults) {
                binding.tvNoResults.visibility = View.VISIBLE
                binding.tvNoResults.text = getString(R.string.no_search_results_found)
            } else {
                binding.tvNoResults.visibility = View.GONE
            }
        }
    }

    /**
     * Hides the soft keyboard.
     */
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    /**
     * Sets up the RecyclerView adapter for news items.
     *
     * @return Configured adapter for news items
     */
    private fun setupNewsRvAdapter(): SimpleRecyclerViewAdapter<NewsBean, RowNewsBinding> {
        if (::newsAdapter.isInitialized && newsAdapter.list.isNotEmpty()) {
            return newsAdapter
        }

        val adapter = createNewsAdapter()
        configureRecyclerView(adapter)
        setupScrollListener()
        return adapter
    }

    /**
     * Creates and configures the news adapter with item click handling.
     */
    private fun createNewsAdapter(): SimpleRecyclerViewAdapter<NewsBean, RowNewsBinding> {
        return SimpleRecyclerViewAdapter<NewsBean, RowNewsBinding>(
            R.layout.row_news,
            BR.newsBean,
            object : SimpleRecyclerViewAdapter.SimpleCallback<NewsBean, RowNewsBinding> {
                override fun onItemClick(v: View, m: NewsBean) {
                    when (v.id) {
                        R.id.clNews -> {
                            // Animación de pulsación
                            v.animate()
                                .scaleX(0.95f)
                                .scaleY(0.95f)
                                .setDuration(50)
                                .withEndAction {
                                    // Animación de rebote al volver
                                    v.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(50)
                                        .withEndAction {
                                            var relatedNews = vm.getRelatedNews(m)
                                            var sizeMb = checkExtraSize(relatedNews)
                                            while (sizeMb > 0.25 && relatedNews.isNotEmpty()) {
                                                // Elimina la noticia con peor neutralidad
                                                relatedNews = relatedNews
                                                    .sortedBy { it.neutralScore }
                                                    .drop(1)
                                                sizeMb = checkExtraSize(relatedNews)
                                                Log.d("ExtraSizeCheck", "relatedNews recortado a %.2f MB".format(sizeMb))
                                            }

                                            Log.d("ExtraSizeCheck", "relatedNews final size: %.2f MB".format(sizeMb))
                                            Log.d("ExtraSizeCheck", "m final size: %.2f MB".format(checkExtraSize(m)))
                                            Log.d("ExtraSizeCheck", "relatedNews size: %.2f MB".format(checkExtraSize(relatedNews)))
                                            Log.d("ExtraSizeCheck", "settings size: %.2f MB".format(checkExtraSize(settings)))
                                            Log.d("ExtraSizeCheck", "total size: %.2f MB".format(
                                                checkExtraSize(m) + checkExtraSize(relatedNews) + checkExtraSize(settings)
                                            ))
                                            startNewActivity(
                                                NewDetailActivity.newIntent(
                                                    requireContext(),
                                                    0,
                                                    m,
                                                    relatedNews,
                                                    settings
                                                )
                                            )
                                            // Agregar animación de transición
                                            requireActivity().overridePendingTransition(
                                                R.anim.slide_in_right,
                                                R.anim.slide_out_left
                                            )
                                        }
                                        .start()
                                }
                                .start()
                        }
                    }
                }

                override fun onViewBinding(
                    holder: SimpleRecyclerViewAdapter.SimpleViewHolder<RowNewsBinding>,
                    m: NewsBean,
                    pos: Int
                ) {
                    holder.binding.apply {
                        executePendingBindings()
                    }
                }
            }
        )
    }
    private fun checkExtraSize(obj: Any): Double {
        val json = Gson().toJson(obj)
        val bytes = json.toByteArray(Charsets.UTF_8)
        val mb = bytes.size.toDouble() / (1024 * 1024)
        return mb
    }
    /**
     * Configures the RecyclerView with layout manager and adapter.
     */
    private fun configureRecyclerView(adapter: SimpleRecyclerViewAdapter<NewsBean, RowNewsBinding>) {
        binding.rvTodayNews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }

    /**
     * Sets up scroll listener to hide keyboard when scrolling.
     */
    private var isScrollListenerProcessing = false

    private fun setupScrollListener() {
        binding.rvTodayNews.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && binding.etSearch.hasFocus()) {
                    binding.etSearch.clearFocus()
                    hideKeyboard()
                }

//                // Solo procesar cuando se detiene el scroll
//                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    checkForPagination(recyclerView)
//                }
            }

//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//
//                // Solo procesar scroll hacia abajo y evitar múltiples llamadas
//                if (dy > 0 && !isScrollListenerProcessing && !isSearchActive) {
//                    isScrollListenerProcessing = true
//                    recyclerView.post {
//                        checkForPagination(recyclerView)
//                        isScrollListenerProcessing = false
//                    }
//                }
//            }
//
//            private fun checkForPagination(recyclerView: RecyclerView) {
//                if (vm.isPaginating.value == true || isSearchActive) return
//
//                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
//                val visibleItemCount = layoutManager.childCount
//                val totalItemCount = layoutManager.itemCount
//                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
//
//                // Si estamos cerca del final (últimos 8 elementos), cargar más
//                if ((visibleItemCount + firstVisibleItem) >= totalItemCount - 8
//                    && firstVisibleItem >= 0) {
//                    Log.d("Paginación", "Solicitando nueva página de noticias")
//                    vm.loadNextPage()
//                }
//            }
        })
    }


    /**
     * Handles filter selection result from FilterActivity.
     */
    private fun handleFilterResult(result: ActivityResult) {
        val data = result.data?.getStringExtra(FILTER_DATA).orEmpty()
        val filterCount = result.data?.getIntExtra(FILTER_COUNT, 0)
        binding.filterCount = filterCount

        // Parse the filter data
        filterData = Gson().fromJson(data, object : TypeToken<LocalFilterBean>() {}.type)
        filterData.media?.let { loggerE(it.joinToString { "," }) }

        // Apply filters client-side without making new API calls
        isSearchActive = false
        vm.applyFilters(filterData)
    }

    /**
     * Aplica la configuración de tamaños de fuente y formato de fecha
     * a todos los componentes relevantes del fragmento.
     *
     * @param settings Configuración a aplicar
     */
    fun applySettings(settings: SettingsBean) {
        Log.d("TodayFragment", "Aplicando configuración: $settings")

        // Guardar configuración
        this.settings = settings

        try {
            // Aplicar tamaño al título principal (usando sp en lugar de pixels)
            binding.tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,
                settings.titleFontSize.toFloat())

            // Actualizar adaptador con nueva configuración
            if (::newsAdapter.isInitialized) {
                // Implementar método para propagar configuración al adaptador
                updateAdapterSettings(settings)
                newsAdapter.notifyDataSetChanged()
            }

            // Guardar configuración para el futuro
            saveSettings()

            Log.d("TodayFragment", "Configuración aplicada correctamente")
        } catch (e: Exception) {
            Log.e("TodayFragment", "Error al aplicar configuración: ${e.message}")
        }
    }

    /**
     * Actualiza la configuración en el adaptador de noticias
     */
    private fun updateAdapterSettings(settings: SettingsBean) {
        // Reemplaza el adaptador con uno nuevo para aplicar los cambios
        val currentList = newsAdapter.list

        newsAdapter = SimpleRecyclerViewAdapter<NewsBean, RowNewsBinding>(
            R.layout.row_news,
            BR.newsBean,
            object : SimpleRecyclerViewAdapter.SimpleCallback<NewsBean, RowNewsBinding> {
                override fun onItemClick(v: View, m: NewsBean) {
                    when (v.id) {
                        R.id.clMain -> {
                            // Corrección del parámetro
                            // get related news
                            val relatedNews = vm.getRelatedNews(m).ifEmpty { emptyList() }
                            startNewActivity(NewDetailActivity.newIntent(requireContext(), 0L, m, relatedNews, settings))
                        }
                    }
                }

                override fun onViewBinding(
                    holder: SimpleRecyclerViewAdapter.SimpleViewHolder<RowNewsBinding>,
                    model: NewsBean,
                    position: Int
                ) {
                    // Usar binding del ViewHolder, no del fragmento
                    holder.binding.tvTitle.setTextSize(
                        android.util.TypedValue.COMPLEX_UNIT_SP,
                        settings.titleFontSize.toFloat()
                    )

                    // Formato de fecha según configuración
                    if (settings.dateFormat.isNotEmpty()) {
                        // Implementar formato de fecha
                    }
                }
            }
        )

        // Restaurar la lista y actualizar el RecyclerView
        newsAdapter.list = currentList
        binding.rvTodayNews.adapter = newsAdapter
    }

    /**
     * Guarda la configuración actual para uso futuro
     */
    private fun saveSettings() {
        try {
            // Guardar configuración en SharedPreferences
            val sharedPrefs = requireActivity().getSharedPreferences("app_settings", 0)
            sharedPrefs.edit {

                putInt("title_font_size", settings.titleFontSize)
                putInt("description_font_size", settings.descriptionFontSize)
                putInt("details_font_size", settings.detailsTextFontSize)
                putString("date_format", settings.dateFormat)
            }

            Log.d("TodayFragment", "Configuración guardada correctamente")
        } catch (e: Exception) {
            Log.e("TodayFragment", "Error al guardar configuración: ${e.message}")
        }
    }

    /**
     * Gets the current title font size.
     *
     * @return Current title font size in pixels
     */
    fun getTitleFontSize(): Int {
        return binding.tvTitle.textSize.toInt()
    }

    /**
     * Gets the current description font size.
     *
     * @return Current description font size
     */
    fun getDescriptionFontSize(): Int {
        return settings.descriptionFontSize
    }

    /**
     * Gets the current date format.
     *
     * @return Current date format string
     */
    fun getDateFormat(): String {
        return settings.dateFormat ?: "DD-MM-YYYY"
    }


    /**
     * Scrolls the RecyclerView to the top.
     */
    fun scrollToTop() {
        binding.rvTodayNews.smoothScrollToPosition(0)
    }
}






