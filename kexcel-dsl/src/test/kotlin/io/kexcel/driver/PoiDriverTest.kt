package io.kexcel.driver

import io.kexcel.core.excel
import io.kexcel.style.ExcelFont
import io.kexcel.style.ExcelStyle
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime

class PoiDriverTest {

    @Test
    fun `should initialize state safely when reusing engine instance`() {
        val driver = PoiDriver()
        val out1 = ByteArrayOutputStream()
        val out2 = ByteArrayOutputStream()

        // First generation
        assertDoesNotThrow {
            excel(out1, driver) {
                sheet("Sheet1") { row { cell(value = "First") } }
            }
        }

        // Second generation (reuse)
        assertDoesNotThrow {
            excel(out2, driver) {
                sheet("Sheet2") { row { cell(value = "Second") } }
            }
        }

        assertTrue(out1.size() > 0)
        assertTrue(out2.size() > 0)
    }

    @Test
    fun `should reuse CellStyle object when using same style repeatedly`() {
        val driver = PoiDriver()
        val out = ByteArrayOutputStream()
        val sharedStyle = ExcelStyle(font = ExcelFont(bold = true, color = "#FF0000"))

        var poiWorkbook: Workbook? = null

        excel(out, driver) {
            // Use reflection to access the actual Workbook object inside the engine (for testing purposes)
            val field = driver.javaClass.getDeclaredField("workbook")
            field.isAccessible = true
            poiWorkbook = field.get(driver) as SXSSFWorkbook

            sheet("CacheTest") {
                // Apply same style to 100 cells
                repeat(100) { i ->
                    row {
                        cell(value = "Data $i", style = sharedStyle)
                    }
                }
            }
        }

        // POI default style (1) + Our shared style (1) = Total should be 2
        // If caching didn't work, it would be 101
        assertNotNull(poiWorkbook)
        assertEquals(2, poiWorkbook!!.numCellStyles, "CellStyle should be cached and reused")
    }

    @Test
    fun `should support 3 and 6 digit HEX colors and fallback to black for invalid values`() {
        val driver = PoiDriver()

        // Use reflection to test the internal private method createXSSFColor
        val method = driver.javaClass.getDeclaredMethod("createXSSFColor", String::class.java)
        method.isAccessible = true

        fun parse(hex: String): ByteArray {
            val xssfColor = method.invoke(driver, hex) as org.apache.poi.xssf.usermodel.XSSFColor
            return xssfColor.rgb
        }

        // 6-digit parsing
        assertArrayEquals(byteArrayOf(255.toByte(), 0, 0), parse("#FF0000"))
        assertArrayEquals(byteArrayOf(0, 255.toByte(), 0), parse("00FF00"))

        // 3-digit parsing (#F00 -> #FF0000)
        assertArrayEquals(byteArrayOf(255.toByte(), 0, 0), parse("#F00"))
        assertArrayEquals(byteArrayOf(0, 0, 255.toByte()), parse("00F"))

        // Invalid values -> Black (0,0,0)
        assertArrayEquals(byteArrayOf(0, 0, 0), parse("INVALID"))
        assertArrayEquals(byteArrayOf(0, 0, 0), parse("#XYZ"))
    }

    @Test
    fun `should write various data types without crash`() {
        val driver = PoiDriver()
        val out = ByteArrayOutputStream()

        assertDoesNotThrow {
            excel(out, driver) {
                sheet("TypeTest") {
                    row {
                        cell(value = "String")
                        cell(value = 100)
                        cell(value = 99.9)
                        cell(value = true)
                        cell(value = LocalDate.now())
                        cell(value = LocalDateTime.now())
                        cell(value = null) // Null safety
                    }
                }
            }
        }
    }

    @Test
    fun `should set formula and recalculation flag correctly in POI workbook`() {
        val driver = PoiDriver()
        val out = ByteArrayOutputStream()

        excel(out, driver, forceFormulaRecalculation = true) {
            sheet("FormulaTest") {
                row {
                    cell(value = 100)
                    cell(value = 200)
                    cell(formula = "SUM(A1:B1)")
                }
            }
        }

        // Re-read generated data to verify
        org.apache.poi.xssf.usermodel.XSSFWorkbook(java.io.ByteArrayInputStream(out.toByteArray())).use { workbook ->
            assertTrue(workbook.forceFormulaRecalculation, "ForceFormulaRecalculation flag should be true")
            val cell = workbook.getSheet("FormulaTest").getRow(0).getCell(2)
            assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, cell.cellType)
            assertEquals("SUM(A1:B1)", cell.cellFormula)
        }
    }

    @Test
    fun `should write java_util_Date and Calendar as correct date values`() {
        val driver = PoiDriver()
        val out = ByteArrayOutputStream()
        val utilDate = java.util.Date()
        val calendar = java.util.Calendar.getInstance()

        excel(out, driver) {
            sheet("DateTest") {
                row {
                    cell(value = utilDate)
                    cell(value = calendar)
                }
            }
        }

        org.apache.poi.xssf.usermodel.XSSFWorkbook(java.io.ByteArrayInputStream(out.toByteArray())).use { workbook ->
            val row = workbook.getSheet("DateTest").getRow(0)
            assertNotNull(row.getCell(0).dateCellValue)
            assertNotNull(row.getCell(1).dateCellValue)
        }
    }

    @Test
    fun `should throw exception when attempting operations on uninitialized engine`() {
        val driver = PoiDriver()

        val exception = assertThrows(IllegalStateException::class.java) {
            driver.startSheet("DirectCall")
        }

        assertTrue(exception.message!!.contains("Workbook not initialized"))
    }
}
