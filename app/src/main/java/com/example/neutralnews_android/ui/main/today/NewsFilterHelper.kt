package com.example.neutralnews_android.ui.main.today

import android.util.Log
import com.example.neutralnews_android.data.bean.filter.LocalFilterBean
import com.example.neutralnews_android.data.bean.news.NeutralNewsBean
import com.example.neutralnews_android.data.bean.news.NewsBean
import com.example.neutralnews_android.util.string.normalized
import java.util.*

/**
 * Helper class for filtering news based on various criteria.
 */
class NewsFilterHelper(private val groupsOfNews: Map<Int, List<NewsBean>>) {

    /**
     * Aplica filtros a las noticias neutrales.
     */
    fun applyFiltersToNeutralNews(
        newsItems: List<NeutralNewsBean>,
        filterData: LocalFilterBean
    ): List<NeutralNewsBean> {
        if (filterData == LocalFilterBean()) return newsItems

        return newsItems.asSequence()
            .filter { newsItem ->
                applyCategoryFilter(filterData, newsItem) &&
                        applyMediaFilter(filterData, newsItem) &&
                        applyDateFilter(filterData, newsItem)
            }.toList()
    }

    /**
     * Busca dentro de la colección de noticias neutrales.
     */
    fun searchInNeutralNews(
        news: List<NeutralNewsBean>,
        query: String
    ): List<NeutralNewsBean> {
        if (query.isEmpty()) return news

        // Búsqueda especializada por grupo
        if (query.startsWith("g", ignoreCase = true) && query.length > 1) {
            val groupId = query.substring(1).toIntOrNull()
            if (groupId != null) {
                Log.d("NewsFilterHelper", "Búsqueda por grupo específico: $groupId")
                return news.filter { it.group == groupId }
            }
        }

        // Búsqueda especializada por fuente
        if (query.startsWith("s", ignoreCase = true) && query.length > 1) {
            val sourceId = query.substring(1)
            Log.d("NewsFilterHelper", "Búsqueda por fuente específica: $sourceId")
            return news.filter { newsItem ->
                newsItem.sourceIds?.any { it.contains(sourceId, ignoreCase = true) } == true
            }
        }

        // Búsqueda estándar con prioridad
        val titleMatches = mutableListOf<NeutralNewsBean>()
        val descriptionMatches = mutableListOf<NeutralNewsBean>()
        val scrapedDescriptionMatches = mutableListOf<NeutralNewsBean>()

        news.forEach { newsItem ->
            when {
                newsItem.group.toString().contains(query, ignoreCase = true) ||
                        newsItem.id.contains(query, ignoreCase = true) ||
                        newsItem.sourceIds?.any { it.contains(query, ignoreCase = true) } == true ||
                        newsItem.neutralTitle.contains(query, ignoreCase = true) -> {
                    titleMatches.add(newsItem)
                }
                newsItem.neutralDescription.contains(query, ignoreCase = true) -> {
                    descriptionMatches.add(newsItem)
                }
                groupsOfNews[newsItem.group]?.any { relatedNews ->
                    relatedNews.description?.contains(query, ignoreCase = true) == true
                } == true -> {
                    scrapedDescriptionMatches.add(newsItem)
                }
            }
        }

        return titleMatches + descriptionMatches + scrapedDescriptionMatches
    }

    private fun applyCategoryFilter(filterData: LocalFilterBean, newsItem: NeutralNewsBean): Boolean {
        if (filterData.categoryTagBean.isNullOrEmpty()) return true
        return filterData.categoryTagBean?.any { it.name == newsItem.category } != false
    }

    private fun applyMediaFilter(filterData: LocalFilterBean, newsItem: NeutralNewsBean): Boolean {
        if (filterData.media?.isEmpty() == true) return true

        val sourceNews = groupsOfNews[newsItem.group]
        if (sourceNews.isNullOrEmpty()) return false

        return sourceNews.any { news ->
            val sourceMedium = news.sourceMedium?.name?.normalized()
            filterData.media?.any { it.name?.normalized() == sourceMedium } == true
        }
    }

    private fun applyDateFilter(filterData: LocalFilterBean, newsItem: NeutralNewsBean): Boolean {
        if (filterData.dateFilter == null && filterData.selectedDates.isNullOrEmpty()) return true

        val newsDate = DateFormatterHelper.getDateForSorting(newsItem.date)
        if (newsDate == null) {
            Log.d("NewsFilterHelper", "No se pudo obtener fecha para noticia: ${newsItem.neutralTitle}")
            return true
        }

        // Filtro múltiple - verificar si la fecha está en cualquiera de las fechas seleccionadas
        if (filterData.selectedDates != null && filterData.selectedDates!!.isNotEmpty()) {
            return isDateInAnySelectedDay(newsDate, filterData.selectedDates!!)
        }

        // Filtro simple
        val filterDate = filterData.dateFilter ?: return true
        val calendar = Calendar.getInstance().apply { time = filterDate }

        return if (filterData.isOlderThan) {
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            newsDate.before(calendar.time)
        } else {
            val startOfDay = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val endOfDay = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time

            (newsDate.after(startOfDay) || newsDate == startOfDay) &&
                    (newsDate.before(endOfDay) || newsDate == endOfDay)
        }
    }

    private fun isDateInAnySelectedDay(newsDate: Date, selectedDates: List<Date>): Boolean {
        val calendar = Calendar.getInstance()

        return selectedDates.any { selectedDate ->
            calendar.time = selectedDate

            val startOfDay = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val endOfDay = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time

            (newsDate.after(startOfDay) || newsDate == startOfDay) &&
                    (newsDate.before(endOfDay) || newsDate == endOfDay)
        }
    }
}

