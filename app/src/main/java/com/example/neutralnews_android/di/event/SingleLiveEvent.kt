package com.example.neutralnews_android.di.event

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Clase `SingleLiveEvent` que extiende `MutableLiveData` y permite que solo un observador sea notificado de los cambios.
 *
 * @param <T> El tipo de datos que se mantendrán en esta instancia de `SingleLiveEvent`.
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val pending = AtomicBoolean(false)

    /**
     * Observa los cambios en los datos mantenidos por esta instancia de `SingleLiveEvent`.
     *
     * @param owner El `LifecycleOwner` que controla el ciclo de vida del observador.
     * @param observer El observador que recibirá las notificaciones de cambios.
     */
    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Se han registrado múltiples observadores, pero solo uno será notificado de los cambios.")
        }
        // Observa el `MutableLiveData` interno
        super.observe(owner) { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        }
    }

    /**
     * Establece un nuevo valor y notifica a los observadores registrados.
     *
     * @param t El nuevo valor a establecer.
     */
    @MainThread
    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }

    /**
     * Llama a esta función para notificar a los observadores registrados sin establecer un nuevo valor.
     */
    @MainThread
    fun call() {
        value = null
    }

    companion object {
        private const val TAG = "SingleLiveEvent"
    }
}