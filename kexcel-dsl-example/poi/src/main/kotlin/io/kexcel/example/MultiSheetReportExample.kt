package io.kexcel.example

import io.kexcel.core.excel
import io.kexcel.style.*
import java.io.OutputStream
import java.time.LocalDate

data class SalesEntry(val date: LocalDate, val amount: Double, val region: String)

/**
 * MultiSheet Report Example: A complex, real-world report with a cover, data, and summary.
 */
fun runMultiSheetReportExample(output: OutputStream) {
    val salesData = listOf(
        SalesEntry(LocalDate.of(2023, 10, 1), 1200.0, "North"),
        SalesEntry(LocalDate.of(2023, 10, 2), 850.0, "South"),
        SalesEntry(LocalDate.of(2023, 10, 3), 2100.0, "North"),
        SalesEntry(LocalDate.of(2023, 10, 4), 1500.0, "East")
    )

    excel(output) {
        sheet("Cover") {
            columnWidth(0 to 8000)
            row(rowNum = 2, height = 40.0) {
                cell(col = 0, value = "SALES PERFORMANCE REPORT", style = ExcelStyle(
                    font = ExcelFont(bold = true, size = 20),
                    alignment = ExcelAlignment(horizontal = HorizontalAlign.CENTER)
                ))
            }
            mergeCells(2, 2, 0, 3)

            row(rowNum = 4) {
                cell(value = "Generated Date:")
                cell(value = LocalDate.now().toString())
            }
            
            row {
                cell(value = "Official Website:")
                cell(value = "KExcel Website", link = "https://github.com/hong1008/kexcel")
            }
        }

        dataSheet("Sales Data", salesData) {
            val headerStyle = ExcelStyle(
                background = ExcelBackground(color = "#4F81BD"),
                font = ExcelFont(bold = true, color = "#FFFFFF")
            )
            column("Date", headerStyle) { it.date }
            column("Region", headerStyle) { it.region }
            column("Amount", headerStyle.merge(ExcelStyle(dataFormat = "#,##0.00"))) { it.amount }
        }

        sheet("Summary") {
            row {
                cell(value = "Total Region Count")
                cell(value = salesData.map { it.region }.distinct().size)
            }
            row {
                cell(value = "Total Amount")
                cell(value = salesData.sumOf { it.amount }, style = ExcelStyle(dataFormat = "$#,##0.00", font = ExcelFont(bold = true)))
            }
        }
    }
}
