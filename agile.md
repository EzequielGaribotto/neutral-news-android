# Neutral News Agile

Antes de empezar a programar, hemos hecho algunos pasos previos en Notion. Primero, empezamos haciendo el documento inicial donde detallabamos qué y cómo ibamos a hacer el proyecto. Luego, hicimos diagramas UML de referencia para visualizar la arquitectura de datos. También recopilamos enlaces útiles, incluidos los RSS de los principales medios de comunicación de España.

## Sprint 1  
### MVP  
Implementar las características básicas de la aplicación, incluyendo la obtención de noticias de fuentes RSS, el análisis de los datos en modelos estructurados y la visualización de los artículos de noticias en la pantalla con una interfaz simple y fácil de usar.

### Dailies  
#### 8 de enero de 2025
**iOS**
- Investigar las fuentes RSS de los medios de comunicación y guardar los enlaces en Notion.  
- Diseñar la pantalla principal en Figma.

**Android**
- Set up del proyecto de Android, con dependency injections, recycler view y todo lo necesario para el desarrollo Frontend de la aplicación.

#### 10 de enero de 2025
**iOS**
- Crear la estructura de datos del modelo de noticias con la información necesaria de los RSS de El País.  
- Realizar la primera llamada a los RSS.

**Android**
- Continuación del set up del proyecto de Android, con utilidades varias, empezar a crear Actividad Inicial (Bottom Navigation View) y sus Fragmentos 

#### 14 de enero de 2025
**iOS**
- Mostrar las noticias del día de El País y ABC en la pantalla de inicio de forma provisional.  
- Empezar a trabajar en las vistas secundarias.

**Android**
- Terminar el set up del proyecto de Android, intentar lograr que la aplicación ejecute sin errores, JVM 17, dependencias de gradle, etc. 

#### 15 de enero de 2025
**iOS**
- Mejorar las pantallas principal y secundarias añadiendo imágenes y fechas de las noticias.  
- Investigar modelos de Kaggle para incorporar en el próximo sprint.

**Android**
- Terminar el setup del proyecto de Android, implementar primeras vistas como están en Figma.

#### 17 de enero de 2025
**iOS**
- Añadir más fuentes RSS de medios de comunicación.  
- Añadir documentación.

**Android**
- Terminar de hacer el setup del proyecto Android con la app funcional con navegación inferior
- Crear las estructuras de datos necesarias para las noticias y los medios de prensa
- Realizar llamadas a los RSS de ABC, El País y RTVE, representar visualmente con RecyclerView el título de la noticia y su contenido en la pagina inicial

### Retrospectiva

**¿Cual era el MVP propuesto al inicio del sprint?**
Implementar las características básicas de la aplicación, incluyendo la obtención de noticias de fuentes RSS, el análisis de los datos en modelos estructurados y la visualización de los artículos de noticias en la pantalla con una interfaz simple y fácil de usar.

**¿Qué MVP podemos mostrar al final del sprint?**
En ambas aplicaciones se puede apreciar el MVP del inicio del sprint. La aplicación de iOS ofrece más detalles de los esperados.

**¿Es mejor o peor de lo esperado?**
Mejor de lo esperado en la aplicación iOS y como se esperaba en la aplicación Android

- **Lo que ha ido bien:** La organización y la manera de enfocar el proyecto, hemos podido definir y encontrarle el propósito a nuestro proyecto, de manera que tenemos una línea de trabajo y una idea clara de cuales son nuestros objetivos, qué tenemos que hacer: sabemos cual es la idea base, y que a partir de esa idea, podemos ir añadiendo más cosas en función de lo que nos parezca mas fácil, útil o apto para nuestro aprendizaje y/o para la aplicación.

- **Lo que ha ido mal:** Los equipos informáticos en las aulas de clase han dado problemas: almacenamiento, JDK, lentitud, y todo lo que ya se conoce, en Android, ha habido problemas ya que se intentaron importar muchas utilidades para la base del proyecto, esto es algo que ha ralentizado un poco el desarrollo inicial con respecto a iOS, pero que era necesario para tener un proyecto bien inicializado y trabajar con comodidad.

- **Lo que cambiaremos para el siguiente sprint:** Dejaremos de asistir a algunas clases para trabajar en casa con nuestros equipos de manera más eficiente, nuestro trabajo es bastante individual, ya que desarrollamos una aplicación en iOS y la otra en Android, podemos aprovechar las horas de clase para discutir más sobre el proyecto en general, sincronizar nuestro trabajo y hacer más scrum.

### App Demo Sprint 1
| Platform | Video |
|----------|------|
| iOS      | [Demo iOS Sprint1](https://github.com/user-attachments/assets/7c62c742-3019-433a-9153-eb2937c2bc1d) |
| Android  | [Demo Android Sprint1](https://github.com/user-attachments/assets/be2afee5-77cc-42d9-9c95-26d178a61327) |

## Sprint 2
### MVP  
Mejorar el diseño y usabilidad de la aplicación Android, saber qué modelo usar de Machine Learning para identificar noticias únicas y crear sus resúmenes neutrales, saber cómo implementarlos en las aplicaciones.

### Dailies  
#### 21 de enero de 2025
**iOS**
- Empezar research de Machine Learning
- Investigar sobre CreateML y CoreML

**Android**
- Comenzar a trabajar en las siguientes tareas en este orden: Diseño agradable, Swipe to refresh, Detalle de la noticia.
- Una vez terminado, empezar a hacer research de Machine Learning

#### 22 de enero de 2025
**iOS**(Obtenidos 900k de datos, con un peso total de 1,1GB)
- Investigar sobre CreateML y CoreML

**Android**
- Research de Machine Learning (Ens han mogut a l'aula 307 i no puc avançar en l'Android)

#### 24 de enero de 2025
**iOS**
- Continuar investigando sobre CreateML y CoreML

**Android**
- Research de Machine Learning

#### 28 de enero de 2025
**iOS**
- Buscar datasets útiles en Kaggle y HuggingFace.

**Android**
- Terminar de hacer un diseño mínimamente agradable, implementar vista swipetorefresh y crear vista de detalle de noticia

#### 29 de enero de 2025
**iOS**
- Investigar dataset shainar/BEAD
- Investigar como compatibilizar un modelo CoreML con TensorFlow Lite para poder usarlo en las dos plataformas
- Investigar como compatibilizar un modelo TensorFlow con CoreML para poder usarlo en las dos plataformas
- 
**Android**
- Investigar dataset shainar/BEAD
- Investigar como compatibilizar un modelo CoreML con TensorFlow Lite para poder usarlo en las dos plataformas
- Investigar como compatibilizar un modelo TensorFlow con CoreML para poder usarlo en las dos plataformas

#### 31 de enero de 2025
**iOS**
- Empezar a crear el modelo con CreateML y entrenarlos con los datos del dataset BEAD, con el algoritmo BERT embedding.
**Android**
- Empezar a crear el modelo con Tensorflow en GoogleCollab y entrenarlos con los datos del dataset BEAD, con el algoritmo BERT embedding.

### Retrospectiva

**¿Cual era el MVP propuesto al inicio del sprint?**
Mejorar el diseño y usabilidad de la aplicación Android, saber qué modelo usar de Machine Learning para identificar noticias únicas y crear sus resúmenes neutrales, saber cómo implementarlos en las aplicaciones.

**¿Qué MVP podemos mostrar al final del sprint?**
Aplicación de Android mejorada. Tenemos datasets útiles para entrenar modelos. Hemos hecho pruebas entrenado con datasets de bias-debias, con algoritmos de BERT Embeding multilenguaje, funciona bastante bien aunque la eficiencia en español no es tan buena como en inglés.
Tenemos otros datasets para catalogar noticias y detectar si se trata de la misma noticia.

**¿Es mejor o peor de lo esperado?**
Lo esperado. Hemos avanzado con el tema de ML pero aun queda bastante ya que es la parte mas compleja de nuestro proyecto y es un mundo bastante nuevo para nosotros.

- **Lo que ha ido bien:** El research en general y el aprendizaje, ahora tenemos mas información de como entrenar modelos.

- **Lo que ha ido mal:** Hemos tenido alguna problema entrenando modelos con muchos datos que se quedo colgado.

- **Lo que cambiaremos para el siguiente sprint:** Mezclar el research con el desarrollo de las apps en si para no dejar de lado la app y hacerlo más entretenido.

- ### App Demo Sprint 2
| Platform | Video |
|----------|------|
| iOS*      | [Demo iOS Sprint1](https://github.com/user-attachments/assets/7c62c742-3019-433a-9153-eb2937c2bc1d) |
| Android  | [Demo Android Sprint2](https://github.com/user-attachments/assets/d6c482dc-9e01-41ce-943c-cb07adfe0d1d) |

*Sense canvis significatius


## Sprint 3
### MVP  
Mejorar el diseño y usabilidad de la aplicación Android y iOS, crear e implementar modelo de machine learning para detectar la misma noticia o para los sesgos, aunque aún no de buenos resultados. Opcional: Empezar a crear el backend para el procesamiento de los datos RSS.

### Dailies  
#### 04 de febrero de 2025

**iOS**
- Crear en la aplicación el botón de filtrar para que se abra un menu en el que poder aplicar varios filtros: por categoría, fecha y medio
  
**Android**
- Crear en la aplicación el botón de filtrar para que se abra un menu en el que poder aplicar varios filtros: por categoría, fecha y medio

#### 05 de febrero de 2025

**iOS**
- Acabar los filtros y comprovar su correcto funcionamiento.
- Opcional (si da tiempo): Empezar a crear el modelo de ML para detectar la misma noticia de distintos medios.
  
**Android**
- Acabar los filtros y comprovar su correcto funcionamiento.
- Opcional (si da tiempo): Empezar a crear el modelo de ML para detectar la misma noticia de distintos medios.

#### 11 de febrero 2025

**iOS**
- Acabar filtro de categoría y fecha.
- Investigar soluciones para agrupado de noticias con SBERT
**Android**
- Arreglar filtro de medio, hacer filtro de categoría y fecha
- Investigar soluciones para agrupado de noticias con SBERT

#### 12 de febrero 2025

**iOS**
- Investigar soluciones para agrupado de noticias con SBERT

**Android**
- Investigar [GDELT PROJECT ](https://www.gdeltproject.org/#:~:text=What%20would%20it,over%20human%20society.), como usarlo para nuestra necesidad específica

#### 14 de febrero 2025

**iOS**
- Cambio de modelo a SBERT multilenguaje para ver si mejoran los resultados de agrupado de noticias

**Android**
- Obtener datos etiquetados de GDELT Project desde Google BigQuery
  
### Retrospectiva

**¿Cual era el MVP propuesto al inicio del sprint?**
Mejorar el diseño y usabilidad de la aplicación Android y iOS, crear e implementar modelo de machine learning para detectar la misma noticia o para los sesgos, aunque aún no de buenos resultados. Opcional: Empezar a crear el backend para el procesamiento de los datos RSS.

**¿Qué MVP podemos mostrar al final del sprint?**
Hemos añadido funcionalidad en ambas aplicaciones con opciones de filtrado de noticias. 
En iOS, hemos convertido [este modelo](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2?utm_source=chatgpt.com) a un .mlmodel y lo hemos implementado en la app para agrupar las mismas noticias de los diferentes medios, con un mal resultado, eso si (como predeciamos en el MVP).
La parte opcional se ha quedado pendiente, ya que nos ha ocupado la totalidad del tiempo la parte principal.

**¿Es mejor o peor de lo esperado?**
Lo esperado. La parte de mejorar los resultados del modelo con las agrupaciones de noticias ha sido lo complicado y no lo hemos conseguido mejorar sustancialemnte. Pero la parte de enconctrar el modelo, adaptarlo e implementarlo a nuestra app esta hecho así como la funcionalidad del filtrado de noticias.

- **Lo que ha ido bien:** Encontar, adaptar e implementar el modelo en la app, asi como la parte del filtraje de noticas en ambas apps.

- **Lo que ha ido mal:** Los resultados del modelo no son buenos.

- **Lo que cambiaremos para el siguiente sprint:** Aplicaremos los aprendizajes de este sprint para optimizar el siguiente.

### App Demo Sprint 3
| Platform | Video |
|----------|------|
| iOS      | [Demo iOS Sprint3](https://github.com/user-attachments/assets/7d4abdd8-171f-47ed-aecc-610a7381214f) |
| Android  | [Demo Android Sprint3](https://github.com/user-attachments/assets/0a2b04d9-6d98-43a0-95a0-364756bc2138)


## Sprint 4
### MVP  
Crear e implementar un modelo funcional que detecte las noticias pertenecientes al mismo evento, cuando el usuario haga click en una noticia, saldrá, abajo de todo, todas las noticias que pertenecen al mismo evento.

#### 18 de febrero 2025

**iOS**
- Empezar a modificar el modelo existente o crear otro para que funcione correctamente
  
**Android**
- Obtener datos etiquetados de GDELT Project desde Google BigQuery [(Obtenidos 900k de datos, con un peso total de 1,1GB)](https://drive.google.com/drive/folders/15Ec2hh_kMP2DpAZZX94jcYUbZU3LYc-J)

#### 19 de febrero 2025

**iOS**
- Empezar a crear otro modelo desde 0 para que funcione correctamente

**Android**
- Scrapear datos de URLS de los datos etiquetados con Beautiful Soup, guardarlos en un pandas dataframe, (Scrapeado el contenido de 100k de páginas web, con un éxito aproximado de 80%)

#### 21 de febrero 2025

**iOS**
- Continuar creando el modelo desde 0 para que funcione correctamente, con pandas, nltk y sklearn

**Android**
- Invesitgar si podemos reducirnos pre-procesado de datos con una sentencia SELECT en Google Big Query más sofisticada (que las paginas web no tengan contenidos con Error 404, poco texto, etc.)
- Entrenar modelo avanzado con transformers y BERT, para clasificar noticias por categoría


### 25 de febrero 2025

**iOS**
- Continuar creando el modelo desde 0 para que funcione correctamente, con pandas, nltk y sklearn. Con el dataset modificado de La Razón-Público


**Android**
- Entrenar modelo avanzado con transformers y BERT, para clasificar noticias por categoría
- Entrenar modelo avanzado con sentence-transformers y SBERT, para clasificar noticias por evento
<!-- 
- Generar títulos de grupos con text summarizer, o utilizar el embedding más cercano al centro del cluster (más eficiente)
-->
### 26 de febrero 2025

**iOS**
- Calcular la similitud del coseno de los embeddings SBERT de cada noticia para poder detectar la misma noticia de diferentes medios

- **Android**
- Mejorar consulta de Google BiqQuery para solo recibir noticias españolas
- Mejorar extracción de datos con BeautifulSoup, recibir H1, (titulo de la noticia) y aplicar filtros más agresivos (si contiene mucho contenido basura, descartar los resultados)

### 28 de febrero 2025

**iOS**
- Calcular la similitud del coseno de los embeddings SBERT de cada noticia para poder detectar la misma noticia de diferentes medios

- **Android**
- Scrapear datos de sitios web españoles y conseguir el archivo gdelt_final_extraction.csv para luego procesar sus datos

### Retrospectiva

**¿Cual era el MVP propuesto al inicio del sprint?**
Crear e implementar un modelo funcional que detecte las noticias pertenecientes al mismo evento, cuando el usuario haga click en una noticia, saldrá, abajo de todo, todas las noticias que pertenecen al mismo evento.

**¿Qué MVP podemos mostrar al final del sprint?**
Tenemos dos modelos diferetes para detectar categorías de noticias y detectar mismas noticias en proceso. No los hemos podido acabar, pero hemos avanzado mucho en esto, que claramente es lo mas nucleo de la app.

**¿Es mejor o peor de lo esperado?**
Algo peor de lo esperado, pero fue muy optimista de nuestra parte pensar que podriamos acabar esto. Es dificil estimar el progreso adecuado y cuanto tiempo nos puede tomar hacer algo que tenemos que investigar por nuestra cuenta, ir a prueba y error y aprendiendo con fallos, que nos quitan tiempo y hacen parecer que no hemos avanzado al no cumplir "x" objetivo, del cual muchas veces no conocemos la totalidad de los pasos o obstaculos que nos vamos a encontrar antes de llegar a este.

> **UPDATE**
> 
> Entre el final del sprint 4 y el inicio del 5 hemos conseguido una agrupación correcta de noticias del dataframe LaRazon-Público usando el modelo SBERT para procesar el texto y generar los embeddings y DBSCAN para hacer el clustering agrupando las noticias que se encuentran cerca.
> Basicamente lo hemos logrado a base de prueba y error y ajustando los parametros `eps`, `min_samples` y `n_neighbors`. 
> Finalmente el resultado es más cercano a lo esperado de lo descrito anteriormente.

- **Lo que ha ido bien:** Entrenar modelos.

- **Lo que ha ido mal:** Hemos tenido algun problema con el Colab y los tiempos de ejecución para entenar los modelos (muy largos).

- **Lo que cambiaremos para el siguiente sprint:** Aplicaremos los aprendizajes de este sprint para optimizar el siguiente, en el que esperamos acabar el modelo e implementarlo en nuestra app.

## Sprint 5
### MVP  
La aplicación carga noticias y en la parte inferior aparecen las relacionadas, con mayor o menor precisión. Las noticias relacionadas se obtienen del backend, que tiene el modelo o alogoritmo de clustering creado.

### 11 de marzo de 2025

**iOS**
- Crear Firebase para base de datos y backend.
- Empezar a procesar las noticias en el backend con el algoritmo de embedding y clustering creado en el sprint anterior.

**Android**
- Mejorar diseño de UI de la aplicación de Android, añadir noticias relacionadas en la parte inferior.

### 12 de marzo de 2025

**Android**
- Mejorar exposición del contenido de la noticia
- Mejorar exposición del contenido del noticia.
- Investigar GDELT Event Dataset (este dataset tiene identificadores de eventos con URLS, que nos vendrá mejor para nuestro propósito)

# DEMO PRESENTACIÓ 14 M 2025

| Platform | Video |
|----------|------|
| iOS      | [Demo iOS](https://github.com/user-attachments/assets/3798bfa8-1e9d-4704-8b88-6318d8bbce5c) |
| Android  | [Demo Android](https://github.com/user-attachments/assets/83d3d8ec-8fb3-4c3f-9513-9de5e0970e56)

# 18 de marzo de 2025
**iOS**
- Implementar Firebase Functions sin errores
- UI

**Android**
- Vincular Firebase a proyecto de Android
- Settings Screen, Sort Option
- 

# 21 de marzo de 2025
**iOS**
- Obtener y almacenar noticias de RSS en Firestore con Firebase Cloud Scheduler
- Agrupar noticias en el servidor periódicamente con Cloud Scheduler

**Android**
- Diseño de clases para Settings Screen y Sort Screen
- Settings screen y Filter Screen

### Retrospectiva

**¿Cual era el MVP propuesto al inicio del sprint?**
La aplicación carga noticias y en la parte inferior aparecen las relacionadas, con mayor o menor precisión. Las noticias relacionadas se obtienen del backend, que tiene el modelo o alogoritmo de clustering creado.

**¿Qué MVP podemos mostrar al final del sprint?**
Hemos implementado el algoritmo de agrupación de noticias en el backend con Firebase Functions y funciona correctamente.

**¿Es mejor o peor de lo esperado?**
Lo esperado. La aplicación hace lo que pide el MVP, pero con mejoras pendientes.

- **Lo que ha ido bien:** Poder mover el algoritmo al backend y su funcionamiento.

- **Lo que ha ido mal:** Muchos problemas para implementar la funcion de firebase functions.

- **Lo que cambiaremos para el siguiente sprint:** Seguiremos dandole caña al servidor, moviendo el fetch de RRS del cliente al servidor y ejecutandose periodicamente y guardando las noticias en la bbdd.

### App Demo Sprint 5
| Platform | Video |
|----------|------|
| iOS      | [Demo iOS Sprint5](https://github.com/user-attachments/assets/7fc1dbef-5ac8-478f-a6da-91697a4b7f11) |
| Android  | Sin cambios significativos

## Sprint 6
### MVP
Tememos la noticia neutral implementada en la app para cada grupo de noticias, ya sea con una api u otro modelo.

### 25 de marzo de 2025
**iOS**
- Investigar cómo hacer la noticia neutral, mirar diferentes posibilidades

**Android**
- Investigar como hacer la noticia neutral, mirar diferentes posibilidades
- Implementar el backend para los grupos de noticias

### 26 de marzo de 2025
**iOS**
- Investigar cómo hacer la noticia neutral, mirar diferentes posibilidades

**Android**
- Implementar el backend para los grupos de noticias
- Investigar como hacer la noticia neutral, mirar diferentes posibilidades

### 28 de marzo de 2025
**iOS**
- Seguir con la neutralización de texto con Google T5

**Android**
- Implementar el backend para los grupos de noticias

### 31 de marzo de 2025
**iOS**
- Seguir con la neutralización de texto con Google T5

**Android**
- Seguir con la neutralización de texto con Google T5

### 1 de abril de 2025
**iOS**
- Intentar acabar la neutralización de texto con Google T5

**Android**
- Scrapear noticias neutras (0-0.25 Tone)
- Fine tune de T5

### 3 de abril de 2025
**iOS**
- Subir el modelo flan-t5-large a VertexAI para poder usarlo en el backend de manera eficiente

**Android**
- Fine tune de T5

### 4 de abril de 2025
**iOS**
- Subir el algoritmo de neutralización en una scheduled firebase function

**Android**
- Fine tune de T5

### Retrospectiva Sprint 6

**¿Cual era el MVP propuesto al inicio del sprint?**
Tememos la noticia neutral implementada en la app para cada grupo de noticias, ya sea con una api u otro modelo.

**¿Qué MVP podemos mostrar al final del sprint?**
Tenemos la noticia implementada en la app para cada grupo de noticias.

**¿Es mejor o peor de lo esperado?**
Peor, ya que no hemos logrado una neutralización fiable de las noticias.

- **Lo que ha ido bien:** 
Hemos movido el fetch de RSS del cliente al servidor, las noticias se añaden periodicamente y se guardan en la base de datos.

- **Lo que ha ido mal:**
No hemos logrado neutralizar las noticias con T5, ni tampoco ha funcionado el Fine Tuning de T5

- **Lo que cambiaremos para el siguiente sprint:** 
Intentaremos neutralizar las noticias con text-bison, otro modelo de Google, y si este tampoco funciona, utilizaremos alguna API de generación de texto.

### App Demo Sprint 6
| Platform | Video |
|----------|------|
| iOS      | Sin cambios significativos
| Android  | Sin cambios significativos

## Sprint 7
### MVP
Tememos la noticia neutral implementada en la app para cada grupo de noticias, ya sea con una api u otro modelo.
El diseño de la aplicación se ha ajustado: las noticias mostradas no tienen ruido HTML, se puede filtrar sin problemas, tiene una pantalla de Settings funcional en Android, y en iOS (opcional).
Existe un indicador de neutralidad en el detalle de cada noticia individual.

### 7 de abril de 2025
**iOS**
- Intentar mejorar neutralización con T5
  
**Android**
-

### 8 de abril de 2025
**iOS**
- Empezar Unit Testing en iOS

**Android**
- Empezar Unit Testing en Android

### 9 de abril de 2025
**iOS**
- Mejorar la organización del backend de las funciones de Firebase
-
**Android**
-

### 10 de abril de 2025
**iOS**
- Intentar neutralizar noticias con text-bison
- Añadir el backend de Firebase Functions al repositorio de Github

**Android**
- Intentar neutralizar noticias con text-bison

## 22 de abril de 2025
**iOS**
- Arreglar el muestreo de noiticias de medios nuevos
- Redactar correos a los medios para pedir permiso para usar su logo y demás

**Android**
- Ajustar backend para hacerlo más apto para la Playstore y App Store
- Arreglar los titulos y descripciones para usar UTF-8

### 23 de abril de 2025
**iOS**
- Arreglos en UI
- Buscar correos de medios para pedir permisos de imagen (logo, imagenes de noticias)

**Android**
- Ajustar backend para que solo scrapee noticias una vez
- Arreglar errores para hacer deploy de una versión avanzada de las noticias

### 24 de abril de 2025
**iOS**
- Contactar con los medios
- Generar mock_neutral_news y mock_news
- Pequeños cambios front

**Android**
- Arreglar backend para que funcione correctamente

### 25 de abril de 2025
**iOS**
- Añadir search bar en home view

**Android**
- Imitar vistas de la versión iOS
- Arreglar problema del backend
- Optimizar scraping


### Retrospectiva Sprint 7

**¿Cual era el MVP propuesto al inicio del sprint?**
Tememos la noticia neutral implementada en la app para cada grupo de noticias, ya sea con una api u otro modelo.
El diseño de la aplicación se ha ajustado: las noticias mostradas no tienen ruido HTML, se puede filtrar sin problemas, tiene una pantalla de Settings funcional en Android, y en iOS (opcional).
Existe un indicador de neutralidad en el detalle de cada noticia individual.

**¿Qué MVP podemos mostrar al final del sprint?**
Tenemos la noticia neutral implementada en la app para cada frupo de noticias, con la api de chat gpt. Tenemos diseño de la app ajustado correctamente. Tenemos indicador de neutralidad.

**¿Es mejor o peor de lo esperado?**
Mejor, ya que hemos conseguido todo lo propuesto y algun detalle más.

- **Lo que ha ido bien:** 
La neutralización de noticias con la api de chat gpt

- **Lo que ha ido mal:**
Nos hemos dado cuenta que habra mas problemas de los que pensabamos con el tema de copyright y derechos de imagen de los medios. Nos hemos puesto en contacto con los medios para solicitar permisos de uso de su logo e imagenes, algunos nos lo han dado, otros no, y otros no han respondido.

- **Lo que cambiaremos para el siguiente sprint:** 
Buscaremos la mejor solución para el tema de los derechos las imagenes de los medios.

### App Demo Sprint 7
| Platform | Video |
|----------|------|
| iOS      | [Demo iOS Sprint7](https://github.com/user-attachments/assets/95c5672d-a4ec-4ce8-965d-e7934d4c7d0d) |
| Android  | [Demo Android Sprint7](https://github.com/user-attachments/assets/1e028e8d-1ec3-47c9-92f5-ba9d3df051ed) |


## Sprint 8
### MVP
Ajustes en el backend:
-Backend arreglado, fetch de noticias cada 4 horas.
-Muestreo de noticias relacionadas con contenido reducido (texto, logos) para evitar problemas en la Playstore y App Store.

Funcionamiento correcto de funcionalidades básicas de UX y UI:  
- Search Bar en la aplicación  
- Filtro por categorías  
- Ordenación de noticias por fecha, popularidad  
- Navegación dinámica por día entre los últimos 7 días  
- Mejoras en el manejo de estado de carga de las noticias y la interfaz de usuario relacionada con este  
- Separar por párrafos, formatear texto (markdown o inventar nuestro propio formato)
- Poner logos de medios
- (Opcional) Botón de compartir noticias funcional  
- (Opcional) Pantalla de Home con Tendencias, fetcheadas desde la API  
- (Opcional) Pantalla de Búsqueda  
- (Opcional) Pantalla de Settings  
- (Opcional) Buscar solución al problema de las imagenes (derechos de autor)  

### 29 de abril de 2025
**iOS**
- Mejoras en el manejo de estado de carga de las noticias y la interfaz de usuario relacionada con este
- Separar por párrafos, formatear texto (markdown o inventar nuestro propio formato)
- Texto de noticias justificado (en bloque)

**Android**
- Arreglar fetch de noticias de backend
- Search Bar
- Arreglar filtro por cateogrías
- Ordenación de noticias por fecha y popularidad

### 5 de maig de 2025
**iOS**
- Opción de ordenar noticias por relevancia
- Mejorar sistema de búsqueda para mostrar primero las coincidencias en los titulares, luego descripcion, luego scraped_description

**Android**
- Guardar embedding en la noticia para evitar regenerarlo

### 8 de maig de 2025
**iOS**
- Opción de ordenar noticias por relevancia
- Poner logos de medios

**Android**
- Guardar embedding en la noticia para evitar regenerarlo (Arreglar errores)
- Ordenación de noticias por fecha, popularidad 
- Navegación dinámica por día entre los últimos 7 días  
- Mejoras en el manejo de estado de carga de las noticias y la interfaz de usuario relacionada con este  

### 9 de maig de 2025
**iOS**
- Opción de ordenar noticias por popularidad
- Popover de info en el neutral score de las noticias

**Android**
- Mejoras en el manejo de estado de carga de las noticias y la interfaz de usuario relacionada con este
- Ordenación de noticias por fecha, popularidad 
- Navegación dinámica por día entre los últimos 7 días

### 12 de maig de 2025
**iOS**
- SVG's de los medios
- Mirar fallos backend

**Android**
- Arreglar cosas del backend
- Implementar sorting
<!--
Funcionamiento correcto de funcionalidades básicas de UX y UI:  
- Ordenación de noticias por fecha, popularidad 
- Navegación dinámica por día entre los últimos 7 días  
- Mejoras en el manejo de estado de carga de las noticias y la interfaz de usuario relacionada con este  
- (Opcional) Botón de compartir noticias funcional
- (Opcional) Pantalla de Home con Tendencias, fetcheadas desde la API  
- (Opcional) Pantalla de Búsqueda  
- (Opcional) Pantalla de Settings  
- (Opcional) Buscar solución al problema de las imagenes (derechos de autor)  
-->
