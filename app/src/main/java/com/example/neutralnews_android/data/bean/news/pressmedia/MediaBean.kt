package com.example.neutralnews_android.data.bean.news.pressmedia

import com.example.neutralnews_android.util.string.normalized

/**
 * Representa los diferentes medios de prensa disponibles en la aplicación.
 */
enum class MediaBean {
    ABC,
    EL_PAIS,
    RTVE,
    ANTENA3,
    COPE,
    DIARIORED,
    EL_ECONOMISTA,
    EL_SALTO,
    EL_DIARIO,
    EXPANSION,
    LA_SEXTA,
    EL_MUNDO,
    ES_DIARIO,
    EL_PERIODICO,
    LA_VANGUARDIA,
    LIBERTAD_DIGITAL {

    };

    /**
     * Obtiene la instancia de [PressMediaBean] correspondiente al medio de prensa.
     *
     * @return Un objeto [PressMediaBean] con el nombre y el enlace RSS del medio de prensa.
     */
    val pressMedia: PressMediaBean
        get() = when (this) {
            ABC -> PressMediaBean(
                name = "ABC", link = "https://www.abc.es/rss/2.0/portada/"
            )

            ANTENA3 -> PressMediaBean(
                name = "Antena 3", link = "https://www.antena3.com/rss/feeds/portada"
            )

            COPE -> PressMediaBean(
                name = "COPE", link = "https://www.cope.es/rss/actualidad/espana"
            )

            DIARIORED -> PressMediaBean(
                name = "Diario Red", link = "https://www.diariored.com/rss"
            )

            EL_DIARIO -> PressMediaBean(
                name = "El Diario", link = "https://www.eldiario.es/rss"
            )

            EL_ECONOMISTA -> PressMediaBean(
                name = "El Confidencial", link = "https://www.elconfidencial.com/rss"
            )

            EL_SALTO -> PressMediaBean(
                name = "El Salto", link = "https://www.elsaltodiario.com/feed"
            )

            ES_DIARIO -> PressMediaBean(
                name = "Es Diario", link = "https://www.esdiario.com/rss"
            )

            EXPANSION -> PressMediaBean(
                    name = "Expansión", link = "https://www.expansion.com/rss/portada.xml"
                )

            LA_SEXTA -> PressMediaBean(
                name = "La Sexta", link = "https://www.lasexta.com/rss/feeds/portada"
            )

            EL_PAIS -> PressMediaBean(
                name = "El País",
                link = "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/portada"
            )

            RTVE -> PressMediaBean(
                name = "RTVE", link = "https://api2.rtve.es/rss/temas_noticias.xml"
            )

            EL_MUNDO -> PressMediaBean(
                name = "El Mundo", link = "https://e00-elmundo.uecdn.es/elmundo/rss/portada.xml"
            )

            EL_PERIODICO -> PressMediaBean(
                name = "El Periódico", link = "https://www.elperiodico.com/es/rss/rss_portada.xml"
            )

            LA_VANGUARDIA -> PressMediaBean(
                name = "La Vanguardia", link = "https://www.lavanguardia.com/mvc/feed/rss/home"
            )

            LIBERTAD_DIGITAL -> PressMediaBean(
                name = "Libertad Digital", link = "https://www.libertaddigital.com/rss/"
            )
        }
}
