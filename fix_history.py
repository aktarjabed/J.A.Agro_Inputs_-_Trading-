with open('app/src/main/java/com/aktarjabed/inbusiness/data/dao/InvoiceDao.kt', 'r') as f:
    content = f.read()

# Replace getHistoricalInvoiceItems query to split identity based on productId nullability
old_query = """        @Query(\"\"\"
        SELECT i.*
        FROM invoice_items i
        INNER JOIN invoices inv ON i.invoiceId = inv.id
        WHERE inv.businessId = :businessId
          AND i.id = (
              SELECT i2.id FROM invoice_items i2
              INNER JOIN invoices inv2 ON i2.invoiceId = inv2.id
              WHERE inv2.businessId = :businessId
                AND LOWER(TRIM(i2.description)) = LOWER(TRIM(i.description))
              ORDER BY inv2.createdAt DESC, i2.id DESC
              LIMIT 1
          )
        ORDER BY inv.createdAt DESC
    \"\"\")
    fun getHistoricalInvoiceItems(businessId: String): Flow<List<InvoiceItem>>"""

new_query = """        @Query(\"\"\"
        SELECT i.*
        FROM invoice_items i
        INNER JOIN invoices inv ON i.invoiceId = inv.id
        WHERE inv.businessId = :businessId
          AND i.id = (
              SELECT i2.id FROM invoice_items i2
              INNER JOIN invoices inv2 ON i2.invoiceId = inv2.id
              WHERE inv2.businessId = :businessId
                AND IFNULL(i2.productId, -1) = IFNULL(i.productId, -1)
                AND (
                    (i.productId IS NOT NULL) OR
                    (i.productId IS NULL AND LOWER(TRIM(i2.description)) = LOWER(TRIM(i.description)))
                )
              ORDER BY inv2.createdAt DESC, i2.id DESC
              LIMIT 1
          )
        ORDER BY inv.createdAt DESC
    \"\"\")
    fun getHistoricalInvoiceItems(businessId: String): Flow<List<InvoiceItem>>"""

content = content.replace(old_query, new_query)

with open('app/src/main/java/com/aktarjabed/inbusiness/data/dao/InvoiceDao.kt', 'w') as f:
    f.write(content)
