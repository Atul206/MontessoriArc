package com.calmcoloring.app.ui.share

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParentGateTest {
    @Test
    fun check_correctSum_returnsTrue() {
        val gate = ParentGateState(a = 4, b = 5)
        assertTrue(gate.check(9))
    }

    @Test
    fun check_incorrectSum_returnsFalse() {
        val gate = ParentGateState(a = 4, b = 5)
        assertFalse(gate.check(10))
    }
}
