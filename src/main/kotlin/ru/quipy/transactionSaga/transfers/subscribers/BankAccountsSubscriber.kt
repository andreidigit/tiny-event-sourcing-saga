package ru.quipy.bankDemo.transfers.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.quipy.core.EventSourcingService
import ru.quipy.streams.AggregateSubscriptionsManager
import ru.quipy.transactionSaga.accounts.api.AccountAggregate
import ru.quipy.transactionSaga.accounts.api.TransferCompletedEvent
import ru.quipy.transactionSaga.accounts.api.TransferFailedEvent
import ru.quipy.transactionSaga.transfers.api.TransferTransactionAggregate
import ru.quipy.transactionSaga.transfers.logic.TransferTransaction
import java.util.*
import javax.annotation.PostConstruct

@Component
class BankAccountsSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val transactionEsService: EventSourcingService<UUID, TransferTransactionAggregate, TransferTransaction>
) {
    private val logger: Logger = LoggerFactory.getLogger(BankAccountsSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(AccountAggregate::class, "transactions::bank-accounts-subscriber") {
            `when`(TransferCompletedEvent::class) { event ->
                transactionEsService.update(event.transactionId) {
                    it.transferTransactionCompleted()
                }
            }
            `when`(TransferFailedEvent::class) { event ->
                transactionEsService.update(event.transactionId) {
                    it.transferTransactionFailed(event.reason)
                }
            }
        }
    }
}
