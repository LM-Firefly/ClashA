/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.cgg.clasha.widget

import android.content.Context
import android.content.DialogInterface
import android.content.res.TypedArray
import android.os.Build
import android.os.Bundle

import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.github.cgg.clasha.R
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Base class for [android.app.Dialog]s styled as a bottom sheet.
 */
class ClashABottomSheetDialog : AppCompatDialog {

    private var mBehavior: BottomSheetBehavior<FrameLayout>? = null

    private var mPeekHeight: Int = 0
    private var mMaxHeight: Int = 0
    var container: FrameLayout? = null
        private set
    private var mCreated: Boolean = false

    internal var mCancelable = true
    private var mCanceledOnTouchOutside = true
    private var mCanceledOnTouchOutsideSet: Boolean = false

    private val mBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        @Override
        fun onStateChanged(@NonNull bottomSheet: View,
                           @BottomSheetBehavior.State newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
                BottomSheetBehavior.from(bottomSheet).setState(
                        BottomSheetBehavior.STATE_COLLAPSED)
            }
        }

        @Override
        fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {
        }
    }

    @JvmOverloads
    constructor(@NonNull context: Context, @StyleRes theme: Int = 0) : super(context, getThemeResId(context, theme)) {
        // We hide the title bar for any style configuration. Otherwise, there will be a gap
        // above the bottom sheet when it is expanded.
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    protected constructor(@NonNull context: Context, cancelable: Boolean,
                          cancelListener: DialogInterface.OnCancelListener) : super(context, cancelable, cancelListener) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        mCancelable = cancelable
    }

    @Override
    fun setContentView(@LayoutRes layoutResId: Int) {
        super.setContentView(wrapInBottomSheet(layoutResId, null, null))
    }

    @Override
    protected fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        val window = getWindow()
        if (window != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                window!!.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            }
            window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
        }

        mCreated = true

    }

    @Override
    fun setContentView(view: View) {
        super.setContentView(wrapInBottomSheet(0, view, null))
    }

    @Override
    fun setContentView(view: View, params: ViewGroup.LayoutParams) {
        super.setContentView(wrapInBottomSheet(0, view, params))
    }

    @Override
    fun setCancelable(cancelable: Boolean) {
        super.setCancelable(cancelable)
        if (mCancelable != cancelable) {
            mCancelable = cancelable
            if (mBehavior != null) {
                mBehavior!!.setHideable(cancelable)
            }
        }
    }

    @Override
    protected fun onStart() {
        super.onStart()
        if (mBehavior != null) {
            mBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
        }
    }

    @Override
    fun setCanceledOnTouchOutside(cancel: Boolean) {
        super.setCanceledOnTouchOutside(cancel)
        if (cancel && !mCancelable) {
            mCancelable = true
        }
        mCanceledOnTouchOutside = cancel
        mCanceledOnTouchOutsideSet = true
    }

    private fun wrapInBottomSheet(layoutResId: Int, view: View?, params: ViewGroup.LayoutParams?): View {
        var view = view
        container = View.inflate(getContext(),
                R.layout.echat_design_bottom_sheet_dialog, null) as FrameLayout
        val coordinator = container!!.findViewById(R.id.coordinator) as CoordinatorLayout
        if (layoutResId != 0 && view == null) {
            view = getLayoutInflater().inflate(layoutResId, coordinator, false)
        }
        val bottomSheet = coordinator.findViewById(R.id.design_bottom_sheet) as FrameLayout
        mBehavior = BottomSheetBehavior.from(bottomSheet)
        mBehavior!!.setBottomSheetCallback(mBottomSheetCallback)
        mBehavior!!.setHideable(mCancelable)
        if (params == null) {
            bottomSheet.addView(view)
        } else {
            bottomSheet.addView(view, params)
        }
        // We treat the CoordinatorLayout as outside the dialog though it is technically inside
        coordinator.findViewById(R.id.touch_outside).setOnClickListener(object : View.OnClickListener() {
            @Override
            fun onClick(view: View) {
                if (mCancelable && isShowing() && shouldWindowCloseOnTouchOutside()) {
                    cancel()
                }
            }
        })
        // Handle accessibility events
        ViewCompat.setAccessibilityDelegate(bottomSheet, object : AccessibilityDelegateCompat() {
            @Override
            fun onInitializeAccessibilityNodeInfo(host: View,
                                                  info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                if (mCancelable) {
                    info.addAction(AccessibilityNodeInfoCompat.ACTION_DISMISS)
                    info.setDismissable(true)
                } else {
                    info.setDismissable(false)
                }
            }

            @Override
            fun performAccessibilityAction(host: View, action: Int, args: Bundle): Boolean {
                if (action == AccessibilityNodeInfoCompat.ACTION_DISMISS && mCancelable) {
                    cancel()
                    return true
                }
                return super.performAccessibilityAction(host, action, args)
            }
        })
        bottomSheet.setOnTouchListener(object : View.OnTouchListener() {
            @Override
            fun onTouch(view: View, event: MotionEvent): Boolean {
                // Consume the event and prevent it from falling through
                return true
            }
        })
        return container
    }

    internal fun shouldWindowCloseOnTouchOutside(): Boolean {
        if (!mCanceledOnTouchOutsideSet) {
            if (Build.VERSION.SDK_INT < 11) {
                mCanceledOnTouchOutside = true
            } else {
                val a = getContext().obtainStyledAttributes(
                        intArrayOf(android.R.attr.windowCloseOnTouchOutside))
                mCanceledOnTouchOutside = a.getBoolean(0, true)
                a.recycle()
            }
            mCanceledOnTouchOutsideSet = true
        }
        return mCanceledOnTouchOutside
    }

    private fun getThemeResId(context: Context, themeId: Int): Int {
        var themeId = themeId
        if (themeId == 0) {
            // If the provided theme is 0, then retrieve the dialogTheme from our theme
            val outValue = TypedValue()
            if (context.getTheme().resolveAttribute(
                            R.attr.bottomSheetDialogTheme, outValue, true)) {
                themeId = outValue.resourceId
            } else {
                // bottomSheetDialogTheme is not provided; we default to our light theme
                themeId = R.style.Theme_Design_Light_BottomSheetDialog
            }
        }
        return themeId
    }


    private fun setBottomSheetCallback() {
        mBehavior!!.setBottomSheetCallback(mBottomSheetCallback)
    }

    fun setPeekHeight(peekHeight: Int) {
        mPeekHeight = peekHeight

        if (mCreated) {
            setPeekHeight()
        }
    }

    fun setMaxHeight(height: Int) {
        mMaxHeight = height

        if (mCreated) {
            setMaxHeight()
        }
    }

    private fun setPeekHeight() {
        if (mPeekHeight <= 0) {
            return
        }
        mBehavior!!.setPeekHeight(mPeekHeight)
    }

    private fun setMaxHeight() {
        if (mMaxHeight <= 0) {
            return
        }

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, mMaxHeight)
        getWindow().setGravity(Gravity.BOTTOM)
    }
}
