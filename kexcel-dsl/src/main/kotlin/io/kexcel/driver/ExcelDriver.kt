package io.kexcel.driver

import io.kexcel.style.ExcelStyle
import java.io.OutputStream

/**
 * Strategy interface for interacting with different Excel libraries.
 *
 * <p>This interface abstracts the core operations of Excel generation, allowing
 * the DSL to remain agnostic of the underlying engine (e.g., Apache POI, FastExcel).
 *
 * <p>Implementations must handle the lifecycle of workbooks, sheets, and rows
 * in a streaming-compatible way.
 *
 * <p><b>Thread Safety:</b> Implementations of this interface are generally
 * <b>not</b> thread-safe. Synchronization should be handled at the DSL or application level.
 *
 * @see io.kexcel.core.excel
 */
interface ExcelDriver {

    /**
     * Initialize the workbook and associate it with the provided [outputStream].
     * <p>This must be the first method called in the lifecycle of an driver.
     * @param outputStream the stream where the generated Excel file will be written
     * @throws IllegalStateException if the workbook is already started
     */
    fun startWorkbook(outputStream: OutputStream)

    /**
     * Finalize the workbook, write to the output stream, and release resources.
     * <p>After this call, no further operations on this engine instance are allowed.
     * @throws IllegalStateException if the workbook has not been started
     */
    fun finishWorkbook()

    /**
     * Create a new sheet with the given [name].
     * <p>Must be invoked after {@link #startWorkbook(OutputStream)}.
     * @param name the name of the sheet
     * @throws IllegalStateException if called before startWorkbook
     */
    fun startSheet(name: String)

    /**
     * Finalize the current sheet.
     * <p>Must be invoked after {@link #startSheet(String)}.
     */
    fun finishSheet()

    /**
     * Set the width of a specific column.
     * <p>Usually called after {@link #startSheet(String)} but before rows are processed.
     * @param col the zero-based column index
     * @param width the width in units (interpretation may vary by engine implementation)
     */
    fun setColumnWidth(col: Int, width: Int)

    /**
     * Merge a range of cells into one.
     * <p><b>Note:</b> In streaming engines, all rows involved in the merge
     * must still be in the memory window.
     * @param firstRow zero-based index of the first row
     * @param lastRow zero-based index of the last row
     * @param firstCol zero-based index of the first column
     * @param lastCol zero-based index of the last column
     */
    fun mergeCells(firstRow: Int, lastRow: Int, firstCol: Int, lastCol: Int)

    /**
     * Start a new row at the specified [rowNum].
     * <p>Must be invoked after {@link #startSheet(String)}.
     * @param rowNum the zero-based row index
     * @param height the optional height of the row in points
     * @throws IllegalArgumentException if rowNum is negative
     */
    fun startRow(rowNum: Int, height: Double? = null)

    /**
     * Finalize the current row.
     * <p>Must be invoked after {@link #startRow(Int, Double?)}.
     */
    fun finishRow()

    /**
     * Write a value to a specific cell in the current row.
     * <p>Must be invoked between {@link #startRow(Int, Double?)} and {@link #finishRow()}.
     * @param col the zero-based column index
     * @param value the data to write (supports String, Number, Boolean, Date, and temporal types)
     * @param style the optional style to apply to the cell
     * @param link the optional hyperlink URL
     */
    fun writeCell(col: Int, value: Any?, style: ExcelStyle? = null, link: String? = null)

    /**
     * Write a formula to a specific cell in the current row.
     * <p>Must be invoked between {@link #startRow(Int, Double?)} and {@link #finishRow()}.
     * @param col the zero-based column index
     * @param formula the formula to write (e.g., "SUM(A1:B1)")
     * @param style the optional style to apply to the cell
     */
    fun writeFormula(col: Int, formula: String, style: ExcelStyle? = null)

    /**
     * Sets whether to force formula recalculation when the workbook is opened.
     * @param value true to force recalculation
     */
    fun setForceFormulaRecalculation(value: Boolean)

    /**
     * Returns the underlying native workbook object.
     * @return engine-specific workbook (e.g., SXSSFWorkbook for POI, Workbook for FastExcel)
     */
    fun nativeWorkbook(): Any?

    /**
     * Returns the currently active native sheet object.
     * @return engine-specific sheet (e.g., SXSSFSheet for POI, Worksheet for FastExcel)
     */
    fun nativeSheet(): Any?
}
