with open('app/src/main/java/com/aktarjabed/inbusiness/domain/usecase/GetProductSuggestionsUseCase.kt', 'r') as f:
    content = f.read()

old_logic = """                val suggestions = mutableListOf<ProductSuggestion>()

                // Track descriptions added from history to prevent duplicates
                val historyMap = mutableMapOf<String, InvoiceItem>()
                for (item in historicalItems) {
                    val key = item.description.trim().lowercase()
                    if (!historyMap.containsKey(key)) {
                        historyMap[key] = item
                        // We will add them to suggestions later to maintain sort or precedence
                    }
                }

                val processedCatalogKeys = mutableSetOf<String>()

                // Add current catalog products, overriding price/gst with historical if available
                for (product in products) {
                    val key = product.name.trim().lowercase()
                    val historicalMatch = historyMap[key]

                    if (historicalMatch != null) {
                        // Historical sale values take precedence
                        suggestions.add(
                            ProductSuggestion(
                                description = product.name,
                                unitType = historicalMatch.unitType,
                                pricePerUnit = historicalMatch.pricePerUnit,
                                gstPercentage = historicalMatch.gstPercentage,
                                product = product
                            )
                        )
                    } else {
                        // No history, use catalog defaults
                        suggestions.add(
                            ProductSuggestion(
                                description = product.name,
                                unitType = product.unitType,
                                pricePerUnit = product.pricePerUnit,
                                gstPercentage = product.gstPercentage,
                                product = product
                            )
                        )
                    }
                    processedCatalogKeys.add(key)
                }

                // Add historical items that don't match any catalog product name
                for ((key, item) in historyMap) {
                    if (!processedCatalogKeys.contains(key)) {
                        suggestions.add(
                            ProductSuggestion(
                                description = item.description,
                                unitType = item.unitType,
                                pricePerUnit = item.pricePerUnit,
                                gstPercentage = item.gstPercentage,
                                product = null
                            )
                        )
                    }
                }"""

new_logic = """                val suggestions = mutableListOf<ProductSuggestion>()

                val historyByProductId = mutableMapOf<Int, InvoiceItem>()
                val historyByDescription = mutableMapOf<String, InvoiceItem>()

                for (item in historicalItems) {
                    if (item.productId != null) {
                        if (!historyByProductId.containsKey(item.productId)) {
                            historyByProductId[item.productId] = item
                        }
                    } else {
                        val key = item.description.trim().lowercase()
                        if (!historyByDescription.containsKey(key)) {
                            historyByDescription[key] = item
                        }
                    }
                }

                // Add current catalog products, overriding price/gst only if there is a linked historical match (by productId)
                for (product in products) {
                    val historicalMatch = historyByProductId[product.id]

                    if (historicalMatch != null) {
                        // Linked historical sale values take precedence
                        suggestions.add(
                            ProductSuggestion(
                                description = product.name,
                                unitType = historicalMatch.unitType,
                                pricePerUnit = historicalMatch.pricePerUnit,
                                gstPercentage = historicalMatch.gstPercentage,
                                product = product
                            )
                        )
                    } else {
                        // No linked history, use catalog defaults
                        suggestions.add(
                            ProductSuggestion(
                                description = product.name,
                                unitType = product.unitType,
                                pricePerUnit = product.pricePerUnit,
                                gstPercentage = product.gstPercentage,
                                product = product
                            )
                        )
                    }
                }

                // Add ad-hoc historical items (productId == null)
                for ((_, item) in historyByDescription) {
                    suggestions.add(
                        ProductSuggestion(
                            description = item.description,
                            unitType = item.unitType,
                            pricePerUnit = item.pricePerUnit,
                            gstPercentage = item.gstPercentage,
                            product = null
                        )
                    )
                }"""

content = content.replace(old_logic, new_logic)
with open('app/src/main/java/com/aktarjabed/inbusiness/domain/usecase/GetProductSuggestionsUseCase.kt', 'w') as f:
    f.write(content)
