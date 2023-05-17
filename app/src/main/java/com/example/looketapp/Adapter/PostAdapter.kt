package com.example.looketapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.looketapp.Model.PostModel
import com.example.looketapp.R

class PostAdapter(private val context: Context, private val postModelList: List<PostModel>) :
    RecyclerView.Adapter<PostAdapter.MyHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.home_post, parent, false)
        return MyHolder(view)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val postModel = postModelList[position]
        val title = postModel.pTitle
        val description = postModel.pDescription
        val image = postModel.pImage

        holder.postTitle.text = title
        holder.postDescription.text = description

        Glide.with(context).load(image).into(holder.postImage)
        // Now we will add a library to load the image
    }

    override fun getItemCount(): Int {
        return postModelList.size
    }

    inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var postImage: ImageView = itemView.findViewById(R.id.postImage)
        var postTitle: TextView = itemView.findViewById(R.id.postTitle)
        var postDescription: TextView = itemView.findViewById(R.id.postDescription)
    }
}
