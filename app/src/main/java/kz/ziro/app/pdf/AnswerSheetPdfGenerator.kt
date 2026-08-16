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

    /** Blank template — no QR, empty header fields to fill by hand. */
    fun generate(context: Context, testType: TestType): File =
        render(context, testType, qrData = null, fileSuffix = "answer_sheet")

    /**
     * Personalized sheet with a real, scannable QR code and filled-in
     * header fields — used for testing the full scan pipeline, and later
     * for real per-student sheets once distribution data flows in.
     */
    fun generateWithQr(context: Context, testType: TestType, qrData: SheetQrData): File =
        render(context, testType, qrData, fileSuffix = "personalized")

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
                variant = registration.test_variant ?: "—"
            )
            val pages = AnswerSheetGeometry.computePages(testType)
            pages.forEach { pageLayout ->
                pageCounter++
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageCounter
                ).create()
                val page = document.startPage(pageInfo)
                drawPage(page.canvas, testType, pageLayout, pages.size, qrData)
                document.finishPage(page)
            }
        }

        val safeName = sessionTitle.replace(Regex("[^A-Za-z0-9Ա-ֿА-я ]"), "_")
        val file = File(context.cacheDir, "${safeName}_batch.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun render(context: Context, testType: TestType, qrData: SheetQrData?, fileSuffix: String): File {
        val document = PdfDocument()
        val pages = AnswerSheetGeometry.computePages(testType)

        pages.forEach { pageLayout ->
            val pageInfo = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageLayout.pageNumber
            ).create()
            val page = document.startPage(pageInfo)
            drawPage(page.canvas, testType, pageLayout, pages.size, qrData)
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
        qrData: SheetQrData?
    ) {
        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 16f; isFakeBoldText = true }
        val subtitlePaint = Paint().apply { color = Color.GRAY; textSize = 9f }
        val headerFieldPaint = Paint().apply { color = Color.DKGRAY; textSize = 9f }
        val filledFieldPaint = Paint().apply { color = Color.BLACK; textSize = 9f; isFakeBoldText = true }
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

        var y = MARGIN
        canvas.drawText("Жауап парағы", MARGIN, y + 10, subtitlePaint)
        canvas.drawText("${testType.name_kk} / ${testType.name_ru}", MARGIN, y + 28, titlePaint)
        if (totalPages > 1) {
            canvas.drawText("Бет ${pageLayout.pageNumber} / $totalPages", MARGIN, y + 42, subtitlePaint)
        }

        val qrBoxSize = 60f
        val qrBoxLeft = PAGE_WIDTH - MARGIN - qrBoxSize
        if (qrData != null) {
            val qrBitmap = makeQrBitmap(qrData.encode(), 300)
            val destRect = android.graphics.RectF(qrBoxLeft, y, qrBoxLeft + qrBoxSize, y + qrBoxSize)
            canvas.drawBitmap(qrBitmap, null, destRect, null)
        } else {
            canvas.drawRect(qrBoxLeft, y, PAGE_WIDTH - MARGIN, y + qrBoxSize, lightLinePaint)
            canvas.drawText("QR", qrBoxLeft + qrBoxSize / 2 - 8, y + qrBoxSize / 2 + 4, subtitlePaint)
        }

        y += 70f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, lightLinePaint)
        y += 16f
        if (qrData != null) {
            canvas.drawText("Аты-жөні: ${qrData.studentName}", MARGIN, y, filledFieldPaint)
            canvas.drawText("Аудитория: ${qrData.classroom}", PAGE_WIDTH / 2f, y, filledFieldPaint)
            y += 16f
            canvas.drawText("Тіл: —", MARGIN, y, filledFieldPaint)
            canvas.drawText("Нұсқа: ${qrData.variant}", PAGE_WIDTH / 2f, y, filledFieldPaint)
        } else {
            canvas.drawText("Аты-жөні: ______________________", MARGIN, y, headerFieldPaint)
            canvas.drawText("Аудитория: __________", PAGE_WIDTH / 2f, y, headerFieldPaint)
            y += 16f
            canvas.drawText("Тіл: __________", MARGIN, y, headerFieldPaint)
            canvas.drawText("Нұсқа: __________", PAGE_WIDTH / 2f, y, headerFieldPaint)
        }
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
