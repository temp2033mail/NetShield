package com.example.data.model

enum class ThreatCategory(val displayName: String, val description: String) {
    MALWARE("Malware & Trojan", "Known virus, payload dropper or ransomware command & control"),
    PHISHING("Phishing & Fraud", "Deceptive site designed to steal passwords, credentials or financial info"),
    CRYPTOMINER("Cryptojacking", "Unauthorized in-browser or background cryptocurrency mining script"),
    ADWARE_TRACKER("Invasive Adware", "Intrusive tracking, ad-fraud redirects and invasive cookie beacons"),
    TELEMETRY("Aggressive Telemetry", "Covert behavioral monitoring, device fingerprinting, and data exfiltration"),
    APP_FIREWALL("App Network Blocked", "Traffic blocked by per-app cellular or Wi-Fi firewall rule"),
    CUSTOM_BLOCKED("User Blacklisted", "Manually blocked domain from custom blocklist"),
    NONE("Clean / Safe", "No known security threats detected")
}

enum class ThreatLevel(val label: String) {
    CRITICAL("Critical Risk"),
    HIGH("High Risk"),
    MEDIUM("Medium Risk"),
    LOW("Low Risk"),
    SAFE("Verified Safe")
}
