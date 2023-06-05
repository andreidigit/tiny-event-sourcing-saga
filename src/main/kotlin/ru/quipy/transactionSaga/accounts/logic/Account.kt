package ru.quipy.transactionSaga.accounts.logic

import ru.quipy.transactionSaga.accounts.api.*
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import ru.quipy.domain.Event
import java.math.BigDecimal
import java.util.*

// вопрос, что делать, если, скажем, обрабатываем какой-то ивент, понимаем, что агрегата, который нужно обновить не существует.
// Может ли ивент (ошибка) существовать в отрыве от агрегата?


class Account : AggregateState<UUID, AccountAggregate> {
    private lateinit var accountId: UUID
    private lateinit var holderId: UUID
    var bankAccounts: MutableMap<UUID, BankAccount> = mutableMapOf()

    override fun getId() = accountId

    fun createNewAccount(id: UUID = UUID.randomUUID(), holderId: UUID): AccountCreatedEvent {
        return AccountCreatedEvent(id, holderId)
    }

    fun createNewBankAccount(): BankAccountCreatedEvent {
        if (bankAccounts.size >= 5)
            throw IllegalStateException("Account $accountId already has ${bankAccounts.size} bank accounts")

        return BankAccountCreatedEvent(accountId = accountId, bankAccountId = UUID.randomUUID())
    }

    fun deposit(toBankAccountId: UUID, amount: BigDecimal): BankAccountDepositEvent {
        val bankAccount = (bankAccounts[toBankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer to: $toBankAccountId"))

        if (bankAccount.balance + amount > BigDecimal(10_000_000))
            throw IllegalStateException("You can't store more than 10.000.000 on account ${bankAccount.id}")

        if (bankAccounts.values.sumOf { it.balance } + amount > BigDecimal(25_000_000))
            throw IllegalStateException("You can't store more than 25.000.000 in total")


        return BankAccountDepositEvent(
            accountId = accountId,
            bankAccountId = toBankAccountId,
            amount = amount
        )
    }

    fun withdraw(fromBankAccountId: UUID, amount: BigDecimal): BankAccountWithdrawalEvent {
        val fromBankAccount = bankAccounts[fromBankAccountId]
            ?: throw IllegalArgumentException("No such account to withdraw from: $fromBankAccountId")

        if (amount > fromBankAccount.balance) {
            throw IllegalArgumentException("Cannot withdraw $amount. Not enough money: ${fromBankAccount.balance}")
        }

        return BankAccountWithdrawalEvent(
            accountId = accountId,
            bankAccountId = fromBankAccountId,
            amount = amount
        )
    }

    fun performTransferFrom(
        bankAccountId: UUID,
        transactionId: UUID,
        transferAmount: BigDecimal,
        destinationAccountId: UUID,
        destinationBankAccountId: UUID,
    ): Event<AccountAggregate> {
        val bankAccount = bankAccounts[bankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer from: $bankAccountId")

        if (transferAmount > bankAccount.balance) {
            return TransferFailedEvent(
                sourceAccountId = accountId,
                sourceBankAccountId = bankAccountId,
                transactionId = transactionId,
                "Cannot withdraw $transferAmount. Not enough money: ${bankAccount.balance}"
            )
        }

        return TransferHalfCompletedEvent(
            sourceAccountId = accountId,
            sourceBankAccountId = bankAccountId,
            transactionId = transactionId,
            transferAmount = transferAmount,
            destinationAccountId = destinationAccountId,
            destinationBankAccountId = destinationBankAccountId,
        )
    }

    fun performTransferTo(
        sourceAccountId: UUID,
        sourceBankAccountId: UUID,
        transactionId: UUID,
        transferAmount: BigDecimal,
        bankAccountId: UUID
    ): Event<AccountAggregate> {
        val frozenBankAccountAmount =
            bankAccounts[bankAccountId]?.pendingTransactions?.values?.sumOf { it.transferAmountFrozen }!!
        val frozenAllBankAccountsAmount =
            bankAccounts.values.sumOf { acc -> acc.pendingTransactions.values.sumOf { it.transferAmountFrozen } }

        val bankAccount = bankAccounts[bankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer to: $bankAccountId")

        if (bankAccount.balance + transferAmount + frozenBankAccountAmount > BigDecimal(10_000_000)) {
            return TransferRollbackEvent(
                sourceAccountId = sourceAccountId,
                sourceBankAccountId = bankAccountId,
                transactionId = transactionId,
                "User can't store more than 10.000.000 on account: ${bankAccount.id}"
            )
        }

        if (
            bankAccounts.values.sumOf { it.balance }
            + transferAmount + frozenAllBankAccountsAmount > BigDecimal(25_000_000)
        ) {
            return TransferRollbackEvent(
                sourceAccountId = sourceAccountId,
                sourceBankAccountId = sourceBankAccountId,
                transactionId = transactionId,
                "User can't store more than 25.000.000 in total on account: ${bankAccount.id}"
            )
        }

        return TransferCompletedEvent(
            sourceAccountId = sourceAccountId,
            sourceBankAccountId = sourceBankAccountId,
            transactionId = transactionId,
            transferAmount = transferAmount,
            destinationBankAccountId = bankAccountId
        )
    }

    fun rollback(bankAccountId: UUID, transactionId: UUID, reason: String): Event<AccountAggregate> {
        val pendingTransaction = bankAccounts[bankAccountId]!!.pendingTransactions[transactionId]!!
        // todo validation
        return TransferFailedEvent(
            sourceAccountId = accountId,
            sourceBankAccountId = bankAccountId,
            transactionId,
            reason
        )
    }

    fun transferCompletePending(sourceBankAccountId: UUID, transactionId: UUID): TransferPendingCompletedEvent {
        return TransferPendingCompletedEvent(
            sourceBankAccountId,
            transactionId
        )
    }

    @StateTransitionFunc
    fun transferHalfCompletedEvent(event: TransferHalfCompletedEvent) {
        bankAccounts[event.sourceBankAccountId]!!.initiatePendingTransaction(
            PendingTransaction(
                event.transactionId,
                event.transferAmount,
            )
        )
    }

    @StateTransitionFunc
    fun transferFailedEvent(event: TransferFailedEvent) {
        bankAccounts[event.sourceBankAccountId]!!.rollbackPendingTransaction(event.transactionId)
    }

    @StateTransitionFunc
    fun transferRollbackEvent(event: TransferRollbackEvent) = Unit

    @StateTransitionFunc
    fun transferCompletePendingEvent(event: TransferPendingCompletedEvent) {
        bankAccounts[event.sourceBankAccountId]!!.processPendingTransaction(event.transactionId)
    }

    @StateTransitionFunc
    fun transferCompletePendingEvent(event: TransferCompletedEvent) {
        bankAccounts[event.destinationBankAccountId]!!.deposit(event.transferAmount)
    }


    /////////////////////////////

    fun transferBetweenInternalAccounts(
        fromBankAccountId: UUID,
        toBankAccountId: UUID,
        transferAmount: BigDecimal
    ): InternalAccountTransferEvent {
        val bankAccountFrom = bankAccounts[fromBankAccountId]
            ?: throw IllegalArgumentException("No such account to withdraw from: $fromBankAccountId")

        if (transferAmount > bankAccountFrom.balance) {
            throw IllegalArgumentException("Cannot withdraw $transferAmount. Not enough money: ${bankAccountFrom.balance}")
        }

        val bankAccountTo = (bankAccounts[toBankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer to: $toBankAccountId"))


        if (bankAccountTo.balance + transferAmount > BigDecimal(10_000_000))
            throw IllegalStateException("You can't store more than 10.000.000 on account ${bankAccountTo.id}")

        return InternalAccountTransferEvent(
            accountId = accountId,
            bankAccountIdFrom = fromBankAccountId,
            bankAccountIdTo = toBankAccountId,
            amount = transferAmount
        )
    }

    @StateTransitionFunc
    fun createNewBankAccount(event: AccountCreatedEvent) {
        accountId = event.accountId
        holderId = event.userId
    }

    @StateTransitionFunc
    fun createNewBankAccount(event: BankAccountCreatedEvent) {
        bankAccounts[event.bankAccountId] = BankAccount(event.bankAccountId)
    }

    @StateTransitionFunc
    fun deposit(event: BankAccountDepositEvent) {
        bankAccounts[event.bankAccountId]!!.deposit(event.amount)
    }

    @StateTransitionFunc
    fun withdraw(event: BankAccountWithdrawalEvent) {
        bankAccounts[event.bankAccountId]!!.withdraw(event.amount)
    }

    @StateTransitionFunc
    fun internalAccountTransfer(event: InternalAccountTransferEvent) {
        bankAccounts[event.bankAccountIdFrom]!!.withdraw(event.amount)
        bankAccounts[event.bankAccountIdTo]!!.deposit(event.amount)
    }


}


data class BankAccount(
    val id: UUID,
    internal var balance: BigDecimal = BigDecimal.ZERO,
    internal var pendingTransactions: MutableMap<UUID, PendingTransaction> = mutableMapOf()
) {
    fun deposit(amount: BigDecimal) {
        this.balance = this.balance.add(amount)
    }

    fun withdraw(amount: BigDecimal) {
        this.balance = this.balance.subtract(amount)
    }

    fun initiatePendingTransaction(pendingTransaction: PendingTransaction) {
        withdraw(pendingTransaction.transferAmountFrozen)
        pendingTransactions[pendingTransaction.transactionId] = pendingTransaction
    }

    fun processPendingTransaction(trId: UUID) {
        val pendingTransaction = pendingTransactions.remove(trId)!!
    }

    fun rollbackPendingTransaction(trId: UUID) {
        val pendingTransaction = pendingTransactions.remove(trId)!!
        deposit(pendingTransaction.transferAmountFrozen) // refund
    }
}

data class PendingTransaction(
    val transactionId: UUID,
    val transferAmountFrozen: BigDecimal,
)
