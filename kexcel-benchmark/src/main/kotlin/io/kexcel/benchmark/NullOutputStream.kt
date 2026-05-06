package io.kexcel.benchmark

import java.io.OutputStream

/**
 * An [OutputStream] that discards all data written to it.
 * Used in benchmarks to eliminate I/O overhead and measure pure DSL/Engine performance.
 */
class NullOutputStream : OutputStream() {
    override fun write(b: Int) {
        // Do nothing
    }

    override fun write(b: ByteArray) {
        // Do nothing
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        // Do nothing
    }
}
