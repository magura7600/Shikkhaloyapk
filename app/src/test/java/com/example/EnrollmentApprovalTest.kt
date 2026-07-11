package com.example

import com.example.data.SupabaseRepository
import com.example.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class EnrollmentApprovalTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeSupabaseRepository : SupabaseRepository() {
        val addedEnrollments = mutableListOf<Enrollment>()
        val deletedRequestIds = mutableListOf<String>()

        override suspend fun addEnrollment(enrollment: Enrollment) {
            addedEnrollments.add(enrollment)
        }

        override suspend fun deleteEnrollmentRequest(requestId: String) {
            deletedRequestIds.add(requestId)
        }
    }

    private lateinit var fakeRepository: FakeSupabaseRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSupabaseRepository()
        viewModel = DashboardViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testApproveEnrollment_withFullQuarters_savesEmptyString() = runTest(testDispatcher) {
        // Arrange
        val request = EnrollmentRequest(
            id = "req_123",
            user_id = "user_1",
            course_id = "course_1",
            requested_quarters = "FULL",
            amount = "1000"
        )
        viewModel.setEnrollmentRequests(listOf(request))
        viewModel.setEnrollments(emptyList())

        // Act
        viewModel.approveEnrollment(request)
        advanceUntilIdle()

        // Assert
        assertEquals(1, fakeRepository.addedEnrollments.size)
        val savedEnrollment = fakeRepository.addedEnrollments.first()
        assertEquals("", savedEnrollment.purchased_quarters)
        assertEquals("user_1", savedEnrollment.user_id)
        assertEquals("course_1", savedEnrollment.course_id)
        assertEquals("1000", savedEnrollment.price_paid)

        assertEquals(1, fakeRepository.deletedRequestIds.size)
        assertEquals("req_123", fakeRepository.deletedRequestIds.first())

        // Verify UI State update
        val uiState = viewModel.uiState.value
        assertTrue(uiState.enrollmentRequests.isEmpty())
        assertEquals(1, uiState.enrollments.size)
        assertEquals("", uiState.enrollments.first().purchased_quarters)
    }

    @Test
    fun testApproveEnrollment_withSpecificQuarters_savesQuartersUnchanged() = runTest(testDispatcher) {
        // Arrange
        val request = EnrollmentRequest(
            id = "req_456",
            user_id = "user_2",
            course_id = "course_2",
            requested_quarters = "q1,q2",
            amount = "500"
        )
        viewModel.setEnrollmentRequests(listOf(request))
        viewModel.setEnrollments(emptyList())

        // Act
        viewModel.approveEnrollment(request)
        advanceUntilIdle()

        // Assert
        assertEquals(1, fakeRepository.addedEnrollments.size)
        val savedEnrollment = fakeRepository.addedEnrollments.first()
        assertEquals("q1,q2", savedEnrollment.purchased_quarters)

        assertEquals(1, fakeRepository.deletedRequestIds.size)
        assertEquals("req_456", fakeRepository.deletedRequestIds.first())

        // Verify UI State update
        val uiState = viewModel.uiState.value
        assertTrue(uiState.enrollmentRequests.isEmpty())
        assertEquals(1, uiState.enrollments.size)
        assertEquals("q1,q2", uiState.enrollments.first().purchased_quarters)
    }
}
