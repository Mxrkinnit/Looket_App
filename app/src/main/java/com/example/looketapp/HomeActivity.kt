package com.example.looketapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.looketapp.Adapter.PostAdapter
import com.example.looketapp.Model.PostModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var postModelList: MutableList<PostModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        recyclerView = findViewById(R.id.recyclerView)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true // this will load from end and will show the latest post first

        recyclerView.layoutManager = layoutManager

        postModelList = ArrayList()

        // now we will retrieve the data from Firebase
        loadPosts()
    }

    private fun loadPosts() {
        val ref: DatabaseReference = FirebaseDatabase.getInstance().getReference("Posts")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                postModelList.clear()
                for (ds in dataSnapshot.children) {
                    val postModel: PostModel? = ds.getValue(PostModel::class.java)
                    postModel?.let { postModelList.add(it) }
                    postAdapter = PostAdapter(this@HomeActivity, postModelList)
                    recyclerView.adapter = postAdapter
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@HomeActivity, "" + databaseError, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                startActivity(Intent(this@HomeActivity, MainActivity::class.java))
            }
            R.id.action_add_post -> {
                startActivity(Intent(this@HomeActivity, AddPostActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity() // means when the user is on the main screen and presses the back button, then shut down the app
    }
}
