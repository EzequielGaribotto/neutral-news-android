package com.example.neutralnews_android.util.tagview

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.neutralnews_android.data.bean.tag.TagBean
import com.example.neutralnews_android.databinding.RowCustomTagBinding
import kotlin.math.max

/**
 * Vista personalizada que se utiliza para mostrar un conjunto de etiquetas (`TagBean`) dentro de un `ViewGroup`.
 * Las etiquetas pueden ser agregadas, eliminadas o seleccionadas.
 *
 * @constructor Crea una nueva instancia de `TagView`.
 * @param context Contexto en el que se crea la vista.
 * @param attrs Atributos del XML que se pasan a la vista.
 */
class TagView(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

    /**
     * Lista que contiene las etiquetas (`TagBean`) actuales que se mostrarán en la vista.
     */
    private var tagsList: ArrayList<TagBean> = ArrayList()

    /**
     * Indica si las etiquetas son solo para visualización (no se pueden modificar).
     */
    var isViewOnly: Boolean = false

    /**
     * Indica si solo se puede seleccionar una etiqueta a la vez.
     */
    var isSingleSelection: Boolean = false

    /**
     * Configura las etiquetas que se mostrarán en la vista.
     *
     * @param tags Lista de objetos `TagBean` que se utilizarán para crear las vistas de etiquetas.
     */
    fun setTags(tags: ArrayList<TagBean>) {
        tagsList = tags
        removeAllViews()
        for (tagText in tags) {
            val tagView = createTagView(tagText)
            if (isViewOnly) {
                tagText.isSelected = true
            }
            addView(tagView)
        }
        requestLayout()
    }

    /**
     * Agrega nuevas etiquetas a la vista, asegurándose de no duplicar las existentes.
     *
     * @param tags Lista de objetos `TagBean` que se agregarán a las etiquetas existentes.
     */
    fun addTags(tags: ArrayList<TagBean>) {
        val newTags = tags.filter { newTag -> tagsList.none { it.id == newTag.id } }
        tagsList.addAll(newTags)
        for (tagText in newTags) {
            val tagView = createTagView(tagText)
            if (isViewOnly) {
                tagText.isSelected = true
            }
            addView(tagView)
        }
        requestLayout()
    }

    /**
     * Elimina las etiquetas especificadas de la vista.
     *
     * @param arrayList Lista de objetos `TagBean` que se eliminarán.
     */
    fun removeTags(arrayList: ArrayList<TagBean>) {

        val tags = tagsList.filter { tagBean -> arrayList.none { it.id == tagBean.id } }
        tagsList.clear()
        tagsList.addAll(tags)
        removeAllViews()
        for (tagText in tags) {
            val tagView = createTagView(tagText)
            if (isViewOnly) {
                tagText.isSelected = true
            }
            addView(tagView)
        }
        requestLayout()
    }

    /**
     * Crea una vista de etiqueta (`RowCustomTagBinding`) para un objeto `TagBean`.
     * También configura la interacción de selección de etiquetas.
     *
     * @param tagText Objeto `TagBean` que representa la etiqueta.
     * @return La vista que representa la etiqueta.
     */
    private fun createTagView(tagText: TagBean): View {

        val layoutInflater = LayoutInflater.from(this.context)

        val tagView = RowCustomTagBinding.inflate(layoutInflater, parent as ViewGroup?, false)

        tagView.bean = tagText
        if (isViewOnly.not()) {
            tagView.clMain.setOnClickListener {

                if (isSingleSelection) {
                    if (tagsList.any { bean -> bean.isSelected } && tagsList.find { bean -> bean.id == tagText.id }?.isSelected == true) {
                        tagsList.find { bean -> bean.isSelected }?.isSelected = false
                        setTags(tagsList)
                        Log.e("TAG", "createTagView: 1111")
                    } else {
                        tagsList.find { bean -> bean.isSelected }?.isSelected = false
                        tagText.isSelected = tagText.isSelected.not()
                        tagView.bean = tagText
                        tagsList.find { bean -> bean.id == tagText.id }?.isSelected = tagText.isSelected
                        setTags(tagsList)
                        Log.e("TAG", "createTagView: 2222")
                    }
                } else {
                    tagText.isSelected = tagText.isSelected.not()
                    tagView.bean = tagText
                    tagsList.find { bean -> bean.id == tagText.id }?.isSelected = tagText.isSelected
                }
            }
        } else {
            tagView.isViewOnly = true
        }

        return tagView.root
    }

    /**
     * Mide las dimensiones de la vista. Esto se utiliza para determinar el tamaño total necesario
     * para mostrar todas las etiquetas dentro del `ViewGroup`.
     *
     * @param widthMeasureSpec Especificación de medidas para el ancho de la vista.
     * @param heightMeasureSpec Especificación de medidas para la altura de la vista.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec)
        var widthUsed = paddingLeft + paddingRight
        var heightUsed = paddingTop + paddingBottom
        var rowHeight = 0
        var rowWidth = 0
        var totalHeight = paddingTop + paddingBottom

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            measureChildWithMargins(child, widthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed)

            val layoutParams = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + layoutParams.leftMargin + layoutParams.rightMargin
            val childHeight = child.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin

            if (rowWidth + childWidth <= maxWidth) {
                rowWidth += childWidth
                rowHeight = max(rowHeight, childHeight)
            } else {
                widthUsed = paddingLeft + paddingRight
                heightUsed += rowHeight + paddingTop + paddingBottom + 20
                rowWidth = childWidth
                rowHeight = childHeight
                totalHeight += rowHeight + paddingTop + paddingBottom + 20
            }

            if (i == childCount - 1) {
                totalHeight += rowHeight + paddingBottom
            }
        }

        setMeasuredDimension(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.EXACTLY)
        )
    }

    /**
     * Coloca las vistas de las etiquetas dentro de la vista principal (`ViewGroup`), organizándolas en filas.
     * Si no cabe una etiqueta en la fila actual, se mueve a la siguiente fila.
     *
     * @param changed Indica si las dimensiones de la vista han cambiado.
     * @param l Coordenada izquierda.
     * @param t Coordenada superior.
     * @param r Coordenada derecha.
     * @param b Coordenada inferior.
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val horizontalPadding = 10
        var left = paddingLeft
        var top = paddingTop
        var maxHeightInRow = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            if (left + childWidth + paddingRight > r - l) {
                left = paddingLeft
                top += maxHeightInRow + paddingTop + paddingBottom + 20
                maxHeightInRow = 0
            }

            child.layout(left, top, left + childWidth, top + childHeight)
            left += childWidth + horizontalPadding
            maxHeightInRow = max(maxHeightInRow, childHeight)
        }

        top += maxHeightInRow + paddingBottom
    }

    /**
     * Obtiene las etiquetas que están seleccionadas.
     *
     * @return Una lista de objetos `TagBean` que están seleccionados.
     */
    fun getSelectedTags(): List<TagBean> {
        return tagsList.filter { tagBean -> tagBean.isSelected }
    }
}
