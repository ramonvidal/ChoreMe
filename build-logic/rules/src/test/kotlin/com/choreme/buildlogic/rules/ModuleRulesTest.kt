package com.choreme.buildlogic.rules

import com.choreme.buildlogic.rules.ModuleRules.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleRulesTest {
    private fun module(path: String, vararg dependsOn: String, usesAndroid: Boolean = path != ":core:model") =
        Module(path, usesAndroid, dependsOn.toSet())

    private val valid = listOf(
        module(":core"),
        module(":feature"),
        module(":core:model"),
        module(":core:data", ":core:model"),
        module(":core:ui", ":core:model"),
        module(":feature:groceries", ":core:model", ":core:data", ":core:ui"),
        module(":app", ":core:data", ":feature:groceries"),
    )

    private fun violationsWith(vararg extra: Module) =
        ModuleRules.violations(valid.filter { m -> extra.none { it.path == m.path } } + extra)

    @Test
    fun `the intended structure passes`() {
        assertEquals(emptyList<String>(), ModuleRules.violations(valid))
    }

    @Test
    fun `features cannot depend on each other`() {
        val violations = violationsWith(module(":feature:chores", ":feature:groceries"))
        assertEquals(1, violations.size)
        assertTrue(violations.single().startsWith(":feature:chores -> :feature:groceries"))
    }

    @Test
    fun `nothing depends on app`() {
        assertTrue(violationsWith(module(":feature:chores", ":app")).single().contains("nothing may depend on :app"))
    }

    @Test
    fun `core cannot depend on features or on unlisted core modules`() {
        assertEquals(1, violationsWith(module(":core:data", ":core:model", ":feature:groceries")).size)
        assertEquals(1, violationsWith(module(":core:ui", ":core:model", ":core:data")).size)
        assertEquals(1, violationsWith(module(":core:model", ":core:ui", usesAndroid = false)).size)
    }

    @Test
    fun `the model module must stay plain Kotlin`() {
        assertTrue(violationsWith(module(":core:model", usesAndroid = true)).single().contains("plain Kotlin"))
    }

    @Test
    fun `new modules must fit the structure`() {
        assertTrue(violationsWith(module(":core:notifications")).single().contains("CORE_DEPENDENCIES"))
        assertTrue(violationsWith(module(":utils")).single().contains("outside the structure"))
        assertTrue(violationsWith(module(":feature:chores:extra")).single().contains("outside the structure"))
    }
}
