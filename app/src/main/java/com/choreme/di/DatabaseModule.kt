package com.choreme.di

import android.content.Context
import androidx.room.Room
import com.choreme.data.local.ChoreMeDatabase
import com.choreme.data.local.GroceryItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChoreMeDatabase =
        Room.databaseBuilder(context, ChoreMeDatabase::class.java, "choreme.db").build()

    @Provides
    fun provideGroceryItemDao(db: ChoreMeDatabase): GroceryItemDao = db.groceryItemDao()
}
