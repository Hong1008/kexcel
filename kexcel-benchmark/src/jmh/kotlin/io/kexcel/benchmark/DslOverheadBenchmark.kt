package io.kexcel.benchmark

import io.kexcel.core.excel
import io.kexcel.driver.FastExcelDriver
import io.kexcel.driver.PoiDriver
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.dhatim.fastexcel.Workbook
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
open class DslOverheadBenchmark {

    private val rowCount = 10_000
    private val colCount = 10

    @Benchmark
    fun nativeFastExcel() {
        val out = NullOutputStream()
        val workbook = Workbook(out, "Native", "1.0")
        val worksheet = workbook.newWorksheet("Sheet1")

        for (r in 0 until rowCount) {
            for (c in 0 until colCount) {
                worksheet.value(r, c, "Data $r-$c")
            }
            if (r > 0 && r % 1000 == 0) {
                worksheet.flush()
            }
        }
        workbook.finish()
    }

    @Benchmark
    fun kexcelDsl() {
        val out = NullOutputStream()
        excel(out, driver = FastExcelDriver()) {
            sheet("Sheet1") {
                repeat(rowCount) { r ->
                    row {
                        repeat(colCount) { c ->
                            cell(value = "Data $r-$c")
                        }
                    }
                }
            }
        }
    }

    @Benchmark
    fun nativePoi() {
        val out = NullOutputStream()
        val wb = SXSSFWorkbook(1000)
        val sheet = wb.createSheet("Sheet1")
        for (r in 0 until rowCount) {
            val row = sheet.createRow(r)
            for (c in 0 until colCount) {
                row.createCell(c).setCellValue("Data $r-$c")
            }
        }
        wb.write(out)
        wb.close()
    }

    @Benchmark
    fun kexcelDslPoi() {
        val out = NullOutputStream()
        excel(out, driver = PoiDriver()) {
            sheet("Sheet1") {
                repeat(rowCount) { r ->
                    row {
                        repeat(colCount) { c ->
                            cell(value = "Data $r-$c")
                        }
                    }
                }
            }
        }
    }
}
