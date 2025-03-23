package com.z0diac.tesapi.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.model.Cast

class CastAdapter(private val castList: List<Cast>) : RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

    class CastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivActorImage: ImageView = itemView.findViewById(R.id.ivCastPhoto)
        val tvActorName: TextView = itemView.findViewById(R.id.tvActorName)
        val tvCharacterName: TextView = itemView.findViewById(R.id.tvCharacterName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cast, parent, false)
        return CastViewHolder(view)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        val cast = castList[position]

        // Load image
        Glide.with(holder.itemView.context)
            .load("https://image.tmdb.org/t/p/w185" + cast.profilePath)
            .placeholder(R.drawable.placeholder_image)
            .into(holder.ivActorImage)

        holder.tvActorName.text = cast.name
        holder.tvCharacterName.text = "as ${cast.character}"
    }

    override fun getItemCount(): Int = castList.size
}
