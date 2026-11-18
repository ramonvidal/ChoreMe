package com.choreme.core.update

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.AppUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Reading GitHub's "latest release" answer (JSON is Android's, so this runs on Robolectric). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ReleaseTest {
    private fun release(tag: String, vararg assets: Pair<String, Long>) = """
        {"tag_name": "$tag", "assets": [${assets.joinToString(",") { (name, size) -> """{"name": "$name", "size": $size, "browser_download_url": "https://example.org/$name"}""" }}]}
    """.trimIndent()

    @Test
    fun `the app's APK on a v0 tag is an update`() {
        assertEquals(
            AppUpdate(171, "0.171", "https://example.org/ChoreMe.apk", 12_000_000),
            releaseFrom(release("v0.171", "ChoreMe.aab" to 9L, "ChoreMe.apk" to 12_000_000L)),
        )
    }

    @Test
    fun `no APK or a tag that isn't ours is no update`() {
        assertNull(releaseFrom(release("v0.171", "screenshots.zip" to 3L)))
        assertNull(releaseFrom(release("nightly", "ChoreMe.apk" to 3L)))
        assertNull(releaseFrom("""{"tag_name": "v0.171"}"""))
    }
}
