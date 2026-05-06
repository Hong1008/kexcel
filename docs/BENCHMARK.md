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
> FastExcel shows a higher relative overhead because the engine itself is extremely fast, making the fixed cost of DSL logic (like lock checks) more visible. In POI, the engine logic is heavier, which masks the DSL abstraction cost.

---

## 4. Large Data Stability (Streaming)
Comparison of execution time for generating workbooks with a massive number of rows.

| Row Count | FastExcel Driver | POI Driver (SXSSF) |
| :--- | :--- | :--- |
| **100,000** | 0.238 s | 0.440 s |
| **1,000,000** | 3.396 s | 4.249 s |

**Conclusion**: Both drivers scale linearly with data size. FastExcel maintains a consistent lead in raw throughput.

---

## 5. Style Inheritance & Merging
Measures how KExcel's style inheritance system impacts performance.

| Scenario | Throughput (ops/s) |
| :--- | :--- |
| **No Style** | 81.47 |
| **Inherited Style** | 55.93 |
| **Individual Cell Styles** | 52.30 |

**Conclusion**: Using **Inherited Styles** (Workbook/Sheet level) is slightly faster than applying individual styles to every cell, as it reduces the complexity of style resolution at write-time.

---

## 6. Optimization Impact (Before vs After)
Comparison of metrics before and after applying `inline` functions and removing `runCatching` from hot loops.

| Metric | Before Optimization | After Optimization | **Improvement** |
| :--- | :--- | :--- | :--- |
| FastExcel DSL Overhead | 16.3% | 8.6% | **~47% Reduction** |
| Extra Memory Alloc/Op | ~200 KB | **~300 Bytes** | **99.8% Reduction** |

---

## 7. Case Study: Cell Merging Performance

### 7.1 The Problem
Reports indicated that using `mergeCells` with large datasets caused significant slowdowns and occasional `OutOfMemoryError`.

### 7.2 Investigation
We created a specialized `MergeBenchmark` to measure the impact of merge frequency (merging 2 cells every 10 rows).

**Initial Results (POI vs FastExcel):**
- **FastExcel**: Stable throughput (32 ops/s) and constant memory usage.
- **Apache POI**: Throughput dropped from 18 ops/s to **3.3 ops/s (▼82%)**, while GC allocation rate spiked by **610%**.

**Root Cause**: Apache POI's standard `addMergedRegion` performs an $O(n^2)$ validation check for every new region against all existing regions in the sheet.

### 7.3 Optimization
We replaced the standard merge method with `addMergedRegionUnsafe()` in `PoiDriver`, which bypasses the expensive overlap validation.

### 7.4 Final Results
After applying the `Unsafe` optimization, POI performance recovered significantly:

| Metric | Before (`addMergedRegion`) | After (`addMergedRegionUnsafe`) | **Improvement** |
| :--- | :--- | :--- | :--- |
| Throughput | 3.36 ops/s | **17.42 ops/s** | **▲ 518%** |
| GC Alloc Rate | 1580.8 MB/s | **328.6 MB/s** | **▼ 79%** |

### 7.5 Engineering Notes
While CPU overhead was solved, **Memory Residency** remains a factor. Both engines store merged region metadata in memory until the file is written. For projects requiring millions of merges, increasing JVM heap size is still recommended.

---

## 8. Recommendations
- **For Maximum Speed**: Use `FastExcelDriver`.
- **For Legacy Support/Complex Formulas**: Use `PoiDriver`.
- **For Best Maintenance**: Utilize the **Style Inheritance** system; it is both faster and cleaner.
