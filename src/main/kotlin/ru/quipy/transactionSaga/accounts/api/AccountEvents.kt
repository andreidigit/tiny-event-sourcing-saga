package ru.quipy.transactionSaga.accounts.api


import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.math.BigDecimal
import java.util.*

const val ACCOUNT_CREATED = "ACCOUNT_CREATED_EVENT"
const val BANK_ACCOUNT_CREATED = "BANK_ACCOUNT_CREATED_EVENT"
const val BANK_ACCOUNT_DEPOSIT = "BANK_ACCOUNT_DEPOSIT_EVENT"
const val BANK_ACCOUNT_WITHDRAWAL = "BANK_ACCOUNT_WITHDRAWAL_EVENT"
const val INTERNAL_ACCOUNT_TRANSFER = "INTERNAL_ACCOUNT_TRANSFER_EVENT"

const val TRANSFER_HALF_COMPLETED = "TRANSFER_HALF_COMPLETED"
const val TRANSFER_COMPLETED = "TRANSFER_COMPLETED"
const val TRANSFER_PENDING_COMPLETED = "TRANSFER_PENDING_COMPLETED"
const val TRANSFER_ROLLBACK = "TRANSFER_ROLLBACK"
const val TRANSFER_FAILED = "TRANSFER_FAILED"


@DomainEvent(name = ACCOUNT_CREATED)
data class AccountCreatedEvent(
    val accountId: UUID,
    val userId: UUID,
) : Event<AccountAggregate>(
    name = ACCOUNT_CREATED,
)

@DomainEvent(name = BANK_ACCOUNT_CREATED)
data class BankAccountCreatedEvent(
    val accountId: UUID,
    val bankAccountId: UUID,
) : Event<AccountAggregate>(
    name = BANK_ACCOUNT_CREATED,
)

@DomainEvent(name = BANK_ACCOUNT_DEPOSIT)
data class BankAccountDepositEvent(
    val accountId: UUID,
    val bankAccountId: UUID,
    val amount: BigDecimal,
) : Event<AccountAggregate>(
    name = BANK_ACCOUNT_DEPOSIT,
)

@DomainEvent(name = BANK_ACCOUNT_WITHDRAWAL)
data class BankAccountWithdrawalEvent(
    val accountId: UUID,
    val bankAccountId: UUID,
    val amount: BigDecimal,
) : Event<AccountAggregate>(
    name = BANK_ACCOUNT_WITHDRAWAL,
)

@DomainEvent(name = INTERNAL_ACCOUNT_TRANSFER)
data class InternalAccountTransferEvent(
    val accountId: UUID,
    val bankAccountIdFrom: UUID,
    val bankAccountIdTo: UUID,
    val amount: BigDecimal,
) : Event<AccountAggregate>(
    name = INTERNAL_ACCOUNT_TRANSFER,
)

////////////

@DomainEvent(name = TRANSFER_HALF_COMPLETED)
data class TransferHalfCompletedEvent(
    val sourceAccountId: UUID,
    val sourceBankAccountId: UUID,
    val transactionId: UUID,
    val transferAmount: BigDecimal,
    val destinationAccountId: UUID,
    val destinationBankAccountId: UUID,

) : Event<AccountAggregate>(
    name = TRANSFER_HALF_COMPLETED,
)

@DomainEvent(name = TRANSFER_COMPLETED)
data class TransferCompletedEvent(
    val sourceAccountId: UUID,
    val sourceBankAccountId: UUID,
    val transactionId: UUID,
    val transferAmount: BigDecimal,
    val destinationBankAccountId: UUID,
) : Event<AccountAggregate>(
    name = TRANSFER_COMPLETED,
)

@DomainEvent(name = TRANSFER_PENDING_COMPLETED)
data class TransferPendingCompletedEvent(
    val sourceBankAccountId: UUID,
    val transactionId: UUID,
) : Event<AccountAggregate>(
    name = TRANSFER_PENDING_COMPLETED,
)

@DomainEvent(name = TRANSFER_ROLLBACK)
data class TransferRollbackEvent(
    val sourceAccountId: UUID,
    val sourceBankAccountId: UUID,
    val transactionId: UUID,
    val reason: String
) : Event<AccountAggregate>(
    name = TRANSFER_ROLLBACK,
)

@DomainEvent(name = TRANSFER_FAILED)
data class TransferFailedEvent(
    val sourceAccountId: UUID,
    val sourceBankAccountId: UUID,
    val transactionId: UUID,
    val reason: String
) : Event<AccountAggregate>(
    name = TRANSFER_FAILED,
)
