package com.v2ray.ang.ui.main

import android.content.Context

object ZasExpiry {
    private const val PREFS = "zas_prefs"
    private const val KEY = "zas_expires_at"
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY, 0L)

    fun isExpired(context: Context): Boolean {
        val e = get(context)
        return e > 0 && System.currentTimeMillis() >= e
    }

    fun remember(pkg: ZasPackage) {
        val ctx = appContext ?: return
        val current = get(ctx)
        if (pkg.expiresAt == 0L || current == 0L || pkg.expiresAt > current) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY, pkg.expiresAt).apply()
        }
    }
}
