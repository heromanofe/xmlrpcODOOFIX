package com.zero.xlmrpctest

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Base64
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream




/**
 * TODO: document your custom view class.
 */
class photoTake(
    context: Context,
    attrs: AttributeSet?,
    image: Bitmap,
    name: String,
    encode:Boolean = true
): LinearLayout(context, attrs){
    var newEncode = encode
    var imageCamera:ImageView
    val imageBP = image
    var encoded = ""
    val nameDef = name
    var deleteView: View? = null
    init {
        inflate(context, R.layout.sample_photo_take, this)
        imageCamera = findViewById(R.id.ImageCamera)
        imageCamera.setImageBitmap(imageBP)
        findViewById<ImageButton>(R.id.DelButton).setOnClickListener {
            (context as Activity).runOnUiThread {
                val parent = this.parent as LinearLayout
                if (!newEncode) (deleteView?.parent as LinearLayout).removeView(deleteView)
                parent.removeView(this)
            }
        }
        if (encode) GlobalScope.launch {
            val byteArrayOutputStream = ByteArrayOutputStream()
            //imageBP.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream) //CHANGE QUALITY HERE
            val byteArray = byteArrayOutputStream.toByteArray()
            val en = Base64.encodeToString(byteArray, Base64.DEFAULT)
            println("ENCODED")
            encoded = en
        }

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.photoTake)
        attributes.recycle()
    }


}