package com.example.neutralnews_android.ui.main.news

import android.app.Application
import com.example.neutralnews_android.di.event.SingleLiveEvent
import com.example.neutralnews_android.di.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NewDetailActivityVM @Inject constructor(application: Application) : BaseViewModel(application) {

    val openLinkEvent = SingleLiveEvent<String>()

    fun openLink(url: String) {
        openLinkEvent.postValue(url)
    }
}