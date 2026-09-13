with open('app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceViewModel.kt', 'r') as f:
    content = f.read()

# 1. Remove grandTotal MutableStateFlow
content = content.replace('val grandTotal = MutableStateFlow(0.0)', '')

# Fix references to grandTotal
content = content.replace('grandTotal.value = 0.0', '')
content = content.replace('grandTotal.value = calcResult.totalAmount', '')

# 2. Add normalization to amountPaid setter and trigger recalculation
old_updateCustomerData = """    fun updateCustomerData(name: String, gstin: String, address: String) {
        customerName.value = name
        customerGSTIN.value = gstin
        buyerAddress.value = address

        // Auto-detect supply type
        if (sellerGstin.value.isNotBlank() && gstin.isNotBlank()) {
            supplyType.value = determineSupplyTypeUseCase(sellerGstin.value, gstin)
        }
    }"""

new_updateCustomerData = """    fun updateCustomerData(name: String, gstin: String, address: String) {
        customerName.value = name
        customerGSTIN.value = gstin
        buyerAddress.value = address

        // Auto-detect supply type
        if (sellerGstin.value.isNotBlank() && gstin.isNotBlank()) {
            supplyType.value = determineSupplyTypeUseCase(sellerGstin.value, gstin)
        }
        recalculateItems()
    }"""
content = content.replace(old_updateCustomerData, new_updateCustomerData)


# Implement setAmountPaid to normalize and recalculate
# Look for amountPaid.value = ... Wait, how is amountPaid set right now? Compose directly modifies it?
# Let's add setAmountPaid and setPaymentMethod

add_payment_methods = """
    fun setAmountPaid(amount: Double) {
        val normalized = if (amount.isFinite()) java.math.BigDecimal(amount.toString()).setScale(2, java.math.RoundingMode.HALF_UP).toDouble() else 0.0
        amountPaid.value = normalized
        recalculateItems()
    }

    fun setPaymentMethod(method: String) {
        paymentMethod.value = method
    }
"""

content = content.replace('    fun setSupplyType', add_payment_methods + '\n    fun setSupplyType')


# 3. Ensure removeItem triggers recalculation
old_removeItem = """    fun removeItem(index: Int) {
        val currentItems = _invoiceItems.value.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _invoiceItems.value = currentItems
        }
    }"""

new_removeItem = """    fun removeItem(index: Int) {
        val currentItems = _invoiceItems.value.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _invoiceItems.value = currentItems
            recalculateItems()
        }
    }"""
content = content.replace(old_removeItem, new_removeItem)


with open('app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceViewModel.kt', 'w') as f:
    f.write(content)
