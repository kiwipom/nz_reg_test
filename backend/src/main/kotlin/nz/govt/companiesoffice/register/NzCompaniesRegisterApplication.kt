package nz.govt.companiesoffice.register

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NzCompaniesRegisterApplication

fun main(args: Array<String>) {
    runApplication<NzCompaniesRegisterApplication>(*args)
}