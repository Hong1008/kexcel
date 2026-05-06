# KExcel DSL

[English](README.md) | 한국어

**Kotlin DSL for Excel generation — engine-agnostic, streaming-first, type-safe.**

---

## Philosophy

Excel 파일 생성은 대부분의 비즈니스 애플리케이션에서 필수적이지만, 기존 라이브러리들은 저수준 API를 직접 다뤄야 하는 부담을 개발자에게 전가합니다. Workbook을 열고, Sheet를 만들고, Row를 생성하고, Cell에 값을 넣고, Style 객체를 만들어 적용하는 — 반복적이고 장황한 코드가 비즈니스 로직을 가립니다.

KExcel DSL은 이 문제를 해결하기 위해 세 가지 원칙을 따릅니다.

1. **일관된 개발 경험 (Unified Developer Experience)**: 엔진의 저수준 API에 종속되지 않고, 비즈니스 로직에만 집중할 수 있는 선언적 DSL을 제공합니다. 동일한 코드가 어떤 환경에서도 일관되게 작동하는 것을 보장합니다.

2. **타협 없는 성능 (Performance without Compromise)**: 추상화 레이어가 주는 편리함을 누리면서도, 스트리밍 처리와 수동 락 제어를 통한 Zero-allocation 최적화로 네이티브 엔진과 대등한 수준의 처리 속도와 메모리 안정성을 유지합니다.

3. **실용적인 추상화 (Pragmatic Abstraction)**: 개발자를 추상화에 가두지 않습니다. DSL의 편의성을 기본으로 하되, 필요할 때는 언제든 네이티브 API에 직접 접근하여 엔진 고유의 기능을 제약 없이 활용할 수 있습니다.

---

## Features

- ✅ **Kotlin DSL** — `excel { sheet { row { cell() } } }` 구조의 직관적인 빌더
- ✅ **Engine Abstraction** — Apache POI / FastExcel 자동 감지 및 교체 가능
- ✅ **DTO Binding** — `dataSheet("Users", users) { column("Name") { it.name } }`
- ✅ **Style Inheritance** — Workbook → Sheet → Row → Cell 계층적 스타일 상속
- ✅ **Streaming** — `Sequence` 기반 대용량 데이터 처리 (100K+ rows)
- ✅ **Native Extension** — `nativeSheet<SXSSFSheet> { ... }` 으로 엔진 고유 기능 접근
- ✅ **Thread Safety** — 동시 쓰기 감지 및 빠른 실패 (Fail-Fast)

---

## 엔진 선택 가이드

KExcel DSL은 비즈니스 로직을 수정하지 않고도 상황에 맞는 최적의 엔진을 선택할 수 있는 자유를 제공합니다.

|  | [Apache POI](https://github.com/apache/poi) | [FastExcel](https://github.com/dhatim/fastexcel) |
|---|---|---|
| JAR 크기 | ~30MB (전이 의존성 포함) | ~150KB |
| 쓰기 성능 | 표준 (SXSSF) | 고성능 (최대 1.3배 빠름) |
| 메모리 효율 | 스트리밍 방식 | 고효율 스트리밍 방식 |
| 수식 평가 | ✅ | ❌ |
| 차트 / 그래프 | ✅ | ❌ |
| 피벗 테이블 | ✅ | ❌ |
| 서버리스 / 컨테이너 적합성 | 무거움 | 경량 |

### 🐘 [Apache POI](https://github.com/apache/poi) (SXSSF)
*업계 표준*
- **추천 대상**: 복잡한 스타일링, 수식 평가, 차트/그래프, 피벗 테이블 등 엑셀의 고급 기능이 필요한 보고서.
- **강점**: 20년 이상 축적된 호환성과 방대한 기능셋.
- **고려 사항**: 전이 의존성으로 인해 전체 JAR 크기가 ~30MB에 달하며, 서버리스/컨테이너 환경에서는 콜드 스타트에 영향을 줄 수 있습니다.

### 🏎️ [FastExcel](https://github.com/dhatim/fastexcel)
*고성능 & 경량*
- **추천 대상**: 대용량 데이터 처리, 성능과 메모리 효율이 중요한 고동시성 환경.
- **강점**: ~150KB의 가벼운 의존성. 벤치마크 결과 KExcel DSL은 네이티브 FastExcel 대비 단 **3.1%의 오버헤드**만 발생시키며, 1,000,000행 처리 시에도 압도적인 성능을 유지합니다. Lambda, Cloud Run 등 서버리스 환경에 적합합니다.
- **고려 사항**: 수식 평가, 차트, 피벗 테이블, 기존 파일 수정 등은 지원하지 않습니다.

**왜 KExcel인가?** KExcel을 사용하면 동일한 DSL 코드로 풍부한 기능이 필요한 보고서는 POI를, 대용량 처리가 중요한 파일은 FastExcel을 선택하여 생성할 수 있습니다. 비즈니스 로직을 중복해서 구현할 필요 없이, 요구사항에 따라 최적의 엔진을 유연하게 활용하세요. 상세 수치는 [벤치마크 리포트](docs/BENCHMARK.ko.md)에서 확인하세요.

---


## Quick Start

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.Hong1008.kexcel:kexcel-dsl:0.1.0")
    
    // 엔진 중 하나를 선택하세요:
    implementation("org.apache.poi:poi-ooxml:5.5.1")       // Apache POI
    // 또는
    implementation("org.dhatim:fastexcel:0.20.0")           // FastExcel
}
```

> **Note:** `kexcel-dsl`은 엔진에 대해 `compileOnly` 의존성을 가집니다. 사용자의 프로젝트에서 원하는 엔진을 직접 `implementation`으로 선언해야 합니다.

---

## Usage

### Basic — Sheet, Row, Cell

가장 기본적인 엑셀 파일 생성입니다. `excel` 블록 안에서 시트, 행, 셀을 선언합니다.

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

컬렉션 데이터를 자동으로 헤더 + 데이터 행으로 바인딩합니다.
컬럼별 스타일, 조건부 셀 스타일, 교차 행 색상 등을 선언적으로 정의할 수 있습니다.

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

스타일은 **Workbook → Sheet → Row → Cell** 순으로 상속되며, 하위 레벨이 상위 레벨을 덮어씁니다.

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

`rows()` 함수는 `Sequence<T>`를 받아 한 행씩 스트리밍 처리합니다. 메모리 사용량은 데이터 크기에 관계없이 일정합니다.

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

대용량 데이터 처리 시의 Flush 주기나 메타데이터 설정을 `WorkbookOptions`를 통해 관리할 수 있습니다.

```kotlin
val options = WorkbookOptions(
    flushInterval = 5000,           // 5000행마다 flush (기본값 1000)
    forceFormulaRecalculation = true, // 파일 오픈 시 수식 재계산 강제
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

DSL이 추상화하지 않는 엔진 고유 기능은 `nativeSheet<T>` / `nativeWorkbook<T>`로 직접 접근합니다.
잘못된 타입을 넘기면 블록이 조용히 무시되므로, **엔진을 교체해도 코드가 깨지지 않습니다.**

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

> ⚠️ **Streaming Engine Caution:** 스트리밍 엔진(POI SXSSF, FastExcel)은 주기적으로 행을 디스크에 flush합니다. flush된 행은 네이티브 객체로도 수정할 수 없습니다. Freeze Pane 등의 설정은 **행 작성 전에**, Auto Filter 같은 메타데이터는 **행 작성 후에** 호출하는 것이 안전합니다.

### Multi-Sheet Report

하나의 `excel` 블록 안에서 여러 시트를 조합하여 복합 보고서를 생성할 수 있습니다.

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

**Engine Auto-Detection:** `ExcelDriverFactory.autoDetect()`가 Classpath를 스캔하여 사용 가능한 엔진을 자동으로 선택합니다. 명시적으로 지정하려면 `excel(output, driver = PoiDriver()) { ... }`를 사용합니다.

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
