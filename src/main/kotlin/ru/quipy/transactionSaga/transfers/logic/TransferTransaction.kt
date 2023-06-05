package ru.quipy.transactionSaga.transfers.logic

import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import ru.quipy.transactionSaga.transfers.api.TransactionCompletedEvent
import ru.quipy.transactionSaga.transfers.api.TransactionFailedEvent
import ru.quipy.transactionSaga.transfers.api.TransferTransactionAggregate
import ru.quipy.transactionSaga.transfers.api.TransferTransactionCreatedEvent
import ru.quipy.transactionSaga.transfers.logic.TransferTransaction.TransactionState.*
import java.math.BigDecimal
import java.util.*

class TransferTransaction : AggregateState<UUID, TransferTransactionAggregate> {
    private lateinit var transferId: UUID
    internal var transactionState = PROCESSING

    private lateinit var sourceParticipant: Participant
    private lateinit var destinationParticipant: Participant

    private lateinit var transferAmount: BigDecimal

    override fun getId() = transferId

    fun initiateTransferTransaction(
        transferId: UUID = UUID.randomUUID(),
        sourceAccountId: UUID,
        sourceBankAccountId: UUID,
        destinationAccountId: UUID,
        destinationBankAccountId: UUID,
        transferAmount: BigDecimal
    ): TransferTransactionCreatedEvent {
        return TransferTransactionCreatedEvent(
            transferId,
            sourceAccountId,
            sourceBankAccountId,
            destinationAccountId,
            destinationBankAccountId,
            transferAmount
        )
    }

    fun transferTransactionCompleted(): TransactionCompletedEvent{
        return TransactionCompletedEvent(
            transferId = transferId
        )
    }

    fun transferTransactionFailed(reason: String): TransactionFailedEvent{
        println(reason) // todo track failed
        return TransactionFailedEvent(
            transferId = transferId
        )
    }

    @StateTransitionFunc
    fun initiateTransferTransaction(event: TransferTransactionCreatedEvent) {
        this.transferId = event.transferId
        this.sourceParticipant = Participant(event.sourceAccountId, event.sourceBankAccountId)
        this.destinationParticipant = Participant(event.destinationAccountId, event.destinationBankAccountId)
        this.transferAmount = event.transferAmount
    }

    @StateTransitionFunc
    fun failed(event: TransactionFailedEvent) {
        transactionState = FAILED
    }

    @StateTransitionFunc
    fun completed(event: TransactionCompletedEvent) {
        transactionState = COMPLETED
    }

    data class Participant(
        internal val accountId: UUID,
        internal val bankAccountId: UUID,
    )

    enum class TransactionState {
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
