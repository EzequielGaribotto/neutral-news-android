package com.example.neutralnews_android.data

object Constants {

    object PrefsKeys {
        const val EN = "en"
        const val ES = "es"
        const val SELECTED_LANGUAGE = "selectedLanguage"
    }

    object BottomMenu {
        const val OPTION_TODAY: Int = 1
        const val OPTION_SETTINGS: Int = 2
    }

    object ApiObject {
        const val TYPE = "type"
        const val FILTER_COUNT = "filterCount"
        const val FILTER_DATA = "filterData"
    }

    object LocalNew {
        const val NEWS_DATABASE: String = "news_database"
        const val NEW_ID: String = "newId"
        const val NEW_DATA: String = "newDetail"
        const val NEWS_LIST: String = "newsList"
    }

    object News {
        const val NEWS: String = "news"
        const val ID: String = "id"
        const val TITLE: String = "title"
        const val DESCRIPTION: String = "description"
        const val SCRAPED_DESCRIPTION: String = "scraped_description"
        const val CATEGORY: String = "category"
        const val IMAGE_URL: String = "image_url"
        const val LINK: String = "link"
        const val SOURCE_MEDIUM: String = "source_medium"
        const val GROUP: String = "group"
        const val NEUTRAL_SCORE: String = "neutral_score"
        const val PUB_DATE: String = "pub_date"
        const val CREATED_AT: String = "created_at"
        const val UPDATED_AT: String = "updated_at"
        const val RELEVANCE: String = "relevance"
    }

    object NeutralNew {
        const val NEUTRAL_NEWS: String = "neutral_news"
        const val NEUTRAL_TITLE: String = "neutral_title"
        const val NEUTRAL_DESCRIPTION: String = "neutral_description"
        const val DATE: String = "date"
        const val IMAGE_MEDIUM: String = "image_medium"
        const val SOURCE_IDS: String = "source_ids"
    }

    object DateFormat {
        const val DATE_FORMAT = "dd 'DE' MMMM, HH:mm"
    }
}