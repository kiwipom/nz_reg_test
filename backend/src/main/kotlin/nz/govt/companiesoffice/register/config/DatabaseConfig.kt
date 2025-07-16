package nz.govt.companiesoffice.register.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackages = ["nz.govt.companiesoffice.register.repository"])
@EntityScan(basePackages = ["nz.govt.companiesoffice.register.entity"])
@EnableTransactionManagement
class DatabaseConfig
