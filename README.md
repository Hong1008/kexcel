# KExcel DSL
[![Release](https://jitpack.io/v/Hong1008/kexcel.svg)](https://jitpack.io/#Hong1008/kexcel)
![Java Version](https://img.shields.io/badge/Java-21-orange)
![Kotlin Version](https://img.shields.io/badge/Kotlin-2.3-purple)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

English | [한국어](README.ko.md)

**Kotlin DSL for Excel generation — engine-agnostic, streaming-first, type-safe.**

---

## Philosophy

Excel file generation is essential for most business applications, but existing libraries often burden developers with low-level APIs. Opening a workbook, creating sheets and rows, writing to cells, and manually managing style objects — this verbose and repetitive code often obscures business logic.

KExcel DSL follows three core principles to solve this:

1. **Unified Developer Experience**: Focus on business logic through a declarative DSL that remains consistent regardless of the underlying engine. The same code works seamlessly across any supported engine.

2. **Performance without Compromise**: Enjoy the convenience of high-level abstraction while maintaining near-native execution speeds and constant memory usage through optimized streaming and function inlining.

3. **Pragmatic Abstraction**: We don't trap you in our abstraction. While the DSL covers most common use cases, you always have direct access to the native engine via extension points to leverage its full power.

---

## Features

- ✅ **Kotlin DSL** — Intuitive builder structure: `excel { sheet { row { cell() } } }`
- ✅ **Engine Abstraction** — Auto-detection and seamless switching between Apache POI and FastExcel
- ✅ **DTO Binding** — `dataSheet("Users", users) { column("Name") { it.name } }`
- ✅ **Style Inheritance** — Hierarchical style inheritance: Workbook → Sheet → Row → Cell
- ✅ **Streaming** — `Sequence`-based large data processing (100K+ rows)
- ✅ **Native Extension** — Access engine-specific features via `nativeSheet<SXSSFSheet> { ... }`
- ✅ **Thread Safety** — Concurrent write detection and Fail-Fast mechanism

---

## Choosing Your Engine

KExcel DSL gives you the freedom to choose the engine that best fits each use case without changing your code.

|  | [Apache POI](https://github.com/apache/poi) | [FastExcel](https://github.com/dhatim/fastexcel) |
|---|---|---|
| JAR Size | ~30MB (incl. transitive deps) | ~150KB |
| Write Performance | Standard (SXSSF) | High-speed (Up to 1.3x faster) |
| Memory Efficiency | Streaming | Streaming |
| Formula Evaluation | ✅ | ❌ |
| Charts / Graphs | ✅ | ❌ |
| Pivot Tables | ✅ | ❌ |
| Serverless / Container Fit | Heavy | Lightweight |

### 🐘 [Apache POI](https://github.com/apache/poi) (SXSSF)
*Industry Standard*
- **Best for**: Reports requiring complex styling, formula evaluation, charts/graphs, pivot tables, and other advanced Excel features.
- **Strength**: 20 years of compatibility and a broad feature set.
- **Trade-off**: Transitive dependencies (~30MB total JAR size), which may impact cold-start times in serverless/container environments.

### 🏎️ [FastExcel](https://github.com/dhatim/fastexcel)
*High-Performance & Lightweight*
- **Best for**: Large datasets (millions of rows) and high-concurrency environments where processing speed and memory are prioritized.
- **Strength**: Small footprint at ~150KB. Benchmark shows approximately 25-30% higher throughput compared to POI SXSSF for 1,000,000 rows. Ideal for Lambda, Cloud Run, and other serverless environments.
- **Trade-off**: Does not support formula evaluation, charts, pivot tables, or modifying existing files.

**Why choose KExcel?** KExcel allows you to use the same DSL to generate feature-rich reports with POI and high-performance large files with FastExcel. You maintain a single, consistent codebase while choosing the best-fit engine for each specific requirement. Detailed results can be found in the [Benchmark Report](docs/BENCHMARK.md).

---


## Quick Start

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Hong1008.kexcel:kexcel-dsl:0.1.0")
    
    // Choose ONE engine:
    implementation("org.apache.poi:poi-ooxml:5.5.1")       // Apache POI
    // OR
    implementation("org.dhatim:fastexcel:0.20.0")           // FastExcel
}
```

> **Note:** `kexcel-dsl` has a `compileOnly` dependency on engines. You must explicitly declare your preferred engine as an `implementation` dependency in your project.

---

## Usage

### Basic — Sheet, Row, Cell

The simplest way to generate an Excel file. Declare sheets, rows, and cells within an `excel` block.

```kotlin
import io.kexcel.core.excel

File("report.xlsx").outputStream().use { output ->
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
        }
    }
}
```

### DTO Binding — `dataSheet`

Automatically binds collection data to header and data rows. You can declaratively define per-column styles, conditional cell styles, alternating row colors, etc.

```kotlin
data class Product(val id: Long, val name: String, val price: Double, val stock: Int)

excel(output) {
    dataSheet("Inventory", products) {
        val headerStyle = ExcelStyle(
            background = ExcelBackground(color = "#4F81BD"),
            font = ExcelFont(bold = true, color = "#FFFFFF"),
            alignment = ExcelAlignment(horizontal = HorizontalAlign.CENTER)
        )

        column(header = "ID", headerStyle = headerStyle) { it.id }
        column(header = "Name", headerStyle = headerStyle) { it.name }
        column(
            header = "Price",
            headerStyle = headerStyle.merge(ExcelStyle(dataFormat = "$#,##0.00"))
        ) { it.price }
        
        // Conditional cell styling: red text when out of stock
        column(
            header = "Stock",
            headerStyle = headerStyle,
            cellStyle = {
                if (it.stock == 0) ExcelStyle(font = ExcelFont(color = "#FF0000", bold = true))
                else null
            }
        ) { it.stock }

        // Alternating row colors
        rowStyle { index, _ ->
            if (index % 2 == 1) ExcelStyle(background = ExcelBackground(color = "#F2F2F2"))
            else null
        }
    }
}
```

### Styling — Hierarchical Inheritance

Styles are inherited in the order of **Workbook → Sheet → Row → Cell**, with lower levels overriding higher ones.

```kotlin
excel(output) {
    // 1. Workbook-level default
    defaultStyle = ExcelStyle(
        font = ExcelFont(size = 11),
        alignment = ExcelAlignment(vertical = VerticalAlign.CENTER)
    )

    sheet("Styled") {
        columnWidth(0 to 4000, 1 to 8000)

        row(height = 30.0) {
            cell(value = "Bold Title", style = ExcelStyle(font = ExcelFont(bold = true, size = 14)))
            cell(value = "Inherits workbook font size and vertical alignment.")
        }

        row {
            cell(value = "Borders", style = ExcelStyle(border = ExcelBorder(all = BorderStyle.THIN)))
            cell(value = "Top only thick", style = ExcelStyle(
                border = ExcelBorder(all = BorderStyle.THIN, top = BorderStyle.THICK)
            ))
        }
    }

    // 2. Sheet-level default overrides workbook default
    sheet("Green Sheet", defaultStyle = ExcelStyle(background = ExcelBackground(color = "#E6FFFA"))) {
        row { cell(value = "Every cell here has a green background.") }
        row { cell(value = "Unless overridden", style = ExcelStyle(background = ExcelBackground(color = "#FFFFFF"))) }
    }
}
```

### Streaming — Large Data with `Sequence`

The `rows()` function takes a `Sequence<T>` and processes it row by row. Memory usage remains constant regardless of data size.

```kotlin
val largeData = generateSequence(1) { it + 1 }
    .take(100_000)
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
```

### Configuration — `WorkbookOptions`

Manage flush intervals and metadata settings via `WorkbookOptions`.

```kotlin
val options = WorkbookOptions(
    flushInterval = 5000,           // Flush every 5000 rows (Default is 1000)
    forceFormulaRecalculation = true, // Force formula recalculation on open
    applicationName = "MyReporter",
    applicationVersion = "2.0"
)

excel(output, options = options) {
    sheet("Configured") {
        row { cell(formula = "A1+B1") }
    }
}
```

---

### Native Extension — Engine-Specific Features

Directly access engine-specific features not abstracted by the DSL via `nativeSheet<T>` / `nativeWorkbook<T>`. If an incorrect type is provided, the block is silently ignored, **ensuring code stability even when switching engines.**

```kotlin
// Apache POI: Freeze Pane + Auto Filter
excel(output) {
    sheet("Sales") {
        nativeSheet<SXSSFSheet> { sheet ->
            sheet.createFreezePane(0, 1)  // Freeze header row
        }

        row { cell(value = "ID"); cell(value = "Name"); cell(value = "Status") }
        row { cell(value = 1); cell(value = "Task A"); cell(value = "Done") }

        nativeSheet<SXSSFSheet> { sheet ->
            sheet.setAutoFilter(CellRangeAddress(0, 2, 0, 2))
        }
    }
}
```

> ⚠️ **Streaming Engine Caution:** Streaming engines (POI SXSSF, FastExcel) periodically flush rows to disk. Flushed rows cannot be modified even via native objects. It is safe to call settings like Freeze Pane **before rows**, and metadata like Auto Filter **after rows**.

### Multi-Sheet Report

Combine multiple sheets within a single `excel` block to create complex, multi-faceted reports.

```kotlin
excel(output) {
    sheet("Cover") {
        row(rowNum = 2, height = 40.0) {
            cell(value = "SALES REPORT", style = ExcelStyle(
                font = ExcelFont(bold = true, size = 20),
                alignment = ExcelAlignment(horizontal = HorizontalAlign.CENTER)
            ))
        }
        mergeCells(2, 2, 0, 3)
        row(rowNum = 4) {
            cell(value = "Website:")
            cell(value = "KExcel", link = "https://github.com/hong1008/kexcel")
        }
    }

    dataSheet("Sales Data", salesData) {
        column("Date") { it.date }
        column("Region") { it.region }
        column("Amount") { it.amount }
    }

    sheet("Summary") {
        row {
            cell(value = "Total")
            cell(value = salesData.sumOf { it.amount })
        }
    }
}
```

---

## Architecture

```
┌──────────────────────────────────────────────┐
│              User Code (DSL)                 │
│  excel(output) { sheet { row { cell() } } }  │
└──────────────────┬───────────────────────────┘
                   │
┌──────────────────▼───────────────────────────┐
│          WorkbookScope / SheetScope          │
│          DataSheetScope / RowScope           │
│         (Style Inheritance & Merge)          │
└──────────────────┬───────────────────────────┘
                   │
┌──────────────────▼───────────────────────────┐
│           ExcelDriver (Interface)            │
│  startWorkbook / startSheet / writeCell ...  │
│  nativeWorkbook() / nativeSheet()            │
└────────┬─────────────────────────┬───────────┘
         │                         │
┌────────▼────────┐     ┌─────────▼──────────┐
│   PoiDriver     │     │  FastExcelDriver   │
│  (SXSSFWorkbook)│     │  (org.dhatim)      │
└─────────────────┘     └────────────────────┘
```

**Engine Auto-Detection:** `ExcelDriverFactory.autoDetect()` scans the classpath to automatically select an available engine. To specify an engine explicitly, use `excel(output, driver = PoiDriver()) { ... }`.

---

## Module Structure

```
kexcel/                          # Root project
├── kexcel-dsl/                  # Core library module
│   └── src/main/kotlin/io/kexcel/
│       ├── core/                # DSL entry point & scopes
│       ├── driver/              # ExcelDriver interface & implementations
│       └── style/               # Domain-independent style model
├── kexcel-dsl-example/          # Example modules (folder only)
│   ├── poi/                     # Examples using Apache POI
│   └── fastexcel/               # Examples using FastExcel
├── gradle/libs.versions.toml   # Centralized version catalog
└── settings.gradle.kts
```

---

## Requirements

- **JDK** 21+
- **Kotlin** 2.3+
- **Engine** (one of):
  - Apache POI 5.5+
  - FastExcel 0.20+

---

## License

[MIT License](LICENSE)
