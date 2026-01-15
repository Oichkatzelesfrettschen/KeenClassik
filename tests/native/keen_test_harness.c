/*
 * keen_test_harness.c: Exhaustive test harness for Keen puzzle generation
 *
 * Tests Classik 3-9 (0-3) and validates clue caps and cage sizes.
 * Reports success/failure statistics and timing data.
 *
 * Build with coverage instrumentation via CMakeLists.txt.
 *
 * SPDX-License-Identifier: MIT
 */

#define _POSIX_C_SOURCE 199309L

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <stdbool.h>
#include <ctype.h>

#include "keen.h"
#include "keen_internal.h"
#include "puzzles.h"

/* Test configuration */
#define MIN_GRID_SIZE 3
#define CLASSIK_MAX_GRID_SIZE 9
#define CLASSIK_DIFFS 4      /* EASY..EXTREME */
#define PUZZLES_PER_COMBO 5  /* Multiple samples per combination */
#define MAX_ATTEMPTS 25      /* Reasonable retry budget */
#define CLASSIK_MAX_CAGE_SIZE 6

static bool clue_values_within_cap(const char* desc) {
    const char* sep = strchr(desc, ';');
    if (!sep) return false;
    const char* p = sep + 1;
    while (*p) {
        char op = *p++;
        if (!isalpha((unsigned char)op)) return false;
        if (strlen(p) < 5) return false;
        char buf[6];
        memcpy(buf, p, 5);
        buf[5] = '\0';
        int val = atoi(buf);
        if (val > (int)MAX_CLUE_VALUE) return false;
        p += 5;
        if (*p == ',') {
            p++;
        } else if (*p != '\0') {
            return false;
        }
    }
    return true;
}

static bool parse_root_indices(const char* desc, int w, int* roots) {
    const char* sep = strchr(desc, ';');
    if (!sep) return false;
    int a = w * w;
    const char* p = desc;

    for (int i = 0; i < a; i++) {
        int val = 0;
        int digits = 0;
        while (p < sep && isdigit((unsigned char)*p)) {
            val = val * 10 + (*p - '0');
            p++;
            digits++;
        }
        if (digits == 0 || val < 0 || val >= a) return false;
        roots[i] = val;
        if (i < a - 1) {
            if (*p != ',') return false;
            p++;
        }
    }
    return p == sep;
}

static bool parse_ops(const char* desc, int w, const int* roots, char* ops) {
    const char* sep = strchr(desc, ';');
    if (!sep) return false;
    int a = w * w;
    const char* p = sep + 1;

    for (int i = 0; i < a; i++) {
        if (roots[i] != i) continue;
        char op = *p++;
        if (!op) return false;
        for (int d = 0; d < 5; d++) {
            if (!isdigit((unsigned char)*p)) return false;
            p++;
        }
        ops[i] = op;
        if (*p == ',') p++;
    }
    return true;
}

static bool cage_sizes_within_limit(const int* roots, int a, int max_size, int* max_seen) {
    int max_cage = 0;
    int* counts = snewn(a, int);

    for (int i = 0; i < a; i++) counts[i] = 0;
    for (int i = 0; i < a; i++) {
        int r = roots[i];
        counts[r]++;
        if (counts[r] > max_cage) max_cage = counts[r];
    }

    sfree(counts);
    if (max_seen) *max_seen = max_cage;
    return max_cage <= max_size;
}

static bool mult_cages_within_limit(const int* roots, const char* ops, int a, int max_mul) {
    int* counts = snewn(a, int);
    for (int i = 0; i < a; i++) counts[i] = 0;
    for (int i = 0; i < a; i++) counts[roots[i]]++;

    bool ok = true;
    for (int i = 0; i < a; i++) {
        if (roots[i] == i && ops[i] == 'm' && counts[i] > max_mul) {
            ok = false;
            break;
        }
    }

    sfree(counts);
    return ok;
}

/* Difficulty names */
static const char* DIFF_NAMES[] = {
    "Easy", "Normal", "Hard", "Extreme"
};

/* Test result tracking */
typedef struct {
    int size;
    int diff;
    int puzzles;
    int attempts;
    int successes;
    int failures;
    double avg_attempts;
    double total_time_ms;
} TestResult;

/* Timer helper */
static double get_time_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return ts.tv_sec * 1000.0 + ts.tv_nsec / 1000000.0;
}

/* Test a single grid size / difficulty combination */
static TestResult test_combination(int size, int diff, int profile, int max_cage_size,
                                   int max_mul_cells, int puzzles, unsigned int base_seed) {
    TestResult result = {
        .size = size,
        .diff = diff,
        .puzzles = puzzles,
        .attempts = 0,
        .successes = 0,
        .failures = 0,
        .avg_attempts = 0.0,
        .total_time_ms = 0.0
    };

    int total_attempts = 0;
    double start_time = get_time_ms();

    for (int puzzle = 0; puzzle < puzzles; puzzle++) {
        bool success = false;
        int attempts = 0;

        while (!success && attempts < MAX_ATTEMPTS) {
            attempts++;
            total_attempts++;

            /* Create seed string from base_seed + puzzle + attempt */
            char seed_str[64];
            snprintf(seed_str, sizeof(seed_str), "test_%u_%d_%d",
                     base_seed + (unsigned int)puzzle, puzzle, attempts);

            /* Create random state */
            random_state* rs = random_new(seed_str, (int)strlen(seed_str));
            if (!rs) {
                fprintf(stderr, "ERROR: Failed to create random state\n");
                continue;
            }

            /* Set up parameters */
            game_params params = {
                .w = size,
                .diff = diff,
                .multiplication_only = 0,
                .mode_flags = 0,
                .profile = profile
            };

            /* Generate puzzle */
            char* aux = NULL;
            char* desc = new_game_desc(&params, rs, &aux, 0);

            if (desc != NULL) {
                if (!clue_values_within_cap(desc)) {
                    fprintf(stderr, "ERROR: Clue cap exceeded for size=%d diff=%d\n", size, diff);
                    sfree(desc);
                    if (aux) sfree(aux);
                    random_free(rs);
                    continue;
                }
                if (max_cage_size > 0 || max_mul_cells > 0) {
                    int a = size * size;
                    int* roots = snewn(a, int);
                    char* ops = snewn(a, char);
                    for (int z = 0; z < a; z++) ops[z] = '\0';

                    if (!parse_root_indices(desc, size, roots)) {
                        fprintf(stderr, "ERROR: Failed to parse root indices for size=%d diff=%d\n",
                                size, diff);
                        sfree(roots);
                        sfree(ops);
                        sfree(desc);
                        if (aux) sfree(aux);
                        random_free(rs);
                        continue;
                    }

                    if (max_cage_size > 0) {
                        int max_seen = 0;
                        if (!cage_sizes_within_limit(roots, a, max_cage_size, &max_seen)) {
                            fprintf(stderr,
                                    "ERROR: Cage size %d exceeds max %d for size=%d diff=%d\n",
                                    max_seen, max_cage_size, size, diff);
                            sfree(roots);
                            sfree(ops);
                            sfree(desc);
                            if (aux) sfree(aux);
                            random_free(rs);
                            continue;
                        }
                    }

                    if (max_mul_cells > 0) {
                        if (!parse_ops(desc, size, roots, ops) ||
                            !mult_cages_within_limit(roots, ops, a, max_mul_cells)) {
                            fprintf(stderr,
                                    "ERROR: Multiplication cage exceeds cap for size=%d diff=%d\n",
                                    size, diff);
                            sfree(roots);
                            sfree(ops);
                            sfree(desc);
                            if (aux) sfree(aux);
                            random_free(rs);
                            continue;
                        }
                    }

                    sfree(roots);
                    sfree(ops);
                }
                success = true;
                result.successes++;
                /* Free resources */
                sfree(desc);
                if (aux) sfree(aux);
            }

            random_free(rs);
        }

        if (!success) {
            result.failures++;
        }
        result.attempts += attempts;
    }

    result.total_time_ms = get_time_ms() - start_time;
    result.avg_attempts = puzzles > 0 ? (double)total_attempts / puzzles : 0.0;

    return result;
}

/* Print a progress bar */
static void print_progress(int current, int total) {
    int width = 50;
    int pos = (current * width) / total;
    printf("\r[");
    for (int i = 0; i < width; i++) {
        if (i < pos) printf("=");
        else if (i == pos) printf(">");
        else printf(" ");
    }
    printf("] %d/%d", current, total);
    fflush(stdout);
}

static int puzzles_for_size(int size) {
    return PUZZLES_PER_COMBO;
}

static int max_mul_cells_for_size(int w) {
    int cells = 0;
    unsigned long val = 1;

    while (val <= MAX_CLUE_VALUE / (unsigned long)w) {
        val *= (unsigned long)w;
        cells++;
    }

    return cells;
}

/* Run all tests */
static int run_profile_tests(const char* label, int profile, int min_size, int max_size,
                             int diff_count, int max_cage_size, bool enforce_mul_cap) {
    int num_combos = (max_size - min_size + 1) * diff_count;
    int result_idx = 0;
    TestResult* results = snewn(num_combos, TestResult);

    printf("Keen Puzzle Generation Test Harness (%s)\n", label);
    printf("========================================\n");
    printf("Grid sizes: %d to %d\n", min_size, max_size);
    printf("Difficulties: %d levels\n", diff_count);
    printf("Puzzles per combo: %d\n", PUZZLES_PER_COMBO);
    printf("Max attempts per puzzle: %d\n", MAX_ATTEMPTS);
    printf("Total test combinations: %d\n\n", num_combos);

    unsigned int base_seed = (unsigned int)time(NULL);
    printf("Base seed: %u\n\n", base_seed);

    double overall_start = get_time_ms();
    int total_tests = 0;
    int total_successes = 0;
    int total_failures = 0;

    for (int size = min_size; size <= max_size; size++) {
        for (int diff = 0; diff < diff_count; diff++) {
            int current = (size - min_size) * diff_count + diff;
            int puzzles = puzzles_for_size(size);
            int max_mul_cells = enforce_mul_cap ? max_mul_cells_for_size(size) : 0;
            print_progress(current, num_combos);

            results[result_idx] = test_combination(size, diff, profile, max_cage_size,
                max_mul_cells, puzzles,
                base_seed + (unsigned int)(current * 1000));

            total_tests += puzzles;
            total_successes += results[result_idx].successes;
            total_failures += results[result_idx].failures;

            result_idx++;
        }
    }

    double overall_time = get_time_ms() - overall_start;
    print_progress(num_combos, num_combos);
    printf("\n\n");

    /* Print results table */
    printf("Results Matrix (successes / puzzles per cell):\n");
    printf("%-8s", "Size");
    for (int d = 0; d < diff_count; d++) {
        printf(" %6.6s", DIFF_NAMES[d]);
    }
    printf("\n");
    printf("--------");
    for (int d = 0; d < diff_count; d++) {
        printf(" ------");
    }
    printf("\n");

    result_idx = 0;
    for (int size = min_size; size <= max_size; size++) {
        printf("%dx%-5d", size, size);
        for (int d = 0; d < diff_count; d++) {
            TestResult* r = &results[result_idx++];
            if (r->successes == r->puzzles) {
                printf("  %d/%d ", r->successes, r->puzzles);
            } else if (r->successes > 0) {
                printf("  %d/%d*", r->successes, r->puzzles);
            } else {
                printf("  FAIL ");
            }
        }
        printf("\n");
    }

    /* Print detailed results */
    printf("\n");
    printf("Detailed Results (showing avg attempts and time):\n");
    printf("%-8s %-15s %8s %8s %10s\n",
           "Size", "Difficulty", "Success", "AvgAttempts", "Time(ms)");
    printf("-------- --------------- -------- ------------ ----------\n");

    result_idx = 0;
    for (int size = min_size; size <= max_size; size++) {
        for (int d = 0; d < diff_count; d++) {
            TestResult* r = &results[result_idx++];
            const char* status = (r->successes == r->puzzles) ? "PASS" :
                                 (r->successes > 0) ? "PARTIAL" : "FAIL";
            printf("%dx%-5d %-15s %8s %12.1f %10.1f\n",
                   size, size, DIFF_NAMES[d], status,
                   r->avg_attempts, r->total_time_ms);
        }
    }

    /* Summary */
    printf("\n");
    printf("Summary\n");
    printf("=======\n");
    printf("Total puzzles attempted: %d\n", total_tests);
    printf("Successful generations:  %d (%.1f%%)\n",
           total_successes, total_tests ? (100.0 * total_successes / total_tests) : 0.0);
    printf("Failed generations:      %d (%.1f%%)\n",
           total_failures, total_tests ? (100.0 * total_failures / total_tests) : 0.0);
    printf("Total time:              %.1f ms\n", overall_time);
    printf("Avg time per puzzle:     %.1f ms\n",
           total_tests ? (overall_time / total_tests) : 0.0);

    if (total_failures > 0) {
        printf("\nWARNING: Some puzzle generations failed!\n");
    } else {
        printf("\nAll tests passed!\n");
    }

    sfree(results);
    return total_failures;
}

static int run_all_tests(void) {
    int failures = 0;
    failures += run_profile_tests("Classik", KEEN_PROFILE_CLASSIK_MODERN, MIN_GRID_SIZE,
                                  CLASSIK_MAX_GRID_SIZE, CLASSIK_DIFFS,
                                  CLASSIK_MAX_CAGE_SIZE, true);
    return failures;
}

int main(int argc, char* argv[]) {
    (void)argc;
    (void)argv;

    int failures = run_all_tests();
    return failures > 0 ? 1 : 0;
}
