package com.example.neutralnews_android.ui.main.today

import com.example.neutralnews_android.data.bean.news.NewsBean
import com.example.neutralnews_android.data.bean.news.NeutralNewsBean
import com.example.neutralnews_android.ui.main.today.TodaySortType
import com.example.neutralnews_android.ui.main.today.TodaySortType.*

/**
 * Helper que centraliza la paginación local y la deduplicación de elementos.
 * Mantiene índices y conjuntos de IDs vistos para evitar insertar duplicados
 * cuando el usuario sube y baja la lista.
 */
class PaginationHelper(private val pageSize: Int = 10, private val initialDisplayLimit: Int = 15) {

    private var cachedRegularAll: List<NewsBean> = emptyList()
    private var cachedNeutralAll: List<NeutralNewsBean> = emptyList()

    private var cachedRegularIndex = 0
    private var cachedNeutralIndex = 0

    // Conjuntos para evitar duplicados persistentes
    private val seenRegularIds = LinkedHashSet<String>()
    private val seenNeutralIds = LinkedHashSet<String>()

    fun setCachedRegularAll(list: List<NewsBean>) {
        cachedRegularAll = list
        cachedRegularIndex = 0
    }

    fun setCachedNeutralAll(list: List<NeutralNewsBean>) {
        cachedNeutralAll = list
        cachedNeutralIndex = 0
    }

    /**
     * Prepara la visualización inicial tomando hasta [initialDisplayLimit] elementos únicos
     * desde las listas en caché. Se consideran los elementos ya presentes en [currentRegular]
     * y [currentNeutral] para evitar duplicados.
     */
    fun prepareInitialDisplay(
        currentRegular: MutableList<NewsBean>,
        currentNeutral: MutableList<NeutralNewsBean>
    ): Pair<List<NewsBean>, List<NeutralNewsBean>> {
        // inicializar conjuntos vistos con lo que ya está en memoria
        seenRegularIds.clear()
        seenNeutralIds.clear()
        // NewsBean.id es non-null según la definición del modelo, por eso podemos usar map
        seenRegularIds.addAll(currentRegular.map { it.id })
        seenNeutralIds.addAll(currentNeutral.map { it.id })

        val regInitial = takeUniqueFromCachedRegular(initialDisplayLimit)
        val neuInitial = takeUniqueFromCachedNeutral(initialDisplayLimit)

        return Pair(regInitial, neuInitial)
    }

    private fun takeUniqueFromCachedRegular(limit: Int): List<NewsBean> {
        val out = mutableListOf<NewsBean>()
        while (out.size < limit && cachedRegularIndex < cachedRegularAll.size) {
            val item = cachedRegularAll[cachedRegularIndex++]
            val id = item.id
            // id no es null según el modelo, comprobar solo duplicados
            if (seenRegularIds.contains(id)) continue
            seenRegularIds.add(id)
            out.add(item)
        }
        return out
    }

    private fun takeUniqueFromCachedNeutral(limit: Int): List<NeutralNewsBean> {
        val out = mutableListOf<NeutralNewsBean>()
        while (out.size < limit && cachedNeutralIndex < cachedNeutralAll.size) {
            val item = cachedNeutralAll[cachedNeutralIndex++]
            val id = item.id
            if (seenNeutralIds.contains(id)) continue
            seenNeutralIds.add(id)
            out.add(item)
        }
        return out
    }

    /**
     * Carga la "siguiente página" desde las listas cacheadas. Devuelve pares de listas
     * (regulares, neutrales) conteniendo únicamente elementos nuevos respecto a [existingRegular]
     * y [existingNeutral].
     */
    fun loadNextPage(
        existingRegular: MutableList<NewsBean>,
        existingNeutral: MutableList<NeutralNewsBean>
    ): Pair<List<NewsBean>, List<NeutralNewsBean>> {
        // Asegurar que los conjuntos vistos contienen lo que ya hay en la UI
        if (seenRegularIds.isEmpty()) seenRegularIds.addAll(existingRegular.map { it.id })
        if (seenNeutralIds.isEmpty()) seenNeutralIds.addAll(existingNeutral.map { it.id })

        val regPage = takeUniqueFromCachedRegular(pageSize)
        val neuPage = takeUniqueFromCachedNeutral(pageSize)

        return Pair(regPage, neuPage)
    }

    fun hasMore(): Boolean {
        return cachedRegularIndex < cachedRegularAll.size || cachedNeutralIndex < cachedNeutralAll.size
    }

    fun sortCachedLists(sortType: TodaySortType) {
        cachedRegularAll = when (sortType) {
            DATE_DESC -> cachedRegularAll.sortedByDescending { it.pubDate ?: it.createdAt ?: it.updatedAt ?: "" }
            DATE_ASC -> cachedRegularAll.sortedBy { it.pubDate ?: it.createdAt ?: it.updatedAt ?: "" }
            UPDATED_AT_DESC -> cachedRegularAll.sortedByDescending { it.updatedAt ?: it.createdAt ?: it.pubDate ?: "" }
            UPDATED_AT_ASC -> cachedRegularAll.sortedBy { it.updatedAt ?: it.createdAt ?: it.pubDate ?: "" }
            RELEVANCE_DESC -> cachedRegularAll.sortedByDescending { it.relevance ?: 0.0 }
            RELEVANCE_ASC -> cachedRegularAll.sortedBy { it.relevance ?: 0.0 }
            SOURCES_DESC -> cachedRegularAll.sortedByDescending { it.sourceIds?.size ?: 0 }
            SOURCES_ASC -> cachedRegularAll.sortedBy { it.sourceIds?.size ?: 0 }
        }
        cachedNeutralAll = when (sortType) {
            DATE_DESC -> cachedNeutralAll.sortedByDescending { it.date ?: it.createdAt ?: it.updatedAt ?: "" }
            DATE_ASC -> cachedNeutralAll.sortedBy { it.date ?: it.createdAt ?: it.updatedAt ?: "" }
            UPDATED_AT_DESC -> cachedNeutralAll.sortedByDescending { it.updatedAt ?: it.createdAt ?: it.date ?: "" }
            UPDATED_AT_ASC -> cachedNeutralAll.sortedBy { it.updatedAt ?: it.createdAt ?: it.date ?: "" }
            RELEVANCE_DESC -> cachedNeutralAll.sortedByDescending { it.relevance ?: 0.0 }
            RELEVANCE_ASC -> cachedNeutralAll.sortedBy { it.relevance ?: 0.0 }
            SOURCES_DESC -> cachedNeutralAll.sortedByDescending { it.sourceIds?.size ?: 0 }
            SOURCES_ASC -> cachedNeutralAll.sortedBy { it.sourceIds?.size ?: 0 }
        }
        // Reset indices after sorting
        cachedRegularIndex = 0
        cachedNeutralIndex = 0
    }

}
