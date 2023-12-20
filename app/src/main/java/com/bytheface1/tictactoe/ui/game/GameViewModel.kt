package com.bytheface1.tictactoe.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytheface1.tictactoe.data.network.FirebaseService
import com.bytheface1.tictactoe.ui.model.GameModel
import com.bytheface1.tictactoe.ui.model.PlayerModel
import com.bytheface1.tictactoe.ui.model.PlayerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(private val firebaseService: FirebaseService) :
    ViewModel() {

    private lateinit var userId: String

    private var _game = MutableStateFlow<GameModel?>(null)
    val game: StateFlow<GameModel?> = _game

    private var _winner =
        MutableStateFlow<PlayerType?>(null)       // When winner != null there is a winner
    val winner: StateFlow<PlayerType?> = _winner

    fun joinToGame(gameId: String, userId: String, owner: Boolean) {
        this.userId = userId
        if (owner) {
            join(gameId)
        } else {
            joinGameLikeGuest(gameId)
        }
    }

    private fun joinGameLikeGuest(gameId: String) {
        viewModelScope.launch {
            firebaseService.joinToGame(gameId).take(1).collect {
                var result = it
                if (result != null) {
                    result = result.copy(player2 = PlayerModel(userId, PlayerType.SecondPlayer))
                    firebaseService.updateGame(result.toData())
                }
            }

            join(gameId)
        }
    }

    /**
     * Method that updates Firebase Data Model
     */
    private fun join(gameId: String) {
        viewModelScope.launch {
            firebaseService.joinToGame(gameId).collect {
                val result =
                    it?.copy(isGameReady = it.player2 != null, isMyTurn = isMyTurn(it.playerTurn))
                _game.value = result
                verifyWinner()
            }
        }
    }

    private fun isMyTurn(playerTurn: PlayerModel): Boolean {
        return playerTurn.userId == userId
    }

    fun onItemSelected(position: Int) {
        val currentGame = _game.value ?: return
        if (currentGame.isGameReady && currentGame.board[position] == PlayerType.Empty && isMyTurn(
                currentGame.playerTurn
            )
        ) {
            viewModelScope.launch {
                val newBoard = currentGame.board.toMutableList()
                newBoard[position] = getPlayer() ?: PlayerType.Empty
                firebaseService.updateGame(
                    currentGame.copy(
                        board = newBoard,
                        playerTurn = getEnemyPlayer()!!
                    ).toData()
                )
            }
        }
    }

    private fun verifyWinner() {
        val board = _game.value?.board
        if (board != null && board.size == 9) {
            when{
                (isGameWon(board, PlayerType.FirstPlayer)) -> _winner.value = PlayerType.FirstPlayer
                (isGameWon(board, PlayerType.SecondPlayer)) -> _winner.value = PlayerType.SecondPlayer
                (isBoardFull(board)) -> _winner.value = PlayerType.Empty
                else -> _winner.value = null
            }
        }
    }

    fun isBoardFull(board: List<PlayerType?>): Boolean {
        return board.all { it != null && it != PlayerType.Empty }
    }

    private fun isGameWon(board: List<PlayerType>, playerType: PlayerType): Boolean {
        val winningCombinations = listOf(
            //Row
            listOf(0, 1, 2),
            listOf(3, 4, 5),
            listOf(6, 7, 8),
            //Column
            listOf(0, 3, 6),
            listOf(1, 4, 7),
            listOf(2, 5, 8),
            //Diagonal
            listOf(0, 4, 8),
            listOf(2, 4, 6)
        )

        return winningCombinations.any { combination ->
            combination.all { index -> board[index] == playerType }
        }
    }


    private fun getPlayer(): PlayerType? {
        return when {
            (game.value?.player1?.userId == userId) -> PlayerType.FirstPlayer
            (game.value?.player2?.userId == userId) -> PlayerType.SecondPlayer
            else -> null
        }
    }

    private fun getEnemyPlayer(): PlayerModel? {
        return if (game.value?.player1?.userId == userId) game.value?.player2 else game.value?.player1
    }
}