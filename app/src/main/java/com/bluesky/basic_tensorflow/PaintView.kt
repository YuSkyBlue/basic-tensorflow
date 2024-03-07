package com.bluesky.basic_tensorflow

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

import java.util.ArrayList

class PaintView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    companion object {
        const val BITMAP_DIMENSION = 128
        const val FEED_DIMENSION = 64
    }

    private var setup = false
    private var drawHere = false
    private val paint: Paint
    private var bitmap: Bitmap? = null
    private val path: Path
    private var canvas: Canvas? = null
    private val transformMat = Matrix()
    private val inverseTransformMat = Matrix()
    private val pointF = PointF()
    private val paintPathList = ArrayList<PaintPath>()
    private var drawTextView: View? = null

    init {
        path = Path()
        paint = Paint()
        paint.isAntiAlias = true
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 6f
    }

    fun reset() {
        path.reset()
        bitmap?.eraseColor(Color.BLACK)
    }

    fun setDrawText(view: View) {
        drawTextView = view
        drawHere = true
    }

    private fun setupScaleMatrices() {
        val width = width.toFloat()
        val height = height.toFloat()
        val scaleW = width / BITMAP_DIMENSION
        val scaleH = height / BITMAP_DIMENSION
        var scale = scaleW
        if (scale > scaleH) {
            scale = scaleH
        }
        val centerX = BITMAP_DIMENSION * scale / 2
        val centerY = BITMAP_DIMENSION * scale / 2
        val dx = width / 2 - centerX
        val dy = height / 2 - centerY
        transformMat.setScale(scale, scale)
        transformMat.postTranslate(dx, dy)
        transformMat.invert(inverseTransformMat)
        setup = true
    }

    fun getBitmapCoords(x: Float, y: Float, out: PointF) {
        val points = floatArrayOf(x, y)
        inverseTransformMat.mapPoints(points)
        out.x = points[0]
        out.y = points[1]
    }

    fun onResume() {
        createBitmap()
    }

    fun onPause() {
        releaseBitmap()
    }

    private fun createBitmap() {
        bitmap?.recycle()
        bitmap = Bitmap.createBitmap(BITMAP_DIMENSION, BITMAP_DIMENSION, Bitmap.Config.ARGB_8888)
        bitmap?.let {
            canvas = Canvas(it)
            reset()
        }
    }

    private fun releaseBitmap() {
        bitmap?.recycle()
        bitmap = null
        canvas = null
        reset()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawHere) {
            drawTextView?.visibility = View.INVISIBLE
            drawHere = false
        }
        val paintPath = PaintPath()
        canvas?.drawPath(path, paint)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                getBitmapCoords(event.x, event.y, pointF)
                path.moveTo(pointF.x, pointF.y)
                path.lineTo(pointF.x, pointF.y)
            }
            MotionEvent.ACTION_MOVE -> {
                getBitmapCoords(event.x, event.y, pointF)
                path.lineTo(pointF.x, pointF.y)
                paintPath.path = path
                paint.color = Color.WHITE
                paintPath.paint = paint
                paintPathList.add(paintPath)
            }
        }
        invalidate()
        return true
    }

    override fun onDraw(c: Canvas) {
        if (!setup) {
            setupScaleMatrices()
        }
        bitmap?.let {
            canvas?.drawBitmap(it, transformMat, paint)
            if (paintPathList.isNotEmpty()) {
                canvas?.drawPath(
                    paintPathList[paintPathList.size - 1].path,
                    paintPathList[paintPathList.size - 1].paint
                )
            }
        }
    }

    fun getPixelData(): FloatArray {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap!!, FEED_DIMENSION, FEED_DIMENSION, false)
        val width = FEED_DIMENSION
        val height = FEED_DIMENSION
        val pixels = IntArray(width * height)
        resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val returnPixels = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val pix = pixels[i]
            val b = pix and 0xff
            returnPixels[i] = b / 255.0f
        }
        return returnPixels
    }

    inner class PaintPath {
        lateinit var path: Path
        lateinit var paint: Paint
    }
}
