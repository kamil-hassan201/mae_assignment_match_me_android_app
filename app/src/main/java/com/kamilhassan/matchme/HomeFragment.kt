package com.kamilhassan.matchme

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.kamilhassan.matchme.models.BoardSize
import com.kamilhassan.matchme.models.MainGame

class HomeFragment: Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
        private const val CREATE_REQUEST_CODE = 9100
    }

    private lateinit var rvBoard: RecyclerView
    private lateinit var clRoot: ConstraintLayout
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var mainGame: MainGame
    private lateinit var adapter: GameBoardAdapter
    private var boardSize: BoardSize = BoardSize.EASY


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val homeView = inflater.inflate(R.layout.home_fragment, container, false)
        rvBoard = homeView.findViewById(R.id.rvGameBoard)
        tvNumMoves = homeView.findViewById(R.id.tvNumMoves)
        tvNumPairs = homeView.findViewById(R.id.tvNumPairs)
        clRoot = homeView.findViewById(R.id.clRoot)
        createBoard()
        return homeView
    }


    // cutom functions
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

        mainGame = MainGame(boardSize)

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
//            val color = ArgbEvaluator().evaluate(mainGame.num_pairs/boardSize.getNumPairs().toFloat(), ContextCompat.getColor(this, R.color.progress_low),
//                ContextCompat.getColor(this,R.color.progress_high) ) as Int
//            tvNumPairs.setTextColor(color)
            if(mainGame.hasWonGame()){
                Snackbar.make(clRoot, "You Won, Congratulations!", Snackbar.LENGTH_LONG).show()
            }
        }

        tvNumMoves.text = mainGame.getNumMoves().toString()

        adapter.notifyDataSetChanged()
    }

}