package com.example

import com.example.threat.ThreatIntelligence
import com.example.data.model.ThreatCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMalwareDomainDetection() {
        val result = ThreatIntelligence.checkDomain("trojan-payload-dropper.biz")
        assertTrue(result.isThreat)
        assertEquals(ThreatCategory.MALWARE, result.category)
    }

    @Test
    fun testPhishingDomainDetection() {
        val result = ThreatIntelligence.checkDomain("paypal-security-update.com")
        assertTrue(result.isThreat)
        assertEquals(ThreatCategory.PHISHING, result.category)
    }

    @Test
    fun testCryptominerDomainDetection() {
        val result = ThreatIntelligence.checkDomain("coinhive.com")
        assertTrue(result.isThreat)
        assertEquals(ThreatCategory.CRYPTOMINER, result.category)
    }

    @Test
    fun testCleanDomainDetection() {
        val result = ThreatIntelligence.checkDomain("wikipedia.org")
        assertFalse(result.isThreat)
        assertEquals(ThreatCategory.NONE, result.category)
    }
}
