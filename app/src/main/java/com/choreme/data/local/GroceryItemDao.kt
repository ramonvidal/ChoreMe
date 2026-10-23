package com.choreme.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryItemDao {
    @Query("SELECT * FROM grocery_items ORDER BY category, name")
    fun observeAll(): Flow<List<GroceryItemEntity>>

    @Upsert
    suspend fun upsert(item: GroceryItemEntity)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun delete(id: String)
}
