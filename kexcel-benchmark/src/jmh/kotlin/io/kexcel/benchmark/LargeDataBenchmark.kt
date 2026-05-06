package io.kexcel.benchmark

import io.kexcel.core.excel
import io.kexcel.driver.FastExcelDriver
import io.kexcel.driver.PoiDriver
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@Fork(value = 1, jvmArgs = ["-Xmx512m", "-XX:+UseG1GC"])
open class LargeDataBenchmark {

    @Param("100000", "1000000")
    var rowCount: Int = 0

    @Benchmark
    fun fastExcelDriver() {
        val out = NullOutputStream()
        excel(out, driver = FastExcelDriver()) {
            sheet("LargeSheet") {
                repeat(rowCount) { r ->
                    row {
                        cell(value = "Row $r")
                        cell(value = r)
                        cell(value = r % 2 == 0)
                    }
                }
            }
        }
    }

    @Benchmark
    fun poiDriver() {
        val out = NullOutputStream()
        excel(out, driver = PoiDriver()) {
            sheet("LargeSheet") {
                repeat(rowCount) { r ->
                    row {
                        cell(value = "Row $r")
                        cell(value = r)
                        cell(value = r % 2 == 0)
                    }
                }
            }
        }
    }
}
