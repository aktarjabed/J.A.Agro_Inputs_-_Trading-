package com.aktarjabed.inbusiness.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.dao.UserQuotaDao
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceSequence
import com.aktarjabed.inbusiness.data.entities.UserQuotaEntity
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import com.aktarjabed.inbusiness.domain.device.DeviceClassifier
import com.aktarjabed.inbusiness.util.SystemClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class InvoiceIdempotencyTest {

    private lateinit var database: AppDatabase
    private lateinit var invoiceDao: InvoiceDao
    private lateinit var userQuotaDao: UserQuotaDao
    private lateinit var repository: InvoiceRepository
    private lateinit var quotaGate: QuotaGate

    private val userId = "test-user-id"
    private val businessId = "test-business-id"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        invoiceDao = database.invoiceDao()
        userQuotaDao = database.userQuotaDao()

        val mockDeviceClassifier = mock(DeviceClassifier::class.java)
        val mockClock = mock(SystemClock::class.java)
        `when`(mockClock.todayEpochDay()).thenReturn(100L)
        `when`(mockClock.monthStartEpochDay()).thenReturn(90L)
        `when`(mockClock.today()).thenReturn(LocalDate.of(2025, 1, 1))

        quotaGate = QuotaGate(userQuotaDao, mockDeviceClassifier, mockClock, context)

        repository = InvoiceRepository(database, invoiceDao, database.productDao(), quotaGate)

        runBlocking {
            userQuotaDao.insertOrReplace(UserQuotaEntity(
                userId = userId,
                tier = "ENTERPRISE",
                dailyUsed = 0,
                lastResetEpochDay = 100L,
                monthlyUsed = 0,
                lastMonthlyResetEpochDay = 90L,
                watermark = false,
                retentionDays = 30,
                freeExpiryEpochDay = 9999L
            ))
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun creatingInvoiceWithSameIdempotencyKey_isIdempotent() = runBlocking {
        val idempotencyKey = UUID.randomUUID().toString()

        // First attempt creates the invoice
        val firstResult = repository.createInvoice(
            userId = userId,
            businessId = businessId,
            customerName = "First Customer",
            customerGSTIN = "",
            buyerAddress = "",
            supplyType = SupplyType.INTRA_STATE,
            totalAmount = 100.0,
            taxAmount = 0.0,
            totalCgst = 0.0,
            totalSgst = 0.0,
            totalIgst = 0.0,
            items = emptyList(),
            idempotencyKey = idempotencyKey
        )
        assertTrue(firstResult is InvoiceCreationResult.Success)

        val initialInvoices = invoiceDao.getAllInvoicesOnce(businessId)
        assertEquals(1, initialInvoices.size)

        // Second attempt with the same idempotency key should return success but not create a duplicate
        val secondResult = repository.createInvoice(
            userId = userId,
            businessId = businessId,
            customerName = "Second Customer", // Different data, but same key
            customerGSTIN = "",
            buyerAddress = "",
            supplyType = SupplyType.INTRA_STATE,
            totalAmount = 200.0,
            taxAmount = 0.0,
            totalCgst = 0.0,
            totalSgst = 0.0,
            totalIgst = 0.0,
            items = emptyList(),
            idempotencyKey = idempotencyKey
        )
        assertTrue(secondResult is InvoiceCreationResult.IdempotentReplay)

        val finalInvoices = invoiceDao.getAllInvoicesOnce(businessId)
        assertEquals(1, finalInvoices.size) // Still only 1 invoice
        assertEquals("First Customer", finalInvoices.first().customerName) // Retains original data
    }
}
