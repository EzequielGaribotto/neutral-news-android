package com.example.neutralnews_android.ui.main.today

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.neutralnews_android.data.bean.filter.LocalFilterBean
import com.example.neutralnews_android.data.bean.news.NeutralNewsBean
import com.example.neutralnews_android.data.bean.news.NewsBean
import com.example.neutralnews_android.data.remote.SnapshotManager
import com.example.neutralnews_android.data.room.AppDatabase
import com.example.neutralnews_android.data.room.entities.NeutralNewsEntity
import com.example.neutralnews_android.data.room.entities.NewsEntity
import com.example.neutralnews_android.di.event.SingleLiveEvent
import com.example.neutralnews_android.di.viewmodel.BaseViewModel
import com.example.neutralnews_android.util.Prefs
import com.google.firebase.firestore.DocumentChange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@HiltViewModel
class TodayFragmentVM @Inject constructor(application: Application) : BaseViewModel(application) {
    private val pageSize = 10
    private var isLoadingMoreItems = false
    val isPaginating = SingleLiveEvent<Boolean>()
    private val paginationHelper = PaginationHelper(pageSize = pageSize, initialDisplayLimit = 10)
    private val groupsOfNews = mutableMapOf<Int, List<NewsBean>>()
    val messageEvent = SingleLiveEvent<String>()
    val isLoading = SingleLiveEvent<Boolean>()
    val neutralNewsList = SingleLiveEvent<List<NeutralNewsBean>>()
    val newsList = SingleLiveEvent<List<List<NewsBean>>>()
    internal var allNeutralNews = mutableListOf<NeutralNewsBean>()
    internal var allNews = mutableListOf<NewsBean>()
    internal var currentFilterData = LocalFilterBean()
    val searchQuery = MutableLiveData<String>("")
    val showNoResults = MutableLiveData<Boolean>(false)
    val currentSortType = MutableLiveData<TodaySortType>(TodaySortType.DATE_DESC)

    private val snapshotManager = SnapshotManager()
    private var newsListenerCompleted = false
    private var neutralNewsListenerCompleted = false
    private var isCacheLoading = false
    private var dataLoaded = false
    private var filteredNeutralNewsCache = listOf<NeutralNewsBean>()
    private var filteredRegularNewsCache = listOf<NewsBean>()
    private var filtersApplied = false
    private var lastAppliedFilter = LocalFilterBean()

    // Data manager para encapsular lógica de caché/Firestore
    private val dataManager: NewsDataManager

    init {
        val database = AppDatabase.getDatabase(application)
        dataManager = NewsDataManager(database.neutralNewsDao(), database.newsDao())
    }

    // === Public API - Date Filter Management ===

    fun parseDateToTimestamp(dateString: String?): Long =
        DateFormatterHelper.parseDateToTimestamp(dateString)

    fun onDateFilterApplied(selectedList: List<Date>) {
        currentFilterData.selectedDates = if (selectedList.isEmpty()) null else selectedList
        DateFilterHelper.persistSelectedDates(selectedList)

        if (selectedList.isNotEmpty()) {
            loadNewsForDates(selectedList, isInitialLoad = false)
        } else {
            // Sin fechas seleccionadas, limpiar y recargar
            allNeutralNews.clear()
            applyFiltersAndSearch(force = true)
        }
    }

    /**
     * Función común para cargar noticias de fechas específicas.
     * PASO 1: Carga PRIMERAS 10 y las muestra en UI
     * PASO 2: Carga el RESTO en background sin actualizar UI
     */
    private fun loadNewsForDates(selectedList: List<Date>, isInitialLoad: Boolean) {
        viewModelScope.launch {
            isLoading.postValue(true)

            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(getApplication())
                val neutralDaoLocal = db.neutralNewsDao()

                val dateRanges = selectedList.map { d ->
                    val cal = Calendar.getInstance()
                    cal.time = d
                    cal.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val from = cal.timeInMillis
                    cal.apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val to = cal.timeInMillis
                    Pair(from, to)
                }

                // PASO 1: Intentar cargar primeras 10 del caché
                val cachedNews = dateRanges.flatMap { (from, to) ->
                    neutralDaoLocal.getNewsBetween(from, to).map { entity ->
                        NeutralNewsBean(
                            id = entity.id,
                            neutralTitle = entity.neutralTitle,
                            neutralDescription = entity.neutralDescription,
                            category = entity.category.toString(),
                            imageUrl = entity.imageUrl,
                            group = entity.group!!,
                            date = if (entity.date != Long.MIN_VALUE) DateFormatterHelper.formatDateToSpanish(entity.date) else null,
                            createdAt = if (entity.createdAt != Long.MIN_VALUE) DateFormatterHelper.formatDateToSpanish(entity.createdAt) else null,
                            updatedAt = if (entity.updatedAt != Long.MIN_VALUE) DateFormatterHelper.formatDateToSpanish(entity.updatedAt) else null,
                            sourceIds = entity.sourceIds,
                            relevance = entity.relevance
                        )
                    }
                }

                if (cachedNews.isNotEmpty()) {
                    Log.d("TodayFragmentVM", "Noticias del caché: ${cachedNews.size}")

                    // PASO 2: Ordenar y tomar primeras 10
                    val sortedAll = NewsSorter.sortNeutralNews(cachedNews, currentSortType.value ?: TodaySortType.DATE_DESC)
                    val first10 = sortedAll.take(10)

                    // PASO 3: Configurar helper con las primeras 10
                    paginationHelper.setCachedNeutralAll(first10)
                    allNeutralNews.clear()
                    allNeutralNews.addAll(first10)

                    Log.d("TodayFragmentVM", "Mostrando PRIMERAS ${first10.size} noticias en UI")

                    // PASO 4: Actualizar UI INMEDIATAMENTE con las primeras 10
                    withContext(Dispatchers.Main) {
                        applyFiltersAndSearch(force = true)
                        isLoading.postValue(false)
                    }

                    // PASO 5: Cargar el RESTO en background SIN actualizar UI
                    if (sortedAll.size > 10) {
                        launch {
                            Log.d("TodayFragmentVM", "Cargando ${sortedAll.size - 10} noticias restantes en background...")

                            // Actualizar helper con TODAS (incluye las primeras 10)
                            paginationHelper.setCachedNeutralAll(sortedAll)

                            Log.d("TodayFragmentVM", "Background: ${sortedAll.size} noticias totales disponibles para paginación")
                            // NO actualizar UI aquí - solo cuando el usuario pagine
                        }
                    }

                    // PASO 6: Verificar actualizaciones en Firestore (sin bloquear)
                    launch {
                        val updatedNews = dataManager.fetchAllNeutralNewsForDates(selectedList)
                        if (updatedNews.isNotEmpty()) {
                            dataManager.saveNeutralNewsToCache(updatedNews)

                            val combined = (cachedNews + updatedNews).distinctBy { it.id }
                            val sortedCombined = NewsSorter.sortNeutralNews(combined, currentSortType.value ?: TodaySortType.DATE_DESC)
                            paginationHelper.setCachedNeutralAll(sortedCombined)

                            Log.d("TodayFragmentVM", "Actualizadas desde Firestore: ahora ${sortedCombined.size} totales")
                        }
                    }

                    // PASO 7: Si es carga inicial, cargar relacionadas
                    if (isInitialLoad) {
                        loadCachedRelatedNews()
                        refreshNeutralNews()
                    }
                } else {
                    // Sin caché: PASO 1 - Cargar PRIMERAS 10 desde Firestore
                    Log.d("TodayFragmentVM", "Sin caché, cargando PRIMERAS 10 desde Firestore...")

                    val first10 = dataManager.fetchFirstNeutralNewsForDates(selectedList, limit = 10)

                    if (first10.isNotEmpty()) {
                        Log.d("TodayFragmentVM", "Primeras ${first10.size} noticias cargadas desde Firestore")

                        // PASO 2: Configurar helper con las primeras 10
                        paginationHelper.setCachedNeutralAll(first10)
                        allNeutralNews.clear()
                        allNeutralNews.addAll(first10)

                        Log.d("TodayFragmentVM", "Mostrando PRIMERAS ${first10.size} noticias en UI")

                        // PASO 3: Actualizar UI INMEDIATAMENTE
                        withContext(Dispatchers.Main) {
                            applyFiltersAndSearch(force = true)
                            isLoading.postValue(false)
                        }

                        // PASO 4: Cargar TODAS en background
                        launch {
                            Log.d("TodayFragmentVM", "Cargando TODAS las noticias en background...")

                            val allNews = dataManager.fetchAllNeutralNewsForDates(selectedList)

                            if (allNews.isNotEmpty()) {
                                Log.d("TodayFragmentVM", "Background: ${allNews.size} noticias totales cargadas")

                                // Guardar en caché
                                dataManager.saveNeutralNewsToCache(allNews)

                                // Actualizar helper con TODAS
                                val sortedAll = NewsSorter.sortNeutralNews(allNews, currentSortType.value ?: TodaySortType.DATE_DESC)
                                paginationHelper.setCachedNeutralAll(sortedAll)

                                Log.d("TodayFragmentVM", "Background: ${sortedAll.size} noticias disponibles para paginación")
                                // NO actualizar UI - solo cuando el usuario pagine
                            }
                        }

                        // PASO 5: Si es carga inicial, iniciar listeners
                        if (isInitialLoad) {
                            loadCachedRelatedNews()
                            refreshNeutralNews()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            messageEvent.postValue("No se encontraron noticias para las fechas seleccionadas")
                            isLoading.postValue(false)
                        }
                    }
                }
            }
        }
    }

    fun restoreSelectedDatesFromPrefs(): List<Date> {
        val dates = DateFilterHelper.restoreSelectedDates()
        if (dates.isNotEmpty()) {
            currentFilterData.selectedDates = dates
            applyFiltersAndSearch()
        }
        return dates
    }

    fun getSelectedDatesFormatted(): Set<String> =
        DateFilterHelper.formatSelectedDates(currentFilterData.selectedDates ?: emptyList())

    fun getSelectedDatesMillis(): LongArray =
        DateFilterHelper.datesToMillisArray(currentFilterData.selectedDates ?: emptyList())

    fun clearSelectedDates() {
        currentFilterData.selectedDates = null
        DateFilterHelper.clearSelectedDates()
        applyFiltersAndSearch(force = true)
    }

    fun calculatePastDays(pastDays: MutableMap<String, Date>): Map<String, Date> {
        pastDays.clear()
        pastDays.putAll(DateFormatterHelper.calculatePastDays())
        return pastDays
    }

    // === Public API - Data Loading ===

    fun setInitialData() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val includeYesterday = currentHour < 12

            // CRITICAL: Construir lista de fechas (hoy + ayer si es antes de las 12)
            val datesToLoad = mutableListOf<Date>()

            // Hoy
            datesToLoad.add(calendar.time)

            // Ayer si es antes de las 12
            if (includeYesterday) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                datesToLoad.add(calendar.time)
            }

            Log.d("TodayFragmentVM", "Cargando noticias iniciales: ${datesToLoad.size} día(s)")

            // CRITICAL: Reutilizar la lógica de onDateFilterApplied
            // Esto carga TODAS las noticias primero, luego muestra las primeras N
            currentFilterData.selectedDates = datesToLoad
            DateFilterHelper.persistSelectedDates(datesToLoad)

            // Cargar noticias usando la misma lógica optimizada
            loadNewsForDates(datesToLoad, isInitialLoad = true)

            // Computar días disponibles en background
            viewModelScope.launch(Dispatchers.IO) {
                computeAvailableDaysIfNeeded()
            }
        }
    }

    private fun computeAvailableDaysIfNeeded() {
        try {
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val generatedDate = Prefs.getString("available_days_generated_date", null)
            if (generatedDate == null || generatedDate != todayKey) {
                viewModelScope.launch(Dispatchers.IO) {
                    computeAndCacheAvailableDays()
                }
                Prefs.putString("available_days_generated_date", todayKey)
            }
        } catch (_: Exception) { }
    }

    private suspend fun computeAndCacheAvailableDays() {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(getApplication())
                val newsDaoLocal = db.newsDao()
                val neutralDaoLocal = db.neutralNewsDao()

                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH) + 1

                for (offset in listOf(-1, 0, 1, 2)) {
                    val target = Calendar.getInstance().apply {
                        set(Calendar.YEAR, currentYear)
                        set(Calendar.MONTH, currentMonth - 1)
                        add(Calendar.MONTH, offset)
                    }
                    val y = target.get(Calendar.YEAR)
                    val m = target.get(Calendar.MONTH) + 1

                    val (from, to) = getMonthRange(target)
                    val days = mutableSetOf<Int>()

                    extractDaysFromNews(newsDaoLocal.getNewsBetween(from, to), days)
                    extractDaysFromNeutralNews(neutralDaoLocal.getNewsBetween(from, to), days)

                    Prefs.putString("available_days_${y}_${m}", days.sorted().joinToString(","))
                }

                Prefs.putLong("available_days_generated_at", System.currentTimeMillis())
            } catch (_: Exception) { }
        }
    }

    private fun getMonthRange(calendar: Calendar): Pair<Long, Long> {
        calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val from = calendar.timeInMillis

        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.apply {
            set(Calendar.DAY_OF_MONTH, lastDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val to = calendar.timeInMillis

        return Pair(from, to)
    }

    private fun extractDaysFromNews(newsEntities: List<NewsEntity>, days: MutableSet<Int>) {
        newsEntities.forEach { n ->
            val ts = if (n.pubDate > 0L) n.pubDate else if (n.createdAt > 0L) n.createdAt else n.updatedAt
            if (ts > 0L) {
                val c = Calendar.getInstance()
                c.timeInMillis = ts
                days.add(c.get(Calendar.DAY_OF_MONTH))
            }
        }
    }

    private fun extractDaysFromNeutralNews(neutralEntities: List<NeutralNewsEntity>, days: MutableSet<Int>) {
        neutralEntities.forEach { ne ->
            val ts = if (ne.date > 0L) ne.date else if (ne.createdAt > 0L) ne.createdAt else ne.updatedAt
            if (ts > 0L) {
                val c = Calendar.getInstance()
                c.timeInMillis = ts
                days.add(c.get(Calendar.DAY_OF_MONTH))
            }
        }
    }

    private suspend fun loadNews(initialDateRanges: List<Pair<Long, Long>> = emptyList()) {
        if (isCacheLoading) return
        isCacheLoading = true

        withContext(Dispatchers.IO) {
            Log.d("TodayFragmentVM", "Iniciando carga desde caché local")
            messageEvent.postValue("Verificando caché local...")

            try {
                // Cargar noticias del día actual desde caché
                val cachedNews = dataManager.loadCachedNeutralNews()

                if (cachedNews.isNotEmpty()) {
                    Log.d("TodayFragmentVM", "Cargadas ${cachedNews.size} noticias del día desde caché")

                    // Configurar paginación con TODAS las noticias
                    paginationHelper.setCachedNeutralAll(cachedNews)

                    // Mostrar SOLO las primeras 10 visualmente
                    allNeutralNews = mutableListOf()
                    val initial = paginationHelper.prepareInitialDisplay(allNeutralNews)
                    if (initial.isNotEmpty()) {
                        allNeutralNews.addAll(initial)
                    }

                    Log.d("TodayFragmentVM", "Mostrando primeras ${allNeutralNews.size} noticias visualmente")

                    // CRITICAL: Actualizar UI inmediatamente con las primeras 10
                    withContext(Dispatchers.Main) {
                        applyFiltersAndSearch(force = true)
                    }

                    // Cargar noticias relacionadas en segundo plano
                    loadCachedRelatedNews()

                    // Verificar actualizaciones en segundo plano (sin bloquear UI)
                    launch(Dispatchers.IO) {
                        verifyAndUpdateFromFirestore()
                    }

                    refreshNeutralNews()
                } else {
                    // No hay caché, cargar desde Firestore
                    Log.d("TodayFragmentVM", "Sin caché, cargando desde Firestore")
                    fetchNeutralNewsFromFirestoreSuspend(filterData = currentFilterData)
                }
            } finally {
                isCacheLoading = false
            }
        }
    }

    private suspend fun loadCachedRelatedNews() {
        try {
            val relatedNews = dataManager.loadCachedRelatedNews()
            if (relatedNews.isNotEmpty()) {
                allNews.clear()
                allNews.addAll(relatedNews)
                updateGroupsOfNews(allNews)
                Log.d("TodayFragmentVM", "Noticias relacionadas cargadas: ${allNews.size}")
            }
        } catch (e: Exception) {
            Log.e("TodayFragmentVM", "Error loading related news: ${e.localizedMessage}")
        }
    }

    /**
     * Verifica actualizaciones desde Firestore en segundo plano sin mostrar loading.
     * Actualiza la UI y el caché si hay cambios.
     */
    private suspend fun verifyAndUpdateFromFirestore() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("TodayFragmentVM", "Verificando actualizaciones desde Firestore...")

                val result = dataManager.verifyUpdatesFromFirestore(allNeutralNews)

                if (result.hasChanges) {
                    Log.d("TodayFragmentVM", "Actualizaciones encontradas: ${result.newItems.size} nuevas, ${result.updatedItems.size} actualizadas")

                    // Actualizar noticias modificadas
                    @Suppress("UNCHECKED_CAST")
                    val updatedNews = result.updatedItems as List<NeutralNewsBean>
                    updatedNews.forEach { updated ->
                        val index = allNeutralNews.indexOfFirst { it.id == updated.id }
                        if (index >= 0) allNeutralNews[index] = updated
                    }

                    // Agregar noticias nuevas al inicio
                    @Suppress("UNCHECKED_CAST")
                    val newNews = result.newItems as List<NeutralNewsBean>
                    if (newNews.isNotEmpty()) {
                        allNeutralNews.addAll(0, newNews)
                    }

                    // Guardar en caché
                    dataManager.saveNeutralNewsToCache(updatedNews + newNews)

                    // Reordenar
                    paginationHelper.setCachedNeutralAll(allNeutralNews.sortedByDescending { it.date ?: "" })

                    // Actualizar UI solo si hay cambios
                    withContext(Dispatchers.Main) {
                        applyFiltersAndSearch(force = true)
                    }
                } else {
                    Log.d("TodayFragmentVM", "No hay actualizaciones desde Firestore")
                }
            } catch (e: Exception) {
                Log.e("TodayFragmentVM", "Error verificando actualizaciones: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun loadNewsFromDbForDateFilter() {
        try {
            val db = AppDatabase.getDatabase(getApplication())
            val neutralDaoLocal = db.neutralNewsDao()

            val dates = (currentFilterData.selectedDates ?: emptyList()).map { d ->
                val cal = Calendar.getInstance()
                cal.time = d
                cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val from = cal.timeInMillis
                cal.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val to = cal.timeInMillis
                Pair(from, to)
            }

            val neutralAccum = dates.flatMap { (from, to) ->
                neutralDaoLocal.getNewsBetween(from, to).map { entity ->
                    NeutralNewsBean(
                        id = entity.id,
                        neutralTitle = entity.neutralTitle,
                        neutralDescription = entity.neutralDescription,
                        category = entity.category.toString(),
                        imageUrl = entity.imageUrl,
                        group = entity.group!!,
                        date = if (entity.date != Long.MIN_VALUE) DateFormatterHelper.formatDateToSpanish(entity.date) else null,
                        createdAt = if (entity.createdAt != Long.MIN_VALUE) DateFormatterHelper.formatDateToSpanish(entity.createdAt) else null,
                        updatedAt = if (entity.updatedAt != Long.MIN_VALUE) DateFormatterHelper.formatDateToSpanish(entity.updatedAt) else null,
                        sourceIds = entity.sourceIds,
                        relevance = entity.relevance
                    )
                }
            }

            val sortedNeutral = neutralAccum.sortedByDescending { it.date ?: "" }
            if (sortedNeutral.isNotEmpty()) {
                paginationHelper.setCachedNeutralAll(sortedNeutral)
                val initialNeu2 = paginationHelper.prepareInitialDisplay(allNeutralNews)
                if (initialNeu2.isNotEmpty()) allNeutralNews.addAll(initialNeu2)
            }
        } catch (e: Exception) {
            Log.e("TodayFragmentVM", "Error loading from DB for date filter: ${e.localizedMessage}")
        }
    }

    fun loadForSelectedDates() {
        viewModelScope.launch {
            isPaginating.postValue(true)
            withContext(Dispatchers.IO) {
                loadNewsFromDbForDateFilter()
            }
            withContext(Dispatchers.Default) {
                applyFiltersAndSearch(force = true)
            }
            isPaginating.postValue(false)
        }
    }

    fun refreshNeutralNews() {
        Log.d("TodayFragmentVM", "Iniciando refreshNeutralNews() con SnapshotListener")
        snapshotManager.startNeutralListener(
            onChanges = { changes ->
                processSnapshotNeutralNewsDocuments(changes) { result ->
                    if (result) {
                        neutralNewsListenerCompleted = true
                        checkAllListenersCompleted()
                    }
                }
            },
            onError = { e ->
                Log.e("TodayFragmentVM", "Error en neutral listener: ${e.localizedMessage}")
                neutralNewsListenerCompleted = true
                checkAllListenersCompleted()
            }
        )
    }

    fun refreshNews() = refreshNeutralNews()

    private fun checkAllListenersCompleted() {
        if (newsListenerCompleted && neutralNewsListenerCompleted) {
            newsListenerCompleted = false
            neutralNewsListenerCompleted = false
            Log.d("TodayFragmentVM", "Todos los listeners han completado su carga inicial")
            applyFiltersAndSearch(force = true)
        }
    }

    private fun processSnapshotNeutralNewsDocuments(newDocuments: List<DocumentChange>, callback: (Boolean) -> Unit) {
        if (newDocuments.isEmpty()) {
            callback(false)
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val newsToAdd = newDocuments.mapNotNull { change ->
                try {
                    FirestoreDataMapper.mapNeutralNewsDocument(change.document.data, change.document.id)
                } catch (e: Exception) {
                    Log.e("processNewNeutralNewsDocuments", "Error: ${e.localizedMessage}")
                    null
                }
            }

            withContext(Dispatchers.Main) {
                val hasChanges = updateNeutralNewsList(newsToAdd)
                callback(hasChanges)
            }
        }
    }

    private fun updateNeutralNewsList(newsToAdd: List<NeutralNewsBean>): Boolean {
        if (newsToAdd.isEmpty()) return false

        val existingIds = allNeutralNews.map { it.id }.toSet()
        val brandNewNews = newsToAdd.filter { it.id !in existingIds }
        val updatedNews = newsToAdd.filter { it.id in existingIds }

        val verifiedUpdatedNews = updatedNews.filter { news ->
            val existing = allNeutralNews.find { it.id == news.id }
            existing?.let {
                it.neutralTitle != news.neutralTitle || it.neutralDescription != news.neutralDescription
            } ?: true
        }

        if (verifiedUpdatedNews.isNotEmpty()) {
            verifiedUpdatedNews.forEach { news ->
                val index = allNeutralNews.indexOfFirst { it.id == news.id }
                if (index >= 0) allNeutralNews[index] = news
            }
            viewModelScope.launch(Dispatchers.IO) {
                dataManager.saveNeutralNewsToCache(verifiedUpdatedNews)
            }
            Log.d("TodayFragmentVM", "Actualizadas ${verifiedUpdatedNews.size} noticias")
        }

        if (brandNewNews.isNotEmpty()) {
            allNeutralNews.addAll(0, brandNewNews)
            viewModelScope.launch(Dispatchers.IO) {
                dataManager.saveNeutralNewsToCache(brandNewNews)
            }
            Log.d("TodayFragmentVM", "Agregadas ${brandNewNews.size} noticias nuevas al inicio")
        }

        val hasChanges = brandNewNews.isNotEmpty() || verifiedUpdatedNews.isNotEmpty()
        if (hasChanges) {
            paginationHelper.setCachedNeutralAll(allNeutralNews.sortedByDescending { it.date ?: "" })
            applyFiltersAndSearch(force = true)
        }

        return hasChanges
    }

    override fun onCleared() {
        snapshotManager.stopAll()
        compositeDisposable.clear()
        super.onCleared()
    }

    // === Public API - Pagination ===

    fun loadNextPage() {
        if (isLoadingMoreItems) return
        viewModelScope.launch {
            isLoadingMoreItems = true
            isPaginating.postValue(true)

            val nextNeutral = withContext(Dispatchers.Default) {
                paginationHelper.loadNextPage(allNeutralNews)
            }

            if (nextNeutral.isNotEmpty()) {
                allNeutralNews.addAll(nextNeutral)
                Log.d("TodayFragmentVM", "Cargadas ${nextNeutral.size} noticias de paginación local")
            } else if (currentFilterData.selectedDates?.isNotEmpty() == true) {
                withContext(Dispatchers.IO) {
                    loadNewsFromDbForDateFilter()
                }
            } else {
                Log.d("TodayFragmentVM", "No hay más noticias para paginar")
            }

            // CRITICAL: Re-aplicar filtros para mostrar las nuevas noticias cargadas
            applyFiltersAndSearch(force = true)

            isLoadingMoreItems = false
            isPaginating.postValue(false)
        }
    }


    fun searchNews(query: String) {
        isLoading.postValue(true)
        viewModelScope.launch {
            try {
                searchQuery.postValue(query)
                applyFiltersAndSearch()
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun updateFilterData(filterData: LocalFilterBean) {
        currentFilterData = filterData
        applyFiltersAndSearch()
    }

    fun applyFilters(filterData: LocalFilterBean) {
        if (currentFilterData == filterData) {
            Log.d("TodayFragmentVM", "No se aplicaron cambios en los filtros")
            return
        }
        currentFilterData = filterData

        if (currentFilterData == LocalFilterBean()) {
            resetFilters()
        } else {
            // CRITICAL: Forzar re-aplicación de filtros y resetear paginación
            filtersApplied = false

            // Resetear allNeutralNews para que se muestren las primeras del filtro
            allNeutralNews.clear()
            val initial = paginationHelper.prepareInitialDisplay(allNeutralNews)
            if (initial.isNotEmpty()) {
                allNeutralNews.addAll(initial)
            }

            Log.d("TodayFragmentVM", "Filtros aplicados, mostrando primeras ${allNeutralNews.size} noticias")

            applyFiltersAndSearch(force = true)
        }
    }

    fun resetFilters() {
        filtersApplied = false
        currentFilterData = LocalFilterBean()

        // Resetear a las primeras noticias
        allNeutralNews.clear()
        val initial = paginationHelper.prepareInitialDisplay(allNeutralNews)
        if (initial.isNotEmpty()) {
            allNeutralNews.addAll(initial)
        }

        applyFiltersAndSearch(force = true)
    }

    fun reapplyFilters() {
        applyFiltersAndSearch(force = true)
    }

    val hasLoadedData: Boolean
        get() = allNeutralNews.isNotEmpty() || allNews.isNotEmpty()

    internal fun applyFiltersAndSearch(force: Boolean = false) {
        Log.d("TodayFragmentVM", "Iniciando applyFiltersAndSearch()")
        val query = searchQuery.value ?: ""

        // PASO 1: Aplicar filtros
        if (!filtersApplied || lastAppliedFilter != currentFilterData || force) {
            filteredNeutralNewsCache = allNeutralNews.toList()
            filteredRegularNewsCache = allNews.toList()

            if (currentFilterData != LocalFilterBean()) {
                val filterHelper = NewsFilterHelper(groupsOfNews)
                filteredNeutralNewsCache = filterHelper.applyFiltersToNeutralNews(filteredNeutralNewsCache, currentFilterData)

                val neutralGroups = filteredNeutralNewsCache.map { it.group }.toSet()
                filteredRegularNewsCache = filteredRegularNewsCache.filter { it.group in neutralGroups }
            }

            updateGroupsOfNews(filteredRegularNewsCache)
            filtersApplied = true
            lastAppliedFilter = currentFilterData.copy()
        }

        // PASO 2: Aplicar búsqueda y ordenación
        val filterHelper = NewsFilterHelper(groupsOfNews)
        val searchedNeutral = if (query.isEmpty()) {
            filteredNeutralNewsCache
        } else {
            filterHelper.searchInNeutralNews(filteredNeutralNewsCache, query)
        }

        // Aplicar ordenación
        val sortedNeutral = NewsSorter.sortNeutralNews(searchedNeutral, currentSortType.value ?: TodaySortType.DATE_DESC)

        val neutralGroupsFound = sortedNeutral.map { it.group }.toSet()
        val searchedRegular = filteredRegularNewsCache.filter { it.group in neutralGroupsFound }

        neutralNewsList.postValue(sortedNeutral)
        newsList.postValue(NewsSorter.filterGroupedNews(searchedRegular))
        showNoResults.postValue(sortedNeutral.isEmpty() && searchedRegular.isEmpty())

        Log.d("TodayFragmentVM", "Finalizado applyFiltersAndSearch()")
    }

    private fun updateGroupsOfNews(filteredNews: List<NewsBean>) {
        val newsGroups = filteredNews.groupBy { it.group ?: -1 }.filter { it.key >= 0 }
        groupsOfNews.clear()
        groupsOfNews.putAll(newsGroups)
    }

    fun getRelatedNews(from: NewsBean): List<NewsBean> = groupsOfNews[from.group] ?: emptyList()

    // === Public API - Firestore Operations ===

    internal suspend fun fetchNeutralNewsFromFirestoreSuspend(filterData: LocalFilterBean): Boolean {
        currentFilterData = filterData

        return withContext(Dispatchers.IO) {
            try {
                // Usar NewsDataManager para cargar solo noticias del día
                val fetchedNews = dataManager.fetchNeutralNewsFromFirestore()

                if (fetchedNews.isNotEmpty()) {
                    paginationHelper.setCachedNeutralAll(fetchedNews)
                    paginationHelper.sortCachedLists(currentSortType.value ?: TodaySortType.DATE_DESC)

                    allNeutralNews = mutableListOf()
                    val initialNeu = paginationHelper.prepareInitialDisplay(allNeutralNews)
                    if (initialNeu.isNotEmpty()) allNeutralNews.addAll(initialNeu)

                    dataLoaded = true
                    messageEvent.postValue("Noticias neutrales obtenidas. Total: ${fetchedNews.size}")

                    // Guardar en caché
                    dataManager.saveNeutralNewsToCache(fetchedNews)

                    // CRITICAL: Aplicar filtros para que se muestren las noticias
                    withContext(Dispatchers.Main) {
                        applyFiltersAndSearch(force = true)
                    }

                    true
                } else {
                    messageEvent.postValue("No se encontraron noticias para hoy")
                    showNoResults.postValue(true)
                    false
                }
            } catch (e: Exception) {
                messageEvent.postValue("Error: ${e.localizedMessage}")
                Log.e("TodayFragmentVM", "Error fetching news: ${e.localizedMessage}")
                false
            }
        }
    }

    fun fetchNeutralNewsFromFirestore(filterData: LocalFilterBean, continuation: Continuation<Boolean>? = null) {
        viewModelScope.launch {
            val result = fetchNeutralNewsFromFirestoreSuspend(filterData)
            continuation?.resume(result)
        }
    }

    internal suspend fun fetchNewsByGroupSuspend(groupId: Int, filterData: LocalFilterBean): Boolean {
        currentFilterData = filterData

        // Verificar si ya tenemos noticias de este grupo en caché
        val cachedGroupNews = groupsOfNews[groupId]
        if (cachedGroupNews != null && cachedGroupNews.isNotEmpty()) {
            Log.d("TodayFragmentVM", "Noticias del grupo $groupId ya en caché: ${cachedGroupNews.size}")

            // Verificar actualizaciones en segundo plano sin loading
            viewModelScope.launch(Dispatchers.IO) {
                verifyAndUpdateGroupNewsFromFirestore(groupId)
            }
            return true
        }

        // Si no está en caché, cargar desde Firestore
        return withContext(Dispatchers.IO) {
            try {
                val fetchedNews = dataManager.fetchNewsByGroup(groupId)

                if (fetchedNews.isNotEmpty()) {
                    // Actualizar allNews
                    val others = allNews.filter { it.group != groupId }
                    allNews.clear()
                    allNews.addAll(others)
                    allNews.addAll(fetchedNews)

                    paginationHelper.sortCachedLists(currentSortType.value ?: TodaySortType.DATE_DESC)

                    // Actualizar mapa de grupos
                    updateGroupsOfNews(allNews)

                    // Guardar en caché
                    dataManager.saveNewsToCache(fetchedNews)

                    // Actualizar UI
                    withContext(Dispatchers.Main) {
                        newsList.postValue(NewsSorter.filterGroupedNews(allNews))
                    }

                    messageEvent.postValue("Noticias del grupo $groupId cargadas: ${fetchedNews.size}")
                    Log.d("TodayFragmentVM", "Grupo $groupId cargado: ${fetchedNews.size} noticias")
                    true
                } else {
                    Log.d("TodayFragmentVM", "No se encontraron noticias para el grupo $groupId")
                    messageEvent.postValue("No se encontraron noticias para el grupo $groupId")
                    true
                }
            } catch (e: Exception) {
                messageEvent.postValue("Error: ${e.localizedMessage}")
                Log.e("TodayFragmentVM", "Error fetching group news: ${e.localizedMessage}")
                false
            }
        }
    }

    fun fetchNewsByGroup(groupId: Int, filterData: LocalFilterBean, continuation: Continuation<Boolean>? = null) {
        viewModelScope.launch {
            val result = fetchNewsByGroupSuspend(groupId, filterData)
            continuation?.resume(result)
        }
    }

    /**
     * Verifica actualizaciones de un grupo en segundo plano.
     */
    private suspend fun verifyAndUpdateGroupNewsFromFirestore(groupId: Int) {
        try {
            Log.d("TodayFragmentVM", "Verificando actualizaciones del grupo $groupId...")

            val currentGroupNews = allNews.filter { it.group == groupId }
            val result = dataManager.verifyGroupUpdatesFromFirestore(groupId, currentGroupNews)

            if (result.hasChanges) {
                Log.d("TodayFragmentVM", "Actualizaciones para grupo $groupId: ${result.newItems.size} nuevas, ${result.updatedItems.size} actualizadas")

                @Suppress("UNCHECKED_CAST")
                val updatedNews = result.updatedItems as List<NewsBean>
                updatedNews.forEach { updated ->
                    val index = allNews.indexOfFirst { it.id == updated.id }
                    if (index >= 0) allNews[index] = updated
                }

                @Suppress("UNCHECKED_CAST")
                val newNews = result.newItems as List<NewsBean>
                if (newNews.isNotEmpty()) {
                    allNews.addAll(newNews)
                }

                // Guardar en caché
                dataManager.saveNewsToCache(updatedNews + newNews)

                // Actualizar UI
                withContext(Dispatchers.Main) {
                    updateGroupsOfNews(allNews)
                    newsList.postValue(NewsSorter.filterGroupedNews(allNews))
                }
            } else {
                Log.d("TodayFragmentVM", "No hay actualizaciones para grupo $groupId")
            }
        } catch (e: Exception) {
            Log.e("TodayFragmentVM", "Error verificando grupo $groupId: ${e.localizedMessage}")
        }
    }

    fun updateSortType(sortType: TodaySortType) {
        Log.d("TodayFragmentVM", "Cambiando ordenación a: $sortType")
        currentSortType.value = sortType

        // CRITICAL: Reordenar TODAS las noticias en el helper
        paginationHelper.sortCachedLists(sortType)

        // CRITICAL: Resetear allNeutralNews y cargar las primeras 10 del NUEVO orden
        allNeutralNews.clear()
        val initial = paginationHelper.prepareInitialDisplay(allNeutralNews)
        if (initial.isNotEmpty()) {
            allNeutralNews.addAll(initial)
        }

        Log.d("TodayFragmentVM", "Mostrando primeras ${allNeutralNews.size} del nuevo orden: $sortType")

        // Aplicar filtros con las nuevas noticias ordenadas
        applyFiltersAndSearch(force = true)
    }
}

