package com.ehome.enpartesapp.ui.Presupuesto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ehome.enpartesapp.R

class PhotoItemAdapter(private val photoItems: MutableList<PhotoItem>) : RecyclerView.Adapter<PhotoItemAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Obtén las referencias a los elementos del layout
        // ...
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.photo_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photoItem = photoItems[position]
        // Configura los elementos del layout con la información de photoItem
        // ...
    }

    override fun getItemCount(): Int = photoItems.size
}