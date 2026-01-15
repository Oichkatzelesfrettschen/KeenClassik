/*
 * PerfTrace.kt: Thin wrapper around android.os.Trace for Perfetto/systrace capture
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 *
 * Usage: PerfTrace.section("PuzzleGeneration") { ... }
 * Capture: adb shell perfetto -o /data/misc/perfetto-traces/keen.pftrace -t 10s \
 *          -c - <<< 'buffers: { size_kb: 65536 } data_sources: { config { name: "linux.ftrace" } }'
 */

package com.oichkatzelesfrettschen.keenclassik.perf

import android.os.Build
import android.os.Trace

object PerfTrace {
    /**
     * Execute a block within a named trace section.
     * Uses android.os.Trace for Perfetto/systrace capture.
     * Safely no-ops on unit tests where Trace is not available.
     */
    inline fun <T> section(name: String, block: () -> T): T {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Trace.beginSection(name)
            }
            try {
                block()
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Trace.endSection()
                }
            }
        } catch (e: RuntimeException) {
            // JVM unit tests: Trace.beginSection throws when android.os.Trace is mocked
            block()
        }
    }

    /**
     * Async trace event - use for operations that may span multiple threads.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun beginAsyncSection(name: String, cookie: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.beginAsyncSection(name, cookie)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun endAsyncSection(name: String, cookie: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.endAsyncSection(name, cookie)
        }
    }

    /**
     * Counter trace for tracking numeric values over time.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun setCounter(name: String, value: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.setCounter(name, value)
        }
    }
}
