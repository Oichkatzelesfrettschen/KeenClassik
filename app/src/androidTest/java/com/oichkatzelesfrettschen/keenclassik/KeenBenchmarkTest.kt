package com.oichkatzelesfrettschen.keenclassik

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume
import org.junit.Before
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.oichkatzelesfrettschen.keenclassik.data.JniResultParser
import com.oichkatzelesfrettschen.keenclassik.data.ParseResult
import com.oichkatzelesfrettschen.keenclassik.data.PuzzleParser

@RunWith(AndroidJUnit4::class)
class KeenBenchmarkTest {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val samplePayload =
        "00,00,01,00,02,01,03,02,03;a00006,m00002,a00009,s00001,123456789"

    private val parsedPuzzle = (PuzzleParser.parse(samplePayload, 3) as? ParseResult.Success)
        ?.puzzle ?: error("Sample payload must parse for benchmarks")

    @Before
    fun assumeBenchmarkRunner() {
        val runnerName = InstrumentationRegistry.getInstrumentation()::class.java.name
        Assume.assumeTrue(
            "Benchmarks require AndroidBenchmarkRunner",
            runnerName == "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        )
    }

    @Test
    fun benchmarkParsePayload() {
        benchmarkRule.measureRepeated {
            val result = PuzzleParser.parse(samplePayload, 3)
            if (result !is ParseResult.Success) {
                error("Parse failed during benchmark")
            }
        }
    }

    @Test
    fun benchmarkJniEnvelopeParse() {
        val wrapped = "OK:$samplePayload"
        benchmarkRule.measureRepeated {
            val result = JniResultParser.parse(wrapped)
            if (result is com.oichkatzelesfrettschen.keenclassik.data.JniResult.Error) {
                error("JNI parse failed during benchmark")
            }
        }
    }

    @Test
    fun benchmarkLatinSquareValidation() {
        benchmarkRule.measureRepeated {
            PuzzleParser.isValidLatinSquare(parsedPuzzle)
        }
    }

    @Test
    fun perfMetricsHooks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val snapshot = PerfMetrics.captureMemorySnapshot()
        assertTrue(snapshot.pssKb >= 0)

        val gfxDump = PerfMetrics.dumpGfxInfo(context)
        assertTrue(gfxDump.isNotBlank())
    }

}
