package com.example.neutralnews_android.data.bean.filter

import com.example.neutralnews_android.data.bean.news.pressmedia.PressMediaBean
import com.example.neutralnews_android.data.bean.tag.TagBean
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

/**
 * Representa un filtro con diversas propiedades para su uso en la aplicación.
 *
 * @property id Identificador único del filtro.
 * @property name Nombre del filtro.
 * @property type Tipo de filtro.
 * @property options Lista de opciones anidadas dentro del filtro.
 * @property resId Identificador de recurso opcional.
 * @property value Valor asociado al filtro.
 * @property isSelected Indica si el filtro está seleccionado.
 */
data class FilterBean(

    @SerializedName("id") val id: Long = 0,

    @SerializedName("name") val name: String? = null,

    @SerializedName("type") val type: String? = null,

    @SerializedName("options") val options: ArrayList<FilterBean>? = ArrayList(),

    var resId: Int? = null,

    @SerializedName("value") val value: String? = null,

    var isSelected: Boolean = false
) : Serializable

/**
 * Representa un conjunto de filtros locales con fechas y categorías.
 *
 * @property startDate Fecha de inicio del filtro.
 * @property endDate Fecha de finalización del filtro.
 * @property media Lista de filtros de medios.
 * @property categories Lista de filtros de categorías.
 * @property mediaTagBean Lista de etiquetas asociadas a los medios.
 * @property categoryTagBean Lista de etiquetas asociadas a las categorías.
 */
data class LocalFilterBean(
    var startDate: String? = "",
    var endDate: String? = "",
    var media: ArrayList<FilterBean>? = ArrayList(),
    var categories: ArrayList<FilterBean>? = ArrayList(),
    var mediaTagBean: List<TagBean>? = null,
    var categoryTagBean: List<TagBean>? = null,
    var dateFilter: Date? = null,
    var selectedDates: List<Date>? = null,
    var isOlderThan: Boolean = false,
)
