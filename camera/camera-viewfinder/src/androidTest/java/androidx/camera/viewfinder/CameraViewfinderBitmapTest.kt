/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.viewfinder

import android.content.Context
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.util.Size
import android.view.Display
import android.view.Surface
import androidx.camera.viewfinder.CameraViewfinder.ImplementationMode.COMPATIBLE
import androidx.camera.viewfinder.CameraViewfinder.ImplementationMode.PERFORMANCE
import androidx.camera.viewfinder.CameraViewfinder.ScaleType.FILL_CENTER
import androidx.camera.viewfinder.internal.utils.futures.FutureCallback
import androidx.camera.viewfinder.internal.utils.futures.Futures
import androidx.camera.viewfinder.utils.CoreAppTestUtil
import androidx.camera.viewfinder.utils.FakeActivity
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario.ActivityAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class CameraViewfinderBitmapTest {

    @get:Rule
    val mActivityRule: ActivityScenarioRule<FakeActivity> =
        ActivityScenarioRule<FakeActivity>(
            FakeActivity::class.java
        )

    companion object {
        private const val ANY_WIDTH = 640
        private const val ANY_HEIGHT = 480
        private val ANY_SIZE: Size by lazy { Size(ANY_WIDTH, ANY_HEIGHT) }
    }

    private val mInstrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var mSurfaceRequest: ViewfinderSurfaceRequest
    private lateinit var mContext: Context
    private lateinit var mDisplay: Display

    @Before
    fun setUp() {
        CoreAppTestUtil.prepareDeviceUI(mInstrumentation)
        mContext = ApplicationProvider.getApplicationContext()

        val displayManager = mContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        mDisplay = displayManager.getDisplay(0)

        mSurfaceRequest = ViewfinderSurfaceRequest(
            ANY_SIZE,
            mDisplay,
            /*isLegacyDevice=*/true,
            /*isFrontCamera=*/true,
            /*sensorOrientation=*/0
        )
    }

    @After
    fun tearDown() {
    }

    @Test
    @Throws(Throwable::class)
    fun bitmapNotNull_whenViewfinderIsDisplaying_surfaceView() {
        // Arrange
        val viewfinder: CameraViewfinder = setUpViewfinder(PERFORMANCE, FILL_CENTER)

        // assert
        runOnMainThread(Runnable {
            val surfaceListenableFuture: ListenableFuture<Surface> =
                viewfinder.requestSurfaceAsync(mSurfaceRequest)

            Futures.addCallback<Surface?>(
                surfaceListenableFuture,
                object : FutureCallback<Surface?> {
                    override fun onSuccess(surface: Surface?) {
                        val bitmap: Bitmap? = viewfinder.getBitmap()
                        Truth.assertThat(bitmap).isNotNull()
                        Truth.assertThat(bitmap?.width).isNotEqualTo(0)
                        Truth.assertThat(bitmap?.height).isNotEqualTo(0)
                    }

                    override fun onFailure(t: Throwable) {
                    }
                },
                ContextCompat.getMainExecutor(mContext)
            )
        })
    }

    @Test
    @Throws(Throwable::class)
    fun bitmapNotNull_whenViewfinderIsDisplaying_textureView() {
        // Arrange
        val viewfinder: CameraViewfinder = setUpViewfinder(COMPATIBLE, FILL_CENTER)

        // assert
        runOnMainThread(Runnable {
            val surfaceListenableFuture: ListenableFuture<Surface> =
                viewfinder.requestSurfaceAsync(mSurfaceRequest)

            Futures.addCallback<Surface?>(
                surfaceListenableFuture,
                object : FutureCallback<Surface?> {
                    override fun onSuccess(surface: Surface?) {
                        val bitmap: Bitmap? = viewfinder.getBitmap()
                        Truth.assertThat(bitmap).isNotNull()
                        Truth.assertThat(bitmap?.width).isNotEqualTo(0)
                        Truth.assertThat(bitmap?.height).isNotEqualTo(0)
                    }

                    override fun onFailure(t: Throwable) {
                    }
                },
                ContextCompat.getMainExecutor(mContext)
            )
        })
    }

    private fun setUpViewfinder(
        mode: CameraViewfinder.ImplementationMode,
        scaleType: CameraViewfinder.ScaleType
    ): CameraViewfinder {
        val viewfinderAtomicReference: AtomicReference<CameraViewfinder> =
            AtomicReference<CameraViewfinder>()
        runOnMainThread {
            val viewfiner =
                CameraViewfinder(ApplicationProvider.getApplicationContext<Context>())
            viewfiner.setImplementationMode(mode)
            viewfiner.setScaleType(scaleType)
            mActivityRule.getScenario().onActivity(
                ActivityAction<FakeActivity> { activity: FakeActivity ->
                    activity.setContentView(
                        viewfiner
                    )
                })
            viewfinderAtomicReference.set(viewfiner)
        }
        return viewfinderAtomicReference.get()
    }

    private fun runOnMainThread(block: Runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
