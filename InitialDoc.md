# Neutral News

**SCRUM MASTER: Martí Espinosa**

<!--
Documentación del proyecto extraída de Notion (profes solicitad acceso si queréis)
- [Project's initial doc](https://www.notion.so/Project-s-initial-doc-15b8b881099580d29139e8376bade4d3)
- [UML](https://www.notion.so/UML-15b8b881099580ef993cdd90388e7cf5)
- [RSS](https://www.notion.so/1698b881099580c994d1e0c569e7e8dc?v=ca15e0babad54c10bf4176137f7f1762)
- [Dailies](https://www.notion.so/Dailies-17b8b8810995800cbf06cfbf9fc7cbad)
- [References](https://www.notion.so/15b8b881099580ba8afedda48fa5d719?v=0d208c261c5e40109a4081f9ca0d7598)
-->

# Dailies

[agile.md](https://github.com/ITEC-BCN/projecte-2-dam-24-25-neutral-news/blob/main/agile.md)


# Project's initial doc

<aside>
Neutral News es el proyecto que desarrollaremos Martí Espinosa y Ezequiel Garibotto, nuestra idea es crear una aplicación que permita comparar noticias entre distintos medios de comunicación. 

Usualmente, vemos casos en los que una misma noticia es tratada de manera distinta en los medios de comunicación, dependiendo de sus intereses. Nuestro proyecto tiene como objetivo ayudar al público general a tener una visión más objetiva de las noticias, mejorando su capacidad crítica. 

Creemos que puede tener un impacto social significativo si se usa correctamente, y podría fomentar un consumo de noticias y medios de comunicación en general más sano.

</aside>

## Especificación funcional

Esta app recopila las noticias del día de los mayores medios de España mediante RSS. Con modelos de machine learning detecta la misma noticia entre los diferentes medios que hablen de ella y proporciona un resumen objetivo de esta.

El usuario verá una interfaz sencilla donde se ven los resúmenes neutrales de cada noticia y tiene la opción de ver la misma  en todos los medios vara ver que diferencias subjetivas existen.

Adicionalmente, la app puede filtrar las noticias según categoría, guardarlas como favorito, leer más tarde.

# Tecnologías a utilizar

Para el desarrollo de la aplicación, usaremos Firebase para nuestro Backend, RSS y scraping para obtener datos y haremos tanto una versión de iOS como Android

## Backend

- **Plataforma**: Firebase
- **Servicios de Firebase a utilizar**:
    - **Firestore**: Para almacenamiento de datos en tiempo real y sincronización entre dispositivos.
    - **Firebase** **ML**: Para integrar modelos de machine learning para generación de resúmenes y análisis de sesgos.
    - **Firebase Cloud Messaging (FCM)***: Para enviar notificaciones push relevantes a los usuarios sobre nuevas noticias o actualizaciones.
    - **Firebase Authentication***: Para gestionar el acceso seguro de los usuarios mediante correo electrónico, redes sociales u otros métodos.
    - **Firebase Hosting***: Para servir contenido estático o una posible versión web.
    - **Analytics***: Para obtener métricas detalladas sobre el comportamiento de los usuarios.

***A evaluar en el futuro**

## Otros

- **Scraping y RSS**: Uso de fuentes RSS para obtener datos de noticias de los principales medios. En caso de que ciertos medios no dispongan de RSS, se integrará scraping utilizando herramientas externas como Beautiful Soup (Python).
- **Control de versiones**: Git y GitHub.
- Agile: [Proofhub](https://itecbcn.proofhub.com/bappswift/#app/todos/project-8901679308/list-268270203798)

## **iOS**:

- **IDE**: Xcode
- **Lenguaje**: Swift
- **Framework de UI**: SwiftUI
- **Machine Learning**: CoreML y CreateML
- **Otros**:
    - Swift Data para almacenamiento local.
    - Combine para manejo de eventos asíncronos

## **Android**:

- **IDE**: Android Studio
- **Lenguaje**: Kotlin
- **Framework de UI**: Android View System (XML)
- **Machine Learning**: TensorFlow y Google Collab ([link](https://github.com/ITEC-BCN/projecte-2-dam-24-25-neutral-news/blob/main/neutral_news_categorization_unification_start.ipynb))

### Librerías y herramientas clave

1. **Almacenamiento local**:
    - **Room**: Para la gestión de bases de datos locales, ideal para almacenar datos estructurados como noticias guardadas y favoritos.
    - **SharedPrefs**: Utilizado para guardar configuraciones simples del usuario, como el idioma seleccionado o las preferencias de visualización.
2. **Manejo de eventos asíncronos**:
    - **LiveData**: Para observar y reaccionar a cambios en los datos en tiempo real.
    - **SingleLiveEvent**: Para gestionar eventos únicos como mensajes o navegación.
3. **Red y APIs**:
    - **Retrofit**: Para realizar llamadas a APIs de manera sencilla y estructurada.
    - **Gson**: Para convertir datos JSON a objetos Kotlin y viceversa.
    - **OkHttp**: Utilizado como cliente HTTP para optimizar las solicitudes.
4. **Diseño y experiencia de usuario**:
    - **Material Components for Android**: Para implementar componentes visuales modernos y consistentes.
    - **Lottie**: Para animaciones atractivas en la interfaz.
    - **Glide**: Para la carga eficiente de imágenes en la aplicación.
    - **Shimmer**: Para efectos visuales de carga en las vistas.
    - **SwipeRefreshLayout**: Para agregar la funcionalidad de arrastrar para actualizar en las listas de noticias.
    - **Cookie Bar**: Para mostrar mensajes de información de manera visual y atractiva.
    - **RangeSeekBar**: Para ofrecer filtros ajustables en las categorías o fechas de las noticias.
5. **Navegación**:
    - **Navigation Component**: Para manejar la navegación entre fragmentos y gestionar la pila de retroceso de forma automática.
6. **Internacionalización**:
    - **Localization**: Para gestionar traducciones y cambiar el idioma de la aplicación dinámicamente.
7. Opcionales (a evaluar en el futuro):
    - **ExoPlayer**: Para reproducir videos y contenido multimedia directamente desde la aplicación.

# A quién va dirigido

La aplicación principalmente va dirigida a personas de mediana edad que quieran mantenerse informadas de una forma más objetiva.

Creemos que nuestro público objetivo sería mayoritariamente personas a partir de los 16 años, edad en la que suelen comenzar a interesarse más por el mundo que les rodea. Sin embargo, no debería existir ninguna restricción de edad para consumir nuestro producto.

Además, podría ser de gran utilidad para:

- Estudiantes y académicos que necesiten información contrastada.
- Profesionales que requieran análisis imparciales para la toma de decisiones.
- Personas mayores que deseen acceder a noticias claras y resumidas.

# Extensiones futuras

En futuras versiones, podríamos incluir:

- **Personalización del feed**: Permitir a los usuarios seleccionar sus temas de interés y filtrar medios específicos.
- **Integración de idiomas**: Expandir la funcionalidad para soportar noticias en otros idiomas, lo cual abriría el mercado internacional.
- **Análisis visual**: Incorporar gráficos que muestren el sesgo de los medios en temas recurrentes o controversiales.
- **Verificación de datos**: Agregar una funcionalidad que identifique posibles noticias falsas utilizando bases de datos de fact-checking.
- **Modo educativo**: Ofrecer explicaciones sobre cómo se detectan los sesgos y cómo interpretar los resúmenes.

# Impacto social esperado

Neutral News tiene el potencial de:

- **Fomentar el pensamiento crítico**: Ayudar a los usuarios a identificar y cuestionar posibles sesgos.
- **Promover el diálogo**: Ofrecer una base más objetiva para debates sobre temas de actualidad.
- **Reducir la desinformación**: Facilitar el acceso a información precisa y balanceada.

Con este proyecto, buscamos empoderar a los usuarios en su consumo de información, fortaleciendo su capacidad para tomar decisiones informadas en un mundo saturado de datos y opiniones encontradas.

# Firebase functions back-end  
### Clonar el repositorio  
Acceder a functions
cd functions

### Activar el entorno virtual en python 3  
python3 -m venv venv

unix  
<kbd>source venv/bin/activate</kbd>

pwsh  
<kbd>.\venv\Scripts\Activate</kbd>

cmd  
<kbd>venv\Scripts\activate</kbd>

### Instalar requerimientos
pip install -r requirements.txt

### Instalar Firebase CLI
Verificar instalación primero
<kbd>firebase --version</kbd>
En caso de que no esté instalado, instalar
<kbd>npm install -g firebase-tools</kbd>

### Hacer login en firebase  
<kbd>firebase login</kbd>

Si no funciona, agregar <kbd>npm</kbd> a las variables de entorno del sistema  

<kbd>firebase emulators:start --only functions</kbd>
Si el anterior no da error  
<kbd>firebase deploy --only functions</kbd>


# Diseño

[Figma](https://www.figma.com/design/BNs3Fzqmo6G3ecXyrBlQJZ/Neutral-News?node-id=2-2&node-type=canvas&t=zEaU5H4yKoBB78g8-0)

# UML
## Casos de uso
![LP5DJWCn38NtFeNL5Iowe3zRL5G9TjrDzsPYXg1CN3aUAe4u30VW5BqOdcewKh9walEzb-ViYf7Qq2T8_6PeWXgxTyCGbLkBaW6eDL5ioa0Q1QbW0p0EANU9SN7WmPGI7lXS5lWDw6Set5BnuLxw4oafPh7OHeSvcpBs7ABuzT5j_r0pn-XHANuv5TWeUKXWJ7Jpm3lbClISKLu_sEAdt4z6](https://github.com/user-attachments/assets/8f5c8668-90b1-4471-893a-458125688da7)
## Clases
![fLJBJiCm4BpdAtnigVe34QgAg0e7L24G7x2rjs4bnwvifmSU_uxZnCPfmaDmZ6TdTcVMyTewCAwfPS4bMCjssX4ds67Z34ZsMYmPYP8zEaEgyX-EN2Dr3sVd1crJene3N2k7YiG4HtMzxBP1NrEoExBkiXzWrLB1OQMDh-YnZjb25csbazI2wRrlWkQXcs8s7qCRT20bsWnxBuQclt8xjZdb](https://github.com/user-attachments/assets/f7c79289-482d-4e31-8815-020c65330b25)


# References
[Ground News](https://ground.news/)

[Kaggle](https://www.kaggle.com/datasets)

[Hugging Face](https://huggingface.co/models)

[RSS List](https://15mpedia.org/wiki/Lista_de_RSS_de_medios)

# Investigation sources: Datasets

[BEAD](https://huggingface.co/datasets/shainar/BEAD)

[SBERT multilingual](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2?utm_source=chatgpt.com)

[GDELT PROJECT](https://www.gdeltproject.org/)

[MIIND](https://learn.microsoft.com/es-es/azure/open-datasets/dataset-microsoft-news?utm_source=chatgpt.com&tabs=azureml-opendatasets)

[Catalonia independence corpus](https://github.com/ixa-ehu/catalonia-independence-corpus)

[Portal Odesia](https://portal.odesia.uned.es/datasets?field_ano_value=1&field_dominio_value%5B0%5D=News&field_idioma_iso639_1_value%5B0%5D=es&utm_source=chatgpt.com)

[Large Spanish Corpus](https://huggingface.co/datasets/josecannete/large_spanish_corpus?utm_source=chatgpt.com)

[Spanish news (LaRazón-Público)](https://www.kaggle.com/datasets/josemamuiz/noticias-laraznpblico/data)

