package com.mori.notireplyassistant.core.common

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import android.content.ComponentName
import android.provider.Settings

@RunWith(RobolectricTestRunner::class)
class NotificationAccessCheckerTest {

    private lateinit var checker: NotificationAccessChecker
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        checker = NotificationAccessChecker(context)
    }

    @Test
    fun hasAccess_whenPackageNotEnabled_returnsFalse() {
        // By default, no packages are enabled in Settings.Secure.ENABLED_NOTIFICATION_LISTENERS
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            "com.other.app/com.other.app.Listener"
        )
        assertFalse(checker.hasAccess())
    }

    @Test
    fun hasAccess_whenPackageEnabled_returnsTrue() {
        val componentName = ComponentName(context.packageName, "com.mori.notireplyassistant.service.NotificationListenerServiceImpl")
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            componentName.flattenToString()
        )

        assertTrue(checker.hasAccess())
    }
}
