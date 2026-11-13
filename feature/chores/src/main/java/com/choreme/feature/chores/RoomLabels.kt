package com.choreme.feature.chores

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.choreme.core.model.room.RoomType
import com.choreme.core.model.room.TaskIdea
import com.choreme.core.ui.title
import java.time.LocalDate
import com.choreme.core.model.room.daysSince

@get:StringRes
internal val RoomType.label: Int
    get() = when (this) {
        RoomType.LIVING_ROOM -> R.string.room_type_living_room
        RoomType.BEDROOM -> R.string.room_type_bedroom
        RoomType.KIDS_ROOM -> R.string.room_type_kids_room
        RoomType.BATHROOM -> R.string.room_type_bathroom
        RoomType.KITCHEN -> R.string.room_type_kitchen
        RoomType.DINING_ROOM -> R.string.room_type_dining_room
        RoomType.OFFICE -> R.string.room_type_office
        RoomType.LAUNDRY -> R.string.room_type_laundry
        RoomType.GARAGE -> R.string.room_type_garage
        RoomType.GARDEN -> R.string.room_type_garden
        RoomType.BALCONY -> R.string.room_type_balcony
        RoomType.HALLWAY -> R.string.room_type_hallway
        RoomType.OTHER -> R.string.room_type_other
    }

/** The task's name: a ready-made chore's own name, or this module's for the ideas that have one. */
@get:StringRes
internal val TaskIdea.label: Int
    get() = if (!own) template.title else when (this) {
        TaskIdea.CHANGE_SHEETS -> R.string.idea_change_sheets
        TaskIdea.DUST -> R.string.idea_dust
        TaskIdea.MOP -> R.string.idea_mop
        TaskIdea.SWEEP -> R.string.idea_sweep
        TaskIdea.WINDOWS -> R.string.idea_windows
        TaskIdea.CLEAN_FRIDGE -> R.string.idea_clean_fridge
        TaskIdea.CLEAN_OVEN -> R.string.idea_clean_oven
        TaskIdea.CLEAN_TOILET -> R.string.idea_clean_toilet
        TaskIdea.WASH_TOWELS -> R.string.idea_wash_towels
        TaskIdea.DECLUTTER -> R.string.idea_declutter
        TaskIdea.MOW_LAWN -> R.string.idea_mow_lawn
        else -> template.title
    }

/** "today", "yesterday", "5 days ago". */
@Composable
internal fun agoText(date: LocalDate, today: LocalDate): String {
    val days = daysSince(date, today)
    return when (days) {
        0 -> stringResource(R.string.ago_today)
        1 -> stringResource(R.string.ago_yesterday)
        else -> pluralStringResource(R.plurals.ago_days, days, days)
    }
}

/** "every day", "every 10 days". */
@Composable
internal fun everyText(days: Int): String = pluralStringResource(R.plurals.every_days, days, days)

/** "Every day", "Every 10 days", for a choice button. */
@Composable
internal fun everyChoice(days: Int): String = pluralStringResource(R.plurals.every_days_choice, days, days)
