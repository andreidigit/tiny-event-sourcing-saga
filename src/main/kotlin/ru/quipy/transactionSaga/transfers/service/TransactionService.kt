package ru.quipy.transactionSaga.transfers.service

import org.springframework.stereotype.Service
import ru.quipy.transactionSaga.transfers.api.TransferTransactionAggregate
import ru.quipy.transactionSaga.transfers.api.TransferTransactionCreatedEvent
import ru.quipy.transactionSaga.transfers.logic.TransferTransaction
import ru.quipy.transactionSaga.transfers.projections.BankAccountCacheRepository
import ru.quipy.core.EventSourcingService
import java.math.BigDecimal
import java.util.*

@Service
class TransactionService(
    private val bankAccountCacheRepository: BankAccountCacheRepository,
    private val transactionEsService: EventSourcingService<UUID, TransferTransactionAggregate, TransferTransaction>
) {
    fun initiateTransferTransaction(
        sourceBankAccountId: UUID,
        destinationBankAccountId: UUID,
        transferAmount: BigDecimal
    ): TransferTransactionCreatedEvent {
        val srcBankAccount = bankAccountCacheRepository.findById(sourceBankAccountId).orElseThrow {
            IllegalArgumentException("Cannot create transaction. There is no source bank account: $sourceBankAccountId")
        }

        val dstBankAccount = bankAccountCacheRepository.findById(destinationBankAccountId).orElseThrow {
            IllegalArgumentException("Cannot create transaction. There is no destination bank account: $destinationBankAccountId")
        }

        return transactionEsService.create {
            it.initiateTransferTransaction(
                sourceAccountId = srcBankAccount.accountId,
                sourceBankAccountId = srcBankAccount.bankAccountId,
                destinationAccountId = dstBankAccount.accountId,
                destinationBankAccountId = dstBankAccount.bankAccountId,
                transferAmount = transferAmount
            )
        }
    }
}
