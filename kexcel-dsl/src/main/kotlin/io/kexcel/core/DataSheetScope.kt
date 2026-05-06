package io.kexcel.core

import io.kexcel.driver.ExcelDriver
import io.kexcel.style.ExcelStyle

/**
 * Defines a column for the [DataSheetScope].
 *
 * <p>Intentionally a plain class (not data class) because lambda fields
 * cannot be meaningfully compared via equals()/hashCode().
 *
 * @param header the column header text
 * @param headerStyle optional style override for this column's header cell only
 * @param style optional static style for data cells in this column
 * @param cellStyle optional dynamic style for data cells, evaluated per item. Has the highest priority.
 * @param linkExtractor optional lambda to extract a hyperlink URL from each item
 * @param valueExtractor lambda to extract the cell value from each item
 */
class ColumnDef<T>(
    val header: String,
    val headerStyle: ExcelStyle? = null,
    val style: ExcelStyle? = null,
    val cellStyle: ((T) -> ExcelStyle?)? = null,
    val linkExtractor: ((T) -> String?)? = null,
    val valueExtractor: ((T) -> Any?)? = null,
    val formulaExtractor: ((Int, T) -> String)? = null
)

/**
 * Scope for DTO-based data sheets.
 * Provides a declarative way to map a collection of items to Excel rows.
 */
@ExcelDslMarker
class DataSheetScope<T>(
    driver: ExcelDriver,
    private val defaultStyle: ExcelStyle? = null
) : BaseScope(driver) {
    private val columns = mutableListOf<ColumnDef<T>>()
    private var _headerStyle: ExcelStyle? = null
    private var _rowStyleFn: ((Int, T) -> ExcelStyle?)? = null

    /**
     * Provides access to the underlying native sheet object.
     *
     * <p><b>Streaming Engine Caution:</b>
     * Streaming engines (POI SXSSF, FastExcel) flush rows to disk periodically.
     * Once a row is flushed, it cannot be modified using the native object.
     *
     * @param T the expected native sheet type
     * @param block the callback receiving the native sheet
     */
    inline fun <reified R> nativeSheet(crossinline block: (R) -> Unit) = writeSafely {
        val native = driver.nativeSheet()
        if (native is R) block(native)
    }

    /**
     * Sets a style to be applied to the header row.
     */
    fun headerStyle(style: ExcelStyle) {
        this._headerStyle = style
    }

    /**
     * Defines a column in the data sheet.
     * @param header the column header text
     * @param style optional static style for data cells in this column
     * @param headerStyle optional style override for this column's header cell only.
     *   If provided, it is merged on top of the sheet-level [headerStyle].
     * @param cellStyle optional dynamic style evaluated per item. Merged on top of [style] and [rowStyle].
     * @param link an optional lambda to extract a hyperlink URL from each item
     * @param extractor a lambda to extract the cell value from each item
     */
    fun column(
        header: String,
        style: ExcelStyle? = null,
        headerStyle: ExcelStyle? = null,
        cellStyle: ((T) -> ExcelStyle?)? = null,
        link: ((T) -> String?)? = null,
        extractor: (T) -> Any?
    ) {
        columns.add(ColumnDef(header, headerStyle, style, cellStyle, link, valueExtractor = extractor))
    }

    /**
     * Defines a formula column in the data sheet.
     * @param header the column header text
     * @param style optional static style for data cells in this column
     * @param headerStyle optional style override for this column's header cell only
     * @param cellStyle optional dynamic style evaluated per item
     * @param formula a lambda to generate the Excel formula string for each item (index, item) -> String
     */
    fun columnFormula(
        header: String,
        style: ExcelStyle? = null,
        headerStyle: ExcelStyle? = null,
        cellStyle: ((T) -> ExcelStyle?)? = null,
        formula: (Int, T) -> String
    ) {
        columns.add(ColumnDef(header, headerStyle, style, cellStyle, formulaExtractor = formula))
    }

    /**
     * Defines a conditional style for data rows.
     * @param fn a lambda that returns a style based on the row index and item
     */
    fun rowStyle(fn: (index: Int, item: T) -> ExcelStyle?) {
        this._rowStyleFn = fn
    }

    /**
     * Writes the header and data rows to the driver.
     * @throws IllegalStateException if no columns have been defined
     */
    internal fun writeTo(data: Sequence<T>) {
        check(columns.isNotEmpty()) {
            "DataSheetScope requires at least one column. Call column() before writing."
        }
        writeHeader()
        writeData(data)
    }

    private fun writeHeader() {
        driver.startRow(0, null)
        try {
            columns.forEachIndexed { i, col ->
                val mergedHeader = defaultStyle
                    ?.merge(_headerStyle)
                    ?.merge(col.headerStyle)
                    ?: _headerStyle?.merge(col.headerStyle)
                    ?: col.headerStyle
                driver.writeCell(i, col.header, mergedHeader, null)
            }
        } finally {
            driver.finishRow()
        }
    }

    private fun writeData(data: Sequence<T>) {
        // Pre-compute per-column base styles (default + column style) once.
        // This avoids repeating the same merge O(rows) times per column.
        val columnBaseStyles: List<ExcelStyle?> = columns.map { col ->
            defaultStyle?.merge(col.style) ?: col.style
        }

        data.forEachIndexed { rowIdx, item ->
            val rowNum = rowIdx + 1 // Row 0 is the header
            driver.startRow(rowNum, null)
            try {
                val rowStyle = try {
                    _rowStyleFn?.invoke(rowIdx, item)
                } catch (e: Exception) {
                    throw ExcelStreamingException("Error evaluating rowStyle at row $rowIdx: ${e.message}", e)
                }

                columns.forEachIndexed { colIdx, col ->
                    val dynamicCellStyle = try {
                        col.cellStyle?.invoke(item)
                    } catch (e: Exception) {
                        throw ExcelStreamingException(
                            "Error evaluating cellStyle for column '${col.header}' at row $rowIdx: ${e.message}",
                            e
                        )
                    }

                    // Optimized style merging: avoid merge() if possible
                    val baseStyle = columnBaseStyles[colIdx]
                    val cellStyle = when {
                        baseStyle == null -> rowStyle?.merge(dynamicCellStyle) ?: dynamicCellStyle
                        rowStyle == null -> baseStyle.merge(dynamicCellStyle)
                        else -> baseStyle.merge(rowStyle).merge(dynamicCellStyle)
                    }

                    if (col.formulaExtractor != null) {
                        val formula = try {
                            col.formulaExtractor.invoke(rowIdx, item)
                        } catch (e: Exception) {
                            throw ExcelStreamingException(
                                "Error extracting formula for column '${col.header}' at row $rowIdx: ${e.message}",
                                e
                            )
                        }
                        driver.writeFormula(colIdx, formula, cellStyle)
                    } else if (col.valueExtractor != null) {
                        val value = try {
                            col.valueExtractor.invoke(item)
                        } catch (e: Exception) {
                            throw ExcelStreamingException(
                                "Error extracting value for column '${col.header}' at row $rowIdx: ${e.message}",
                                e
                            )
                        }
                        val link = try {
                            col.linkExtractor?.invoke(item)
                        } catch (e: Exception) {
                            throw ExcelStreamingException(
                                "Error extracting link for column '${col.header}' at row $rowIdx: ${e.message}",
                                e
                            )
                        }
                        driver.writeCell(colIdx, value, cellStyle, link)
                    }
                }
            } finally {
                driver.finishRow()
            }
        }
    }
}
