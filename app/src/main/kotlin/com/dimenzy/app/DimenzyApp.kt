package com.dimenzy.app

import android.app.Application
import java.io.File

/** Hand written dependency container (no DI framework). Later features add the API client and ModelRepository here. */
class AppContainer(private val app: Application) {
  /** Generated models live at filesDir/models/<uuid>.glb. */
  val modelsDir: File
    get() = File(app.filesDir, "models").apply { mkdirs() }
}

class DimenzyApp : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}
