package com.github.cgg.clasha.widget

import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.webkit.WebSettings.ZoomDensity
import android.widget.*
import androidx.annotation.StringRes
import com.blankj.utilcode.util.LogUtils
import com.github.cgg.clasha.BuildConfig
import com.github.cgg.clasha.R
import com.github.cgg.clasha.utils.UtilsKt

import com.github.cgg.clasha.App.app

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: BottomSheets
 * @create: 2018-12-19
 * @describe
 */
class ClashAWebviewBottomSheetDialog : ClashABottomSheetDialog {


    private var mProgress: ProgressBar? = null
    private var llToolbarClose: LinearLayout? = null
    private var ivToolbarNavigation: ImageView? = null
    private var tvToolbarTitle: TextView? = null
    private var mWebview: ClashABottomSheetWebview? = null
    private var isCanBack = true
    private var isShowNavBack = true
    private var port: String? = null
    private val handler = object : Handler() {
        @Override
        fun handleMessage(@NonNull msg: Message) {
            super.handleMessage(msg)
            val what = msg.what

            if (what == GET_PROXYS_SELECTS) {
                this.removeMessages(GET_PROXYS_SELECTS)
            }
        }
    }


    private val mWebViewClient = object : WebViewClient() {

        //>= Android 5.0
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            loadJS("window.localStorage.setItem('" + "externalControllerHost" + "','" + "127.0.0.1" + "');")
            loadJS("window.localStorage.setItem('externalControllerPort','$port');")
            val url = request.getUrl()
            if (url != null) {
                view.loadUrl(url!!.toString())
            }
            return true
        }

        @Override
        fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
            loadJS("window.localStorage.setItem('" + "externalControllerHost" + "','" + "127.0.0.1" + "');")
            loadJS("window.localStorage.setItem('externalControllerPort','$port');")
        }

        @Override
        fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }

        @Override
        fun onPageFinished(view: WebView, url: String) {
            if (mProgress != null) {
                mProgress!!.setVisibility(View.GONE)
            }
        }

        @Override
        fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
            LogUtils.e("WebView error: $errorCode + $description")

        }

    }

    private val mWebChromeClient = object : WebChromeClient() {

        @Override
        fun onProgressChanged(view: WebView, newProgress: Int) {
            if (mProgress != null) {
                mProgress!!.setMax(100)
                mProgress!!.setProgress(newProgress)
                // 如果进度大于或者等于100，则隐藏进度条
                if (newProgress >= 100) {
                    mProgress!!.setVisibility(View.GONE)
                }
            }
        }

        @Override
        fun onConsoleMessage(cm: ConsoleMessage): Boolean {
            LogUtils.i(cm.message() + " -- From line "
                    + cm.lineNumber() + " of "
                    + cm.sourceId())
            return true
        }
    }

    private var time = System.currentTimeMillis()


    constructor(@NonNull context: Context) : super(context) {
        init(context)
    }

    constructor(@NonNull context: Context, theme: Int) : super(context, theme) {
        init(context)
    }

    protected constructor(@NonNull context: Context, cancelable: Boolean, cancelListener: DialogInterface.OnCancelListener) : super(context, cancelable, cancelListener) {
        init(context)
    }

    private fun init(context: Context) {
        val view = LayoutInflater.from(context).inflate(R.layout.echat_dialog_layout_webview, null)

        //获得主要的主体颜色
        val typedValue = TypedValue()
        context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true)
        val colorPrimary = typedValue.data

        //设置控件
        val toolbar = view.findViewById(R.id.toolbarLayout) as RelativeLayout
        ivToolbarNavigation = view.findViewById(R.id.ivToolbarNavigation) as ImageView
        tvToolbarTitle = view.findViewById(R.id.tvToolbarTitle) as TextView
        llToolbarClose = view.findViewById(R.id.llToolbarClose) as LinearLayout
        mWebview = view.findViewById(R.id.webview) as ClashABottomSheetWebview
        mProgress = view.findViewById(android.R.id.progress) as ProgressBar

        //设置toolbar颜色 圆角
        val outerRadius = floatArrayOf(10f, 10f, 10f, 10f, 0f, 0f, 0f, 0f)
        val inset = RectF(0, 0, 0, 0)
        val innerRadius = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)//内矩形 圆角半径
        val roundRectShape = RoundRectShape(outerRadius, inset, innerRadius)
        val drawable = ShapeDrawable(roundRectShape)
        drawable.getPaint().setColor(colorPrimary)
        toolbar.setBackground(drawable)


        if (!isShowNavBack) ivToolbarNavigation!!.setVisibility(View.GONE)
        //事件
        llToolbarClose!!.setOnClickListener(object : View.OnClickListener() {
            @Override
            fun onClick(v: View) {
                dismiss()
            }
        })
        ivToolbarNavigation!!.setOnClickListener(object : View.OnClickListener() {
            @Override
            fun onClick(v: View) {
                if (isCanBack) {
                    if (mWebview!!.canGoBack()) {
                        mWebview!!.goBack()
                    } else {
                        dismiss()
                    }
                } else {
                    dismiss()
                }
            }
        })
        tvToolbarTitle!!.setOnClickListener(object : View.OnClickListener() {
            @Override
            fun onClick(v: View) {

            }
        })

        //设置ContentView
        setContentView(view)

        //初始化Webview
        mWebview!!.setWebViewClient(mWebViewClient)
        mWebview!!.setWebChromeClient(mWebChromeClient)
        val settings = mWebview!!.getSettings()
        //开启JavaScript支持
        settings.setJavaScriptEnabled(true)
        //默认设置为true，即允许在 File 域下执行任意 JavaScript 代码
        settings.setAllowFileAccess(true)
        settings.setAllowContentAccess(true)
        settings.setAllowFileAccessFromFileURLs(true)
        settings.setAllowUniversalAccessFromFileURLs(true)
        //Disable zoom
        settings.setSupportZoom(true)
        //提高渲染优先级
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        // 开启DOM storage API 功能
        settings.setDomStorageEnabled(true)
        // 开启database storage API功能
        settings.setDatabaseEnabled(true)
        // 开启Application Cache功能
        settings.setAppCacheEnabled(true)
        //设置脚本是否允许自动打开弹窗
        settings.setJavaScriptCanOpenWindowsAutomatically(true)
        //设置WebView是否支持多屏窗口
        settings.setSupportMultipleWindows(true)
        settings.setLoadWithOverviewMode(true)
        settings.setUseWideViewPort(true)
        settings.setBuiltInZoomControls(true)
        settings.setDisplayZoomControls(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (BuildConfig.DEBUG) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {//这个版本之后 被默认禁止
            settings.setAllowUniversalAccessFromFileURLs(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        //允许iframe与外部域名不一致的时候出现的 请求丢失cookie
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebview, true)
        }

    }

    fun loadUrl(url: String) {
        if (mWebview == null) return
        mWebview!!.loadUrl(url)
    }

    private fun loadJS(trigger: String) {
        mWebview!!.post(object : Runnable() {
            @Override
            fun run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mWebview!!.evaluateJavascript(trigger, null)
                } else {
                    mWebview!!.loadUrl(trigger)
                }
            }
        })
    }

    fun setPort(port: String) {
        this.port = port
    }

    fun setCanBack(canBack: Boolean) {
        isCanBack = canBack
    }

    fun setShowBackNav(show: Boolean) {
        isShowNavBack = show
        ivToolbarNavigation!!.setVisibility(if (show) View.VISIBLE else View.GONE)
    }

    fun setTitle(@StringRes resId: Int) {
        setTitle(getContext().getResources().getText(resId).toString())
    }

    fun setTitle(title: String) {
        if (tvToolbarTitle != null) {
            tvToolbarTitle!!.setText(title)
        }
    }

    @Override
    protected fun onStop() {
        super.onStop()
        if (mWebview != null) {
            mWebview!!.clearHistory()
            (mWebview!!.getParent() as ViewGroup).removeView(mWebview)
            mWebview!!.loadUrl("about:blank")
            mWebview!!.stopLoading()
            mWebview!!.setWebChromeClient(null)
            mWebview!!.setWebViewClient(null)
            mWebview!!.destroy()
        }
    }

    @Override
    fun show() {
        super.show()
        mWebview!!.bindBottomSheetDialog(getContainer(), object : View.OnTouchListener() {
            @Override
            fun onTouch(v: View, event: MotionEvent): Boolean {

                getProxysSelects()
                return false
            }
        })
    }

    private fun getProxysSelects() {
        if (System.currentTimeMillis() - time > 1000 * 3) {
            time = System.currentTimeMillis()
            UtilsKt.getSelectProxys()
        }
    }

    @Override
    fun onBackPressed() {
        super.onBackPressed()
    }

    companion object {


        private val GET_PROXYS_SELECTS = 10001
    }
}
