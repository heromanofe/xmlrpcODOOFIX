package com.zero.xlmrpctest

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.zero.xlmrpctest.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File
    private var curMK = 0
    var db = ""
    var uid = -1
    var password = ""
    var url = ""
    var model = ""
    var resID = -1
    var contextName = "NEW FORMAT"
    var photos = mutableListOf<View>()
    var maxPhotos = 5

    var imgTable = ""
    var imgModelName = ""
    var modelNew = -1
    var attach = ""
    var dataImg = ""
    var username = ""
    lateinit var models: XmlRpcClient
    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = "demo_140_1629613879"
        password = "admin"
        username = "admin"
        url = "https://demo3.odoo.com"
        model = "project.task"
        attach = "ir.attachment"
        resID = 3 //ID of resource where you want to have attachment
        maxPhotos = 5 //MAX photos allowed
        contextName = model+"_"+intent.getStringExtra("ContextFormat").toString()
        var tmpNew = ""
        contextName.forEach {
            if (it.isLetterOrDigit() || it == '-') tmpNew += it
            else tmpNew += "_"
        }
        contextName = tmpNew.replace("__","")

        cameraExecutor = Executors.newSingleThreadExecutor()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraSw(initial = true)
        outputDirectory = getOutputDirectory()
        GlobalScope.launch {
            val Common = object : XmlRpcClient() {init {
                setConfig(object : XmlRpcClientConfigImpl() {init {
                    serverURL = URL(java.lang.String.format("%s/xmlrpc/2/common", url))
                    isEnabledForExceptions = true
                    connectionTimeout = 5000
                    replyTimeout = 5000
                }
                })
            }
            }
            //-----------------------------------------------------------------------------------------------------------//
            uid = Common.execute(
                "authenticate", listOf(
                    db, username, password, emptyMap<Any, Any>()
                )
            ) as Int
        }



        models = object : XmlRpcClient() {init { setConfig(object : XmlRpcClientConfigImpl() {init {
            serverURL = URL(java.lang.String.format("%s/xmlrpc/2/object", url))
            isEnabledForExceptions = true
            connectionTimeout = 2000
        }
        })
        }
        }
        println()
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    fun cameraSw(cameraOn:Boolean = true,cameraOff:Boolean = false,initial:Boolean=false){
        stopCamera(cameraOff)
        if(!initial) {
            photos = mutableListOf()
            binding.photoPlaceLY.children.forEach { photos.add(it) }
            binding.photoPlaceLY.removeAllViews()
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        }
        binding.CapturePHButtonn.setOnClickListener {
            GlobalScope.launch {
                if(binding.photoPlaceLY.childCount + curMK >= 5) return@launch
                curMK += 1
                takePhoto()
                curMK-=1
            }
        }
        binding.savePHButton.setOnClickListener {
            if (binding.photoPlaceLY.childCount > 0) { sendPhotos() }
        }
        startCamera(cameraOn)
        initImages()

    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Checks the orientation of the screen
        cameraSw(false)
    }

    private fun initImages(){
        GlobalScope.launch {
            delay(100)
            val orientation = resources.configuration.orientation
            val width = if (orientation == Configuration.ORIENTATION_PORTRAIT){ binding.photoPlaceLY.width/5
            } else{
                binding.photoPlaceLY.height/5
            }
            runOnUiThread {
                photos?.forEach {
                    val view = it
                    view.layoutParams.width = width
                    view.layoutParams.height = width
                    binding.photoPlaceLY.addView(view)
                }
            }
        }

    }

    suspend fun takePhoto(): photoTake? {
        val imageCapture = imageCapture ?: return null
        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            "${contextName}_" + SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpeg"
        )
        var imgView: photoTake? = null
        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("Photo capture failed: ${exc.message}", exc.toString())
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    var myBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    val name = "${contextName}_" + SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpeg"
                    val viewImg = photoTake(this@MainActivity, null, myBitmap, name)
                    val orientation = resources.configuration.orientation
                    val width = if (orientation == Configuration.ORIENTATION_PORTRAIT){ binding.photoPlaceLY.width/5
                    } else{
                        binding.photoPlaceLY.height/5
                    }

                    viewImg.layoutParams = LinearLayout.LayoutParams(width, width)
                    viewImg.gravity = Gravity.START
                    runOnUiThread {
                        binding.photoPlaceLY.addView(viewImg)
                        imgView = viewImg
                    }
                }
            }
        )
        var counter = 0
        while (counter < 100) {
            if (imgView != null) return imgView
            delay(100)
            counter += 1
        }
        return imgView
    }
    private fun getOutputDirectory(otherFolder:String=""): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)+otherFolder).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    fun startCamera(cameraOn: Boolean) {
        stopCamera(false)
        val viewFinder = binding.CameraFinderL
        if(cameraOn) {
            val msg = "Camera Was Started"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val surface =
            cameraProviderFuture.addListener({
                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }
                val isLandscape = requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                imageCapture = ImageCapture.Builder()
                    .setTargetResolution(if (isLandscape) Size(1920, 1080) else Size(1080, 1920))
                    .setFlashMode(ImageCapture.FLASH_MODE_ON)
                    //.setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                    .also {

                        it.flashMode = ImageCapture.FLASH_MODE_ON
                    }
                // Select back camera as a default

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview,imageCapture)
                } catch(exc: Exception) {
                    Log.e("Use case binding failed", exc.toString())
                }

            }, ContextCompat.getMainExecutor(this))
    }
    fun stopCamera(cameraOff: Boolean) {
        cameraExecutor.shutdown()
        if (cameraOff) {
            val msg = "Camera was Stopped"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDestroy() {

        super.onDestroy()
        stopCamera(true)
    }

    fun sendPhotos() {
        val childMax = binding.photoPlaceLY.childCount
        if (childMax < 1 && childMax <= maxPhotos) {
            if (childMax < 1) {
                Toast.makeText(
                    this,
                    "No Photos were added!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            else{
                Toast.makeText(
                    this,
                    "Too many photos, MAX are: $maxPhotos",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
        Toast.makeText(
            this,
            "SENDING FILES",
            Toast.LENGTH_SHORT
        ).show()
        GlobalScope.launch {

            binding.photoPlaceLY.children.forEach { it as photoTake
                while (it.encoded == "") delay(500)
                val add = listOf(object : HashMap<Any, Any>() {init {
                    put("datas", it.encoded)
                    put("type", "binary")
                    put("name", it.nameDef)
                    put("res_model", model)
                    put("res_id", resID)
                } })
                setExeKw(
                    attach, listOf(
                        add
                    )
                )
            }
            println()
            runOnUiThread {
                binding.photoPlaceLY.removeAllViews()
                binding.photoPlaceLY.forceLayout()
                binding.photoPlaceLY.requestLayout()
            }
        }
    }
    fun setExeKw(model_name: String, fields: List<Any>): Array<Any?>? {
        // Execute_KW with model, method, fields and Attributes
            val ret = models.execute(
                "execute_kw", listOf(
                    db,                   //Odoo Database                                                                 | String
                    uid,                  //Odoo Uid                                                                      | String
                    password,             //Odoo Password                                                                 | String
                    model_name,                  //Model_name (res.partners)                                                     | String
                    "create",                    //method_name (create)                                                          | String
                    listOf(fields)               //fields to fetch [ listOf("name", "Ilja") ]                                    | List<Any> --> [ HashMap <String, String> ]
                )
            ) as Array<Any?>
            return ret
    }

}