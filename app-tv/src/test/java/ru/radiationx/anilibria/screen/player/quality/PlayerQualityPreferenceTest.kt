package ru.radiationx.anilibria.screen.player.quality

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.radiationx.data.datasource.holders.AppPreference
import ru.radiationx.data.entity.common.PlayerQuality
import ru.radiationx.data.entity.domain.release.QualityInfo
import java.lang.reflect.Proxy

class PlayerQualityPreferenceTest {

    @Test
    fun `select highest quality within both screen dimensions`() {
        val cases = listOf(
            Triple(3840, 2160, PlayerQuality.FULLHD),
            Triple(1920, 1080, PlayerQuality.FULLHD),
            Triple(1366, 768, PlayerQuality.HD),
            Triple(1280, 720, PlayerQuality.HD),
            Triple(1920, 720, PlayerQuality.HD),
            Triple(1024, 768, PlayerQuality.SD),
            Triple(854, 480, PlayerQuality.SD),
            Triple(0, 0, PlayerQuality.SD),
        )
        cases.forEach { (width, height, expected) ->
            assertEquals("$width x $height", expected, screenPlayerQuality(width, height))
            assertEquals("$height x $width", expected, screenPlayerQuality(height, width))
        }
    }

    @Test
    fun `full HD screen falls back to highest available episode quality`() {
        val preferred = screenPlayerQuality(1920, 1080)
        assertEquals(PlayerQuality.FULLHD, QualityInfo("sd", "hd", "fhd").getActualFor(preferred))
        assertEquals(PlayerQuality.HD, QualityInfo("sd", "hd", null).getActualFor(preferred))
        assertEquals(PlayerQuality.SD, QualityInfo("sd", null, null).getActualFor(preferred))
    }

    @Test
    fun `HD screen does not select available full HD stream`() {
        val preferred = screenPlayerQuality(1280, 720)
        assertEquals(PlayerQuality.HD, QualityInfo("sd", "hd", "fhd").getActualFor(preferred))
        assertEquals(PlayerQuality.SD, QualityInfo("sd", null, "fhd").getActualFor(preferred))
    }

    @Test
    fun `automatic default does not replace an existing manual choice`() {
        val fixture = PreferenceFixture()
        val automatic = fixture.preference.withDefault(PlayerQuality.FULLHD)
        assertEquals(PlayerQuality.FULLHD, automatic.value)
        assertEquals(PlayerQuality.SD, fixture.preference.value)

        fixture.save(PlayerQuality.SD)
        assertEquals(PlayerQuality.SD, automatic.value)
        assertEquals(PlayerQuality.SD, fixture.preference.withDefault(PlayerQuality.HD).value)
    }

    @Test(timeout = 5000)
    fun `manual SD emits a change even when underlying default was SD`() = runBlocking {
        val fixture = PreferenceFixture()
        val automatic = fixture.preference.withDefault(PlayerQuality.FULLHD)
        val values = mutableListOf<PlayerQuality>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            automatic.take(2).toList(values)
        }
        fixture.save(PlayerQuality.SD)
        job.join()
        assertEquals(listOf(PlayerQuality.FULLHD, PlayerQuality.SD), values)
    }

    private class PreferenceFixture {
        private var stored: String? = null
        private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()
        private val preferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            when (method.name) {
                "contains" -> stored != null
                "getString" -> stored ?: args!![1]
                "registerOnSharedPreferenceChangeListener" -> {
                    listeners.add(args!![0] as SharedPreferences.OnSharedPreferenceChangeListener)
                    null
                }
                else -> error("Unexpected SharedPreferences call: ${method.name}")
            }
        } as SharedPreferences

        val preference = AppPreference(
            key = "quality",
            sharedPreferences = preferences,
            get = { key -> PlayerQuality.valueOf(getString(key, "SD")!!) },
            set = { key, value -> putString(key, value.name) },
        )

        fun save(quality: PlayerQuality) {
            stored = quality.name
            listeners.forEach { it.onSharedPreferenceChanged(preferences, "quality") }
        }
    }
}
