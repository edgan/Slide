package me.ccrama.redditslide.test

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.karumi.shot.ScreenshotTest
import me.ccrama.redditslide.Activities.MainActivity
import me.ccrama.redditslide.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.app.Activity

import kotlinx.coroutines.*

@RunWith(AndroidJUnit4::class)
class MainActivityScreenshotTest : ScreenshotTest {

    private lateinit var activityScenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun testMainActivityDefaultState() {
        GlobalScope.launch(Dispatchers.Main) {
            println("Before delay")
            delay(10000) // Wait for 10 seconds
            println("After delay")
        }

        // Once the view is displayed, capture the screenshot from the activity.
        activityScenario.onActivity { activity ->
            compareScreenshot(
                activity = activity,
                name = "main_activity_default"
            )
        }
    }
}
