package io.kexcel.example

import io.kexcel.core.excel
import java.io.OutputStream
import java.time.LocalDate

/**
 * Basic Example: Demonstrates basic sheet, row, and cell operations.
 */
fun runBasicExample(output: OutputStream) {
    excel(output) {
        sheet("Simple Sheet") {
            row {
                cell(value = "Hello")
                cell(value = "KExcel")
            }
            row {
                cell(value = "Date")
                cell(value = LocalDate.now())
            }
            row {
                cell(value = "Number")
                cell(value = 123.45)
            }
            row {
                cell(value = "Boolean")
                cell(value = true)
            }
            row {
                cell(value = "Formula (SUM)")
                cell(value = 100)
                cell(value = 200)
                cell(formula = "SUM(B5:C5)")
            }
        }
    }
}
