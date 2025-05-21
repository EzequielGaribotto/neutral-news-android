package com.example.neutralnews_android.di.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.example.neutralnews_android.BR
import com.example.neutralnews_android.R

/**
 * A custom dialog class that provides data binding support for custom layouts.
 * This dialog is initialized with a layout resource ID and a listener for handling view clicks.
 *
 * @param V The type of [ViewDataBinding] associated with the layout.
 * @property context The context in which the dialog is created.
 * @property layoutId The layout resource ID to be used for the dialog's view.
 * @property listener A listener that handles view clicks within the dialog.
 */
class BaseCustomDialog<V : ViewDataBinding>(
    context: Context,
    private val layoutId: Int,
    private val listener: Listener
) : Dialog(
    context, R.style.Dialog
) {
    private var binding: V? = null

    /**
     * Retrieves the data binding instance for the dialog.
     *
     * @return The [ViewDataBinding] instance, or null if the dialog has not been initialized.
     */
    fun getBinding(): V? {
        init()
        return binding
    }

    /**
     * Initializes the dialog by inflating the layout and setting the listener.
     */
    private fun init() {
        if (binding == null) binding =
            DataBindingUtil.inflate(LayoutInflater.from(context), layoutId, null, false)
        binding?.setVariable(BR.callback, listener)
    }

    /**
     * Called when the dialog is created. Sets the content view of the dialog to the
     * root of the data binding layout.
     *
     * @param savedInstanceState The previous state of the dialog, if any.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        binding?.root?.let { setContentView(it) }
    }

    /**
     * Listener interface for handling view clicks within the dialog.
     */
    interface Listener {
        /**
         * Called when a view inside the dialog is clicked.
         *
         * @param view The view that was clicked.
         */
        fun onViewClick(view: View)
    }
}
