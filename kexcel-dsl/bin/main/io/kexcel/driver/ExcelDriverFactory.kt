package io.kexcel.driver

/**
 * Factory for automatically detecting and creating [ExcelDriver] implementations
 * based on the presence of specific library classes on the classpath.
 *
 * <p>This allows the DSL to be used without explicitly specifying an engine,
 * providing a "plug-and-play" experience for developers.
 *
 * <p><b>Detection Priority:</b>
 * <ol>
 *   <li>Apache POI (PoiDriver)</li>
 *   <li>FastExcel (FastExcelDriver)</li>
 * </ol>
 */
object ExcelDriverFactory {
    private const val POI_CLASS = "org.apache.poi.xssf.streaming.SXSSFWorkbook"
    private const val FASTEXCEL_CLASS = "org.dhatim.fastexcel.Workbook"

    /**
     * Automatically detects an available Excel engine from the classpath.
     *
     * @return an instance of [ExcelDriver] compatible with the available dependencies
     * @throws IllegalStateException if no supported Excel engine dependency is found
     */
    fun autoDetect(): ExcelDriver {
        return when {
            isClassPresent(POI_CLASS) -> PoiDriver()
            isClassPresent(FASTEXCEL_CLASS) -> FastExcelDriver()
            else -> throw IllegalStateException(
                "No supported Excel engine found on the classpath. " +
                        "Please add at least one of the following dependencies: \n" +
                        "1. org.apache.poi:poi-ooxml\n" +
                        "2. org.dhatim:fastexcel"
            )
        }
    }

    private fun isClassPresent(className: String): Boolean {
        return try {
            Class.forName(className, false, ExcelDriverFactory::class.java.classLoader)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
