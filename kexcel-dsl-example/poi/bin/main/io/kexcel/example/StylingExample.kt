package io.kexcel.example

import io.kexcel.core.excel
import io.kexcel.style.*
import java.io.OutputStream

/**
 * Styling Example: Comprehensive demonstration of the styling system.
 */
fun runStylingExample(output: OutputStream) {
    excel(output) {
        defaultStyle = ExcelStyle(
            font = ExcelFont(size = 11),
            alignment = ExcelAlignment(vertical = VerticalAlign.CENTER)
        )

        sheet("Advanced Styling") {
            columnWidth(0 to 4000, 1 to 8000, 2 to 6000)

            row(height = 30.0) {
                cell(value = "Inheritance", style = ExcelStyle(font = ExcelFont(bold = true, size = 14)))
                cell(value = "This cell inherits Workbook font and vertical alignment.")
            }

            row {
                cell(value = "Alignment")
                cell(value = "Right Aligned", style = ExcelStyle(alignment = ExcelAlignment(horizontal = HorizontalAlign.RIGHT)))
                cell(value = "Center Aligned", style = ExcelStyle(alignment = ExcelAlignment(horizontal = HorizontalAlign.CENTER)))
            }

            row {
                cell(value = "Colors")
                cell(value = "Blue Background", style = ExcelStyle(background = ExcelBackground(color = "#D9EAD3")))
                cell(value = "Red Text", style = ExcelStyle(font = ExcelFont(color = "#FF0000")))
            }

            row {
                cell(value = "Borders")
                cell(
                    value = "Thin Border",
                    style = ExcelStyle(border = ExcelBorder(all = BorderStyle.THIN))
                )
            }

            row(height = 40.0) {
                cell(value = "Text Wrap")
                cell(
                    value = "This is a very long text that should be wrapped within the cell because wrapText is enabled.",
                    style = ExcelStyle(wrapText = true)
                )
            }
        }

        sheet("Green Sheet", defaultStyle = ExcelStyle(background = ExcelBackground(color = "#E6FFFA"))) {
            row {
                cell(value = "Every cell in this sheet has a light green background by default.")
            }
            row {
                cell(value = "Unless overridden", style = ExcelStyle(background = ExcelBackground(color = "#FFFFFF")))
            }
        }
    }
}
