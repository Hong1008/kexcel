package io.kexcel.example

import org.junit.jupiter.api.Test
import java.io.File

class ExampleRunnerTest {

    @Test
    fun `generate basic example`() {
        runExample("fastexcel_basic.xlsx") { runBasicExample(it) }
    }

    @Test
    fun `generate datasheet example`() {
        runExample("fastexcel_datasheet.xlsx") { runDataSheetExample(it) }
    }

    @Test
    fun `generate styling example`() {
        runExample("fastexcel_styling.xlsx") { runStylingExample(it) }
    }

    @Test
    fun `generate streaming example`() {
        runExample("fastexcel_streaming.xlsx") { runStreamingExample(it) }
    }

    @Test
    fun `generate multisheet report example`() {
        runExample("fastexcel_multisheet.xlsx") { runMultiSheetReportExample(it) }
    }

    @Test
    fun `generate native extension example`() {
        runExample("fastexcel_native.xlsx") { runNativeExtensionExample(it) }
    }

    private fun runExample(fileName: String, block: (java.io.OutputStream) -> Unit) {
        val file = File(fileName)
        file.outputStream().use { block(it) }
        println("Generated example: ${file.absolutePath}")
    }
}
