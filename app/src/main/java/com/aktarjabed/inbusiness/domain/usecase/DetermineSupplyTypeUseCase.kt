package com.aktarjabed.inbusiness.domain.usecase

import com.aktarjabed.inbusiness.domain.invoice.GstCalculator
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import javax.inject.Inject

class DetermineSupplyTypeUseCase @Inject constructor() {
    operator fun invoke(sellerGstin: String?, buyerGstin: String?): SupplyType {
        return GstCalculator.determineSupplyType(sellerGstin, buyerGstin)
    }
}
