package com.example.neutralnews_android.ui.main.today

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.neutralnews_android.data.Constants.NeutralNew.DATE
import com.example.neutralnews_android.data.Constants.NeutralNew.IMAGE_MEDIUM
import com.example.neutralnews_android.data.Constants.NeutralNew.NEUTRAL_DESCRIPTION
import com.example.neutralnews_android.data.Constants.NeutralNew.NEUTRAL_NEWS
import com.example.neutralnews_android.data.Constants.NeutralNew.NEUTRAL_TITLE
import com.example.neutralnews_android.data.Constants.NeutralNew.SOURCE_IDS
import com.example.neutralnews_android.data.Constants.News.CATEGORY
import com.example.neutralnews_android.data.Constants.News.DESCRIPTION
import com.example.neutralnews_android.data.Constants.News.GROUP
import com.example.neutralnews_android.data.Constants.News.IMAGE_URL
import com.example.neutralnews_android.data.Constants.News.LINK
import com.example.neutralnews_android.data.Constants.News.NEWS
import com.example.neutralnews_android.data.Constants.News.PUB_DATE
import com.example.neutralnews_android.data.Constants.News.SCRAPED_DESCRIPTION
import com.example.neutralnews_android.data.Constants.News.SOURCE_MEDIUM
import com.example.neutralnews_android.data.Constants.News.TITLE
import com.example.neutralnews_android.data.Constants.News.CREATED_AT
import com.example.neutralnews_android.data.Constants.News.RELEVANCE
import com.example.neutralnews_android.data.Constants.News.NEUTRAL_SCORE
import com.example.neutralnews_android.data.Constants.News.UPDATED_AT
import com.example.neutralnews_android.data.Constants.DateFormat.DATE_FORMAT
import com.example.neutralnews_android.data.bean.filter.LocalFilterBean
import com.example.neutralnews_android.data.bean.news.NeutralNewsBean
import com.example.neutralnews_android.data.bean.news.NewsBean
import com.example.neutralnews_android.data.bean.news.pressmedia.MediaBean
import com.example.neutralnews_android.data.room.AppDatabase
import com.example.neutralnews_android.data.room.dao.NeutralNewsDao
import com.example.neutralnews_android.data.room.dao.NewsDao
import com.example.neutralnews_android.data.room.entities.NeutralNewsEntity
import com.example.neutralnews_android.data.room.entities.NewsEntity
import com.example.neutralnews_android.di.event.SingleLiveEvent
import com.example.neutralnews_android.di.viewmodel.BaseViewModel
import com.example.neutralnews_android.util.string.normalized
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.text.format
// concurrent hash map
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.category

/**
 * ViewModel para el fragmento de noticias de hoy.
 *
 * Esta versión está adaptada para obtener noticias desde Firestore e incluye funcionalidad de búsqueda.
 */
@HiltViewModel
class TodayFragmentVM @Inject constructor(application: Application) : BaseViewModel(application) {
    // Variables para paginación
    private val pageSize = 10
    private var lastLoadedTimestamp: Timestamp? = null
    private var isLoadingFirstPage = false
    private var isLoadingMoreItems = false
    private var hasMoreItems = true
    val isPaginating =
        SingleLiveEvent<Boolean>() // Para mostrar indicador de carga al final de la lista


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

    init {
        val database = AppDatabase.getDatabase(application)
        neutralNewsDao = database.neutralNewsDao()
        newsDao = database.newsDao()
    }


    fun setInitialData() {
        viewModelScope.launch {
            isLoading.postValue(true)
            loadNews()
            applyFiltersAndSearch()
            isLoading.postValue(false)
        }
    }

    private var isCacheLoading = false
    private var isFirestoreLoading = false
    private var dataLoaded = false

    /**
     * Carga datos desde la caché local.
     *
     * @return True si hay datos en caché no vacíos, False en caso contrario.
     */
    private suspend fun loadNews() {
        if (isCacheLoading) return
        isCacheLoading = true

        withContext(Dispatchers.IO) {
            Log.d("loadNews", "Iniciando carga desde caché local")
            messageEvent.postValue("Verificando caché local...")

            try {
                coroutineScope {
                    val regularNewsDeferred = async<Boolean> {
                        foundCachedRegularNewsEntities()
                    }

                    val neutralNewsDeferred = async<Boolean> {
                        foundCachedNeutralNewsEntities()
                    }

                    var regularLoaded = regularNewsDeferred.await()
                    var neutralLoaded = neutralNewsDeferred.await()

                    // Crear los jobs en paralelo sin esperar inmediatamente
                    val regularFirestoreJob = if (!regularLoaded) {
                        Log.d("loadNews", "Iniciando carga de noticias regulares desde Firestore")
                        async {
                            fetchNewsFromFirestoreSuspend(filterData = currentFilterData)
                            true
                        }
                    } else null

                    val neutralFirestoreJob = if (!neutralLoaded) {
                        Log.d("loadNews", "Iniciando carga de noticias neutrales desde Firestore")
                        async {
                            fetchNeutralNewsFromFirestoreSuspend(filterData = currentFilterData)
                            true
                        }
                    } else null

                    // Esperar a que ambos terminen
                    if (regularFirestoreJob != null) {
                        regularLoaded = regularFirestoreJob.await()
                        Log.d("loadNews", "Noticias regulares cargadas desde Firestore")
                    } else {
                        Log.d("loadNews", "Noticias regulares cargadas desde caché")
                        refreshRegularNews()
                    }

                    if (neutralFirestoreJob != null) {
                        neutralLoaded = neutralFirestoreJob.await()
                        Log.d("loadNews", "Noticias neutrales cargadas desde Firestore")
                    } else {
                        Log.d("loadNews", "Noticias neutrales cargadas desde caché")
                        refreshNeutralNews()
                    }

                }
            } finally {
                Log.d("loadNews", "Loaded news")
                isCacheLoading = false
            }
        }

    }

    fun refreshRegularNews() {
        Log.d("refreshRegularNews", "Iniciando refreshRegularNews() con SnapshotListener")
        newsListenerCompleted = false
        newsSnapshotListener?.remove()
        newsSnapshotListener = null
        setupNewsListener()
    }

    fun refreshNeutralNews() {
        Log.d("refreshNeutralNews", "Iniciando refreshNeutralNews() con SnapshotListener")
        neutralNewsListenerCompleted = false
        neutralNewsSnapshotListener?.remove()
        neutralNewsSnapshotListener = null
        setupNeutralNewsListener()
    }

    // Añade estas variables para mantener referencia a los listeners
    private var newsSnapshotListener: ListenerRegistration? = null
    private var neutralNewsSnapshotListener: ListenerRegistration? = null
    // Variables para controlar si los listeners han completado su carga inicial
    private var newsListenerCompleted = false
    private var neutralNewsListenerCompleted = false

    // Reemplaza refreshNews con esta implementación
    fun refreshNews() {
        Log.d("refreshNews", "Iniciando refreshNews() con SnapshotListener")

        refreshNeutralNews()
        refreshRegularNews()
    }

    private fun setupNewsListener() {
        val db = FirebaseFirestore.getInstance()
        // Usar un timestamp reciente como punto de partida
        val startTime = Timestamp(System.currentTimeMillis()/1000 - 86400*3, 0)

        // Consulta optimizada: solo documentos recientes y limitados
        val query = db.collection(NEWS)
            .whereGreaterThan(CREATED_AT, startTime)
            .whereNotEqualTo(DESCRIPTION,"")
            .whereGreaterThan(GROUP,0)

        newsSnapshotListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("setupNewsListener", "Error en listener de noticias: ${e.localizedMessage}")
                newsListenerCompleted = true
                checkAllListenersCompleted()
                return@addSnapshotListener
            }

            // Procesar cambios eficientemente
            if (snapshot != null && !snapshot.isEmpty) {
                val changes = snapshot.documentChanges
                val filteredChanges = changes.filter { it.type != DocumentChange.Type.REMOVED }

                if (filteredChanges.isNotEmpty()) {
                    // NO marcar como completado hasta que termine el procesamiento
                    processSnapshotNewsDocuments(filteredChanges) { result ->
                        // Solo ejecutar esto cuando el procesamiento asíncrono esté completo
                        if (result || neutralNewsListenerCompleted) {
                            newsListenerCompleted = true
                            Log.d("setupNewsListener", "Cambios significativos en noticias")
                            checkAllListenersCompleted()
                        } else {
                            Log.d("setupNewsListener", "No se encontraron cambios significativos en noticias")
                        }
                    }
                } else {
                    Log.d("setupNewsListener", "No hay cambios en las noticias")
                }
            } else {
                Log.d("setupNewsListener", "No hay cambios en las noticias")
            }
        }
    }

    private fun setupNeutralNewsListener() {
        val db = FirebaseFirestore.getInstance()
        // Usar un timestamp reciente como punto de partida
        val startTime = Timestamp(System.currentTimeMillis()/1000 - 86400 * 3, 0)

        // Consulta optimizada
        val query = db.collection(NEUTRAL_NEWS)
            .whereGreaterThan(UPDATED_AT, startTime)
            .whereNotEqualTo(NEUTRAL_DESCRIPTION,"")

        neutralNewsSnapshotListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("setupNeutralNewsListener", "Error en listener de noticias neutrales: ${e.localizedMessage}")
                neutralNewsListenerCompleted = true
                checkAllListenersCompleted()
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val changes = snapshot.documentChanges
                val filteredChanges = changes.filter { it.type != DocumentChange.Type.REMOVED }

                if (filteredChanges.isNotEmpty()) {
                    // NO marcar como completado hasta que termine el procesamiento
                    processSnapshotNeutralNewsDocuments(filteredChanges) { result ->
                        // Solo ejecutar esto cuando el procesamiento asíncrono esté completo
                        if (result || newsListenerCompleted) {
                            Log.d("setupNeutralNewsListener", "Cambios significativos en noticias neutrales")
                            neutralNewsListenerCompleted = true
                            checkAllListenersCompleted()
                        } else {
                            Log.d("setupNeutralNewsListener", "No se encontraron cambios significativos en noticias neutrales")
                        }
                    }
                } else {
                    Log.d("setupNeutralNewsListener", "No hay cambios en las noticias neutrales")
                }
            } else {
                Log.d("setupNeutralNewsListener", "No hay cambios en las noticias neutrales")
            }
        }
    }

    /**
     * Verifica si todos los listeners han completado su procesamiento inicial
     * y aplica filtros y búsqueda si es así.
     */
    private fun checkAllListenersCompleted() {
        if (newsListenerCompleted && neutralNewsListenerCompleted) {
            newsListenerCompleted = false
            neutralNewsListenerCompleted = false
            Log.d("checkAllListenersCompleted", "Todos los listeners han completado su carga inicial")
            applyFiltersAndSearch(force = true)
        } else {
            Log.d("checkAllListenersCompleted", "Esperando a que los listeners completen su carga")
        }
    }

    /**
     * Procesa los documentos de noticias neutrales nuevos o modificados.
     *
     * @param newDocuments Lista de cambios de documentos.
     */
    private fun processSnapshotNeutralNewsDocuments(newDocuments: List<DocumentChange>, callback: (Boolean) -> Unit) {
        val changesByType = newDocuments.groupBy { it.type }
        Log.d("processSnapshotNeutralNewsDocuments", "Cambios detectados (noticias neutrales): ${newDocuments.size}, por tipo: " +
                "ADDED=${changesByType[DocumentChange.Type.ADDED]?.size ?: 0}, " +
                "MODIFIED=${changesByType[DocumentChange.Type.MODIFIED]?.size ?: 0}, " +
                "REMOVED=${changesByType[DocumentChange.Type.REMOVED]?.size ?: 0}")

        val addedOrModifiedDocs = newDocuments
            .filter { it.type == DocumentChange.Type.ADDED || it.type == DocumentChange.Type.MODIFIED }

        if (addedOrModifiedDocs.isEmpty()) {
            callback(false)
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val newsToAdd = addedOrModifiedDocs.mapNotNull { change ->
                try {
                    val doc = change.document
                    processNeutralNewDocument(doc.data, doc.id)
                } catch (e: Exception) {
                    Log.e("processNewNeutralNewsDocuments", "Error: ${e.localizedMessage}")
                    null
                }
            }

            withContext(Dispatchers.Main) {
                if (newsToAdd.isNotEmpty()) {
                    val existingIds = allNeutralNews.map { it.id }.toSet()
                    val brandNewNews = newsToAdd.filter { it.id !in existingIds }
                    val updatedNews = newsToAdd.filter { it.id in existingIds }
                    var verifiedUpdatedNews: List<NeutralNewsBean> = emptyList()
                    if (updatedNews.isNotEmpty()) {
                        Log.d("processNewNeutralNewsDocuments", "Verificando cambios en ${updatedNews.size} noticias neutrales")
                        verifiedUpdatedNews = updatedNews.filter { news ->
                            val index = allNeutralNews.indexOfFirst { it.id == news.id }
                            if (index >= 0) {
                                val existingNews = allNeutralNews[index]
                                existingNews.neutralTitle != news.neutralTitle ||
                                        existingNews.neutralDescription != news.neutralDescription
                            } else {
                                true
                            }
                        }

                        if (verifiedUpdatedNews.isNotEmpty()) {
                            Log.d("processNewNeutralNewsDocuments", "Actualizando ${verifiedUpdatedNews.size} noticias neutrales con cambios reales")
                            verifiedUpdatedNews.forEach { news ->
                                val index = allNeutralNews.indexOfFirst { it.id == news.id }
                                allNeutralNews[index] = news
                                Log.d("processNewNeutralNewsDocuments", "Noticia neutral actualizada: ${news.id}, título: ${news.neutralTitle}")
                            }
                            Log.d("processNewNeutralNewsDocuments", "Actualizando ${verifiedUpdatedNews.size} en caché")
                            saveNeutralNewsToLocalCache(verifiedUpdatedNews)
                        }
                    }

                    if (brandNewNews.isNotEmpty()) {
                        allNeutralNews.addAll(brandNewNews)
                        Log.d("processNewNeutralNewsDocuments", "Guardando ${brandNewNews.size} noticias neutrales en caché")
                        saveNeutralNewsToLocalCache(brandNewNews)
                    }

                    val hasSignificantChanges = brandNewNews.isNotEmpty() || verifiedUpdatedNews.isNotEmpty()

                    callback(hasSignificantChanges)
                }
            }
        }
    }

    fun processNeutralNewDocument(data: Map<String, Any>?, docId: String): NeutralNewsBean? {
        if (data == null) return null

        val neutralTitle = data[NEUTRAL_TITLE] as? String ?: return null
        val neutralDescription = data[NEUTRAL_DESCRIPTION] as? String ?: return null
        val group = data[GROUP] as? Long ?: return null
        val category = data[CATEGORY] as? String ?: return null
        val imageUrl = data[IMAGE_URL] as? String
        val createdAt = data[CREATED_AT] as? Timestamp
        val date = data[DATE] as? Timestamp
        val updatedAt = data[UPDATED_AT] as? Timestamp
        val sourceIds = data[SOURCE_IDS] as? List<String>
        val relevance = data[RELEVANCE] as? Double ?: (data[RELEVANCE] as? Long)?.toDouble()
        val imageMediumRaw = data[IMAGE_MEDIUM] as? String
        val formattedCreatedAt = formatDateToSpanish(createdAt)
        val formattedDate = formatDateToSpanish(date)
        val formattedUpdatedAt = formatDateToSpanish(updatedAt)
        val imageMedium = MediaBean.entries.find {
            it.pressMedia.name?.normalized() == imageMediumRaw?.normalized()
        }?.pressMedia?.name

        if (imageMediumRaw == null) {
            Log.e(
                "processNewDocument",
                "sourceMedium not found for: $imageMediumRaw"
            )
            return null
        }
        return NeutralNewsBean(
            id = docId,
            neutralTitle = neutralTitle,
            neutralDescription = neutralDescription,
            group = group.toInt(),
            category = category,
            imageUrl = imageUrl,
            createdAt = formattedCreatedAt,
            date = formattedDate,
            updatedAt = formattedUpdatedAt,
            sourceIds = sourceIds,
            relevance = relevance,
            imageMedium = imageMedium
        )
    }

    /**
     * Guarda las noticias neutrales en caché local.
     *
     * @param newsList Lista de noticias neutrales a guardar.
     */

    private fun saveNeutralNewsToLocalCache(newsList: List<NeutralNewsBean>) {
        if (newsList.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Convertir en lotes de 50 para operaciones más eficientes
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
                                createdAt = getDateForSorting(news.createdAt)?.time ?: 0L,
                                date = getDateForSorting(news.date)?.time ?: 0L,
                                updatedAt = getDateForSorting(news.updatedAt)?.time ?: 0L,
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

    /**
     * Guarda las noticias en caché local.
     *
     * @param newsList Lista de noticias a guardar.
     */

    private fun saveNewsToLocalCache(newsList: List<NewsBean>) {
        viewModelScope.launch(Dispatchers.IO) {
            newsList.chunked(50).forEach { batch ->
                val entities = newsList.map { news ->
                    NewsEntity(
                        id = news.id,
                        title = news.title ?: "",
                        description = news.description ?: "",
                        category = news.category ?: "",
                        imageUrl = news.imageUrl ?: "",
                        link = news.link,
                        createdAt = getDateForSorting(news.createdAt)?.time ?: Long.MIN_VALUE,
                        pubDate = getDateForSorting(news.pubDate)?.time ?: Long.MIN_VALUE,
                        updatedAt = getDateForSorting(news.updatedAt)?.time ?: Long.MIN_VALUE,
                        group = news.group,
                        neutralScore = news.neutralScore?.toFloat(),
                        sourceMediumName = news.sourceMedium?.name?.normalized() ?: ""
                    )
                }
                if (entities.isNotEmpty()) {
                    newsDao.insertAll(entities)
                }
            }
        }
    }

    // Eliminar listeners cuando ya no se necesitan
    private fun removeFirestoreListeners() {
        newsSnapshotListener?.remove()
        newsSnapshotListener = null

        neutralNewsSnapshotListener?.remove()
        neutralNewsSnapshotListener = null
    }

    // No olvides modificar onCleared para limpiar los listeners
    override fun onCleared() {
        removeFirestoreListeners()
        compositeDisposable.clear()
        super.onCleared()
    }

    private fun processNewDocument(data: Map<String, Any>, docId: String): NewsBean? {
        val group = data[GROUP] as? Long ?: return null
        val title = data[TITLE] as? String ?: return null
        val description = data[DESCRIPTION] as? String ?: return null
        val scrapedDescription = data[SCRAPED_DESCRIPTION] as? String
        val category = data[CATEGORY] as? String
        val imageUrl = data[IMAGE_URL] as? String
        val link = data[LINK] as? String
        val createdAt = data[CREATED_AT] as? Timestamp
        val pubDate = data[PUB_DATE] as? Timestamp
        val updatedAt = data[UPDATED_AT] as? Timestamp
        val sourceMediumRaw = data[SOURCE_MEDIUM] as? String
        val neutralScore = data[NEUTRAL_SCORE] as? Long

        val sourceMedium = MediaBean.entries.find {
            it.pressMedia.name?.normalized() == sourceMediumRaw?.normalized()
        }?.pressMedia

        if (sourceMedium == null) {
            Log.e(
                "processNewDocument",
                "sourceMedium not found for: $sourceMediumRaw"
            )
            return null
        }

        val newsDescription = if (scrapedDescription.isNullOrEmpty()) {
            description
        } else {
            scrapedDescription
        }

        if (newsDescription.isEmpty()) {
            Log.e("processNewDocument", "newsDescription is null or empty")
            return null
        }

        return NewsBean(
            id = docId,
            title = title,
            description = newsDescription,
            category = category ?: "",
            imageUrl = imageUrl ?: "",
            link = link,
            pubDate = formatDateToSpanish(pubDate),
            createdAt = formatDateToSpanish(createdAt),
            updatedAt = formatDateToSpanish(updatedAt),
            sourceMedium = sourceMedium,
            group = group.toInt(),
            neutralScore = neutralScore?.toFloat(),

        )
    }

    private fun processSnapshotNewsDocuments(newDocuments: List<DocumentChange>, callback: (Boolean) -> Unit) {
        val changesByType = newDocuments.groupBy { it.type }
        Log.d("processSnapshotNewsDocuments", "Cambios detectados (noticias normales): ${newDocuments.size}, por tipo: " +
                "ADDED=${changesByType[DocumentChange.Type.ADDED]?.size ?: 0}, " +
                "MODIFIED=${changesByType[DocumentChange.Type.MODIFIED]?.size ?: 0}, " +
                "REMOVED=${changesByType[DocumentChange.Type.REMOVED]?.size ?: 0}")

        val addedOrModifiedDocs = newDocuments
            .filter { it.type == DocumentChange.Type.ADDED || it.type == DocumentChange.Type.MODIFIED }


        if (addedOrModifiedDocs.isEmpty()) {
            callback(false)
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val newsToAdd = addedOrModifiedDocs.mapNotNull { change ->
                try {
                    val doc = change.document
                    processNewDocument(doc.data, doc.id)
                } catch (e: Exception) {
                    Log.e("processNewNewsDocuments", "Error: ${e.localizedMessage}")
                    null
                }
            }

            withContext(Dispatchers.Main) {
                if (newsToAdd.isNotEmpty()) {
                    val existingIds = allNews.map { it.id }.toSet()

                    val brandNewNews = newsToAdd.filter { it.id !in existingIds }
                    val updatedNews = newsToAdd.filter { it.id in existingIds }
                    var verifiedUpdatedNews: List<NewsBean> = emptyList()

                    if (updatedNews.isNotEmpty()) {
                        Log.d("processNewNewsDocuments", "Verificando cambios en ${updatedNews.size} noticias normales")
                        verifiedUpdatedNews = updatedNews.filter { news ->
                            val index = allNews.indexOfFirst { it.id == news.id }
                            if (index >= 0) {
                                val existingNews = allNews[index]
                                // Solo actualizar si cambian estos campos específicos
                                existingNews.title != news.title ||
                                        existingNews.description != news.description ||
                                        existingNews.neutralScore != news.neutralScore ||
                                        existingNews.imageUrl != news.imageUrl ||
                                        existingNews.group != news.group
                            } else {
                                true
                            }
                        }

                        if (verifiedUpdatedNews.isNotEmpty()) {
                            Log.d("processNewNewsDocuments", "Actualizando ${verifiedUpdatedNews.size} noticias normales con cambios reales")
                            verifiedUpdatedNews.forEach { news ->
                                val index = allNews.indexOfFirst { it.id == news.id }
                                allNews[index] = news
                                Log.d("processNewNewsDocuments", "Noticia normal actualizada: ${news.id}, título: ${news.title}")
                            }
                            Log.d("processNewNewsDocuments", "Actualizando ${verifiedUpdatedNews.size} en caché")
                            saveNewsToLocalCache(verifiedUpdatedNews)
                        }
                    }

                    if (brandNewNews.isNotEmpty()) {
                        allNews.addAll(brandNewNews)
                        Log.d("processNewNewsDocuments", "Guardando ${brandNewNews.size} noticias normales en caché")
                        saveNewsToLocalCache(brandNewNews)
                    }

                    val hasSignificantChanges = brandNewNews.isNotEmpty() || verifiedUpdatedNews.isNotEmpty()

                    callback(hasSignificantChanges)
                }
            }
        }
    }

    private suspend fun foundCachedRegularNewsEntities(): Boolean =
        newsDao.getAllNews().let { entities ->
            if (entities.isNotEmpty()) {
                Log.d(
                    "TodayFragmentVM",
                    "Cargando ${entities.size} noticias regulares desde caché"
                )
                messageEvent.postValue("Cargando ${entities.size} noticias regulares desde caché...")

                val cachedNews = entities.map { entity: NewsEntity ->
                    NewsBean(
                        id = entity.id,
                        title = entity.title,
                        description = entity.description,
                        category = entity.category ?: "",
                        imageUrl = entity.imageUrl ?: "",
                        link = entity.link,
                        // Use proper date formatting from timestamp, avoid Long.MIN_VALUE values
                        createdAt = if (entity.createdAt != Long.MIN_VALUE) formatDateToSpanish(entity.createdAt) else null,
                        pubDate = if (entity.pubDate != Long.MIN_VALUE) formatDateToSpanish(entity.pubDate) else null,
                        updatedAt = if (entity.updatedAt != Long.MIN_VALUE) formatDateToSpanish(entity.updatedAt) else null,
                        sourceMedium = MediaBean.entries.find {
                            it.pressMedia.name?.normalized() == entity.sourceMediumName?.normalized()
                        }?.pressMedia,
                        group = entity.group,
                        neutralScore = entity.neutralScore?.toFloat(),
                    )
                }

                allNews = cachedNews.toMutableList()
                dataLoaded = true
                messageEvent.postValue("Noticias regulares cargadas desde caché: ${allNews.size}")
                true
            } else {
                messageEvent.postValue("Sin noticias regulares en caché")
                false
            }
        }

    private suspend fun foundCachedNeutralNewsEntities(): Boolean =
        neutralNewsDao.getAllNews().firstOrNull()?.let { entities ->
            if (entities.isNotEmpty()) {
                Log.d(
                    "TodayFragmentVM",
                    "Cargando ${entities.size} noticias neutrales desde caché"
                )
                messageEvent.postValue("Cargando ${entities.size} noticias neutrales desde caché...")

                val cachedNews = entities.map { entity: NeutralNewsEntity ->
                    // Convert stored timestamps to date strings, avoiding Long.MIN_VALUE
                    val formattedDate = if (entity.date != null && entity.date != Long.MIN_VALUE) {
                        formatDateToSpanish(entity.date)
                    } else null

                    val formattedCreatedAt = if (entity.createdAt != null && entity.createdAt != Long.MIN_VALUE) {
                        formatDateToSpanish(entity.createdAt)
                    } else null

                    val formattedUpdatedAt = if (entity.updatedAt != null && entity.updatedAt != Long.MIN_VALUE) {
                        formatDateToSpanish(entity.updatedAt)
                    } else null

                    NeutralNewsBean(
                        id = entity.id,
                        neutralTitle = entity.neutralTitle,
                        neutralDescription = entity.neutralDescription,
                        category = entity.category.toString(),
                        imageUrl = entity.imageUrl,
                        group = entity.group!!.toInt(),
                        date = formattedDate,
                        createdAt = formattedCreatedAt,
                        updatedAt = formattedUpdatedAt,
                        sourceIds = entity.sourceIds,
                        relevance = entity.relevance
                    )
                }

                allNeutralNews = cachedNews.toMutableList()
                dataLoaded = true
                Log.d(
                    "TodayFragmentVM",
                    "Noticias neutrales cargadas desde caché: ${allNeutralNews.size}"
                )
                Log.d(
                    "TodayFragmentVM",
                    "Sample neutral news from cache: ${allNeutralNews.firstOrNull()?.let { 
                        "id=${it.id}, date=${it.date}, createdAt=${it.createdAt}, updatedAt=${it.updatedAt}" 
                    }}"
                )
                true
            } else {
                Log.d("TodayFragmentVM", "Sin noticias neutrales en caché")
                messageEvent.postValue("Sin noticias neutrales en caché")
                false
            }
        } == true

    /**
     * Filtra noticias por consulta de búsqueda.
     */
    fun searchNews(query: String) {
        isLoading.postValue(true)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    searchQuery.postValue(query)
                }

                withContext(Dispatchers.Default) {
                    applyFiltersAndSearch()
                }

            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun updateFilterData(filterData: LocalFilterBean) {
        currentFilterData = filterData
        applyFiltersAndSearch()
    }

    /**
     * Aplica filtros a los datos de noticias sin realizar una nueva llamada a API.
     */
    fun applyFilters(filterData: LocalFilterBean) {
        if (currentFilterData == filterData) {
            Log.d("TodayFragmentVM", "No se aplicaron cambios en los filtros")
            return
        } else {
            currentFilterData = filterData
        }
        if (currentFilterData == LocalFilterBean()) {
            resetFilters()
            return
        }
        applyFiltersAndSearch()
    }
    val hasLoadedData: Boolean
        get() = allNeutralNews.isNotEmpty() || allNews.isNotEmpty()

    // Variables para mantener resultados filtrados (antes de búsqueda)
    private var filteredNeutralNewsCache = listOf<NeutralNewsBean>()
    private var filteredRegularNewsCache = listOf<NewsBean>()
    private var filtersApplied = false

    internal fun applyFiltersAndSearch(force:Boolean = false) {
        Log.d("TodayFragmentVM", "Iniciando applyFiltersAndSearch()")
        val query = searchQuery.value ?: ""

        // PASO 1: Aplicar filtros (solo si han cambiado o no se han aplicado antes)
        if (!filtersApplied || lastAppliedFilter != currentFilterData || force) {
            Log.d("TodayFragmentVM", "Aplicando filtros (primera vez o filtros cambiados)")

            // Comenzar desde las colecciones completas
            filteredNeutralNewsCache = allNeutralNews.toList()
            filteredRegularNewsCache = allNews.toList()

            // Aplicar filtros si existen
            if (currentFilterData != LocalFilterBean()) {
                Log.d("TodayFragmentVM", "Aplicando filtros: $currentFilterData")
                filteredNeutralNewsCache = applyFiltersToNeutralNews(filteredNeutralNewsCache, currentFilterData)
                // Extraer grupos de noticias neutrales filtradas
                val neutralGroups = filteredNeutralNewsCache.map { it.group }.toSet()

                // Filtrar noticias regulares por grupos de noticias neutrales
                filteredRegularNewsCache = filteredRegularNewsCache.filter { news ->
                    news.group in neutralGroups
                }
                Log.d("TodayFragmentVM", "Después de filtrar - noticias neutrales: ${filteredNeutralNewsCache.size}")
                Log.d("TodayFragmentVM", "Después de filtrar - noticias regulares: ${filteredRegularNewsCache.size}")
            }

            // Actualizar mapa de grupos
            updateGroupsOfNews(filteredRegularNewsCache)

            // Marcar que ya se aplicaron filtros
            filtersApplied = true
            lastAppliedFilter = currentFilterData.copy()
        }

        // PASO 2: Aplicar búsqueda sobre datos ya filtrados
        if (query.isEmpty()) {
            Log.d("TodayFragmentVM", "Búsqueda vacía, mostrando todas las noticias filtradas")
            neutralNewsList.postValue(filteredNeutralNewsCache)
            newsList.postValue(formatAndGroupNews(filteredRegularNewsCache))
            showNoResults.postValue(filteredNeutralNewsCache.isEmpty() && filteredRegularNewsCache.isEmpty())
        } else {
            Log.d("TodayFragmentVM", "Aplicando búsqueda con consulta: '$query' sobre datos ya filtrados")

            // Buscar SOLO en las colecciones ya filtradas
            val searchedNeutral = searchInNeutralNews(filteredNeutralNewsCache, query)
            val searchedRegular = filteredRegularNewsCache

            neutralNewsList.postValue(searchedNeutral)
            newsList.postValue(formatAndGroupNews(searchedRegular))
            showNoResults.postValue(searchedNeutral.isEmpty() && searchedRegular.isEmpty())
        }

        Log.d("TodayFragmentVM", "Finalizado applyFiltersAndSearch()")
    }

    // Necesitas añadir estas funciones:
    private var lastAppliedFilter = LocalFilterBean()

    fun resetFilters() {
        filtersApplied = false
        currentFilterData = LocalFilterBean()
        applyFiltersAndSearch()
    }

    /**
     * Busca dentro de la colección de noticias neutrales.
     */
    private fun searchInNeutralNews(
        news: List<NeutralNewsBean>,
        query: String
    ): MutableList<NeutralNewsBean> {
        if (query.isEmpty()) {
            return news.toMutableList()
        }

        // Crear listas separadas para diferentes tipos de coincidencias
        val titleMatches = mutableListOf<NeutralNewsBean>()
        val descriptionMatches = mutableListOf<NeutralNewsBean>()
        val scrapedDescriptionMatches = mutableListOf<NeutralNewsBean>()

        // Verificar si es una búsqueda especializada (grupo o fuente)
        if (query.startsWith("g", ignoreCase = true) && query.length > 1) {
            // Búsqueda por ID de grupo - formato: "g<número>"
            val groupId = query.substring(1).toIntOrNull()
            if (groupId != null) {
                Log.d("searchInNeutralNews", "Búsqueda por grupo específico: $groupId")
                return news.filter { it.group == groupId }.toMutableList()
            }
        } else if (query.startsWith("s", ignoreCase = true) && query.length > 1) {
            // Búsqueda por ID de fuente - formato: "s<id>"
            val sourceId = query.substring(1)
            Log.d("searchInNeutralNews", "Búsqueda por fuente específica: $sourceId")
            return news.filter { newsItem ->
                newsItem.sourceIds?.any { it.contains(sourceId, ignoreCase = true) } == true
            }.toMutableList()
        }

        // Búsqueda estándar si no es especializada
        news.forEach { newsItem ->
            // Verificar coincidencias en el título
            if (newsItem.group.toString().contains(query, ignoreCase = true)) {
                titleMatches.add(newsItem)
            }
            else if (newsItem.id.contains(query, ignoreCase = true)) {
                titleMatches.add(newsItem)
            }
            else if (newsItem.sourceIds?.any { it.contains(query, ignoreCase = true) } == true) {
                titleMatches.add(newsItem)
            }
            else if (newsItem.neutralTitle.contains(query, ignoreCase = true)) {
                titleMatches.add(newsItem)
            }
            // Verificar coincidencias en la descripción (solo si no coincidió ya por título)
            else if (newsItem.neutralDescription.contains(query, ignoreCase = true)) {
                descriptionMatches.add(newsItem)
            }
            // Verificar coincidencias en la descripción raspada en noticias relacionadas
            else {
                val relatedNewsHasMatch = groupsOfNews[newsItem.group]?.any { relatedNews ->
                    relatedNews.description?.contains(query, ignoreCase = true) == true
                } == true

                if (relatedNewsHasMatch) {
                    scrapedDescriptionMatches.add(newsItem)
                }
            }
        }

        // Combinar resultados en orden de prioridad
        val result = mutableListOf<NeutralNewsBean>()
        result.addAll(titleMatches)
        result.addAll(descriptionMatches)
        result.addAll(scrapedDescriptionMatches)

        return result
    }

    /**
     * Aplica filtros a los datos de noticias neutrales.
     */
    private fun applyFiltersToNeutralNews(
        newsItems: List<NeutralNewsBean>,
        filterData: LocalFilterBean
    ): List<NeutralNewsBean> {
        if (filterData == LocalFilterBean()) {
            return newsItems.toList()
        }

        return newsItems.asSequence()
            .filter { newsItem ->
            applyCategoryNeutralFilter(filterData, newsItem) &&
            applyMediaFilterNeutralNews(filterData, newsItem) &&
            applyDateFilterNeutralNews(filterData, newsItem)
        }.toList()
    }

    private fun applyMediaFilterNeutralNews(filterData: LocalFilterBean, newsItem: NeutralNewsBean): Boolean {
        if (filterData.media?.isEmpty() == true) {
            return true
        }
        val sourceNews = groupsOfNews[newsItem.group]
        if (sourceNews.isNullOrEmpty()) return false
        val anySourceMediaMatches = sourceNews.any { news ->
            val sourceMedium = news.sourceMedium?.name?.normalized()
            val mediaFilter = filterData.media?.any { it.name?.normalized() == sourceMedium }
            mediaFilter == true
        }

        return anySourceMediaMatches
    }

    /**
     * Aplica el filtro de fecha a las noticias neutrales.
     */
    private fun applyDateFilterNeutralNews(filterData: LocalFilterBean, newsItem: NeutralNewsBean): Boolean {
        // Si no hay filtro de fecha, mostrar todas las noticias
        if (filterData.dateFilter == null && filterData.selectedDates.isNullOrEmpty()) {
            return true
        }

        val newsDate = getDateForSorting(newsItem.date)
        if (newsDate == null) {
            // Si no podemos obtener la fecha, incluimos la noticia por defecto
            Log.d("DateFilter", "No se pudo obtener fecha para noticia: ${newsItem.neutralTitle}")
            return true
        }

        // Filtro múltiple - verificar si la fecha está en cualquiera de las fechas seleccionadas
        if (filterData.selectedDates != null && filterData.selectedDates!!.isNotEmpty()) {
            return isDateInAnySelectedDay(newsDate, filterData.selectedDates!!)
        }

        // Filtro simple (comportamiento anterior)
        val filterDate = filterData.dateFilter

        // Log para depuración
        Log.d("DateFilter", "Comparando fechas - Noticia: ${SimpleDateFormat("dd 'DE' MMMM, HH:mm", Locale.getDefault()).format(newsDate)}")
        Log.d("DateFilter", "Filtro: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(filterDate)}")
        Log.d("DateFilter", "Es anterior a: ${filterData.isOlderThan}")

        // Configurar las horas de inicio y fin del día
        val calendar = Calendar.getInstance()
        calendar.time = filterDate

        // Si estamos buscando noticias anteriores a una fecha
        if (filterData.isOlderThan) {
            // Establecer a inicio del día (00:00:00)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.time

            Log.d("DateFilter", "Comparando antes de: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(startOfDay)}")
            val result = newsDate.before(startOfDay)
            Log.d("DateFilter", "Resultado: $result")
            return result
        } else {
            // Establecer a inicio del día (00:00:00)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.time

            // Establecer a fin del día (23:59:59)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.time

            Log.d("DateFilter", "Rango del día - Inicio: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(startOfDay)}")
            Log.d("DateFilter", "Rango del día - Fin: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(endOfDay)}")

            // Verificar si la fecha de la noticia está dentro del día seleccionado
            val isWithinRange = (newsDate.after(startOfDay) || newsDate.equals(startOfDay)) &&
                    (newsDate.before(endOfDay) || newsDate.equals(endOfDay))

            Log.d("DateFilter", "¿Dentro del rango?: $isWithinRange")
            return isWithinRange
        }
    }

    /**
     * Comprueba si una fecha está dentro de cualquiera de las fechas seleccionadas.
     */
    private fun isDateInAnySelectedDay(newsDate: Date, selectedDates: List<Date>): Boolean {
        val calendar = Calendar.getInstance()

        for (selectedDate in selectedDates) {
            calendar.time = selectedDate

            // Configurar inicio del día
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.time

            // Configurar fin del día
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.time

            // Si la noticia está dentro de este día, devolver true
            if ((newsDate.after(startOfDay) || newsDate.equals(startOfDay)) &&
                (newsDate.before(endOfDay) || newsDate.equals(endOfDay))) {
                return true
            }
        }

        return false // No coincide con ningún día seleccionado
    }

    /**
     * Aplica el filtro de fecha a las noticias regulares.
     */
    private fun applyDateFilter(filterData: LocalFilterBean, newsItem: NewsBean): Boolean {
        // Si no hay filtro de fecha, mostrar todas las noticias
        if (filterData.dateFilter == null && filterData.selectedDates.isNullOrEmpty()) {
            return true
        }

        val newsDate = getDateForSorting(newsItem.date) ?: return true

        // Filtro múltiple - verificar si la fecha está en cualquiera de las fechas seleccionadas
        if (filterData.selectedDates != null && filterData.selectedDates!!.isNotEmpty()) {
            return isDateInAnySelectedDay(newsDate, filterData.selectedDates!!)
        }

        // Filtro simple (comportamiento anterior)
        val filterDate = filterData.dateFilter

        // Configurar las horas de inicio y fin del día
        val calendar = Calendar.getInstance()
        calendar.time = filterDate

        // Establecer a inicio del día (00:00:00)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time

        // Si estamos buscando noticias anteriores a una fecha
        if (filterData.isOlderThan) {
            return newsDate.before(startOfDay)
        }

        // Establecer a fin del día (23:59:59)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.time

        // Verificar si la fecha de la noticia está dentro del día seleccionado
        return newsDate.after(startOfDay) && newsDate.before(endOfDay)


    }

    /**
     * Aplica filtros a los datos de noticias regulares.
     */
    private fun applyFiltersToNews(
        newsItems: List<NewsBean>,
        filterData: LocalFilterBean
    ): List<NewsBean> {
        if (filterData == LocalFilterBean()) {
            return newsItems
        }

        return newsItems.asSequence()
            .filter { newsItem ->
                applyMediaFilter(filterData, newsItem) &&
                applyCategoryFilter(filterData, newsItem) &&
                applyDateFilter(filterData, newsItem)
            }
            .toList()
    }

    /**
     * Actualiza el mapa groupsOfNews con las noticias filtradas.
     */
    private fun updateGroupsOfNews(filteredNews: List<NewsBean>) {
        val newsGroups = filteredNews.groupBy { it.group ?: -1 }
            .filter { it.key >= 0 } // Filtrar grupos inválidos

        groupsOfNews.clear()
        groupsOfNews.putAll(newsGroups)
    }

    /**
     * Formatea y agrupa las noticias para su visualización.
     */
    private fun formatAndGroupNews(newsItems: List<NewsBean>): List<List<NewsBean>> {
        return filterGroupedNews(newsItems)
    }

    /**
     * Obtiene noticias relacionadas para una noticia específica.
     *
     * @param from La noticia para la que queremos encontrar noticias relacionadas.
     * @return Una lista de noticias relacionadas con el mismo grupo.
     */
    fun getRelatedNews(from: NewsBean): List<NewsBean> {
        return groupsOfNews[from.group] ?: emptyList()
    }

    /**
     * Obtiene noticias neutrales desde Firestore.
     *
     * Esta función recupera todas las noticias neutrales desde la colección de Firestore,
     * registra el proceso y envía los mensajes y resultados apropiados.
     *
     * @param filterData Datos de filtro local opcionales para personalizar las noticias.
     */
    internal suspend fun fetchNeutralNewsFromFirestoreSuspend(
        filterData: LocalFilterBean
    ): Boolean {
        return suspendCoroutine { continuation ->
            fetchNeutralNewsFromFirestore(filterData, continuation)
        }
    }

    /**
     * Obtiene noticias neutrales desde Firestore.
     *
     * Esta función recupera todas las noticias neutrales desde la colección de Firestore,
     * registra el proceso y envía los mensajes y resultados apropiados.
     */

    fun fetchNeutralNewsFromFirestore(
        filterData: LocalFilterBean,
        continuation: Continuation<Boolean>? = null
    ) {
        if (allNeutralNews.isNotEmpty()) {
            Log.d("fetchNeutralNewsFromFirestore", "Ya hay noticias neutrales cargadas, usando caché")
            messageEvent.postValue("Usando noticias neutrales en caché")
        }

        currentFilterData = filterData

        val db = FirebaseFirestore.getInstance()
        db.collection(NEUTRAL_NEWS).get().addOnSuccessListener { snapshot ->
            if (snapshot != null && !snapshot.isEmpty) {
                Log.d(
                    "fetchNeutralNewsFromFirestore",
                    "Tamaño del snapshot: ${snapshot.size()}"
                )
                val fetchedNeutralNews = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    try {
                        if (data == null) {
                            Log.e("fetchNeutralNewsFromFirestore", "Data is null for document: ${doc.id}")
                            return@mapNotNull null
                        }
                        if (doc == null) {
                            Log.e("fetchNeutralNewsFromFirestore", "Document is null")
                        }
                        val result = processNeutralNewDocument(data,doc.id)
                        if (result == null) {
                            return@mapNotNull null
                        }
                        result
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(
                            "fetchNeutralNewsFromFirestore",
                            "Error al analizar el documento: ${e.localizedMessage}"
                        )
                        null
                    }
                }

                allNeutralNews = fetchedNeutralNews.toMutableList()

                dataLoaded = true

                Log.d(
                    "fetchNeutralNewsFromFirestore",
                    "Cantidad de noticias neutrales: ${allNeutralNews.size}"
                )
                messageEvent.postValue("Noticias neutrales obtenidas con éxito. Total: ${allNeutralNews.size}")

                // Guardar en caché local con el timestamp original
                saveNeutralNewsToLocalCache(fetchedNeutralNews)
            } else {
                messageEvent.postValue("No se encontraron noticias neutrales en Firestore")
                Log.w(
                    "fetchNeutralNewsFromFirestore",
                    "No se encontraron noticias neutrales en Firestore"
                )
                // Solo mostrar "No hay resultados" si no hay consulta de búsqueda (mostrando estado vacío)
                Log.d("fetchNeutralNewsFromFirestore", "Mostrando mensaje 'No hay resultados': ${searchQuery.value.isNullOrEmpty()}")
                showNoResults.postValue(searchQuery.value.isNullOrEmpty())
            }
        }.addOnSuccessListener {
            continuation?.resume(true)
        }.addOnFailureListener { e ->
            continuation?.resume(false)
            messageEvent.postValue("Error al obtener noticias neutrales: ${e.localizedMessage}")
            Log.e(
                "fetchNeutralNewsFromFirestore",
                "Error al obtener noticias neutrales: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Obtiene datos de noticias desde Firestore.
     *
     * Esta función recupera todas las noticias desde la colección de Firestore,
     * registra el proceso y envía los mensajes y resultados apropiados.
     *
     * @param filterData Datos de filtro local opcionales para personalizar las noticias.
     */

    internal suspend fun fetchNewsFromFirestoreSuspend(
        filterData: LocalFilterBean
    ): Boolean {
        return suspendCoroutine { continuation ->
            fetchNewsFromFirestore(filterData, continuation)
        }
    }

    fun fetchNewsFromFirestore(
        filterData: LocalFilterBean,
        continuation: Continuation<Boolean>? = null
    ) {
        val startTime = System.currentTimeMillis()
        Log.d("TIMER", "Iniciando consulta a Firestore")

        if (allNews.isNotEmpty()) {
            Log.d("fetchNewsFromFirestore", "Ya hay noticias cargadas, usando caché")
            messageEvent.postValue("Usando noticias en caché")
            return
        }

        isFirestoreLoading = true

        // Guardar los datos de filtro
        currentFilterData = filterData

        val db = FirebaseFirestore.getInstance()
        db.collection(NEWS)
            .whereGreaterThan(GROUP, 0)
            .whereNotEqualTo(DESCRIPTION, "")
            .orderBy(GROUP, Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val endTime = System.currentTimeMillis()
                val elapsedTime = endTime - startTime
                Log.d("TIMER", "Consulta completada en $elapsedTime ms")

                if (snapshot != null && !snapshot.isEmpty) {
                    Log.d("fetchNewsFromFirestore", "Snapshot size: ${snapshot.size()}")
                    val fetchedNews = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        try {
                            if (data == null) {
                                Log.e("fetchNewsFromFirestore", "Data is null for document: ${doc.id}")
                                return@mapNotNull null
                            }
                            val result = processNewDocument(data, doc.id)
                            if (result == null) {
                                return@mapNotNull null
                            }
                            result
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("fetchNewsFromFirestore", "Error parsing document: ${e.localizedMessage}")
                            null
                        }
                    }

                    // Almacenar todas las noticias sin filtrar
                    allNews = fetchedNews.toMutableList()

                    Log.d("fetchNewsFromFirestore", "Fetched news count: ${fetchedNews.size}")
                    messageEvent.postValue("News fetched successfully. Total: ${fetchedNews.size}")
                    // Guardar en caché local
                    saveNewsToLocalCache(fetchedNews)
                } else {
                    messageEvent.postValue("No news found in Firestore")
                    Log.w("fetchNewsFromFirestore", "No news found in Firestore")
                }
                isFirestoreLoading = false
            }
            .addOnSuccessListener {
                continuation?.resume(true)
            }
            .addOnFailureListener { e ->
                val endTime = System.currentTimeMillis()
                val elapsedTime = endTime - startTime
                Log.e("TIMER", "Consulta falló después de $elapsedTime ms: ${e.localizedMessage}")

                continuation?.resume(false)
                messageEvent.postValue("Error fetching news: ${e.localizedMessage}")
                Log.e("fetchNewsFromFirestore", "Error fetching news: ${e.localizedMessage}")
                isFirestoreLoading = false
            }
    }
    /**
     * Agrupa los elementos de noticias obtenidos según su número de grupo y medio de origen.
     *
     * La función asegura que solo se consideren grupos con más de un elemento,
     * y para cada fuente en un grupo se selecciona la noticia más reciente.
     *
     * @param fetchedNews La lista de elementos de noticias obtenidos de Firestore.
     * @return Una lista de elementos de noticias agrupados.
     */
    private fun filterGroupedNews(fetchedNews: List<NewsBean>): List<List<NewsBean>> {
        // Step 1: Filter out news with a valid non-null, non-negative group ID
        val validNews = fetchedNews.filter { it.group != null && it.group >= 0 }

        // Step 2: Group news by their group ID
        val groupedNews = validNews.groupBy { it.group }

        // Step 3: Keep only groups with more than one item
        val nonSingletonGroups = groupedNews.filter { (_, newsList) -> newsList.size > 1 }

        // Step 4: For each group, keep only the most recent news per source
        val mostRecentPerSourcePerGroup = nonSingletonGroups.mapValues { (_, newsList) ->
            newsList.groupBy { it.sourceMedium }.mapValues { (_, sourceNewsList) ->
                    sourceNewsList.maxByOrNull {
                        getDateForSorting(it.date)?.time ?: Long.MIN_VALUE
                    }!!
            }.values.sortedByDescending {
                getDateForSorting(it.date)?.time ?: Long.MIN_VALUE
                }
        }

        // Step 5: Sort all groups by the most recent news item inside each group
        val sortedGroups = mostRecentPerSourcePerGroup.values.sortedByDescending { group ->
                getDateForSorting(group.firstOrNull()?.date)?.time ?: Long.MIN_VALUE
            }

        // Step 6: Format date for display
        return sortedGroups.map { group ->
            group.map { news ->
                news.copy(pubDate = formatDateToSpanish(news.pubDate))
            }
        }
    }

    /**
     * Aplica el filtro de medios a las noticias.
     *
     * @return True si el filtro de medios se aplica, false en caso contrario.
     */
    private fun applyMediaFilter(filterData: LocalFilterBean, newsItem: NewsBean): Boolean {
        if (filterData.media.isNullOrEmpty()) return true
        return filterData.media?.any {
            it.name?.normalized() == newsItem.sourceMedium?.name?.normalized()
        } != false
    }

    /**
     * Aplica el filtro de categoría a las noticias.
     *
     * @return True si el filtro de categoría se aplica, false en caso contrario.
     */
    private fun applyCategoryFilter(filterData: LocalFilterBean, newsItem: NewsBean): Boolean {
        if (filterData.categoryTagBean.isNullOrEmpty()) return true
        return filterData.categoryTagBean?.any {
            it.name == newsItem.category
        } != false
    }

    /**
     * Aplica el filtro de categoría a las noticias neutrales.
     *
     * @return True si el filtro de categoría se aplica, false en caso contrario.
     */
    private fun applyCategoryNeutralFilter(
        filterData: LocalFilterBean, newsItem: NeutralNewsBean
    ): Boolean {
        if (filterData.categoryTagBean.isNullOrEmpty()) return true
        return filterData.categoryTagBean?.any {
            it.name == newsItem.category
        } != false
    }

    /**
     * Formatea una fecha en formato español.
     *
     * @param value Cualquier objeto que represente una fecha (Timestamp, Date, String, Long)
     * @return Cadena de fecha formateada o null si no se puede formatear.
     */
    private val dateFormatCache = ConcurrentHashMap<String, String?>()

    // Función optimizada de formateo de fechas
    private fun formatDateToSpanish(value: Any?): String? {
        if (value == null) return null

        // Si es String y está en caché, devolver directamente
        if (value is String && dateFormatCache.containsKey(value)) {
            return dateFormatCache[value]
        }

        // Formato solamente cuando es necesario
        val outputFormat = SimpleDateFormat(DATE_FORMAT, Locale("es", "ES"))
        val date: Date? = when (value) {
            is Timestamp -> value.toDate()
            is Date -> value
            is String -> parseStringDate(value)
            is Long -> Date(value)
            else -> null
        }

        val result = date?.let { outputFormat.format(it).uppercase() }

        // Guardar en caché si es una cadena
        if (value is String && result != null) {
            dateFormatCache[value] = result
        }

        return result
    }

    /**
     * Helper function to parse date strings in various formats
     */
    private fun parseStringDate(dateString: String): Date? {
        // Priorizar formatos españoles ya que es lo que más usamos
        val inputFormats = listOf(
            SimpleDateFormat(DATE_FORMAT, Locale("es", "ES")),
            SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")),
        )

        for (format in inputFormats) {
            try {
                val parsedDate = format.parse(dateString)
                if (parsedDate != null) {
                    return parsedDate
                }
            } catch (e: ParseException) {
                // Intentar con el siguiente formato
            }
        }

        Log.e("DateDebug", "No se pudo parsear la fecha: '$dateString'")
        return null
    }

    /**
     * Convierte una cadena de fecha a un objeto Date para comparaciones.
     * Maneja múltiples formatos y asigna el año actual cuando no está presente.
     */
    private fun getDateForSorting(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null

        // Obtener año actual para usarlo cuando no se especifica
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Intentar con múltiples formatos
        val formats = listOf(
            DATE_FORMAT,
            // settings.dateFormat
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale("es", "ES"))
                val date = sdf.parse(dateString)

                if (format == DATE_FORMAT) {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    calendar.set(Calendar.YEAR, currentYear)
                    return calendar.time
                }

                return date
            } catch (e: Exception) {
                // Intentar con el siguiente formato
                continue
            }
        }

        return null
    }


    /**
     * Carga la primera página de noticias neutrales desde Firestore.
     */
    fun loadFirstPageNeutralNews() {
        if (isLoadingFirstPage) {
            Log.d("Paginación", "Ya hay una carga inicial en progreso, ignorando solicitud")
            return
        }

        viewModelScope.launch {
            try {
                isLoadingFirstPage = true
                lastLoadedTimestamp = null
                hasMoreItems = true
                allNeutralNews.clear()

                loadNeutralNewsPage()
            } catch (e: Exception) {
                Log.e("Paginación", "Error cargando la primera página: ${e.message}", e)
                messageEvent.postValue("Error al cargar noticias: ${e.message}")
            } finally {
                isLoadingFirstPage = false
            }
        }
    }

    /**
     * Carga la siguiente página de noticias neutrales.
     */
    fun loadNextPage() {
        if (isLoadingMoreItems || !hasMoreItems) {
            Log.d("Paginación", "Solicitud omitida: carga=${isLoadingMoreItems}, hasMore=${hasMoreItems}")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("Paginación", "Iniciando carga de siguiente página")
                isLoadingMoreItems = true
                isPaginating.postValue(true)

                loadNeutralNewsPage()
            } catch (e: Exception) {
                Log.e("Paginación", "Error: ${e.message}", e)
            } finally {
                isLoadingMoreItems = false
                isPaginating.postValue(false)
            }
        }
    }

    private suspend fun loadNeutralNewsPage() {
        val db = FirebaseFirestore.getInstance()

        Log.d("Paginación", "Iniciando carga de página. Último timestamp: $lastLoadedTimestamp")

        var queryRef = db.collection(NEUTRAL_NEWS)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        // Si no es la primera página, añadir startAfter
        if (lastLoadedTimestamp != null) {
            queryRef = queryRef.startAfter(lastLoadedTimestamp)
            Log.d("Paginación", "Consultando después de timestamp: $lastLoadedTimestamp")
        }

        try {
            Log.d("Paginación", "Ejecutando consulta a Firestore...")
            val startTime = System.currentTimeMillis()
            val snapshot = queryRef.get().await()
            val queryTime = System.currentTimeMillis() - startTime
            Log.d("Paginación", "Consulta completada en $queryTime ms. Documentos: ${snapshot.size()}")

            if (snapshot.isEmpty) {
                Log.d("Paginación", "No hay más resultados disponibles")
                hasMoreItems = false
                return
            }

            // Importante: Solo verificar IDs en lugar de objetos completos
            val existingIds = allNeutralNews.map { it.id }.toSet()
            Log.d("Paginación", "IDs existentes: ${existingIds.size}")

            val fetchedNeutralNews = snapshot.documents.mapNotNull { document ->
                try {
                    // Solo procesar documentos que no existen ya
                    if (document.id !in existingIds) {
                        val docData = document.data ?: return@mapNotNull null
                        processNeutralNewDocument(docData, document.id)
                    } else {
                        Log.d("Paginación", "Documento ya existe: ${document.id}")
                        null
                    }
                } catch (e: Exception) {
                    Log.e("Paginación", "Error procesando documento: ${e.message}")
                    null
                }
            }

            Log.d("Paginación", "Noticias neutrales procesadas: ${fetchedNeutralNews.size}")

            if (fetchedNeutralNews.isNotEmpty()) {
                // Guardar el último timestamp para la próxima paginación
                val lastNews = fetchedNeutralNews.lastOrNull()
                if (lastNews?.date != null) {
                    lastLoadedTimestamp = Timestamp(
                        lastNews.date.toLong(),
                        0
                    ) // Guardar el timestamp del último elemento
                    Log.d("Paginación", "Nuevo timestamp para próxima página: $lastLoadedTimestamp")
                }

                // Añadir a la lista existente
                allNeutralNews.addAll(fetchedNeutralNews)
                Log.d("Paginación", "Nuevo tamaño de allNeutralNews: ${allNeutralNews.size}")

                // Guardar en caché local
                saveNeutralNewsToLocalCache(fetchedNeutralNews)

                // Cargar las noticias normales relacionadas
                loadRelatedRegularNews(fetchedNeutralNews)

                // IMPORTANTE: Solo establecer hasMoreItems=false si no se recibieron resultados
                // o si recibimos menos del tamaño de página
                hasMoreItems = fetchedNeutralNews.size >= pageSize
                Log.d("Paginación", "¿Hay más elementos para cargar? $hasMoreItems")

                // Aplicar filtros y actualizar la UI
                applyFiltersAndSearch()
            } else {
                hasMoreItems = false
                Log.d("Paginación", "No se encontraron noticias nuevas (posible duplicación)")
            }
        } catch (e: Exception) {
            Log.e("Paginación", "Error cargando página: ${e.message}", e)
            messageEvent.postValue("Error al cargar más noticias: ${e.message}")
        }
    }

    /**
     * Carga solo las noticias normales relacionadas con las noticias neutrales dadas.
     */
    private suspend fun loadRelatedRegularNews(neutralNews: List<NeutralNewsBean>) {
        if (neutralNews.isEmpty()) return

        Log.d("Paginación", "Cargando noticias relacionadas para ${neutralNews.size} grupos")
        val groups = neutralNews.map { it.group }.distinct()
        Log.d("Paginación", "Consultando grupos: $groups")

        try {
            val db = FirebaseFirestore.getInstance()
            val query = db.collection("news").whereIn(GROUP, groups)
                .limit(100) // Limitamos para evitar exceder cuotas

            val snapshot = query.get().await()
            Log.d("Paginación", "Documentos encontrados para grupos: ${snapshot.documents.size}")

            val relatedNews = mutableListOf<NewsBean>()
            snapshot.documents.forEach { doc ->
                processNewDocument(doc.data as Map<String, Any>, doc.id)?.let {
                    relatedNews.add(it)
                }
            }

            Log.d("Paginación", "Noticias regulares procesadas: ${relatedNews.size}")

            // Actualizamos la colección de noticias regulares
            allNews.addAll(relatedNews)
            updateGroupsOfNews(relatedNews)
            Log.d("Paginación", "Carga de noticias relacionadas completada en ${System.currentTimeMillis() % 1000} ms")
        } catch (e: Exception) {
            Log.e("Paginación", "Error cargando noticias relacionadas: ${e.message}", e)
        }
    }

    private suspend fun loadNewsForGroups(groups: List<Int>) {
        val db = FirebaseFirestore.getInstance()
        Log.d("Paginación", "Consultando grupos: $groups")

        try {
            val existingIds = allNews.map { it.id }.toSet()
            val query = db.collection(NEWS).whereIn(GROUP, groups)

            val snapshot = query.get().await()
            Log.d("Paginación", "Documentos encontrados para grupos: ${snapshot.size()}")

            val fetchedNews = snapshot.documents.mapNotNull { document ->
                try {
                    val docData = document.data
                    if (docData != null && document.id !in existingIds) {
                        processNewDocument(docData, document.id)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("Paginación", "Error procesando documento: ${e.message}")
                    null
                }
            }

            Log.d("Paginación", "Noticias regulares procesadas: ${fetchedNews.size}")
            if (fetchedNews.isNotEmpty()) {
                // Añadir noticias nuevas
                allNews.addAll(fetchedNews)

                // Actualizar groupsOfNews
                updateGroupsOfNews(allNews)

                // Guardar en caché local
                saveNewsToLocalCache(fetchedNews)
            }
        } catch (e: Exception) {
            Log.e("Paginación", "Error cargando grupos: ${e.message}")
        }
    }

    fun searchNewsInFirebase(query: String) {
        if (query.length < 2) {
            if (query.isEmpty()) {
                loadFirstPageNeutralNews()
            }
            return
        }

        viewModelScope.launch {
            try {
                searchQuery.value = query
                Log.d("searchNewsInFirebase", "No results: false")
                showNoResults.postValue(false)

                Log.d("Búsqueda", "Iniciando búsqueda en Firebase: '$query'")
                val lowerQuery = query.trim().normalized()
                val db = FirebaseFirestore.getInstance()

                // Recuperar un conjunto mayor de documentos recientes
                val recentNewsSnapshot = db.collection(NEUTRAL_NEWS)
                    .orderBy(CREATED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                Log.d("Búsqueda", "Documentos recuperados para búsqueda: ${recentNewsSnapshot.size()}")

                // Realizar búsqueda en memoria con contains()
                val results = recentNewsSnapshot.documents.mapNotNull { doc ->
                    processNeutralNewDocument(doc.data, doc.id)?.let { news ->
                        // Verificamos si el título o descripción CONTIENEN la consulta
                        val titleMatch = news.neutralTitle.normalized().contains(lowerQuery, ignoreCase = true)
                        val descMatch = news.neutralDescription.normalized().contains(lowerQuery, ignoreCase = true)

                        // Log detallado para diagnóstico
                        if (titleMatch) {
                            Log.d("Búsqueda", "Coincidencia en título: '${news.neutralTitle}'")
                        }
                        if (descMatch) {
                            Log.d("Búsqueda", "Coincidencia en descripción: título='${news.neutralTitle}', desc contiene='$lowerQuery'")
                        }

                        if (titleMatch || descMatch) news else null
                    }
                }

                // Ordenamos por relevancia (primero títulos, luego descripciones)
                val sortedResults = results.sortedWith(compareBy(
                    // Primero noticias donde el título contiene la consulta
                    { !it.neutralTitle.normalized().contains(lowerQuery, ignoreCase = true) },
                    // Después por fecha de creación (más recientes primero)
                    { it.date ?: 0 }
                ))

                Log.d("Búsqueda", "Total resultados encontrados: ${sortedResults.size}")

                if (sortedResults.isNotEmpty()) {
                    // Cargar noticias relacionadas
                    loadRelatedRegularNews(sortedResults)

                    // Actualizar la UI
                    allNeutralNews = sortedResults.toMutableList()
                    neutralNewsList.postValue(sortedResults.toMutableList())
                    Log.d("Búsqueda", "No results: false")
                    showNoResults.postValue(false)
                    messageEvent.postValue("Se encontraron ${sortedResults.size} resultados")
                } else {
                    Log.d("Búsqueda", "No se encontraron resultados para: '$lowerQuery'")
                    allNeutralNews.clear()
                    neutralNewsList.postValue(mutableListOf())
                    Log.d("Búsqueda", "No results: false")
                    showNoResults.postValue(true)
                    messageEvent.postValue("No se encontraron resultados para: $query")
                }
            } catch (e: Exception) {
                Log.e("Búsqueda", "Error en búsqueda: ${e.message}", e)
                messageEvent.postValue("Error en búsqueda: ${e.message}")
            } finally {
            }
        }
    }

}

