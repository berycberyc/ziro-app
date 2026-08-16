package kz.ziro.app.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import kz.ziro.app.data.Stage
import kz.ziro.app.data.TestType

/**
 * Generates a blank answer sheet PDF from a TestType's stages.
 * Stages marked with newPage=true start a fresh page (admin-controlled).
 * Within each stage, questions fill the left column top-to-bottom first,
 * then the right column (block numbering, not alternating).
 */
object AnswerSheetPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f

    fun generate(context: Context, testType: TestType): File {
        val document = PdfDocument()

        // Split stages into pages: a stage with newPage=true starts a new page.
        // The very first stage always begins page 1 regardless of its flag.
        val pages = mutableListOf<MutableList<Stage>>()
        testType.stages.forEachIndexed { index, stage ->
            if (index == 0 || !stage.newPage) {
                if (pages.isEmpty()) pages.add(mutableListOf())
                pages.last().add(stage)
            } else {
                pages.add(mutableListOf(stage))
            }
        }
        if (pages.isEmpty()) pages.add(mutableListOf())

        pages.forEachIndexed { pageIndex, stagesOnPage ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            drawContent(page.canvas, testType, stagesOnPage, pageIndex + 1, pages.size)
            document.finishPage(page)
        }

        val fileName = "${testType.code}_answer_sheet.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return file
    }

    private fun drawContent(
        canvas: Canvas,
        testType: TestType,
        stagesOnPage: List<Stage>,
        pageNumber: Int,
        totalPages: Int
    ) {
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
        }
        val headerFieldPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
        }
        val subjectPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }
        val questionPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
        }
        val circlePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val boxPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val qrBoxPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        var y = MARGIN

        canvas.drawText("Жауап парағы", MARGIN, y + 10, subtitlePaint)
        canvas.drawText("${testType.name_kk} / ${testType.name_ru}", MARGIN, y + 28, titlePaint)
        if (totalPages > 1) {
            canvas.drawText(
                "Бет $pageNumber / $totalPages",
                MARGIN, y + 42, subtitlePaint
            )
        }

        val qrBoxSize = 60f
        canvas.drawRect(
            PAGE_WIDTH - MARGIN - qrBoxSize, y,
            PAGE_WIDTH - MARGIN, y + qrBoxSize,
            qrBoxPaint
        )
        canvas.drawText(
            "QR",
            PAGE_WIDTH - MARGIN - qrBoxSize / 2 - 8, y + qrBoxSize / 2 + 4,
            subtitlePaint
        )

        y += 70f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, qrBoxPaint)
        y += 16f

        canvas.drawText("Аты-жөні: ______________________", MARGIN, y, headerFieldPaint)
        canvas.drawText("Аудитория: __________", PAGE_WIDTH / 2f, y, headerFieldPaint)
        y += 16f
        canvas.drawText("Тіл: __________", MARGIN, y, headerFieldPaint)
        canvas.drawText("Нұсқа: __________", PAGE_WIDTH / 2f, y, headerFieldPaint)

        y += 24f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, qrBoxPaint)
        y += 20f

        val colWidth = (PAGE_WIDTH - 2 * MARGIN - 20f) / 2
        var colX = MARGIN
        var colY = y
        var maxYInRow = y

        stagesOnPage.forEachIndexed { index, stage ->
            val stageHeight = drawStage(
                canvas, colX, colY, colWidth, stage,
                subjectPaint, questionPaint, circlePaint, boxPaint
            )
            maxYInRow = maxOf(maxYInRow, colY + stageHeight)

            if (index % 2 == 0) {
                colX = MARGIN + colWidth + 20f
            } else {
                colX = MARGIN
                colY = maxYInRow + 16f
                maxYInRow = colY
            }
        }
    }

    private fun drawStage(
        canvas: Canvas,
        x: Float,
        startY: Float,
        width: Float,
        stage: Stage,
        subjectPaint: Paint,
        questionPaint: Paint,
        circlePaint: Paint,
        boxPaint: Paint
    ): Float {
        var y = startY
        canvas.drawText(
            "${stage.subject} (${stage.questions} сұрақ, ${stage.minutes} мин)",
            x, y, subjectPaint
        )
        y += 14f

        val rowHeight = 12f
        val colGap = width / 2
        // Block numbering: first half of questions fill the left column
        // top-to-bottom, then the second half fill the right column —
        // not alternating between columns.
        val rowsPerCol = (stage.questions + 1) / 2

        for (q in 1..stage.questions) {
            val col = (q - 1) / rowsPerCol
            val row = (q - 1) % rowsPerCol
            val qx = x + col * colGap
            val qy = y + row * rowHeight

            canvas.drawText("$q.", qx, qy, questionPaint)

            if (stage.format == "number") {
                for (i in 0..2) {
                    val bx = qx + 18f + i * 12f
                    canvas.drawRect(bx, qy - 8f, bx + 10f, qy + 2f, boxPaint)
                }
            } else {
                val letters = listOf("A", "B", "C", "D")
                letters.forEachIndexed { i, _ ->
                    val cx = qx + 20f + i * 14f
                    canvas.drawCircle(cx, qy - 3f, 5f, circlePaint)
                }
            }
        }

        val totalRows = rowsPerCol
        return 14f + totalRows * rowHeight + 10f
    }
}
