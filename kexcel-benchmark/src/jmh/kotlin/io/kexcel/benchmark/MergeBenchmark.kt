package io.kexcel.benchmark

import io.kexcel.core.excel
import io.kexcel.driver.FastExcelDriver
import io.kexcel.driver.PoiDriver
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
open class MergeBenchmark {

    @Param("10000")
    var rowCount: Int = 10000

    @Param("0", "10", "100") // 0 means no merge
    var mergeInterval: Int = 0

    @Param("FastExcel", "POI")
    lateinit var driverName: String

    @Benchmark
    fun testMergePerformance() {
        val out = NullOutputStream()
        val driver = when (driverName) {
            "FastExcel" -> FastExcelDriver()
            "POI" -> PoiDriver()
            else -> throw IllegalArgumentException()
        }

        excel(out, driver) {
            sheet("MergeTest") {
                repeat(rowCount) { r ->
                    row {
                        cell(value = "Data $r-0")
                        cell(value = "Data $r-1")
                    }

                    // Perform merge based on interval
                    if (mergeInterval > 0 && r % mergeInterval == 0) {
                        // Merge current row's two cells
                        mergeCells(r, r, 0, 1)
                    }
                }
            }
        }
    }
}
