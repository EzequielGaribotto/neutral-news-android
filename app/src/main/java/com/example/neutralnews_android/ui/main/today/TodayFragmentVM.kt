package com.example.neutralnews_android.ui.main.today

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.neutralnews_android.data.Constants.NeutralNew.NEUTRAL_NEWS
import com.example.neutralnews_android.data.Constants.News.DESCRIPTION
import com.example.neutralnews_android.data.Constants.News.GROUP
import com.example.neutralnews_android.data.Constants.News.NEWS
import com.example.neutralnews_android.data.bean.filter.LocalFilterBean
import com.example.neutralnews_android.data.bean.news.NeutralNewsBean
import com.example.neutralnews_android.data.bean.news.NewsBean
import com.example.neutralnews_android.data.bean.news.pressmedia.MediaBean
import com.example.neutralnews_android.data.remote.SnapshotManager
import com.example.neutralnews_android.data.room.AppDatabase
import com.example.neutralnews_android.data.room.dao.NeutralNewsDao
import com.example.neutralnews_android.data.room.dao.NewsDao
import com.example.neutralnews_android.data.room.entities.NeutralNewsEntity
import com.example.neutralnews_android.data.room.entities.NewsEntity
import com.example.neutralnews_android.di.event.SingleLiveEvent
import com.example.neutralnews_android.di.viewmodel.BaseViewModel
import com.example.neutralnews_android.util.Prefs
import com.example.neutralnews_android.util.string.normalized
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Extension function to convert Firebase Task to suspend function
 */
private suspend fun <T> Task<T>.await(): T = suspendCoroutine { continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener { exception ->
        throw exception
    }
}

@HiltViewModel
class TodayFragmentVM @Inject constructor(application: Application) : BaseViewModel(application) {
    private val pageSize = 10
    private var isLoadingMoreItems = false
    val isPaginating = SingleLiveEvent<Boolean>()
    private val paginationHelper = PaginationHelper(pageSize = pageSize, initialDisplayLimit = 15)
    private val groupsOfNews = mutableMapOf<Int, List<NewsBean>>()
    val messageEvent = SingleLiveEvent<String>()
    val isLoading = SingleLiveEvent<Boolean>()
    val neutralNewsDao: NeutralNewsDao
    private val newsDao: NewsDao
    val neutralNewsList = SingleLiveEvent<List<NeutralNewsBean>>()
    val newsList = SingleLiveEvent<List<List<NewsBean>>>()
    internal var allNeutralNews = mutableListOf<NeutralNewsBean>()
    internal var allNews = mutableListOf<NewsBean>()
    private var currentFilterData = LocalFilterBean()
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

    init {
        val database = AppDatabase.getDatabase(application)
        neutralNewsDao = database.neutralNewsDao()
        newsDao = database.newsDao()
    }

    // === Public API - Date Filter Management ===

    fun parseDateToTimestamp(dateString: String?): Long =
        DateFormatterHelper.parseDateToTimestamp(dateString)

    fun onDateFilterApplied(selectedList: List<Date>) {
        currentFilterData.selectedDates = if (selectedList.isEmpty()) null else selectedList
        DateFilterHelper.persistSelectedDates(selectedList)
        if (selectedList.isNotEmpty()) loadForSelectedDates()
        applyFiltersAndSearch(force = true)
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
            isLoading.postValue(true)
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val includeYesterday = currentHour < 12

            val dateRanges = buildInitialDateRanges(calendar, includeYesterday)
            loadNews(dateRanges)

            computeAvailableDaysIfNeeded()
            applyFiltersAndSearch()
            isLoading.postValue(false)
        }
    }

    private fun buildInitialDateRanges(calendar: Calendar, includeYesterday: Boolean): List<Pair<Long, Long>> {
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calendar.timeInMillis

        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val todayEnd = calendar.timeInMillis

        val ranges = mutableListOf(Pair(todayStart, todayEnd))

        if (includeYesterday) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val yesterdayStart = calendar.timeInMillis

            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val yesterdayEnd = calendar.timeInMillis
            ranges.add(Pair(yesterdayStart, yesterdayEnd))
        }

        return ranges
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
                coroutineScope {
                    val neutralLoaded = async { foundCachedNeutralNewsEntities() }.await()

                    if (!neutralLoaded) {
                        Log.d("TodayFragmentVM", "Iniciando carga de noticias neutrales desde Firestore")
                        async { fetchNeutralNewsFromFirestoreSuspend(filterData = currentFilterData) }.await()
                    } else {
                        Log.d("TodayFragmentVM", "Noticias neutrales cargadas desde caché")
                        // CRITICAL: Siempre verificar actualizaciones desde Firestore, incluso si hay caché
                        // Esto asegura que si abrimos la app después de un tiempo, las noticias se actualicen
                        verifyAndUpdateFromFirestore()
                        refreshNeutralNews()
                    }
                }
            } finally {
                Log.d("TodayFragmentVM", "Loaded news")
                isCacheLoading = false
            }
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

                val snapshot = FirebaseFirestore.getInstance()
                    .collection(NEUTRAL_NEWS)
                    .get()
                    .await()

                if (snapshot != null && !snapshot.isEmpty) {
                    val fetchedNews = snapshot.documents.mapNotNull { doc ->
                        FirestoreDataMapper.mapNeutralNewsDocument(doc.data, doc.id)
                    }

                    // Comparar y actualizar si hay cambios
                    val hasUpdates = updateNeutralNewsListSilently(fetchedNews)

                    if (hasUpdates) {
                        Log.d("TodayFragmentVM", "Se encontraron actualizaciones, refrescando UI")
                        withContext(Dispatchers.Main) {
                            applyFiltersAndSearch(force = true)
                        }
                    } else {
                        Log.d("TodayFragmentVM", "No hay actualizaciones desde Firestore")
                    }
                }
            } catch (e: Exception) {
                Log.e("TodayFragmentVM", "Error verificando actualizaciones: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Actualiza la lista de noticias sin mostrar indicadores de loading.
     * Retorna true si hubo cambios.
     */
    private fun updateNeutralNewsListSilently(fetchedNews: List<NeutralNewsBean>): Boolean {
        val existingIds = allNeutralNews.map { it.id }.toSet()
        val fetchedIds = fetchedNews.map { it.id }.toSet()

        // Noticias nuevas que no están en el caché
        val brandNewNews = fetchedNews.filter { it.id !in existingIds }

        // Noticias que existen pero pueden haber sido actualizadas
        val potentialUpdates = fetchedNews.filter { it.id in existingIds }

        val verifiedUpdates = potentialUpdates.filter { fetchedItem ->
            val existing = allNeutralNews.find { it.id == fetchedItem.id }
            existing?.let {
                it.neutralTitle != fetchedItem.neutralTitle ||
                it.neutralDescription != fetchedItem.neutralDescription ||
                it.date != fetchedItem.date ||
                it.updatedAt != fetchedItem.updatedAt
            } ?: false
        }

        var hasChanges = false

        // Actualizar noticias modificadas
        if (verifiedUpdates.isNotEmpty()) {
            verifiedUpdates.forEach { updated ->
                val index = allNeutralNews.indexOfFirst { it.id == updated.id }
                if (index >= 0) {
                    allNeutralNews[index] = updated
                }
            }
            saveNeutralNewsToLocalCache(verifiedUpdates)
            Log.d("TodayFragmentVM", "Actualizadas silenciosamente ${verifiedUpdates.size} noticias")
            hasChanges = true
        }

        // Agregar noticias nuevas al inicio
        if (brandNewNews.isNotEmpty()) {
            allNeutralNews.addAll(0, brandNewNews)
            saveNeutralNewsToLocalCache(brandNewNews)
            Log.d("TodayFragmentVM", "Agregadas silenciosamente ${brandNewNews.size} noticias nuevas")
            hasChanges = true
        }

        // Reordenar si hubo cambios
        if (hasChanges) {
            paginationHelper.setCachedNeutralAll(allNeutralNews.sortedByDescending { it.date ?: "" })
        }

        return hasChanges
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

    fun refreshNews() {
        Log.d("TodayFragmentVM", "Iniciando refreshNews() con SnapshotManager")
        refreshNeutralNews()
    }

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
                if (index >= 0) {
                    allNeutralNews[index] = news
                }
            }
            saveNeutralNewsToLocalCache(verifiedUpdatedNews)
            Log.d("TodayFragmentVM", "Actualizadas ${verifiedUpdatedNews.size} noticias")
        }

        if (brandNewNews.isNotEmpty()) {
            // IMPORTANT: Insertar nuevas noticias AL PRINCIPIO para que aparezcan arriba
            allNeutralNews.addAll(0, brandNewNews)
            saveNeutralNewsToLocalCache(brandNewNews)
            Log.d("TodayFragmentVM", "Agregadas ${brandNewNews.size} noticias nuevas al inicio")
        }

        // Re-aplicar ordenación y filtros para reflejar cambios
        val hasChanges = brandNewNews.isNotEmpty() || verifiedUpdatedNews.isNotEmpty()
        if (hasChanges) {
            // Reordenar en el helper también
            paginationHelper.setCachedNeutralAll(allNeutralNews.sortedByDescending { it.date ?: "" })
            applyFiltersAndSearch(force = true)
        }

        return hasChanges
    }

    private fun saveNeutralNewsToLocalCache(newsList: List<NeutralNewsBean>) {
        if (newsList.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                newsList.chunked(50).forEach { batch ->
                    val entities = batch.mapNotNull { news ->
                        try {
                            NeutralNewsEntity(
                                id = news.id,
                                neutralTitle = news.neutralTitle,
                                neutralDescription = news.neutralDescription,
                                category = news.category,
                                imageUrl = news.imageUrl,
                                group = news.group,
                                createdAt = DateFormatterHelper.getDateForSorting(news.createdAt)?.time ?: 0L,
                                date = DateFormatterHelper.getDateForSorting(news.date)?.time ?: 0L,
                                updatedAt = DateFormatterHelper.getDateForSorting(news.updatedAt)?.time ?: 0L,
                                sourceIds = news.sourceIds,
                                imageMedium = news.imageMedium,
                                relevance = news.relevance
                            )
                        } catch (e: Exception) {
                            Log.e("DateDebug", "Error en entidad: ${e.message}")
                            null
                        }
                    }

                    if (entities.isNotEmpty()) {
                        neutralNewsDao.insertAll(entities)
                    }
                }
            } catch (e: Exception) {
                Log.e("DateDebug", "Error saving to cache: ${e.message}", e)
            }
        }
    }

    private fun saveNewsToLocalCache(newsList: List<NewsBean>) {
        viewModelScope.launch(Dispatchers.IO) {
            newsList.chunked(50).forEach { batch ->
                val entities = batch.map { news ->
                    NewsEntity(
                        id = news.id,
                        title = news.title ?: "",
                        description = news.description ?: "",
                        category = news.category ?: "",
                        imageUrl = news.imageUrl ?: "",
                        link = news.link,
                        createdAt = DateFormatterHelper.getDateForSorting(news.createdAt)?.time ?: Long.MIN_VALUE,
                        pubDate = DateFormatterHelper.getDateForSorting(news.pubDate)?.time ?: Long.MIN_VALUE,
                        updatedAt = DateFormatterHelper.getDateForSorting(news.updatedAt)?.time ?: Long.MIN_VALUE,
                        group = news.group,
                        neutralScore = news.neutralScore,
                        sourceMediumName = news.sourceMedium?.name?.normalized() ?: ""
                    )
                }
                if (entities.isNotEmpty()) {
                    newsDao.insertAll(entities)
                }
            }
        }
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
            } else {
                // Si no hay más en la paginación local, intentar cargar desde DB si hay filtro de fechas
                if (currentFilterData.selectedDates?.isNotEmpty() == true) {
                    withContext(Dispatchers.IO) {
                        loadNewsFromDbForDateFilter()
                    }
                } else {
                    Log.d("TodayFragmentVM", "No hay más noticias para paginar")
                }
            }

            // Re-aplicar filtros y búsqueda para que aparezcan las noticias
            // Esto es CRÍTICO para búsquedas: si el usuario busca algo que no está visible,
            // necesitamos mostrarlo ahora
            withContext(Dispatchers.Default) {
                applyFiltersAndSearch(force = true)
            }

            isLoadingMoreItems = false
            isPaginating.postValue(false)
        }
    }

    private fun loadNewsFromDbForDateFilter() {
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

    private suspend fun foundCachedNeutralNewsEntities(): Boolean {
        val entities = neutralNewsDao.getAllNews().firstOrNull() ?: emptyList()
        return if (entities.isNotEmpty()) {
            val mappedAll = entities.map { entity ->
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
            }.sortedByDescending { it.date ?: "" }

            paginationHelper.setCachedNeutralAll(mappedAll)
            allNeutralNews = mutableListOf()
            val initialNeu = paginationHelper.prepareInitialDisplay(allNeutralNews)
            if (initialNeu.isNotEmpty()) allNeutralNews.addAll(initialNeu)

            dataLoaded = true
            messageEvent.postValue("Noticias neutrales cargadas desde caché: ${allNeutralNews.size}")

            // CRITICAL: Aplicar filtros y búsqueda para que se muestren las noticias
            withContext(Dispatchers.Main) {
                applyFiltersAndSearch(force = true)
            }

            // También cargar noticias relacionadas del caché
            loadCachedRelatedNews()

            true
        } else {
            messageEvent.postValue("Sin noticias neutrales en caché")
            false
        }
    }

    private suspend fun loadCachedRelatedNews() {
        try {
            val regularNewsEntities = newsDao.getAllNews()
            if (regularNewsEntities.isNotEmpty()) {
                val mappedRegularNews = regularNewsEntities.map { entity ->
                    val sourceMedium = MediaBean.entries.find {
                        it.pressMedia.name?.normalized() == entity.sourceMediumName
                    }?.pressMedia

                    NewsBean(
                        id = entity.id,
                        title = entity.title,
                        description = entity.description,
                        category = entity.category,
                        imageUrl = entity.imageUrl,
                        link = entity.link,
                        createdAt = if (entity.createdAt != Long.MIN_VALUE) DateFormatterHelper.formatDateToSpanish(entity.createdAt) else null,
                        pubDate = if (entity.pubDate != Long.MIN_VALUE) DateFormatterHelper.formatDateToSpanish(entity.pubDate) else null,
                        date = if (entity.pubDate != Long.MIN_VALUE) DateFormatterHelper.formatDateToSpanish(entity.pubDate) else null,
                        updatedAt = if (entity.updatedAt != Long.MIN_VALUE) DateFormatterHelper.formatDateToSpanish(entity.updatedAt) else null,
                        sourceMedium = sourceMedium,
                        group = entity.group,
                        neutralScore = entity.neutralScore
                    )
                }

                allNews.clear()
                allNews.addAll(mappedRegularNews)
                updateGroupsOfNews(allNews)

                Log.d("TodayFragmentVM", "Noticias relacionadas cargadas desde caché: ${allNews.size}")
            }
        } catch (e: Exception) {
            Log.e("TodayFragmentVM", "Error loading cached related news: ${e.localizedMessage}")
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

    // === Public API - Search and Filter ===

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
            applyFiltersAndSearch()
        }
    }

    fun resetFilters() {
        filtersApplied = false
        currentFilterData = LocalFilterBean()
        applyFiltersAndSearch()
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
        return suspendCoroutine { continuation ->
            fetchNeutralNewsFromFirestore(filterData, continuation)
        }
    }

    fun fetchNeutralNewsFromFirestore(filterData: LocalFilterBean, continuation: Continuation<Boolean>? = null) {
        if (allNews.isNotEmpty()) {
            messageEvent.postValue("Usando noticias en caché")
            continuation?.resume(true)
            return
        }

        currentFilterData = filterData

        FirebaseFirestore.getInstance().collection(NEUTRAL_NEWS).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val fetchedNeutralNews = snapshot.documents.mapNotNull { doc ->
                        FirestoreDataMapper.mapNeutralNewsDocument(doc.data, doc.id)
                    }

                    val sortedAll = fetchedNeutralNews.sortedByDescending { it.date ?: "" }
                    paginationHelper.setCachedNeutralAll(sortedAll)
                    paginationHelper.sortCachedLists(currentSortType.value ?: TodaySortType.DATE_DESC)

                    allNeutralNews = mutableListOf()
                    val initialNeu = paginationHelper.prepareInitialDisplay(allNeutralNews)
                    if (initialNeu.isNotEmpty()) allNeutralNews.addAll(initialNeu)

                    dataLoaded = true
                    messageEvent.postValue("Noticias neutrales obtenidas con éxito. Total: ${sortedAll.size}")
                    saveNeutralNewsToLocalCache(fetchedNeutralNews)

                    // CRITICAL: Aplicar filtros para que se muestren las noticias
                    applyFiltersAndSearch(force = true)

                    continuation?.resume(true)
                } else {
                    messageEvent.postValue("No se encontraron noticias neutrales en Firestore")
                    showNoResults.postValue(searchQuery.value.isNullOrEmpty())
                    continuation?.resume(true)
                }
            }
            .addOnFailureListener { e ->
                messageEvent.postValue("Error al obtener noticias neutrales: ${e.localizedMessage}")
                continuation?.resume(false)
            }
    }

    internal suspend fun fetchNewsByGroupSuspend(groupId: Int, filterData: LocalFilterBean): Boolean {
        return suspendCoroutine { continuation ->
            fetchNewsByGroup(groupId, filterData, continuation)
        }
    }

    fun fetchNewsByGroup(groupId: Int, filterData: LocalFilterBean, continuation: Continuation<Boolean>? = null) {
        Log.d("TodayFragmentVM", "fetchNewsByGroup: verificando grupo $groupId")

        currentFilterData = filterData

        // Primero verificar si ya tenemos noticias de este grupo en caché
        val cachedGroupNews = groupsOfNews[groupId]
        if (cachedGroupNews != null && cachedGroupNews.isNotEmpty()) {
            Log.d("TodayFragmentVM", "Noticias del grupo $groupId ya en caché: ${cachedGroupNews.size}")
            continuation?.resume(true)

            // IMPORTANT: Aunque estén en caché, verificar actualizaciones en segundo plano sin loading
            viewModelScope.launch(Dispatchers.IO) {
                verifyAndUpdateGroupNewsFromFirestore(groupId)
            }
            return
        }

        // Si no está en caché, cargar desde Firestore
        Log.d("TodayFragmentVM", "Cargando noticias del grupo $groupId desde Firestore")
        FirebaseFirestore.getInstance().collection(NEWS)
            .whereEqualTo(GROUP, groupId)
            .whereNotEqualTo(DESCRIPTION, "")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val fetchedNews = snapshot.documents.mapNotNull { doc ->
                        FirestoreDataMapper.mapNewsDocument(doc.data ?: return@mapNotNull null, doc.id)
                    }

                    if (fetchedNews.isNotEmpty()) {
                        // Eliminar entradas previas de ese grupo
                        val others = allNews.filter { it.group != groupId }
                        allNews.clear()
                        allNews.addAll(others)
                        allNews.addAll(fetchedNews)

                        paginationHelper.sortCachedLists(currentSortType.value ?: TodaySortType.DATE_DESC)

                        // CRITICAL: Actualizar mapa de grupos
                        updateGroupsOfNews(allNews)

                        // Actualizar UI con noticias agrupadas
                        newsList.postValue(NewsSorter.filterGroupedNews(allNews))

                        // Guardar en caché local
                        saveNewsToLocalCache(fetchedNews)

                        messageEvent.postValue("Noticias del grupo $groupId cargadas: ${fetchedNews.size}")
                        Log.d("TodayFragmentVM", "Grupo $groupId cargado exitosamente con ${fetchedNews.size} noticias")
                    }
                    continuation?.resume(true)
                } else {
                    Log.d("TodayFragmentVM", "No se encontraron noticias para el grupo $groupId")
                    messageEvent.postValue("No se encontraron noticias para el grupo $groupId")
                    continuation?.resume(true)
                }
            }
            .addOnFailureListener { e ->
                messageEvent.postValue("Error al obtener noticias del grupo $groupId: ${e.localizedMessage}")
                Log.e("TodayFragmentVM", "Error fetching group news: ${e.localizedMessage}")
                continuation?.resume(false)
            }
    }

    /**
     * Verifica actualizaciones de noticias de un grupo específico desde Firestore.
     * Se ejecuta en segundo plano sin mostrar loading indicators.
     */
    private suspend fun verifyAndUpdateGroupNewsFromFirestore(groupId: Int) {
        try {
            Log.d("TodayFragmentVM", "Verificando actualizaciones del grupo $groupId desde Firestore...")

            val snapshot = FirebaseFirestore.getInstance()
                .collection(NEWS)
                .whereEqualTo(GROUP, groupId)
                .whereNotEqualTo(DESCRIPTION, "")
                .get()
                .await()

            if (snapshot != null && !snapshot.isEmpty) {
                val fetchedNews = snapshot.documents.mapNotNull { doc ->
                    FirestoreDataMapper.mapNewsDocument(doc.data ?: return@mapNotNull null, doc.id)
                }

                val hasUpdates = updateGroupNewsListSilently(groupId, fetchedNews)

                if (hasUpdates) {
                    Log.d("TodayFragmentVM", "Se encontraron actualizaciones para grupo $groupId, refrescando UI")
                    withContext(Dispatchers.Main) {
                        updateGroupsOfNews(allNews)
                        newsList.postValue(NewsSorter.filterGroupedNews(allNews))
                    }
                } else {
                    Log.d("TodayFragmentVM", "No hay actualizaciones para grupo $groupId")
                }
            }
        } catch (e: Exception) {
            Log.e("TodayFragmentVM", "Error verificando actualizaciones del grupo $groupId: ${e.localizedMessage}")
        }
    }

    /**
     * Actualiza las noticias de un grupo sin mostrar indicadores de loading.
     * Retorna true si hubo cambios.
     */
    private fun updateGroupNewsListSilently(groupId: Int, fetchedNews: List<NewsBean>): Boolean {
        val existingGroupNews = allNews.filter { it.group == groupId }
        val existingIds = existingGroupNews.map { it.id }.toSet()

        // Noticias nuevas que no están en el caché
        val brandNewNews = fetchedNews.filter { it.id !in existingIds }

        // Noticias que existen pero pueden haber sido actualizadas
        val potentialUpdates = fetchedNews.filter { it.id in existingIds }

        val verifiedUpdates = potentialUpdates.filter { fetchedItem ->
            val existing = existingGroupNews.find { it.id == fetchedItem.id }
            existing?.let {
                it.title != fetchedItem.title ||
                it.description != fetchedItem.description ||
                it.pubDate != fetchedItem.pubDate ||
                it.updatedAt != fetchedItem.updatedAt
            } ?: false
        }

        var hasChanges = false

        // Actualizar noticias modificadas
        if (verifiedUpdates.isNotEmpty()) {
            verifiedUpdates.forEach { updated ->
                val index = allNews.indexOfFirst { it.id == updated.id }
                if (index >= 0) {
                    allNews[index] = updated
                }
            }
            saveNewsToLocalCache(verifiedUpdates)
            Log.d("TodayFragmentVM", "Actualizadas silenciosamente ${verifiedUpdates.size} noticias del grupo $groupId")
            hasChanges = true
        }

        // Agregar noticias nuevas
        if (brandNewNews.isNotEmpty()) {
            allNews.addAll(brandNewNews)
            saveNewsToLocalCache(brandNewNews)
            Log.d("TodayFragmentVM", "Agregadas silenciosamente ${brandNewNews.size} noticias nuevas al grupo $groupId")
            hasChanges = true
        }

        return hasChanges
    }

    fun updateSortType(sortType: TodaySortType) {
        currentSortType.value = sortType
        paginationHelper.sortCachedLists(sortType)
        applyFiltersAndSearch(force = true)
    }
}

