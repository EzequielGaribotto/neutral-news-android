package com.example.neutralnews_android.ui.main.filter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.example.neutralnews_android.R
import com.example.neutralnews_android.data.Constants.ApiObject.FILTER_COUNT
import com.example.neutralnews_android.data.Constants.ApiObject.FILTER_DATA
import com.example.neutralnews_android.data.bean.filter.FilterBean
import com.example.neutralnews_android.data.bean.filter.LocalFilterBean
import com.example.neutralnews_android.data.bean.tag.TagBean
import com.example.neutralnews_android.databinding.ActivityFilterBinding
import com.example.neutralnews_android.di.view.AppActivity
import com.example.neutralnews_android.util.fullScreen
import com.example.neutralnews_android.util.loggerE
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint

/**
 * FilterActivity es una actividad que permite a los usuarios aplicar filtros a las noticias.
 */
@AndroidEntryPoint
class FilterActivity : AppActivity() {

    private var filterData: LocalFilterBean = LocalFilterBean()

    companion object {
        /**
         * Crea un nuevo Intent para iniciar FilterActivity.
         *
         * @param c El contexto.
         * @param filterData Los datos del filtro.
         * @return El Intent para iniciar FilterActivity.
         */
        fun newIntent(c: Context, filterData: LocalFilterBean = LocalFilterBean()): Intent {
            val intent = Intent(c, FilterActivity::class.java)
            intent.putExtra(FILTER_DATA, Gson().toJson(filterData))
            return intent
        }
    }

    // Binding
    private lateinit var binding: ActivityFilterBinding
    private val vm: FilterActivityVM by viewModels()

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Binding
        if (!::binding.isInitialized) {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_filter)
            binding.vm = vm
        }
        fullScreen()

        // Inicialización
        filterData = Gson().fromJson(
            intent.getStringExtra(FILTER_DATA), object : TypeToken<LocalFilterBean>() {}.type
        )

        loggerE(filterData.toString())

        // On click
        vm.obrClick.observe(this) { view ->
            when (view.id) {
                R.id.txtDone -> {
                    if (binding.tagCategories.getSelectedTags().isNotEmpty()) {
                        filterData.categories?.clear()
                        for (tag in binding.tagCategories.getSelectedTags()) {
                            tag.names?.forEach {
                                filterData.categories?.add(FilterBean(tag.id, it, "category"))
                            }
                        }
                        filterData.categoryTagBean = binding.tagCategories.getSelectedTags()
                    } else {
                        filterData.categoryTagBean = ArrayList()
                    }

                    if (binding.tagMedia.getSelectedTags().isNotEmpty()) {
                        filterData.media?.clear()
                        for (tag in binding.tagMedia.getSelectedTags()) {
                            filterData.media?.add(FilterBean(tag.id, tag.name, "media"))
                        }
                        filterData.mediaTagBean = binding.tagMedia.getSelectedTags()
                    } else {
                        filterData.mediaTagBean = ArrayList()
                    }

                    loggerE(filterData.toString())

                    val intent = Intent()
                    intent.putExtra(FILTER_DATA, Gson().toJson(filterData))
                    intent.putExtra(FILTER_COUNT, getFilterCount())
                    setResult(RESULT_OK, intent)
                    loggerE("Filter data: ${filterData.startDate} - ${filterData.endDate}")
                    loggerE("Filter data: ${filterData.media}")
                    loggerE("Filter data: ${filterData.categories}")
                    finish()
                }

                R.id.txtClearAll -> {
                    clearAllFilter()
                    val intent = Intent()
                    intent.putExtra(FILTER_DATA, Gson().toJson(filterData))
                    intent.putExtra(FILTER_COUNT, getFilterCount())
                    setResult(RESULT_OK, intent)
                    finish()
                }

                R.id.imgBack -> {
                    finish()
                }
            }
        }

        // Establecer datos
        setData()

        // Pantalla completa
        fullScreen()
        setCategoriesTagView()
        setMediaTagView()
    }

    private fun setMediaTagView() {
        loggerE("::::::${filterData.mediaTagBean}")
        val arl: ArrayList<TagBean> = ArrayList()
        arl.add(TagBean(1,
            getString(R.string.media_one),
            filterData.mediaTagBean?.find { it.id.toInt() == 1 } != null))
        arl.add(TagBean(2,
            getString(R.string.media_two),
            filterData.mediaTagBean?.find { it.id.toInt() == 2 } != null))
        arl.add(TagBean(3,
            getString(R.string.media_three),
            filterData.mediaTagBean?.find { it.id.toInt() == 3 } != null))
        arl.add(TagBean(4,
            getString(R.string.media_four),
            filterData.mediaTagBean?.find { it.id.toInt() == 4 } != null))
        arl.add(TagBean(5,
            getString(R.string.media_five),
            filterData.mediaTagBean?.find { it.id.toInt() == 5 } != null))
        arl.add(TagBean(6,
            getString(R.string.media_six),
            filterData.mediaTagBean?.find { it.id.toInt() == 6 } != null))
        arl.add(TagBean(7,
            getString(R.string.media_seven),
            filterData.mediaTagBean?.find { it.id.toInt() == 7 } != null))
        arl.add(TagBean(8,
            getString(R.string.media_eight),
            filterData.mediaTagBean?.find { it.id.toInt() == 8 } != null))
        arl.add(TagBean(9,
            getString(R.string.media_nine),
            filterData.mediaTagBean?.find { it.id.toInt() == 9 } != null))
        arl.add(TagBean(10,
            getString(R.string.media_ten),
            filterData.mediaTagBean?.find { it.id.toInt() == 10 } != null))
        arl.add(TagBean(11,
            getString(R.string.media_eleven),
            filterData.mediaTagBean?.find { it.id.toInt() == 11 } != null))
        arl.add(TagBean(12,
            getString(R.string.media_twelve),
            filterData.mediaTagBean?.find { it.id.toInt() == 12 } != null))
        arl.add(TagBean(13,
            getString(R.string.media_thirteen),
            filterData.mediaTagBean?.find { it.id.toInt() == 13 } != null))
        arl.add(TagBean(14,
            getString(R.string.media_fourteen),
            filterData.mediaTagBean?.find { it.id.toInt() == 14 } != null))
        arl.add(TagBean(15,
            getString(R.string.media_fifteen),
            filterData.mediaTagBean?.find { it.id.toInt() == 15 } != null))
        arl.add(TagBean(16,
            getString(R.string.media_sixteen),
            filterData.mediaTagBean?.find { it.id.toInt() == 16 } != null))

        binding.tagMedia.setTags(arl)
        binding.tagMedia.isSingleSelection = false
    }
    private fun setCategoriesTagView() {
        val arl: ArrayList<TagBean> = ArrayList()
        arl.add(TagBean(1,
            name = getString(R.string.category_economy),
            names = arrayOf(
                getString(R.string.category_economy),
            ),
            filterData.categoryTagBean?.find { it.id.toInt() == 1 } != null))
        arl.add(TagBean(2,
            name = getString(R.string.category_politics),
            names = arrayOf(
                getString(R.string.category_politics),
            ),
            filterData.categoryTagBean?.find { it.id.toInt() == 2 } != null))
        arl.add(TagBean(3,
            name = getString(R.string.category_science),
            names = arrayOf(
                getString(R.string.category_science),
            ),
            filterData.categoryTagBean?.find { it.id.toInt() == 3 } != null))
        arl.add(TagBean(4,
            name = getString(R.string.category_technology),
            names = arrayOf(
                getString(R.string.category_technology),
            ),
            filterData.categoryTagBean?.find { it.id.toInt() == 4 } != null))
        arl.add(TagBean(5,
            name = getString(R.string.category_culture),
            names = arrayOf(
                getString(R.string.category_culture),
            ),
            filterData.categoryTagBean?.find { it.id.toInt() == 5 } != null))
        arl.add(TagBean(6,
            name = getString(R.string.category_society),
            names = arrayOf(
                getString(R.string.category_society),
            ),
            filterData.categoryTagBean?.find { it.id.toInt() == 6 } != null))
        arl.add(TagBean(7,
            name = getString(R.string.category_sports),
            names = arrayOf(
                getString(R.string.category_sports),
            ),
            filterData.categoryTagBean?.find { it.id.toInt() == 7 } != null))
        arl.add(TagBean(8,
            name = getString(R.string.category_international),
            names = arrayOf(
                getString(R.string.category_international),
            ),
            filterData.categoryTagBean?.find { it.id.toInt() == 8 } != null))
        arl.add(TagBean(9,
            name = getString(R.string.category_entertainment),
            names = arrayOf(
                getString(R.string.category_entertainment),
            ),
            filterData.categoryTagBean?.find { it.id.toInt() == 9 } != null))
        arl.add(TagBean(10,
            name = getString(R.string.category_others),
            names = arrayOf(
                getString(R.string.category_others),
                getString(R.string.category_uncategorized),
                getString(R.string.category_various)
            ),
            filterData.categoryTagBean?.find { it.id.toInt() == 10 } != null))
        binding.tagCategories.setTags(arl)
        binding.tagCategories.isSingleSelection = false
    }

    // Limpiar filtros
    @SuppressLint("SetTextI18n")
    private fun clearAllFilter() {
        filterData = LocalFilterBean()
        setMediaTagView()
        setCategoriesTagView()
        filterData.media?.clear()
    }

    // Obtener el conteo de filtros
    private fun getFilterCount(): Int {
        var filterCount = 0
        if (filterData.media?.isNotEmpty() == true) {
            filterCount++
        }

        if (filterData.categories?.isNotEmpty() == true) {
            filterCount++
        }

        if (filterData.startDate != "" || filterData.endDate != "") {
            filterCount++
        }

        return filterCount
    }

    // Establecer datos
    @SuppressLint("SetTextI18n", "StringFormatMatches")
    private fun setData() {
//        filterData.startDate = ""
//        filterData.endDate = ""
//        filterData.media = ArrayList()
//        filterData.categories = ArrayList()

    }
}