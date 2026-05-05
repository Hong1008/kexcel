package io.kexcel.core

import io.kexcel.style.*
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class DataSheetTest {

    data class User(val name: String, val joinDate: LocalDate, val amount: Double, val detailUrl: String)

    @Test
    fun `test dataSheet basic binding and styling`() {
        val userList = listOf(
            User("Alice", LocalDate.of(2023, 1, 1), 1000.0, "https://example.com/alice"),
            User("Bob", LocalDate.of(2023, 2, 1), 2000.0, "https://example.com/bob")
        )

        val headerStyle = ExcelStyle(
            font = ExcelFont(bold = true),
            background = ExcelBackground(color = "#CCE5FF")
        )
        val amountStyle = ExcelStyle(dataFormat = "#,##0.00")
        val stripedStyle = ExcelStyle(background = ExcelBackground(color = "#F2F2F2"))

        val outputStream = ByteArrayOutputStream()
        excel(outputStream) {
            dataSheet("Users", userList) {
                headerStyle(headerStyle)

                column("이름") { it.name }
                column("가입일") { it.joinDate }
                column("금액", style = amountStyle) { it.amount }
                column("상세보기", link = { it.detailUrl }) { "Link" }

                rowStyle { index, _ ->
                    if (index % 2 == 1) stripedStyle else null
                }
            }
        }

        // Cross-validate with POI
        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheet("Users")

        // Check Header (Row 0)
        val headerRow = sheet.getRow(0)
        assertEquals("이름", headerRow.getCell(0).stringCellValue)
        assertEquals("가입일", headerRow.getCell(1).stringCellValue)
        assertEquals("금액", headerRow.getCell(2).stringCellValue)
        assertEquals("상세보기", headerRow.getCell(3).stringCellValue)

        // Check Header Style (Simplified check: bg color)
        // Note: Exact color hex check might vary depending on POI's color management, but we check if style is applied
        assertEquals(true, headerRow.getCell(0).cellStyle.font.bold)

        // Check Data (Row 1 - Alice)
        val row1 = sheet.getRow(1)
        assertEquals("Alice", row1.getCell(0).stringCellValue)
        assertEquals(1000.0, row1.getCell(2).numericCellValue)
        assertEquals("#,##0.00", row1.getCell(2).cellStyle.dataFormatString)
        assertEquals("https://example.com/alice", row1.getCell(3).hyperlink.address)

        // Check Data (Row 2 - Bob)
        val row2 = sheet.getRow(2)
        assertEquals("Bob", row2.getCell(0).stringCellValue)
        // Check Striped Style on Row 2 (index 1 in lambda)
        // Bob's row is data index 1, so rowStyle should apply stripedStyle
        // We check if it has the background color set (not null)
        val bobStyle = row2.getCell(0).cellStyle
        // Depending on engine (Poi vs FastExcel), the exact way colors are stored might differ
        // But the cell should have a fill pattern if background is applied
        assertEquals(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND, bobStyle.fillPattern)

        workbook.close()
    }

    @Test
    fun `test dataSheet with empty list`() {
        val emptyList = emptyList<User>()
        val outputStream = ByteArrayOutputStream()
        excel(outputStream) {
            dataSheet("Empty", emptyList) {
                column("Col1") { it.name }
            }
        }

        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheet("Empty")
        assertEquals(1, sheet.physicalNumberOfRows) // Only header
        assertEquals("Col1", sheet.getRow(0).getCell(0).stringCellValue)
        workbook.close()
    }

    @Test
    fun `test dataSheet dynamic cell styling`() {
        val userList = listOf(
            User("Alice", LocalDate.of(2023, 1, 1), -500.0, ""),
            User("Bob", LocalDate.of(2023, 2, 1), 2000.0, "")
        )

        val redStyle = ExcelStyle(font = ExcelFont(color = "#FF0000"))

        val outputStream = ByteArrayOutputStream()
        excel(outputStream) {
            dataSheet("Dynamic", userList) {
                column("이름") { it.name }
                column(
                    header = "금액",
                    cellStyle = { if (it.amount < 0) redStyle else null }
                ) { it.amount }
            }
        }

        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheet("Dynamic")

        // Alice (Row 1): amount is negative, should be red
        val aliceAmountCell = sheet.getRow(1).getCell(1)
        assertEquals(-500.0, aliceAmountCell.numericCellValue)

        // Verify red color (XSSF specific check for RGB)
        val aliceFont = workbook.getFontAt(aliceAmountCell.cellStyle.fontIndex) as XSSFFont
        val aliceColor = aliceFont.xssfColor
        Assertions.assertNotNull(aliceColor)
        // Red is FF0000, POI returns it as ARGB "FFFF0000"
        assertEquals("FFFF0000", aliceColor?.argbHex)

        // Bob (Row 2): amount is positive, should NOT have custom red color
        val bobAmountCell = sheet.getRow(2).getCell(1)
        assertEquals(2000.0, bobAmountCell.numericCellValue)
        val bobFont = workbook.getFontAt(bobAmountCell.cellStyle.fontIndex) as XSSFFont
        val bobColor = bobFont.xssfColor
        // Bob's cell should not have the red color. It may be null or a default color.
        Assertions.assertTrue(bobColor == null || bobColor.argbHex != "FFFF0000")

        workbook.close()
    }

    @Test
    fun `test nativeSheet in dataSheet scope`() {
        val userList = listOf(User("Alice", LocalDate.now(), 1000.0, ""))
        val outputStream = ByteArrayOutputStream()

        excel(outputStream) {
            dataSheet("NativeTest", userList) {
                column("Name") { it.name }

                // Use native POI feature to freeze header
                nativeSheet<org.apache.poi.xssf.streaming.SXSSFSheet> { sheet ->
                    sheet.createFreezePane(0, 1)
                }
            }
        }

        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheet("NativeTest")

        // Verify freeze pane exists
        val pane = sheet.paneInformation
        Assertions.assertNotNull(pane)
        assertEquals(0, pane.verticalSplitPosition)
        assertEquals(1, pane.horizontalSplitPosition)

        workbook.close()
    }

    @Test
    fun `test dataSheet with formula column`() {
        val userList = listOf(
            User("Alice", LocalDate.now(), 1000.0, ""),
            User("Bob", LocalDate.now(), 2000.0, "")
        )

        val outputStream = ByteArrayOutputStream()
        excel(outputStream) {
            dataSheet("FormulaTest", userList) {
                column("Name") { it.name }
                column("Amount") { it.amount }
                // Formula: B2 * 1.1 (Row starts at 2 since Row 1 is header)
                columnFormula("Taxed Amount") { index, item -> "B${index + 2} * 1.1" }
            }
        }

        val workbook = XSSFWorkbook(ByteArrayInputStream(outputStream.toByteArray()))
        val sheet = workbook.getSheet("FormulaTest")

        // Alice (Row 1, Sheet row index 1)
        val aliceRow = sheet.getRow(1)
        assertEquals("B2 * 1.1", aliceRow.getCell(2).cellFormula)

        // Bob (Row 2, Sheet row index 2)
        val bobRow = sheet.getRow(2)
        assertEquals("B3 * 1.1", bobRow.getCell(2).cellFormula)

        workbook.close()
    }

    @Test
    fun `test dataSheet should throw exception when no columns are defined`() {
        val userList = listOf(User("Alice", LocalDate.now(), 1000.0, ""))
        val outputStream = ByteArrayOutputStream()

        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            excel(outputStream) {
                dataSheet("NoColumns", userList) {
                    // No column() calls here
                }
            }
        }
        Assertions.assertTrue(exception.message!!.contains("requires at least one column"))
    }

    @Test
    fun `test dataSheet should wrap extraction errors in ExcelStreamingException`() {
        val userList = listOf(User("Alice", LocalDate.now(), 1000.0, ""))
        val outputStream = ByteArrayOutputStream()

        val exception = Assertions.assertThrows(ExcelStreamingException::class.java) {
            excel(outputStream) {
                dataSheet("ErrorTest", userList) {
                    column("Name") { throw RuntimeException("Extraction Failed") }
                }
            }
        }
        Assertions.assertTrue(exception.message!!.contains("Error extracting value for column 'Name'"))
    }

    @Test
    fun `test dataSheet should wrap style calculation errors in ExcelStreamingException`() {
        val userList = listOf(User("Alice", LocalDate.now(), 1000.0, ""))
        val outputStream = ByteArrayOutputStream()

        val exception = Assertions.assertThrows(ExcelStreamingException::class.java) {
            excel(outputStream) {
                dataSheet("StyleError", userList) {
                    column("Name", cellStyle = { throw RuntimeException("Style Failed") }) { it.name }
                }
            }
        }
        Assertions.assertTrue(exception.message!!.contains("Error evaluating cellStyle for column 'Name'"))
    }

    @Test
    fun `test dataSheet should wrap formula calculation errors in ExcelStreamingException`() {
        val userList = listOf(User("Alice", LocalDate.now(), 1000.0, ""))
        val outputStream = ByteArrayOutputStream()

        val exception = Assertions.assertThrows(ExcelStreamingException::class.java) {
            excel(outputStream) {
                dataSheet("FormulaError", userList) {
                    column("Name") { it.name }
                    columnFormula("Total") { _, _ -> throw RuntimeException("Formula Failed") }
                }
            }
        }
        Assertions.assertTrue(exception.message!!.contains("Error extracting formula for column 'Total'"))
    }
}
