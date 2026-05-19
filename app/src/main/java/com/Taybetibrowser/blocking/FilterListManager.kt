package com.Taybetibrowser.blocking

import android.content.Context
import java.io.File
import java.io.InputStream
import java.net.URL

class FilterListManager(private val context: Context) {

    private val filterDir = File(context.filesDir, "filter_lists").apply {
        if (!exists()) mkdirs()
    }

    private val filterSources = listOf(
        FilterSource(
            name = "EasyList",
            url = "https://easylist.to/easylist/easylist.txt",
            file = File(filterDir, "easylist.txt")
        ),
        FilterSource(
            name = "EasyPrivacy",
            url = "https://easylist.to/easylist/easyprivacy.txt",
            file = File(filterDir, "easyprivacy.txt")
        ),
        FilterSource(
            name = "Malware Domain List",
            url = "https://www.malwaredomainlist.com/hostslist/hosts.txt",
            file = File(filterDir, "malware.txt")
        )
    )

    fun updateFilterLists(onComplete: (Boolean) -> Unit) {
        Thread {
            var allSuccess = true
            for (source in filterSources) {
                try {
                    val inputStream = URL(source.url).openStream()
                    inputStream.use { input ->
                        source.file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    allSuccess = false
                }
            }
            onComplete(allSuccess)
        }.start()
    }

    fun getFilterListContent(name: String): String? {
        val source = filterSources.find { it.name == name } ?: return null
        return if (source.file.exists()) {
            source.file.readText()
        } else {
            null
        }
    }

    fun getLastUpdateTime(name: String): Long {
        val source = filterSources.find { it.name == name } ?: return 0
        return if (source.file.exists()) source.file.lastModified() else 0
    }

    fun clearFilterLists() {
        filterDir.listFiles()?.forEach { it.delete() }
    }

    private data class FilterSource(
        val name: String,
        val url: String,
        val file: File
    )
}
