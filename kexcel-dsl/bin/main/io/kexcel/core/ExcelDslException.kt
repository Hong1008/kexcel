package io.kexcel.core

/**
 * Base exception for all errors occurring within the Excel DSL.
 *
 * <p>This is a sealed class, ensuring that all DSL-related exceptions are
 * part of a controlled hierarchy.
 */
sealed class ExcelDslException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when an error occurs during Excel streaming operations.
 *
 * <p>Common causes include:
 * <ul>
 *   <li>Attempting to write to a row index smaller than the current pointer.</li>
 *   <li>Underlying engine-specific I/O errors.</li>
 * </ul>
 *
 * @see SheetScope.row
 * @see SheetScope.rows
 */
class ExcelStreamingException(message: String, cause: Throwable? = null) : ExcelDslException(message, cause)

/**
 * Thrown when concurrent write access is detected in the DSL builders.
 *
 * <p>Excel DSL builders are not thread-safe. This exception is thrown
 * when [BaseScope] detects multiple threads attempting to call DSL methods
 * on the same builder instance.
 *
 * @see BaseScope
 */
class ExcelConcurrentWriteException(message: String) : ExcelDslException(message)

/**
 * Thrown when the DSL or engine is improperly configured.
 *
 * <p>Examples include initializing an engine that has already been started
 * or providing invalid configuration parameters.
 */
class ExcelConfigurationException(message: String) : ExcelDslException(message)
