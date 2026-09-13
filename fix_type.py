with open('app/src/main/java/com/aktarjabed/inbusiness/domain/usecase/GetProductSuggestionsUseCase.kt', 'r') as f:
    content = f.read()

content = content.replace('val historyByProductId = mutableMapOf<Int, InvoiceItem>()', 'val historyByProductId = mutableMapOf<Long, InvoiceItem>()')

with open('app/src/main/java/com/aktarjabed/inbusiness/domain/usecase/GetProductSuggestionsUseCase.kt', 'w') as f:
    f.write(content)
