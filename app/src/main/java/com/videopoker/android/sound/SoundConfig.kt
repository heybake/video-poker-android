package com.videopoker.android.sound

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

enum class SoundGroup(val displayName: String) {
    BUTTONS("Buttons"),
    DEALING("Dealing"),
    HANDS("Hands"),
    WINS("Wins"),
    OTHER("Other")
}

enum class SoundEffect(
    val id: String,
    val assetName: String,
    val displayName: String,
    val description: String,
    val group: SoundGroup
) {
    BET("bet", "bet.mp3", "Bet", "Bet one or bet max", SoundGroup.BUTTONS),
    HOLD("hold", "hold.mp3", "Hold", "A card is held", SoundGroup.BUTTONS),
    RELEASE("release", "release.mp3", "Release", "A held card is let go", SoundGroup.BUTTONS),
    OPTION_BUTTON("option_button", "option_button.mp3", "Option button", "Options, Help, See Pays, Speed & menus", SoundGroup.BUTTONS),
    DEAL("deal", "deal.mp3", "Deal", "The five cards come out", SoundGroup.DEALING),
    DRAW("draw", "draw.mp3", "Draw", "Replacement cards come out", SoundGroup.DEALING),
    MADE_HAND("made_hand", "made_hand.mp3", "Made hand", "The deal already pays", SoundGroup.HANDS),
    WIN_SMALL("win_small", "win_small.mp3", "Small win", "Pays under 3 times the bet", SoundGroup.WINS),
    WIN_MEDIUM("win_medium", "win_medium.mp3", "Medium win", "3 to 8 times the bet", SoundGroup.WINS),
    WIN_BIG("win_big", "win_big.mp3", "Big win", "8 times the bet or more", SoundGroup.WINS),
    WIN_ROYAL("win_royal", "win_royal.mp3", "Royal flush", "The jackpot hand", SoundGroup.WINS),
    COUNT_TICK("count_tick", "count_tick.mp3", "Count-up tick", "The credit meter counting up", SoundGroup.OTHER),
    NO_WIN("no_win", "no_win.mp3", "No win", "A losing hand", SoundGroup.OTHER),
    ADD_MONEY("add_money", "add_money.mp3", "Add money", "Inserting cash into the machine", SoundGroup.OTHER),
    COIN_SIZE("coin_size", "coin_size.mp3", "Coin size", "Changing coin denomination", SoundGroup.OTHER),
    CASH_OUT("cash_out", "cash_out.mp3", "Cash out", "Cash out voucher", SoundGroup.OTHER);

    companion object {
        fun fromId(id: String?): SoundEffect? =
            entries.firstOrNull { it.id == id }
    }
}

enum class SoundPack(val id: String, val displayName: String) {
    CLASSIC("classic", "Classic"),
    KNOCK("knock", "Knock"),
    BROOK("brook", "Brook"),
    VOWS("vows", "Vows"),
    NIGHTFALL("nightfall", "Nightfall"),
    ELATION("elation", "Elation"),
    VAUDEVILLE("vaudeville", "Vaudeville"),
    FAITH("faith", "Faith"),
    JERSEY("jersey", "Jersey"),
    FENWAY("fenway", "Fenway"),
    OMEN("omen", "Omen"),
    AUTUMN("autumn", "Autumn"),
    SYNTH("synth", "Synth"),
    TEXAS("texas", "Texas"),
    BARSTOOL("barstool", "Barstool"),
    ACES("aces", "Aces"),
    MEMORY("memory", "Memory");

    fun next(): SoundPack = entries[(ordinal + 1) % entries.size]

    fun previous(): SoundPack = entries[(ordinal - 1 + entries.size) % entries.size]

    companion object {
        fun fromId(id: String?): SoundPack =
            entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

data class SoundConfig(
    val perSoundPacks: Map<SoundEffect, SoundPack> = emptyMap(),
    val presetName: String = ""
) {
    fun packFor(effect: SoundEffect): SoundPack =
        perSoundPacks[effect] ?: SoundPack.CLASSIC

    fun uniformPack(): SoundPack? {
        val first = packFor(SoundEffect.entries.first())
        return if (SoundEffect.entries.all { packFor(it) == first }) first else null
    }

    fun dominantPack(): SoundPack {
        val counts = mutableMapOf<SoundPack, Int>()
        for (effect in SoundEffect.entries) {
            val pack = packFor(effect)
            counts[pack] = (counts[pack] ?: 0) + 1
        }
        return counts.maxByOrNull { it.value }?.key ?: SoundPack.CLASSIC
    }

    fun summaryText(): String {
        val uni = uniformPack()
        if (uni != null) return uni.displayName
        return presetName.ifBlank { "Custom mix" }
    }

    fun withAll(pack: SoundPack): SoundConfig {
        val updated = if (pack == SoundPack.CLASSIC) {
            emptyMap()
        } else {
            SoundEffect.entries.associateWith { pack }
        }
        return SoundConfig(perSoundPacks = updated, presetName = "")
    }

    fun withEffect(effect: SoundEffect, pack: SoundPack): SoundConfig {
        val updated = SoundEffect.entries.associateWith { packFor(it) }.toMutableMap()
        updated[effect] = pack
        val normalized = if (updated.values.all { it == SoundPack.CLASSIC }) emptyMap() else updated
        return SoundConfig(perSoundPacks = normalized, presetName = "")
    }

    fun toJson(pretty: Boolean = false, overrideName: String? = null): String {
        val root = JsonObject()
        root.addProperty("kind", PRESET_KIND)
        root.addProperty("version", 1)
        val effectiveName = overrideName?.trim() ?: presetName
        root.addProperty("name", effectiveName.ifBlank { summaryText() })
        val soundsObj = JsonObject()
        for (effect in SoundEffect.entries) {
            soundsObj.addProperty(effect.id, packFor(effect).id)
        }
        root.add("sounds", soundsObj)
        return if (pretty) {
            GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n"
        } else {
            root.toString()
        }
    }

    companion object {
        const val PRESET_KIND = "vp_sound_preset"

        val SAMPLE_HAND_SEQUENCE: List<Pair<SoundEffect, Long>> = listOf(
            SoundEffect.BET to 0L,
            SoundEffect.DEAL to 500L,
            SoundEffect.MADE_HAND to 1100L,
            SoundEffect.HOLD to 1900L,
            SoundEffect.HOLD to 2200L,
            SoundEffect.RELEASE to 2500L,
            SoundEffect.DRAW to 3300L,
            SoundEffect.WIN_MEDIUM to 4100L,
            SoundEffect.COUNT_TICK to 4600L,
            SoundEffect.COUNT_TICK to 4680L,
            SoundEffect.COUNT_TICK to 4760L,
            SoundEffect.COUNT_TICK to 4840L
        )

        const val SAMPLE_HAND_DURATION_MS: Long = 5200L

        fun assetPathFor(pack: SoundPack, effect: SoundEffect): String =
            "sounds/${pack.id}/${effect.assetName}"

        fun fromJson(raw: String?, fallbackName: String = ""): SoundConfig {
            if (raw.isNullOrBlank()) return SoundConfig()
            return try {
                val obj = JsonParser.parseString(raw).asJsonObject
                val mapObj = obj.get("sounds")?.takeIf { it.isJsonObject }?.asJsonObject
                    ?: obj.get("map")?.takeIf { it.isJsonObject }?.asJsonObject
                    ?: return SoundConfig()
                val validPresent = SoundEffect.entries.mapNotNull { eff ->
                    if (!mapObj.has(eff.id)) return@mapNotNull null
                    val rawId = mapObj.get(eff.id)?.takeIf { it.isJsonPrimitive }?.asString
                    rawId?.takeIf { id -> SoundPack.entries.any { it.id == id } }?.let { SoundPack.fromId(it) }
                }
                val missingFallback = validPresent
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key ?: SoundPack.CLASSIC
                val parsedMap = mutableMapOf<SoundEffect, SoundPack>()
                for (effect in SoundEffect.entries) {
                    if (!mapObj.has(effect.id)) {
                        parsedMap[effect] = missingFallback
                    } else {
                        val packId = mapObj.get(effect.id)?.takeIf { it.isJsonPrimitive }?.asString
                        parsedMap[effect] = SoundPack.fromId(packId)
                    }
                }
                val normalized = if (parsedMap.values.all { it == SoundPack.CLASSIC }) emptyMap() else parsedMap
                val rawName = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: obj.get("presetName")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: fallbackName
                val cleanName = if (normalized.isEmpty() && rawName == SoundPack.CLASSIC.displayName) {
                    ""
                } else {
                    rawName.trim()
                }
                SoundConfig(perSoundPacks = normalized, presetName = cleanName)
            } catch (_: Exception) {
                SoundConfig()
            }
        }
    }
}
