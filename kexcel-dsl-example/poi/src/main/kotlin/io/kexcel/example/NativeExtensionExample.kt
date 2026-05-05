package io.kexcel.example

import io.kexcel.core.excel
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.ss.util.CellRangeAddress
import java.io.OutputStream

/**
 * Native Extension Example: Demonstrates how to access POI-specific features.
 * Features shown: Freeze Pane and Auto Filter.
 */
fun runNativeExtensionExample(output: OutputStream) {
    excel(output) {
        sheet("Native Features") {
            // 1. Freeze the top row (Call BEFORE rows for safety)
            nativeSheet<SXSSFSheet> { sheet ->
                sheet.createFreezePane(0, 1)
            }

            row {
                cell(value = "ID")
                cell(value = "Name")
                cell(value = "Status")
            }

            row { cell(value = 1); cell(value = "Task A"); cell(value = "Done") }
            row { cell(value = 2); cell(value = "Task B"); cell(value = "Pending") }

            // 2. Add Auto Filter (Call AFTER rows is fine for metadata)
            nativeSheet<SXSSFSheet> { sheet ->
                sheet.setAutoFilter(CellRangeAddress(0, 2, 0, 2))
            }
        }
    }
}
