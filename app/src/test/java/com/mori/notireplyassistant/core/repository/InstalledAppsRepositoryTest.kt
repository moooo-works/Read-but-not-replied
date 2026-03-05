package com.mori.notireplyassistant.core.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // using Robolectric since it uses android.content.Intent
class InstalledAppsRepositoryTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var pm: PackageManager
    @Mock private lateinit var mockDrawable1: Drawable
    @Mock private lateinit var mockDrawable2: Drawable
    @Mock private lateinit var mockDrawable3: Drawable

    private lateinit var repository: InstalledAppsRepository

    @Before
    fun setup() {
        org.mockito.MockitoAnnotations.openMocks(this)
        `when`(context.packageManager).thenReturn(pm)
        repository = InstalledAppsRepository(context)
    }

    @Test
    fun getInstalledApps_filtersAndSortsCorrectly() {
        val resolveInfo1 = createResolveInfo("com.test.z")
        val resolveInfo2 = createResolveInfo("com.test.a")
        val resolveInfo3 = createResolveInfo("com.test.m")

        // Setup package manager to return mock labels and icons
        setupMockPackageManager(resolveInfo1, "Zebra App", mockDrawable1)
        setupMockPackageManager(resolveInfo2, "Apple App", mockDrawable2)
        setupMockPackageManager(resolveInfo3, "Monkey App", mockDrawable3)

        // On SDK 33 it calls queryIntentActivities with ResolveInfoFlags
        `when`(pm.queryIntentActivities(
            any(Intent::class.java),
            any(PackageManager.ResolveInfoFlags::class.java)
        )).thenReturn(listOf(resolveInfo1, resolveInfo2, resolveInfo3))

        val result = repository.getInstalledApps()

        assertEquals(3, result.size)
        // Check sorting by label (Apple, Monkey, Zebra)
        assertEquals("Apple App", result[0].name)
        assertEquals("com.test.a", result[0].packageName)
        assertEquals(mockDrawable2, result[0].icon)

        assertEquals("Monkey App", result[1].name)
        assertEquals("com.test.m", result[1].packageName)
        assertEquals(mockDrawable3, result[1].icon)

        assertEquals("Zebra App", result[2].name)
        assertEquals("com.test.z", result[2].packageName)
        assertEquals(mockDrawable1, result[2].icon)
    }

    private fun createResolveInfo(packageName: String): ResolveInfo {
        val resolveInfo = mock(ResolveInfo::class.java)
        val activityInfo = ActivityInfo()
        activityInfo.packageName = packageName
        resolveInfo.activityInfo = activityInfo
        return resolveInfo
    }

    private fun setupMockPackageManager(resolveInfo: ResolveInfo, label: String, icon: Drawable) {
        `when`(resolveInfo.loadLabel(pm)).thenReturn(label)
        `when`(resolveInfo.loadIcon(pm)).thenReturn(icon)
    }
}
