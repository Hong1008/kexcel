# Agent Guide (AGENTS.md)

Welcome, AI Agent! This document provides the essential context, design philosophy, and technical decisions behind **KExcel** to help you contribute effectively.

## 1. Core Philosophy & Constraints

*   **Streaming-First**: Every feature must prioritize memory efficiency for large datasets. High-memory operations are strictly discouraged.
*   **Engine Agnostic**: Maintain a clean abstraction layer. Business logic should remain unchanged regardless of the underlying engine (POI, FastExcel, etc.).
*   **Sequential Writing Only**: Due to the nature of streaming engines, moving back to previous rows or sheets is impossible. Ensure the DSL reflects this linear flow.
*   **Fail-Fast over Silent Failure**: Use `require()` and `check()` blocks to catch invalid states (e.g., setting both value and formula in one cell) immediately rather than ignoring them.

## 2. Architecture Patterns

*   **Driver Pattern**: All engine-specific logic must reside within implementations of the `ExcelDriver` interface.
*   **Scope-based DSL**: Builders use a hierarchical scope structure (`WorkbookScope` -> `SheetScope` -> `RowScope`). Use `@PublishedApi` and `internal` visibility for driver access within these scopes.
*   **Style Inheritance**: Styles follow a strict inheritance path: `Workbook` -> `Sheet` -> `Row` -> `Cell`. Styles are merged at the last possible moment to minimize object creation.

## 3. Architecture Decision Records (ADR)

*   **No Auto-size Columns**: Automatic column width is not supported because streaming engines cannot calculate content width without loading rows into memory, which violates the "Streaming-First" principle.
*   **Explicit Naming (`columnFormula`)**: We use `columnFormula` instead of overloading `column` to prevent developer confusion and ensure type-safety.
*   **Thread Safety with `ReentrantLock`**: We use a `ReentrantLock` to detect and "fail-fast" upon concurrent writes from different threads, as Excel generation is inherently sequential.
*   **Exception Strategy**: Wrap engine-specific errors in `ExcelStreamingException`. However, standard validation errors (like `IllegalArgumentException`) should be thrown directly to preserve their meaning.
*   **`compileOnly` Dependencies**: Excel engines are declared as `compileOnly` in the library to allow users to choose only the engine they need without bloating their classpath.
*   **Native Hook Resiliency**: `nativeSheet<T>` and `nativeWorkbook<T>` blocks must silently skip execution if the provided type `T` does not match the active engine. This ensures code doesn't crash when switching engines.

## 4. Development Standards

*   **Tech Stack**: Kotlin 2.x, Java 21, Gradle 9.x.
*   **Language**: Source code, KDoc, and test method names must be in **English**.
*   **Infrastructure**: Deployment via JitPack (`jitpack.yml` configured for Java 21). CI/CD via GitHub Actions (Tests & Dokka).

## 5. Testing Strategy

*   **DSL Validation**: Use `MockExcelDriver` to verify DSL-to-Driver calls without actual file I/O.
*   **Integration Tests**: Perform cross-engine verification by generating a file with one engine and reading it back with Apache POI to ensure data integrity.

## 6. Roadmap & Future Work

*   **Native Image Support**: Currently limited to manual implementation via `nativeSheet`. Future DSL support is planned.
*   **Framework Integration**: Planned support for Spring Boot Starters and various Template Engines.

## 7. Tips for Success

*   **Check Compatibility**: When adding a feature, always verify it works (or is safely stubbed) for both `PoiDriver` and `FastExcelDriver`.
*   **Run Tests First**: Always run `./gradlew :kexcel-dsl:test` before starting a new task to ensure a stable baseline.
*   **Be Explicit**: Prefer explicit naming over complex overloading when extending the DSL.
*   **Maintain Scopes**: Ensure `writeSafely` is used in builders to maintain the sequential write lock.
