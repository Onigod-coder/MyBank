import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

data class Bank(
    val id: Int,
    val fullName: String,
    val shortName: String,
    val currentAccountInterestRate: BigDecimal = BigDecimal("0.5") // 0.5% по умолчанию
) {
    init {
        require(currentAccountInterestRate in BigDecimal("0.1")..BigDecimal("2.0")) {
            "Процент по текущему счету должен быть от 0.1% до 2%"
        }
    }
    
    val accounts = mutableMapOf<Int, BankAccount>()
    val clients = mutableSetOf<PhysicalPerson>()
    
    fun addClient(client: PhysicalPerson) {
        clients.add(client)
    }
    
    fun createCurrentAccount(id: Int, client: PhysicalPerson): CurrentAccount {
        val clientAccounts = accounts.values.filterIsInstance<CurrentAccount>()
            .count { it.clientId == client.id }
        
        require(clientAccounts < 3) {
            "Физическое лицо может иметь не более 3 текущих счетов в одном банке"
        }
        
        val account = CurrentAccount(
            id = id,
            bankId = this.id,
            clientId = client.id,
            interestRate = currentAccountInterestRate
        )
        accounts[account.id] = account
        return account
    }
    
    fun createDepositAccount(
        id: Int,
        client: PhysicalPerson,
        amount: BigDecimal,
        termMonths: Int,
        minBalance: BigDecimal,
        isReplenishable: Boolean,
        isWithdrawable: Boolean,
        interestRate: BigDecimal,
        isDailyCapitalization: Boolean,
        isRenewable: Boolean
    ): DepositAccount {
        require(termMonths >= 3) {
            "Срок депозита должен быть не менее 3 месяцев"
        }
        
        require(interestRate in BigDecimal("18.0")..BigDecimal("25.0")) {
            "Процентная ставка по депозиту должна быть от 18% до 25%"
        }
        
        val clientDeposits = accounts.values.filterIsInstance<DepositAccount>()
            .count { it.clientId == client.id }
        
        require(clientDeposits < 1) {
            "Физическое лицо может иметь не более 1 депозитного счета в одном банке"
        }
        
        val account = DepositAccount(
            id = id,
            bankId = this.id,
            clientId = client.id,
            amount = amount,
            termMonths = termMonths,
            minBalance = minBalance,
            isReplenishable = isReplenishable,
            isWithdrawable = isWithdrawable,
            interestRate = interestRate,
            isDailyCapitalization = isDailyCapitalization,
            isRenewable = isRenewable
        )
        accounts[account.id] = account
        return account
    }
    
    fun createCreditAccount(
        id: Int,
        client: PhysicalPerson,
        amount: BigDecimal,
        interestRate: BigDecimal,
        termMonths: Int,
        startDate: LocalDate
    ): CreditAccount {
        val clientCredits = accounts.values.filterIsInstance<CreditAccount>()
            .count { it.clientId == client.id }
        
        require(clientCredits < 1) {
            "Физическое лицо может иметь не более 1 кредита в одном банке"
        }
        
        val account = CreditAccount(
            id = id,
            bankId = this.id,
            clientId = client.id,
            amount = amount,
            interestRate = interestRate,
            termMonths = termMonths,
            startDate = startDate
        )
        accounts[account.id] = account
        return account
    }
    
    fun calculateDepositIncome(
        amount: BigDecimal,
        termMonths: Int,
        interestRate: BigDecimal,
        isDailyCapitalization: Boolean
    ): BigDecimal {
        val annualRate = interestRate.divide(BigDecimal("100"))
        val monthlyRate = annualRate.divide(BigDecimal("12"), 10, RoundingMode.HALF_UP)
        
        return if (isDailyCapitalization) {
            val dailyRate = annualRate.divide(BigDecimal("365"), 10, RoundingMode.HALF_UP)
            val days = termMonths * 30L
            amount.multiply(BigDecimal.ONE.add(dailyRate).pow(days.toInt()))
                .subtract(amount)
                .setScale(2, RoundingMode.HALF_UP)
        } else {
            amount.multiply(monthlyRate).multiply(BigDecimal(termMonths))
                .setScale(2, RoundingMode.HALF_UP)
        }
    }
    
    fun calculateCreditOverpayment(
        amount: BigDecimal,
        termMonths: Int,
        interestRate: BigDecimal
    ): BigDecimal {
        val monthlyRate = interestRate.divide(BigDecimal("100")).divide(BigDecimal("12"), 10, RoundingMode.HALF_UP)
        val monthlyPayment = if (monthlyRate == BigDecimal.ZERO) {
            amount.divide(BigDecimal(termMonths), 10, RoundingMode.HALF_UP)
        } else {
            try {
                val onePlusRate = BigDecimal.ONE.add(monthlyRate)
                val powerResult = onePlusRate.pow(-termMonths)
                val discountFactor = BigDecimal.ONE.subtract(powerResult)
                if (discountFactor == BigDecimal.ZERO) {
                    amount.divide(BigDecimal(termMonths), 10, RoundingMode.HALF_UP)
                } else {
                    amount.multiply(monthlyRate).divide(discountFactor, 10, RoundingMode.HALF_UP)
                }
            } catch (e: Exception) {
                // Если произошла ошибка при расчете, используем простое деление
                amount.divide(BigDecimal(termMonths), 10, RoundingMode.HALF_UP)
            }
        }
        
        val totalPayments = monthlyPayment.multiply(BigDecimal(termMonths))
        return totalPayments.subtract(amount).setScale(2, RoundingMode.HALF_UP)
    }
    
    fun generateCreditSchedule(
        amount: BigDecimal,
        termMonths: Int,
        interestRate: BigDecimal,
        startDate: LocalDate
    ): List<CreditPayment> {
        val monthlyRate = interestRate.divide(BigDecimal("100")).divide(BigDecimal("12"), 10, RoundingMode.HALF_UP)
        val monthlyPayment = if (monthlyRate == BigDecimal.ZERO) {
            amount.divide(BigDecimal(termMonths), 10, RoundingMode.HALF_UP)
        } else {
            try {
                val onePlusRate = BigDecimal.ONE.add(monthlyRate)
                val powerResult = onePlusRate.pow(-termMonths)
                val discountFactor = BigDecimal.ONE.subtract(powerResult)
                if (discountFactor == BigDecimal.ZERO) {
                    amount.divide(BigDecimal(termMonths), 10, RoundingMode.HALF_UP)
                } else {
                    amount.multiply(monthlyRate).divide(discountFactor, 10, RoundingMode.HALF_UP)
                }
            } catch (e: Exception) {
                // Если произошла ошибка при расчете, используем простое деление
                amount.divide(BigDecimal(termMonths), 10, RoundingMode.HALF_UP)
            }
        }
        
        val schedule = mutableListOf<CreditPayment>()
        var remainingAmount = amount
        
        for (i in 1..termMonths) {
            val interestPayment = remainingAmount.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP)
            val principalPayment = monthlyPayment.subtract(interestPayment)
            remainingAmount = remainingAmount.subtract(principalPayment)
            
            schedule.add(CreditPayment(
                paymentNumber = i,
                paymentDate = startDate.plusMonths(i.toLong()),
                monthlyPayment = monthlyPayment.setScale(2, RoundingMode.HALF_UP),
                principalPayment = principalPayment.setScale(2, RoundingMode.HALF_UP),
                interestPayment = interestPayment,
                remainingAmount = remainingAmount.setScale(2, RoundingMode.HALF_UP)
            ))
        }
        
        return schedule
    }
}
