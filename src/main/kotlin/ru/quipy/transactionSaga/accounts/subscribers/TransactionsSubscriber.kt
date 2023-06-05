package ru.quipy.transactionSaga.accounts.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.quipy.transactionSaga.accounts.logic.Account
import ru.quipy.core.EventSourcingService
import ru.quipy.streams.AggregateSubscriptionsManager
import ru.quipy.transactionSaga.accounts.api.*
import ru.quipy.transactionSaga.transfers.api.TransferTransactionAggregate
import ru.quipy.transactionSaga.transfers.api.TransferTransactionCreatedEvent
import java.util.*
import javax.annotation.PostConstruct

@Component
class TransactionsSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val accountEsService: EventSourcingService<UUID, AccountAggregate, Account>
) {
    private val logger: Logger = LoggerFactory.getLogger(TransactionsSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(
            TransferTransactionAggregate::class,
            "accounts::transaction-processing-subscriber"
        ) {
            `when`(TransferTransactionCreatedEvent::class) { event ->
                logger.info("Transaction to process: ${event.transferId}")

                accountEsService.update(event.sourceAccountId) { // todo sukhoa idempotence!
                    it.performTransferFrom(
                        event.sourceBankAccountId,
                        event.transferId,
                        event.transferAmount,
                        event.destinationAccountId,
                        event.destinationBankAccountId
                    )
                }

                logger.info("Transaction: ${event.transferId} has Started")
            }
            // todo sukhoa bank account deleted event
        }

        subscriptionsManager.createSubscriber(AccountAggregate::class, "accounts::bank-accounts-subscriber") {
            `when`(TransferHalfCompletedEvent::class) { event ->
                accountEsService.update(event.destinationAccountId) { // todo sukhoa idempotence!
                    it.performTransferTo(
                        event.sourceAccountId,
                        event.sourceBankAccountId,
                        event.transactionId,
                        event.transferAmount,
                        event.destinationBankAccountId
                    )
                }
                logger.info("Transaction: ${event.transactionId} is Half-Completed")
            }
            `when`(TransferRollbackEvent::class) { event ->
                accountEsService.update(event.sourceAccountId) {
                    it.rollback(event.sourceBankAccountId, event.transactionId, event.reason)
                }
                logger.info("Transaction: ${event.transactionId} Failed")
            }
            `when`(TransferCompletedEvent::class) { event ->
                accountEsService.update(event.sourceAccountId) {
                    it.transferCompletePending(event.sourceBankAccountId, event.transactionId)
                }
                logger.info("Transaction: ${event.transactionId} Completed")

            }
        }
    }
}
