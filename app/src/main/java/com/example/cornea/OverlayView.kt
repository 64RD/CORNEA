package com.example.cornea

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<BoundingBox> = emptyList()
    private var bgBitmap: Bitmap? = null

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        color = Color.GREEN
    }

    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
    }

    private val labelBgPaint = Paint().apply {
        color = Color.BLACK
        alpha = 180
    }

    init {
        //Make the overlay transparent by default
        setWillNotDraw(false)
        setBackgroundColor(Color.TRANSPARENT)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    fun clear() {
        results = emptyList()
        invalidate()
    }

    fun setBackgroundBitmap(bitmap: Bitmap?) {
        bgBitmap = bitmap
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        bgBitmap?.let { bmp ->
            val maxW = MeasureSpec.getSize(widthMeasureSpec)
            val scaledH = (bmp.height.toFloat() * (maxW.toFloat() / bmp.width.toFloat())).toInt()
            setMeasuredDimension(maxW, scaledH)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bmp = bgBitmap ?: return

        //Compute scaled destination rect (aspect fit)
        val viewRatio = width.toFloat() / height.toFloat()
        val bmpRatio = bmp.width.toFloat() / bmp.height.toFloat()

        val dst: RectF
        val scaleX: Float
        val scaleY: Float
        val offsetX: Float
        val offsetY: Float

        if (bmpRatio > viewRatio) {
            //Image is wider — fit width
            val scaledH = width.toFloat() / bmpRatio
            offsetX = 0f
            offsetY = (height - scaledH) / 2
            scaleX = width.toFloat()
            scaleY = scaledH
            dst = RectF(0f, offsetY, width.toFloat(), offsetY + scaledH)
        } else {
            // Image is taller — fit height
            val scaledW = height.toFloat() * bmpRatio
            offsetX = (width - scaledW) / 2
            offsetY = 0f
            scaleX = scaledW
            scaleY = height.toFloat()
            dst = RectF(offsetX, 0f, offsetX + scaledW, height.toFloat())
        }

        //Draw background bitmap centered and scaled
        canvas.drawBitmap(bmp, null, dst, null)

        //Draw bounding boxes adjusted for scaling and offset
        if (results.isEmpty()) return

        results.forEach { b ->
            val left = offsetX + b.x1 * scaleX
            val top = offsetY + b.y1 * scaleY
            val right = offsetX + b.x2 * scaleX
            val bottom = offsetY + b.y2 * scaleY

            val rect = RectF(left, top, right, bottom)
            canvas.drawRect(rect, boxPaint)

            val label = if (b.clsName.isNotEmpty()) {
                "${b.clsName} ${"%.2f".format(b.cnf)}"
            } else {
                "${b.cls} ${"%.2f".format(b.cnf)}"
            }

            val pad = 8f
            val textW = labelPaint.measureText(label)
            val textH = labelPaint.textSize
            val bgRect = RectF(left, top - textH - 2 * pad, left + textW + 2 * pad, top)
            canvas.drawRect(bgRect, labelBgPaint)
            canvas.drawText(label, left + pad, top - pad, labelPaint)
        }
    }


    fun getAnnotatedBitmap(): Bitmap? {
        if (bgBitmap == null || width == 0 || height == 0) return null
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        draw(canvas)
        return output
    }
}
