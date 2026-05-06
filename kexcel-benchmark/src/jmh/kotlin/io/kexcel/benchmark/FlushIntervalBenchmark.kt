package io.kexcel.benchmark

import io.kexcel.core.excel
import io.kexcel.driver.FastExcelDriver
import io.kexcel.driver.PoiDriver
import io.kexcel.driver.WorkbookOptions
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
open class FlushIntervalBenchmark {

    @Param("100", "500", "1000", "5000", "10000")
    var interval: Int = 0

    @Param("FastExcel", "POI")
    var driverName: String = ""

    private val rowCount = 50_000

    @Benchmark
    fun testFlushInterval() {
        val out = NullOutputStream()
        val options = WorkbookOptions(flushInterval = interval)
        val driver = if (driverName == "FastExcel") FastExcelDriver() else PoiDriver()
        
        excel(out, driver = driver, options = options) {
            sheet("FlushTest") {
                repeat(rowCount) { r ->
                    row {
                        repeat(5) { c ->
                            cell(value = "Data $r-$c")
                        }
                    }
                }
            }
        }
    }
}
