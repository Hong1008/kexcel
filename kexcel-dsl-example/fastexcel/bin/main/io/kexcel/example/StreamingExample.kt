package io.kexcel.example

import io.kexcel.core.excel
import io.kexcel.style.ExcelFont
import io.kexcel.style.ExcelStyle
import java.io.OutputStream

/**
 * Streaming Example: Demonstrates handling large datasets with low memory footprint.
 */
fun runStreamingExample(output: OutputStream) {
    val rowCount = 100_000
    val largeData = generateSequence(1) { it + 1 }
        .take(rowCount)
        .map { i -> "Item #$i" to (Math.random() * 1000) }

    excel(output) {
        sheet("Large Data") {
            row {
                cell(value = "ID", style = ExcelStyle(font = ExcelFont(bold = true)))
                cell(value = "Value", style = ExcelStyle(font = ExcelFont(bold = true)))
            }
            
            rows(largeData) { (name, value) ->
                cell(value = name)
                cell(value = value)
            }
        }
    }
}
