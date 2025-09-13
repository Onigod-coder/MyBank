import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class ReportGenerator(private val bankingService: BankingService) {
    
    fun generateCurrentAccountsReport(clientId: Int): String {
        val accounts = bankingService.getClientCurrentAccounts(clientId)
        
        if (accounts.isEmpty()) {
            return "У клиента нет текущих счетов"
        }
        
        val report = StringBuilder()
        report.appendLine("=== ОТЧЕТ ПО ТЕКУЩИМ СЧЕТАМ ===")
        report.appendLine("Дата формирования: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
        report.appendLine()
        
        accounts.forEachIndexed { index, account ->
            val bank = bankingService.getBanks().find { it.id == account.bankId }
            report.appendLine("${index + 1}. Счет №${account.id}")
            report.appendLine("   Банк: ${bank?.shortName ?: "Неизвестно"}")
            report.appendLine("   Остаток: ${account.balance.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
            report.appendLine("   Процентная ставка: ${account.interestRate}% годовых")
            report.appendLine("   Дата открытия: ${account.createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
            report.appendLine()
        }
        
        val totalBalance = accounts.sumOf { it.balance }
        report.appendLine("Общий остаток по всем счетам: ${totalBalance.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        
        return report.toString()
    }
    
    fun generateDepositAccountsReport(clientId: Int): String {
        val accounts = bankingService.getClientDepositAccounts(clientId)
        
        if (accounts.isEmpty()) {
            return "У клиента нет депозитных счетов"
        }
        
        val report = StringBuilder()
        report.appendLine("=== ОТЧЕТ ПО ДЕПОЗИТНЫМ СЧЕТАМ ===")
        report.appendLine("Дата формирования: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
        report.appendLine()
        
        accounts.forEachIndexed { index, account ->
            val bank = bankingService.getBanks().find { it.id == account.bankId }
            report.appendLine("${index + 1}. Депозит №${account.id}")
            report.appendLine("   Банк: ${bank?.shortName ?: "Неизвестно"}")
            report.appendLine("   Сумма: ${account.balance.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
            report.appendLine("   Процентная ставка: ${account.interestRate}% годовых")
            report.appendLine("   Срок: ${account.termMonths} месяцев")
            report.appendLine("   Дата открытия: ${account.createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
            report.appendLine("   Дата окончания: ${account.endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
            report.appendLine("   Минимальный остаток: ${account.minBalance.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
            report.appendLine("   Пополняемый: ${if (account.isReplenishable) "Да" else "Нет"}")
            report.appendLine("   Снятие разрешено: ${if (account.isWithdrawable) "Да" else "Нет"}")
            report.appendLine("   Ежедневная капитализация: ${if (account.isDailyCapitalization) "Да" else "Нет"}")
            report.appendLine("   Пролонгируемый: ${if (account.isRenewable) "Да" else "Нет"}")
            report.appendLine("   Статус: ${if (account.isExpired()) "Истек" else "Действует"}")
            report.appendLine()
        }
        
        val totalBalance = accounts.sumOf { it.balance }
        report.appendLine("Общая сумма депозитов: ${totalBalance.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        
        return report.toString()
    }
    
    fun generateCreditAccountsReport(clientId: Int): String {
        val accounts = bankingService.getClientCreditAccounts(clientId)
        
        if (accounts.isEmpty()) {
            return "У клиента нет кредитных счетов"
        }
        
        val report = StringBuilder()
        report.appendLine("=== ОТЧЕТ ПО КРЕДИТНЫМ СЧЕТАМ ===")
        report.appendLine("Дата формирования: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
        report.appendLine()
        
        accounts.forEachIndexed { index, account ->
            val bank = bankingService.getBanks().find { it.id == account.bankId }
            report.appendLine("${index + 1}. Кредит №${account.id}")
            report.appendLine("   Банк: ${bank?.shortName ?: "Неизвестно"}")
            report.appendLine("   Сумма кредита: ${account.amount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
            report.appendLine("   Остаток к погашению: ${account.remainingAmount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
            report.appendLine("   Процентная ставка: ${account.interestRate}% годовых")
            report.appendLine("   Срок: ${account.termMonths} месяцев")
            report.appendLine("   Дата выдачи: ${account.startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
            report.appendLine("   Ежемесячный платеж: ${account.calculateMonthlyPayment().setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
            report.appendLine("   Статус: ${if (account.isFullyPaid()) "Погашен" else "Действует"}")
            report.appendLine()
        }
        
        val totalRemaining = accounts.sumOf { it.remainingAmount }
        report.appendLine("Общий остаток к погашению: ${totalRemaining.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        
        return report.toString()
    }
    
    fun generateDepositIncomeEstimate(
        bankId: Int,
        amount: BigDecimal,
        termMonths: Int,
        interestRate: BigDecimal,
        isDailyCapitalization: Boolean
    ): String {
        val income = bankingService.getDepositIncomeEstimate(bankId, amount, termMonths, interestRate, isDailyCapitalization)
        
        if (income == null) {
            return "Банк не найден"
        }
        
        val totalAmount = amount.add(income)
        val bank = bankingService.getBanks().find { it.id == bankId }
        
        val report = StringBuilder()
        report.appendLine("=== ПРЕДВАРИТЕЛЬНЫЙ РАСЧЕТ ДОХОДА ПО ДЕПОЗИТУ ===")
        report.appendLine("Банк: ${bank?.shortName ?: "Неизвестно"}")
        report.appendLine("Сумма вклада: ${amount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Срок: $termMonths месяцев")
        report.appendLine("Процентная ставка: $interestRate% годовых")
        report.appendLine("Капитализация: ${if (isDailyCapitalization) "Ежедневная" else "Ежемесячная"}")
        report.appendLine()
        report.appendLine("Предполагаемый доход: ${income.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Итоговая сумма: ${totalAmount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        
        return report.toString()
    }
    
    fun generateCreditOverpaymentEstimate(
        bankId: Int,
        amount: BigDecimal,
        termMonths: Int,
        interestRate: BigDecimal
    ): String {
        val overpayment = bankingService.getCreditOverpaymentEstimate(bankId, amount, termMonths, interestRate)
        
        if (overpayment == null) {
            return "Банк не найден"
        }
        
        val totalAmount = amount.add(overpayment)
        val monthlyRate = interestRate.divide(BigDecimal("100")).divide(BigDecimal("12"), 10, java.math.RoundingMode.HALF_UP)
        val monthlyPayment = if (monthlyRate == BigDecimal.ZERO) {
            amount.divide(BigDecimal(termMonths), 10, java.math.RoundingMode.HALF_UP)
        } else {
            try {
                val onePlusRate = BigDecimal.ONE.add(monthlyRate)
                val powerResult = onePlusRate.pow(-termMonths)
                val discountFactor = BigDecimal.ONE.subtract(powerResult)
                if (discountFactor == BigDecimal.ZERO) {
                    amount.divide(BigDecimal(termMonths), 10, java.math.RoundingMode.HALF_UP)
                } else {
                    amount.multiply(monthlyRate).divide(discountFactor, 10, java.math.RoundingMode.HALF_UP)
                }
            } catch (e: Exception) {
                // Если произошла ошибка при расчете, используем простое деление
                amount.divide(BigDecimal(termMonths), 10, java.math.RoundingMode.HALF_UP)
            }
        }
        
        val bank = bankingService.getBanks().find { it.id == bankId }
        
        val report = StringBuilder()
        report.appendLine("=== ПРЕДВАРИТЕЛЬНЫЙ РАСЧЕТ ПЕРЕПЛАТЫ ПО КРЕДИТУ ===")
        report.appendLine("Банк: ${bank?.shortName ?: "Неизвестно"}")
        report.appendLine("Сумма кредита: ${amount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Срок: $termMonths месяцев")
        report.appendLine("Процентная ставка: $interestRate% годовых")
        report.appendLine()
        report.appendLine("Ежемесячный платеж: ${monthlyPayment.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Общая сумма к возврату: ${totalAmount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Переплата: ${overpayment.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        
        return report.toString()
    }
    
    fun generateCreditSchedule(
        bankId: Int,
        amount: BigDecimal,
        termMonths: Int,
        interestRate: BigDecimal,
        startDate: LocalDate
    ): String {
        val schedule = bankingService.getCreditSchedule(bankId, amount, termMonths, interestRate, startDate)
        
        if (schedule == null) {
            return "Банк не найден"
        }
        
        val bank = bankingService.getBanks().find { it.id == bankId }
        
        val report = StringBuilder()
        report.appendLine("=== ГРАФИК ПОГАШЕНИЯ КРЕДИТА ===")
        report.appendLine("Банк: ${bank?.shortName ?: "Неизвестно"}")
        report.appendLine("Сумма кредита: ${amount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Срок: $termMonths месяцев")
        report.appendLine("Процентная ставка: $interestRate% годовых")
        report.appendLine("Дата выдачи: ${startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
        report.appendLine()
        
        report.appendLine("№ | Дата платежа | Ежемесячный платеж | Основной долг | Проценты | Остаток")
        report.appendLine("--|--------------|-------------------|---------------|----------|--------")
        
        schedule.forEach { payment ->
            report.appendLine(
                "${payment.paymentNumber.toString().padEnd(2)}| " +
                "${payment.paymentDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")).padEnd(12)}| " +
                "${payment.monthlyPayment.setScale(2, java.math.RoundingMode.HALF_UP).toString().padEnd(17)}| " +
                "${payment.principalPayment.setScale(2, java.math.RoundingMode.HALF_UP).toString().padEnd(13)}| " +
                "${payment.interestPayment.setScale(2, java.math.RoundingMode.HALF_UP).toString().padEnd(8)}| " +
                "${payment.remainingAmount.setScale(2, java.math.RoundingMode.HALF_UP)}"
            )
        }
        
        return report.toString()
    }
    
    fun generatePartialPaymentEstimate(
        creditAccountId: Int,
        paymentAmount: BigDecimal
    ): String {
        val account = bankingService.getBanks().flatMap { it.accounts.values }
            .filterIsInstance<CreditAccount>()
            .find { it.id == creditAccountId }
        
        if (account == null) {
            return "Кредитный счет не найден"
        }
        
        val interestAmount = account.calculateInterestForPeriod()
        val principalAmount = paymentAmount.subtract(interestAmount)
        val newRemainingAmount = account.remainingAmount.subtract(principalAmount)
        
        val report = StringBuilder()
        report.appendLine("=== РАСЧЕТ ЧАСТИЧНОГО ДОСРОЧНОГО ПОГАШЕНИЯ ===")
        report.appendLine("Сумма платежа: ${paymentAmount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Проценты к погашению: ${interestAmount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Основной долг к погашению: ${principalAmount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Остаток после платежа: ${newRemainingAmount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        
        if (newRemainingAmount > BigDecimal.ZERO) {
            val remainingMonths = account.termMonths - ChronoUnit.MONTHS.between(account.startDate, LocalDate.now()).toInt()
            if (remainingMonths > 0) {
                val newMonthlyPayment = newRemainingAmount.multiply(account.monthlyRate)
                    .divide(BigDecimal.ONE.subtract(BigDecimal.ONE.add(account.monthlyRate).pow(-remainingMonths)), 10, java.math.RoundingMode.HALF_UP)
                report.appendLine("Новый ежемесячный платеж: ${newMonthlyPayment.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
            }
        }
        
        return report.toString()
    }
    
    fun generateFullPaymentEstimate(creditAccountId: Int): String {
        val account = bankingService.getBanks().flatMap { it.accounts.values }
            .filterIsInstance<CreditAccount>()
            .find { it.id == creditAccountId }
        
        if (account == null) {
            return "Кредитный счет не найден"
        }
        
        val interestAmount = account.calculateInterestForPeriod()
        val totalPayment = account.remainingAmount.add(interestAmount)
        
        val report = StringBuilder()
        report.appendLine("=== РАСЧЕТ ПОЛНОГО ДОСРОЧНОГО ПОГАШЕНИЯ ===")
        report.appendLine("Остаток основного долга: ${account.remainingAmount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Проценты за период: ${interestAmount.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        report.appendLine("Итого к доплате: ${totalPayment.setScale(2, java.math.RoundingMode.HALF_UP)} руб.")
        
        return report.toString()
    }
}
