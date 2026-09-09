package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * Response of `twgCSharpTest/Login.json`. Ported from `nz.co.warehouseandroidtest.data.User`.
 *
 * Unlike the product models, these JSON keys are already camelCase, so no `@SerialName` is
 * needed. Every property carries a default: the endpoint is shared with non-guest flows and
 * kotlinx.serialization throws on a missing key that has no default, which would turn an
 * absent optional field into a failed login.
 *
 * This is a wire type. Do not persist it — see [UserSession] for what is stored.
 */
@Serializable
data class User(
    val customerId: String? = null,
    /** Always empty for guest sessions, so the element type is unconfirmed. */
    val preferredBranchIds: List<String> = emptyList(),
    val eReceiptsPreferred: Boolean = false,
    val isTeamMember: Boolean = false,
    val isStaff: Boolean = false,
    val masterEmailOptIn: Boolean = false,
    /** Format unverified; prefer [expiryMinutes] when computing an expiry. */
    val expiresDatetime: String? = null,
    val expiryMinutes: Int = 0,
    val guest: Boolean = false,
    val platformDemandWare: String? = null,
    val environment: String? = null,
    val developmentPlatform: Boolean = false,
    val apiVersion: Double = 0.0,
    val requestedApiVersion: Double = 0.0,
)
