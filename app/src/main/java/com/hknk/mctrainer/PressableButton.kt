package com.hknk.mctrainer

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat

class PressableButton : androidx.appcompat.widget.AppCompatButton {

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs, defStyle)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
        background = ContextCompat.getDrawable(context, R.drawable.button_background)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    override fun drawableStateChanged() {
        if (isPressed) {
            setPadding(
                paddingLeft,
                10.toPx.toInt(),
                paddingRight,
                paddingBottom
            )
        } else {
            setPadding(
                paddingLeft,
                5.toPx.toInt(),
                paddingRight,
                paddingBottom
            )
        }
        super.drawableStateChanged()
    }
    val Number.toPx
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        )
}