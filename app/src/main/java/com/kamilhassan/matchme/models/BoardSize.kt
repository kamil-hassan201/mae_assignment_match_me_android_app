package com.kamilhassan.matchme.models

enum class BoardSize (val numPieces: Int){
    EASY( 8),
    MEDIUM( 18),
    HARD( 24);

    companion object{
        fun getByValue(value:Int) = values().first{it.numPieces == value}
    }

    fun getWidth(): Int {
        return when(this){
            EASY-> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }
    fun getHeight(): Int{
        return numPieces / getWidth()
    }
    fun getNumPairs(): Int{
        return numPieces / 2
    }
}