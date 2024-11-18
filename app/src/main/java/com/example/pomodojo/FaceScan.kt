package com.example.pomodojo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

 class FaceScan : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var bitmap: Bitmap

     /**
      * Contract between the application and the phone's gallery
      */
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let {
                imageView.setImageURI(it)
                bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(this.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(this.contentResolver, it)
                }
            }
        }
    }


     /**
      * Contract between the application and the phone's camera
      */
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val capturedImage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.extras?.getParcelable("data", Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.extras?.getParcelable<Bitmap>("data")
            }
            capturedImage?.let {
                imageView.setImageBitmap(it)
                bitmap = it
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_scan)

        imageView = findViewById(R.id.imageView)
        val galleryBtn: Button = findViewById(R.id.galleryBtn)
        val cameraBtn: Button = findViewById(R.id.cameraBtn)
        val analyzeBtn: Button = findViewById(R.id.analyzeBtn)

        // Open gallery
        galleryBtn.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(galleryIntent)
        }

        // Open camera
        cameraBtn.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        }

        // Analyze the image
        analyzeBtn.setOnClickListener {
            if (::bitmap.isInitialized) {
                detectFaces(bitmap)
            } else {
                Toast.makeText(this, "Please select or capture an image first!", Toast.LENGTH_SHORT).show()
            }
        }
    }

     /**
      * Function for face detection and the smile analysis
      */
     private fun detectFaces(bitmap: Bitmap) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        // Convert the Bitmap to an InputImage
        val image = InputImage.fromBitmap(bitmap, 0)

        val detector = FaceDetection.getClient(options)

        // Image processing
        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    Toast.makeText(this, "No faces detected!", Toast.LENGTH_SHORT).show()
                } else {
                    for (face in faces) {
                        val smileProb = face.smilingProbability
                        val smileMessage = if (smileProb != null) {
                            "Happiness Coefficient: ${(smileProb * 100).toInt()}%" // Smile output
                        } else {
                            "Mouth is not detected" // No smile output
                        }
                        Toast.makeText(this, smileMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Face detection failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
