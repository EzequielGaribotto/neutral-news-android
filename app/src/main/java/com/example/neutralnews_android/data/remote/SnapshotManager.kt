package com.example.neutralnews_android.data.remote

import android.util.Log
import com.example.neutralnews_android.data.Constants.News.CREATED_AT
import com.example.neutralnews_android.data.Constants.News.DESCRIPTION
import com.example.neutralnews_android.data.Constants.News.GROUP
import com.example.neutralnews_android.data.Constants.News.NEWS
import com.example.neutralnews_android.data.Constants.News.UPDATED_AT
import com.example.neutralnews_android.data.Constants.NeutralNew.NEUTRAL_DESCRIPTION
import com.example.neutralnews_android.data.Constants.NeutralNew.NEUTRAL_NEWS
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Manager ligero para encapsular la creación y eliminación de snapshot listeners.
 * Este manager construye las consultas y entrega solo los DocumentChange relevantes
 * al callback proporcionado. Los errores se propagan vía onError.
 */
class SnapshotManager {
    private var neutralListener: ListenerRegistration? = null

    fun startNeutralListener(onChanges: (List<DocumentChange>) -> Unit, onError: (Exception) -> Unit) {
        stopNeutralListener()
        try {
            val db = FirebaseFirestore.getInstance()
            val startTime = Timestamp(System.currentTimeMillis() / 1000 - 86400 * 3, 0)

            val query = db.collection(NEUTRAL_NEWS)
                .whereGreaterThan(UPDATED_AT, startTime)
                .whereNotEqualTo(NEUTRAL_DESCRIPTION, "")

            neutralListener = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val changes = snapshot.documentChanges
                    val filteredChanges = changes.filter { it.type != DocumentChange.Type.REMOVED }
                    if (filteredChanges.isNotEmpty()) {
                        onChanges(filteredChanges)
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("SnapshotManager", "startNeutralListener error: ${ex.localizedMessage}")
            onError(ex)
        }
    }


    fun stopNeutralListener() {
        try {
            neutralListener?.remove()
        } catch (_: Exception) {
        }
        neutralListener = null
    }

    fun stopAll() {
        stopNeutralListener()
    }
}
