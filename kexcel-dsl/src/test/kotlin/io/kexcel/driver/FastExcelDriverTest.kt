package io.kexcel.driver

import io.kexcel.core.excel
import io.kexcel.style.*
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime

class FastExcelDriverTest {

    @Test
    fun `test engine instance reuse`() {
        val driver = FastExcelDriver()

        repeat(2) { i ->
            val out = ByteArrayOutputStream()
            assertDoesNotThrow {
                excel(out, driver) {
                    sheet("Sheet_$i") { row { cell(value = "Data $i") } }
                }
            }
            assertTrue(out.size() > 0)
        }
    }

    @Test
    fun `test FastExcel engine data verification`() {
        val driver = FastExcelDriver()
        val out = ByteArrayOutputStream()
        val now = LocalDateTime.now().withNano(0)

        excel(out, driver) {
            sheet("FastTest") {
                row {
                    cell(value = "Hello FastExcel")
                    cell(value = 12345)
                    cell(value = true)
                    cell(value = now)
                    cell(value = null) // Null case
                }
            }
        }

        XSSFWorkbook(ByteArrayInputStream(out.toByteArray())).use { workbook ->
            val sheet = workbook.getSheet("FastTest")
            val row = sheet.getRow(0)

            assertEquals("Hello FastExcel", row.getCell(0).stringCellValue)
            assertEquals(12345.0, row.getCell(1).numericCellValue)
            assertEquals(true, row.getCell(2).booleanCellValue)
            assertEquals(now.toLocalDate(), row.getCell(3).localDateTimeCellValue.toLocalDate())

            val nullCell = row.getCell(4)
            assertTrue(nullCell == null || nullCell.cellType == CellType.BLANK)
        }
    }

    @Test
    fun `test style and border information`() {
        val driver = FastExcelDriver()
        val out = ByteArrayOutputStream()
        val testStyle = ExcelStyle(
            font = ExcelFont(bold = true, color = "#FF0000"),
            background = ExcelBackground(color = "#FFFF00"),
            alignment = ExcelAlignment(horizontal = HorizontalAlign.CENTER),
            border = ExcelBorder(all = BorderStyle.THIN)
        )

        excel(out, driver) {
            sheet("StyleTest") {
                row {
                    cell(value = "Styled Cell", style = testStyle)
                }
            }
        }

        XSSFWorkbook(ByteArrayInputStream(out.toByteArray())).use { workbook ->
            val cell = workbook.getSheet("StyleTest").getRow(0).getCell(0)
            val style = cell.cellStyle

            assertTrue(workbook.getFontAt(style.fontIndex).bold, "Font should be bold")
            assertEquals(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER, style.alignment)

            // 테두리 검증
            assertEquals(org.apache.poi.ss.usermodel.BorderStyle.THIN, style.borderTop)
            assertEquals(org.apache.poi.ss.usermodel.BorderStyle.THIN, style.borderBottom)
            assertEquals(org.apache.poi.ss.usermodel.BorderStyle.THIN, style.borderLeft)
            assertEquals(org.apache.poi.ss.usermodel.BorderStyle.THIN, style.borderRight)
        }
    }

    @Test
    fun `test large data creation`() {
        val driver = FastExcelDriver()
        val out = ByteArrayOutputStream()

        assertDoesNotThrow {
            excel(out, driver) {
                sheet("LargeData") {
                    repeat(1500) { i ->
                        row {
                            cell(value = "Row $i")
                        }
                    }
                }
            }
        }

        XSSFWorkbook(ByteArrayInputStream(out.toByteArray())).use { workbook ->
            assertEquals(1500, workbook.getSheet("LargeData").lastRowNum + 1)
        }
    }

    @Test
    fun `test hyperlink and cell merge`() {
        val driver = FastExcelDriver()
        val out = ByteArrayOutputStream()

        excel(out, driver) {
            sheet("Features") {
                row {
                    cell(value = "Google", link = "https://www.google.com")
                }
                mergeCells(1, 2, 0, 1)
            }
        }

        XSSFWorkbook(ByteArrayInputStream(out.toByteArray())).use { workbook ->
            val sheet = workbook.getSheet("Features")

            val hasLink = sheet.hyperlinkList.any { it.address == "https://www.google.com" }
            assertTrue(hasLink, "Hyperlink should be present")

            assertTrue(sheet.mergedRegions.any {
                it.firstRow == 1 && it.lastRow == 2 && it.firstColumn == 0 && it.lastColumn == 1
            })
        }
    }

    @Test
    fun `test formula writing in FastExcel`() {
        val driver = FastExcelDriver()
        val out = ByteArrayOutputStream()

        excel(out, driver) {
            sheet("FormulaTest") {
                row {
                    cell(value = 100)
                    cell(value = 200)
                    cell(formula = "A1+B1")
                }
            }
        }

        XSSFWorkbook(ByteArrayInputStream(out.toByteArray())).use { workbook ->
            val cell = workbook.getSheet("FormulaTest").getRow(0).getCell(2)
            assertEquals(CellType.FORMULA, cell.cellType)
            assertEquals("A1+B1", cell.cellFormula)
        }
    }

    @Test
    fun `test various date types support`() {
        val driver = FastExcelDriver()
        val out = ByteArrayOutputStream()
        val localDate = LocalDate.of(2024, 5, 1)
        val utilDate = java.util.Date()
        val calendar = java.util.Calendar.getInstance()

        excel(out, driver) {
            sheet("DateTest") {
                row {
                    cell(value = localDate)
                    cell(value = utilDate)
                    cell(value = calendar)
                }
            }
        }

        XSSFWorkbook(ByteArrayInputStream(out.toByteArray())).use { workbook ->
            val row = workbook.getSheet("DateTest").getRow(0)
            // Check LocalDate
            assertEquals(localDate, row.getCell(0).localDateTimeCellValue.toLocalDate())
            // Check java.util.Date and Calendar (approximate check for date part)
            assertNotNull(row.getCell(1).dateCellValue)
            assertNotNull(row.getCell(2).dateCellValue)
        }
    }

    @Test
    fun `test wrapText and dataFormat`() {
        val driver = FastExcelDriver()
        val out = ByteArrayOutputStream()
        val customStyle = ExcelStyle(
            wrapText = true,
            dataFormat = "#,##0.00"
        )

        excel(out, driver) {
            sheet("StyleTest") {
                row {
                    cell(value = 1234.567, style = customStyle)
                }
            }
        }

        XSSFWorkbook(ByteArrayInputStream(out.toByteArray())).use { workbook ->
            val cell = workbook.getSheet("StyleTest").getRow(0).getCell(0)
            val style = cell.cellStyle
            assertTrue(style.wrapText)
            assertEquals("#,##0.00", style.dataFormatString)
        }
    }

    @Test
    fun `test force formula recalculation no-op`() {
        val driver = FastExcelDriver()
        val out = ByteArrayOutputStream()

        assertDoesNotThrow {
            excel(out, driver, forceFormulaRecalculation = true) {
                sheet("NoOp") { row { cell(value = 1) } }
            }
        }
    }
}
