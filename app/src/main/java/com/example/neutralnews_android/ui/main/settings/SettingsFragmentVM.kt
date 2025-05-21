package com.example.neutralnews_android.ui.main.settings

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.example.neutralnews_android.di.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsFragmentVM @Inject constructor(application: Application) : BaseViewModel(application) {
    val fieldTitleFontSize = MutableLiveData<String>()
    val fieldDescriptionFontSize = MutableLiveData<String>()
    val fieldDetailsTextFontSize = MutableLiveData<String>()
    val fieldDateFormat = MutableLiveData<String>()

    fun initializeValues(titleFontSize: String, descriptionFontSize: String, detailsTextFontSize: String, dateFormat: String) {
        fieldTitleFontSize.value = titleFontSize
        fieldDescriptionFontSize.value = descriptionFontSize
        fieldDetailsTextFontSize.value = detailsTextFontSize
        fieldDateFormat.value = dateFormat
    }
}