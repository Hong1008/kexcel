package io.kexcel.core

import io.kexcel.driver.ExcelDriver
import io.kexcel.driver.WorkbookOptions
import io.kexcel.style.ExcelFont
import io.kexcel.style.ExcelStyle
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.LocalDate

class SheetTest {

    // Test class for verifying the Excel DSL functionality.
    // It uses a MockExcelDriver to record and verify the sequence of engine calls.
    class MockExcelDriver : ExcelDriver {
        // Record the call log for verification purposes.
        val callLog = mutableListOf<String>()
        val writtenStyles = mutableMapOf<Pair<Int, Int>, ExcelStyle?>()

        override fun startWorkbook(outputStream: OutputStream) {
            callLog.add("startWorkbook")
        }

        override fun finishWorkbook() {
            callLog.add("finishWorkbook")
        }

        override fun startSheet(name: String) {
            callLog.add("startSheet:$name")
        }

        override fun finishSheet() {
            callLog.add("finishSheet")
        }

        override fun setColumnWidth(col: Int, width: Int) {
            callLog.add("setColumnWidth:$col:$width")
        }

        override fun mergeCells(firstRow: Int, lastRow: Int, firstCol: Int, lastCol: Int) {
            callLog.add("mergeCells")
        }

        private var currentRow = -1

        override fun startRow(rowNum: Int, height: Double?) {
            currentRow = rowNum
            callLog.add("startRow:$rowNum")
        }

        override fun finishRow() {
            callLog.add("finishRow")
        }

        override fun writeCell(col: Int, value: Any?, style: ExcelStyle?, link: String?) {
            callLog.add("writeCell:$col:$value")
            writtenStyles[currentRow to col] = style
        }

        override fun writeFormula(col: Int, formula: String, style: ExcelStyle?) {
            callLog.add("writeFormula:$col:$formula")
            writtenStyles[currentRow to col] = style
        }

        override fun applyOptions(options: WorkbookOptions) {
            callLog.add("applyOptions:flushInterval=${options.flushInterval},forceFormulaRecalculation=${options.forceFormulaRecalculation}")
        }

        override fun nativeWorkbook(): Any? = null
        override fun nativeSheet(): Any? = null
    }

    @Test
    fun `test basic excel creation flow and engine call sequence`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        excel(out, driver) {
            sheet("TestSheet") {
                row(0) {
                    cell(value = "Header")
                }
                row {
                    cell(value = "Data")
                }
            }
        }

        val expectedLogs = listOf(
            "applyOptions:flushInterval=1000,forceFormulaRecalculation=false",
            "startWorkbook",
            "startSheet:TestSheet",
            "startRow:0",
            "writeCell:0:Header",
            "finishRow",
            "startRow:1",
            "writeCell:0:Data",
            "finishRow",
            "finishSheet",
            "finishWorkbook"
        )

        assertEquals(expectedLogs, driver.callLog)
    }

    @Test
    fun `test style inheritance and merge logic`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        val wbStyle = ExcelStyle(font = ExcelFont(bold = true))
        val sheetStyle = ExcelStyle(font = ExcelFont(color = "RED"))
        val rowStyle = ExcelStyle(font = ExcelFont(size = 14))

        excel(out, driver, defaultStyle = wbStyle) {
            sheet("StyleSheet", defaultStyle = sheetStyle) {
                row(style = rowStyle) {
                    cell(0, "MergedStyle")
                    cell(1, "OverrideStyle", style = ExcelStyle(font = ExcelFont(bold = false)))
                }
            }
        }

        val style0 = driver.writtenStyles[0 to 0]
        assertNotNull(style0)
        val font0 = style0!!.font!!
        assertTrue(font0.bold!!)
        assertEquals("RED", font0.color)
        assertEquals(14, font0.size)

        val style1 = driver.writtenStyles[0 to 1]
        assertNotNull(style1)
        val font1 = style1!!.font!!
        assertFalse(font1.bold!!)
        assertEquals("RED", font1.color)
    }

    @Test
    fun `test date type data and data format style`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()
        val today = LocalDate.of(2024, 5, 2)
        val dateStyle = ExcelStyle(dataFormat = "yyyy-mm-dd")

        excel(out, driver) {
            sheet("DateSheet") {
                row {
                    cell(value = today, style = dateStyle)
                }
            }
        }

        val style = driver.writtenStyles[0 to 0]
        assertEquals("yyyy-mm-dd", style?.dataFormat)
        assertTrue(driver.callLog.any { it.contains("writeCell:0:2024-05-02") })
    }

    @Test
    fun `test engine auto-detection`() {
        val out = ByteArrayOutputStream()

        // 엔진을 명시하지 않아도 Classpath에 POI가 있다면 에러 없이 실행되어야 함
        assertDoesNotThrow {
            excel(out) {
                sheet("AutoDetectSheet") {
                    row { cell(value = "Auto Injected") }
                }
            }
        }
    }

    @Test
    fun `test streaming environment row number reverse order`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        val exception = assertThrows(ExcelStreamingException::class.java) {
            excel(out, driver) {
                sheet("ErrorSheet") {
                    row(5) { cell(value = "Row 5") }
                    row(3) { cell(value = "Row 3 - Error") }
                }
            }
        }

        assertTrue(exception.message!!.contains("Cannot write to row 3"))
    }

    @Test
    fun `test dsl execution throws exception`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        try {
            excel(out, driver) {
                sheet("FailSheet") {
                    row {
                        throw RuntimeException("Business Logic Error")
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        assertTrue(driver.callLog.contains("startWorkbook"))
        assertTrue(driver.callLog.contains("finishWorkbook"), "finishWorkbook must be called even on error")
    }

    @Test
    fun `test large sequence based streaming`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        val dataSequence = sequenceOf("Data1", "Data2", "Data3")

        excel(out, driver) {
            sheet("StreamSheet") {
                row { cell(value = "Header") }
                rows(dataSequence) { data ->
                    cell(value = data)
                }
            }
        }

        val writeCellCalls = driver.callLog.filter { it.startsWith("writeCell") }
        assertEquals(4, writeCellCalls.size)
        assertEquals("writeCell:0:Header", writeCellCalls[0])
        assertEquals("writeCell:0:Data1", writeCellCalls[1])
    }

    @Test
    fun `test cell formula and conflict validation`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        // 1. Success case: formula
        excel(out, driver) {
            sheet("Formula") {
                row {
                    cell(value = 10)
                    cell(value = 20)
                    cell(formula = "SUM(A1:B1)")
                }
            }
        }
        assertTrue(driver.callLog.contains("writeFormula:2:SUM(A1:B1)"))

        // 2. Conflict case: both value and formula
        assertThrows(IllegalArgumentException::class.java) {
            excel(out, driver) {
                sheet("Error") {
                    row {
                        cell(value = "Data", formula = "SUM(A1)")
                    }
                }
            }
        }
    }

    @Test
    fun `test column width and merge cells`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        excel(out, driver) {
            sheet("Layout") {
                columnWidth(0 to 100, 1 to 200)
                mergeCells(0, 1, 0, 1)
                row { cell(value = "Merged") }
            }
        }

        assertTrue(driver.callLog.contains("setColumnWidth:0:100"))
        assertTrue(driver.callLog.contains("setColumnWidth:1:200"))
        assertTrue(driver.callLog.contains("mergeCells"))
    }

    @Test
    fun `test manual column indexing`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        excel(out, driver) {
            sheet("Index") {
                row {
                    cell(value = "A") // col 0
                    cell(col = 5, value = "F") // col 5
                    cell(value = "G") // col 6 (auto increment from last col)
                }
            }
        }

        assertTrue(driver.callLog.contains("writeCell:0:A"))
        assertTrue(driver.callLog.contains("writeCell:5:F"))
        assertTrue(driver.callLog.contains("writeCell:6:G"))
    }

    @Test
    fun `test force formula recalculation option`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        excel(out, driver, options = WorkbookOptions(forceFormulaRecalculation = true)) {
            sheet("Sheet1") { row { cell(value = 1) } }
        }

        assertTrue(driver.callLog.any { it.contains("forceFormulaRecalculation=true") })
    }

    @Test
    fun `test nativeSheet access in sheet scope`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()
        var nativeCalled = false

        excel(out, driver) {
            sheet("Native") {
                nativeSheet<String> { // Mock returns null, but we test the mechanism
                    nativeCalled = true
                }
            }
        }
        // Since MockExcelDriver.nativeSheet() returns null, String cast won't happen.
        // This test ensures it doesn't crash even if type doesn't match.
        assertFalse(nativeCalled)
    }

    @Test
    fun `test skip and skipRows functionality`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        excel(out, driver) {
            sheet("SkipTest") {
                skipRows(2) // Skip to row 2
                row {
                    cell(value = "A3") // row 2, col 0
                    skip(2) // Skip to col 3
                    cell(value = "D3") // row 2, col 3
                }
                row {
                    cell(value = "A4") // row 3, col 0
                }
            }
        }

        assertTrue(driver.callLog.contains("startRow:2"))
        assertTrue(driver.callLog.contains("writeCell:0:A3"))
        assertTrue(driver.callLog.contains("writeCell:3:D3"))
        assertTrue(driver.callLog.contains("startRow:3"))
        assertTrue(driver.callLog.contains("writeCell:0:A4"))
    }

    @Test
    fun `test nextRowNum and nextColNum getters`() {
        val driver = MockExcelDriver()
        val out = ByteArrayOutputStream()

        var rowNumAtStart = -1
        var colNumAfterSkip = -1

        excel(out, driver) {
            sheet("GetterTest") {
                rowNumAtStart = nextRowNum // Should be 0
                skipRows(5)
                row {
                    skip(3)
                    colNumAfterSkip = nextColNum // Should be 3
                    cell(value = "Value")
                }
            }
        }

        assertEquals(0, rowNumAtStart)
        assertEquals(3, colNumAfterSkip)
    }
}

