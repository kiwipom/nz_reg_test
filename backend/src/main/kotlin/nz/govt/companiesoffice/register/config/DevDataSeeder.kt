package nz.govt.companiesoffice.register.config

import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.entity.DirectorStatus
import nz.govt.companiesoffice.register.entity.Shareholder
import nz.govt.companiesoffice.register.repository.CompanyRepository
import nz.govt.companiesoffice.register.repository.DirectorRepository
import nz.govt.companiesoffice.register.repository.ShareholderRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@Profile("dev")
class DevDataSeeder(
    private val companyRepository: CompanyRepository,
    private val directorRepository: DirectorRepository,
    private val shareholderRepository: ShareholderRepository,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DevDataSeeder::class.java)

    override fun run(vararg args: String?) {
        logger.info("Starting development data seeding...")

        // Only seed if no companies exist
        if (companyRepository.count() == 0L) {
            seedCompanies()
            logger.info("Development data seeding completed successfully!")
        } else {
            logger.info("Development data already exists, skipping seeding")
        }
    }

    private fun seedCompanies() {
        val companies = createTestCompanies()
        val savedCompanies = companyRepository.saveAll(companies)
        logger.info("Seeded ${savedCompanies.size} companies")

        // Create directors and shareholders for each company
        savedCompanies.forEach { company ->
            val directors = createDirectorsForCompany(company)
            val shareholders = createShareholdersForCompany(company)

            directorRepository.saveAll(directors)
            shareholderRepository.saveAll(shareholders)

            logger.debug(
                "Created ${directors.size} directors and ${shareholders.size} shareholders for ${company.companyName}",
            )
        }
    }

    private fun createTestCompanies(): List<Company> {
        return listOf(
            Company(
                companyName = "Tech Innovations Limited",
                companyNumber = "1234567",
                companyType = CompanyType.LTD,
                nzbn = "9429047658423",
                incorporationDate = LocalDate.of(2020, 1, 15),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Green Solutions NZ Ltd",
                companyNumber = "2345678",
                companyType = CompanyType.LTD,
                nzbn = "9429047658424",
                incorporationDate = LocalDate.of(2019, 3, 22),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Kiwi Manufacturing Co",
                companyNumber = "3456789",
                companyType = CompanyType.LTD,
                nzbn = "9429047658425",
                incorporationDate = LocalDate.of(2018, 7, 10),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Digital Marketing Plus",
                companyNumber = "4567890",
                companyType = CompanyType.LTD,
                nzbn = "9429047658426",
                incorporationDate = LocalDate.of(2021, 5, 18),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Auckland Consulting Group",
                companyNumber = "5678901",
                companyType = CompanyType.LTD,
                nzbn = "9429047658427",
                incorporationDate = LocalDate.of(2017, 11, 3),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Wellington Web Services",
                companyNumber = "6789012",
                companyType = CompanyType.LTD,
                nzbn = "9429047658428",
                incorporationDate = LocalDate.of(2022, 2, 28),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Christchurch Construction Ltd",
                companyNumber = "7890123",
                companyType = CompanyType.LTD,
                nzbn = "9429047658429",
                incorporationDate = LocalDate.of(2016, 9, 12),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Hamilton Healthcare Solutions",
                companyNumber = "8901234",
                companyType = CompanyType.LTD,
                nzbn = "9429047658430",
                incorporationDate = LocalDate.of(2020, 8, 7),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Tauranga Tourism Services",
                companyNumber = "9012345",
                companyType = CompanyType.LTD,
                nzbn = "9429047658431",
                incorporationDate = LocalDate.of(2019, 12, 15),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Dunedin Development Co",
                companyNumber = "0123456",
                companyType = CompanyType.LTD,
                nzbn = "9429047658432",
                incorporationDate = LocalDate.of(2018, 4, 20),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Rotorua Retail Group",
                companyNumber = "1234568",
                companyType = CompanyType.LTD,
                nzbn = "9429047658433",
                incorporationDate = LocalDate.of(2021, 10, 11),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Palmerston North Properties",
                companyNumber = "2345679",
                companyType = CompanyType.LTD,
                nzbn = "9429047658434",
                incorporationDate = LocalDate.of(2017, 6, 25),
                status = "ACTIVE",
            ),
            Company(
                companyName = "Inactive Test Company",
                companyNumber = "9999999",
                companyType = CompanyType.LTD,
                nzbn = "9429047658435",
                incorporationDate = LocalDate.of(2015, 1, 1),
                status = "REMOVED",
            ),
        )
    }

    private fun createDirectorsForCompany(company: Company): List<Director> {
        return when (company.companyNumber) {
            "1234567" -> listOf(
                createDirector(
                    company,
                    "John Michael Smith",
                    LocalDate.of(1980, 3, 15),
                    "Auckland, New Zealand",
                    "123 Queen Street",
                    "Auckland",
                    true,
                    false,
                ),
                createDirector(
                    company,
                    "Sarah Jane Wilson",
                    LocalDate.of(1975, 7, 22),
                    "Wellington, New Zealand",
                    "456 Lambton Quay",
                    "Wellington",
                    true,
                    false,
                ),
            )
            "2345678" -> listOf(
                createDirector(
                    company,
                    "Michael Robert Brown",
                    LocalDate.of(1983, 11, 8),
                    "Christchurch, New Zealand",
                    "789 Colombo Street",
                    "Christchurch",
                    true,
                    false,
                ),
                createDirector(
                    company,
                    "Emma Louise Davis",
                    LocalDate.of(1978, 9, 14),
                    "Hamilton, New Zealand",
                    "321 Victoria Street",
                    "Hamilton",
                    true,
                    false,
                ),
            )
            "3456789" -> listOf(
                createDirector(
                    company,
                    "David Paul Johnson",
                    LocalDate.of(1970, 5, 30),
                    "Tauranga, New Zealand",
                    "654 Cameron Road",
                    "Tauranga",
                    true,
                    false,
                ),
                createDirector(
                    company,
                    "Lisa Marie Thompson",
                    LocalDate.of(1985, 12, 3),
                    "Dunedin, New Zealand",
                    "987 George Street",
                    "Dunedin",
                    true,
                    false,
                ),
            )
            "4567890" -> listOf(
                createDirector(
                    company,
                    "James Alexander Clark",
                    LocalDate.of(1990, 2, 18),
                    "Palmerston North, New Zealand",
                    "147 Main Street",
                    "Palmerston North",
                    true,
                    false,
                ),
            )
            "5678901" -> listOf(
                createDirector(
                    company,
                    "Robert William Taylor",
                    LocalDate.of(1972, 8, 25),
                    "Auckland, New Zealand",
                    "258 Parnell Road",
                    "Auckland",
                    true,
                    false,
                ),
                createDirector(
                    company,
                    "Jennifer Kate Anderson",
                    LocalDate.of(1988, 4, 12),
                    "Sydney, Australia",
                    "369 Ponsonby Road",
                    "Auckland",
                    false,
                    true,
                ),
                createResignedDirector(
                    company,
                    "Mark Steven Williams",
                    LocalDate.of(1965, 1, 7),
                    "Auckland, New Zealand",
                    "741 Remuera Road",
                    "Auckland",
                    true,
                    false,
                ),
            )
            else -> listOf(
                createDirector(
                    company,
                    "Default Director",
                    LocalDate.of(1980, 1, 1),
                    "Auckland, New Zealand",
                    "123 Default Street",
                    "Auckland",
                    true,
                    false,
                ),
            )
        }
    }

    private fun createDirector(
        company: Company,
        fullName: String,
        dateOfBirth: LocalDate,
        placeOfBirth: String,
        address: String,
        city: String,
        isNzResident: Boolean,
        isAustralianResident: Boolean,
    ): Director {
        return Director(
            company = company,
            fullName = fullName,
            dateOfBirth = dateOfBirth,
            placeOfBirth = placeOfBirth,
            residentialAddressLine1 = address,
            residentialCity = city,
            residentialCountry = "NZ",
            isNzResident = isNzResident,
            isAustralianResident = isAustralianResident,
            status = DirectorStatus.ACTIVE,
            consentGiven = true,
            consentDate = company.incorporationDate.minusDays(5),
            appointedDate = company.incorporationDate,
        )
    }

    private fun createResignedDirector(
        company: Company,
        fullName: String,
        dateOfBirth: LocalDate,
        placeOfBirth: String,
        address: String,
        city: String,
        isNzResident: Boolean,
        isAustralianResident: Boolean,
    ): Director {
        return Director(
            company = company,
            fullName = fullName,
            dateOfBirth = dateOfBirth,
            placeOfBirth = placeOfBirth,
            residentialAddressLine1 = address,
            residentialCity = city,
            residentialCountry = "NZ",
            isNzResident = isNzResident,
            isAustralianResident = isAustralianResident,
            status = DirectorStatus.RESIGNED,
            consentGiven = true,
            consentDate = company.incorporationDate.minusDays(5),
            appointedDate = company.incorporationDate,
            resignedDate = company.incorporationDate.plusYears(2),
        )
    }

    private fun createShareholdersForCompany(company: Company): List<Shareholder> {
        return when (company.companyNumber) {
            "1234567" -> listOf(
                createIndividualShareholder(company, "John Michael Smith", "123 Queen Street", "Auckland"),
                createIndividualShareholder(company, "Sarah Jane Wilson", "456 Lambton Quay", "Wellington"),
                createCorporateShareholder(company, "Innovation Holdings Ltd", "789 Custom Street", "Auckland"),
            )
            "2345678" -> listOf(
                createIndividualShareholder(company, "Michael Robert Brown", "789 Colombo Street", "Christchurch"),
                createIndividualShareholder(company, "Emma Louise Davis", "321 Victoria Street", "Hamilton"),
                createCorporateShareholder(company, "Green Investment Trust", "456 Willis Street", "Wellington"),
            )
            "3456789" -> listOf(
                createIndividualShareholder(company, "David Paul Johnson", "654 Cameron Road", "Tauranga"),
                createIndividualShareholder(company, "Lisa Marie Thompson", "987 George Street", "Dunedin"),
                createCorporateShareholder(company, "Manufacturing Partners Ltd", "123 Industrial Road", "Hamilton"),
            )
            "4567890" -> listOf(
                createIndividualShareholder(company, "James Alexander Clark", "147 Main Street", "Palmerston North"),
                createCorporateShareholderInternational(
                    company,
                    "Digital Ventures Inc",
                    "500 Collins Street",
                    "Melbourne",
                    "AU",
                ),
            )
            "5678901" -> listOf(
                createIndividualShareholder(company, "Robert William Taylor", "258 Parnell Road", "Auckland"),
                createIndividualShareholder(company, "Jennifer Kate Anderson", "369 Ponsonby Road", "Auckland"),
                createCorporateShareholder(company, "Consulting Capital Ltd", "741 Shortland Street", "Auckland"),
            )
            else -> listOf(
                createCorporateShareholder(company, "${company.companyName} Trust", "123 Default Street", "Auckland"),
            )
        }
    }

    private fun createIndividualShareholder(
        company: Company,
        fullName: String,
        address: String,
        city: String,
    ): Shareholder {
        return Shareholder(
            company = company,
            fullName = fullName,
            addressLine1 = address,
            city = city,
            country = "NZ",
            isIndividual = true,
        )
    }

    private fun createCorporateShareholder(
        company: Company,
        fullName: String,
        address: String,
        city: String,
    ): Shareholder {
        return Shareholder(
            company = company,
            fullName = fullName,
            addressLine1 = address,
            city = city,
            country = "NZ",
            isIndividual = false,
        )
    }

    private fun createCorporateShareholderInternational(
        company: Company,
        fullName: String,
        address: String,
        city: String,
        country: String,
    ): Shareholder {
        return Shareholder(
            company = company,
            fullName = fullName,
            addressLine1 = address,
            city = city,
            country = country,
            isIndividual = false,
        )
    }
}
