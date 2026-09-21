package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RetailPulseRepository
import com.example.model.*
import com.example.util.LocationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var repository: RetailPulseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = RetailPulseRepository(context)
    }

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RetailPulse", appName)
    }

    @Test
    fun testThirteenBranchesSeeded() = runBlocking {
        val branches = repository.branches.first()
        assertEquals(13, branches.size)
        assertTrue(branches.any { it.branchName == "Branch 01" && it.address.contains("Connaught Place") })
        assertTrue(branches.any { it.branchName == "Branch 02" })
        assertTrue(branches.any { it.branchName == "Branch 13" })
    }

    @Test
    fun testGeofenceCalculation() {
        val cp = Branch(
            branchId = "br_01",
            branchName = "Connaught Place",
            address = "CP, New Delhi",
            latitude = 28.6315,
            longitude = 77.2167,
            geofenceRadius = 100.0
        )

        // Exact match -> distance 0 -> inside geofence
        val distanceSame = LocationHelper.calculateDistanceMeters(28.6315, 77.2167, cp.latitude, cp.longitude)
        assertTrue(distanceSame < 1.0)
        assertTrue(LocationHelper.isWithinGeofence(28.6315, 77.2167, cp.latitude, cp.longitude, cp.geofenceRadius))

        // 2 km away -> outside geofence
        val distanceFar = LocationHelper.calculateDistanceMeters(28.6500, 77.2167, cp.latitude, cp.longitude)
        assertTrue(distanceFar > 100.0)
        assertFalse(LocationHelper.isWithinGeofence(28.6500, 77.2167, cp.latitude, cp.longitude, cp.geofenceRadius))
    }

    @Test
    fun testWalkInCreationAndStatusUpdate() = runBlocking {
        val initialCount = repository.walkIns.first().size
        val testUser = UserProfile(
            userId = "usr_emp_101",
            name = "Amit Verma",
            employeeId = "EMP-101",
            role = UserRole.EMPLOYEE,
            branchId = "br_01",
            branchName = "Connaught Place Flagship"
        )

        val result = repository.createWalkIn(
            customerName = "Rahul Sharma",
            customerMobile = "+91 9876543210",
            category = "Smartphones",
            model = "Galaxy S24 Ultra",
            budget = 129999.0,
            notes = "Customer comparing with iPhone 15 Pro Max",
            user = testUser
        )

        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("Rahul Sharma", created?.customerName)

        val updatedList = repository.walkIns.first()
        assertEquals(initialCount + 1, updatedList.size)

        // Update status to INTERESTED
        val updateRes = repository.updateLeadStatus(created!!.walkInId, LeadStatus.INTERESTED, testUser)
        assertTrue(updateRes.isSuccess)
        val afterUpdate = repository.walkIns.first().find { it.walkInId == created.walkInId }
        assertEquals(LeadStatus.INTERESTED, afterUpdate?.status)
    }

    @Test
    fun testSaleClosure() = runBlocking {
        val testUser = UserProfile(
            userId = "usr_emp_101",
            name = "Amit Verma",
            employeeId = "EMP-101",
            role = UserRole.EMPLOYEE,
            branchId = "br_01",
            branchName = "Connaught Place Flagship"
        )

        val leadResult = repository.createWalkIn(
            customerName = "Priya Mehta",
            customerMobile = "+91 9811223344",
            category = "Laptops",
            model = "MacBook Pro M3",
            budget = 169900.0,
            notes = "Ready to bill",
            user = testUser
        )
        val lead = leadResult.getOrThrow()

        val saleResult = repository.closeSaleAndLead(
            walkInId = lead.walkInId,
            invoiceNumber = "INV-DEL-TEST-999",
            billAmount = 169900.0,
            saleDate = "2026-09-20",
            productCategory = "Laptops",
            productModel = "MacBook Pro M3 16GB",
            quantity = 1,
            discount = 5000.0,
            notes = "With extended warranty",
            user = testUser
        )

        assertTrue(saleResult.isSuccess)

        val closedLead = repository.walkIns.first().find { it.walkInId == lead.walkInId }
        assertEquals(LeadStatus.CLOSED, closedLead?.status)

        val sales = repository.sales.first()
        assertTrue(sales.any { it.invoiceNumber == "INV-DEL-TEST-999" })
    }
}
