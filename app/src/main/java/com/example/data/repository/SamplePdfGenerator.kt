package com.example.data.repository

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object SamplePdfGenerator {
    fun generateSamplePdf(context: Context, fileName: String = "sample_elsevier_paper.pdf"): File {
        val file = File(context.filesDir, fileName)
        if (file.exists()) return file // Avoid re-generation

        val pdfDocument = PdfDocument()

        val sections = listOf(
            "Abstract" to "In recent years, Artificial Intelligence (AI) and Machine Learning (ML) have revolutionized scientific publishing and literature retrieval. This sample paper demonstrates the flawless operation of the native Elsevier PDF Reader in our custom Android application. By leveraging memory-efficient rendering pipelines, users can read, zoom, and bookmark critical scientific articles seamlessly.",
            "1. Introduction" to "Scientific literature is expanding at an unprecedented rate. Standard tools for document reading often fall short on mobile devices due to slow rendering speeds and excessive memory consumption. Here, we present an elegant, Jetpack Compose-based Native PDF Viewer that integrates directly with Elsevier's Scopus APIs to fetch and display academic articles in high fidelity without external heavy rendering engines.",
            "2. Methodology" to "Our architecture employs a clean MVVM structure combined with a Room database for offline persistence. The native PdfRenderer API of Android is used to convert vector PDF pages into high-resolution screen-optimized bitmaps on a background Dispatcher. This prevents Main Thread blocking and guarantees 60fps scrolling during read sessions.",
            "3. Results & Discussion" to "Initial benchmarks indicate a 75% reduction in initial page-load latency compared to standard web-based PDF viewers. Memory consumption remains linear with respect to the screen size rather than the document length. Users report high satisfaction with the integrated hybrid reading modes (Native Reader and ScienceDirect Portal).",
            "4. Conclusion" to "The custom Elsevier Reader applet provides a reliable, robust, and delightful interface for researchers globally. Future enhancements will include automated abstract summaries, collaborative highlight sharing, and dynamic reference tracing using Elsevier's citations graph API."
        )

        // Render 3 pages
        for (pageNumber in 1..3) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4 Size: 595 x 842 points
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            
            // Draw background card grid
            paint.color = Color.parseColor("#FAFAFA")
            canvas.drawRect(0f, 0f, 595f, 842f, paint)

            // Draw Elsevier header bar
            paint.color = Color.parseColor("#FF5C00") // Elsevier Orange
            canvas.drawRect(40f, 40f, 555f, 50f, paint)

            paint.color = Color.parseColor("#1C3F60") // Dark Slate Blue
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("ELSEVIER RESEARCH READER (SAMPLE DEMO)", 40f, 70f, paint)

            paint.color = Color.GRAY
            paint.isFakeBoldText = false
            paint.textSize = 8f
            canvas.drawText("Open Access Demo Paper • Published: July 2026", 40f, 85f, paint)

            // Draw Title on page 1
            if (pageNumber == 1) {
                paint.color = Color.parseColor("#1A2E40")
                paint.textSize = 16f
                paint.isFakeBoldText = true
                canvas.drawText("Optimizing Scientific Literature Access On Mobile Devices", 40f, 130f, paint)
                canvas.drawText("Using Custom Native Jetpack Compose Rendering Engine", 40f, 150f, paint)

                paint.color = Color.parseColor("#333333")
                paint.textSize = 10f
                paint.isFakeBoldText = true
                canvas.drawText("Authors: Dr. Rachmaul, Prof. DeepMind AI Studio", 40f, 185f, paint)
                
                paint.textSize = 9f
                paint.isFakeBoldText = false
                paint.color = Color.GRAY
                canvas.drawText("Affiliation: Department of Advanced Systems and AI Coding, Global AI Hub", 40f, 200f, paint)
                
                // Draw Section
                drawSection(canvas, sections[0].first, sections[0].second, 40f, 230f)
                drawSection(canvas, sections[1].first, sections[1].second, 40f, 410f)
            } else if (pageNumber == 2) {
                drawSection(canvas, sections[2].first, sections[2].second, 40f, 110f)
                drawSection(canvas, sections[3].first, sections[3].second, 40f, 320f)

                // Draw a scientific chart graphic programmatically on Page 2
                drawDemoChart(canvas, 40f, 530f)
            } else {
                drawSection(canvas, sections[4].first, sections[4].second, 40f, 110f)

                // References
                paint.color = Color.parseColor("#1A2E40")
                paint.textSize = 12f
                paint.isFakeBoldText = true
                canvas.drawText("References", 40f, 350f, paint)

                paint.color = Color.parseColor("#444444")
                paint.textSize = 9f
                paint.isFakeBoldText = false
                val refs = listOf(
                    "[1] Rachmaul, R. (2026). Jetpack Compose Native PDF Rendering Pipelines. Journal of Mobile Engineering, 12(3), 145-159.",
                    "[2] DeepMind, G. (2025). Advanced Agentic AI for Production Applications. Science & Tech, 404, e9912.",
                    "[3] Elsevier Developer Portal. (2024). Scopus API and Article Retrieval Guidelines. Technical Report."
                )
                var yOffset = 385f
                for (ref in refs) {
                    canvas.drawText(ref, 40f, yOffset, paint)
                    yOffset += 20f
                }
            }

            // Page Number Footer
            paint.color = Color.GRAY
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText("Halaman $pageNumber dari 3", 260f, 810f, paint)

            pdfDocument.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    private fun drawSection(canvas: android.graphics.Canvas, title: String, text: String, x: Float, y: Float) {
        val titlePaint = Paint().apply {
            color = Color.parseColor("#FF5C00") // Orange section title
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#222222")
            textSize = 9.5f
            isAntiAlias = true
        }

        canvas.drawText(title, x, y, titlePaint)

        // Simple text wrap
        val words = text.split(" ")
        var currentLine = ""
        var currentY = y + 18f
        val maxWidth = 515f // 595 - 40 - 40

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = textPaint.measureText(testLine)
            if (width < maxWidth) {
                currentLine = testLine
            } else {
                canvas.drawText(currentLine, x, currentY, textPaint)
                currentY += 14f
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, textPaint)
        }
    }

    private fun drawDemoChart(canvas: android.graphics.Canvas, x: Float, y: Float) {
        val paint = Paint().apply { isAntiAlias = true }
        // Chart container
        paint.color = Color.parseColor("#F0F3F6")
        canvas.drawRect(x, y, x + 515f, y + 180f, paint)

        // Axis lines
        paint.color = Color.BLACK
        paint.strokeWidth = 1.2f
        canvas.drawLine(x + 40f, y + 15f, x + 40f, y + 150f, paint) // Y-axis
        canvas.drawLine(x + 40f, y + 150f, x + 490f, y + 150f, paint) // X-axis

        // Grid lines
        paint.color = Color.parseColor("#D0D5DD")
        paint.strokeWidth = 0.5f
        for (i in 1..4) {
            val gy = y + 150f - (i * 30f)
            canvas.drawLine(x + 40f, gy, x + 490f, gy, paint)
        }

        // Plot data line
        paint.color = Color.parseColor("#1C3F60")
        paint.strokeWidth = 2.5f
        val points = listOf(
            Pair(x + 50f, y + 130f),
            Pair(x + 120f, y + 110f),
            Pair(x + 200f, y + 70f),
            Pair(x + 280f, y + 85f),
            Pair(x + 360f, y + 40f),
            Pair(x + 440f, y + 25f)
        )
        for (idx in 0 until points.size - 1) {
            canvas.drawLine(points[idx].first, points[idx].second, points[idx + 1].first, points[idx + 1].second, paint)
        }

        // Data points circles
        paint.color = Color.parseColor("#FF5C00")
        for (point in points) {
            canvas.drawCircle(point.first, point.second, 3.5f, paint)
        }

        // Chart Title
        paint.color = Color.BLACK
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText("Gambar 1. Performa Rendering Bitmaps vs PDF Viewer Web (MiliDetik)", x + 110f, y + 170f, paint)
    }
}
