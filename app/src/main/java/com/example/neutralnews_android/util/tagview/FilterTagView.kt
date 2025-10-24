package com.example.neutralnews_android.util.tagview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.neutralnews_android.R
import com.example.neutralnews_android.data.bean.tag.TagBean
import com.example.neutralnews_android.databinding.RowCustomTagBinding
import com.google.android.material.chip.ChipGroup

/**
 * A compact TagView replacement using ChipGroup for better layout and visibility.
 * Keeps the same external API used in FilterActivity: setTags(List<TagBean>), getSelectedTags(), isSingleSelection.
 */
class FilterTagView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var chipGroup: ChipGroup
    private val arrTagViewBeanMain: ArrayList<TagBean> = ArrayList()
    var isSingleSelection: Boolean = false

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_filter_tag_view, this, true)
        chipGroup = view.findViewById(R.id.chipGroup)
    }

    private fun createTagView(tagViewBean: TagBean): View {
        val layoutInflater = LayoutInflater.from(context)
        val tagView = RowCustomTagBinding.inflate(layoutInflater, chipGroup, false)
        tagView.bean = tagViewBean
        // Allow clicks to toggle selection
        tagView.root.setOnClickListener {
            if (isSingleSelection) {
                // Deselect previous
                val prev = arrTagViewBeanMain.find { it.isSelected }
                if (prev != null && prev.id != tagViewBean.id) {
                    prev.isSelected = false
                }
                tagViewBean.isSelected = !tagViewBean.isSelected
            } else {
                tagViewBean.isSelected = !tagViewBean.isSelected
            }
            refreshAllViews()
        }
        return tagView.root
    }

    private fun refreshAllViews() {
        chipGroup.removeAllViews()
        for (i in 0 until arrTagViewBeanMain.size) {
            val bean = arrTagViewBeanMain[i]
            val v = createTagView(bean)
            // set text
            val txt = v.findViewById<TextView>(R.id.txtTitle)
            txt.text = bean.name ?: ""
            // background set via binding in row_custom_tag.xml
            chipGroup.addView(v)
        }
    }

    fun setTags(arrTagViewBean: List<TagBean>) {
        chipGroup.removeAllViews()
        arrTagViewBeanMain.clear()
        arrTagViewBeanMain.addAll(arrTagViewBean.map { it.copy() })
        refreshAllViews()
    }

    fun getSelectedTags(): List<TagBean> {
        return arrTagViewBeanMain.filter { it.isSelected }
    }
}

