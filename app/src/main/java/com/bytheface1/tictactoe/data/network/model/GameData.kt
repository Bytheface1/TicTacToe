package com.bytheface1.tictactoe.data.network.model

import com.bytheface1.tictactoe.ui.model.GameModel
import com.bytheface1.tictactoe.ui.model.PlayerModel
import com.bytheface1.tictactoe.ui.model.PlayerType
import java.util.Calendar

data class GameData(
    val board: List<Int?>? = null,
    val gameId: String? = null,
    val player1: PlayerData? = null,
    val player2: PlayerData? = null,
    val playerTurn: PlayerData? = null
) {
    fun toModel(): GameModel {
        return GameModel(
            board = board?.map { PlayerType.getPlayerById(it) } ?: mutableListOf(),
            gameId = gameId.orEmpty(),
            player1 = player1!!.toModel(),
            player2 = player2?.toModel(),
            playerTurn = playerTurn!!.toModel()
        )
    }
}

data class PlayerData(
    val userId: String? = Calendar.getInstance().timeInMillis.hashCode()
        .toString(), // Primary Key using hash of time
    val playerType: Int? = null
) {
    fun toModel(): PlayerModel {
        return PlayerModel(
            userId = userId!!,
            playerType = PlayerType.getPlayerById(playerType)
        )
    }
}