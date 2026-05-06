package io.kexcel.core

import io.kexcel.driver.ExcelDriver
import io.kexcel.style.ExcelStyle

/**
 * Scope for creating a sheet.
 *
 * <p>Supports column configuration, individual rows, and high-performance streaming of row sequences.
 *
 * @param driver the underlying [ExcelDriver]
 * @param defaultStyle the style inherited from workbook or sheet level
 */
@ExcelDslMarker
class SheetScope(
    driver: ExcelDriver,
    @PublishedApi
    internal val defaultStyle: ExcelStyle? = null
) : BaseScope(driver) {
    /**
     * The zero-based index of the next row to be processed in this sheet.
     * <p>This value increments automatically as rows are added.
     */
    @PublishedApi
    internal var nextRowNum: Int = 0

    @PublishedApi
    internal val rowScope = RowScope(driver)

    /**
     * Configures widths for specific columns.
     * @param widths pairs of column index and width (units depend on the driver implementation)
     */
    fun columnWidth(vararg widths: Pair<Int, Int>) = writeSafely {
        widths.forEach { (col, width) ->
            driver.setColumnWidth(col, width)
        }
    }

    @PublishedApi
    internal fun validateRowNum(target: Int) {
        if (target < nextRowNum) {
            throw ExcelStreamingException("Cannot write to row $target because row $nextRowNum has already been processed and flushed (Streaming mode requirement).")
        }
    }

    /**
     * Defines a single row.
     * <p>If [rowNum] is not provided, the index increments automatically based on the last written row.
     * @param rowNum the zero-based row index
     * @param height the optional height of the row in points
     * @param style the optional style to apply to this specific row
     * @param init the DSL block for configuring the row's cells
     * @throws ExcelStreamingException if [rowNum] is less than the current sequence pointer
     * @see RowScope
     */
    inline fun row(
        rowNum: Int? = null,
        height: Double? = null,
        style: ExcelStyle? = null,
        crossinline init: RowScope.() -> Unit
    ) = writeSafely {
        val targetRow = rowNum ?: nextRowNum
        validateRowNum(targetRow)

        driver.startRow(targetRow, height)
        val mergedStyle = this.defaultStyle?.merge(style) ?: style
        rowScope.reset(mergedStyle)
        rowScope.apply(init)
        driver.finishRow()

        nextRowNum = targetRow + 1
    }

    /**
     * Creates rows from a [Sequence] for high-performance, memory-efficient streaming.
     * <p>Ideal for processing large datasets from a database cursor or file.
     *
     * <pre>
     * // Example usage:
     * sheet("Sales") {
     *     rows(salesRepository.findAllAsSequence()) { sale ->
     *         cell(value = sale.id)
     *         cell(value = sale.amount)
     *     }
     * }
     * </pre>
     *
     * @param data the stream of data items to process
     * @param height the optional height for each generated row
     * @param style the optional style for each generated row
     * @param init the DSL block to map a data item to row cells
     * @throws ExcelStreamingException if data processing violates sequential row order
     */
    inline fun <T> rows(
        data: Sequence<T>,
        height: Double? = null,
        style: ExcelStyle? = null,
        crossinline init: RowScope.(T) -> Unit
    ) = writeSafely {
        val mergedStyle = this.defaultStyle?.merge(style) ?: style
        data.forEach { item ->
            driver.startRow(nextRowNum, height)
            rowScope.reset(mergedStyle)
            rowScope.init(item)
            driver.finishRow()
            nextRowNum++
        }
    }

    /**
     * Creates rows from an [Iterable] (e.g., List, Set).
     * <p>Note: Internally converted to [Sequence] for processing.
     * @see rows
     */
    fun <T> rows(
        data: Iterable<T>,
        height: Double? = null,
        style: ExcelStyle? = null,
        init: RowScope.(T) -> Unit
    ) {
        rows(data.asSequence(), height, style, init)
    }

    /**
     * Merges a range of cells within this sheet.
     *
     * <p><b>Memory Caution:</b> Merged region metadata is stored in memory until the workbook is finished.
     * Excessive merging (e.g., thousands of regions) can lead to OutOfMemoryError, especially with Apache POI.
     * For high-frequency merging, consider using FastExcel or increasing heap size.
     *
     * <p><b>Streaming Constraint:</b> Both [firstRow] and [lastRow] must be within the current memory
     * window (not yet flushed to disk).
     *
     * @param firstRow zero-based index of the first row
     * @param lastRow zero-based index of the last row
     * @param firstCol zero-based index of the first column
     * @param lastCol zero-based index of the last column
     */
    fun mergeCells(firstRow: Int, lastRow: Int, firstCol: Int, lastCol: Int) = writeSafely {
        driver.mergeCells(firstRow, lastRow, firstCol, lastCol)
    }

    /**
     * Provides access to the underlying native sheet object.
     *
     * <p><b>Streaming Engine Caution:</b>
     * Streaming engines (POI SXSSF, FastExcel) flush rows to disk periodically to save memory.
     * Once a row is flushed, it cannot be modified using the native object.
     *
     * <p><b>Safe Patterns:</b>
     * - Call <b>BEFORE</b> adding rows for: Freeze panes, sheet protection, zoom level, print setup.
     * - Call <b>AFTER</b> adding rows for: Auto filters (works on sheet metadata).
     *
     * <p><b>Unsafe Patterns:</b>
     * - Attempting to modify or read rows that have already been flushed.
     *
     * @param T the expected native sheet type
     * @param block the callback receiving the native sheet
     */
    inline fun <reified T> nativeSheet(crossinline block: (T) -> Unit) = writeSafely {
        val native = driver.nativeSheet()
        if (native is T) block(native)
    }
}

/**
 * Scope for creating a row.
 *
 * <p>Provides DSL methods for defining cells and applying inherited styles.
 *
 * @param driver the underlying [ExcelDriver]
 */
@ExcelDslMarker
class RowScope(driver: ExcelDriver) : BaseScope(driver) {
    /**
     * The zero-based index of the next column to be processed in the current row.
     * <p>This value increments automatically as cells are added.
     */
    @PublishedApi
    internal var nextColNum: Int = 0

    @PublishedApi
    internal var defaultStyle: ExcelStyle? = null

    @PublishedApi
    internal fun reset(defaultStyle: ExcelStyle?) {
        nextColNum = 0
        this.defaultStyle = defaultStyle
    }

    /**
     * Defines a cell in the current row.
     * <p>If [col] is not provided, the index increments automatically.
     * @param col the zero-based column index
     * @param value the data content (String, Number, Boolean, or Date types)
     * @param formula the optional Excel formula (e.g., "SUM(A1:B1)")
     * @param style the optional style to apply to this cell
     * @param link the optional hyperlink URL
     * @throws IllegalArgumentException if both value and formula are provided
     */
    fun cell(
        col: Int? = null,
        value: Any? = null,
        formula: String? = null,
        style: ExcelStyle? = null,
        link: String? = null
    ) = writeSafely {
        require(value == null || formula == null) {
            "A cell cannot have both a value and a formula. Please provide only one."
        }
        val targetCol = col ?: nextColNum
        val mergedStyle = if (style == null) this.defaultStyle else this.defaultStyle?.merge(style) ?: style

        if (formula != null) {
            driver.writeFormula(targetCol, formula, mergedStyle)
        } else {
            driver.writeCell(targetCol, value, mergedStyle, link)
        }

        nextColNum = targetCol + 1
    }
}