package io.kexcel.driver

import io.kexcel.style.*
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFFont
import java.io.OutputStream

/**
 * Implements [ExcelDriver] using Apache POI (SXSSF).
 *
 * SXSSFWorkbook is a streaming extension of POI that writes data in chunks
 * (defaulting to 100 rows) to keep memory usage low while still allowing
 * random access to the recently written rows.
 */
class PoiDriver : ExcelDriver {

    private var workbook: SXSSFWorkbook? = null
    private var outputStream: OutputStream? = null
    private var currentSheet: SXSSFSheet? = null
    private var currentRow: Row? = null
    private var options: WorkbookOptions = WorkbookOptions()

    // Style and Font Cache (Manages memory and object limits)
    private val styleCache = mutableMapOf<ExcelStyle, CellStyle>()
    private val fontCache = mutableMapOf<ExcelFont, Font>()
    private val colorMap = DefaultIndexedColorMap()

    override fun startWorkbook(outputStream: OutputStream) {
        this.workbook = SXSSFWorkbook(options.flushInterval)
        this.workbook?.forceFormulaRecalculation = options.forceFormulaRecalculation
        this.outputStream = outputStream
    }

    override fun finishWorkbook() {
        val wb = workbook ?: return
        val os = outputStream ?: return

        try {
            wb.write(os)
        } finally {
            wb.close() // close() calls dispose() internally to clean up temp files
            styleCache.clear()
            fontCache.clear()
            this.workbook = null
            this.outputStream = null
        }
    }

    override fun startSheet(name: String) {
        val wb = workbook ?: throw IllegalStateException("Workbook not initialized. Call startWorkbook() first.")
        this.currentSheet = wb.createSheet(name) as SXSSFSheet
    }

    override fun finishSheet() {
        currentSheet = null
    }

    override fun setColumnWidth(col: Int, width: Int) {
        // POI units are 1/256th of a character width
        currentSheet?.setColumnWidth(col, width)
    }

    override fun mergeCells(firstRow: Int, lastRow: Int, firstCol: Int, lastCol: Int) {
        currentSheet?.addMergedRegion(CellRangeAddress(firstRow, lastRow, firstCol, lastCol))
    }

    override fun startRow(rowNum: Int, height: Double?) {
        val sheet = currentSheet ?: throw IllegalStateException("Sheet not initialized. Call startSheet() first.")
        this.currentRow = sheet.createRow(rowNum)
        height?.let {
            currentRow?.heightInPoints = it.toFloat()
        }
    }

    override fun finishRow() {
        currentRow = null
    }

    override fun writeCell(col: Int, value: Any?, style: ExcelStyle?, link: String?) {
        val row = currentRow ?: return
        val cell = row.createCell(col)

        when (value) {
            is Number -> cell.setCellValue(value.toDouble())
            is Boolean -> cell.setCellValue(value)
            is java.time.LocalDateTime -> cell.setCellValue(value)
            is java.time.LocalDate -> cell.setCellValue(value)
            is java.time.ZonedDateTime -> cell.setCellValue(value.toLocalDateTime())
            is java.util.Date -> cell.setCellValue(value)
            is java.util.Calendar -> cell.setCellValue(value)
            else -> cell.setCellValue(value?.toString() ?: "")
        }

        if (style != null) {
            cell.cellStyle = getOrCreateStyle(style)
        }

        if (link != null) {
            val helper = workbook?.creationHelper ?: return
            val hyperlink = helper.createHyperlink(HyperlinkType.URL)
            hyperlink.address = link
            cell.hyperlink = hyperlink
        }
    }

    override fun writeFormula(col: Int, formula: String, style: ExcelStyle?) {
        val row = currentRow ?: return
        val cell = row.createCell(col)
        cell.cellFormula = formula

        if (style != null) {
            cell.cellStyle = getOrCreateStyle(style)
        }
    }

    override fun applyOptions(options: WorkbookOptions) {
        this.options = options
    }

    override fun nativeWorkbook(): Any? = workbook

    override fun nativeSheet(): Any? = currentSheet

    private fun getOrCreateStyle(style: ExcelStyle): CellStyle {
        return styleCache.getOrPut(style) {
            val wb = workbook ?: throw IllegalStateException("Workbook not initialized")
            val poiStyle = wb.createCellStyle() as XSSFCellStyle

            style.font?.let { excelFont ->
                poiStyle.setFont(getOrCreateFont(excelFont))
            }

            style.alignment?.let { align ->
                align.horizontal?.let { poiStyle.alignment = convertHorizontalAlign(it) }
                align.vertical?.let { poiStyle.verticalAlignment = convertVerticalAlign(it) }
            }
            style.background?.color?.let { hex ->
                poiStyle.setFillForegroundColor(createXSSFColor(hex))
                poiStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            }

            style.border?.let { b ->
                b.resolvedTop()?.let { poiStyle.borderTop = convertBorderStyle(it) }
                b.resolvedBottom()?.let { poiStyle.borderBottom = convertBorderStyle(it) }
                b.resolvedLeft()?.let { poiStyle.borderLeft = convertBorderStyle(it) }
                b.resolvedRight()?.let { poiStyle.borderRight = convertBorderStyle(it) }
            }

            style.wrapText?.let { poiStyle.wrapText = it }

            style.dataFormat?.let { format ->
                poiStyle.dataFormat = wb.createDataFormat().getFormat(format)
            }

            poiStyle
        }
    }

    private fun getOrCreateFont(excelFont: ExcelFont): Font {
        return fontCache.getOrPut(excelFont) {
            val wb = workbook ?: throw IllegalStateException("Workbook not initialized")
            val poiFont = wb.createFont() as XSSFFont

            excelFont.bold?.let { poiFont.bold = it }
            excelFont.size?.let { poiFont.fontHeightInPoints = it.toShort() }
            excelFont.color?.let { hex -> poiFont.setColor(createXSSFColor(hex)) }
            excelFont.underlined?.let {
                if (it) poiFont.underline = Font.U_SINGLE
            }

            poiFont
        }
    }

    private fun createXSSFColor(hex: String): XSSFColor {
        val cleanHex = hex.removePrefix("#").uppercase()
        val rgb = try {
            when (cleanHex.length) {
                6 -> byteArrayOf(
                    cleanHex.substring(0, 2).toInt(16).toByte(),
                    cleanHex.substring(2, 4).toInt(16).toByte(),
                    cleanHex.substring(4, 6).toInt(16).toByte()
                )

                3 -> byteArrayOf(
                    cleanHex.substring(0, 1).repeat(2).toInt(16).toByte(),
                    cleanHex.substring(1, 2).repeat(2).toInt(16).toByte(),
                    cleanHex.substring(2, 3).repeat(2).toInt(16).toByte()
                )

                else -> byteArrayOf(0, 0, 0) // Invalid length falls back to black
            }
        } catch (e: Exception) {
            byteArrayOf(0, 0, 0) // Parsing error falls back to black
        }
        return XSSFColor(rgb, colorMap)
    }

    private fun convertHorizontalAlign(align: HorizontalAlign): HorizontalAlignment = when (align) {
        HorizontalAlign.LEFT -> HorizontalAlignment.LEFT
        HorizontalAlign.CENTER -> HorizontalAlignment.CENTER
        HorizontalAlign.RIGHT -> HorizontalAlignment.RIGHT
        HorizontalAlign.FILL -> HorizontalAlignment.FILL
        HorizontalAlign.JUSTIFY -> HorizontalAlignment.JUSTIFY
        HorizontalAlign.GENERAL -> HorizontalAlignment.GENERAL
    }

    private fun convertVerticalAlign(align: VerticalAlign): VerticalAlignment = when (align) {
        VerticalAlign.TOP -> VerticalAlignment.TOP
        VerticalAlign.CENTER -> VerticalAlignment.CENTER
        VerticalAlign.BOTTOM -> VerticalAlignment.BOTTOM
        VerticalAlign.JUSTIFY -> VerticalAlignment.JUSTIFY
    }

    private fun convertBorderStyle(style: io.kexcel.style.BorderStyle): org.apache.poi.ss.usermodel.BorderStyle =
        when (style) {
            io.kexcel.style.BorderStyle.THIN -> org.apache.poi.ss.usermodel.BorderStyle.THIN
            io.kexcel.style.BorderStyle.MEDIUM -> org.apache.poi.ss.usermodel.BorderStyle.MEDIUM
            io.kexcel.style.BorderStyle.THICK -> org.apache.poi.ss.usermodel.BorderStyle.THICK
            io.kexcel.style.BorderStyle.DASHED -> org.apache.poi.ss.usermodel.BorderStyle.DASHED
            io.kexcel.style.BorderStyle.DOTTED -> org.apache.poi.ss.usermodel.BorderStyle.DOTTED
            io.kexcel.style.BorderStyle.DOUBLE -> org.apache.poi.ss.usermodel.BorderStyle.DOUBLE
            io.kexcel.style.BorderStyle.HAIR -> org.apache.poi.ss.usermodel.BorderStyle.HAIR
        }
}
