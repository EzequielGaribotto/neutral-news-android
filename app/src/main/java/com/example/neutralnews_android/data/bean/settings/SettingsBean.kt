package com.example.neutralnews_android.data.bean.settings

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class SettingsBean(

    @SerializedName("titleFontSize") val titleFontSize: Int = 14,

    @SerializedName("descriptionFontSize") val descriptionFontSize: Int = 12,

    @SerializedName("detailsTextFontSize") val detailsTextFontSize: Int = 10,

    @SerializedName("dateFormat") val dateFormat: String = "DD-MM-YYYY",

    ) : Serializable