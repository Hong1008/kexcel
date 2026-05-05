package io.kexcel.driver

import io.kexcel.style.*
import org.dhatim.fastexcel.BorderSide
import org.dhatim.fastexcel.HyperLink
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * Implements [ExcelDriver] using [org.dhatim.fastexcel].
 *
 * FastExcel offers superior performance and lower memory usage compared to Apache POI,
 * making it an excellent choice for high-performance Excel generation tasks.
 */
class FastExcelDriver(
    private val applicationName: String = "KotlinDslExcel",
    private val applicationVersion: String = "1.0"
) : ExcelDriver {

    private var workbook: Workbook? = null
    private var currentWorksheet: Worksheet? = null
    private var currentRowNum: Int = 0

    override fun startWorkbook(outputStream: OutputStream) {
        this.workbook = Workbook(outputStream, applicationName, applicationVersion)
    }

    override fun finishWorkbook() {
        // If the last sheet was not explicitly finished, finish it here.
        currentWorksheet?.finish()
        workbook?.finish()
        workbook = null
        currentWorksheet = null
    }

    override fun startSheet(name: String) {
        currentWorksheet = workbook?.newWorksheet(name)
        currentRowNum = 0
    }

    override fun finishSheet() {
        // FastExcel's Worksheet must be explicitly finished.
        currentWorksheet?.finish()
    }

    override fun setColumnWidth(col: Int, width: Int) {
        // Maintains consistency with POI units (1/256th character width).
        // Converts user input from excel(out) { width(0, 5000) } to FastExcel units.
        currentWorksheet?.width(col, width.toDouble() / 256.0)
    }

    override fun mergeCells(firstRow: Int, lastRow: Int, firstCol: Int, lastCol: Int) {
        // FastExcel range(top, left, bottom, right)
        currentWorksheet?.range(firstRow, firstCol, lastRow, lastCol)?.merge()
    }

    override fun startRow(rowNum: Int, height: Double?) {
        this.currentRowNum = rowNum
        height?.let {
            currentWorksheet?.rowHeight(rowNum, it)
        }
    }

    override fun finishRow() {
        // Flushes every 1000 rows to maintain low memory usage. (FastExcel recommended pattern)
        if (currentRowNum > 0 && currentRowNum % 1000 == 0) {
            currentWorksheet?.flush()
        }
    }

    override fun writeCell(col: Int, value: Any?, style: ExcelStyle?, link: String?) {
        val ws = currentWorksheet ?: throw IllegalStateException("Worksheet not initialized")

        // 1. Write value (Check Boolean before Number to prevent type confusion)
        when (value) {
            is Boolean -> ws.value(currentRowNum, col, value)
            is Number -> ws.value(currentRowNum, col, value)
            is String -> ws.value(currentRowNum, col, value)
            is LocalDate -> ws.value(currentRowNum, col, value)
            is LocalDateTime -> ws.value(currentRowNum, col, value)
            is Date -> ws.value(currentRowNum, col, value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
            is Calendar -> ws.value(
                currentRowNum,
                col,
                value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            )

            null -> { /* Do nothing - keep the cell empty */
            }

            else -> ws.value(currentRowNum, col, value.toString())
        }

        // 2. Apply style
        if (style != null || link != null) {
            val styleSetter = ws.style(currentRowNum, col)
            style?.let { applyStyle(styleSetter, it) }
            link?.let { ws.hyperlink(currentRowNum, col, HyperLink(it)) }
            styleSetter.set()
        }
    }

    override fun writeFormula(col: Int, formula: String, style: ExcelStyle?) {
        val ws = currentWorksheet ?: throw IllegalStateException("Worksheet not initialized")
        ws.formula(currentRowNum, col, formula)

        if (style != null) {
            val styleSetter = ws.style(currentRowNum, col)
            applyStyle(styleSetter, style)
            styleSetter.set()
        }
    }

    override fun setForceFormulaRecalculation(value: Boolean) {
        // FastExcel viewer handles this.
    }

    private fun applyStyle(styleSetter: org.dhatim.fastexcel.StyleSetter, s: ExcelStyle) {
        s.font?.let { f ->
            if (f.bold == true) styleSetter.bold()
            f.size?.let { styleSetter.fontSize(it) }
            f.color?.let { styleSetter.fontColor(it.replace("#", "")) }
        }
        s.background?.color?.let { styleSetter.fillColor(it.replace("#", "")) }
        s.alignment?.let { a ->
            a.horizontal?.let { styleSetter.horizontalAlignment(it.name.lowercase()) }
            a.vertical?.let { styleSetter.verticalAlignment(it.name.lowercase()) }
        }
        s.border?.let { b ->
            b.resolvedTop()?.let { styleSetter.borderStyle(BorderSide.TOP, it.name.lowercase()) }
            b.resolvedBottom()?.let { styleSetter.borderStyle(BorderSide.BOTTOM, it.name.lowercase()) }
            b.resolvedLeft()?.let { styleSetter.borderStyle(BorderSide.LEFT, it.name.lowercase()) }
            b.resolvedRight()?.let { styleSetter.borderStyle(BorderSide.RIGHT, it.name.lowercase()) }
        }
        if (s.wrapText == true) styleSetter.wrapText(true)
        s.dataFormat?.let { styleSetter.format(it) }
    }

    override fun nativeWorkbook(): Any? = workbook

    override fun nativeSheet(): Any? = currentWorksheet
}
