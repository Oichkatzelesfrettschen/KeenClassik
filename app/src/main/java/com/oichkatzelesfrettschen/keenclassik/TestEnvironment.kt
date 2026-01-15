/*
 * TestEnvironment.kt: Lightweight instrumentation detection for UI tests
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik

import android.content.Context

object TestEnvironment {
    const val INSTRUMENTATION_PREF_KEY = "keen_instrumentation"

    @Volatile
    private var cached: Boolean? = null
    @Volatile
    private var appContext: Context? = null

    @JvmStatic
    fun primeFromContext(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext?.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
        if (prefs?.getBoolean(INSTRUMENTATION_PREF_KEY, false) == true) {
            cached = true
        }
    }

    fun isInstrumentation(): Boolean {
        if (System.getProperty("keen.instrumentation") == "true") {
            cached = true
            return true
        }
        val context = appContext
        if (context != null) {
            val prefs = context.getSharedPreferences(
                context.packageName + "_preferences",
                Context.MODE_PRIVATE
            )
            if (prefs.getBoolean(INSTRUMENTATION_PREF_KEY, false)) {
                cached = true
                return true
            }
        }
        val cachedValue = cached
        if (cachedValue != null) {
            return cachedValue
        }
        val detected = try {
            Class.forName(
                "androidx.test.platform.app.InstrumentationRegistry",
                false,
                TestEnvironment::class.java.classLoader
            )
            true
        } catch (e: ClassNotFoundException) {
            try {
                val loader = Thread.currentThread().contextClassLoader
                loader?.loadClass("androidx.test.platform.app.InstrumentationRegistry")
                true
            } catch (inner: ClassNotFoundException) {
                false
            }
        }
        cached = detected
        return detected
    }

    @JvmStatic
    fun getAppContext(): Context? = appContext
}
