package kz.ziro.app.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import kz.ziro.app.data.TestType
import kz.ziro.app.pdf.AnswerSheetGeometry.PAGE_HEIGHT
import kz.ziro.app.pdf.AnswerSheetGeometry.PAGE_WIDTH
import kz.ziro.app.pdf.AnswerSheetGeometry.MARGIN

/**
 * Generates a blank answer sheet PDF from a TestType's stages, using the
 * shared AnswerSheetGeometry so the printed layout and the OMR analyzer
 * that later reads it always agree on where every bubble is.
 */
object AnswerSheetPdfGenerator {

    fun generate(context: Context, testType: TestType): File {
        val document = PdfDocument()
        val pages = AnswerSheetGeometry.computePages(testType)

        pages.forEach { pageLayout ->
            val pageInfo = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageLayout.pageNumber
            ).create()
            val page = document.startPage(pageInfo)
            drawPage(page.canvas, testType, pageLayout, pages.size)
            document.finishPage(page)
        }

        val fileName = "${testType.code}_answer_sheet.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return file
    }

    private fun drawPage(
        canvas: Canvas,
        testType: TestType,
        pageLayout: AnswerSheetGeometry.PageLayout,
        totalPages: Int
    ) {
        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 16f; isFakeBoldText = true }
        val subtitlePaint = Paint().apply { color = Color.GRAY; textSize = 9f }
        val headerFieldPaint = Paint().apply { color = Color.DKGRAY; textSize = 9f }
        val subjectPaint = Paint().apply { color = Color.BLACK; textSize = 11f; isFakeBoldText = true }
        val questionPaint = Paint().apply { color = Color.BLACK; textSize = 8f }
        val circlePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val boxPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val lightLinePaint = Paint().apply { color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 1f }
        val registrationMarkPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

        // Corner registration marks (used by the app to correct skew when scanning)
        val half = AnswerSheetGeometry.MARK_SIZE / 2
        AnswerSheetGeometry.registrationMarkCenters().forEach { mark ->
            canvas.drawRect(mark.x - half, mark.y - half, mark.x + half, mark.y + half, registrationMarkPaint)
        }

        var y = MARGIN
        canvas.drawText("Жауап парағы", MARGIN, y + 10, subtitlePaint)
        canvas.drawText("${testType.name_kk} / ${testType.name_ru}", MARGIN, y + 28, titlePaint)
        if (totalPages > 1) {
            canvas.drawText("Бет ${pageLayout.pageNumber} / $totalPages", MARGIN, y + 42, subtitlePaint)
        }

        val qrBoxSize = 60f
        canvas.drawRect(PAGE_WIDTH - MARGIN - qrBoxSize, y, PAGE_WIDTH - MARGIN, y + qrBoxSize, lightLinePaint)
        canvas.drawText("QR", PAGE_WIDTH - MARGIN - qrBoxSize / 2 - 8, y + qrBoxSize / 2 + 4, subtitlePaint)

        y += 70f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, lightLinePaint)
        y += 16f
        canvas.drawText("Аты-жөні: ______________________", MARGIN, y, headerFieldPaint)
        canvas.drawText("Аудитория: __________", PAGE_WIDTH / 2f, y, headerFieldPaint)
        y += 16f
        canvas.drawText("Тіл: __________", MARGIN, y, headerFieldPaint)
        canvas.drawText("Нұсқа: __________", PAGE_WIDTH / 2f, y, headerFieldPaint)
        y += 24f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, lightLinePaint)

        pageLayout.stages.forEach { stageLayout ->
            canvas.drawText(
                "${stageLayout.stage.subject} (${stageLayout.stage.questions} сұрақ, ${stageLayout.stage.minutes} мин)",
                stageLayout.x, stageLayout.y + 10f, subjectPaint
            )
            stageLayout.questions.forEach { q ->
                val labelX = q.options.first().x - 20f
                canvas.drawText("${q.questionNumber}.", labelX, q.options.first().y + 3f, questionPaint)
                if (q.format == "number") {
                    q.options.forEach { opt ->
                        canvas.drawRect(opt.x - 5f, opt.y - 5f, opt.x + 5f, opt.y + 5f, boxPaint)
                    }
                } else {
                    q.options.forEach { opt ->
                        canvas.drawCircle(opt.x, opt.y, 5f, circlePaint)
                    }
                }
            }
        }
    }
}
