package com.choreme.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Items are kept forever and toggled between "have" and "need", so the weekly
 * shop is a matter of ticking what ran out rather than retyping the list.
 */
@Entity(tableName = "grocery_items")
data class GroceryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val isNeeded: Boolean,
    val isStaple: Boolean,
    val updatedAt: Long,
)
