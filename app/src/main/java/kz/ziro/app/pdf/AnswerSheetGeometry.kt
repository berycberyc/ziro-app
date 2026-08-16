package kz.ziro.app.pdf

import kz.ziro.app.data.Stage
import kz.ziro.app.data.TestType

/**
 * Single source of truth for where every bubble/box/digit-segment sits on
 * an answer sheet page. Both AnswerSheetPdfGenerator (drawing) and the
 * future OMR analyzer (reading) use these exact coordinates, so a printed
 * sheet and its recognition logic can never drift out of sync.
 *
 * All coordinates are in PDF points on a PAGE_WIDTH x PAGE_HEIGHT page
 * (standard A4, matching PdfDocument's default scale).
 */
object AnswerSheetGeometry {

    const val PAGE_WIDTH = 595f
    const val PAGE_HEIGHT = 842f
    const val MARGIN = 32f

    const val MARK_SIZE = 14f
    const val MARK_INSET = 12f

    // Header: QR sits beside the text block (not above it), so the header
    // stays short and the question grid can start much higher up the page.
    const val QR_SIZE = 90f
    const val HEADER_LEFT_WIDTH = 320f
    const val HEADER_GAP = 16f
    const val HEADER_LINE_HEIGHT = 13f
    const val HEADER_LINE_COUNT = 6 // test name, session, 4 fields — same font/size
    const val DIVIDER_GAP = 10f
    val CONTENT_START_Y: Float = MARGIN + QR_SIZE + DIVIDER_GAP

    // Seven-segment digit cell (for number-format answers), matching the
    // real РФМШ answer-sheet style: student connects dots to draw a digit
    // instead of writing free-hand.
    const val DIGIT_COUNT = 6
    const val DIGIT_CELL_WIDTH = 15f
    const val DIGIT_CELL_HEIGHT = 22f
    const val DIGIT_CELL_GAP = 4f

    data class OptionCenter(val label: String, val x: Float, val y: Float)
    data class QuestionLayout(
        val questionNumber: Int,
        val format: String, // "abcd" or "number"
        val options: List<OptionCenter>
        // abcd: 4 points labeled "A".."D"
        // number: up to DIGIT_COUNT*7 points labeled "<digitIndex>:<segment>",
        //         segment in {a,b,c,d,e,f,g}; nodes for drawing are derived
        //         from the same digit cell geometry (see digitCellNodes()).
    )
    data class StageLayout(
        val stage: Stage,
        val x: Float,
        val y: Float,
        val width: Float,
        val questions: List<QuestionLayout>,
        val height: Float
    )
    data class PageLayout(val pageNumber: Int, val stages: List<StageLayout>)

    /** The four registration mark centers, in the same order every time. */
    fun registrationMarkCenters(): List<OptionCenter> {
        val half = MARK_SIZE / 2
        val inset = MARK_INSET + half
        return listOf(
            OptionCenter("top-left", inset, inset),
            OptionCenter("top-right", PAGE_WIDTH - inset, inset),
            OptionCenter("bottom-left", inset, PAGE_HEIGHT - inset),
            OptionCenter("bottom-right", PAGE_WIDTH - inset, PAGE_HEIGHT - inset)
        )
    }

    /** Splits stages into pages exactly like the PDF generator does. */
    fun computePages(testType: TestType): List<PageLayout> {
        val stageGroups = mutableListOf<MutableList<Stage>>()
        testType.stages.forEachIndexed { index, stage ->
            if (index == 0 || !stage.newPage) {
                if (stageGroups.isEmpty()) stageGroups.add(mutableListOf())
                stageGroups.last().add(stage)
            } else {
                stageGroups.add(mutableListOf(stage))
            }
        }
        if (stageGroups.isEmpty()) stageGroups.add(mutableListOf())

        return stageGroups.mapIndexed { pageIndex, stagesOnPage ->
            PageLayout(pageIndex + 1, layoutStagesOnPage(stagesOnPage))
        }
    }

    private fun layoutStagesOnPage(stagesOnPage: List<Stage>): List<StageLayout> {
        val contentStartY = CONTENT_START_Y
        val fullWidth = PAGE_WIDTH - 2 * MARGIN
        val colWidth = (fullWidth - 20f) / 2

        var colX = MARGIN
        var colY = contentStartY
        var maxYInRow = contentStartY

        val result = mutableListOf<StageLayout>()

        stagesOnPage.forEachIndexed { index, stage ->
            val isNumberFormat = stage.format == "number"
            // Number-format stages need much more horizontal room per
            // question (six digit cells), so they take the full row width
            // instead of sharing a two-column split with the next stage.
            val useX = if (isNumberFormat) MARGIN else colX
            val useWidth = if (isNumberFormat) fullWidth else colWidth

            if (isNumberFormat && colX != MARGIN) {
                // Flush the current row before starting a full-width stage.
                colX = MARGIN
                colY = maxYInRow + 16f
                maxYInRow = colY
            }

            val layout = layoutStage(stage, useX, colY, useWidth)
            result.add(layout)
            maxYInRow = maxOf(maxYInRow, colY + layout.height)

            if (isNumberFormat) {
                colX = MARGIN
                colY = maxYInRow + 16f
                maxYInRow = colY
            } else if (colX == MARGIN) {
                colX = MARGIN + colWidth + 20f
            } else {
                colX = MARGIN
                colY = maxYInRow + 16f
                maxYInRow = colY
            }
        }
        return result
    }

    private fun layoutStage(stage: Stage, x: Float, startY: Float, width: Float): StageLayout {
        return if (stage.format == "number") {
            layoutNumberStage(stage, x, startY, width)
        } else {
            layoutAbcdStage(stage, x, startY, width)
        }
    }

    private fun layoutAbcdStage(stage: Stage, x: Float, startY: Float, width: Float): StageLayout {
        val titleHeight = 22f
        val rowHeight = 12f
        val colGap = width / 2
        val rowsPerCol = (stage.questions + 1) / 2

        val labelWidth = 20f
        val letterSpacing = 15f
        val bubbleRadius = 5.5f
        val blockWidth = labelWidth + 3 * letterSpacing + 2 * bubbleRadius
        val blockOffsetX = ((colGap - blockWidth) / 2f).coerceAtLeast(0f)

        val questions = (1..stage.questions).map { q ->
            val col = (q - 1) / rowsPerCol
            val row = (q - 1) % rowsPerCol
            val qx = x + col * colGap + blockOffsetX
            val qy = startY + titleHeight + row * rowHeight

            val options = listOf("A", "B", "C", "D").mapIndexed { i, letter ->
                OptionCenter(letter, qx + labelWidth + i * letterSpacing, qy - 3f)
            }
            QuestionLayout(q, stage.format, options)
        }

        val totalHeight = titleHeight + rowsPerCol * rowHeight + 10f
        return StageLayout(stage, x, startY, width, questions, totalHeight)
    }

    /** The 6 seven-segment-display node positions within one digit cell (relative). */
    private fun digitCellNodes(): Map<String, OptionCenter> {
        val w = DIGIT_CELL_WIDTH
        val h = DIGIT_CELL_HEIGHT
        return mapOf(
            "TL" to OptionCenter("TL", 0f, 0f),
            "TR" to OptionCenter("TR", w, 0f),
            "ML" to OptionCenter("ML", 0f, h / 2),
            "MR" to OptionCenter("MR", w, h / 2),
            "BL" to OptionCenter("BL", 0f, h),
            "BR" to OptionCenter("BR", w, h)
        )
    }

    /** Midpoints of the 7 segments within one digit cell (relative to cell origin). */
    fun digitSegmentMidpoints(): Map<String, OptionCenter> {
        val w = DIGIT_CELL_WIDTH
        val h = DIGIT_CELL_HEIGHT
        return mapOf(
            "a" to OptionCenter("a", w / 2, 0f),       // top
            "b" to OptionCenter("b", w, h / 4),         // upper-right
            "c" to OptionCenter("c", w, 3 * h / 4),     // lower-right
            "d" to OptionCenter("d", w / 2, h),         // bottom
            "e" to OptionCenter("e", 0f, 3 * h / 4),    // lower-left
            "f" to OptionCenter("f", 0f, h / 4),        // upper-left
            "g" to OptionCenter("g", w / 2, h / 2)      // middle
        )
    }

    private fun layoutNumberStage(stage: Stage, x: Float, startY: Float, width: Float): StageLayout {
        val titleHeight = 22f
        val rowHeight = DIGIT_CELL_HEIGHT + 14f
        val labelWidth = 22f
        val cellStride = DIGIT_CELL_WIDTH + DIGIT_CELL_GAP

        val questions = (1..stage.questions).map { q ->
            val row = q - 1
            val qy = startY + titleHeight + row * rowHeight
            val cellsLeft = x + labelWidth

            val options = (0 until DIGIT_COUNT).flatMap { digitIndex ->
                val cellX = cellsLeft + digitIndex * cellStride
                digitSegmentMidpoints().values.map { seg ->
                    OptionCenter("$digitIndex:${seg.label}", cellX + seg.x, qy + seg.y)
                }
            }
            QuestionLayout(q, stage.format, options)
        }

        val totalHeight = titleHeight + stage.questions * rowHeight + 10f
        return StageLayout(stage, x, startY, width, questions, totalHeight)
    }
}
