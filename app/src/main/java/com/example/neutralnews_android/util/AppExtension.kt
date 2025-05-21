@file:Suppress("unused")

package com.example.neutralnews_android.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.Uri
import android.nfc.NfcManager
import android.os.Build
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.neutralnews_android.BuildConfig
import com.example.neutralnews_android.R
import com.example.neutralnews_android.data.Constants.PrefsKeys.EN
import com.example.neutralnews_android.data.Constants.PrefsKeys.ES
import com.example.neutralnews_android.data.Constants.PrefsKeys.SELECTED_LANGUAGE
import com.example.neutralnews_android.util.preferences.Prefs
import com.makeramen.roundedimageview.RoundedImageView
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn


@BindingAdapter("imageUrl")
fun setImageUrl(view: RoundedImageView, drawable: Drawable?) {
    view.setImageDrawable(drawable)
}

@BindingAdapter("imageResource")
fun bindImageResource(imageView: ImageView, resource: Int) {
    imageView.setImageResource(resource)
}

@BindingAdapter("drawableStartCompat")
fun setDrawableStartCompat(textView: TextView, drawableResId: Int?) {
    drawableResId?.let {
        val drawable = ContextCompat.getDrawable(textView.context, it)
        textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
    }
}

@BindingAdapter("onScrollChanged")
fun setOnScrollChanged(scrollView: ScrollView, listener: (Int, Int) -> Unit) {
    scrollView.viewTreeObserver.addOnScrollChangedListener {
        val scrollY = scrollView.scrollY
        val scrollRange = scrollView.getChildAt(0).height - scrollView.height
        listener(scrollY, scrollRange)
    }
}

/**
 * Load image from URL with Glide
 * @param url image URL
 * @param placeholder placeholder drawable
 * @param viewWidth width of the image view
 * @param viewHeight height of the image view
 */
@SuppressLint("CheckResult")
@BindingAdapter("imageUrl", "placeholder", "viewWidth", "viewHeight", requireAll = false)
fun ImageView.loadImage(
    url: String?,
    placeholder: Drawable?,
    viewWidth: Int?,
    viewHeight: Int?
) {
    val options = RequestOptions()
    if (viewWidth != null && viewHeight != null) options.override(viewWidth, viewHeight)
    if (placeholder != null) options.placeholder(placeholder)
    Glide.with(this.context).load(url).apply(options).into(this)
}


/**
 * Load image from resources with Glide
 * @param res resource id
 * @param placeholder placeholder drawable
 * @param viewWidth width of the image view
 * @param viewHeight height of the image view
 */
@SuppressLint("CheckResult")
@BindingAdapter("imageRes", "placeholder", "viewWidth", "viewHeight", requireAll = false)
fun ImageView.loadImageRes(
    res: Int,
    placeholder: Drawable?,
    viewWidth: Int?,
    viewHeight: Int?
) {
    val options = RequestOptions()
    if (viewWidth != null && viewHeight != null) options.override(viewWidth, viewHeight)
    if (placeholder != null) options.placeholder(placeholder)
    Glide.with(this.context).load(res).apply(options).into(this)
}

//Log for activity
fun Activity.loggerD(msg: String) {
    if (BuildConfig.DEBUG)
        Log.d(this.localClassName, msg)
}

fun Activity.loggerE(msg: String) {
    if (BuildConfig.DEBUG)
        Log.e(this.localClassName, msg)
}

fun Activity.loggerI(msg: String) {
    if (BuildConfig.DEBUG)
        Log.i(this.localClassName, msg)
}

fun Activity.loggerV(msg: String) {
    if (BuildConfig.DEBUG)
        Log.v(this.localClassName, msg)
}

fun Activity.loggerW(msg: String) {
    if (BuildConfig.DEBUG)
        Log.w(this.localClassName, msg)
}

//Log for fragment
fun Fragment.loggerD(msg: String) {
    if (BuildConfig.DEBUG)
        Log.d(this.javaClass.simpleName, msg)
}

fun Fragment.loggerE(msg: String) {
    if (BuildConfig.DEBUG)
        Log.e(this.javaClass.simpleName, msg)
}

fun Fragment.loggerI(msg: String) {
    if (BuildConfig.DEBUG)
        Log.i(this.javaClass.simpleName, msg)
}

fun Fragment.loggerV(msg: String) {
    if (BuildConfig.DEBUG)
        Log.v(this.javaClass.simpleName, msg)
}

fun Fragment.loggerW(msg: String) {
    if (BuildConfig.DEBUG)
        Log.w(this.javaClass.simpleName, msg)
}

//Log for context
fun Context.loggerD(msg: String) {
    if (BuildConfig.DEBUG)
        Log.d(this.javaClass.simpleName, msg)
}

fun Context.loggerE(msg: String) {
    if (BuildConfig.DEBUG)
        Log.e(this.javaClass.simpleName, msg)
}

fun Context.loggerI(msg: String) {
    if (BuildConfig.DEBUG)
        Log.i(this.javaClass.simpleName, msg)
}

fun Context.loggerV(msg: String) {
    if (BuildConfig.DEBUG)
        Log.v(this.javaClass.simpleName, msg)
}

fun Context.loggerW(msg: String) {
    if (BuildConfig.DEBUG)
        Log.w(this.javaClass.simpleName, msg)
}


//Open Web View
fun Context.openWebView(url: String, callback: (Boolean) -> Unit) {
    try {
        val customTabColorSchemeParams = CustomTabColorSchemeParams.Builder()
        customTabColorSchemeParams.setToolbarColor(ContextCompat.getColor(this, R.color.black))
        customTabColorSchemeParams.setNavigationBarColor(ContextCompat.getColor(this, R.color.black))

        val builder = CustomTabsIntent.Builder()
        builder.setColorScheme(COLOR_SCHEME_DARK)
        builder.setColorSchemeParams(COLOR_SCHEME_DARK, customTabColorSchemeParams.build())
        builder.setDefaultColorSchemeParams(customTabColorSchemeParams.build())
        builder.setShareState(CustomTabsIntent.SHARE_STATE_OFF)

        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
        callback(true)
    } catch (e: Exception) {
        e.printStackTrace()
        callback(false)
    }
}

//Open Keyboard
fun View.showKeyboard() {
    this.requestFocus()
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

//Close Keyboard
fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}


//Permission
fun Context.checkGalleryPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    } else {
        false
    }
}

fun Context.checkCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

fun Context.checkNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun Context.checkLocationPermission(): Boolean {
    return if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else {
        false
    }
}

fun getTOSTitlesRegex(): Regex {
    val regex = when (Prefs.getString(SELECTED_LANGUAGE, EN)) {
        EN -> {
            Regex("""\n\n\d{1,2}\.([\s\S]*?)\n""")
        }

        ES -> {
            Regex("""\n\n\d{1,2}\.([\s\S]*?)\n""")
        }

        else -> {
            Regex("""\n\n\d{1,2}\.([\s\S]*?)\n""")
        }
    }
    return regex
}

fun Context.checkBluetoothPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
    }
}


//Check GPS
fun Context.isGPS(): Boolean {
    val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

//Check NFC
fun Context.isNFC(): Boolean {
    val manager = getSystemService(Context.NFC_SERVICE) as NfcManager
    val adapter = manager.defaultAdapter
    return adapter != null && adapter.isEnabled
}

//Is Show Password
fun EditText.isShowPassword(isShow: Boolean) {
    if (isShow) {
        this.transformationMethod = HideReturnsTransformationMethod.getInstance()
        this.setSelection(this.text.length)
    } else {
        this.transformationMethod = PasswordTransformationMethod.getInstance()
        this.setSelection(this.text.length)
    }
}

fun showBlurEffect(context: Context, view: ViewGroup) {
    // Wait for the view to be laid out before applying blur
    view.post {
        try {
            // Check if view has valid dimensions
            if (view.width <= 0 || view.height <= 0) {
                println("View dimensions not valid yet: ${view.width}x${view.height}")
                // Try again after a delay
                view.postDelayed({ showBlurEffect(context, view) }, 100)
                return@post
            }

            val bitmap = getBitmapFromView(view)
            if (bitmap != null && !bitmap.isRecycled) {
                Blurry.with(context).radius(10).sampling(2).onto(view)
            } else {
                println("Bitmap is null or already recycled")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun removeBlurEffect(viewGroup: ViewGroup) {
    Blurry.delete(viewGroup)
}

private fun getBitmapFromView(view: View): Bitmap? {
    return try {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

//Full Screen
fun Activity.fullScreen(isDark: Boolean = false) {
    window.apply {
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        if (isDark) {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        statusBarColor = Color.TRANSPARENT
    }
}

//Vibrate
fun View.vibrate() {
    @Suppress("DEPRECATION")
    performHapticFeedback(
        HapticFeedbackConstants.VIRTUAL_KEY,
        // Ignore device's setting. Otherwise, you can use FLAG_IGNORE_VIEW_SETTING to ignore view's setting.
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
}


//Get App Version
fun Context.getAppVersionCode(): Int {
    return packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
}


fun ExoPlayer.playingProgressListener() = flow {
    while (true) {
        delay(10)
        if (isPlaying) {
            emit(currentPosition)
        }
    }
}.flowOn(Dispatchers.Main)