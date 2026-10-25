package com.choreme.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.grocery.GroceryIcon

/*
 * Pictures for chores and grocery items: line drawings from Tabler Icons (MIT), copied into
 * res/drawable as ic_item_*. App controls use Material Symbols (Apache 2.0), as ic_<name>.
 * Licenses: assets/licenses. They take the color of the text around them, so they suit dark mode.
 */

/** Shown wherever a chore appears: the Today tab, the agenda and the widget. */
@get:DrawableRes
val ChoreTemplate.icon: Int
    get() = when (this) {
        ChoreTemplate.DISHES -> R.drawable.ic_item_dishes
        ChoreTemplate.LAUNDRY -> R.drawable.ic_item_laundry
        ChoreTemplate.FOLD_CLOTHES -> R.drawable.ic_item_shirt
        ChoreTemplate.CLEAN_KITCHEN -> R.drawable.ic_item_spray
        ChoreTemplate.CLEAN_BATHROOM -> R.drawable.ic_item_bath
        ChoreTemplate.TRASH -> R.drawable.ic_item_trash
        ChoreTemplate.VACUUM -> R.drawable.ic_item_vacuum
        ChoreTemplate.MAKE_BEDS -> R.drawable.ic_item_bed
        ChoreTemplate.WATER_PLANTS -> R.drawable.ic_item_plant
        ChoreTemplate.GROCERIES -> R.drawable.ic_item_cart
        ChoreTemplate.OTHER -> R.drawable.ic_item_sparkles
    }

@get:DrawableRes
val GroceryIcon.drawable: Int
    get() = when (this) {
        GroceryIcon.APPLE -> R.drawable.ic_item_apple
        GroceryIcon.BANANA -> R.drawable.ic_item_banana
        GroceryIcon.CITRUS -> R.drawable.ic_item_citrus
        GroceryIcon.LEMON -> R.drawable.ic_item_lemon
        GroceryIcon.GRAPE -> R.drawable.ic_item_grape
        GroceryIcon.CHERRY -> R.drawable.ic_item_cherry
        GroceryIcon.MELON -> R.drawable.ic_item_melon
        GroceryIcon.AVOCADO -> R.drawable.ic_item_avocado
        GroceryIcon.CARROT -> R.drawable.ic_item_carrot
        GroceryIcon.PEPPER -> R.drawable.ic_item_pepper
        GroceryIcon.MUSHROOM -> R.drawable.ic_item_mushroom
        GroceryIcon.SALAD -> R.drawable.ic_item_salad
        GroceryIcon.LEAF -> R.drawable.ic_item_leaf
        GroceryIcon.BREAD -> R.drawable.ic_item_bread
        GroceryIcon.BAGUETTE -> R.drawable.ic_item_baguette
        GroceryIcon.CAKE -> R.drawable.ic_item_cake
        GroceryIcon.MEAT -> R.drawable.ic_item_meat
        GroceryIcon.SAUSAGE -> R.drawable.ic_item_sausage
        GroceryIcon.FISH -> R.drawable.ic_item_fish
        GroceryIcon.MILK -> R.drawable.ic_item_milk
        GroceryIcon.EGG -> R.drawable.ic_item_egg
        GroceryIcon.CHEESE -> R.drawable.ic_item_cheese
        GroceryIcon.NOODLES -> R.drawable.ic_item_noodles
        GroceryIcon.CEREAL -> R.drawable.ic_item_cereal
        GroceryIcon.WHEAT -> R.drawable.ic_item_wheat
        GroceryIcon.SALT -> R.drawable.ic_item_salt
        GroceryIcon.BOTTLE -> R.drawable.ic_item_bottle
        GroceryIcon.NUT -> R.drawable.ic_item_nut
        GroceryIcon.SOUP -> R.drawable.ic_item_soup
        GroceryIcon.COFFEE -> R.drawable.ic_item_coffee
        GroceryIcon.TEA -> R.drawable.ic_item_tea
        GroceryIcon.CHOCOLATE -> R.drawable.ic_item_chocolate
        GroceryIcon.ICE_CREAM -> R.drawable.ic_item_ice_cream
        GroceryIcon.PIZZA -> R.drawable.ic_item_pizza
        GroceryIcon.FROZEN -> R.drawable.ic_item_frozen
        GroceryIcon.WATER -> R.drawable.ic_item_water
        GroceryIcon.JUICE -> R.drawable.ic_item_juice
        GroceryIcon.BEER -> R.drawable.ic_item_beer
        GroceryIcon.WINE -> R.drawable.ic_item_wine
        GroceryIcon.COOKIE -> R.drawable.ic_item_cookie
        GroceryIcon.CHIPS -> R.drawable.ic_item_chips
        GroceryIcon.CANDY -> R.drawable.ic_item_candy
        GroceryIcon.TOILET_PAPER -> R.drawable.ic_item_toilet_paper
        GroceryIcon.BUCKET -> R.drawable.ic_item_bucket
        GroceryIcon.LAUNDRY -> R.drawable.ic_item_laundry
        GroceryIcon.SPRAY -> R.drawable.ic_item_spray
        GroceryIcon.TRASH -> R.drawable.ic_item_trash
        GroceryIcon.BATTERY -> R.drawable.ic_item_battery
        GroceryIcon.BULB -> R.drawable.ic_item_bulb
        GroceryIcon.DENTAL -> R.drawable.ic_item_dental
        GroceryIcon.LOTION -> R.drawable.ic_item_lotion
        GroceryIcon.RAZOR -> R.drawable.ic_item_razor
        GroceryIcon.DIAPER -> R.drawable.ic_item_diaper
        GroceryIcon.PAW -> R.drawable.ic_item_paw
        GroceryIcon.BASKET -> R.drawable.ic_item_basket
    }

/** One of the pictures above, decorative (the text next to it says what it is). */
@Composable
fun ItemIcon(@DrawableRes icon: Int, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Icon(painterResource(icon), contentDescription = null, modifier = modifier, tint = tint)
}
