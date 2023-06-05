package ru.quipy.transactionSaga.transfers.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import ru.quipy.transactionSaga.transfers.api.TransferTransactionAggregate
import ru.quipy.transactionSaga.transfers.logic.TransferTransaction
import java.util.*


@Configuration
class TransactionCountedContextConfig {

    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Bean
    fun transactionEsService(): EventSourcingService<UUID, TransferTransactionAggregate, TransferTransaction> =
        eventSourcingServiceFactory.create()

}
