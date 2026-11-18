package com.dimenzy.app

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dimenzy.app.data.ModelRepository
import com.dimenzy.app.data.ModelStore
import com.dimenzy.app.data.StabilityApi
import com.dimenzy.app.image.CameraShots
import com.dimenzy.app.image.ImagePrep
import com.dimenzy.app.ui.ar.ArCoreCheck
import com.dimenzy.app.ui.ar.ArViewModel
import com.dimenzy.app.ui.capture.CaptureViewModel
import com.dimenzy.app.ui.generate.GenerateViewModel
import com.dimenzy.app.ui.preview.PreviewViewModel
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create

/** Hand written dependency container (no DI framework). Everything is built lazily, on first use. */
class AppContainer(private val app: Application) {

  /** Generated models live at filesDir/models/<uuid>.glb. */
  val modelStore: ModelStore by lazy { ModelStore(File(app.filesDir, "models")) }

  private val imagePrep: ImagePrep by lazy { ImagePrep(app.contentResolver, File(app.cacheDir, "upload.jpg")) }

  private val cameraShots: CameraShots by lazy { CameraShots(app) }

  // No logging interceptor: it would print the authorization header (spec 0002).
  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .build()
  }

  private val stabilityApi: StabilityApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.stability.ai/")
      .client(httpClient)
      .build()
      .create<StabilityApi>()
  }

  private val modelRepository: ModelRepository by lazy {
    ModelRepository(stabilityApi, modelStore, imagePrep.preparedPhoto, BuildConfig.STABILITY_API_KEY)
  }

  val captureViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { CaptureViewModel(createSavedStateHandle(), imagePrep, cameraShots) }
  }

  val generateViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { GenerateViewModel(createSavedStateHandle(), modelRepository, imagePrep) }
  }

  val previewViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { PreviewViewModel(createSavedStateHandle(), modelStore) }
  }

  val arCoreCheck: ArCoreCheck by lazy { ArCoreCheck(app) }

  val arViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { ArViewModel(createSavedStateHandle(), modelStore) }
  }

  private companion object {
    const val TIMEOUT_SECONDS = 90L
  }
}

class DimenzyApp : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}
