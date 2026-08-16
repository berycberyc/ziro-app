package kz.ziro.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import kz.ziro.app.data.TestType
import kz.ziro.app.omr.SheetQrData
import kz.ziro.app.pdf.AnswerSheetGeometry.MARGIN
import kz.ziro.app.pdf.AnswerSheetGeometry.PAGE_HEIGHT
import kz.ziro.app.pdf.AnswerSheetGeometry.PAGE_WIDTH

/**
 * Generates answer sheet PDFs from a TestType's stages, using the shared
 * AnswerSheetGeometry so the printed layout and the OMR analyzer that later
 * reads it always agree on where every bubble/segment is.
 */
object AnswerSheetPdfGenerator {

    private fun languageLabel(code: String?): String = when (code) {
        "kk" -> "Қазақша"
        "ru" -> "Орысша"
        else -> "—"
    }

    fun generate(context: Context, testType: TestType): File =
        render(context, testType, qrData = null, sessionTitle = null, fileSuffix = "answer_sheet")

    fun generateWithQr(context: Context, testType: TestType, qrData: SheetQrData): File =
        render(context, testType, qrData, sessionTitle = null, fileSuffix = "personalized")

    fun generateBatch(
        context: Context,
        sessionTitle: String,
        items: List<Pair<kz.ziro.app.data.Registration, TestType>>
    ): File {
        val sorted = items.sortedWith(
            compareBy(
                { it.first.classroom ?: "" },
                { it.first.students?.full_name ?: "" }
            )
        )

        val document = PdfDocument()
        var pageCounter = 0

        sorted.forEach { (registration, testType) ->
            val qrData = SheetQrData(
                testTypeId = testType.id ?: "",
                studentName = registration.students?.full_name ?: "—",
                classroom = registration.classroom ?: "—",
                variant = registration.test_variant ?: "—",
                language = languageLabel(registration.students?.language)
            )
            val pages = AnswerSheetGeometry.computePages(testType)
            pages.forEach { pageLayout ->
                pageCounter++
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageCounter
                ).create()
                val page = document.startPage(pageInfo)
                drawPage(page.canvas, testType, pageLayout, pages.size, qrData, sessionTitle)
                document.finishPage(page)
            }
        }

        val safeName = sessionTitle.replace(Regex("[^A-Za-z0-9Ա-ֿА-я ]"), "_")
        val file = File(context.cacheDir, "${safeName}_batch.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun render(
        context: Context,
        testType: TestType,
        qrData: SheetQrData?,
        sessionTitle: String?,
        fileSuffix: String
    ): File {
        val document = PdfDocument()
        val pages = AnswerSheetGeometry.computePages(testType)

        pages.forEach { pageLayout ->
            val pageInfo = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageLayout.pageNumber
            ).create()
            val page = document.startPage(pageInfo)
            drawPage(page.canvas, testType, pageLayout, pages.size, qrData, sessionTitle)
            document.finishPage(page)
        }

        val fileName = "${testType.code}_${fileSuffix}.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return file
    }

    private fun makeQrBitmap(content: String, sizePx: Int): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun drawPage(
        canvas: Canvas,
        testType: TestType,
        pageLayout: AnswerSheetGeometry.PageLayout,
        totalPages: Int,
        qrData: SheetQrData?,
        sessionTitle: String?
    ) {
        // Test name, session name, and all header fields share one font —
        // only filled-in (black) vs blank-template (gray) differs.
        val headerLinePaint = Paint().apply {
            color = if (qrData != null) Color.BLACK else Color.DKGRAY
            textSize = 10.5f
            isFakeBoldText = qrData != null
        }
        val subjectPaint = Paint().apply { color = Color.BLACK; textSize = 11f; isFakeBoldText = true }
        val questionPaint = Paint().apply { color = Color.BLACK; textSize = 8f }
        val circlePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val lightLinePaint = Paint().apply { color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 1f }
        val digitDotPaint = Paint().apply { color = Color.GRAY; style = Paint.Style.FILL }
        val registrationMarkPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

        val half = AnswerSheetGeometry.MARK_SIZE / 2
        AnswerSheetGeometry.registrationMarkCenters().forEach { mark ->
            canvas.drawRect(mark.x - half, mark.y - half, mark.x + half, mark.y + half, registrationMarkPaint)
        }

        // Header: text block on the left, QR beside it (not stacked above).
        var textY = MARGIN + 10f
        val lineH = AnswerSheetGeometry.HEADER_LINE_HEIGHT

        canvas.drawText("${testType.name_kk} / ${testType.name_ru}", MARGIN, textY, headerLinePaint); textY += lineH
        if (!sessionTitle.isNullOrBlank()) {
            canvas.drawText(sessionTitle, MARGIN, textY, headerLinePaint); textY += lineH
        }
        if (qrData != null) {
            canvas.drawText("Аты-жөні: ${qrData.studentName}", MARGIN, textY, headerLinePaint); textY += lineH
            canvas.drawText("Тіл: ${qrData.language}", MARGIN, textY, headerLinePaint); textY += lineH
            canvas.drawText("Аудитория: ${qrData.classroom}", MARGIN, textY, headerLinePaint); textY += lineH
            canvas.drawText("Нұсқа: ${qrData.variant}", MARGIN, textY, headerLinePaint)
        } else {
            canvas.drawText("Аты-жөні: ______________________", MARGIN, textY, headerLinePaint); textY += lineH
            canvas.drawText("Тіл: __________", MARGIN, textY, headerLinePaint); textY += lineH
            canvas.drawText("Аудитория: __________", MARGIN, textY, headerLinePaint); textY += lineH
            canvas.drawText("Нұсқа: __________", MARGIN, textY, headerLinePaint)
        }

        val qrSize = AnswerSheetGeometry.QR_SIZE
        val qrLeft = MARGIN + AnswerSheetGeometry.HEADER_LEFT_WIDTH + AnswerSheetGeometry.HEADER_GAP
        val qrTop = MARGIN
        if (qrData != null) {
            val qrBitmap = makeQrBitmap(qrData.encode(), 400)
            val destRect = android.graphics.RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
            canvas.drawBitmap(qrBitmap, null, destRect, null)
        } else {
            canvas.drawRect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize, lightLinePaint)
            canvas.drawText("QR", qrLeft + qrSize / 2 - 10, qrTop + qrSize / 2 + 5, headerLinePaint)
        }

        val dividerY = MARGIN + qrSize + AnswerSheetGeometry.DIVIDER_GAP - 4f
        canvas.drawLine(MARGIN, dividerY, PAGE_WIDTH - MARGIN, dividerY, lightLinePaint)
        // Question grid starts at AnswerSheetGeometry.CONTENT_START_Y, right below this line.

        pageLayout.stages.forEach { stageLayout ->
            canvas.drawText(
                "${stageLayout.stage.subject} (${stageLayout.stage.questions} сұрақ, ${stageLayout.stage.minutes} мин)",
                stageLayout.x, stageLayout.y + 10f, subjectPaint
            )
            stageLayout.questions.forEach { q ->
                if (q.format == "number") {
                    drawDigitQuestion(canvas, q, stageLayout.x, questionPaint, digitDotPaint)
                } else {
                    val labelX = q.options.first().x - 20f
                    canvas.drawText("${q.questionNumber}.", labelX, q.options.first().y + 3f, questionPaint)
                    q.options.forEach { opt ->
                        canvas.drawCircle(opt.x, opt.y, 5.5f, circlePaint)
                    }
                }
            }
        }

        if (totalPages > 1) {
            val footerPaint = Paint().apply { color = Color.GRAY; textSize = 8f }
            canvas.drawText(
                "Бет ${pageLayout.pageNumber}/$totalPages",
                PAGE_WIDTH - MARGIN - 50f, PAGE_HEIGHT - 20f, footerPaint
            )
        }
    }

    /** Draws one number-format question: label + N seven-segment digit cells. */
    private fun drawDigitQuestion(
        canvas: Canvas,
        q: AnswerSheetGeometry.QuestionLayout,
        stageX: Float,
        questionPaint: Paint,
        dotPaint: Paint
    ) {
        // options are labeled "<digitIndex>:<segment>"; group back by digit.
        val byDigit = q.options.groupBy { it.label.substringBefore(":") }
        val firstDigitTop = byDigit["0"]?.minOfOrNull { it.y } ?: return
        canvas.drawText("${q.questionNumber}.", stageX, firstDigitTop + AnswerSheetGeometry.DIGIT_CELL_HEIGHT / 2 + 8f, questionPaint)

        val w = AnswerSheetGeometry.DIGIT_CELL_WIDTH
        val h = AnswerSheetGeometry.DIGIT_CELL_HEIGHT

        for (digitIndex in 0 until AnswerSheetGeometry.DIGIT_COUNT) {
            val segs = byDigit[digitIndex.toString()] ?: continue
            val segMap = segs.associateBy { it.label.substringAfter(":") }
            val a = segMap["a"] ?: continue
            val cellLeft = a.x - w / 2
            val cellTop = a.y

            // Draw the 6 corner "dots" (guide nodes) student connects with a pen.
            val nodes = listOf(
                cellLeft to cellTop,                 // TL
                cellLeft + w to cellTop,              // TR
                cellLeft to cellTop + h / 2,           // ML
                cellLeft + w to cellTop + h / 2,       // MR
                cellLeft to cellTop + h,               // BL
                cellLeft + w to cellTop + h            // BR
            )
            nodes.forEach { (nx, ny) -> canvas.drawCircle(nx, ny, 1.1f, dotPaint) }
        }
    }
}
