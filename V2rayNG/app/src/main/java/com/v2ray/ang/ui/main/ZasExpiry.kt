package com.v2ray.ang.ui.main

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ZasExpiry {
    // true : l'application exige un code valide (mets false pour tes tests)
    const val REQUIRE_CODE = true

    private const val PREFS = "zas_prefs"
    private const val K_REM = "zas_rem_ms"
    private const val K_ELAPSED = "zas_elapsed"
    private const val K_WALL = "zas_wall"
    private const val K_CODE = "zas_code"

    private var appContext: Context? = null

    var tick by mutableStateOf(0)
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun code(c: Context): String = prefs(c).getString(K_CODE, "") ?: ""

    fun saveFromServer(c: Context, code: String, remainingMs: Long) {
        prefs(c).edit()
            .putString(K_CODE, code)
            .putLong(K_REM, remainingMs)
            .putLong(K_ELAPSED, SystemClock.elapsedRealtime())
            .putLong(K_WALL, System.currentTimeMillis())
            .apply()
        tick++
    }

    fun expireNow(c: Context) {
        prefs(c).edit()
            .putLong(K_REM, 0L)
            .putLong(K_ELAPSED, SystemClock.elapsedRealtime())
            .putLong(K_WALL, System.currentTimeMillis())
            .apply()
        tick++
    }

    // null : aucun code n'a jamais été activé
    fun remainingMs(c: Context): Long? {
        val p = prefs(c)
        if (!p.contains(K_REM)) return null
        val base = p.getLong(K_REM, 0L)
        val e0 = p.getLong(K_ELAPSED, 0L)
        val w0 = p.getLong(K_WALL, 0L)
        val e = SystemClock.elapsedRealtime()
        val passed = if (e >= e0) e - e0 else (System.currentTimeMillis() - w0).coerceAtLeast(0L)
        return (base - passed).coerceAtLeast(0L)
    }

    fun hasActive(c: Context): Boolean = (remainingMs(c) ?: 0L) > 0L

    fun isExpired(c: Context): Boolean = remainingMs(c)?.let { it <= 0L } == true

    fun canUse(c: Context): Boolean = if (REQUIRE_CODE) hasActive(c) else !isExpired(c)

    // Compatibilité avec l'en-tête : expiration absolue (0 = aucun abonnement)
    fun get(context: Context): Long {
        val r = remainingMs(context) ?: return 0L
        return System.currentTimeMillis() + r
    }

    // Les anciens fichiers .zas n'accordent plus d'accès : seul un code du panel le fait
    fun remember(pkg: ZasPackage) {
    }
}
