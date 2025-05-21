package com.example.neutralnews_android.di.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.example.neutralnews_android.BR
import com.example.neutralnews_android.R

/**
 * Generic RecyclerView adapter that uses DataBinding.
 *
 * @param M Type of the data model.
 * @param B Type of the ViewDataBinding.
 * @property layoutResId Layout resource ID for the item view.
 * @property modelVariableId BR variable ID for binding data.
 * @property callback Callback for handling item interactions.
 */
class SimpleRecyclerViewAdapter<M, B : ViewDataBinding>(
    @LayoutRes private val layoutResId: Int,
    private val modelVariableId: Int,
    private var callback: SimpleCallback<M, B>? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val viewTypeLoader = 1
    private val viewTypeItem = 0
    private var dataList: MutableList<M> = ArrayList()
    private var isLoading = false

    /**
     * Removes an item from the list at the specified index.
     * @param i Index of the item to remove.
     */
    fun removeItem(i: Int) {
        try {
            if (i != -1) {
                dataList.removeAt(i)
                notifyItemRemoved(i)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Removes a range of items from the list.
     * @param start Start index of the range.
     * @param end End index of the range.
     * @param expandItem Index up to which items should be kept.
     */
    fun removeItemRange(start: Int, end: Int, expandItem: Int) {
        try {
            if (start != -1) {
                dataList = dataList.subList(0, expandItem)
                notifyItemRangeRemoved(start, end)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Shows only two items in the list.
     * @param end Index up to which items should be removed.
     */
    fun showOnlyTwoItems(end: Int) {
        try {
            dataList = dataList.subList(0, 2)
            notifyItemRangeRemoved(1, end)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Adds an item at a specific index.
     * @param i Index where the item should be inserted.
     * @param scanResult The item to insert.
     */
    operator fun set(i: Int, scanResult: M) {
        dataList.add(i, scanResult)
        notifyItemChanged(i)
    }

    /**
     * Interface for handling item interactions.
     */
    interface SimpleCallback<M, B : ViewDataBinding> {
        fun onItemClick(v: View, m: M) {}
        fun onItemClick(v: View, m: M, pos: Int) {}
        fun onPositionClick(v: View, pos: Int) {}
        fun onViewBinding(holder: SimpleViewHolder<B>, m: M, pos: Int) {}
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == viewTypeItem) {
            val binding: B = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                layoutResId,
                parent,
                false
            )
            callback?.let { binding.setVariable(BR.callback, it) }
            SimpleViewHolder(binding)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row_loader, parent, false)
            LoadingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SimpleViewHolder<*>) {
            holder.binding.setVariable(modelVariableId, dataList[position])
            callback?.onViewBinding(holder as SimpleViewHolder<B>, dataList[position], position)
        }
    }

    override fun getItemCount(): Int = if (isLoading) dataList.size + 1 else dataList.size

    override fun getItemViewType(position: Int): Int = if (position == dataList.size && isLoading) viewTypeLoader else viewTypeItem

    /**
     * Sets the loading state of the adapter.
     * @param isLoading Whether the loader should be shown.
     */
    fun setLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        if (isLoading) {
            notifyItemInserted(dataList.size)
        } else {
            notifyItemRemoved(dataList.size)
        }
    }

    /**
     * Populates the list with dummy data.
     * @param size Number of dummy items.
     * @param seed Sample data item.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setDummyList(size: Int, seed: M) {
        dataList.clear()
        repeat(size) { dataList.add(seed) }
        notifyDataSetChanged()
    }

    /**
     * Gets or sets the list data.
     */
    var list: List<M>
        get() = dataList
        @SuppressLint("NotifyDataSetChanged")
        set(newDataList) {
            dataList.clear()
            dataList.addAll(newDataList)
            notifyDataSetChanged()
        }

    /**
     * Adds a list of items to the existing dataset.
     * @param newDataList List of new items.
     */
    fun addToList(newDataList: List<M>) {
        val positionStart = dataList.size
        dataList.addAll(newDataList)
        notifyItemRangeInserted(positionStart, newDataList.size)
    }

    /**
     * Clears the list and refreshes the adapter.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun clearList() {
        dataList.clear()
        notifyDataSetChanged()
    }

    /**
     * Adds a single item to the dataset.
     * @param data The item to add.
     */
    fun addData(data: M) {
        dataList.add(data)
        notifyItemInserted(dataList.size - 1)
    }

    /**
     * Sets an item at a specific position.
     * @param position Index of the item.
     * @param data New item data.
     */
    fun setData(position: Int, data: M) {
        dataList[position] = data
        notifyItemChanged(position, data)
    }

    /**
     * ViewHolder for binding views.
     */
    class SimpleViewHolder<B : ViewDataBinding>(val binding: B) : RecyclerView.ViewHolder(binding.root)

    /**
     * ViewHolder for loading indicator.
     */
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
