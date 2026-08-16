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
 * reads it always agree on where every bubble is.
 */
object AnswerSheetPdfGenerator {

    private fun languageLabel(code: String?): String = when (code) {
        "kk" -> "Қазақша"
        "ru" -> "Орысша"
        else -> "—"
    }

    /** Blank template — no QR, empty header fields to fill by hand. */
    fun generate(context: Context, testType: TestType): File =
        render(context, testType, qrData = null, sessionTitle = null, fileSuffix = "answer_sheet")

    /**
     * Personalized sheet with a real, scannable QR code and filled-in
     * header fields — used for testing the full scan pipeline, and later
     * for real per-student sheets once distribution data flows in.
     */
    fun generateWithQr(context: Context, testType: TestType, qrData: SheetQrData): File =
        render(context, testType, qrData, sessionTitle = null, fileSuffix = "personalized")

    /**
     * Combined batch of personalized sheets for every paid, distributed
     * student in a session, sorted by classroom — one file, ready to share.
     */
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
        val subtitlePaint = Paint().apply { color = Color.GRAY; textSize = 9f }
        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 15f; isFakeBoldText = true }
        val sessionPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f }
        val headerFieldPaint = Paint().apply { color = Color.DKGRAY; textSize = 9f }
        val filledFieldPaint = Paint().apply { color = Color.BLACK; textSize = 10f; isFakeBoldText = true }
        val subjectPaint = Paint().apply { color = Color.BLACK; textSize = 11f; isFakeBoldText = true }
        val questionPaint = Paint().apply { color = Color.BLACK; textSize = 8f }
        val circlePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val boxPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val lightLinePaint = Paint().apply { color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 1f }
        val registrationMarkPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

        val half = AnswerSheetGeometry.MARK_SIZE / 2
        AnswerSheetGeometry.registrationMarkCenters().forEach { mark ->
            canvas.drawRect(mark.x - half, mark.y - half, mark.x + half, mark.y + half, registrationMarkPaint)
        }

        val qrSize = AnswerSheetGeometry.QR_SIZE
        val qrLeft = (PAGE_WIDTH - qrSize) / 2f
        var y = MARGIN

        // QR centered at the very top for reliable scanning.
        if (qrData != null) {
            val qrBitmap = makeQrBitmap(qrData.encode(), 400)
            val destRect = android.graphics.RectF(qrLeft, y, qrLeft + qrSize, y + qrSize)
            canvas.drawBitmap(qrBitmap, null, destRect, null)
        } else {
            canvas.drawRect(qrLeft, y, qrLeft + qrSize, y + qrSize, lightLinePaint)
            canvas.drawText("QR", qrLeft + qrSize / 2 - 10, y + qrSize / 2 + 5, subtitlePaint)
        }
        y += qrSize + AnswerSheetGeometry.HEADER_QR_GAP

        // Test name + session name, prominent, left aligned below the QR.
        canvas.drawText("${testType.name_kk} / ${testType.name_ru}", MARGIN, y + 4, titlePaint)
        if (totalPages > 1) {
            canvas.drawText(" (Бет ${pageLayout.pageNumber}/$totalPages)", MARGIN + 260, y + 4, subtitlePaint)
        }
        if (!sessionTitle.isNullOrBlank()) {
            canvas.drawText(sessionTitle, MARGIN, y + 16, sessionPaint)
        }
        y += AnswerSheetGeometry.TITLE_BLOCK_HEIGHT

        // Header fields — single compact column of 4 lines.
        val fieldLineHeight = AnswerSheetGeometry.FIELD_LINE_HEIGHT
        if (qrData != null) {
            canvas.drawText("Аты-жөні: ${qrData.studentName}", MARGIN, y, filledFieldPaint); y += fieldLineHeight
            canvas.drawText("Тіл: ${qrData.language}", MARGIN, y, filledFieldPaint); y += fieldLineHeight
            canvas.drawText("Аудитория: ${qrData.classroom}", MARGIN, y, filledFieldPaint); y += fieldLineHeight
            canvas.drawText("Нұсқа: ${qrData.variant}", MARGIN, y, filledFieldPaint); y += fieldLineHeight
        } else {
            canvas.drawText("Аты-жөні: ______________________", MARGIN, y, headerFieldPaint); y += fieldLineHeight
            canvas.drawText("Тіл: __________", MARGIN, y, headerFieldPaint); y += fieldLineHeight
            canvas.drawText("Аудитория: __________", MARGIN, y, headerFieldPaint); y += fieldLineHeight
            canvas.drawText("Нұсқа: __________", MARGIN, y, headerFieldPaint); y += fieldLineHeight
        }
        y += AnswerSheetGeometry.DIVIDER_GAP
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, lightLinePaint)
        // y now equals AnswerSheetGeometry.CONTENT_START_Y exactly — the
        // question grid below starts at that same coordinate.

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
                        canvas.drawCircle(opt.x, opt.y, 5.5f, circlePaint)
                    }
                }
            }
        }
    }
}
