package com.example.neutralnews_android.util.span

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.TypefaceSpan

/**
 * CustomTypefaceSpan es una clase personalizada que permite aplicar un tipo de fuente (Typeface)
 * específico a una porción de texto dentro de un TextView. Hereda de TypefaceSpan y sobrescribe
 * los métodos necesarios para personalizar el tipo de letra en el texto.
 *
 * @param family El nombre de la familia de fuentes. Si se desea aplicar un tipo de letra
 *               específico, se puede proporcionar un valor null.
 * @param newType El nuevo tipo de letra (Typeface) que se aplicará al texto.
 */
class CustomTypefaceSpan(family: String?, private val newType: Typeface) : TypefaceSpan(family) {

    /**
     * Actualiza el estado de dibujo del texto para aplicar el tipo de letra personalizado.
     *
     * @param ds El objeto TextPaint utilizado para dibujar el texto en la pantalla.
     *           Este objeto contiene la configuración para el estilo del texto.
     */
    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, newType)
    }

    /**
     * Actualiza el estado de medición del texto para aplicar el tipo de letra personalizado.
     *
     * @param paint El objeto TextPaint utilizado para medir el texto antes de ser dibujado.
     */
    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, newType)
    }

    companion object {
        /**
         * Aplica el tipo de letra personalizado al objeto Paint proporcionado.
         *
         * Este método también asegura que se mantengan los estilos de texto, como negrita o
         * cursiva, si son necesarios.
         *
         * @param paint El objeto Paint al que se le aplicará el tipo de letra.
         * @param tf El tipo de letra (Typeface) que se aplicará al texto.
         */
        private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
            val oldStyle: Int
            val old = paint.typeface
            oldStyle = old?.style ?: 0
            val fake = oldStyle and tf.style.inv()

            // Si el tipo de letra original no es negrita, pero el nuevo tipo lo es, se establece la negrita.
            if (fake and Typeface.BOLD != 0) {
                paint.isFakeBoldText = true
            }

            // Si el tipo de letra original no es cursiva, pero el nuevo tipo lo es, se establece cursiva.
            if (fake and Typeface.ITALIC != 0) {
                paint.textSkewX = -0.25f
            }

            // Aplica el nuevo tipo de letra al objeto Paint.
            paint.typeface = tf
        }
    }
}
