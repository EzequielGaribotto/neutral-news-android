<h1 align="center">Neutral News – Android</h1>

> ⚠️ Este repositorio corresponde únicamente a la app Android del proyecto Neutral News.  
> El sistema completo está compuesto por tres partes:
>
> - 📡 [**Backend**](https://github.com/martiespinosa/neutral-news-backend) – Extracción, procesamiento, agrupación y generación de noticias neutralizadas. (Ezequiel & Martí)  
> - 📱 **App Android** (este repositorio) – Aplicación móvil nativa para Android. (Ezequiel Garibotto)  
> - 🍎 **[App iOS](https://github.com/martiespinosa/neutral-news)** – Aplicación móvil nativa para iOS. (Martí Espinosa)

Neutral News forma parte del proyecto final del CFGS en Desarrollo de Aplicaciones Multiplataforma.  
Se ha desarrollado una app Android completamente funcional, pensada para facilitar el consumo de noticias agrupadas y neutralizadas gracias al uso de Inteligencia Artificial.

---

## 📑 Índice

- [📰 ¿Qué es Neutral News?](#-qué-es-neutral-news)  
- [📱 ¿Cómo funciona la app Android?](#-cómo-funciona-la-app-android)  
- [🛠️ Tecnologías utilizadas](#%EF%B8%8F-tecnologías-utilizadas)  
- [🧠 Inteligencia Artificial integrada](#-inteligencia-artificial-integrada)  
- [📷 Demo](#-demo)  
- [📍 Formación](#-formación)  

---

## 📰 ¿Qué es Neutral News?

Neutral News es una plataforma que agrupa noticias similares de distintos medios y genera una versión neutral del acontecimiento, libre de sesgos ideológicos o editoriales.  
La app Android permite acceder fácilmente a estas noticias, filtradas por categoría o evento, con una experiencia de usuario intuitiva y moderna.

---

## 📱 ¿Cómo funciona la app Android?

- Consulta en tiempo real los datos desde Cloud Firestore, donde se almacena el contenido generado por el backend.  
- Muestra noticias agrupadas y neutralizadas mediante una interfaz moderna basada en Material Design.  
- Permite visualizar los artículos agrupados, leer el resumen neutral y acceder a las fuentes originales.  
- Arquitectura basada en MVVM, con uso intensivo de LiveData, ViewModel y Dependency Injection (Hilt/Dagger) para asegurar un código escalable y mantenible.

---

## 🛠️ Tecnologías utilizadas

| Categoría                 | Tecnologías / Herramientas                                              |
|---------------------------|------------------------------------------------------------------------|
| Lenguaje                  | Kotlin, Kotlin DSL                                                     |
| Framework                 | Android SDK, Android Jetpack (LiveData, ViewModel, Navigation, RecyclerView) |
| Diseño UI/UX              | XML Layouts, Material Design, Data Binding                             |
| Arquitectura              | MVVM (Model-View-ViewModel), Repository Pattern                        |
| Inyección de dependencias | Hilt, Dagger                                                          |
| Base de datos local       | Room (DAO)                                                           |
| Base de datos remota      | Firebase Cloud Firestore                                             |
| Gestión de versiones      | Git, GitHub                                                         |
| Entorno de desarrollo     | Android Studio, Gradle                                              |

---

## 🧠 Inteligencia Artificial integrada

Aunque el modelo de IA se ejecuta en el backend, la app Android se conecta con una base de datos que contiene contenido generado mediante:

- Embeddings semánticos con SBERT  
- Agrupación de noticias por KMeans y DBSCAN  
- Resumen neutral generado con OpenAI GPT  

Esto permite que la app muestre una versión neutral de cada grupo de noticias y permita al usuario leer todas las fuentes originales.

---

## 📷 Demo

[Ver demo (20/05/2025](https://github.com/user-attachments/assets/cdc97bb6-300f-4396-a17c-a06e766f63a8)

---

## 📍 Formación

Esta aplicación ha sido desarrollada como parte del proyecto final del ciclo formativo de Desarrollo de Aplicaciones Multiplataforma (DAM) en el Institut Tecnològic de Barcelona, bajo el rol de Junior Android Developer en Eulix.

A través de este proyecto se ha puesto en práctica el conocimiento en:

- Desarrollo nativo Android con Kotlin  
- Integración de Firebase y servicios cloud  
- Aplicación de patrones de arquitectura moderna (MVVM)  
- Diseño UI/UX profesional  
- Trabajo en equipo y planificación ágil  
- Implementación y consumo de contenido generado por IA
