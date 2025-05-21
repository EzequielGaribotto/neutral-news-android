package com.example.neutralnews_android.ui.main.news

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.viewModels
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.neutralnews_android.BR
import com.example.neutralnews_android.R
import com.example.neutralnews_android.data.Constants.LocalNew.NEWS_LIST
import com.example.neutralnews_android.data.Constants.LocalNew.NEW_DATA
import com.example.neutralnews_android.data.Constants.LocalNew.NEW_ID
import com.example.neutralnews_android.data.bean.news.NewsBean
import com.example.neutralnews_android.data.bean.settings.SettingsBean
import com.example.neutralnews_android.databinding.ActivityNewDetailBinding
import com.example.neutralnews_android.databinding.RowSourceNewsBinding
import com.example.neutralnews_android.di.adapter.SimpleRecyclerViewAdapter
import com.example.neutralnews_android.di.view.AppActivity
import com.example.neutralnews_android.util.fullScreen
import com.example.neutralnews_android.util.openWebView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividad que muestra los detalles de una noticia.
 *
 * Esta actividad se encarga de mostrar los detalles completos de una noticia.
 * La noticia se pasa a través de un `Intent`, y la actividad observa un evento
 * para abrir el enlace relacionado con la noticia.
 *
 * @constructor Crea una nueva instancia de la actividad de detalles de la noticia.
 */
@AndroidEntryPoint
class NewDetailActivity : AppActivity() {

    private lateinit var binding: ActivityNewDetailBinding
    private val vm: NewDetailActivityVM by viewModels()

    // Datos recibidos
    private var bean: NewsBean = NewsBean()
    private var newId: Long = 0
    private var newsList: List<NewsBean> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!::binding.isInitialized) {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_new_detail)
            binding.vm = vm
        }
        fullScreen()

        // Recuperar el objeto NewsBean desde el Intent
        try {
            bean = Gson().fromJson(intent.getStringExtra(NEW_DATA), object : TypeToken<NewsBean>() {}.type)
            newsList = Gson().fromJson(intent.getStringExtra(NEWS_LIST), object : TypeToken<List<NewsBean>>() {}.type)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.bean = bean
        newId = intent.getLongExtra(NEW_ID, 0)
        binding.tvCategory.text = bean.category?.uppercase()
        if (newsList.isEmpty()) {
            binding.tvToolbarTitle.text = bean.sourceMedium?.name?.uppercase()
            binding.tvImageDisclaimer.text =
                getString(R.string.image_disclaimer, bean.sourceMedium?.name)
            binding.tvToolbarSubtitle.text = bean.title
        } else {
            binding.tvToolbarTitle.text = getString(R.string.neutral_new)
            binding.tvImageDisclaimer.text = getString(R.string.image_disclaimer, bean.imageMedium)
            binding.tvToolbarSubtitle.text = bean.title
        }

        // Load image and extract dominant color
        loadNewsImage()
        setUpClickObservers()
        setUpScrollListener()
        setUpSourceNewsAdapter()
    }

    private fun setUpClickObservers() {
        vm.openLinkEvent.observe(this) { url ->
            // Animar el contenedor principal para dar feedback
            binding.svContent.animate()
                .alpha(0.8f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(150)
                .withEndAction {
                    binding.svContent.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()

                    openWebView(url) { success ->

                    }
                    overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
                }
                .start()
        }

        vm.obrClick.observe(this) { view ->
            when (view.id) {
                R.id.btnBack -> {
                    // Animar el botón de retroceso
                    view.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(100)
                        .withEndAction {
                            view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()

                            // Cerrar la actividad con animación
                            finishWithAnimation()
                        }
                        .start()
                }
            }
        }
    }

    private fun finishWithAnimation() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    // Sobrescribir onBackPressed para usar la animación personalizada
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithAnimation()
    }

    @SuppressLint("StringFormatMatches")
    private fun setUpScrollListener() {
        // Textos que se mostrarán
        val infoText = if (bean.sourceIds != null) getString(
            R.string.source_coverage_count_info,
            bean.sourceIds?.size
        ) else getString(R.string.neutral_score_info, bean.neutralScore)

        // Configuración inicial
        binding.tvToolbarSubtitle.text = infoText
        binding.tvToolbarSubtitle.typeface = Typeface.SANS_SERIF
        binding.tvToolbarSubtitle.alpha = 1f

        // Constantes para la animación
        val scrollThreshold = 300
        val animDuration = 600L
        val interpolator = AccelerateDecelerateInterpolator()

        // Variable para controlar el estado actual
        var isShowingTitle = false

        binding.svContent.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val shouldShowTitle = scrollY > scrollThreshold

            // Sólo animar si hay un cambio de estado
            if (shouldShowTitle != isShowingTitle) {
                isShowingTitle = shouldShowTitle

                // Cancelar cualquier animación en curso
                binding.tvToolbarSubtitle.animate().cancel()

                // Animar el cambio con crossfade
                binding.tvToolbarSubtitle.animate().alpha(0f).setDuration(animDuration / 2)
                    .setInterpolator(interpolator).withEndAction {
                        // Cambiar texto y tipografía en el punto medio de la animación
                        if (isShowingTitle) {
                            binding.tvToolbarSubtitle.text = bean.title
                            // SERIF BOLD
                            binding.tvToolbarSubtitle.typeface = Typeface.create(
                                Typeface.SERIF,
                                Typeface.BOLD
                            )
                        } else {
                            binding.tvToolbarSubtitle.text = infoText
                            binding.tvToolbarSubtitle.typeface = Typeface.SANS_SERIF
                        }

                        // Completar la animación mostrando el nuevo texto
                        binding.tvToolbarSubtitle.animate().alpha(1f).setDuration(animDuration / 2)
                            .setInterpolator(interpolator).start()
                    }.start()
            }
        }
    }

    private fun setUpSourceNewsAdapter() {
        val newsAdapter = SimpleRecyclerViewAdapter(
            R.layout.row_source_news,
            BR.newsBean,
            object : SimpleRecyclerViewAdapter.SimpleCallback<NewsBean, RowSourceNewsBinding> {
                override fun onItemClick(v: View, m: NewsBean) {
                    when (v.id) {
                        R.id.clNews -> {
                            println("PubDate ${m.pubDate} - ${m.sourceMedium?.name}")
                            startNewActivity(newIntent(this@NewDetailActivity, 0, m, emptyList()))


                        }
                    }
                }

                override fun onViewBinding(
                    holder: SimpleRecyclerViewAdapter.SimpleViewHolder<RowSourceNewsBinding>,
                    m: NewsBean,
                    pos: Int
                ) {
                    println("New ${m.sourceMedium} neutral score: ${m.neutralScore}")
                    holder.binding.apply {
                        executePendingBindings()
                    }
                }
            })

        binding.rvFiveNews.apply {
            layoutManager =
                LinearLayoutManager(this@NewDetailActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = newsAdapter

            val screenWidth = resources.displayMetrics.widthPixels
            val itemWidth = (screenWidth * 0.66).toInt()

            if (itemDecorationCount > 0) {
                for (i in itemDecorationCount - 1 downTo 0) {
                    removeItemDecorationAt(i)
                }
            }

            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    view.layoutParams.width = itemWidth

                    val position = parent.getChildAdapterPosition(view)
                    val itemCount = parent.adapter?.itemCount ?: 0

                    if (position < itemCount - 1) {
                        val scale = context.resources.displayMetrics.density
                        outRect.right = (8 * scale + 0.5f).toInt()
                    } else {
                        outRect.right = 0
                    }
                }
            })
        }
        newsAdapter.list = newsList
    }

    /**
     * Loads the news image and extracts the dominant color
     */
    private fun loadNewsImage() {
        bean.imageUrl?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable?>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Puedes cambiar el método usado aquí para probar las alternativas
                        //extractDominantColor(resource.toBitmap())
                        extractAverageColor(resource.toBitmap())
                        // extractColorWithVibrant(resource.toBitmap())
                        return false
                    }
                })
                .into(binding.ivNewsImage)
        }
    }

    /**
     * Extracts the dominant color from the image and applies it to the background
     */
    private fun extractDominantColor(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val dominantColor = palette?.getDominantColor(resources.getColor(R.color.black_95, theme))
            dominantColor?.let {
                // Create gradient drawable
                val colors = intArrayOf(
                    it,                      // Full dominant color at top
                    adjustAlpha(it, 0.3f),   // 30% opacity in middle
                    Color.TRANSPARENT        // Transparent at bottom
                )

                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    colors
                )

                // Apply to background
                binding.backgroundGradient.background = gradientDrawable

                // Also apply solid color to statusBar for consistency
                window.statusBarColor = it
            }
        }
    }

    /**
     * Alternativa 1: Extrae el color promedio de la imagen muestreando píxeles
     * Esta aproximación calcula un promedio de los colores de la imagen
     */
    private fun extractAverageColor(bitmap: Bitmap) {
        // Redimensionar para mejorar rendimiento
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)

        var redSum = 0
        var greenSum = 0
        var blueSum = 0
        var pixelCount = 0

        // Muestrear cada 5 píxeles para mejorar rendimiento
        for (y in 0 until resizedBitmap.height step 5) {
            for (x in 0 until resizedBitmap.width step 5) {
                val pixel = resizedBitmap.getPixel(x, y)

                // Ignorar píxeles transparentes o demasiado oscuros
                if (Color.alpha(pixel) > 128 && (Color.red(pixel) + Color.green(pixel) + Color.blue(
                        pixel
                    )) > 150
                ) {
                    redSum += Color.red(pixel)
                    greenSum += Color.green(pixel)
                    blueSum += Color.blue(pixel)
                    pixelCount++
                }
            }
        }

        // Si no encontramos suficientes píxeles, usar color predeterminado
        if (pixelCount == 0) {
            val defaultColor = resources.getColor(R.color.black_95, theme)
            applyColorToBackground(defaultColor)
            return
        }

        // Calcular el promedio
        val avgRed = redSum / pixelCount
        val avgGreen = greenSum / pixelCount
        val avgBlue = blueSum / pixelCount

        val averageColor = Color.rgb(avgRed, avgGreen, avgBlue)

        // Aplicar el color
        applyColorToBackground(averageColor)

        // Liberar memoria
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }
    }

    /**
     * Alternativa 2: Usa la paleta Vibrant para extraer colores más vibrantes
     * En lugar de usar el color dominante, usa colores vibrantes para un resultado más atractivo
     */
    private fun extractColorWithVibrant(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            // Intentar obtener un color vibrante
            var selectedColor = palette?.getVibrantColor(0)

            // Si no hay color vibrante, intentar con otros
            if (selectedColor == 0) {
                selectedColor = palette?.getLightVibrantColor(0)
            }
            if (selectedColor == 0) {
                selectedColor = palette?.getDarkVibrantColor(0)
            }
            if (selectedColor == 0) {
                selectedColor = palette?.getMutedColor(0)
            }
            if (selectedColor == 0) {
                selectedColor =
                    palette?.getDominantColor(resources.getColor(R.color.black_95, theme))
            }

            // Si encontramos un color, aplicarlo
            if (selectedColor != 0 && selectedColor != null) {
                // Si el color es demasiado claro, oscurecerlo ligeramente
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(selectedColor, hsl)

                // Asegurar que la luminosidad no sea demasiado alta (para fondos oscuros)
                if (hsl[2] > 0.7f) {
                    hsl[2] = 0.7f
                    selectedColor = ColorUtils.HSLToColor(hsl)
                }

                applyColorToBackground(selectedColor)
            } else {
                // Si todo falla, usar el color predeterminado
                val defaultColor = resources.getColor(R.color.black_95, theme)
                applyColorToBackground(defaultColor)
            }
        }
    }

    /**
     * Función auxiliar para aplicar el color extraído al fondo
     */
    private fun applyColorToBackground(color: Int) {
        // Crear gradiente
        val colors = intArrayOf(
            color,                    // Color completo arriba
            adjustAlpha(color, 0.3f), // 30% de opacidad en el medio
            Color.TRANSPARENT         // Transparente abajo
        )

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, colors
        )

        // Aplicar al fondo
        if (bean.date != null) {
            binding.backgroundGradient.setBackgroundColor(color)
        } else {
            binding.backgroundGradient.background = gradientDrawable
        }
        // También aplicar a la barra de estado
        window.statusBarColor = color
    }

    /**
     * Helper function to adjust alpha value of a color
     */
    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        return (color and 0x00ffffff) or (alpha shl 24)
    }

    companion object {
        /**
         * Crea un `Intent` para abrir la actividad de detalles de la noticia.
         *
         * @param c El contexto de la aplicación.
         * @param newId El ID de la noticia.
         * @param newData Los datos de la noticia, que son opcionales.
         * @param newsList Lista de noticias relacionadas.
         * @param settings Configuración de la aplicación.
         * @return Un `Intent` para abrir la actividad de detalles de la noticia.
         */
        fun newIntent(c: Context, newId: Long, newData: NewsBean = NewsBean(), newsList: List<NewsBean>, settings: SettingsBean = SettingsBean()): Intent {
            val intent = Intent(c, NewDetailActivity::class.java)
            intent.putExtra(NEW_ID, newId)
            intent.putExtra(NEW_DATA, Gson().toJson(newData))
            intent.putExtra(NEWS_LIST, Gson().toJson(newsList))
            intent.putExtra("settings", settings)
            return intent
        }
    }
}
