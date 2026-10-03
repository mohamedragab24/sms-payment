package com.example.smstotelegram.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/** Animated payment onboarding illustration: wallet/phone in the center and payment items orbiting it. */
class PaymentOrbitView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val blue = Color.rgb(35, 174, 228)
    private val orange = Color.rgb(255, 105, 55)
    private val navy = Color.rgb(45, 76, 102)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var start = System.nanoTime()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h * .46f
        val t = ((System.nanoTime() - start) / 1_000_000_000f) % 5f
        val angle = t / 5f * (Math.PI * 2.0).toFloat()

        // soft glow
        paint.color = Color.argb(28, 35, 174, 228)
        canvas.drawCircle(cx, cy, minOf(w, h) * .39f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = Color.argb(120, 35, 174, 228)
        canvas.drawArc(cx-w*.36f, cy-h*.33f, cx+w*.36f, cy+h*.33f, 205f, 150f, false, paint)
        paint.color = Color.argb(165, 255, 105, 55)
        canvas.drawArc(cx-w*.36f, cy-h*.33f, cx+w*.36f, cy+h*.33f, 25f, 155f, false, paint)
        paint.style = Paint.Style.FILL

        drawPhone(canvas, cx, cy)

        val r = minOf(w, h) * .32f
        drawCoin(canvas, cx + r*cos(angle), cy + r*.68f*sin(angle))
        drawCard(canvas, cx + r*cos(angle + 2.1f), cy + r*.68f*sin(angle + 2.1f))
        drawShield(canvas, cx + r*cos(angle + 4.2f), cy + r*.68f*sin(angle + 4.2f))

        // Title
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = minOf(w, h) * .075f
        paint.color = blue
        canvas.drawText("بوابة الدفع فهمت", cx, h*.88f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = minOf(w, h) * .045f
        paint.color = navy
        canvas.drawText("دفع أسرع • أمان أعلى", cx, h*.95f, paint)

        postInvalidateOnAnimation()
    }

    private fun drawPhone(c: Canvas, cx: Float, cy: Float) {
        val pw = width*.30f; val ph = height*.48f
        paint.color = Color.WHITE
        paint.setShadowLayer(16f, 0f, 7f, Color.argb(45,0,0,0))
        setLayerType(View.LAYER_TYPE_SOFTWARE, paint)
        c.drawRoundRect(cx-pw/2, cy-ph/2, cx+pw/2, cy+ph/2, 28f, 28f, paint)
        paint.clearShadowLayer()
        paint.color = blue
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 7f
        c.drawRoundRect(cx-pw/2, cy-ph/2, cx+pw/2, cy+ph/2, 28f, 28f, paint)
        paint.style = Paint.Style.FILL
        paint.color = orange
        c.drawRoundRect(cx-pw*.28f, cy-ph*.08f, cx+pw*.28f, cy+ph*.08f, 18f,18f,paint)
        paint.color = blue
        c.drawCircle(cx, cy-ph*.22f, 18f, paint)
        paint.color = Color.WHITE
        paint.strokeWidth = 7f; paint.style = Paint.Style.STROKE
        c.drawLine(cx-9, cy-ph*.22f, cx-2, cy-ph*.22f+8, paint)
        c.drawLine(cx-2, cy-ph*.22f+8, cx+12, cy-ph*.22f-9, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawCoin(c: Canvas, x: Float, y: Float) {
        paint.color = orange; c.drawCircle(x,y,27f,paint)
        paint.color = Color.WHITE; paint.textAlign=Paint.Align.CENTER; paint.textSize=28f; paint.typeface=Typeface.DEFAULT_BOLD
        c.drawText("$",x,y+10f,paint)
    }

    private fun drawCard(c: Canvas, x: Float, y: Float) {
        paint.color = blue
        c.drawRoundRect(x-48,y-30,x+48,y+30,12f,12f,paint)
        paint.color = orange
        c.drawRect(x+18,y-11,x+36,y+11,paint)
        paint.color = Color.WHITE
        paint.strokeWidth=5f
        c.drawLine(x-30,y+9,x+5,y+9,paint)
    }

    private fun drawShield(c: Canvas, x: Float, y: Float) {
        val p=Path(); p.moveTo(x,y-34); p.lineTo(x+27,y-22); p.lineTo(x+22,y+18); p.lineTo(x,y+34); p.lineTo(x-22,y+18); p.lineTo(x-27,y-22); p.close()
        paint.color=blue; c.drawPath(p,paint)
        paint.color=Color.WHITE; paint.style=Paint.Style.STROKE; paint.strokeWidth=6f
        c.drawLine(x-12,y,x-2,y+10,paint); c.drawLine(x-2,y+10,x+14,y-10,paint); paint.style=Paint.Style.FILL
    }
}
