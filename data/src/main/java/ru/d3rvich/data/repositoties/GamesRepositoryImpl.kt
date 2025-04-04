package ru.d3rvich.data.repositoties

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.d3rvich.core.domain.entities.GameDetailEntity
import ru.d3rvich.core.domain.entities.GameEntity
import ru.d3rvich.core.domain.entities.ScreenshotEntity
import ru.d3rvich.core.domain.model.LoadSource
import ru.d3rvich.core.domain.model.Result
import ru.d3rvich.core.domain.model.map
import ru.d3rvich.core.domain.preferences.FilterPreferencesBody
import ru.d3rvich.core.domain.repositories.GamesRepository
import ru.d3rvich.data.mapper.toGameDBO
import ru.d3rvich.data.mapper.toGameDetailEntity
import ru.d3rvich.data.mapper.toGameEntity
import ru.d3rvich.data.mapper.toScreenshotEntityList
import ru.d3rvich.data.paging.GamesPagingSource
import ru.d3rvich.data.util.safeApiCall
import ru.d3rvich.database.JetGamesDatabase
import ru.d3rvich.remote.JetGamesApiService

/**
 * Created by Ilya Deryabin at 01.02.2024
 */
internal class GamesRepositoryImpl(
    private val gamesPagingSourceFactory: GamesPagingSource.GamesPagingSourceFactory,
    private val apiService: JetGamesApiService,
    private val database: JetGamesDatabase,
) : GamesRepository {

    private companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }

    override fun getGames(
        search: String,
        filterPreferencesBody: FilterPreferencesBody,
    ): Flow<PagingData<GameEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                gamesPagingSourceFactory.create(search, filterPreferencesBody)
            }).flow
    }

    override suspend fun getGameDetail(
        gameId: Int,
        loadSource: LoadSource,
    ): Result<GameDetailEntity> {
        return when (loadSource) {
            LoadSource.Local -> {
                when (val detail = database.gamesDao.gameDetail(gameId)) {
                    null -> {
                        Result.Failure(IllegalArgumentException("Game with id={$gameId} doesn't exist!"))
                    }

                    else -> {
                        Result.Success(detail.toGameDetailEntity())
                    }
                }
            }

            LoadSource.Network -> {
                safeApiCall {
                    apiService.getGameDetail(gameId = gameId)
                }.map { it.toGameDetailEntity() }
            }
        }
    }

    override suspend fun getGameScreenshots(gameId: Int): Result<List<ScreenshotEntity>> =
        safeApiCall {
            apiService.getScreenshots(gameId = gameId)
        }.map { it.results.toScreenshotEntityList() }

    override fun getFavoriteGames(search: String): Flow<PagingData<GameEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                if (search.isEmpty()) {
                    database.gamesDao.games()
                } else {
                    database.gamesDao.search(query = search)
                }
            }
        ).flow.map { pagingData -> pagingData.map { gameDBO -> gameDBO.toGameEntity() } }
    }

    override suspend fun isGameFavorite(gameId: Int): Boolean =
        database.gamesDao.isGameExist(gameId = gameId)

    override suspend fun saveGameDetail(gameDetail: GameDetailEntity): Result<Unit> {
        return try {
            database.gamesDao.insert(game = gameDetail.toGameDBO())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun deleteGameDetail(gameDetail: GameDetailEntity): Result<Unit> {
        return try {
            database.gamesDao.delete(game = gameDetail.toGameDBO())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}