package com.choreme.core.model.chore

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ChoreChoicesTest {
    private val today = LocalDate.of(2026, 10, 31)
    private fun saved(title: String) = SavedChore(title, title, "mom")

    @Test
    fun `typed chores land in the right group, in any of the family's languages`() {
        assertEquals(ChoreTemplate.ORGANIC_WASTE, guessTemplate("Lixo Bio"))
        assertEquals(ChoreTemplate.TRASH, guessTemplate("Lixo geral"))
        assertEquals(ChoreTemplate.RECYCLING, guessTemplate("Lixo plástico"))
        assertEquals(ChoreTemplate.TAKE_MEDICINE, guessTemplate("Tomar remédio Alinne"))
        assertEquals(ChoreTemplate.FEED_PETS, guessTemplate("Feed fish"))
        assertEquals(ChoreTemplate.TIDY_UP, guessTemplate("Arrumar sala"))
        assertEquals(ChoreTemplate.RECYCLING, guessTemplate("Altpapier rausbringen"))
        // Only the first words count: an email about the bathroom isn't cleaning.
        assertEquals(null, guessTemplate("Mandar email dono da casa sobre O2 e Boxen do banheiro"))
        assertEquals(null, guessTemplate("Pegar passaporte na Rathaus"))
        assertEquals(ChoreGroup.BINS, ChoreChoice.Saved(saved("Lixo Bio")).group)
        assertEquals(null, ChoreChoice.Saved(saved("Pegar passaporte na Rathaus")).group)
    }

    @Test
    fun `a saved chore keeps the group it was saved with`() {
        val choice = ChoreChoice.Saved(SavedChore("x", "Something odd", "mom", template = ChoreTemplate.FEED_PETS))
        assertEquals(ChoreGroup.CARE, choice.group)
    }

    @Test
    fun `most used counts what was done and what's on the list`() {
        val medicine = saved("Tomar remedio")
        val choices = choreChoices(listOf(medicine))
        val chores = listOf(
            Chore("1", "Tomar remedio", dueDate = today, doneCount = 9),
            Chore("2", "Wash dishes", ChoreTemplate.DISHES, dueDate = today, doneCount = 2),
            Chore("3", "Vacuum", ChoreTemplate.VACUUM, dueDate = today),
        )
        assertEquals(
            listOf(ChoreChoice.Saved(medicine), ChoreChoice.BuiltIn(ChoreTemplate.DISHES), ChoreChoice.BuiltIn(ChoreTemplate.VACUUM)),
            mostUsed(choices, chores),
        )
    }

    @Test
    fun `search finds by name and by kind of chore`() {
        val bio = saved("Lixo Bio")
        val choices = choreChoices(listOf(bio, saved("Feed fish")))
        val titles = { c: ChoreChoice -> if (c is ChoreChoice.Saved) c.saved.title else c.template.name.lowercase().replace('_', ' ') }
        val found = searchChoices("lix", choices, titles)
        assert(ChoreChoice.Saved(bio) in found)
        assert(ChoreChoice.BuiltIn(ChoreTemplate.TRASH) in found)
        assertEquals(listOf(ChoreChoice.Saved(saved("Feed fish"))), searchChoices("fish", choices, titles).filterIsInstance<ChoreChoice.Saved>())
        assertEquals(emptyList<ChoreChoice>(), searchChoices("x", choices, titles))
    }
}
