package com.example.neutralnews_android.ui.main.today

import android.util.Log
import com.example.neutralnews_android.data.Constants.NeutralNew.DATE
import com.example.neutralnews_android.data.Constants.NeutralNew.IMAGE_MEDIUM
import com.example.neutralnews_android.data.Constants.NeutralNew.NEUTRAL_DESCRIPTION
import com.example.neutralnews_android.data.Constants.NeutralNew.NEUTRAL_TITLE
import com.example.neutralnews_android.data.Constants.NeutralNew.SOURCE_IDS
import com.example.neutralnews_android.data.Constants.News.CATEGORY
import com.example.neutralnews_android.data.Constants.News.CREATED_AT
import com.example.neutralnews_android.data.Constants.News.DESCRIPTION
import com.example.neutralnews_android.data.Constants.News.GROUP
import com.example.neutralnews_android.data.Constants.News.IMAGE_URL
import com.example.neutralnews_android.data.Constants.News.LINK
import com.example.neutralnews_android.data.Constants.News.NEUTRAL_SCORE
import com.example.neutralnews_android.data.Constants.News.PUB_DATE
import com.example.neutralnews_android.data.Constants.News.RELEVANCE
import com.example.neutralnews_android.data.Constants.News.SCRAPED_DESCRIPTION
import com.example.neutralnews_android.data.Constants.News.SOURCE_MEDIUM
import com.example.neutralnews_android.data.Constants.News.TITLE
import com.example.neutralnews_android.data.Constants.News.UPDATED_AT
import com.example.neutralnews_android.data.bean.news.NeutralNewsBean
import com.example.neutralnews_android.data.bean.news.NewsBean
import com.example.neutralnews_android.data.bean.news.pressmedia.MediaBean
import com.example.neutralnews_android.util.string.normalized
import com.google.firebase.Timestamp

/**
 * Helper class for mapping Firestore documents to data beans.
 */
object FirestoreDataMapper {

    /**
     * Mapea un documento de noticia neutral desde Firestore.
     */
    fun mapNeutralNewsDocument(data: Map<String, Any>?, docId: String): NeutralNewsBean? {
        if (data == null) return null

        val neutralTitle = data[NEUTRAL_TITLE] as? String ?: return null
        val neutralDescription = data[NEUTRAL_DESCRIPTION] as? String ?: return null
        val group = data[GROUP] as? Long ?: return null
        val category = data[CATEGORY] as? String ?: return null
        val imageUrl = data[IMAGE_URL] as? String
        val createdAt = data[CREATED_AT] as? Timestamp
        val date = data[DATE] as? Timestamp
        val updatedAt = data[UPDATED_AT] as? Timestamp
        @Suppress("UNCHECKED_CAST")
        val sourceIds = data[SOURCE_IDS] as? List<String>
        val relevance = data[RELEVANCE] as? Double ?: (data[RELEVANCE] as? Long)?.toDouble()
        val imageMediumRaw = data[IMAGE_MEDIUM] as? String

        val formattedCreatedAt = DateFormatterHelper.formatDateToSpanish(createdAt)
        val formattedDate = DateFormatterHelper.formatDateToSpanish(date)
        val formattedUpdatedAt = DateFormatterHelper.formatDateToSpanish(updatedAt)

        val imageMedium = MediaBean.entries.find {
            it.pressMedia.name?.normalized() == imageMediumRaw?.normalized()
        }?.pressMedia?.name

        if (imageMediumRaw == null) {
            Log.e("FirestoreDataMapper", "sourceMedium not found for: $imageMediumRaw")
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
     * Mapea un documento de noticia regular desde Firestore.
     */
    fun mapNewsDocument(data: Map<String, Any>, docId: String): NewsBean? {
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
            Log.e("FirestoreDataMapper", "sourceMedium not found for: $sourceMediumRaw")
            return null
        }

        val newsDescription = scrapedDescription?.takeIf { it.isNotEmpty() } ?: description

        if (newsDescription.isEmpty()) {
            Log.e("FirestoreDataMapper", "newsDescription is null or empty")
            return null
        }

        return NewsBean(
            id = docId,
            title = title,
            description = newsDescription,
            category = category ?: "",
            imageUrl = imageUrl ?: "",
            link = link,
            pubDate = DateFormatterHelper.formatDateToSpanish(pubDate),
            createdAt = DateFormatterHelper.formatDateToSpanish(createdAt),
            updatedAt = DateFormatterHelper.formatDateToSpanish(updatedAt),
            sourceMedium = sourceMedium,
            group = group.toInt(),
            neutralScore = neutralScore?.toFloat(),
        )
    }
}

