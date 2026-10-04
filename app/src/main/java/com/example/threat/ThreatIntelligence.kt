package com.example.threat

import com.example.data.model.ThreatCategory
import com.example.data.model.ThreatLevel

data class ThreatCheckResult(
    val isThreat: Boolean,
    val category: ThreatCategory,
    val level: ThreatLevel,
    val threatName: String,
    val reason: String
)

object ThreatIntelligence {

    // Curated high-risk malicious domains (Malware / Ransomware C2 / Trojans)
    private val MALWARE_DOMAINS = hashSetOf(
        "darkcomet-c2.top",
        "trojan-payload-dropper.biz",
        "emotet-beacon-node.cc",
        "lockbit-ransom-decryptor.onion.pet",
        "cobaltstrike-listener.xyz",
        "malware-traffic-analysis.net",
        "botnet-c2-gateway.org",
        "payload-delivery.cloud",
        "redline-stealer-collector.site",
        "agent-tesla-exfil.info",
        "rat-server-connect.su",
        "malware-test-beacon.org"
    )

    // Curated phishing & credential theft domains
    private val PHISHING_DOMAINS = hashSetOf(
        "paypal-security-update.com",
        "apple-id-verify-alert.net",
        "secure-login-chase.info",
        "wellsfargo-verify-customer.cc",
        "netflix-billing-renewal.top",
        "google-account-verify-session.biz",
        "microsoft-login-auth-verify.xyz",
        "instagram-copyright-badge-appeal.co",
        "coinbase-auth-resolve.net",
        "bankofamerica-secure-auth.org",
        "phishing-update-alert.xyz"
    )

    // Curated cryptojacking / illicit in-app/browser mining
    private val CRYPTOMINER_DOMAINS = hashSetOf(
        "coinhive.com",
        "crypto-loot.com",
        "minr.pw",
        "monerominer.rocks",
        "webminepool.com",
        "cryptonight-miner.io",
        "coin-have.com",
        "jsecoin.com",
        "authedmine.com",
        "hashvault-mining.pro",
        "cryptominer-test-pool.io"
    )

    // Invasive Adware, Popunder, Click-fraud
    private val ADWARE_DOMAINS = hashSetOf(
        "ad-click-fraud.net",
        "popunder-redirects.info",
        "zeroredirect2.com",
        "push-notification-spam.biz",
        "traffic-arbitrage-bot.top",
        "malvertising-injection.cc",
        "adsupply-intrusive.net",
        "revenuehits-redirect.org",
        "outbrain-malvertising.club"
    )

    // Invasive Telemetry, Spyware beacons, Covert device tracking
    private val TELEMETRY_DOMAINS = hashSetOf(
        "telemetry-beacon.analytics-cloud.io",
        "fingerprint-tracker.biz",
        "covert-location-logger.org",
        "device-telemetry-exfil.net",
        "app-stealth-spy-collector.info",
        "commercial-spy-sync.su",
        "aggressive-profiler.xyz",
        "unauthorized-keystroke-log.top"
    )

    // Suspicious pattern keywords in domains
    private val SUSPICIOUS_KEYWORDS = listOf(
        "verify-account", "login-update", "secure-bank-login",
        "wallet-connect-airdrop", "free-crypto-giveaway", "stealer-download",
        "crack-keygen", "payload-bin", "ransom-pay"
    )

    /**
     * Inspects a requested domain against our threat intelligence database,
     * including exact matching, parent-domain suffix matching, and heuristics.
     */
    fun checkDomain(domain: String): ThreatCheckResult {
        val cleanDomain = domain.trim().lowercase().removePrefix("www.")

        // 1. Direct malware / ransomware check
        if (matchesDomainList(cleanDomain, MALWARE_DOMAINS)) {
            return ThreatCheckResult(
                isThreat = true,
                category = ThreatCategory.MALWARE,
                level = ThreatLevel.CRITICAL,
                threatName = "Malware Command & Control (C2)",
                reason = "Domain is linked to botnet command infrastructure, payload distributors, or ransomware beacons."
            )
        }

        // 2. Direct phishing check
        if (matchesDomainList(cleanDomain, PHISHING_DOMAINS)) {
            return ThreatCheckResult(
                isThreat = true,
                category = ThreatCategory.PHISHING,
                level = ThreatLevel.CRITICAL,
                threatName = "Credential Theft / Phishing",
                reason = "Impersonation site designed to harvest personal credentials, 2FA codes, or banking information."
            )
        }

        // 3. Cryptojacking check
        if (matchesDomainList(cleanDomain, CRYPTOMINER_DOMAINS)) {
            return ThreatCheckResult(
                isThreat = true,
                category = ThreatCategory.CRYPTOMINER,
                level = ThreatLevel.HIGH,
                threatName = "Illicit Cryptojacking Pool",
                reason = "Domain runs covert CPU/GPU cryptocurrency mining scripts that drain battery and overheat hardware."
            )
        }

        // 4. Adware / Malvertising check
        if (matchesDomainList(cleanDomain, ADWARE_DOMAINS)) {
            return ThreatCheckResult(
                isThreat = true,
                category = ThreatCategory.ADWARE_TRACKER,
                level = ThreatLevel.MEDIUM,
                threatName = "Invasive Malvertising & Ad-Fraud",
                reason = "Distributes popunders, malicious redirects, or click-fraud scripts."
            )
        }

        // 5. Telemetry / Spyware check
        if (matchesDomainList(cleanDomain, TELEMETRY_DOMAINS)) {
            return ThreatCheckResult(
                isThreat = true,
                category = ThreatCategory.TELEMETRY,
                level = ThreatLevel.MEDIUM,
                threatName = "Covert Tracking & Data Exfiltration",
                reason = "Collects unauthorized device fingerprinting, clipboard, or location telemetry."
            )
        }

        // 6. Heuristic Keyword & Suspicious TLD check
        for (kw in SUSPICIOUS_KEYWORDS) {
            if (cleanDomain.contains(kw)) {
                return ThreatCheckResult(
                    isThreat = true,
                    category = ThreatCategory.PHISHING,
                    level = ThreatLevel.HIGH,
                    threatName = "Heuristic Threat Match ($kw)",
                    reason = "Domain exhibits deceptive security keywords typical of credential harvesting campaigns."
                )
            }
        }

        // Clean & Verified
        return ThreatCheckResult(
            isThreat = false,
            category = ThreatCategory.NONE,
            level = ThreatLevel.SAFE,
            threatName = "Verified Safe",
            reason = "No known malicious signatures or anomalies detected."
        )
    }

    private fun matchesDomainList(cleanDomain: String, list: Set<String>): Boolean {
        if (list.contains(cleanDomain)) return true
        for (item in list) {
            if (cleanDomain.endsWith(".$item")) return true
        }
        return false
    }

    /**
     * Sample test threats for the user to trigger interactive protection tests.
     */
    val SAMPLE_TEST_DOMAINS = listOf(
        "malware-test-beacon.org",
        "phishing-update-alert.xyz",
        "crypto-loot.com",
        "ad-click-fraud.net",
        "telemetry-beacon.analytics-cloud.io",
        "google.com",
        "wikipedia.org",
        "github.com"
    )
}
