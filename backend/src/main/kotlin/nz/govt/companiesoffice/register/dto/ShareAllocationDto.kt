package nz.govt.companiesoffice.register.dto

import org.springframework.format.annotation.DateTimeFormat
import java.math.BigDecimal
import java.time.LocalDate

data class ShareAllocationRequest(
    val companyId: Long,
    val shareholderId: Long,
    val shareClass: String,
    val numberOfShares: Long,
    val nominalValue: BigDecimal,
    val amountPaid: BigDecimal? = null,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val allocationDate: LocalDate? = null,
    val certificateNumber: String? = null,
    val restrictions: String? = null,
)

data class ShareTransferRequest(
    val toShareholderId: Long,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val transferDate: LocalDate? = null,
    val certificateNumber: String? = null,
)

data class PaymentUpdateRequest(
    val additionalPayment: BigDecimal,
)

data class CancellationRequest(
    val reason: String,
)
