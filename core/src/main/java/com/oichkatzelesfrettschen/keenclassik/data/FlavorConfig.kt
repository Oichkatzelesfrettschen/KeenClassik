/*
 * FlavorConfig.kt: Flavor capability surface for mode/size gating
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 */

package com.oichkatzelesfrettschen.keenclassik.data

interface FlavorConfig {
    val fullModeSet: Boolean
    val minGridSize: Int
    val maxGridSize: Int
}

object FlavorConfigProvider {
    @Volatile
    private var config: FlavorConfig = object : FlavorConfig {
        override val fullModeSet: Boolean = false
        override val minGridSize: Int = 3
        override val maxGridSize: Int = 9
    }

    @JvmStatic
    fun get(): FlavorConfig = config

    @JvmStatic
    fun set(newConfig: FlavorConfig) {
        config = newConfig
    }
}
