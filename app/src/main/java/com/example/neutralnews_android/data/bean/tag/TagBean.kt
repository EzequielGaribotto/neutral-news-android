package com.example.neutralnews_android.data.bean.tag

/**
 * Representa una etiqueta que puede estar asociada a diferentes elementos dentro de la aplicación, en este caso, las opciones de filtro por categorías
 *
 * @property id Identificador único de la etiqueta.
 * @property name Nombre principal de la etiqueta.
 * @property names Lista de nombres alternativos de la etiqueta.
 * @property isSelected Indica si la etiqueta está seleccionada.
 */
data class TagBean(
    val id: Long = 0,
    val name: String? = null,
    val names: Array<String>? = emptyArray(), var isSelected: Boolean = true
) {
    /**
     * Constructor que inicializa una etiqueta con un único nombre.
     *
     * @param id Identificador único de la etiqueta.
     * @param name Nombre principal de la etiqueta.
     * @param userSubscribeId Identificador de suscripción del usuario.
     * @param isSelected Indica si la etiqueta está seleccionada.
     */
    constructor(id: Long, name: String?, isSelected: Boolean) : this(
        id,
        name,
        emptyArray(),
        isSelected
    ) {
        this.isSelected = isSelected
    }

    /**
     * Constructor que inicializa una etiqueta con múltiples nombres alternativos.
     *
     * @param id Identificador único de la etiqueta.
     * @param names Lista de nombres alternativos.
     * @param isSelected Indica si la etiqueta está seleccionada.
     */
    constructor(id: Long, names: Array<String>, isSelected: Boolean) : this(
        id,
        null,
        names,
        isSelected
    ) {
        this.isSelected = isSelected
    }

    /**
     * Verifica si dos objetos [TagBean] son iguales basándose en su id, nombre y userSubscribeId.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TagBean) return false
        return id == other.id && name == other.name
    }

    /**
     * Genera un código hash basado en id, nombre, userSubscribeId e isSelected.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + isSelected.hashCode()
        return result
    }
}
