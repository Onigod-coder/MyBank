import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

class BankingService {
    private val banks = mutableMapOf<Int, Bank>()
    private val clients = mutableMapOf<Int, PhysicalPerson>()
    private var nextBankId = 1
    private var nextClientId = 1
    private var nextAccountId = 1
    
    fun createBank(fullName: String, shortName: String, currentAccountInterestRate: BigDecimal = BigDecimal("0.5")): Bank {
        val bank = Bank(id = nextBankId++, fullName = fullName, shortName = shortName, currentAccountInterestRate = currentAccountInterestRate)
        banks[bank.id] = bank
        return bank
    }
    
    fun createClient(fullName: String, inn: String, passportNumber: String, passportSeries: String): PhysicalPerson {
        val client = PhysicalPerson(
            id = nextClientId++,
            fullName = fullName,
            inn = inn,
            passportNumber = passportNumber,
            passportSeries = passportSeries
        )
        clients[client.id] = client
        return client
    }
    
    fun openCurrentAccount(bankId: Int, clientId: Int): CurrentAccount? {
        val bank = banks[bankId] ?: return null
        val client = clients[clientId] ?: return null
        
        bank.addClient(client)
        val account = bank.createCurrentAccount(nextAccountId++, client)
        client.addCurrentAccount(account.id)
        return account
    }
    
    fun openDepositAccount(
        bankId: Int,
        clientId: Int,
        amount: BigDecimal,
        termMonths: Int,
        minBalance: BigDecimal,
        isReplenishable: Boolean,
        isWithdrawable: Boolean,
        interestRate: BigDecimal,
        isDailyCapitalization: Boolean,
        isRenewable: Boolean
    ): DepositAccount? {
        val bank = banks[bankId] ?: return null
        val client = clients[clientId] ?: return null
        
        // Проверяем, что у клиента есть текущий счет для перевода средств
        val hasCurrentAccount = bank.accounts.values
            .filterIsInstance<CurrentAccount>()
            .any { it.clientId == clientId && it.balance >= amount }
        
        require(hasCurrentAccount) { "Для открытия депозита необходим текущий счет с достаточным балансом" }
        
        bank.addClient(client)
        val account = bank.createDepositAccount(
            id = nextAccountId++,
            client = client,
            amount = amount,
            termMonths = termMonths,
            minBalance = minBalance,
            isReplenishable = isReplenishable,
            isWithdrawable = isWithdrawable,
            interestRate = interestRate,
            isDailyCapitalization = isDailyCapitalization,
            isRenewable = isRenewable
        )
        client.addDepositAccount(account.id)
        return account
    }
    
    fun openCreditAccount(
        bankId: Int,
        clientId: Int,
        amount: BigDecimal,
        interestRate: BigDecimal,
        termMonths: Int,
        startDate: LocalDate = LocalDate.now()
    ): CreditAccount? {
        val bank = banks[bankId] ?: return null
        val client = clients[clientId] ?: return null
        
        bank.addClient(client)
        val account = bank.createCreditAccount(
            id = nextAccountId++,
            client = client,
            amount = amount,
            interestRate = interestRate,
            termMonths = termMonths,
            startDate = startDate
        )
        client.addCreditAccount(account.id)
        return account
    }
    
    fun transferBetweenAccounts(fromAccountId: Int, toAccountId: Int, amount: BigDecimal): Boolean {
        val fromAccount = findAccountById(fromAccountId) ?: return false
        val toAccount = findAccountById(toAccountId) ?: return false
        
        return fromAccount.transfer(toAccount, amount)
    }
    
    fun depositToAccount(accountId: Int, amount: BigDecimal): Boolean {
        val account = findAccountById(accountId) ?: return false
        account.deposit(amount)
        return true
    }
    
    fun withdrawFromAccount(accountId: Int, amount: BigDecimal): Boolean {
        val account = findAccountById(accountId) ?: return false
        return account.withdraw(amount)
    }
    
    fun makeCreditPayment(creditAccountId: Int, paymentAmount: BigDecimal): CreditPaymentResult? {
        val account = findAccountById(creditAccountId) as? CreditAccount ?: return null
        return account.makePayment(paymentAmount)
    }
    
    fun processInterestAccrual() {
        banks.values.forEach { bank ->
            bank.accounts.values.forEach { account ->
                when (account) {
                    is CurrentAccount -> {
                        val interest = account.calculateInterest()
                        if (interest > BigDecimal.ZERO) {
                            account.deposit(interest)
                        }
                    }
                    is DepositAccount -> {
                        account.addInterest()
                    }
                }
            }
        }
    }
    
    fun processDepositExpiration() {
        banks.values.forEach { bank ->
            val expiredDeposits = bank.accounts.values
                .filterIsInstance<DepositAccount>()
                .filter { it.isExpired() }
            
            expiredDeposits.forEach { deposit ->
                val client = clients[deposit.clientId] ?: return@forEach
                
                if (deposit.isRenewable) {
                    // Автоматическое продление
                    deposit.addInterest()
                } else {
                    // Перевод на текущий счет или создание нового
                    val currentAccounts = bank.accounts.values
                        .filterIsInstance<CurrentAccount>()
                        .filter { it.clientId == client.id }
                    
                    if (currentAccounts.isNotEmpty()) {
                        // Переводим на первый доступный текущий счет
                        val targetAccount = currentAccounts.first()
                        deposit.transfer(targetAccount, deposit.balance)
                    } else {
                        // Создаем новый текущий счет
                        val newCurrentAccount = bank.createCurrentAccount(nextAccountId++, client)
                        client.addCurrentAccount(newCurrentAccount.id)
                        deposit.transfer(newCurrentAccount, deposit.balance)
                    }
                }
            }
        }
    }
    
    fun getClientCurrentAccounts(clientId: Int): List<CurrentAccount> {
        return banks.values.flatMap { bank ->
            bank.accounts.values.filterIsInstance<CurrentAccount>()
                .filter { it.clientId == clientId }
        }
    }
    
    fun getClientDepositAccounts(clientId: Int): List<DepositAccount> {
        return banks.values.flatMap { bank ->
            bank.accounts.values.filterIsInstance<DepositAccount>()
                .filter { it.clientId == clientId }
        }
    }
    
    fun getClientCreditAccounts(clientId: Int): List<CreditAccount> {
        return banks.values.flatMap { bank ->
            bank.accounts.values.filterIsInstance<CreditAccount>()
                .filter { it.clientId == clientId }
        }
    }
    
    fun getDepositIncomeEstimate(
        bankId: Int,
        amount: BigDecimal,
        termMonths: Int,
        interestRate: BigDecimal,
        isDailyCapitalization: Boolean
    ): BigDecimal? {
        val bank = banks[bankId] ?: return null
        return bank.calculateDepositIncome(amount, termMonths, interestRate, isDailyCapitalization)
    }
    
    fun getCreditOverpaymentEstimate(
        bankId: Int,
        amount: BigDecimal,
        termMonths: Int,
        interestRate: BigDecimal
    ): BigDecimal? {
        val bank = banks[bankId] ?: return null
        return bank.calculateCreditOverpayment(amount, termMonths, interestRate)
    }
    
    fun getCreditSchedule(
        bankId: Int,
        amount: BigDecimal,
        termMonths: Int,
        interestRate: BigDecimal,
        startDate: LocalDate
    ): List<CreditPayment>? {
        val bank = banks[bankId] ?: return null
        return bank.generateCreditSchedule(amount, termMonths, interestRate, startDate)
    }
    
    fun getBanks(): List<Bank> = banks.values.toList()
    fun getClients(): List<PhysicalPerson> = clients.values.toList()
    
    private fun findAccountById(accountId: Int): BankAccount? {
        return banks.values.flatMap { it.accounts.values }
            .firstOrNull { it.id == accountId }
    }
}
