package com.example.neutralnews_android.ui.main.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.example.neutralnews_android.R
import com.example.neutralnews_android.data.bean.settings.SettingsBean
import com.example.neutralnews_android.databinding.FragmentSettingsBinding
import com.example.neutralnews_android.di.view.AppFragment
import com.example.neutralnews_android.ui.main.MainActivity
import com.example.neutralnews_android.ui.main.today.TodayFragment
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "SettingsFragment"

@AndroidEntryPoint
class SettingsFragment : AppFragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val vm: SettingsFragmentVM by viewModels()
    // Valores por defecto
    private val defaultTitleSize = "18"
    private val defaultDescriptionSize = "14"
    private val defaultDetailsSize = "16"
    private val defaultDateFormat = "DD-MM-YYYY"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView: Iniciando creación de vista")
        // Siempre crear nuevo binding para evitar problemas de estado
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_settings, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.vm = vm
        Log.d(TAG, "onCreateView: Binding inicializado")

        updateSettingsValues()
        setupManualTextSync()
        setupSpinner()
        setupOnClickListeners()

        Log.d(TAG, "onCreateView: Vista configurada completamente")
        return binding.root
    }

    private fun setupManualTextSync() {
        // Configurar los EditText para actualizar visualmente
        binding.etTitleFontSize.setText(vm.fieldTitleFontSize.value ?: defaultTitleSize)
        binding.etDescriptionFontSize.setText(vm.fieldDescriptionFontSize.value ?: defaultDescriptionSize)
        binding.etDetailsTextFontSize.setText(vm.fieldDetailsTextFontSize.value ?: defaultDetailsSize)

        // Añadir TextChangedListener para sincronización bidireccional sin Data Binding
        setupTextWatcher(binding.etTitleFontSize) { text ->
            Log.d(TAG, "UI → VM: etTitleFontSize cambiado a: $text")
            if (vm.fieldTitleFontSize.value != text) {
                vm.fieldTitleFontSize.value = text
            }
        }

        setupTextWatcher(binding.etDescriptionFontSize) { text ->
            Log.d(TAG, "UI → VM: etDescriptionFontSize cambiado a: $text")
            if (vm.fieldDescriptionFontSize.value != text) {
                vm.fieldDescriptionFontSize.value = text
            }
        }

        setupTextWatcher(binding.etDetailsTextFontSize) { text ->
            Log.d(TAG, "UI → VM: etDetailsTextFontSize cambiado a: $text")
            if (vm.fieldDetailsTextFontSize.value != text) {
                vm.fieldDetailsTextFontSize.value = text
            }
        }

        // Observar cambios en el ViewModel
        vm.fieldTitleFontSize.observe(viewLifecycleOwner) { value ->
            Log.d(TAG, "VM → UI: Título cambiado a: $value")
            if (binding.etTitleFontSize.text.toString() != value) {
                binding.etTitleFontSize.setText(value)
            }
        }

        vm.fieldDescriptionFontSize.observe(viewLifecycleOwner) { value ->
            Log.d(TAG, "VM → UI: Descripción cambiada a: $value")
            if (binding.etDescriptionFontSize.text.toString() != value) {
                binding.etDescriptionFontSize.setText(value)
            }
        }

        vm.fieldDetailsTextFontSize.observe(viewLifecycleOwner) { value ->
            Log.d(TAG, "VM → UI: Detalles cambiados a: $value")
            if (binding.etDetailsTextFontSize.text.toString() != value) {
                binding.etDetailsTextFontSize.setText(value)
            }
        }
    }

    private fun setupTextWatcher(editText: EditText, onTextChanged: (String) -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged(s?.toString() ?: "")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Fragmento resumido")
        // Verificar valores actuales
        Log.d(TAG, "EditText valores actuales - Título: ${binding.etTitleFontSize.text}, " +
                "Descripción: ${binding.etDescriptionFontSize.text}, " +
                "Detalles: ${binding.etDetailsTextFontSize.text}")
    }

    private fun updateSettingsValues() {
        Log.d(TAG, "updateSettingsValues: Iniciando obtención de valores")
        val todayFragment = parentFragmentManager.findFragmentByTag("TodayFragment") as? TodayFragment

        if (todayFragment != null) {
            val titleFontSize = todayFragment.getTitleFontSize().toString()
            val descriptionFontSize = todayFragment.getDescriptionFontSize().toString()
            val dateFormat = todayFragment.getDateFormat()

            // Inicializar valores en el ViewModel
            vm.fieldTitleFontSize.value = titleFontSize
            vm.fieldDescriptionFontSize.value = descriptionFontSize
            vm.fieldDetailsTextFontSize.value = "16" // Valor por defecto
            vm.fieldDateFormat.value = dateFormat

            // Actualizar etiquetas
            binding.tvActualTextSize.text = getString(R.string.actual_title_size, titleFontSize)
            binding.tvActualDescriptionSize.text = getString(R.string.actual_description_size, descriptionFontSize)
            binding.tvActualDateFormat.text = getString(R.string.actual_date_format, dateFormat)
        } else {
            Log.e(TAG, "updateSettingsValues: TodayFragment no encontrado - usando valores predeterminados")
            // Usar valores predeterminados cuando no se encuentra TodayFragment
            vm.fieldTitleFontSize.value = defaultTitleSize
            vm.fieldDescriptionFontSize.value = defaultDescriptionSize
            vm.fieldDetailsTextFontSize.value = defaultDetailsSize
            vm.fieldDateFormat.value = defaultDateFormat

            // Actualizar etiquetas con valores predeterminados
            binding.tvActualTextSize.text = getString(R.string.actual_title_size, defaultTitleSize)
            binding.tvActualDescriptionSize.text = getString(R.string.actual_description_size, defaultDescriptionSize)
            binding.tvActualDateFormat.text = getString(R.string.actual_date_format, defaultDateFormat)
        }
    }

    private fun setupSpinner() {
        val spinnerDateFormat: Spinner = binding.spinnerDateFormat
        Log.d(TAG, "setupSpinner: Configurando spinner con formato: ${vm.fieldDateFormat.value}")

        // Configurar el spinner
        val dateFormats = resources.getStringArray(R.array.date_format_options)
        val currentFormat = vm.fieldDateFormat.value ?: defaultDateFormat
        val position = dateFormats.indexOf(currentFormat)
        Log.d(TAG, "Posición para formato '$currentFormat': $position")

        if (position >= 0) {
            spinnerDateFormat.setSelection(position)
        }

        spinnerDateFormat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedDateFormat = parent.getItemAtPosition(position).toString()
                Log.d(TAG, "Spinner: formato seleccionado: $selectedDateFormat")
                vm.fieldDateFormat.value = selectedDateFormat
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No hacer nada
            }
        }
    }

    private fun passSettingsToTodayFragment(settings: SettingsBean) {
        Log.d(TAG, "passSettingsToTodayFragment: Aplicando configuración: $settings")

        // En lugar de crear una nueva instancia, busca el fragmento existente
        val todayFragment = parentFragmentManager.findFragmentByTag("TodayFragment") as? TodayFragment

        if (todayFragment != null) {
            try {
                todayFragment.applySettings(settings)
                Log.d(TAG, "Configuración aplicada correctamente al TodayFragment existente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al aplicar configuración: ${e.message}")
            }
        } else {
            // Si no se encuentra el fragmento, almacena la configuración en MainActivity
            (requireActivity() as? MainActivity)?.let {
                Log.d(TAG, "TodayFragment no encontrado. Guardando configuración en MainActivity")
                it.saveSettingsForLater(settings)
                it.navigateToTodayFragment()
            } ?: run {
                Log.e(TAG, "No se pudo obtener referencia a MainActivity")
                msgError("Error al guardar la configuración")
            }
        }
    }

    private fun setupOnClickListeners() {
        vm.obrClick.observe(viewLifecycleOwner) { view ->
            when (view.id) {
                R.id.btnSaveSettings -> {
                    val titleText = binding.etTitleFontSize.text.toString()
                    val descriptionText = binding.etDescriptionFontSize.text.toString()
                    val detailsText = binding.etDetailsTextFontSize.text.toString()

                    Log.d(TAG, "Botón guardar presionado con valores - Título: $titleText, Descripción: $descriptionText, Detalles: $detailsText, Fecha: ${vm.fieldDateFormat.value}")

                    fun isNumeric(text: String) = text.isNotEmpty() && text.all { it.isDigit() }

                    if (titleText.isBlank() || descriptionText.isBlank() || detailsText.isBlank()) {
                        msgInfo("Por favor, complete todos los campos")
                        return@observe
                    }
                    if (!isNumeric(titleText) || !isNumeric(descriptionText) || !isNumeric(detailsText)) {
                        msgInfo("Solo se permiten números en los campos de tamaño")
                        return@observe
                    }

                    val settings = SettingsBean(
                        titleFontSize = titleText.toInt(),
                        descriptionFontSize = descriptionText.toInt(),
                        detailsTextFontSize = detailsText.toInt(),
                        dateFormat = vm.fieldDateFormat.value ?: defaultDateFormat
                    )

                    passSettingsToTodayFragment(settings)
                    msgNormal("Configuración guardada correctamente")
                }
                R.id.imgBack -> {
                    Log.d(TAG, "Navegando de vuelta a TodayFragment")
                    (requireActivity() as? MainActivity)?.navigateToTodayFragment()
                }
            }
        }
    }
}