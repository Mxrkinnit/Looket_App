package com.example.looketapp

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import java.io.ByteArrayOutputStream

class AddPostActivity : AppCompatActivity() {

    private lateinit var titleBlogEditText: EditText
    private lateinit var descriptionBlogEditText: EditText
    private lateinit var uploadButton: Button
    private lateinit var blogImageImageView: ImageView

    private var imageUri: Uri? = null
    private val GALLERY_IMAGE_CODE = 100
    private val CAMERA_IMAGE_CODE = 200
    private lateinit var progressDialog: ProgressDialog
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        val actionBar: ActionBar? = supportActionBar
        actionBar?.title = "Add Post"
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        // Call the method here and ask for permission in the manifest
        permission()
        titleBlogEditText = findViewById(R.id.title_blog)
        descriptionBlogEditText = findViewById(R.id.description_blog)
        uploadButton = findViewById(R.id.upload)
        blogImageImageView = findViewById(R.id.post_image_blog)

        progressDialog = ProgressDialog(this)
        auth = FirebaseAuth.getInstance()

        // When user clicks on the image, ask to choose the image from gallery or camera
        blogImageImageView.setOnClickListener {
            imagePickDialog()
        }

        // When the user clicks on the upload button, upload the data to Firebase
        uploadButton.setOnClickListener {
            val title = titleBlogEditText.text.toString()
            val description = descriptionBlogEditText.text.toString()

            if (TextUtils.isEmpty(title)) {
                titleBlogEditText.error = "Title is required"
            } else if (TextUtils.isEmpty(description)) {
                descriptionBlogEditText.error = "Description is required"
            } else {
                uploadData(title, description)
            }
        }
    }

    private fun uploadData(title: String, description: String) {
        progressDialog.setMessage("Publishing post")
        progressDialog.show()

        // Get the time when the user uploads the post
        val timeStamp = System.currentTimeMillis().toString()

        // Set the file path of the image
        val filePath = "Posts/post_$timeStamp"

        if (blogImageImageView.drawable != null) {
            // Get the image from the ImageView
            val bitmap = (blogImageImageView.drawable as BitmapDrawable).bitmap
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val data = baos.toByteArray()

            // Create the reference of storage in Firebase
            val reference: StorageReference =
                FirebaseStorage.getInstance().reference.child(filePath)
            reference.putBytes(data)
                .addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                    val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                    // Wait until the URI task is successful
                    while (!uriTask.isSuccessful);
                    val downloadUri = uriTask.result.toString()

                    if (uriTask.isSuccessful) {
                        // URI is received, post is published to the database

                        // Upload the data to the Firebase Database
                        val user: FirebaseUser? = auth.currentUser
                        val hashMap: HashMap<String, Any> = HashMap()
                        hashMap["uid"] = user?.uid ?: ""
                        hashMap["uEmail"] = user?.email ?: ""
                        hashMap["pId"] = timeStamp
                        hashMap["pTitle"] = title
                        hashMap["pImage"] = downloadUri
                        hashMap["pDescription"] = description
                        hashMap["pTime"] = timeStamp

                        // Push the data to the Firebase Database
                        val ref: DatabaseReference = FirebaseDatabase.getInstance().getReference("Posts")
                        ref.child(timeStamp).setValue(hashMap)
                            .addOnSuccessListener(OnSuccessListener<Void?> {
                                progressDialog.dismiss()
                                Toast.makeText(
                                    this@AddPostActivity,
                                    "Post Published",
                                    Toast.LENGTH_SHORT
                                ).show()
                                titleBlogEditText.setText("")
                                descriptionBlogEditText.setText("")
                                blogImageImageView.setImageURI(null)
                                imageUri = null

                                // After post is published, go to HomeActivity (main dashboard)
                                startActivity(Intent(this@AddPostActivity, HomeActivity::class.java))
                            })
                            .addOnFailureListener(OnFailureListener { e ->
                                progressDialog.dismiss()
                                Toast.makeText(
                                    this@AddPostActivity,
                                    "" + e.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            })
                    }
                })
                .addOnFailureListener(OnFailureListener { e ->
                    Toast.makeText(this@AddPostActivity, "" + e.message, Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                })
        }
    }

    private fun imagePickDialog() {
        // 0 is for camera and 1 is for gallery
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose image from")
        builder.setItems(options, DialogInterface.OnClickListener { dialog, which ->
            if (which == 0) {
                cameraPick()
            }
            if (which == 1) {
                galleryPick()
            }
        })
        builder.create().show()
    }

    private fun cameraPick() {
        // Camera
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp Pick")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp desc")
        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, CAMERA_IMAGE_CODE)
    }

    private fun galleryPick() {
        // Gallery
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_IMAGE_CODE)
    }

    private fun permission() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {}
                override fun onPermissionDenied(response: PermissionDeniedResponse) {}
                override fun onPermissionRationaleShouldBeShown(
                    p0: com.karumi.dexter.listener.PermissionRequest?,
                    token: PermissionToken?
                ) {
                    if (token != null) {
                        token.continuePermissionRequest()
                    }
                }
            }).check()

        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {}
                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                    token: PermissionToken?
                ) {
                }
            }).check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY_IMAGE_CODE) {
                imageUri = data?.data
                blogImageImageView.setImageURI(imageUri)
            }
            if (requestCode == CAMERA_IMAGE_CODE) {
                blogImageImageView.setImageURI(imageUri)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
