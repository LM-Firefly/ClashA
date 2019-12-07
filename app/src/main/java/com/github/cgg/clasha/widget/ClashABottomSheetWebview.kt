package com.github.cgg.clasha.widget

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.github.cgg.clasha.R

/**
 * @Author: ccg
 * @Email: ccg
 * @program: BottomSheets
 * @create: 2018-12-19
 * @describe
 */
class ClashABottomSheetWebview : WebView {

    private var bottomCoordinator: CoordinatorLayout? = null
    private val downY: Float = 0.toFloat()
    private val moveY: Float = 0.toFloat()

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    fun bindBottomSheetDialog(container: FrameLayout, listener: OnTouchListener?) {
        try {
            bottomCoordinator = container.findViewById(R.id.coordinator) as CoordinatorLayout
            setOnTouchListener(object : OnTouchListener() {
                @Override
                fun onTouch(v: View, event: MotionEvent): Boolean {
                    if (bottomCoordinator != null) {
                        bottomCoordinator!!.requestDisallowInterceptTouchEvent(true)
                    }

                    if (listener != null) {
                        listener!!.onTouch(v, event)
                    }
                    return false

                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}
