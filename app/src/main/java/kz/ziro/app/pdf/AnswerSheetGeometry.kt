package kz.ziro.app.pdf

import kz.ziro.app.data.Stage
import kz.ziro.app.data.TestType

/**
 * Single source of truth for where every bubble/box sits on an answer sheet
 * page. Both AnswerSheetPdfGenerator (drawing) and BubbleSheetAnalyzer
 * (reading) use these exact coordinates, so a printed sheet and its
 * recognition logic can never drift out of sync.
 *
 * All coordinates are in PDF points on a PAGE_WIDTH x PAGE_HEIGHT page
 * (standard A4 at 72dpi-equivalent scale, matching PdfDocument's default).
 */
object AnswerSheetGeometry {

    const val PAGE_WIDTH = 595f
    const val PAGE_HEIGHT = 842f
    const val MARGIN = 32f

    const val MARK_SIZE = 14f
    const val MARK_INSET = 12f

    // Header layout — QR is large and centered at the top for reliable
    // scanning; header fields stack as one compact column below it. Both
    // the generator (drawing) and this geometry (bubble coordinates)
    // build the content start from these exact same constants, in the
    // exact same order, so they can never drift apart.
    const val QR_SIZE = 105f
    const val HEADER_QR_GAP = 8f
    const val TITLE_BLOCK_HEIGHT = 24f
    const val FIELD_LINE_HEIGHT = 13f
    const val FIELD_LINE_COUNT = 4
    const val DIVIDER_GAP = 6f
    val CONTENT_START_Y: Float =
        MARGIN + QR_SIZE + HEADER_QR_GAP + TITLE_BLOCK_HEIGHT +
            FIELD_LINE_HEIGHT * FIELD_LINE_COUNT + DIVIDER_GAP

    data class OptionCenter(val label: String, val x: Float, val y: Float)
    data class QuestionLayout(
        val questionNumber: Int,
        val format: String, // "abcd" or "number"
        val options: List<OptionCenter> // 4 letters, or 3 digit boxes (label "0","1","2" = box index)
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

        val colWidth = (PAGE_WIDTH - 2 * MARGIN - 20f) / 2
        var colX = MARGIN
        var colY = contentStartY
        var maxYInRow = contentStartY

        val result = mutableListOf<StageLayout>()

        stagesOnPage.forEachIndexed { index, stage ->
            val layout = layoutStage(stage, colX, colY, colWidth)
            result.add(layout)
            maxYInRow = maxOf(maxYInRow, colY + layout.height)

            if (index % 2 == 0) {
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
        val titleHeight = 22f
        val rowHeight = 12f
        val colGap = width / 2
        val rowsPerCol = (stage.questions + 1) / 2

        val labelWidth = 20f
        val letterSpacing = 15f
        val boxSpacing = 13f
        val bubbleRadius = 5.5f

        val blockWidth = if (stage.format == "number") {
            labelWidth + 2 * boxSpacing + 10f
        } else {
            labelWidth + 3 * letterSpacing + 2 * bubbleRadius
        }
        val blockOffsetX = ((colGap - blockWidth) / 2f).coerceAtLeast(0f)

        val questions = (1..stage.questions).map { q ->
            val col = (q - 1) / rowsPerCol
            val row = (q - 1) % rowsPerCol
            val qx = x + col * colGap + blockOffsetX
            val qy = startY + titleHeight + row * rowHeight

            val options = if (stage.format == "number") {
                (0..2).map { i ->
                    val bx = qx + labelWidth + i * boxSpacing + 5f
                    OptionCenter(i.toString(), bx, qy - 3f)
                }
            } else {
                listOf("A", "B", "C", "D").mapIndexed { i, letter ->
                    val cx = qx + labelWidth + i * letterSpacing
                    OptionCenter(letter, cx, qy - 3f)
                }
            }

            QuestionLayout(q, stage.format, options)
        }

        val totalHeight = titleHeight + rowsPerCol * rowHeight + 10f
        return StageLayout(stage, x, startY, width, questions, totalHeight)
    }
}
