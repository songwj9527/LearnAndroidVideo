package com.songwj.openvideo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_fbo_select.*

class FBOSelectActivity : AppCompatActivity() , View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fbo_select)

        button3.setOnClickListener(this)
        button4.setOnClickListener(this)
        button5.setOnClickListener(this)
        button6.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        v?.let{
            val id = v.id
            var newIntent = Intent(this, FBOPlayerActivity::class.java)
            if (id == R.id.button3) {
                newIntent.putExtra("Type", 1)
            }
            else if (id == R.id.button4) {
                newIntent.putExtra("Type", 2)
            }
            else if (id == R.id.button5) {
                newIntent.putExtra("Type", 3)
            }
            else if (id == R.id.button6) {
                newIntent.putExtra("Type", 4)
            }
            startActivity(newIntent)
        }

    }
}