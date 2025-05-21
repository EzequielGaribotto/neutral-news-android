@file:Suppress("unused")

package com.example.neutralnews_android.util.span

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.neutralnews_android.R

/**
 * Interfaz para manejar los eventos de clic en spans personalizados.
 */
interface ClickAbleCustomSpanListener {
    fun onClickSpan(string: String)
}

/**
 * Clase para crear y gestionar spans personalizados en un TextView.
 */
class CustomSpanStrings {

    private var textView: TextView? = null
    private var colorList: IntArray = intArrayOf()
    private var stringsList: Array<out String> = arrayOf()
    private var spannableStringBuilder: SpannableStringBuilder? = null
    private var completeString: String = ""
    private var clickAbleCustomSpanListener: ClickAbleCustomSpanListener? = null
    private var isStrikeThrough: Boolean = false

    /**
     * Establece el texto completo que se va a aplicar el span.
     *
     * @param completeString El texto completo.
     * @return La instancia actual de CustomSpanStrings.
     */
    fun setCompleteString(completeString: String): CustomSpanStrings {
        this.completeString = completeString
        spannableStringBuilder = SpannableStringBuilder(completeString)
        return this
    }

    /**
     * Establece las cadenas que se van a aplicar el span.
     *
     * @param strings Las cadenas que se van a aplicar el span.
     * @return La instancia actual de CustomSpanStrings.
     */
    fun setStrings(vararg strings: String): CustomSpanStrings {
        this.stringsList = strings
        return this
    }

    /**
     * Establece spans clicables para las cadenas especificadas.
     *
     * @param context El contexto.
     * @param clickAbleCustomSpanListener El listener para los eventos de clic.
     * @return La instancia actual de CustomSpanStrings.
     */
    fun setClickableSpan(context: Context, clickAbleCustomSpanListener: ClickAbleCustomSpanListener): CustomSpanStrings {
        this.clickAbleCustomSpanListener = clickAbleCustomSpanListener
        stringsList.forEach {
            spannableStringBuilder?.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        clickAbleCustomSpanListener.onClickSpan(it)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        val font = ResourcesCompat.getFont(context, R.font.montserrat_semi_bold)
                        font?.let { it1 -> applyCustomTypeFace(ds, it1) }
                        ds.isUnderlineText = false
                    }
                },
                completeString.indexOf(it), completeString.indexOf(it) + it.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return this
    }

    /**
     * Establece spans de color para las cadenas especificadas.
     *
     * @param colorList La lista de colores.
     * @return La instancia actual de CustomSpanStrings.
     */
    fun setColorSpan(vararg colorList: Int): CustomSpanStrings {
        this.colorList = colorList
        stringsList.forEachIndexed { index, it ->
            spannableStringBuilder?.setSpan(
                ForegroundColorSpan(colorList[index]),
                completeString.indexOf(it),
                completeString.indexOf(it) + it.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return this
    }

    /**
     * Establece spans de subrayado para las cadenas especificadas.
     *
     * @return La instancia actual de CustomSpanStrings.
     */
    fun setUnderlineSpan(): CustomSpanStrings {
        stringsList.forEach {
            spannableStringBuilder?.setSpan(
                UnderlineSpan(),
                completeString.indexOf(it),
                completeString.indexOf(it) + it.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return this
    }

    /**
     * Establece spans de negrita para las cadenas especificadas.
     *
     * @return La instancia actual de CustomSpanStrings.
     */
    fun setBoldSpan(): CustomSpanStrings {
        stringsList.forEach {
            spannableStringBuilder?.setSpan(
                StyleSpan(Typeface.BOLD),
                completeString.indexOf(it),
                completeString.indexOf(it) + it.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return this
    }

    /**
     * Establece spans de tamaño de texto para las cadenas especificadas.
     *
     * @param size La lista de tamaños de texto.
     * @return La instancia actual de CustomSpanStrings.
     */
    fun setTextSize(vararg size: Int): CustomSpanStrings {
        stringsList.forEach {
            spannableStringBuilder?.setSpan(
                AbsoluteSizeSpan(size[stringsList.indexOf(it)]),
                completeString.indexOf(it),
                completeString.indexOf(it) + it.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return this
    }

    /**
     * Establece spans de familia de fuente para las cadenas especificadas.
     *
     * @param typeface La lista de tipos de fuente.
     * @return La instancia actual de CustomSpanStrings.
     */
    fun setFontFamily(vararg typeface: Typeface): CustomSpanStrings {
        stringsList.forEachIndexed { index, string ->
            if (index < typeface.size) {
                spannableStringBuilder?.setSpan(
                    CustomTypefaceSpan("", typeface[index]),
                    completeString.indexOf(string),
                    completeString.indexOf(string) + string.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return this
    }

    /**
     * Establece spans de tachado para las cadenas especificadas.
     *
     * @return La instancia actual de CustomSpanStrings.
     */
    fun setStrikeThrough(): CustomSpanStrings {
        stringsList.forEach {
            spannableStringBuilder?.setSpan(
                StrikethroughSpan(),
                completeString.indexOf(it),
                completeString.indexOf(it) + it.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return this
    }

    /**
     * Establece el TextView al que se le aplicarán los spans.
     *
     * @param text El TextView.
     * @return La instancia actual de CustomSpanStrings.
     */
    fun setTextView(text: TextView): CustomSpanStrings {
        this.textView = text
        return this
    }

    /**
     * Construye la cadena spannable y la aplica al TextView.
     *
     * @return El SpannableStringBuilder.
     */
    fun build(): SpannableStringBuilder? {
        this.textView?.setText(spannableStringBuilder, TextView.BufferType.SPANNABLE)
        this.textView?.movementMethod = LinkMovementMethod.getInstance()
        return spannableStringBuilder
    }

    /**
     * Aplica una fuente personalizada al paint.
     *
     * @param paint El objeto Paint.
     * @param tf El tipo de fuente (Typeface).
     */
    private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
        val oldStyle: Int
        val old: Typeface = paint.typeface
        oldStyle = old.style
        val fake = oldStyle and tf.style.inv()
        if (fake and Typeface.BOLD != 0) {
            paint.isFakeBoldText = true
        }
        if (fake and Typeface.ITALIC != 0) {
            paint.textSkewX = -0.25f
        }
        paint.typeface = tf
    }
}
