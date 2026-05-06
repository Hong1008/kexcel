package io.kexcel.driver

/**
 * Configuration options for the Excel workbook generation.
 *
 * This DTO centralizes metadata and engine-specific configurations to keep the [ExcelDriver]
 * interface clean and avoid repetitive method additions for new settings.
 */
data class WorkbookOptions(
    /**
     * Interval (in rows) at which the streaming engine should flush data to the output stream.
     * Higher values improve performance but increase memory usage.
     * Default is 1000 rows.
     */
    val flushInterval: Int = 1000,

    /**
     * Whether to force formula recalculation when the workbook is opened in an Excel viewer.
     * Default is false.
     */
    val forceFormulaRecalculation: Boolean = false,

    /**
     * Application name to be embedded in the Excel metadata.
     */
    val applicationName: String = "KExcel",

    /**
     * Application version to be embedded in the Excel metadata.
     */
    val applicationVersion: String = "1.0"
)
