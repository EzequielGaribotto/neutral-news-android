package com.example.neutralnews_android.data.bean.news

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.UUID

/**
 * Clase de datos que representa una noticia neutral.
 *
 * @property id Identificador único de la noticia.
 * @property neutralTitle Título neutral de la noticia.
 * @property neutralDescription Descripción neutral de la noticia.
 * @property category Categoría de la noticia.
 * @property imageUrl URL de la imagen de la noticia.
 * @property group Grupo de la noticia.
 * @property createdAt Fecha de creación de la noticia.
 */
data class NeutralNewsBean(
    @SerializedName("id")
    val id: String,

    @SerializedName("neutralTitle")
    val neutralTitle: String,

    @SerializedName("neutralDescription")
    val neutralDescription: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("imageUrl")
    val imageUrl: String? = null,

    @SerializedName("group")
    val group: Int,

    @SerializedName("updatedAt")
    val updatedAt: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("date")
    val date: String? = null,

    @SerializedName("sourceIds")
    val sourceIds: List<String>? = null,

    @SerializedName("relevance")
    val relevance: Double? = null,

    @SerializedName("imageMedium") val imageMedium: String? = null,

    ) : Serializable {
    companion object {
        val mock = NeutralNewsBean(
            neutralTitle = "Lorem ipsum dolor sit amet",
            neutralDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            category = "Category",
            imageUrl = null,
            group = 0,
            createdAt = "2023-10-01",
            date = "2023-10-01",
            sourceIds = listOf("source1", "source2"),
            id = UUID.randomUUID().toString(),
        )
    }
}