package com.choreme.core.ui

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.choreme.core.model.chore.ChoreTemplate
import java.util.Locale

/** The ready-made chore's name, in the phone's language. */
@get:StringRes
val ChoreTemplate.title: Int
    get() = when (this) {
        ChoreTemplate.DISHES -> R.string.template_dishes
        ChoreTemplate.LAUNDRY -> R.string.template_laundry
        ChoreTemplate.FOLD_CLOTHES -> R.string.template_fold_clothes
        ChoreTemplate.CLEAN_KITCHEN -> R.string.template_clean_kitchen
        ChoreTemplate.CLEAN_BATHROOM -> R.string.template_clean_bathroom
        ChoreTemplate.TRASH -> R.string.template_trash
        ChoreTemplate.VACUUM -> R.string.template_vacuum
        ChoreTemplate.MAKE_BEDS -> R.string.template_make_beds
        ChoreTemplate.WATER_PLANTS -> R.string.template_water_plants
        ChoreTemplate.GROCERIES -> R.string.template_groceries
        ChoreTemplate.TIDY_UP -> R.string.template_tidy_up
        ChoreTemplate.ORGANIC_WASTE -> R.string.template_organic_waste
        ChoreTemplate.RECYCLING -> R.string.template_recycling
        ChoreTemplate.FEED_PETS -> R.string.template_feed_pets
        ChoreTemplate.TAKE_MEDICINE -> R.string.template_take_medicine
        ChoreTemplate.OTHER -> R.string.template_other
    }

/**
 * A chore's name is saved as text, in whatever language the phone that added it showed. A
 * ready-made one ("Clean kitchen", "Limpar a cozinha") is shown in this phone's language
 * instead; a name someone typed stays exactly as typed.
 */
object ChoreNames {
    private val LANGUAGES = listOf(Locale.ENGLISH, Locale.forLanguageTag("pt-BR"))

    @Volatile
    private var known: Map<String, ChoreTemplate>? = null

    /** The ready-made chore [title] is the name of, in any of the app's languages. */
    fun templateNamed(context: Context, title: String): ChoreTemplate? {
        val names = known ?: buildMap {
            for (locale in LANGUAGES) {
                val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) })
                for (template in ChoreTemplate.entries) {
                    if (template != ChoreTemplate.OTHER) put(key(localized.getString(template.title)), template)
                }
            }
        }.also { known = it }
        return names[key(title)]
    }

    /** [title] as this phone should show it. */
    fun shown(context: Context, title: String): String =
        templateNamed(context, title)?.let { context.getString(it.title) } ?: title

    private fun key(name: String) = name.trim().lowercase()
}

/** [title] in this phone's language when it's a ready-made chore's name (see [ChoreNames]). */
@Composable
fun choreTitle(title: String): String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(title, configuration) { ChoreNames.shown(context, title) }
}
