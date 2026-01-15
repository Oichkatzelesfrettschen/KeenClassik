/*
 * jni_error_codes.h: Structured error codes for JNI boundary
 *
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2024-2025 KeenKenning Contributors
 *
 * Defines error codes returned from native puzzle generation.
 * The JNI layer returns strings in the format:
 *   Success: "OK:payload_data"
 *   Error:   "ERR:code:message"
 *
 * This allows the Kotlin layer to parse responses into the
 * PuzzleGenerationResult sealed hierarchy with proper context.
 */

#ifndef JNI_ERROR_CODES_H
#define JNI_ERROR_CODES_H

/* Success prefix */
#define JNI_PREFIX_OK "OK:"
#define JNI_PREFIX_ERR "ERR:"

/* Error codes - must match JniErrorCode enum in Kotlin */
#define JNI_ERR_NONE 0
#define JNI_ERR_GRID_SIZE 1       /* Grid size mismatch */
#define JNI_ERR_GENERATION_FAIL 2 /* Native generation returned NULL */
#define JNI_ERR_MEMORY 3          /* Memory allocation failed */
#define JNI_ERR_INVALID_PARAMS 4  /* Invalid parameters (size, diff, etc.) */
#define JNI_ERR_CLUE_GENERATION 5 /* Failed to generate valid clues */
#define JNI_ERR_INVALID_MODES 6   /* Incompatible mode flags */
#define JNI_ERR_SIZE_LIMIT 7      /* Grid size too large for mode */

/* Helper macro for error string formatting */
#define JNI_ERR_FMT "ERR:%d:%s"

/* Max error message length */
#define JNI_ERR_MSG_MAX 256

#endif /* JNI_ERROR_CODES_H */
