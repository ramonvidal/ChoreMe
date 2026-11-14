package com.choreme.core.ui

import androidx.annotation.DrawableRes
import com.choreme.core.model.room.RoomType

/** The small picture of a kind of room: on its card, next to its name, and in the type picker. */
@get:DrawableRes
val RoomType.icon: Int
    get() = when (this) {
        RoomType.LIVING_ROOM -> R.drawable.ic_item_sofa
        RoomType.BEDROOM -> R.drawable.ic_item_bed
        RoomType.KIDS_ROOM -> R.drawable.ic_room_kids
        RoomType.BATHROOM -> R.drawable.ic_item_bath
        RoomType.KITCHEN -> R.drawable.ic_room_kitchen
        RoomType.DINING_ROOM -> R.drawable.ic_room_dining
        RoomType.OFFICE -> R.drawable.ic_room_office
        RoomType.LAUNDRY -> R.drawable.ic_item_laundry
        RoomType.GARAGE -> R.drawable.ic_room_garage
        RoomType.GARDEN -> R.drawable.ic_room_garden
        RoomType.BALCONY -> R.drawable.ic_room_balcony
        RoomType.HALLWAY -> R.drawable.ic_room_hallway
        RoomType.OTHER -> R.drawable.ic_room_other
    }
