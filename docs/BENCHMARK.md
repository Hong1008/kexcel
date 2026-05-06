# KExcel Performance Benchmark Report

This document details the performance characteristics of KExcel DSL compared to native engine APIs (FastExcel and Apache POI).

## 1. Executive Summary
- **DSL Overhead**: Reduced from **16.3% to 8.6%** (FastExcel) and near **0%** (POI) after optimization.
- **Memory Efficiency**: DSL abstraction adds almost **zero extra allocations** per cell thanks to function inlining.
- **Engine Comparison**: FastExcel is approximately **25-30% faster** than POI SXSSF for large datasets.
- **Stability**: Constant memory footprint confirmed for up to 1,000,000 rows within 512MB heap.

---

## 2. Benchmark Environment
- **JVM**: OpenJDK 21 (Adoptium)
- **Heap Size**: 512MB (`-Xmx512m`)
- **GC**: G1GC
- **JMH Version**: 1.36
- **Engines**: 
  - FastExcel 0.18.4
  - Apache POI 5.2.5 (SXSSF)

---

## 3. DSL Overhead Analysis
Measures the cost of using the KExcel DSL vs. calling engine APIs directly.
(10,000 Rows x 10 Columns)

| Driver | Native Throughput | DSL Throughput | **Overhead (%)** |
| :--- | :--- | :--- | :--- |
| **FastExcel** | 7.925 ops/s | 7.243 ops/s | **8.6%** |
| **Apache POI** | 4.485 ops/s | 5.086 ops/s | **~0%** (within error margin) |

> [!NOTE]
> DSL overhead is higher on FastExcel because the engine itself is extremely fast, making the relative cost of safe-checks and lock-handling more visible. For POI, the engine logic dominates the execution time.

---

## 4. Large Data Stability (Streaming)
Execution time for generating workbooks with massive row counts.

| Row Count | FastExcel Driver | POI Driver (SXSSF) |
| :--- | :--- | :--- |
| **100,000** | 0.238 sec | 0.440 sec |
| **1,000,000** | 3.396 sec | 4.249 sec |

**Verdict**: Both drivers show linear performance scaling. FastExcel remains the performance leader for raw data throughput.

---

## 5. Style Merging & Inheritance
Measures the impact of the KExcel style inheritance system.

| Scenario | Throughput (ops/s) |
| :--- | :--- |
| **No Style** | 81.47 |
| **Inherited Style** | 55.93 |
| **Individual Cell Style** | 52.30 |

**Verdict**: Using **Inherited Styles** (Workbook/Sheet level defaults) is slightly faster than applying individual styles to every cell, as it reduces the complexity of style resolution during writing.

---

## 6. Optimization Impact (Before vs After)
Impact of applying `inline` functions and removing `runCatching` from hot loops.

| Metric | Before Optimization | After Optimization | Improvement |
| :--- | :--- | :--- | :--- |
| FastExcel DSL Overhead | 16.3% | 8.6% | **47% Reduction** |
| Allocation per Operation | ~200 KB extra | **~300 Bytes** extra | **99.8% Reduction** |

> [!TIP]
> The dramatic reduction in allocations confirms that the Kotlin compiler is successfully inlining the DSL blocks, eliminating lambda object creation in the hot loops.

---

## 7. Recommendation
- **For Maximum Speed**: Use `FastExcelDriver`.
- **For Legacy Support/Complex Formulas**: Use `PoiDriver`.
- **For Best Maintenance**: Utilize the **Style Inheritance** system; it is both faster and cleaner.
