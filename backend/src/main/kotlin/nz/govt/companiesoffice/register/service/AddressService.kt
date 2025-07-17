package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Address
import nz.govt.companiesoffice.register.entity.AddressType
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.repository.AddressRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class AddressService(
    private val addressRepository: AddressRepository,
    private val auditService: AuditService,
    private val addressValidationService: AddressValidationService,
) {

    private val logger = LoggerFactory.getLogger(AddressService::class.java)

    fun createAddress(address: Address): Address {
        logger.info("Creating address for company ${address.company.id} of type ${address.addressType}")

        validateAddress(address)
        checkForOverlappingAddresses(address)

        val savedAddress = addressRepository.save(address)

        auditService.logEvent(
            action = nz.govt.companiesoffice.register.audit.AuditAction.CREATE,
            resourceType = "Address",
            resourceId = savedAddress.id.toString(),
            details = mapOf(
                "companyId" to address.company.id,
                "addressType" to address.addressType.name,
                "effectiveFrom" to address.effectiveFrom,
                "effectiveTo" to address.effectiveTo,
            ),
        )

        logger.info("Successfully created address ${savedAddress.id} for company ${address.company.id}")
        return savedAddress
    }

    fun updateAddress(address: Address): Address {
        logger.info("Updating address ${address.id} for company ${address.company.id}")

        validateAddress(address)
        checkForOverlappingAddresses(address)

        val savedAddress = addressRepository.save(address)

        auditService.logEvent(
            action = nz.govt.companiesoffice.register.audit.AuditAction.UPDATE,
            resourceType = "Address",
            resourceId = savedAddress.id.toString(),
            details = mapOf(
                "companyId" to address.company.id,
                "addressType" to address.addressType.name,
                "effectiveFrom" to address.effectiveFrom,
                "effectiveTo" to address.effectiveTo,
            ),
        )

        logger.info("Successfully updated address ${savedAddress.id}")
        return savedAddress
    }

    fun changeAddress(
        company: Company,
        addressType: AddressType,
        newAddress: Address,
        effectiveDate: LocalDate = LocalDate.now(),
    ): Address {
        logger.info("Changing ${addressType.name} address for company ${company.id} effective $effectiveDate")

        // Get current address for validation
        val currentAddress = getCurrentAddress(company, addressType)

        // Validate the address change using the validation service
        if (currentAddress != null) {
            val validationResult = addressValidationService.validateAddressUpdate(
                currentAddress = currentAddress,
                newAddress = newAddress,
                effectiveDate = effectiveDate,
            )

            if (!validationResult.isValid) {
                throw IllegalArgumentException(
                    "Address change validation failed: ${validationResult.errors.joinToString(", ")}",
                )
            }

            // Log warnings if any
            if (validationResult.hasWarnings()) {
                logger.warn("Address change warnings: ${validationResult.warnings.joinToString(", ")}")
            }
        }

        // End current address if it exists
        if (currentAddress != null && currentAddress.effectiveTo == null) {
            currentAddress.effectiveTo = effectiveDate.minusDays(1)
            addressRepository.save(currentAddress)

            auditService.logEvent(
                action = nz.govt.companiesoffice.register.audit.AuditAction.UPDATE,
                resourceType = "Address",
                resourceId = currentAddress.id.toString(),
                details = mapOf(
                    "companyId" to company.id,
                    "action" to "END_ADDRESS",
                    "endDate" to currentAddress.effectiveTo,
                ),
            )
        }

        // Create new address
        newAddress.company = company
        newAddress.effectiveFrom = effectiveDate
        newAddress.effectiveTo = null

        return createAddress(newAddress)
    }

    /**
     * Initiates an address change workflow with validation and approval steps
     */
    fun initiateAddressChangeWorkflow(
        company: Company,
        addressType: AddressType,
        newAddress: Address,
        effectiveDate: LocalDate = LocalDate.now(),
        requestedBy: String? = null,
    ): AddressChangeWorkflow {
        logger.info("Initiating address change workflow for company ${company.id}, type ${addressType.name}")

        val currentAddress = getCurrentAddress(company, addressType)

        // Perform comprehensive validation
        val validationResult = if (currentAddress != null) {
            addressValidationService.validateAddressUpdate(currentAddress, newAddress, effectiveDate)
        } else {
            addressValidationService.validateAddress(newAddress)
        }

        val workflow = AddressChangeWorkflow(
            companyId = company.id,
            addressType = addressType,
            currentAddress = currentAddress,
            proposedAddress = newAddress,
            effectiveDate = effectiveDate,
            status = if (validationResult.isValid) {
                AddressChangeStatus.PENDING_APPROVAL
            } else {
                AddressChangeStatus.VALIDATION_FAILED
            },
            validationResult = validationResult,
            requestedBy = requestedBy,
            requestedAt = java.time.LocalDateTime.now(),
        )

        // Determine if automatic approval is possible
        if (canAutoApprove(workflow)) {
            return approveAddressChange(workflow)
        }

        auditService.logEvent(
            action = nz.govt.companiesoffice.register.audit.AuditAction.CREATE,
            resourceType = "AddressChangeWorkflow",
            resourceId = "${company.id}-${addressType.name}-${System.currentTimeMillis()}",
            details = mapOf(
                "companyId" to company.id,
                "addressType" to addressType.name,
                "status" to workflow.status.name,
                "requestedBy" to requestedBy,
                "hasValidationErrors" to validationResult.hasErrors(),
                "hasValidationWarnings" to validationResult.hasWarnings(),
            ),
        )

        return workflow
    }

    /**
     * Approves and executes an address change workflow
     */
    fun approveAddressChange(workflow: AddressChangeWorkflow): AddressChangeWorkflow {
        logger.info("Approving address change workflow for company ${workflow.companyId}")

        if (!workflow.validationResult.isValid) {
            throw IllegalStateException("Cannot approve workflow with validation errors")
        }

        // Execute the address change
        val newAddress = changeAddress(
            company = workflow.currentAddress?.company ?: workflow.proposedAddress.company,
            addressType = workflow.addressType,
            newAddress = workflow.proposedAddress,
            effectiveDate = workflow.effectiveDate,
        )

        val approvedWorkflow = workflow.copy(
            status = AddressChangeStatus.APPROVED,
            approvedAt = java.time.LocalDateTime.now(),
            executedAddress = newAddress,
        )

        auditService.logEvent(
            action = nz.govt.companiesoffice.register.audit.AuditAction.UPDATE,
            resourceType = "AddressChangeWorkflow",
            resourceId = "${workflow.companyId}-${workflow.addressType.name}",
            details = mapOf(
                "companyId" to workflow.companyId,
                "addressType" to workflow.addressType.name,
                "status" to "APPROVED",
                "newAddressId" to newAddress.id,
            ),
        )

        return approvedWorkflow
    }

    /**
     * Rejects an address change workflow
     */
    fun rejectAddressChange(workflow: AddressChangeWorkflow, reason: String): AddressChangeWorkflow {
        logger.info("Rejecting address change workflow for company ${workflow.companyId}: $reason")

        val rejectedWorkflow = workflow.copy(
            status = AddressChangeStatus.REJECTED,
            rejectionReason = reason,
            rejectedAt = java.time.LocalDateTime.now(),
        )

        auditService.logEvent(
            action = nz.govt.companiesoffice.register.audit.AuditAction.UPDATE,
            resourceType = "AddressChangeWorkflow",
            resourceId = "${workflow.companyId}-${workflow.addressType.name}",
            details = mapOf(
                "companyId" to workflow.companyId,
                "addressType" to workflow.addressType.name,
                "status" to "REJECTED",
                "reason" to reason,
            ),
        )

        return rejectedWorkflow
    }

    /**
     * Bulk address update workflow for companies with multiple addresses
     */
    fun bulkUpdateAddresses(
        company: Company,
        addressUpdates: List<AddressUpdate>,
        effectiveDate: LocalDate = LocalDate.now(),
    ): BulkAddressUpdateResult {
        logger.info("Processing bulk address update for company ${company.id} with ${addressUpdates.size} updates")

        val results = mutableListOf<AddressChangeWorkflow>()
        val errors = mutableListOf<String>()

        // Validate all updates first
        for (update in addressUpdates) {
            try {
                val workflow = initiateAddressChangeWorkflow(
                    company = company,
                    addressType = update.addressType,
                    newAddress = update.newAddress,
                    effectiveDate = effectiveDate,
                    requestedBy = update.requestedBy,
                )
                results.add(workflow)
            } catch (e: Exception) {
                errors.add("${update.addressType.name}: ${e.message}")
            }
        }

        // If any validation failed, return early
        if (errors.isNotEmpty()) {
            return BulkAddressUpdateResult(
                company = company,
                workflows = results,
                errors = errors,
                status = BulkUpdateStatus.VALIDATION_FAILED,
            )
        }

        // Approve all valid workflows
        val approvedWorkflows = results.map { workflow ->
            if (canAutoApprove(workflow)) {
                approveAddressChange(workflow)
            } else {
                workflow
            }
        }

        auditService.logEvent(
            action = nz.govt.companiesoffice.register.audit.AuditAction.UPDATE,
            resourceType = "BulkAddressUpdate",
            resourceId = company.id.toString(),
            details = mapOf(
                "companyId" to company.id,
                "updateCount" to addressUpdates.size,
                "successCount" to approvedWorkflows.count { it.status == AddressChangeStatus.APPROVED },
                "pendingCount" to approvedWorkflows.count { it.status == AddressChangeStatus.PENDING_APPROVAL },
            ),
        )

        return BulkAddressUpdateResult(
            company = company,
            workflows = approvedWorkflows,
            errors = errors,
            status = if (approvedWorkflows.all { it.status == AddressChangeStatus.APPROVED }) {
                BulkUpdateStatus.COMPLETED
            } else {
                BulkUpdateStatus.PENDING_APPROVAL
            },
        )
    }

    private fun canAutoApprove(workflow: AddressChangeWorkflow): Boolean {
        // Auto-approve if:
        // 1. No validation errors
        // 2. No significant warnings (like country changes for registered addresses)
        // 3. Not a registered address change to different country
        // 4. Change is within reasonable timeframe (not too far in future)

        if (!workflow.validationResult.isValid) {
            return false
        }

        // Check for significant warnings that require manual approval
        val significantWarnings = listOf(
            "different country may have legal implications",
            "different region may affect court jurisdiction",
        )

        val hasSignificantWarnings = workflow.validationResult.warnings.any { warning ->
            significantWarnings.any { significant -> warning.contains(significant, ignoreCase = true) }
        }

        if (hasSignificantWarnings) {
            logger.info("Manual approval required due to significant warnings: ${workflow.validationResult.warnings}")
            return false
        }

        // Check if effective date is too far in future (requires manual approval)
        if (workflow.effectiveDate.isAfter(LocalDate.now().plusMonths(6))) {
            logger.info("Manual approval required for address change more than 6 months in future")
            return false
        }

        return true
    }

    @Transactional(readOnly = true)
    fun getCurrentAddress(company: Company, addressType: AddressType): Address? {
        return addressRepository.findEffectiveAddressByCompanyAndType(company, addressType)
    }

    @Transactional(readOnly = true)
    fun getCurrentAddressById(companyId: Long, addressType: AddressType): Address? {
        return addressRepository.findEffectiveAddressByCompanyIdAndType(companyId, addressType)
    }

    @Transactional(readOnly = true)
    fun getCurrentAddresses(company: Company): List<Address> {
        return addressRepository.findEffectiveAddressesByCompany(company)
    }

    @Transactional(readOnly = true)
    fun getAddressHistory(company: Company, addressType: AddressType): List<Address> {
        return addressRepository.findAddressHistoryByCompanyAndType(company, addressType)
    }

    @Transactional(readOnly = true)
    fun getAllAddressHistory(company: Company): List<Address> {
        return addressRepository.findAddressHistoryByCompany(company)
    }

    @Transactional(readOnly = true)
    fun getAddressAtDate(company: Company, addressType: AddressType, date: LocalDate): Address? {
        return addressRepository.findEffectiveAddressByCompanyAndType(company, addressType, date)
    }

    @Transactional(readOnly = true)
    fun validateCompanyRequiredAddresses(company: Company): List<String> {
        val errors = mutableListOf<String>()

        // Check for required registered address
        val registeredAddress = getCurrentAddress(company, AddressType.REGISTERED)
        if (registeredAddress == null) {
            errors.add("Company must have a registered address")
        }

        // Check for required service address (can be same as registered)
        val serviceAddress = getCurrentAddress(company, AddressType.SERVICE)
        if (serviceAddress == null) {
            errors.add("Company must have a service address")
        }

        return errors
    }

    @Transactional(readOnly = true)
    fun searchAddressesByCity(city: String): List<Address> {
        return addressRepository.findEffectiveAddressesByCity(city)
    }

    @Transactional(readOnly = true)
    fun searchAddressesByPostcode(postcode: String): List<Address> {
        return addressRepository.findEffectiveAddressesByPostcode(postcode)
    }

    @Transactional(readOnly = true)
    fun getAvailableCities(country: String = "NZ"): List<String> {
        return addressRepository.findDistinctCitiesByCountry(country)
    }

    @Transactional(readOnly = true)
    fun getAvailablePostcodes(country: String = "NZ"): List<String> {
        return addressRepository.findDistinctPostcodesByCountry(country)
    }

    fun validateAddress(address: Address) {
        val errors = mutableListOf<String>()

        // Basic field validation
        if (address.addressLine1.trim().isEmpty()) {
            errors.add("Address line 1 is required")
        }
        if (address.addressLine1.length > 255) {
            errors.add("Address line 1 must be 255 characters or less")
        }
        if (address.addressLine2?.length ?: 0 > 255) {
            errors.add("Address line 2 must be 255 characters or less")
        }
        if (address.city.trim().isEmpty()) {
            errors.add("City is required")
        }
        if (address.city.length > 100) {
            errors.add("City must be 100 characters or less")
        }
        if (address.region?.length ?: 0 > 100) {
            errors.add("Region must be 100 characters or less")
        }
        if (address.postcode?.length ?: 0 > 10) {
            errors.add("Postcode must be 10 characters or less")
        }
        if (address.country.length != 2) {
            errors.add("Country must be a valid 2-letter country code")
        }

        // Email validation
        if (address.email != null && !isValidEmail(address.email!!)) {
            errors.add("Invalid email format")
        }

        // Phone validation
        if (address.phone != null && !isValidPhoneNumber(address.phone!!)) {
            errors.add("Invalid phone number format")
        }

        // Date validation
        if (address.effectiveTo != null && address.effectiveFrom >= address.effectiveTo) {
            errors.add("Effective from date must be before effective to date")
        }

        // NZ-specific validation
        if (address.country == "NZ") {
            validateNZAddress(address, errors)
        }

        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Address validation failed: ${errors.joinToString(", ")}")
        }
    }

    private fun validateNZAddress(address: Address, errors: MutableList<String>) {
        // NZ postcode validation (4-digit)
        if (address.postcode != null && !address.postcode!!.matches(Regex("^\\d{4}$"))) {
            errors.add("New Zealand postcode must be 4 digits")
        }

        // NZ phone number validation
        if (address.phone != null && !isValidNZPhoneNumber(address.phone!!)) {
            errors.add("Invalid New Zealand phone number format")
        }
    }

    private fun checkForOverlappingAddresses(address: Address) {
        val fromDate = address.effectiveFrom
        val toDate = address.effectiveTo ?: LocalDate.of(9999, 12, 31)

        val hasOverlap = addressRepository.hasOverlappingAddresses(
            company = address.company,
            addressType = address.addressType,
            fromDate = fromDate,
            toDate = toDate,
            excludeId = address.id,
        )

        if (hasOverlap) {
            throw IllegalStateException(
                "Address effective dates overlap with existing ${address.addressType.name} " +
                    "address for company ${address.company.id}",
            )
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Basic international phone number validation
        return phone.matches(Regex("^\\+?[1-9]\\d{1,14}$"))
    }

    private fun isValidNZPhoneNumber(phone: String): Boolean {
        // NZ phone number formats: +64 9 xxx xxxx, 09 xxx xxxx, 021 xxx xxxx, etc.
        val nzPhonePattern = Regex("^(\\+64|0)([2-9]\\d{7,8}|21\\d{6,7}|22\\d{6,7}|27\\d{6,7})$")
        return phone.replace("\\s".toRegex(), "").matches(nzPhonePattern)
    }
}
