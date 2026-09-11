package com.aktarjabed.inbusiness.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.dao.ProductDao
import com.aktarjabed.inbusiness.data.dao.UserQuotaDao
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.Product
import com.aktarjabed.inbusiness.data.entities.UserQuotaEntity
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.device.DeviceClassifier
import com.aktarjabed.inbusiness.util.SystemClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class InvoiceTransactionTest {
    private lateinit var db: AppDatabase
    private lateinit var invoiceDao: InvoiceDao
    private lateinit var productDao: ProductDao
    private lateinit var userQuotaDao: UserQuotaDao
    private lateinit var quotaGate: QuotaGate
    private lateinit var repository: InvoiceRepository
    private lateinit var businessContext: com.aktarjabed.inbusiness.domain.context.BusinessContext

    private val businessId = "test_business_id"
    private val userId = "test_user_id"

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
        invoiceDao = db.invoiceDao()
        productDao = db.productDao()
        userQuotaDao = db.userQuotaDao()

        val mockDeviceClassifier = mock(DeviceClassifier::class.java)
        val mockClock = mock(SystemClock::class.java)
        `when`(mockClock.todayEpochDay()).thenReturn(100L)
        `when`(mockClock.monthStartEpochDay()).thenReturn(90L)
        `when`(mockClock.today()).thenReturn(LocalDate.of(2025, 1, 1))

        quotaGate = QuotaGate(userQuotaDao, mockDeviceClassifier, mockClock, context)

        businessContext = mock(com.aktarjabed.inbusiness.domain.context.BusinessContext::class.java)
        `when`(businessContext.activeBusinessId).thenReturn(kotlinx.coroutines.flow.flowOf(businessId))
        `when`(businessContext.currentUserId).thenReturn(kotlinx.coroutines.flow.flowOf(userId))

        repository = InvoiceRepository(db, invoiceDao, productDao, quotaGate, businessContext)

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
    fun closeDb() {
        db.close()
    }

    @Test
    fun testTransactionRollbackOnStockFailure() = runBlocking {
        // Create 2 products. Product 1 has enough stock. Product 2 has 0 stock.
        val prod1 = Product(businessId = businessId, name = "P1", brand = "B1", category = "C1", unitType = "Kg", pricePerUnit = 100.0, availableStock = 10.0, batchNumber = "B1", isWholesaleOnly = false)
        val prod2 = Product(businessId = businessId, name = "P2", brand = "B2", category = "C2", unitType = "Kg", pricePerUnit = 200.0, availableStock = 0.0, batchNumber = "B2", isWholesaleOnly = false)

        val p1Id = productDao.insertProduct(prod1)
        val p2Id = productDao.insertProduct(prod2)

        val initialSeq = invoiceDao.getInvoiceSequence(businessId)?.lastSequenceNumber ?: 0

        val items = listOf(
            InvoiceItem(description = "P1", quantity = 5.0, pricePerUnit = 100.0, productId = p1Id), // Should succeed internally
            InvoiceItem(description = "P2", quantity = 5.0, pricePerUnit = 200.0, productId = p2Id)  // Will fail stock deduction
        )

        val result = repository.createInvoice(
            sellerName = "Seller", sellerAddress = "Address", sellerGSTIN = null, customerName = "Test Cust", customerGSTIN = null, buyerAddress = "",
            supplyType = SupplyType.INTRA_STATE, subtotal = 1500.0, totalAmount = 1500.0, taxAmount = 0.0, totalCgst = 0.0, totalSgst = 0.0, totalIgst = 0.0,
            items = items
        )

        assertTrue("Expected InsufficientStock result", result is InvoiceCreationResult.InsufficientStock)

        // Verify Rollback
        val invoices = invoiceDao.getAllInvoicesOnce(businessId)
        assertTrue("Invoice should not be inserted", invoices.isEmpty())

        val postSeq = invoiceDao.getInvoiceSequence(businessId)?.lastSequenceNumber ?: 0
        assertEquals("Sequence should rollback", initialSeq, postSeq)

        val updatedP1 = productDao.getProductById(p1Id, businessId)
        assertEquals("Product 1 stock should rollback", 10.0, updatedP1!!.availableStock, 0.0)
    }
}
