package com.example.edgeaicore.core.billing

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SubscriptionTier {
    FREE,
    PRO,
    ULTIMATE
}

enum class Entitlement {
    LOCAL_AI_BASIC,
    LOCAL_AI_ADVANCED,
    MEMORY_BASIC,
    MEMORY_ADVANCED,
    PRIVATE_SERVER,
    ADVANCED_AGENT,
    MULTI_DEVICE_SYNC
}

interface SubscriptionProvider {
    val currentTier: StateFlow<SubscriptionTier>
    fun hasEntitlement(entitlement: Entitlement): Boolean
    suspend fun purchaseTier(tier: SubscriptionTier): Boolean
}

class EntitlementManager(
    private val subscriptionProvider: SubscriptionProvider
) {
    fun isEntitled(entitlement: Entitlement): Boolean {
        return subscriptionProvider.hasEntitlement(entitlement)
    }
}

class BillingManager(private val context: Context) : SubscriptionProvider {
    private val _currentTier = MutableStateFlow(SubscriptionTier.PRO)
    override val currentTier: StateFlow<SubscriptionTier> = _currentTier.asStateFlow()

    val entitlements = EntitlementManager(this)

    override fun hasEntitlement(entitlement: Entitlement): Boolean {
        val tier = _currentTier.value
        return when (entitlement) {
            Entitlement.LOCAL_AI_BASIC -> true // Free on-device AI is always enabled
            Entitlement.MEMORY_BASIC -> true
            Entitlement.LOCAL_AI_ADVANCED -> tier != SubscriptionTier.FREE
            Entitlement.MEMORY_ADVANCED -> tier != SubscriptionTier.FREE
            Entitlement.PRIVATE_SERVER -> tier == SubscriptionTier.PRO || tier == SubscriptionTier.ULTIMATE
            Entitlement.ADVANCED_AGENT -> tier == SubscriptionTier.PRO || tier == SubscriptionTier.ULTIMATE
            Entitlement.MULTI_DEVICE_SYNC -> tier == SubscriptionTier.ULTIMATE
        }
    }

    override suspend fun purchaseTier(tier: SubscriptionTier): Boolean {
        _currentTier.value = tier
        return true
    }
}
