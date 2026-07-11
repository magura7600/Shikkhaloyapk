package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.graphics.Color

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class DashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDashboard() {
        composeTestRule.setContent {
            DashboardScreen(
                email = "test@test.com",
                userId = "123",
                profile = UserProfile(
                    user_id = "123",
                    email = "test@test.com",
                    role = "student",
                    full_name = "Test",
                    institution = "Test",
                    contact = "1234",
                    uid_code = "1234"
                ),
                onLogout = {},
                onProfileUpdate = {}
            )
        }
    }
}
