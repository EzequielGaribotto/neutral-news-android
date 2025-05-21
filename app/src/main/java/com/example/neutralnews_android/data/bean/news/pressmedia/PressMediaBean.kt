package com.example.neutralnews_android.data.bean.news.pressmedia

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Representa un medio de prensa con su nombre y enlace asociado.
 *
 * @property name Nombre del medio de prensa.
 * @property link Enlace al sitio web o fuente RSS del medio de prensa.
 */
data class PressMediaBean(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("link")
    val link: String? = null

) : Serializable {
    companion object {
        /**
         * Instancia de prueba de un medio de prensa.
         */
        val mock = PressMediaBean(
            name = "El País",
            link = "https://www.elpais.com"
        )
    }
}
