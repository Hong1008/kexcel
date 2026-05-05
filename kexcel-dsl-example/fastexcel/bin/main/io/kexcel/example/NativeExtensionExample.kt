package io.kexcel.example

import io.kexcel.core.excel
import org.dhatim.fastexcel.Worksheet
import java.io.OutputStream

/**
 * Native Extension Example: Demonstrates how to access FastExcel-specific features.
 * Features shown: Auto Filter.
 */
fun runNativeExtensionExample(output: OutputStream) {
    excel(output) {
        sheet("Native Features") {
            row {
                cell(value = "ID")
                cell(value = "Name")
            }

            row { cell(value = 1); cell(value = "Alice") }
            row { cell(value = 2); cell(value = "Bob") }

            // FastExcel native auto filter
            nativeSheet<Worksheet> { ws ->
                ws.setAutoFilter(0, 0, 2, 1)
            }
        }
    }
}
