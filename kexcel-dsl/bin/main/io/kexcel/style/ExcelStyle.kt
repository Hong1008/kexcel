package io.kexcel.style

/**
 * Domain-independent style model representing Excel cell formatting.
 *
 * <p>This class uses nullable properties to support a hierarchical style inheritance system.
 * When merging styles, non-null values in the child style override those in the parent.
 *
 * <p><b>Thread Safety:</b> This is an immutable data class and is fully thread-safe.
 *
 * @see ExcelFont
 * @see ExcelBackground
 * @see ExcelAlignment
 * @see ExcelBorder
 */
data class ExcelStyle(
    val font: ExcelFont? = null,
    val background: ExcelBackground? = null,
    val alignment: ExcelAlignment? = null,
    val border: ExcelBorder? = null,
    val wrapText: Boolean? = null,
    val dataFormat: String? = null
) {
    /**
     * Merges this style with [other], where [other] takes precedence.
     * <p>Inheritance chain: Workbook Default -> Sheet Default -> Row Style -> Cell Style.
     * @param other the style to merge into this one. If null, this instance is returned.
     * @return a new immutable ExcelStyle instance representing the combined formatting
     */
    fun merge(other: ExcelStyle?): ExcelStyle {
        if (other == null) return this
        return ExcelStyle(
            font = (font ?: ExcelFont()).merge(other.font),
            background = (background ?: ExcelBackground()).merge(other.background),
            alignment = (alignment ?: ExcelAlignment()).merge(other.alignment),
            border = (border ?: ExcelBorder()).merge(other.border),
            wrapText = other.wrapText ?: this.wrapText,
            dataFormat = other.dataFormat ?: this.dataFormat
        )
    }
}

/**
 * Represents font formatting properties.
 */
data class ExcelFont(
    val bold: Boolean? = null,
    val size: Int? = null,
    val color: String? = null,
    val underlined: Boolean? = null
) {
    /**
     * @param other override properties
     */
    fun merge(other: ExcelFont?): ExcelFont {
        if (other == null) return this
        return ExcelFont(
            bold = other.bold ?: this.bold,
            size = other.size ?: this.size,
            color = other.color ?: this.color,
            underlined = other.underlined ?: this.underlined
        )
    }
}

/**
 * Represents background filling properties.
 * <p>Note: Colors should be provided in Hex format (e.g., "#FF0000").
 */
data class ExcelBackground(
    val color: String? = null
) {
    fun merge(other: ExcelBackground?): ExcelBackground {
        if (other == null) return this
        return ExcelBackground(
            color = other.color ?: this.color
        )
    }
}

/**
 * Represents text alignment properties.
 */
data class ExcelAlignment(
    val horizontal: HorizontalAlign? = null,
    val vertical: VerticalAlign? = null
) {
    fun merge(other: ExcelAlignment?): ExcelAlignment {
        if (other == null) return this
        return ExcelAlignment(
            horizontal = other.horizontal ?: this.horizontal,
            vertical = other.vertical ?: this.vertical
        )
    }
}

/**
 * Enumeration for horizontal alignment in a cell.
 */
enum class HorizontalAlign {
    GENERAL, LEFT, CENTER, RIGHT, FILL, JUSTIFY
}

/**
 * Enumeration for vertical alignment in a cell.
 */
enum class VerticalAlign {
    TOP, CENTER, BOTTOM, JUSTIFY
}

/**
 * Enumeration for border thickness/style.
 */
enum class BorderStyle {
    THIN, MEDIUM, THICK, DASHED, DOTTED, DOUBLE, HAIR
}

/**
 * Represents cell border properties.
 *
 * <p>The [all] property serves as a shorthand for setting all four sides at once.
 * Individual side properties ([top], [bottom], [left], [right]) take precedence over [all].
 *
 * <pre>
 * // All sides thin
 * ExcelBorder(all = BorderStyle.THIN)
 *
 * // All sides thin, but top is thick
 * ExcelBorder(all = BorderStyle.THIN, top = BorderStyle.THICK)
 * </pre>
 */
data class ExcelBorder(
    val all: BorderStyle? = null,
    val top: BorderStyle? = null,
    val bottom: BorderStyle? = null,
    val left: BorderStyle? = null,
    val right: BorderStyle? = null
) {
    /** Resolved top border: [top] takes precedence over [all]. */
    fun resolvedTop(): BorderStyle? = top ?: all

    /** Resolved bottom border: [bottom] takes precedence over [all]. */
    fun resolvedBottom(): BorderStyle? = bottom ?: all

    /** Resolved left border: [left] takes precedence over [all]. */
    fun resolvedLeft(): BorderStyle? = left ?: all

    /** Resolved right border: [right] takes precedence over [all]. */
    fun resolvedRight(): BorderStyle? = right ?: all

    fun merge(other: ExcelBorder?): ExcelBorder {
        if (other == null) return this
        return ExcelBorder(
            all = other.all ?: this.all,
            top = other.top ?: this.top,
            bottom = other.bottom ?: this.bottom,
            left = other.left ?: this.left,
            right = other.right ?: this.right
        )
    }
}
