package com.example.neutralnews_android.ui.main.today

import android.util.Log
import com.example.neutralnews_android.data.bean.news.NeutralNewsBean
import com.example.neutralnews_android.data.bean.news.NewsBean
import com.example.neutralnews_android.data.bean.news.pressmedia.MediaBean
import com.example.neutralnews_android.data.room.dao.NeutralNewsDao
import com.example.neutralnews_android.data.room.dao.NewsDao
import com.example.neutralnews_android.data.room.entities.NeutralNewsEntity
import com.example.neutralnews_android.data.room.entities.NewsEntity
import com.example.neutralnews_android.util.string.normalized
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Clase helper para manejar la carga, caché y actualización de noticias.
 * Encapsula toda la lógica de acceso a datos para mantener el ViewModel limpio.
 */
class NewsDataManager(
    private val neutralNewsDao: NeutralNewsDao,
    private val newsDao: NewsDao
) {

    // === Caché de Noticias Neutrales ===

    /**
     * Carga noticias neutrales del caché local.
     * Solo carga las del día actual del usuario.
     */
    suspend fun loadCachedNeutralNews(): List<NeutralNewsBean> {
        val entities = neutralNewsDao.getAllNews().firstOrNull() ?: emptyList()

        if (entities.isEmpty()) {
            Log.d("NewsDataManager", "Sin noticias neutrales en caché")
            return emptyList()
        }

        // Filtrar solo las noticias del día actual
        val todayRange = getTodayRange()
        val todayEntities = entities.filter { entity ->
            val timestamp = entity.date
            timestamp in todayRange.first..todayRange.second
        }

        Log.d("NewsDataManager", "Noticias del día actual: ${todayEntities.size} de ${entities.size} totales")

        return todayEntities.map { entity ->
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
    }

    /**
     * Carga noticias relacionadas del caché.
     */
    suspend fun loadCachedRelatedNews(): List<NewsBean> {
        val regularNewsEntities = newsDao.getAllNews()

        if (regularNewsEntities.isEmpty()) return emptyList()

        return regularNewsEntities.map { entity ->
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
    }

    /**
     * Guarda noticias neutrales en el caché local.
     */
    suspend fun saveNeutralNewsToCache(newsList: List<NeutralNewsBean>) {
        if (newsList.isEmpty()) return

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
                        Log.e("NewsDataManager", "Error creando entidad: ${e.message}")
                        null
                    }
                }

                if (entities.isNotEmpty()) {
                    neutralNewsDao.insertAll(entities)
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataManager", "Error guardando en caché: ${e.message}", e)
        }
    }

    /**
     * Guarda noticias relacionadas en el caché local.
     */
    suspend fun saveNewsToCache(newsList: List<NewsBean>) {
        if (newsList.isEmpty()) return

        try {
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
        } catch (e: Exception) {
            Log.e("NewsDataManager", "Error guardando noticias: ${e.message}", e)
        }
    }

    // === Firestore Operations ===

    /**
     * Obtiene noticias neutrales desde Firestore.
     * Hace query directa SOLO del día actual para máxima velocidad.
     */
    suspend fun fetchNeutralNewsFromFirestore(): List<NeutralNewsBean> {
        try {
            val todayRange = getTodayRange()

            // CRÍTICO: Query directa con filtro en Firestore, NO cargar todo
            // Convertir timestamps a Timestamp de Firebase
            val startTimestamp = com.google.firebase.Timestamp(todayRange.first / 1000, 0)
            val endTimestamp = com.google.firebase.Timestamp(todayRange.second / 1000, 0)

            Log.d("NewsDataManager", "Haciendo query solo de noticias del día desde Firestore...")

            val snapshot = FirebaseFirestore.getInstance()
                .collection("neutral_news")
                .whereGreaterThanOrEqualTo("date", startTimestamp)
                .whereLessThanOrEqualTo("date", endTimestamp)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.d("NewsDataManager", "No se encontraron noticias del día en Firestore")
                return emptyList()
            }

            val todayNews = snapshot.documents.mapNotNull { doc ->
                FirestoreDataMapper.mapNeutralNewsDocument(doc.data, doc.id)
            }

            Log.d("NewsDataManager", "Noticias del día cargadas desde Firestore: ${todayNews.size}")

            return todayNews.sortedByDescending { it.date ?: "" }
        } catch (e: Exception) {
            Log.e("NewsDataManager", "Error obteniendo noticias: ${e.localizedMessage}")
            return emptyList()
        }
    }

    /**
     * Obtiene las PRIMERAS N noticias de fechas específicas desde Firestore.
     * CRITICAL: Solo carga las primeras para mostrar rápido en UI.
     */
    suspend fun fetchFirstNeutralNewsForDates(dates: List<java.util.Date>, limit: Int = 10): List<NeutralNewsBean> {
        if (dates.isEmpty()) return emptyList()

        try {
            val allNews = mutableListOf<NeutralNewsBean>()

            val dateRanges = dates.map { date ->
                val cal = java.util.Calendar.getInstance()
                cal.time = date
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                cal.set(java.util.Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                Pair(start, end)
            }

            Log.d("NewsDataManager", "Cargando PRIMERAS $limit noticias para ${dateRanges.size} fechas...")

            // CRITICAL: Cargar solo con límite para mostrar rápido
            for ((start, end) in dateRanges) {
                val startTimestamp = com.google.firebase.Timestamp(start / 1000, 0)
                val endTimestamp = com.google.firebase.Timestamp(end / 1000, 0)

                val snapshot = FirebaseFirestore.getInstance()
                    .collection("neutral_news")
                    .whereGreaterThanOrEqualTo("date", startTimestamp)
                    .whereLessThanOrEqualTo("date", endTimestamp)
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()

                val newsForDate = snapshot.documents.mapNotNull { doc ->
                    FirestoreDataMapper.mapNeutralNewsDocument(doc.data, doc.id)
                }

                allNews.addAll(newsForDate)

                // Si ya tenemos suficientes, parar
                if (allNews.size >= limit) break
            }

            Log.d("NewsDataManager", "Primeras ${allNews.size} noticias cargadas (para UI)")

            return allNews.sortedByDescending { it.date ?: "" }.take(limit)
        } catch (e: Exception) {
            Log.e("NewsDataManager", "Error obteniendo primeras noticias: ${e.localizedMessage}")
            return emptyList()
        }
    }

    /**
     * Obtiene TODAS las noticias neutrales de fechas específicas desde Firestore.
     * CRITICAL: Se usa en BACKGROUND después de mostrar las primeras.
     */
    suspend fun fetchAllNeutralNewsForDates(dates: List<java.util.Date>): List<NeutralNewsBean> {
        if (dates.isEmpty()) return emptyList()

        try {
            val allNews = mutableListOf<NeutralNewsBean>()

            val dateRanges = dates.map { date ->
                val cal = java.util.Calendar.getInstance()
                cal.time = date
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                cal.set(java.util.Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                Pair(start, end)
            }

            Log.d("NewsDataManager", "Cargando TODAS las noticias restantes en background...")

            // Cargar TODAS sin límite
            for ((start, end) in dateRanges) {
                val startTimestamp = com.google.firebase.Timestamp(start / 1000, 0)
                val endTimestamp = com.google.firebase.Timestamp(end / 1000, 0)

                val snapshot = FirebaseFirestore.getInstance()
                    .collection("neutral_news")
                    .whereGreaterThanOrEqualTo("date", startTimestamp)
                    .whereLessThanOrEqualTo("date", endTimestamp)
                    .get()
                    .await()

                val newsForDate = snapshot.documents.mapNotNull { doc ->
                    FirestoreDataMapper.mapNeutralNewsDocument(doc.data, doc.id)
                }

                allNews.addAll(newsForDate)
            }

            Log.d("NewsDataManager", "TODAS las noticias cargadas en background: ${allNews.size} totales")

            return allNews.sortedByDescending { it.date ?: "" }
        } catch (e: Exception) {
            Log.e("NewsDataManager", "Error obteniendo todas las noticias: ${e.localizedMessage}")
            return emptyList()
        }
    }

    /**
     * Obtiene noticias de un grupo específico desde Firestore.
     */
    suspend fun fetchNewsByGroup(groupId: Int): List<NewsBean> {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("news")
                .whereEqualTo("group", groupId)
                .whereNotEqualTo("description", "")
                .get()
                .await()

            if (snapshot.isEmpty) return emptyList()

            return snapshot.documents.mapNotNull { doc ->
                FirestoreDataMapper.mapNewsDocument(doc.data ?: return@mapNotNull null, doc.id)
            }
        } catch (e: Exception) {
            Log.e("NewsDataManager", "Error obteniendo noticias del grupo $groupId: ${e.localizedMessage}")
            return emptyList()
        }
    }

    /**
     * Verifica actualizaciones desde Firestore sin mostrar loading.
     * Solo compara las del día actual.
     */
    suspend fun verifyUpdatesFromFirestore(currentNews: List<NeutralNewsBean>): UpdateResult {
        try {
            val fetchedNews = fetchNeutralNewsFromFirestore()
            return compareAndGetUpdates(currentNews, fetchedNews)
        } catch (e: Exception) {
            Log.e("NewsDataManager", "Error verificando actualizaciones: ${e.localizedMessage}")
            return UpdateResult(emptyList(), emptyList(), false)
        }
    }

    /**
     * Verifica actualizaciones de un grupo específico.
     */
    suspend fun verifyGroupUpdatesFromFirestore(
        groupId: Int,
        currentGroupNews: List<NewsBean>
    ): UpdateResult {
        try {
            val fetchedNews = fetchNewsByGroup(groupId)
            return compareAndGetUpdates(currentGroupNews, fetchedNews)
        } catch (e: Exception) {
            Log.e("NewsDataManager", "Error verificando grupo $groupId: ${e.localizedMessage}")
            return UpdateResult(emptyList(), emptyList(), false)
        }
    }

    // === Utilidades ===

    /**
     * Obtiene el rango de timestamps del día actual del usuario.
     */
    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Inicio del día
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        // Fin del día
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    /**
     * Compara dos listas y devuelve las actualizaciones y nuevas noticias.
     */
    private fun <T : Any> compareAndGetUpdates(
        currentList: List<T>,
        fetchedList: List<T>
    ): UpdateResult {
        val currentIds = currentList.map { getNewsId(it) }.toSet()

        val brandNew = fetchedList.filter { getNewsId(it) !in currentIds }
        val potentialUpdates = fetchedList.filter { getNewsId(it) in currentIds }

        val verified = potentialUpdates.filter { fetched ->
            val existing = currentList.find { getNewsId(it) == getNewsId(fetched) }
            existing?.let { hasChanged(it, fetched) } ?: false
        }

        return UpdateResult(brandNew, verified, brandNew.isNotEmpty() || verified.isNotEmpty())
    }

    private fun getNewsId(item: Any): String = when (item) {
        is NeutralNewsBean -> item.id
        is NewsBean -> item.id
        else -> ""
    }

    private fun hasChanged(existing: Any, fetched: Any): Boolean = when {
        existing is NeutralNewsBean && fetched is NeutralNewsBean -> {
            existing.neutralTitle != fetched.neutralTitle ||
            existing.neutralDescription != fetched.neutralDescription ||
            existing.date != fetched.date ||
            existing.updatedAt != fetched.updatedAt
        }
        existing is NewsBean && fetched is NewsBean -> {
            existing.title != fetched.title ||
            existing.description != fetched.description ||
            existing.pubDate != fetched.pubDate ||
            existing.updatedAt != fetched.updatedAt
        }
        else -> false
    }

    /**
     * Clase para retornar resultados de actualización.
     */
    data class UpdateResult(
        val newItems: List<Any>,
        val updatedItems: List<Any>,
        val hasChanges: Boolean
    )
}

