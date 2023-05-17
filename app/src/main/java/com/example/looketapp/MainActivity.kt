package com.example.looketapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    lateinit var registerbtn:Button
    private lateinit var auth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        auth= FirebaseAuth.getInstance()

        registerbtn=findViewById(R.id.Btn_Register)
        registerbtn.setOnClickListener {
            val alex=Intent(this,PhoneActivity::class.java)
            startActivity(alex)
        }
    }
}