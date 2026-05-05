package io.kexcel.core

import io.kexcel.driver.ExcelDriver
import io.kexcel.driver.ExcelDriverFactory
import io.kexcel.style.ExcelStyle
import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock

/**
 * DSL Marker for the Excel DSL to prevent nested builder calls.
 */
@DslMarker
annotation class ExcelDslMarker

/**
 * Base class for all Excel DSL builders, providing thread-safety checks.
 *
 * <p>Excel generation is inherently sequential. This base class ensures that
 * multiple threads do not attempt to write to the same builder concurrently.
 *
 * <p><b>Thread Safety:</b> This class is NOT thread-safe for concurrent writes.
 * It uses a [ReentrantLock] to detect and fail fast upon concurrent access from different threads,
 * while allowing reentrant calls from the same thread.
 */
@ExcelDslMarker
abstract class BaseScope(@PublishedApi internal val driver: ExcelDriver) {
    protected val writeLock = ReentrantLock()

    /**
     * Executes the given [block] while ensuring exclusive access to the scope.
     * @throws ExcelConcurrentWriteException if another thread is already writing to this builder instance
     */
    @PublishedApi
    internal fun <T> writeSafely(block: () -> T): T {
        if (!writeLock.tryLock()) {
            throw ExcelConcurrentWriteException("Concurrent write detected! Excel DSL builders are not thread-safe and cannot be shared between threads.")
        }
        return try {
            block()
        } finally {
            writeLock.unlock()
        }
    }
}

/**
 * Scope for creating a workbook.
 *
 * <p>The [WorkbookScope] is the root of the DSL and manages the lifecycle of the driver.
 *
 * @param engine the underlying [ExcelDriver] to use for generation
 */
@ExcelDslMarker
class WorkbookScope(driver: ExcelDriver) : BaseScope(driver) {
    /** Global default style applied to all sheets in this workbook. */
    var defaultStyle: ExcelStyle? = null

    /**
     * Defines a new sheet in the workbook.
     * <p>This operation starts a new sheet context in the driver.
     * @param name the name of the sheet
     * @param defaultStyle the optional default style applied specifically to this sheet
     * @param init the DSL block for configuring the sheet
     * @see SheetScope
     */
    fun sheet(name: String, defaultStyle: ExcelStyle? = null, init: SheetScope.() -> Unit) = writeSafely {
        driver.startSheet(name)
        val mergedStyle = this.defaultStyle?.merge(defaultStyle) ?: defaultStyle
        SheetScope(driver, mergedStyle).apply(init)
        driver.finishSheet()
    }

    /**
     * Defines a data sheet in the workbook using DTO binding.
     * <p>Automatically generates a header row followed by data rows based on the provided [data].
     * @param name the name of the sheet
     * @param data the sequence of items to bind to rows
     * @param defaultStyle optional default style for this sheet
     * @param init DSL block to configure columns and row styles
     * @see DataSheetScope
     */
    fun <T> dataSheet(
        name: String,
        data: Sequence<T>,
        defaultStyle: ExcelStyle? = null,
        init: DataSheetScope<T>.() -> Unit
    ) = writeSafely {
        driver.startSheet(name)
        val mergedStyle = this.defaultStyle?.merge(defaultStyle) ?: defaultStyle
        val scope = DataSheetScope<T>(driver, mergedStyle)
        scope.apply(init)
        scope.writeTo(data)
        driver.finishSheet()
    }

    /**
     * Defines a data sheet using an [Iterable] (e.g., List, Set).
     * @see dataSheet
     */
    fun <T> dataSheet(
        name: String,
        data: Iterable<T>,
        defaultStyle: ExcelStyle? = null,
        init: DataSheetScope<T>.() -> Unit
    ) {
        dataSheet(name, data.asSequence(), defaultStyle, init)
    }

    /**
     * Provides access to the underlying native workbook object.
     *
     * @param T the expected native workbook type
     * @param block the callback receiving the native workbook
     */
    inline fun <reified T> nativeWorkbook(crossinline block: (T) -> Unit) = writeSafely {
        val native = driver.nativeWorkbook()
        if (native is T) block(native)
    }
}


/**
 * Entry point for the Excel DSL.
 *
 * <p>Starts the Excel generation process. If an [engine] is not provided,
 * it will be automatically detected based on the classpath dependencies.
 *
 * <pre>
 * // Auto-detect engine:
 * excel(response.outputStream) {
 *     sheet("Inventory") { ... }
 * }
 *
 * // Explicit engine:
 * excel(response.outputStream, driver = PoiDriver()) { ... }
 * </pre>
 *
 * @param output the stream to write the final Excel file
 * @param driver the [ExcelDriver] implementation. Defaults to auto-detection.
 * @param defaultStyle optional global default style for the entire workbook
 * @param init the configuration block using [WorkbookScope]
 * @throws ExcelDslException or its subclasses if generation fails
 * @see ExcelDriverFactory.autoDetect
 */
fun excel(
    output: OutputStream,
    driver: ExcelDriver = ExcelDriverFactory.autoDetect(),
    defaultStyle: ExcelStyle? = null,
    forceFormulaRecalculation: Boolean = false,
    init: WorkbookScope.() -> Unit
) {
    val scope = WorkbookScope(driver)
    scope.defaultStyle = defaultStyle
    driver.startWorkbook(output)
    if (forceFormulaRecalculation) {
        driver.setForceFormulaRecalculation(true)
    }
    try {
        scope.apply(init)
    } catch (e: Exception) {
        when (e) {
            is ExcelDslException -> throw e
            is IllegalArgumentException, is IllegalStateException -> throw e
            else -> throw ExcelStreamingException("Error during excel generation: ${e.message}", e)
        }
    } finally {
        driver.finishWorkbook()
    }
}