/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.themovie.app.movieapp.di

import android.content.Context
import androidx.room.Room
import com.themovie.app.movieapp.data.DefaultTaskRepository
import com.themovie.app.movieapp.data.TaskRepository
import com.themovie.app.movieapp.data.source.local.TheMovieDao
import com.themovie.app.movieapp.data.source.local.TheMovieDatabase
import com.themovie.app.movieapp.data.source.network.NetworkDataSource
import com.themovie.app.movieapp.data.source.network.TaskNetworkDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindTaskRepository(repository: DefaultTaskRepository): TaskRepository
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Singleton
    @Binds
    abstract fun bindNetworkDataSource(dataSource: TaskNetworkDataSource): NetworkDataSource
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDataBase(@ApplicationContext context: Context): TheMovieDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            TheMovieDatabase::class.java,
            "TheMovie.db"
        ).build()
    }

    @Provides
    fun provideTaskDao(database: TheMovieDatabase): TheMovieDao = database.theMovieDao()
}