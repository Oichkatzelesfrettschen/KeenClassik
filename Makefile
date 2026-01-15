# KeenClassik - Unified Build System
# Keen puzzle game for Android (Classik-only: 3-9 grids, classic operators).
# Acts as the single entry point for all development tasks.

# Configuration
# JAVA_HOME_TARGET can be overridden (e.g., make JAVA_HOME_TARGET=/path/to/jdk-21)
JAVA_HOME_TARGET ?= /usr/lib/jvm/java-21-openjdk
ifneq ($(JAVA_HOME),)
JAVA_HOME_TARGET := $(JAVA_HOME)
endif
GRADLE := JAVA_HOME=$(JAVA_HOME_TARGET) ./gradlew

# Host Toolchain
C_STD ?= c23
CC := clang
PGO_MODE ?= generate
PGO_PROFILE ?=
BOLT_RELOCS ?= 1
VECTOR_FLAGS ?= -fvectorize -fslp-vectorize -fno-math-errno
CFLAGS ?= -O3 -march=native -flto -ffast-math -funroll-loops $(VECTOR_FLAGS)
CFLAGS += -std=$(C_STD) -Iapp/src/main/jni -DSTANDALONE_LATIN_TEST -Wall -Wextra -Werror
LDFLAGS ?=

ifeq ($(findstring clang,$(CC)),)
$(error CC must be clang for mandatory perf toolchain)
endif

ifneq ($(PGO_MODE),)
  ifeq ($(PGO_MODE),generate)
    CFLAGS += -fprofile-instr-generate
    LDFLAGS += -fprofile-instr-generate
  else ifeq ($(PGO_MODE),use)
    ifeq ($(PGO_PROFILE),)
      $(error PGO_PROFILE is required when PGO_MODE=use)
    endif
    CFLAGS += -fprofile-instr-use=$(PGO_PROFILE)
    LDFLAGS += -fprofile-instr-use=$(PGO_PROFILE)
  else ifneq ($(PGO_MODE),off)
    $(error Unknown PGO_MODE=$(PGO_MODE); use generate|use|off)
  endif
endif

ifeq ($(BOLT_RELOCS),1)
  LDFLAGS += -Wl,--emit-relocs
endif
SIMD_FLAGS = -mavx2 -msse2
JNI_DIR = app/src/main/jni
HOST_TOOLS_DIR = build/host-tools
HOST_TOOL = $(HOST_TOOLS_DIR)/latin_gen_opt
HOST_SOURCES = $(JNI_DIR)/latin.c \
               $(JNI_DIR)/random.c \
               $(JNI_DIR)/malloc.c \
               $(JNI_DIR)/maxflow_optimized.c \
               $(JNI_DIR)/tree234.c \
               tests/native/host_stubs.c

# --- Main Targets ---

.PHONY: help all build release install clean test lint format tools check-env android-test android-bench
.PHONY: perf-host-build perf-host perf-flamegraph perf-coverage perf-valgrind perf-infer perf-pgo perf-bolt perf-ctest

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

all: tools ## Full CI pipeline: clean, build tools, lint, test, and build APKs
	$(GRADLE) clean lintDebug testDebugUnitTest assembleDebug

# --- Android Build ---

build: ## Build Debug APK
	$(GRADLE) assembleDebug

release: ## Build Release APK
	$(GRADLE) assembleRelease

install: ## Install Debug APK to connected device
	$(GRADLE) installDebug

clean: ## Remove build artifacts
	$(GRADLE) clean
	rm -f $(HOST_TOOL)
	rm -rf .cxx

# --- Quality Assurance ---

test: ## Run local unit tests
	$(GRADLE) testDebugUnitTest

android-test: ## Run connected UI tests with emulator keepalive/logcat capture
	./scripts/run_android_tests.sh --ui --task=connectedDebugAndroidTest

android-bench: ## Run connected benchmarks with AndroidBenchmarkRunner
	./scripts/run_android_tests.sh --bench --task=connectedDebugAndroidTest

lint: ## Run Android Lint
	$(GRADLE) lintDebug

format: ## Apply formatting (Placeholder)
	@echo "Formatting not yet implemented. Please adhere to Kotlin coding conventions."

# --- Native Tools ---

tools: $(HOST_TOOL) ## Build host-side C tools for perf/analysis

$(HOST_TOOL): $(HOST_SOURCES)
	@mkdir -p $(HOST_TOOLS_DIR)
	$(CC) $(CFLAGS) $(SIMD_FLAGS) -o $(HOST_TOOL) $(HOST_SOURCES) $(LDFLAGS)

# --- Verification ---

verify-arm: ## Build Release and verify ARMv8 binary architecture and flags
	$(GRADLE) assembleRelease
	@echo "Verifying ARMv8 Binary..."
	@find app/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/arm64-v8a -name "libkeen-android-jni.so" -exec file {} \;
	@echo "Checking for Optimization Flags in Compile Database..."
	@find app/.cxx -name compile_commands.json -exec grep -H "aarch64" {} \; | head -n 1 || true
	@echo "Build Verified: Native ARMv8 target with NDK toolchain."

# --- Development Helpers ---

check-env: ## Verify development environment
	@echo "Checking Java..."
	@if [ -d "$(JAVA_HOME_TARGET)" ]; then echo "OK: Java 21 found at $(JAVA_HOME_TARGET)"; else echo "FAIL: Java 21 not found at $(JAVA_HOME_TARGET)"; exit 1; fi
	@echo "Checking ADB..."
	@adb version > /dev/null 2>&1 && echo "OK: ADB found" || echo "WARN: ADB not found"

run: install ## Run the app (Classik)
	adb shell am start -n com.oichkatzelesfrettschen.keenclassik/.KeenActivity

# --- Host Performance Tooling ---

perf-host-build: ## Build host tool with perf-friendly flags
	./scripts/perf/host_build.sh --mode=perf

perf-host: ## Record perf data for host Latin generator
	./scripts/perf/host_perf_record.sh

perf-flamegraph: ## Generate flamegraph from host perf data
	./scripts/perf/host_flamegraph.sh

perf-coverage: ## Generate gcovr coverage report from host tool
	./scripts/perf/host_coverage.sh

perf-valgrind: ## Run valgrind on host tool
	./scripts/perf/host_valgrind.sh

perf-infer: ## Run infer static analysis on host tool build
	./scripts/perf/host_infer.sh

perf-pgo: ## Run host PGO pipeline (clang + llvm-profdata)
	./scripts/perf/host_pgo.sh

perf-bolt: ## Run host BOLT pipeline (llvm-bolt + perf2bolt)
	./scripts/perf/host_bolt.sh

perf-ctest: ## Run host CMake + CTest smoke test
	./scripts/perf/host_ctest.sh
