package com.ehome.enpartesapp.ui.Presupuesto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ehome.enpartesapp.R

class PhotoTypeAdapter(private val photoTypes: List<Pair<String, String>>, private val savePhotoWithSelectedType: (String) -> Unit) :
    RecyclerView.Adapter<PhotoTypeAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.photo_type_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.photo_type_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (key, description) = photoTypes[position]
        holder.textView.text = description
        holder.itemView.setOnClickListener {
            savePhotoWithSelectedType(key) // Llama a la función que se pasó como parámetro
        }
    }

    override fun getItemCount(): Int = photoTypes.size
}