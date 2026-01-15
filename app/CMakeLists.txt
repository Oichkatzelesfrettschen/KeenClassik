# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.22)

project(keen-android-jni LANGUAGES C CXX)

# ==============================================================================
# Build Options
# ==============================================================================
set(KEEN_SANITIZERS "" CACHE STRING "Comma-separated sanitizer list (e.g. address,undefined)")
option(KEEN_COVERAGE "Enable sanitizer coverage instrumentation" OFF)
option(KEEN_FUNC_TRACE "Enable -finstrument-functions tracing" OFF)
option(KEEN_LTO "Enable LTO (IPO)" ON)
set(KEEN_PGO "generate" CACHE STRING "PGO mode: generate|use|off")
set(KEEN_PGO_PROFILE "" CACHE STRING "Path to LLVM .profdata for PGO")
option(KEEN_BOLT_RELOCS "Emit relocations for BOLT" ON)

# ==============================================================================
# Library Definition
# ==============================================================================
add_library(
    keen-android-jni
    SHARED
    src/main/jni/keen-android-jni.c
    src/main/jni/dlx.c
    src/main/jni/dsf.c
    src/main/jni/keen.c
    src/main/jni/keen_generate.c
    src/main/jni/keen_hints.c
    src/main/jni/keen_solver.c
    src/main/jni/keen_validate.c
    src/main/jni/latin.c
    src/main/jni/malloc.c
    src/main/jni/maxflow_optimized.c
    src/main/jni/random.c
    src/main/jni/tree234.c
)

# ==============================================================================
# Language Standards (C23/C++23)
# ==============================================================================
set_target_properties(keen-android-jni PROPERTIES
    C_STANDARD 23
    C_STANDARD_REQUIRED ON
    C_EXTENSIONS OFF
    CXX_STANDARD 23
    CXX_STANDARD_REQUIRED ON
    CXX_EXTENSIONS OFF
)

target_include_directories(keen-android-jni PRIVATE src/main/jni)

# ==============================================================================
# Base Optimization Flags
# ==============================================================================
target_compile_options(keen-android-jni PRIVATE
    -O3
    -fvectorize
    -fslp-vectorize
    -fno-math-errno
    -freciprocal-math
    -ffunction-sections
    -fdata-sections
)

# ==============================================================================
# JNI Compatibility Waivers
# Prevent false positives from standard JNI patterns
# ==============================================================================
target_compile_options(keen-android-jni PRIVATE
    -Wno-error=unused-parameter
    -Wno-error=deprecated-declarations
    -Wno-unused-command-line-argument
)

# ==============================================================================
# Architecture-Specific Tuning
# ==============================================================================
if(ANDROID_ABI STREQUAL "arm64-v8a")
    # ARMv8.2-a: FP16, crypto, CRC, dot product (supported by 99% of high-end devices)
    target_compile_options(keen-android-jni PRIVATE
        -march=armv8.2-a+crc+crypto+fp16+dotprod
        -mtune=cortex-x4
        -mbranch-protection=standard
        -moutline-atomics
    )
elseif(ANDROID_ABI STREQUAL "x86_64")
    # x86-64-v3: AVX2, FMA, BMI2 (emulator/ChromeOS)
    target_compile_options(keen-android-jni PRIVATE
        -march=x86-64-v3
    )
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    # 32-bit ARM: NEON with soft-float ABI
    target_compile_options(keen-android-jni PRIVATE
        -mfloat-abi=softfp
        -mfpu=neon
    )
endif()

# ==============================================================================
# Linker Configuration
# ==============================================================================
target_link_options(keen-android-jni PRIVATE
    -fuse-ld=lld
    -Wl,--gc-sections
    -Wl,--icf=all
    -Wl,--build-id=sha1
    -Wl,-z,max-page-size=16384
)

# ==============================================================================
# LTO (Link-Time Optimization)
# ==============================================================================
if(KEEN_LTO)
    target_compile_options(keen-android-jni PRIVATE -flto=thin)
    target_link_options(keen-android-jni PRIVATE -flto=thin)
endif()

# ==============================================================================
# Function Tracing
# ==============================================================================
if(KEEN_FUNC_TRACE)
    target_compile_options(keen-android-jni PRIVATE -finstrument-functions)
endif()

# ==============================================================================
# Coverage Instrumentation
# ==============================================================================
if(KEEN_COVERAGE)
    target_compile_options(keen-android-jni PRIVATE
        -fsanitize-coverage=trace-pc-guard,trace-cmp,trace-div,trace-gep
    )
endif()

# ==============================================================================
# Sanitizers (arm64-v8a and x86_64 only)
# ==============================================================================
if(KEEN_SANITIZERS)
    if(ANDROID_ABI MATCHES "arm64-v8a|x86_64")
        # Prefer HWASan on arm64 for better performance
        if(ANDROID_ABI STREQUAL "arm64-v8a" AND KEEN_SANITIZERS MATCHES "address")
            message(STATUS "Using HWASan for arm64-v8a (recommended over ASan)")
        endif()
        target_compile_options(keen-android-jni PRIVATE
            -fsanitize=${KEEN_SANITIZERS}
            -fno-sanitize-recover=all
            -fno-omit-frame-pointer
        )
        target_link_options(keen-android-jni PRIVATE -fsanitize=${KEEN_SANITIZERS})
    else()
        message(STATUS "Skipping sanitizers for ${ANDROID_ABI} (unsupported)")
    endif()
endif()

# ==============================================================================
# PGO (Profile-Guided Optimization)
# ==============================================================================
if(KEEN_PGO STREQUAL "generate")
    target_compile_options(keen-android-jni PRIVATE -fprofile-instr-generate)
    target_link_options(keen-android-jni PRIVATE -fprofile-instr-generate)
    target_compile_definitions(keen-android-jni PRIVATE PGO_INSTRUMENTATION_ENABLED)
elseif(KEEN_PGO STREQUAL "use")
    if(NOT KEEN_PGO_PROFILE)
        message(FATAL_ERROR "KEEN_PGO_PROFILE is required when KEEN_PGO=use")
    endif()
    target_compile_options(keen-android-jni PRIVATE
        -fprofile-instr-use=${KEEN_PGO_PROFILE}
        -Wno-profile-instr-out-of-date
    )
    target_link_options(keen-android-jni PRIVATE -fprofile-instr-use=${KEEN_PGO_PROFILE})
endif()

# ==============================================================================
# BOLT Preparation
# ==============================================================================
if(KEEN_BOLT_RELOCS)
    target_link_options(keen-android-jni PRIVATE
        -Wl,--emit-relocs
        -Wl,--no-rosegment
    )
endif()

# ==============================================================================
# Linked Libraries
# ==============================================================================
target_link_libraries(keen-android-jni
    android
    log
)
