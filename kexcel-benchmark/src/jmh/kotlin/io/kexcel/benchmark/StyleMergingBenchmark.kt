package io.kexcel.benchmark

import io.kexcel.core.excel
import io.kexcel.driver.FastExcelDriver
import io.kexcel.driver.PoiDriver
import io.kexcel.style.ExcelFont
import io.kexcel.style.ExcelStyle
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
open class StyleMergingBenchmark {

    @Param("FastExcel", "POI")
    var driverName: String = ""

    private val rowCount = 20_000
    private val wbStyle = ExcelStyle(font = ExcelFont(bold = true, size = 12))
    private val cellStyle = ExcelStyle(font = ExcelFont(color = "#FF0000"))

    private fun getDriver() = if (driverName == "FastExcel") FastExcelDriver() else PoiDriver()

    @Benchmark
    fun noStyle() {
        val out = NullOutputStream()
        excel(out, driver = getDriver()) {
            sheet("NoStyle") {
                repeat(rowCount) {
                    row { cell(value = "Data") }
                }
            }
        }
    }

    @Benchmark
    fun inheritedStyle() {
        val out = NullOutputStream()
        excel(out, driver = getDriver(), defaultStyle = wbStyle) {
            sheet("Inherited") {
                repeat(rowCount) {
                    row { cell(value = "Data") }
                }
            }
        }
    }

    @Benchmark
    fun individualCellStyle() {
        val out = NullOutputStream()
        excel(out, driver = getDriver()) {
            sheet("Individual") {
                repeat(rowCount) {
                    row { cell(value = "Data", style = cellStyle) }
                }
            }
        }
    }
}
