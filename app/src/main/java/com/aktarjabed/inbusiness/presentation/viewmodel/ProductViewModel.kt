package com.aktarjabed.inbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.dao.ProductDao
import com.aktarjabed.inbusiness.data.entities.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productDao: ProductDao
) : ViewModel() {

    val existingCategories: StateFlow<List<String>> = productDao.getUniqueCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val existingUnitTypes: StateFlow<List<String>> = productDao.getUniqueUnitTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveProduct(
        name: String,
        brand: String,
        category: String,
        unitType: String,
        price: Double,
        stock: Double,
        batch: String?,
        isWholesale: Boolean
    ) {
        viewModelScope.launch {
            val newProduct = Product(
                name = name,
                brand = brand,
                category = category.trim(),
                unitType = unitType.trim(),
                pricePerUnit = price,
                availableStock = stock,
                batchNumber = batch,
                isWholesaleOnly = isWholesale
            )

            productDao.insertProduct(newProduct)
        }
    }
}
