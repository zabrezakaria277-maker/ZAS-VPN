package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.handler.AngConfigManager
import java.text.Normalizer

internal data class ServerRowUiModel(
    val guid: String,
    val profile: ProfileItem,
    val remarks: String,
    val statistics: String,
    val typeDescription: String,
    val testDelayMillis: Long,
    val subscriptionBadge: String,
)

internal data class ServerGroupUiState(
    val servers: List<ServersCache> = emptyList(),
    val rows: List<ServerRowUiModel> = emptyList(),
)

internal fun buildServerRowUiModel(
    server: ServersCache,
    subscriptionRemarks: String,
): ServerRowUiModel {
    val profile = server.profile
    val locked = ZasLock.isLocked(profile.remarks)
    return ServerRowUiModel(
        guid = server.guid,
        profile = profile,
        remarks = if (locked) zasWithFlag(profile.remarks) else profile.remarks,
        statistics = if (locked) ""
        else profile.description.nullIfBlank()
            ?: AngConfigManager.generateDescription(profile),
        typeDescription = if (locked) "Serveur protégé"
        else serverProtocolDescription(profile),
        testDelayMillis = server.testDelayMillis,
        subscriptionBadge = subscriptionRemarks.firstOrNull()?.toString().orEmpty(),
    )
}

private val ZAS_COUNTRY_NAMES = listOf(
    "ALLEMAGNE" to "DE", "GERMANY" to "DE",
    "FRANCE" to "FR",
    "TURQUIE" to "TR", "TURKEY" to "TR",
    "PAYS-BAS" to "NL", "PAYS BAS" to "NL", "HOLLANDE" to "NL", "NETHERLANDS" to "NL",
    "ROYAUME-UNI" to "GB", "ROYAUME UNI" to "GB", "ANGLETERRE" to "GB", "UNITED KINGDOM" to "GB",
    "ETATS-UNIS" to "US", "ETATS UNIS" to "US", "AMERIQUE" to "US", "UNITED STATES" to "US",
    "SINGAPOUR" to "SG", "SINGAPORE" to "SG",
    "CANADA" to "CA",
    "JAPON" to "JP", "JAPAN" to "JP",
    "INDE" to "IN", "INDIA" to "IN",
    "ITALIE" to "IT", "ITALY" to "IT",
    "ESPAGNE" to "ES", "SPAIN" to "ES",
    "SUISSE" to "CH", "SWITZERLAND" to "CH",
    "SUEDE" to "SE", "SWEDEN" to "SE",
    "FINLANDE" to "FI", "FINLAND" to "FI",
    "POLOGNE" to "PL", "POLAND" to "PL",
    "RUSSIE" to "RU", "RUSSIA" to "RU",
    "BRESIL" to "BR", "BRAZIL" to "BR",
    "EMIRATS" to "AE", "DUBAI" to "AE",
    "HONG KONG" to "HK", "HONGKONG" to "HK",
)

private val ZAS_COUNTRY_CODES = setOf(
    "DE", "FR", "TR", "NL", "GB", "UK", "US", "SG", "CA", "JP", "IN", "IT", "ES",
    "CH", "SE", "FI", "PL", "RU", "BR", "AE", "HK",
)

private fun zasFlagOf(code: String): String {
    val c = if (code == "UK") "GB" else code
    if (c.length != 2) return ""
    val sb = StringBuilder()
    for (ch in c) sb.appendCodePoint(0x1F1E6 + (ch - 'A'))
    return sb.toString()
}

private fun zasDetectFlag(name: String): String {
    val up = Normalizer.normalize(name.uppercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    for ((label, code) in ZAS_COUNTRY_NAMES) {
        if (up.contains(label)) return zasFlagOf(code)
    }
    val tokens = up.split(Regex("[^A-Z]+")).filter { it.length == 2 }
    for (t in tokens) {
        if (t in ZAS_COUNTRY_CODES) return zasFlagOf(t)
    }
    return ""
}

private fun zasWithFlag(remarks: String): String {
    val hasFlag = remarks.codePoints().anyMatch { it in 0x1F1E6..0x1F1FF }
    if (hasFlag) return remarks
    val flag = zasDetectFlag(remarks)
    if (flag.isEmpty()) return remarks
    val rest = remarks.removePrefix("🔒").trim()
    return "🔒 $flag $rest"
}

private fun serverProtocolDescription(profile: ProfileItem): String {
    if (profile.configType.isComplexType()) return profile.configType.name
    val parts = mutableListOf(profile.configType.name)
    profile.network?.let { network ->
        if (network.isNotBlank() && !network.equals("tcp", ignoreCase = true)) {
            parts.add(network)
        }
    }
    profile.security?.let { security ->
        if (security.isNotBlank()) {
            parts.add(
                if (profile.insecure == true && security.equals("tls", ignoreCase = true)) {
                    "$security insecure"
                } else {
                    security
                }
            )
        }
    }
    return parts.joinToString(" / ")
}
