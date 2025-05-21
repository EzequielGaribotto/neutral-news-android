package com.example.neutralnews_android.util.string

import java.text.Normalizer

/**
 * Extensión de la clase String que normaliza una cadena de texto.
 *
 * Esta función realiza las siguientes operaciones en la cadena:
 * - Normaliza la cadena a la forma NFD.
 * - Elimina los signos diacríticos.
 * - Convierte todos los caracteres a minúsculas.
 * - Reemplaza los espacios por guiones.
 * - Filtra los caracteres para que solo queden letras, dígitos y guiones.
 *
 * Ejemplo: "Hola Mundo!" se convierte en "hola-mundo".
 * @return La cadena normalizada.
 */
fun String.normalized(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
        .filter { it.isLetterOrDigit() || it == '-' }
}