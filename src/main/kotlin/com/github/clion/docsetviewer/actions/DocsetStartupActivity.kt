package com.github.clion.docsetviewer.actions

import com.github.clion.docsetviewer.core.index.DocsetIndexService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Startup activity that loads configured docsets when the IDE starts.
 */
class DocsetStartupActivity : ProjectActivity {

    private val log = Logger.getInstance(DocsetStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val indexService = DocsetIndexService.getInstance()
                indexService.loadFromSettings()
                log.info("Docset startup complete. Loaded ${indexService.getAllDocsets().size} docsets.")
            } catch (e: Exception) {
                log.warn("Failed to load docsets on startup: ${e.message}")
            }
        }
    }
}
