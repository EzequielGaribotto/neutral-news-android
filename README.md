# Neutral News

![App Icon](https://github.com/user-attachments/assets/f6b051c2-4e50-4b4f-b6c1-9f26233d1eb1)

- [iOS](https://github.com/ITEC-BCN/projecte-2-dam-24-25-neutral-news/blob/main/iOS/)
- [Android](https://github.com/ITEC-BCN/projecte-2-dam-24-25-neutral-news/blob/main/Android/)

## Indice
- [Abstract](#abstract)
- [Arquitectura](#arquitectura)
- [Diseño BBDD](#diseño-bbdd)
- [Reflexiones y conclusiones](#reflexiones-y-conclusiones)
  - [Valoración de las características realizadas frente a las planteadas](#valoración-de-las-características-realizadas-frente-a-las-planteadas)
  - [Problemas encontrados](#problemas-encontrados)
  - [Conclusiones](#conclusiones)
- [App Demo](#app-demo)  
- [Recursos adicionales](#recursos-adicionales)  

## Abstract
Recopilamos noticias de diversos medios de comunicación españoles con el objetivo es mostrar cómo varía el enfoque de cada medio sobre una misma noticia y generar un resumen neutral que ayude al usuario a tener una visión más objetiva de la información.

## Arquitectura
Ambas apps siguen una arquitectura MVVM, separando la interfaz de usuario, la lógica de presentación y el acceso a datos. 
Se comunica con el backend en Google Cloud, que se encarga del procesamiento de noticias y devuelve a la app los resultados agrupados y neutralizados.

## Diseño BBDD
Usamos Firebase Firestore como base de datos para almacenar las noticias obtenidas mediante los RSS de los medios. También almacenamos las noticias neutrales con todos los valores necessarios.

## Reflexiones y conclusiones

### Valoración de las características realizadas frente a las planteadas
Hemos logrado implementar todas las características que nos planteamos al inicio del proyecto. El núcleo de la app está completamente desarrollado, aunque aún podrían añadirse funcionalidades adicionales en el futuro.

### Problemas encontrados
A lo largo de todo el desarrollo nos hemos encontrado con bastantes problemas, algunos de los principales son los siguientes:
- **Agrupar las mismas noticias**: fue más difícil de lo que parecía. Lo logramos hacer con embeddings y clusters de la librería DBSCAN.
- **Neutralizar noticias**: inicialmente planeamos entrenar nuestro propio modelo, pero por falta de tiempo decidimos usar una API. Probamos la de Gemini y la de OpenAI, y nos quedamos con OpenAI porque nos pareció más neutral y ofrecía una mejor redacción.
- **Problemas de copyright con los medios**: nos dimos cuenta que podriamos tener problemas para sacarla a la Play Store y App Sotre por el uso de imagenes con derechos, hemos contactado con los medios para solicitar permisos y algunos nos lo han dado y otros no.

### Conclusiones
Creemos que hemos hecho un muy buen trabajo, en el que hemos aprendido a desarrollar una aplicación completa desde cero. Este proyecto nos ha permitido aplicar conocimientos aprendidos en clase, prácticas y autoaprendizaje, tanto de desarrollo móvil como de integración con servicios en la nube, además de introducirnos en el uso de algoritmos de ML.

### App Demo
| Platform | Video |
|----------|------|
| iOS     | [Demo iOS](https://github.com/user-attachments/assets/c6110d9a-a5bf-44e7-9afd-e38de9f4ff34) |
| Android  | [Demo Android](https://github.com/user-attachments/assets/cdc97bb6-300f-4396-a17c-a06e766f63a8) |

## Recursos adicionales
- [InitialDoc.md](https://github.com/ITEC-BCN/projecte-2-dam-24-25-neutral-news/blob/main/InitialDoc.md)
- [agile.md](https://github.com/ITEC-BCN/projecte-2-dam-24-25-neutral-news/blob/main/agile.md)
- [videos](https://github.com/ITEC-BCN/projecte-2-dam-24-25-neutral-news/blob/main/videos/)
- [backend](https://github.com/ITEC-BCN/projecte-2-dam-24-25-neutral-news/blob/main/functions/)
