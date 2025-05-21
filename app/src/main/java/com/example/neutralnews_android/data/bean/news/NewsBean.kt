package com.example.neutralnews_android.data.bean.news

import com.example.neutralnews_android.data.bean.news.pressmedia.PressMediaBean
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.UUID

/**
 * Clase de datos que representa una noticia.
 *
 * @property id Identificador único de la noticia.
 * @property title Título de la noticia.
 * @property description Descripción de la noticia.
 * @property category Categoría de la noticia.
 * @property categories Lista de categorías de la noticia.
 * @property imageUrl URL de la imagen de la noticia.
 * @property link Enlace a la noticia.
 * @property pubDate Fecha de publicación de la noticia.
 * @property sourceMedium Medio de origen de la noticia.
 */

data class NewsBean(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("category")
    val category: String? = null,

    @SerializedName("categories")
    val categories: List<String>? = null,

    @SerializedName("imageUrl")
    var imageUrl:String? = null,

    @SerializedName("link")
    val link: String? = null,

    @SerializedName("neutralScore")
    val neutralScore: Float? = null,

    @SerializedName("pubDate")
    val pubDate: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null,

    @SerializedName("date")
    val date: String? = null,

    @SerializedName("sourceMedium")
    val sourceMedium: PressMediaBean? = PressMediaBean(),

    @SerializedName("relevance")
    val relevance: Double? = null,

    @SerializedName("sourceIds")
    val sourceIds: List<String>? = null,

    @SerializedName("imageMedium") val imageMedium: String? = null,

    val group: Int? = null,
) : Serializable {
    companion object {
        val mock = NewsBean(
            title = "Lorem ipsum dolor sit amet",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            category = "Category",
            categories = arrayListOf("Category1", "Category2"),
            imageUrl = null,
            neutralScore = null,
            link = "Link",
            pubDate = "PubDate",
            createdAt = "2023-10-01",
            updatedAt = "2023-10-01",
            sourceMedium = PressMediaBean.mock,
            group = null,
        )
    }
}