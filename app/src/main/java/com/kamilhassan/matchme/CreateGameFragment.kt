package com.kamilhassan.matchme

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.kamilhassan.matchme.models.BoardSize
import com.kamilhassan.matchme.utilities.BitmapScaler
import com.kamilhassan.matchme.utilities.EXTRA_GAME_NAME
import com.kamilhassan.matchme.utilities.isPermissionGranted
import com.kamilhassan.matchme.utilities.requestPermission
import java.io.ByteArrayOutputStream

class CreateGameFragment: Fragment(){

    companion object {
        private const val TAG = "CREATE_GAME"
        private const val PICK_PHOTOS_CODE = 1234
        private const val READ_EXTERNAL_PHOTOS_CODE = 987
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_LENGTH = 3
        private const val MAX_GAME_LENGTH = 14

    }
    private lateinit var boardSize: BoardSize
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var etGameName: EditText
    private lateinit var adapter: ImagePickerAdapter
    private lateinit var pbUpload: ProgressBar

    private var numOfPics = -1
    private val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val createGameView = inflater.inflate(R.layout.create_game_fragment, container, false)
        val bundle = this.arguments

        if (bundle != null) {
            boardSize = bundle.get("boardSize") as BoardSize
        }
        numOfPics = boardSize.getNumPairs()

        // initializing ids
        rvImagePicker = createGameView.findViewById(R.id.rvImagePicker)
        btnSave = createGameView.findViewById(R.id.btn_saveCustom)
        etGameName = createGameView.findViewById(R.id.et_gameName)
        pbUpload = createGameView.findViewById(R.id.pbUpload)

        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_LENGTH))
        etGameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }
            override fun afterTextChanged(p0: Editable?) {}
        })


        btnSave.setOnClickListener{
            val imm = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
            saveDataToFirebase()
        }

        adapter = ImagePickerAdapter(requireContext(), chosenImageUris,boardSize, object :ImagePickerAdapter.ImageClickListener{
            override fun onPlaceHolderClicked() {
                if(isPermissionGranted(requireContext(), READ_PHOTOS_PERMISSION)){
                    launchIntentForPhotos()
                }
                else{
                    requestPermission(activity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }
            }

        })
        rvImagePicker.adapter = adapter
        rvImagePicker.hasFixedSize()
        rvImagePicker.layoutManager = GridLayoutManager(requireContext(), boardSize.getWidth())


        (activity as AppCompatActivity).supportActionBar?.title = "Chosen pics: (0 / ${boardSize.getNumPairs()})"
        return createGameView
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            android.R.id.home ->{
                fragmentManager?.popBackStackImmediate()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode != PICK_PHOTOS_CODE || resultCode != Activity.RESULT_OK || data == null){
            Log.i(TAG, "Images haven't been selected. The user may have been closed the activity")
        }
        else{
            val selectedUri = data.data
            val clipData = data.clipData
            if(clipData != null){
                Log.i(TAG, "Num Images Selected ${clipData.itemCount} $clipData")
                for (i in 0 until clipData.itemCount){
                    if(chosenImageUris.size < boardSize.getNumPairs()){
                        var clipItem = clipData.getItemAt(i)
                        chosenImageUris.add(clipItem.uri)
                    }
                }
            }
            else if(selectedUri != null){
                chosenImageUris.add(selectedUri)
            }
        }
        adapter.notifyDataSetChanged()
        (activity as AppCompatActivity).supportActionBar?.title = "Chosen pics: (${chosenImageUris.size} / ${boardSize.getNumPairs()})"

        btnSave.isEnabled = shouldEnableSaveButton()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                launchIntentForPhotos()
            }
            else{
                Toast.makeText(requireContext(), "In order to create custom game, you need to provide permission to access storage", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    // custom functions

    private fun saveDataToFirebase() {
        btnSave.isEnabled = false
        val gameName = etGameName.text.toString()
        // check if the same game name exist in db
        db.collection("games").document(gameName).get()
            .addOnSuccessListener { document->
                if(document != null && document.data !== null){
                    AlertDialog.Builder(requireContext())
                        .setTitle("Name already taken")
                        .setMessage("$gameName already exist! please try another name")
                        .setPositiveButton("OK", null)
                        .show()
                }else{
                    handleImageUploadingToFirebase(gameName)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Encountered error while uploading the game", exception)
                Toast.makeText(requireContext(), "Error in saving the game", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
            }
    }

    private fun handleImageUploadingToFirebase(gameName: String) {
        pbUpload.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()
        for((index, photoUri) in chosenImageUris.withIndex()){
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath)

            photoReference.putBytes(imageByteArray)
                .continueWithTask{ photoUploadTask ->
                    Log.i(TAG, "Uploaded Bytes: ${photoUploadTask.result?.bytesTransferred}")
                    photoReference.downloadUrl
                }.addOnCompleteListener{ downloadUrlTask ->
                    if(!downloadUrlTask.isSuccessful){
                        Log.e(TAG, "Error in firebase storage", downloadUrlTask.exception)
                        Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if(didEncounterError){
                        pbUpload.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)

                    pbUpload.progress = uploadedImageUrls.size * 100 / chosenImageUris.size

                    Log.i(TAG, "Finished Upload ${downloadUrl}")
                    if(uploadedImageUrls.size == chosenImageUris.size){
                        handleImageUploaded(gameName, uploadedImageUrls)
                    }
                }
        }
    }

    private fun handleImageUploaded(gameName: String, uploadedImageUrls: MutableList<String>) {
        db.collection("games").document(gameName)
            .set(mapOf("images" to uploadedImageUrls))
            .addOnCompleteListener{createGameTask->
                pbUpload.visibility = View.GONE
                if(!createGameTask.isSuccessful){
                    Log.e(TAG, "Error in creating game", createGameTask.exception)
                    Toast.makeText(requireContext(), "Failed to create new game", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created $gameName")
                AlertDialog.Builder(requireContext())
                    .setTitle("Game uploaded! Let's play $gameName")
                    .setPositiveButton("OK") {_,_->
                        // going back to home fragment
                        val bundle = Bundle()
                        bundle.putSerializable("gameName", gameName)

                        val homeFragment =  HomeFragment()
                        homeFragment.arguments = bundle

                        requireFragmentManager()!!.beginTransaction().replace(R.id.fragmentContainer, homeFragment).commit()
                    }.show()
            }
    }


    @SuppressLint("ServiceCast")
    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val mainBitmap = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source = ImageDecoder.createSource(activity?.contentResolver!!, photoUri)
            ImageDecoder.decodeBitmap(source)
        }
        else{
            MediaStore.Images.Media.getBitmap(activity?.contentResolver!!, photoUri)
        }
        val scaledBitmap = BitmapScaler.scaleToFitHeight(mainBitmap, 250)
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    private fun shouldEnableSaveButton(): Boolean {
        if(chosenImageUris.size != boardSize.getNumPairs() ){
            return false
        }
        if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_LENGTH){
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose Images"), PICK_PHOTOS_CODE)

    }

}