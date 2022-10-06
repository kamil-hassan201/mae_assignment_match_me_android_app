package com.kamilhassan.matchme.models

import com.kamilhassan.matchme.utilities.DEFAULT_ICONS

class MainGame(private val boardSize: BoardSize, private val customImages: List<String>?) {
    val cards: List<MemoryCard>
    var num_pairs = 0
    private var numCardFlips: Int = 0
    private var indexOfSingleSelectedCard: Int? = null
    init {
        if(customImages == null){
            val choosen_icons = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
            val randomized_icons = (choosen_icons + choosen_icons).shuffled()
            cards = randomized_icons.map{MemoryCard(it)}
        }
        else{
            val randomizedImages = (customImages + customImages).shuffled()
            cards = randomizedImages.map{ MemoryCard(it.hashCode(),it) }
        }

    }
    fun flipCard(position: Int): Boolean {
        numCardFlips++
        var foundMatch = false
        if(indexOfSingleSelectedCard == null){
            // 0 or 2 cards flipped over
            restoreCards()
            indexOfSingleSelectedCard = position
        }
        else{
            // 1 card flipped over
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        //flip
        cards[position].isFaceUp = !cards[position].isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if(cards[position1].identifier != cards[position2].identifier){
            return false
        }
        else{
            cards[position1].isMatched = true
            cards[position2].isMatched = true
            num_pairs++
            return true        }
    }

    private fun restoreCards() {
        for (card in cards){
            if(!card.isMatched){
                card.isFaceUp = false
            }
        }
    }

    fun hasWonGame(): Boolean {
        return num_pairs == boardSize.getNumPairs()
    }
    fun isCardFacedUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        return numCardFlips / 2
    }
}
