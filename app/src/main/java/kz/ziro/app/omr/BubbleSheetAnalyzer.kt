package kz.ziro.app.omr

import android.graphics.Bitmap
import android.graphics.Point
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kz.ziro.app.data.TestType
import kz.ziro.app.pdf.AnswerSheetGeometry

object OpenCvInit {
    private var loaded = false

    /** Must be called once before any OpenCV Mat operations. Safe to call repeatedly. */
    fun ensureLoaded(): Boolean {
        if (!loaded) {
            loaded = OpenCVLoader.initLocal()
        }
        return loaded
    }
}

data class RecognizedAnswer(
    val questionNumber: Int,
    val subject: String,
    val format: String,
    val detectedLabel: String?, // "A".."D" or "0".."9" digit string later; null = blank
    val confident: Boolean       // false = ambiguous, needs human review
)

data class AnalysisResult(
    val answers: List<RecognizedAnswer>,
    val cornersFound: Boolean
)

object BubbleSheetAnalyzer {

    // Canonical working resolution: 3 pixels per PDF point.
    private const val SCALE = 3f
    private val CANVAS_WIDTH = (AnswerSheetGeometry.PAGE_WIDTH * SCALE).toInt()
    private val CANVAS_HEIGHT = (AnswerSheetGeometry.PAGE_HEIGHT * SCALE).toInt()

    fun analyze(bitmap: Bitmap, testType: TestType, pageNumber: Int): AnalysisResult {
        if (!OpenCvInit.ensureLoaded()) {
            throw IllegalStateException("OpenCV кітапханасы жүктелмеді")
        }

        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)
        val gray = Mat()
        Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_RGBA2GRAY)

        val corners = detectCorners(gray)
        if (corners == null) {
            return AnalysisResult(emptyList(), cornersFound = false)
        }

        val warped = warpToCanonical(gray, corners)

        val pages = AnswerSheetGeometry.computePages(testType)
        val pageLayout = pages.getOrNull(pageNumber - 1) ?: pages.first()

        val results = mutableListOf<RecognizedAnswer>()
        pageLayout.stages.forEach { stageLayout ->
            stageLayout.questions.forEach { q ->
                val darkness = q.options.map { opt ->
                    opt.label to sampleDarkness(warped, opt.x * SCALE, opt.y * SCALE)
                }
                val sorted = darkness.sortedByDescending { it.second }
                val best = sorted.first()
                val secondBest = sorted.getOrNull(1)

                val filled = best.second > FILL_THRESHOLD
                val ambiguousGap = secondBest != null && (best.second - secondBest.second) < AMBIGUOUS_GAP

                results.add(
                    RecognizedAnswer(
                        questionNumber = q.questionNumber,
                        subject = stageLayout.stage.subject,
                        format = q.format,
                        detectedLabel = if (filled) best.first else null,
                        confident = filled && !ambiguousGap
                    )
                )
            }
        }

        return AnalysisResult(results, cornersFound = true)
    }

    // Mean pixel value (0-255) is inverted here: darker fill -> higher score.
    private const val FILL_THRESHOLD = 90f // score above this = considered filled
    private const val AMBIGUOUS_GAP = 15f  // if top two scores are this close, flag for review
    private const val SAMPLE_RADIUS = 5

    private fun sampleDarkness(warped: Mat, x: Float, y: Float): Float {
        val cx = x.toInt().coerceIn(SAMPLE_RADIUS, warped.cols() - SAMPLE_RADIUS - 1)
        val cy = y.toInt().coerceIn(SAMPLE_RADIUS, warped.rows() - SAMPLE_RADIUS - 1)
        val roi = Rect(cx - SAMPLE_RADIUS, cy - SAMPLE_RADIUS, SAMPLE_RADIUS * 2, SAMPLE_RADIUS * 2)
        val region = Mat(warped, roi)
        val meanVal = Core.mean(region).`val`[0]
        // meanVal near 0 = black (filled), near 255 = white (empty)
        return (255f - meanVal.toFloat())
    }

    private data class CornerSet(val topLeft: Point, val topRight: Point, val bottomLeft: Point, val bottomRight: Point)

    /**
     * Finds the four solid black registration squares near each corner of
     * the photo by thresholding and looking for a small square-ish blob
     * closest to each expected corner region.
     */
    private fun detectCorners(gray: Mat): CornerSet? {
        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        val w = gray.cols()
        val h = gray.rows()
        val minArea = (w * h) * 0.00005
        val maxArea = (w * h) * 0.01

        data class Candidate(val cx: Double, val cy: Double, val area: Double)
        val candidates = mutableListOf<Candidate>()

        for (c in contours) {
            val area = Imgproc.contourArea(c)
            if (area < minArea || area > maxArea) continue
            val rect = Imgproc.boundingRect(c)
            val aspect = rect.width.toDouble() / rect.height.toDouble()
            if (aspect < 0.6 || aspect > 1.6) continue
            candidates.add(Candidate(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0, area))
        }

        if (candidates.size < 4) return null

        fun closestTo(px: Double, py: Double) =
            candidates.minByOrNull { (it.cx - px) * (it.cx - px) + (it.cy - py) * (it.cy - py) }

        val tl = closestTo(0.0, 0.0) ?: return null
        val tr = closestTo(w.toDouble(), 0.0) ?: return null
        val bl = closestTo(0.0, h.toDouble()) ?: return null
        val br = closestTo(w.toDouble(), h.toDouble()) ?: return null

        return CornerSet(
            Point(tl.cx.toInt(), tl.cy.toInt()),
            Point(tr.cx.toInt(), tr.cy.toInt()),
            Point(bl.cx.toInt(), bl.cy.toInt()),
            Point(br.cx.toInt(), br.cy.toInt())
        )
    }

    private fun warpToCanonical(gray: Mat, corners: CornerSet): Mat {
        val src = MatOfPoint2f(
            org.opencv.core.Point(corners.topLeft.x.toDouble(), corners.topLeft.y.toDouble()),
            org.opencv.core.Point(corners.topRight.x.toDouble(), corners.topRight.y.toDouble()),
            org.opencv.core.Point(corners.bottomRight.x.toDouble(), corners.bottomRight.y.toDouble()),
            org.opencv.core.Point(corners.bottomLeft.x.toDouble(), corners.bottomLeft.y.toDouble())
        )

        val marks = AnswerSheetGeometry.registrationMarkCenters()
        val tl = marks[0]; val tr = marks[1]; val bl = marks[2]; val br = marks[3]
        val dst = MatOfPoint2f(
            org.opencv.core.Point((tl.x * SCALE).toDouble(), (tl.y * SCALE).toDouble()),
            org.opencv.core.Point((tr.x * SCALE).toDouble(), (tr.y * SCALE).toDouble()),
            org.opencv.core.Point((br.x * SCALE).toDouble(), (br.y * SCALE).toDouble()),
            org.opencv.core.Point((bl.x * SCALE).toDouble(), (bl.y * SCALE).toDouble())
        )

        val transform = Imgproc.getPerspectiveTransform(src, dst)
        val warped = Mat()
        Imgproc.warpPerspective(gray, warped, transform, Size(CANVAS_WIDTH.toDouble(), CANVAS_HEIGHT.toDouble()))
        return warped
    }
}
