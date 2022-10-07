package com.kamilhassan.matchme

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kamilhassan.matchme.models.BoardSize
import com.kamilhassan.matchme.models.MainGame
import com.kamilhassan.matchme.models.UserImageList
import com.squareup.picasso.Picasso

class HomeFragment: Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
    }

    private lateinit var rvBoard: RecyclerView
    private lateinit var clRoot: ConstraintLayout
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var mainGame: MainGame
    private lateinit var adapter: GameBoardAdapter
    private var boardSize: BoardSize = BoardSize.EASY
    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val homeView = inflater.inflate(R.layout.home_fragment, container, false)

        // checking if has bundle from new custom game

        val bundle = this.arguments

        if (bundle != null) {
            val customGameName = bundle.get("gameName") as String
            gameName = customGameName
            val fragment: Fragment? = requireFragmentManager().findFragmentByTag("prevHome")
            if (fragment != null) {
                val manager: FragmentManager = requireFragmentManager()
                //manager.popBackStack(manager.getBackStackEntryAt(manager.getBackStackEntryCount()-1).getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            downloadGame(customGameName)
        }

        rvBoard = homeView.findViewById(R.id.rvGameBoard)
        tvNumMoves = homeView.findViewById(R.id.tvNumMoves)
        tvNumPairs = homeView.findViewById(R.id.tvNumPairs)
        clRoot = homeView.findViewById(R.id.clRoot)
        createBoard()
        return homeView
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        (activity as AppCompatActivity).supportActionBar?.title = if (gameName == null) "Match Me" else gameName
    }

    // menu related code
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mb_refresh ->{
                if(mainGame.getNumMoves() > 0 && !mainGame.hasWonGame()){
                    showAlertDialog("Quit the current game?", null, View.OnClickListener {
                        createBoard()
                    })
                }
                else{
                    createBoard()
                }
                return true
            }
            R.id.mb_createNewGame ->{
                showBoardSizeDialog()
                return true
            }
            R.id.mb_create_custom ->{
                showCreateCustomDialog()
                return true
            }
            R.id.mi_playcustom ->{
                showCustomGameDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    // custom functions

    private fun showCustomGameDialog() {
        val customDownloadView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_game_download, null)
        showAlertDialog("Fetch memory game", customDownloadView, View.OnClickListener {
            val etDownloadGame = customDownloadView.findViewById<EditText>(R.id.et_downloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })

    }



    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener {document->
            val userImageList: UserImageList? = document.toObject(UserImageList::class.java)
            if(userImageList?.images == null){
                Log.e(TAG, "Invalid data from firestore")
                Snackbar.make(clRoot, "Sorry! no such game", Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            val numberCards = userImageList.images.size * 2
            boardSize = BoardSize.getByValue(numberCards)
            customGameImages = userImageList.images
            gameName = customGameName

            createBoard()

            // pre fetch images
            for (imageUrl in userImageList.images){
                Picasso.get().load(imageUrl).fetch()
            }

            Snackbar.make(clRoot, "Now playing: $gameName", Snackbar.LENGTH_LONG).show()

            (activity as AppCompatActivity).supportActionBar?.title = if (gameName == null) "Match Me" else gameName

        }.addOnFailureListener{exception->
            Log.e(TAG, "Exception when retrieving game", exception)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createBoard() {

        when(boardSize){
            BoardSize.EASY -> {
                tvNumMoves.text = "0"
                tvNumPairs.text = "0/4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "0"
                tvNumPairs.text = "0/9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "0"
                tvNumPairs.text = "0/12"
            }
        }

        mainGame = MainGame(boardSize, customGameImages)

        // creating adapter and layout (Recycler view logic)
        adapter = GameBoardAdapter(requireContext(), boardSize, mainGame.cards, object:
            GameBoardAdapter.CardClickListener {
            override fun onCardClick(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter = adapter
        rvBoard.hasFixedSize()
        rvBoard.layoutManager= GridLayoutManager(requireContext(), boardSize.getWidth())
    }
    private fun updateGameWithFlip(position: Int) {
        // error handling
        if(mainGame.hasWonGame()){
            Snackbar.make(clRoot, "You have already won!", Snackbar.LENGTH_LONG).show()
            return
        }
        if(mainGame.isCardFacedUp(position)){
            Snackbar.make(clRoot, "Invalid move!", Snackbar.LENGTH_SHORT).show()
            return
        }
        if(mainGame.flipCard(position)){
            tvNumPairs.text = "${mainGame.num_pairs}/${boardSize.getNumPairs()}"
            if(mainGame.hasWonGame()){
                Snackbar.make(clRoot, "You Won, Congratulations!", Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.GREEN, Color.MAGENTA, Color.RED)).oneShot()
            }
        }

        tvNumMoves.text = mainGame.getNumMoves().toString()

        adapter.notifyDataSetChanged()
    }



    // show custom modals
    private fun showCreateCustomDialog() {
        val boardSizeView = LayoutInflater.from(context).inflate(R.layout.game_option_view, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.rd_game_option_group)

        showAlertDialog("Choose new custom game size!", boardSizeView, View.OnClickListener {
            val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rb_easy -> BoardSize.EASY;
                R.id.rb_medium -> BoardSize.MEDIUM;
                else -> BoardSize.HARD
            }

            val bundle = Bundle()
            bundle.putSerializable("boardSize", desiredBoardSize)

            val createGame =  CreateGameFragment()
            createGame.arguments = bundle

            requireFragmentManager()!!.beginTransaction().replace(R.id.fragmentContainer, createGame, "prevHome").addToBackStack(null).commit()

        })
    }

    private fun showBoardSizeDialog() {
        val boardSizeView = LayoutInflater.from(context).inflate(R.layout.game_option_view, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.rd_game_option_group)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rb_easy);
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rb_medium);
            BoardSize.HARD -> radioGroupSize.check(R.id.rb_hard);
        }
        showAlertDialog("Choose new board size!", boardSizeView, View.OnClickListener {
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rb_easy -> BoardSize.EASY;
                R.id.rb_medium -> BoardSize.MEDIUM;
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            createBoard()
        })
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle(title)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok"){_, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }


}