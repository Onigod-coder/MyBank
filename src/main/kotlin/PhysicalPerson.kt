import java.util.*

data class PhysicalPerson(
    val id: Int,
    val fullName: String,
    val inn: String,
    val passportNumber: String,
    val passportSeries: String
) {
    init {
        require(inn.matches(Regex("\\d{12}"))) {
            "ИНН должен содержать 12 цифр"
        }
        require(passportNumber.matches(Regex("\\d{6}"))) {
            "Номер паспорта должен содержать 6 цифр"
        }
        require(passportSeries.matches(Regex("\\d{4}"))) {
            "Серия паспорта должна содержать 4 цифры"
        }
    }
    
    val currentAccounts = mutableSetOf<Int>()
    val depositAccounts = mutableSetOf<Int>()
    val creditAccounts = mutableSetOf<Int>()
    
    fun addCurrentAccount(accountId: Int) {
        currentAccounts.add(accountId)
    }
    
    fun addDepositAccount(accountId: Int) {
        depositAccounts.add(accountId)
    }
    
    fun addCreditAccount(accountId: Int) {
        creditAccounts.add(accountId)
    }
}
