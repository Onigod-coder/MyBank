import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

abstract class BankAccount(
    val id: Int,
    val bankId: Int,
    val clientId: Int,
    var balance: BigDecimal = BigDecimal.ZERO,
    val createdAt: LocalDate = LocalDate.now()
) {
    abstract val accountType: String
    
    open fun deposit(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "Сумма пополнения должна быть положительной" }
        balance = balance.add(amount)
    }
    
    open fun withdraw(amount: BigDecimal): Boolean {
        require(amount > BigDecimal.ZERO) { "Сумма снятия должна быть положительной" }
        if (balance >= amount) {
            balance = balance.subtract(amount)
            return true
        }
        return false
    }
    
    fun transfer(toAccount: BankAccount, amount: BigDecimal): Boolean {
        if (withdraw(amount)) {
            toAccount.deposit(amount)
            return true
        }
        return false
    }
}

class CurrentAccount(
    id: Int,
    bankId: Int,
    clientId: Int,
    val interestRate: BigDecimal
) : BankAccount(id = id, bankId = bankId, clientId = clientId) {
    override val accountType = "Текущий"
    var lastInterestDate: LocalDate = LocalDate.now()
    var minBalanceInPeriod: BigDecimal = BigDecimal.ZERO
    
    fun calculateInterest(): BigDecimal {
        val now = LocalDate.now()
        val daysInMonth = ChronoUnit.DAYS.between(lastInterestDate, now)
        
        if (daysInMonth >= 30) {
            val interest = minBalanceInPeriod
                .multiply(interestRate)
                .divide(BigDecimal("100"))
                .divide(BigDecimal("12"), 10, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP)
            
            lastInterestDate = now
            minBalanceInPeriod = balance
            return interest
        }
        return BigDecimal.ZERO
    }
    
    override fun deposit(amount: BigDecimal) {
        super.deposit(amount)
        if (minBalanceInPeriod == BigDecimal.ZERO || balance < minBalanceInPeriod) {
            minBalanceInPeriod = balance
        }
    }
    
    override fun withdraw(amount: BigDecimal): Boolean {
        val result = super.withdraw(amount)
        if (result && balance < minBalanceInPeriod) {
            minBalanceInPeriod = balance
        }
        return result
    }
}

class DepositAccount(
    id: Int,
    bankId: Int,
    clientId: Int,
    amount: BigDecimal,
    val termMonths: Int,
    val minBalance: BigDecimal,
    val isReplenishable: Boolean,
    val isWithdrawable: Boolean,
    val interestRate: BigDecimal,
    val isDailyCapitalization: Boolean,
    val isRenewable: Boolean
) : BankAccount(id = id, bankId = bankId, clientId = clientId, balance = amount) {
    override val accountType = "Депозитный"
    val endDate: LocalDate = createdAt.plusMonths(termMonths.toLong())
    var lastInterestDate: LocalDate = createdAt
    var accumulatedInterest: BigDecimal = BigDecimal.ZERO
    
    init {
        require(amount >= minBalance) { "Начальная сумма должна быть не менее минимального остатка" }
    }
    
    fun calculateDailyInterest(): BigDecimal {
        val dailyRate = interestRate.divide(BigDecimal("100")).divide(BigDecimal("365"), 10, RoundingMode.HALF_UP)
        return balance.multiply(dailyRate).setScale(10, RoundingMode.HALF_UP)
    }
    
    fun calculateMonthlyInterest(): BigDecimal {
        val monthlyRate = interestRate.divide(BigDecimal("100")).divide(BigDecimal("12"), 10, RoundingMode.HALF_UP)
        return balance.multiply(monthlyRate).setScale(10, RoundingMode.HALF_UP)
    }
    
    fun addInterest() {
        val now = LocalDate.now()
        
        if (isDailyCapitalization) {
            val days = ChronoUnit.DAYS.between(lastInterestDate, now)
            if (days > 0) {
                val dailyInterest = calculateDailyInterest()
                val totalInterest = dailyInterest.multiply(BigDecimal(days))
                balance = balance.add(totalInterest)
                lastInterestDate = now
            }
        } else {
            val months = ChronoUnit.MONTHS.between(lastInterestDate.withDayOfMonth(1), now.withDayOfMonth(1))
            if (months > 0) {
                val monthlyInterest = calculateMonthlyInterest()
                accumulatedInterest = accumulatedInterest.add(monthlyInterest.multiply(BigDecimal(months)))
                lastInterestDate = now.withDayOfMonth(1)
            }
        }
    }
    
    fun isExpired(): Boolean = LocalDate.now().isAfter(endDate)
    
    fun canWithdraw(amount: BigDecimal): Boolean {
        if (!isWithdrawable) return false
        if (balance.subtract(amount) < minBalance) return false
        return true
    }
    
    override fun deposit(amount: BigDecimal) {
        require(isReplenishable) { "Депозит не пополняемый" }
        super.deposit(amount)
    }
    
    override fun withdraw(amount: BigDecimal): Boolean {
        if (!canWithdraw(amount)) {
            if (!isWithdrawable) {
                // Если запрещено снятие, пересчитываем по ставке текущего счета
                return false
            }
            return false
        }
        return super.withdraw(amount)
    }
}

class CreditAccount(
    id: Int,
    bankId: Int,
    clientId: Int,
    val amount: BigDecimal,
    val interestRate: BigDecimal,
    val termMonths: Int,
    val startDate: LocalDate
) : BankAccount(id = id, bankId = bankId, clientId = clientId, balance = amount) {
    override val accountType = "Кредитный"
    var remainingAmount: BigDecimal = amount
    var lastPaymentDate: LocalDate = startDate
    val monthlyRate: BigDecimal = interestRate.divide(BigDecimal("100")).divide(BigDecimal("12"), 10, RoundingMode.HALF_UP)
    
    fun calculateMonthlyPayment(): BigDecimal {
        return if (monthlyRate == BigDecimal.ZERO) {
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
    }
    
    fun calculateInterestForPeriod(): BigDecimal {
        val days = ChronoUnit.DAYS.between(lastPaymentDate, LocalDate.now())
        return remainingAmount.multiply(interestRate)
            .divide(BigDecimal("100"))
            .multiply(BigDecimal(days))
            .divide(BigDecimal("365"), 10, RoundingMode.HALF_UP)
    }
    
    fun makePayment(paymentAmount: BigDecimal): CreditPaymentResult {
        val interestAmount = calculateInterestForPeriod()
        val principalAmount = paymentAmount.subtract(interestAmount)
        
        if (principalAmount < BigDecimal.ZERO) {
            return CreditPaymentResult(
                success = false,
                message = "Недостаточно средств для погашения процентов"
            )
        }
        
        remainingAmount = remainingAmount.subtract(principalAmount)
        lastPaymentDate = LocalDate.now()
        
        if (remainingAmount <= BigDecimal.ZERO) {
            remainingAmount = BigDecimal.ZERO
            return CreditPaymentResult(
                success = true,
                message = "Кредит полностью погашен",
                remainingAmount = BigDecimal.ZERO
            )
        }
        
        return CreditPaymentResult(
            success = true,
            message = "Платеж принят",
            remainingAmount = remainingAmount
        )
    }
    
    fun isFullyPaid(): Boolean = remainingAmount <= BigDecimal.ZERO
}

data class CreditPayment(
    val paymentNumber: Int,
    val paymentDate: LocalDate,
    val monthlyPayment: BigDecimal,
    val principalPayment: BigDecimal,
    val interestPayment: BigDecimal,
    val remainingAmount: BigDecimal
)

data class CreditPaymentResult(
    val success: Boolean,
    val message: String,
    val remainingAmount: BigDecimal = BigDecimal.ZERO
)
