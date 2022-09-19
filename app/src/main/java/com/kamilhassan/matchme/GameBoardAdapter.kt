package com.kamilhassan.matchme

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.kamilhassan.matchme.models.BoardSize
import com.kamilhassan.matchme.models.MemoryCard
import kotlin.math.min

class GameBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) : RecyclerView.Adapter<GameBoardAdapter.ViewHolder>() {
    companion object{
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryBoardAdapter"
    }

    interface CardClickListener {
        fun onCardClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val width = (parent.width / boardSize.getWidth()) - MARGIN_SIZE * 2
        val height = (parent.height / boardSize.getHeight()) - MARGIN_SIZE * 2
        val sideLength = min(width, height)
        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)

        // get card view
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.height = sideLength
        layoutParams.width = sideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.numPieces

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)
        fun bind(position: Int) {
            val card = cards[position]
            imageButton.setImageResource(if(card.isFaceUp) card.identifier else R.drawable.card_bg2)

            // set pair colors
            imageButton.alpha = if(card.isMatched) 0.4f else 1.0f
            val colorStateList = if (card.isMatched) ContextCompat.getColorStateList(context, R.color.color_purple) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)

            imageButton.setOnClickListener(){
                cardClickListener.onCardClick(position)
            }
        }
    }

}
