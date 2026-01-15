/*
 * GridValidator.kt: Abstraction for grid validation (native or test fakes)
 *
 * SPDX-License-Identifier: MIT
 */

package com.oichkatzelesfrettschen.keenclassik.data

interface GridValidator {
    fun validateGrid(
        size: Int,
        grid: IntArray,
        dsf: IntArray,
        clues: LongArray,
        modeFlags: Int
    ): IntArray?
}

object NativeGridValidator : GridValidator {
    override fun validateGrid(
        size: Int,
        grid: IntArray,
        dsf: IntArray,
        clues: LongArray,
        modeFlags: Int
    ): IntArray? = KeenValidator.validateGrid(size, grid, dsf, clues, modeFlags)
}
