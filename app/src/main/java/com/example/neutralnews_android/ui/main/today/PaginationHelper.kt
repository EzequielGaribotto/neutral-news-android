package com.example.neutralnews_android.ui.main.today

import com.example.neutralnews_android.data.bean.news.NeutralNewsBean
import com.example.neutralnews_android.ui.main.today.TodaySortType.*

/**
 * Helper que centraliza la paginación local y la deduplicación de elementos.
 * Mantiene índices y conjuntos de IDs vistos para evitar insertar duplicados
 * cuando el usuario sube y baja la lista.
 */
class PaginationHelper(private val pageSize: Int = 10, private val initialDisplayLimit: Int = 15) {

    private var cachedNeutralAll: List<NeutralNewsBean> = emptyList()

    private var cachedNeutralIndex = 0

    // Conjuntos para evitar duplicados persistentes
    private val seenNeutralIds = LinkedHashSet<String>()

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
        currentNeutral: MutableList<NeutralNewsBean>
    ): List<NeutralNewsBean> {
        // inicializar conjuntos vistos con lo que ya está en memoria
        seenNeutralIds.clear()
        // NewsBean.id es non-null según la definición del modelo, por eso podemos usar map
        seenNeutralIds.addAll(currentNeutral.map { it.id })

        val neuInitial = takeUniqueFromCachedNeutral(initialDisplayLimit)

        return neuInitial
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
        existingNeutral: MutableList<NeutralNewsBean>
    ): List<NeutralNewsBean> {
        // Asegurar que los conjuntos vistos contienen lo que ya hay en la UI
        seenNeutralIds.addAll(existingNeutral.map { it.id })

        val neuPage = takeUniqueFromCachedNeutral(pageSize)

        return neuPage
    }

    fun sortCachedLists(sortType: TodaySortType) {

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
        cachedNeutralIndex = 0
    }

}
