package io.kexcel.example

import io.kexcel.core.excel
import io.kexcel.style.*
import java.io.OutputStream
import java.time.LocalDate

data class Product(
    val id: Long,
    val name: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val lastRestocked: LocalDate
)

/**
 * DataSheet Example: Demonstrates DTO binding, conditional styling, and data formatting.
 */
fun runDataSheetExample(output: OutputStream) {
    val products = listOf(
        Product(101, "Laptop", "Electronics", 1200.0, 5, LocalDate.of(2023, 12, 1)),
        Product(102, "Mouse", "Electronics", 25.0, 50, LocalDate.of(2023, 11, 15)),
        Product(103, "Desk Chair", "Furniture", 150.0, 0, LocalDate.of(2023, 10, 10)),
        Product(104, "Monitor", "Electronics", 300.0, 2, LocalDate.of(2023, 12, 5)),
        Product(105, "Bookshelf", "Furniture", 85.0, 12, LocalDate.of(2023, 9, 20))
    )

    excel(output) {
        dataSheet("Inventory", products) {
            val headerStyle = ExcelStyle(
                background = ExcelBackground(color = "#4F81BD"),
                font = ExcelFont(bold = true, color = "#FFFFFF"),
                alignment = ExcelAlignment(horizontal = HorizontalAlign.CENTER)
            )

            column(header = "ID", headerStyle = headerStyle) { it.id }
            column(header = "Product Name", headerStyle = headerStyle) { it.name }
            column(header = "Category", headerStyle = headerStyle) { it.category }

            column(
                header = "Price",
                headerStyle = headerStyle.merge(ExcelStyle(dataFormat = "$#,##0.00"))
            ) { it.price }

            column(
                header = "Stock",
                headerStyle = headerStyle,
                cellStyle = {
                    if (it.stock == 0) ExcelStyle(font = ExcelFont(color = "#FF0000", bold = true))
                    else null
                }
            ) { it.stock }

            column(
                header = "Last Restocked",
                headerStyle = headerStyle.merge(ExcelStyle(dataFormat = "yyyy-mm-dd"))
            ) { it.lastRestocked }

            rowStyle { index, _ ->
                if (index % 2 == 1) ExcelStyle(background = ExcelBackground(color = "#F2F2F2"))
                else null
            }
        }
    }
}
