package ru.quipy.transactionSaga.transfers.api

import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.math.BigDecimal
import java.util.*

const val TRANSFER_TRANSACTION_CREATED = "TRANSFER_TRANSACTION_CREATED"
const val TRANSFER_TRANSACTION_COMPLETED = "TRANSFER_TRANSACTION_COMPLETED"
const val TRANSFER_TRANSACTION_FAILED = "TRANSFER_TRANSACTION_FAILED"

@DomainEvent(name = TRANSFER_TRANSACTION_CREATED)
data class TransferTransactionCreatedEvent(
    val transferId: UUID,

    val sourceAccountId: UUID,
    val sourceBankAccountId: UUID,

    val destinationAccountId: UUID,
    val destinationBankAccountId: UUID,

    val transferAmount: BigDecimal,
) : Event<TransferTransactionAggregate>(
    name = TRANSFER_TRANSACTION_CREATED,
)

@DomainEvent(name = TRANSFER_TRANSACTION_COMPLETED)
data class TransactionCompletedEvent(
    val transferId: UUID,
) : Event<TransferTransactionAggregate>(
    name = TRANSFER_TRANSACTION_COMPLETED,
)

@DomainEvent(name = TRANSFER_TRANSACTION_FAILED)
data class TransactionFailedEvent(
    val transferId: UUID,
) : Event<TransferTransactionAggregate>(
    name = TRANSFER_TRANSACTION_FAILED,
)
