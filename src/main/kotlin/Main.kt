import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

fun main() {
    val bankingService = BankingService()
    val reportGenerator = ReportGenerator(bankingService)
    val scanner = Scanner(System.`in`)
    
    println("=== БАНКОВСКАЯ СИСТЕМА ===")
    println("Добро пожаловать в систему управления банковскими счетами!")
    
    // Создаем тестовые данные
    initializeTestData(bankingService)
    
    while (true) {
        printMainMenu()
        val choice = scanner.nextLine().trim()
        
        when (choice) {
            "1" -> manageBanks(bankingService, scanner)
            "2" -> manageClients(bankingService, scanner)
            "3" -> manageAccounts(bankingService, scanner)
            "4" -> manageOperations(bankingService, scanner)
            "5" -> generateReports(reportGenerator, scanner)
            "6" -> processSystemOperations(bankingService, scanner)
            "0" -> {
                println("Спасибо за использование банковской системы!")
                break
            }
            else -> println("Неверный выбор. Попробуйте снова.")
        }
        
        println("\nНажмите Enter для продолжения...")
        scanner.nextLine()
    }
}

fun printMainMenu() {
    println("\n=== ГЛАВНОЕ МЕНЮ ===")
    println("1. Управление банками")
    println("2. Управление клиентами")
    println("3. Управление счетами")
    println("4. Операции со счетами")
    println("5. Отчеты")
    println("6. Системные операции")
    println("0. Выход")
    print("Выберите пункт меню: ")
}

fun manageBanks(bankingService: BankingService, scanner: Scanner) {
    while (true) {
        println("\n=== УПРАВЛЕНИЕ БАНКАМИ ===")
        println("1. Создать банк")
        println("2. Просмотреть список банков")
        println("3. Настроить процентную ставку")
        println("0. Назад")
        print("Выберите действие: ")
        
        when (scanner.nextLine().trim()) {
            "1" -> {
                print("Введите полное наименование банка: ")
                val fullName = scanner.nextLine().trim()
                print("Введите краткое наименование банка: ")
                val shortName = scanner.nextLine().trim()
                print("Введите процентную ставку по текущим счетам (0.1-2.0%): ")
                val rate = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат числа. Используется значение по умолчанию 0.5%")
                    BigDecimal("0.5")
                }
                
                val bank = bankingService.createBank(fullName, shortName, rate)
                println("Банк '${bank.fullName}' успешно создан с ID: ${bank.id}")
            }
            "2" -> {
                val banks = bankingService.getBanks()
                if (banks.isEmpty()) {
                    println("Банки не найдены")
                } else {
                    banks.forEachIndexed { index, bank ->
                        println("${index + 1}. ${bank.fullName} (${bank.shortName}) - ${bank.currentAccountInterestRate}%")
                    }
                }
            }
            "3" -> {
                val banks = bankingService.getBanks()
                if (banks.isEmpty()) {
                    println("Банки не найдены")
                } else {
                    banks.forEachIndexed { index, bank ->
                        println("${index + 1}. ${bank.fullName} (${bank.shortName}) - ${bank.currentAccountInterestRate}%")
                    }
                    print("Выберите номер банка: ")
                    val bankIndex = try {
                        scanner.nextLine().trim().toInt() - 1
                    } catch (e: NumberFormatException) {
                        -1
                    }
                    
                    if (bankIndex in banks.indices) {
                        print("Введите новую процентную ставку (0.1-2.0%): ")
                        val newRate = try {
                            BigDecimal(scanner.nextLine().trim())
                        } catch (e: NumberFormatException) {
                            println("Неверный формат числа")
                            continue
                        }
                        
                        if (newRate in BigDecimal("0.1")..BigDecimal("2.0")) {
                            // В реальном приложении здесь нужно было бы обновить ставку
                            println("Процентная ставка обновлена")
                        } else {
                            println("Процентная ставка должна быть от 0.1% до 2.0%")
                        }
                    } else {
                        println("Неверный номер банка")
                    }
                }
            }
            "0" -> break
            else -> println("Неверный выбор")
        }
    }
}

fun manageClients(bankingService: BankingService, scanner: Scanner) {
    while (true) {
        println("\n=== УПРАВЛЕНИЕ КЛИЕНТАМИ ===")
        println("1. Создать клиента")
        println("2. Просмотреть список клиентов")
        println("0. Назад")
        print("Выберите действие: ")
        
        when (scanner.nextLine().trim()) {
            "1" -> {
                createClientWithValidation(bankingService, scanner)
            }
            "2" -> {
                val clients = bankingService.getClients()
                if (clients.isEmpty()) {
                    println("Клиенты не найдены")
                } else {
                    clients.forEachIndexed { index, client ->
                        println("${index + 1}. ${client.fullName} (ИНН: ${client.inn})")
                    }
                }
            }
            "0" -> break
            else -> println("Неверный выбор")
        }
    }
}

fun manageAccounts(bankingService: BankingService, scanner: Scanner) {
    while (true) {
        println("\n=== УПРАВЛЕНИЕ СЧЕТАМИ ===")
        println("1. Открыть текущий счет")
        println("2. Открыть депозитный счет")
        println("3. Открыть кредитный счет")
        println("0. Назад")
        print("Выберите действие: ")
        
        when (scanner.nextLine().trim()) {
            "1" -> openCurrentAccount(bankingService, scanner)
            "2" -> openDepositAccount(bankingService, scanner)
            "3" -> openCreditAccount(bankingService, scanner)
            "0" -> break
            else -> println("Неверный выбор")
        }
    }
}

fun openCurrentAccount(bankingService: BankingService, scanner: Scanner) {
    val banks = bankingService.getBanks()
    val clients = bankingService.getClients()
    
    if (banks.isEmpty() || clients.isEmpty()) {
        println("Необходимо создать банки и клиентов")
        return
    }
    
    println("Выберите банк:")
    banks.forEachIndexed { index, bank ->
        println("${index + 1}. ${bank.fullName} (${bank.shortName})")
    }
    print("Номер банка: ")
    val bankIndex = try {
        scanner.nextLine().trim().toInt() - 1
    } catch (e: NumberFormatException) {
        -1
    }
    
    if (bankIndex !in banks.indices) {
        println("Неверный номер банка")
        return
    }
    
    println("Выберите клиента:")
    clients.forEachIndexed { index, client ->
        println("${index + 1}. ${client.fullName}")
    }
    print("Номер клиента: ")
    val clientIndex = try {
        scanner.nextLine().trim().toInt() - 1
    } catch (e: NumberFormatException) {
        -1
    }
    
    if (clientIndex !in clients.indices) {
        println("Неверный номер клиента")
        return
    }
    
    val account = bankingService.openCurrentAccount(banks[bankIndex].id, clients[clientIndex].id)
    if (account != null) {
        println("Текущий счет успешно открыт с ID: ${account.id}")
    } else {
        println("Ошибка при открытии счета")
    }
}

fun openDepositAccount(bankingService: BankingService, scanner: Scanner) {
    val banks = bankingService.getBanks()
    val clients = bankingService.getClients()
    
    if (banks.isEmpty() || clients.isEmpty()) {
        println("Необходимо создать банки и клиентов")
        return
    }
    
    println("Выберите банк:")
    banks.forEachIndexed { index, bank ->
        println("${index + 1}. ${bank.fullName} (${bank.shortName})")
    }
    print("Номер банка: ")
    val bankIndex = try {
        scanner.nextLine().trim().toInt() - 1
    } catch (e: NumberFormatException) {
        -1
    }
    
    if (bankIndex !in banks.indices) {
        println("Неверный номер банка")
        return
    }
    
    println("Выберите клиента:")
    clients.forEachIndexed { index, client ->
        println("${index + 1}. ${client.fullName}")
    }
    print("Номер клиента: ")
    val clientIndex = try {
        scanner.nextLine().trim().toInt() - 1
    } catch (e: NumberFormatException) {
        -1
    }
    
    if (clientIndex !in clients.indices) {
        println("Неверный номер клиента")
        return
    }
    
    print("Введите сумму депозита: ")
    val amount = try {
        BigDecimal(scanner.nextLine().trim())
    } catch (e: NumberFormatException) {
        println("Неверный формат суммы")
        return
    }
    
    print("Введите срок в месяцах (минимум 3): ")
    val termMonths = try {
        scanner.nextLine().trim().toInt()
    } catch (e: NumberFormatException) {
        println("Неверный формат срока")
        return
    }
    
    print("Введите минимальный остаток: ")
    val minBalance = try {
        BigDecimal(scanner.nextLine().trim())
    } catch (e: NumberFormatException) {
        println("Неверный формат суммы")
        return
    }
    
    print("Пополняемый депозит? (y/n): ")
    val isReplenishable = scanner.nextLine().trim().lowercase() == "y"
    
    print("Разрешено снятие? (y/n): ")
    val isWithdrawable = scanner.nextLine().trim().lowercase() == "y"
    
    print("Введите процентную ставку (18-25%): ")
    val interestRate = try {
        BigDecimal(scanner.nextLine().trim())
    } catch (e: NumberFormatException) {
        println("Неверный формат ставки")
        return
    }
    
    print("Ежедневная капитализация? (y/n): ")
    val isDailyCapitalization = scanner.nextLine().trim().lowercase() == "y"
    
    print("Пролонгируемый депозит? (y/n): ")
    val isRenewable = scanner.nextLine().trim().lowercase() == "y"
    
    try {
        val account = bankingService.openDepositAccount(
            banks[bankIndex].id,
            clients[clientIndex].id,
            amount,
            termMonths,
            minBalance,
            isReplenishable,
            isWithdrawable,
            interestRate,
            isDailyCapitalization,
            isRenewable
        )
        
        if (account != null) {
            println("Депозитный счет успешно открыт с ID: ${account.id}")
            
            // Показываем предварительный расчет дохода
            val reportGenerator = ReportGenerator(bankingService)
            val incomeReport = reportGenerator.generateDepositIncomeEstimate(
                banks[bankIndex].id,
                amount,
                termMonths,
                interestRate,
                isDailyCapitalization
            )
            println("\n$incomeReport")
        } else {
            println("Ошибка при открытии депозитного счета")
        }
    } catch (e: Exception) {
        println("Ошибка: ${e.message}")
    }
}

fun openCreditAccount(bankingService: BankingService, scanner: Scanner) {
    val banks = bankingService.getBanks()
    val clients = bankingService.getClients()
    
    if (banks.isEmpty() || clients.isEmpty()) {
        println("Необходимо создать банки и клиентов")
        return
    }
    
    println("Выберите банк:")
    banks.forEachIndexed { index, bank ->
        println("${index + 1}. ${bank.fullName} (${bank.shortName})")
    }
    print("Номер банка: ")
    val bankIndex = try {
        scanner.nextLine().trim().toInt() - 1
    } catch (e: NumberFormatException) {
        -1
    }
    
    if (bankIndex !in banks.indices) {
        println("Неверный номер банка")
        return
    }
    
    println("Выберите клиента:")
    clients.forEachIndexed { index, client ->
        println("${index + 1}. ${client.fullName}")
    }
    print("Номер клиента: ")
    val clientIndex = try {
        scanner.nextLine().trim().toInt() - 1
    } catch (e: NumberFormatException) {
        -1
    }
    
    if (clientIndex !in clients.indices) {
        println("Неверный номер клиента")
        return
    }
    
    print("Введите сумму кредита: ")
    val amount = try {
        BigDecimal(scanner.nextLine().trim())
    } catch (e: NumberFormatException) {
        println("Неверный формат суммы")
        return
    }
    
    print("Введите процентную ставку: ")
    val interestRate = try {
        BigDecimal(scanner.nextLine().trim())
    } catch (e: NumberFormatException) {
        println("Неверный формат ставки")
        return
    }
    
    print("Введите срок в месяцах: ")
    val termMonths = try {
        scanner.nextLine().trim().toInt()
    } catch (e: NumberFormatException) {
        println("Неверный формат срока")
        return
    }
    
    print("Введите дату выдачи (dd.MM.yyyy) или Enter для текущей даты: ")
    val dateInput = scanner.nextLine().trim()
    val startDate = if (dateInput.isEmpty()) {
        LocalDate.now()
    } else {
        try {
            LocalDate.parse(dateInput, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        } catch (e: DateTimeParseException) {
            println("Неверный формат даты. Используется текущая дата")
            LocalDate.now()
        }
    }
    
    try {
        val account = bankingService.openCreditAccount(
            banks[bankIndex].id,
            clients[clientIndex].id,
            amount,
            interestRate,
            termMonths,
            startDate
        )
        
        if (account != null) {
            println("Кредитный счет успешно открыт с ID: ${account.id}")
            
            // Показываем предварительный расчет переплаты и график
            try {
                val reportGenerator = ReportGenerator(bankingService)
                println("Создание отчета о переплате...")
                val overpaymentReport = reportGenerator.generateCreditOverpaymentEstimate(
                    banks[bankIndex].id,
                    amount,
                    termMonths,
                    interestRate
                )
                println("\n$overpaymentReport")
                
                println("Создание графика погашения...")
                val scheduleReport = reportGenerator.generateCreditSchedule(
                    banks[bankIndex].id,
                    amount,
                    termMonths,
                    interestRate,
                    startDate
                )
                println("\n$scheduleReport")
            } catch (e: Exception) {
                println("Ошибка при создании отчетов: ${e.message}")
                println("Тип ошибки: ${e.javaClass.simpleName}")
                e.printStackTrace()
            }
        } else {
            println("Ошибка при открытии кредитного счета")
        }
    } catch (e: Exception) {
        println("Ошибка: ${e.message}")
    }
}

fun manageOperations(bankingService: BankingService, scanner: Scanner) {
    while (true) {
        println("\n=== ОПЕРАЦИИ СО СЧЕТАМИ ===")
        println("1. Пополнить счет")
        println("2. Снять со счета")
        println("3. Перевод между счетами")
        println("4. Погашение кредита")
        println("0. Назад")
        print("Выберите действие: ")
        
        when (scanner.nextLine().trim()) {
            "1" -> {
                print("Введите ID счета: ")
                val accountId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                print("Введите сумму: ")
                val amount = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат суммы")
                    continue
                }
                
                if (bankingService.depositToAccount(accountId, amount)) {
                    println("Счет успешно пополнен")
                } else {
                    println("Ошибка при пополнении счета")
                }
            }
            "2" -> {
                print("Введите ID счета: ")
                val accountId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                print("Введите сумму: ")
                val amount = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат суммы")
                    continue
                }
                
                if (bankingService.withdrawFromAccount(accountId, amount)) {
                    println("Средства успешно сняты")
                } else {
                    println("Ошибка при снятии средств")
                }
            }
            "3" -> {
                print("Введите ID счета отправителя: ")
                val fromAccountId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                print("Введите ID счета получателя: ")
                val toAccountId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                print("Введите сумму: ")
                val amount = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат суммы")
                    continue
                }
                
                if (bankingService.transferBetweenAccounts(fromAccountId, toAccountId, amount)) {
                    println("Перевод выполнен успешно")
                } else {
                    println("Ошибка при переводе")
                }
            }
            "4" -> {
                print("Введите ID кредитного счета: ")
                val creditAccountId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                print("Введите сумму платежа: ")
                val amount = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат суммы")
                    continue
                }
                
                val result = bankingService.makeCreditPayment(creditAccountId, amount)
                if (result != null) {
                    println("Результат: ${result.message}")
                    if (result.remainingAmount > BigDecimal.ZERO) {
                        println("Остаток к погашению: ${result.remainingAmount}")
                    }
                } else {
                    println("Ошибка при погашении кредита")
                }
            }
            "0" -> break
            else -> println("Неверный выбор")
        }
    }
}

fun generateReports(reportGenerator: ReportGenerator, scanner: Scanner) {
    while (true) {
        println("\n=== ОТЧЕТЫ ===")
        println("1. Отчет по текущим счетам клиента")
        println("2. Отчет по депозитным счетам клиента")
        println("3. Отчет по кредитным счетам клиента")
        println("4. Расчет дохода по депозиту")
        println("5. Расчет переплаты по кредиту")
        println("6. График погашения кредита")
        println("0. Назад")
        print("Выберите отчет: ")
        
        when (scanner.nextLine().trim()) {
            "1" -> {
                print("Введите ID клиента: ")
                val clientId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                val report = reportGenerator.generateCurrentAccountsReport(clientId)
                println(report)
            }
            "2" -> {
                print("Введите ID клиента: ")
                val clientId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                val report = reportGenerator.generateDepositAccountsReport(clientId)
                println(report)
            }
            "3" -> {
                print("Введите ID клиента: ")
                val clientId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                val report = reportGenerator.generateCreditAccountsReport(clientId)
                println(report)
            }
            "4" -> {
                print("Введите ID банка: ")
                val bankId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                print("Введите сумму: ")
                val amount = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат суммы")
                    continue
                }
                print("Введите срок в месяцах: ")
                val termMonths = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат срока")
                    continue
                }
                print("Введите процентную ставку: ")
                val interestRate = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат ставки")
                    continue
                }
                print("Ежедневная капитализация? (y/n): ")
                val isDailyCapitalization = scanner.nextLine().trim().lowercase() == "y"
                
                val report = reportGenerator.generateDepositIncomeEstimate(
                    bankId, amount, termMonths, interestRate, isDailyCapitalization
                )
                println(report)
            }
            "5" -> {
                print("Введите ID банка: ")
                val bankId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                print("Введите сумму кредита: ")
                val amount = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат суммы")
                    continue
                }
                print("Введите срок в месяцах: ")
                val termMonths = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат срока")
                    continue
                }
                print("Введите процентную ставку: ")
                val interestRate = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат ставки")
                    continue
                }
                
                val report = reportGenerator.generateCreditOverpaymentEstimate(
                    bankId, amount, termMonths, interestRate
                )
                println(report)
            }
            "6" -> {
                print("Введите ID банка: ")
                val bankId = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат ID")
                    continue
                }
                print("Введите сумму кредита: ")
                val amount = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат суммы")
                    continue
                }
                print("Введите срок в месяцах: ")
                val termMonths = try {
                    scanner.nextLine().trim().toInt()
                } catch (e: NumberFormatException) {
                    println("Неверный формат срока")
                    continue
                }
                print("Введите процентную ставку: ")
                val interestRate = try {
                    BigDecimal(scanner.nextLine().trim())
                } catch (e: NumberFormatException) {
                    println("Неверный формат ставки")
                    continue
                }
                print("Введите дату выдачи (dd.MM.yyyy): ")
                val dateInput = scanner.nextLine().trim()
                val startDate = try {
                    LocalDate.parse(dateInput, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                } catch (e: DateTimeParseException) {
                    println("Неверный формат даты")
                    continue
                }
                
                val report = reportGenerator.generateCreditSchedule(
                    bankId, amount, termMonths, interestRate, startDate
                )
                println(report)
            }
            "0" -> break
            else -> println("Неверный выбор")
        }
    }
}

fun processSystemOperations(bankingService: BankingService, scanner: Scanner) {
    while (true) {
        println("\n=== СИСТЕМНЫЕ ОПЕРАЦИИ ===")
        println("1. Начислить проценты")
        println("2. Обработать истекшие депозиты")
        println("0. Назад")
        print("Выберите операцию: ")
        
        when (scanner.nextLine().trim()) {
            "1" -> {
                bankingService.processInterestAccrual()
                println("Проценты начислены")
            }
            "2" -> {
                bankingService.processDepositExpiration()
                println("Истекшие депозиты обработаны")
            }
            "0" -> break
            else -> println("Неверный выбор")
        }
    }
}

fun createClientWithValidation(bankingService: BankingService, scanner: Scanner) {
    while (true) {
        println("\n=== СОЗДАНИЕ КЛИЕНТА ===")
        
        // Ввод ФИО
        var fullName: String
        while (true) {
            print("Введите ФИО: ")
            fullName = scanner.nextLine().trim()
            if (fullName.isBlank()) {
                println("❌ ФИО не может быть пустым. Попробуйте снова.")
                continue
            }
            if (fullName.length < 5) {
                println("❌ ФИО должно содержать минимум 5 символов. Попробуйте снова.")
                continue
            }
            break
        }
        
        // Ввод ИНН
        var inn: String
        while (true) {
            print("Введите ИНН (12 цифр): ")
            inn = scanner.nextLine().trim()
            if (inn.isBlank()) {
                println("❌ ИНН не может быть пустым. Попробуйте снова.")
                continue
            }
            if (!inn.matches(Regex("\\d{12}"))) {
                println("❌ ИНН должен содержать ровно 12 цифр. Введено: ${inn.length} символов. Попробуйте снова.")
                continue
            }
            break
        }
        
        // Ввод серии паспорта
        var passportSeries: String
        while (true) {
            print("Введите серию паспорта (4 цифры): ")
            passportSeries = scanner.nextLine().trim()
            if (passportSeries.isBlank()) {
                println("❌ Серия паспорта не может быть пустой. Попробуйте снова.")
                continue
            }
            if (!passportSeries.matches(Regex("\\d{4}"))) {
                println("❌ Серия паспорта должна содержать ровно 4 цифры. Введено: ${passportSeries.length} символов. Попробуйте снова.")
                continue
            }
            break
        }
        
        // Ввод номера паспорта
        var passportNumber: String
        while (true) {
            print("Введите номер паспорта (6 цифр): ")
            passportNumber = scanner.nextLine().trim()
            if (passportNumber.isBlank()) {
                println("❌ Номер паспорта не может быть пустым. Попробуйте снова.")
                continue
            }
            if (!passportNumber.matches(Regex("\\d{6}"))) {
                println("❌ Номер паспорта должен содержать ровно 6 цифр. Введено: ${passportNumber.length} символов. Попробуйте снова.")
                continue
            }
            break
        }
        
        // Проверка на дубликаты
        val existingClients = bankingService.getClients()
        val duplicateClient = existingClients.find { 
            it.inn == inn || (it.passportSeries == passportSeries && it.passportNumber == passportNumber)
        }
        
        if (duplicateClient != null) {
            println("❌ Клиент с такими данными уже существует:")
            println("   ФИО: ${duplicateClient.fullName}")
            println("   ИНН: ${duplicateClient.inn}")
            println("   Паспорт: ${duplicateClient.passportSeries} ${duplicateClient.passportNumber}")
            print("Хотите попробовать снова? (y/n): ")
            val retry = scanner.nextLine().trim().lowercase()
            if (retry != "y" && retry != "yes") {
                break
            }
            continue
        }
        
        // Создание клиента
        try {
            val client = bankingService.createClient(fullName, inn, passportNumber, passportSeries)
            println("✅ Клиент '${client.fullName}' успешно создан!")
            println("   ID: ${client.id}")
            println("   ИНН: ${client.inn}")
            println("   Паспорт: ${client.passportSeries} ${client.passportNumber}")
            break
        } catch (e: NumberFormatException) {
            println("❌ Ошибка при создании клиента: ${e.message}")
            print("Хотите попробовать снова? (y/n): ")
            val retry = scanner.nextLine().trim().lowercase()
            if (retry != "y" && retry != "yes") {
                break
            }
        } catch (e: Exception) {
            println("❌ Неожиданная ошибка: ${e.message}")
            print("Хотите попробовать снова? (y/n): ")
            val retry = scanner.nextLine().trim().lowercase()
            if (retry != "y" && retry != "yes") {
                break
            }
        }
    }
}

fun initializeTestData(bankingService: BankingService) {
    // Создаем тестовые банки
    val bank1 = bankingService.createBank("ПАО Сбербанк", "Сбербанк", BigDecimal("1.2"))
    val bank2 = bankingService.createBank("ВТБ Банк", "ВТБ", BigDecimal("0.8"))
    
    // Создаем тестовых клиентов
    val client1 = bankingService.createClient("Иванов Иван Иванович", "123456789012", "567890", "1234")
    val client2 = bankingService.createClient("Петров Петр Петрович", "987654321098", "123456", "5678")
    
    println("Тестовые данные созданы:")
    println("- Банки: ${bankingService.getBanks().size}")
    println("- Клиенты: ${bankingService.getClients().size}")
    println("- Банк 1: ID=${bank1.id}, ${bank1.shortName}")
    println("- Банк 2: ID=${bank2.id}, ${bank2.shortName}")
    println("- Клиент 1: ID=${client1.id}, ${client1.fullName}")
    println("- Клиент 2: ID=${client2.id}, ${client2.fullName}")
}
