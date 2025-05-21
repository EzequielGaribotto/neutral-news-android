package com.example.neutralnews_android.ui.main.filter

import android.app.Application
import com.example.neutralnews_android.di.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FilterActivityVM @Inject constructor(application: Application) : BaseViewModel(application) {

}