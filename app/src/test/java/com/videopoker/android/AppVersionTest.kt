package com.videopoker.android

import com.videopoker.android.ui.state.GameState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AppVersionTest {

    @Test
    fun `in-app version matches BuildConfig, gradle versionName, and VERSION file`() {
        val versionFile = File("../VERSION").readText().trim()
        val gradleText = File("build.gradle.kts").readText()
        val gradleVersionName = Regex("""versionName\s*=\s*"([^"]+)"""")
            .find(gradleText)
            ?.groupValues
            ?.get(1)

        assertEquals(BuildConfig.VERSION_NAME, GameState.APP_VERSION)
        assertEquals(versionFile, GameState.APP_VERSION)
        assertEquals(gradleVersionName, GameState.APP_VERSION)
    }
}
