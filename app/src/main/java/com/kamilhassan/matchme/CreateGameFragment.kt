package com.kamilhassan.matchme

import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.kamilhassan.matchme.models.BoardSize

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
}