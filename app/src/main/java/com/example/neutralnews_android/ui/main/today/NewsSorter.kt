package com.example.neutralnews_android.ui.main.today

import com.example.neutralnews_android.data.bean.news.NeutralNewsBean
import com.example.neutralnews_android.data.bean.news.NewsBean

/**
 * Helper class for sorting news based on different criteria.
 */
object NewsSorter {

    /**
     * Ordena las noticias neutrales según el tipo de ordenación especificado.
     */
    fun sortNeutralNews(
        news: List<NeutralNewsBean>,
        sortType: TodaySortType
    ): List<NeutralNewsBean> {
        return when (sortType) {
            TodaySortType.DATE_DESC -> news.sortedByDescending {
                DateFormatterHelper.parseDateToTimestamp(it.date)
            }
            TodaySortType.DATE_ASC -> news.sortedBy {
                DateFormatterHelper.parseDateToTimestamp(it.date)
            }
            TodaySortType.UPDATED_AT_DESC -> news.sortedByDescending {
                DateFormatterHelper.parseDateToTimestamp(it.updatedAt)
            }
            TodaySortType.UPDATED_AT_ASC -> news.sortedBy {
                DateFormatterHelper.parseDateToTimestamp(it.updatedAt)
            }
            TodaySortType.RELEVANCE_DESC -> news.sortedByDescending { it.relevance ?: 0.0 }
            TodaySortType.RELEVANCE_ASC -> news.sortedBy { it.relevance ?: 0.0 }
            TodaySortType.SOURCES_DESC -> news.sortedByDescending { it.sourceIds?.size ?: 0 }
            TodaySortType.SOURCES_ASC -> news.sortedBy { it.sourceIds?.size ?: 0 }
        }
    }

    /**
     * Agrupa y filtra las noticias regulares por grupo y fuente.
     */
    fun filterGroupedNews(fetchedNews: List<NewsBean>): List<List<NewsBean>> {
        val validNews = fetchedNews.filter { it.group != null && it.group >= 0 }
        val groupedNews = validNews.groupBy { it.group }
        val nonSingletonGroups = groupedNews.filter { (_, newsList) -> newsList.size > 1 }

        val mostRecentPerSourcePerGroup = nonSingletonGroups.mapValues { (_, newsList) ->
            newsList.groupBy { it.sourceMedium }.mapValues { (_, sourceNewsList) ->
                sourceNewsList.maxByOrNull {
                    DateFormatterHelper.getDateForSorting(it.date)?.time ?: Long.MIN_VALUE
                }!!
            }.values.sortedByDescending {
                DateFormatterHelper.getDateForSorting(it.date)?.time ?: Long.MIN_VALUE
            }
        }

        val sortedGroups = mostRecentPerSourcePerGroup.values.sortedByDescending { group ->
            DateFormatterHelper.getDateForSorting(group.firstOrNull()?.date)?.time ?: Long.MIN_VALUE
        }

        return sortedGroups.map { group ->
            group.map { news ->
                news.copy(pubDate = DateFormatterHelper.formatDateToSpanish(news.pubDate))
            }
        }
    }
}

