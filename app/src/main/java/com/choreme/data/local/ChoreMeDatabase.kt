package com.choreme.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GroceryItemEntity::class], version = 1)
abstract class ChoreMeDatabase : RoomDatabase() {
    abstract fun groceryItemDao(): GroceryItemDao
}
