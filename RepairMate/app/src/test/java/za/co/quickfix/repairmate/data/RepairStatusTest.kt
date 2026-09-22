package za.co.quickfix.repairmate.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import za.co.quickfix.repairmate.data.local.RepairStatus

class RepairStatusTest {
    @Test fun stages_advance_in_order_and_stop_at_collected() {
        assertEquals(RepairStatus.DIAGNOSING, RepairStatus.RECEIVED.next())
        assertEquals(RepairStatus.IN_PROGRESS, RepairStatus.DIAGNOSING.next())
        assertEquals(RepairStatus.READY, RepairStatus.IN_PROGRESS.next())
        assertEquals(RepairStatus.COLLECTED, RepairStatus.READY.next())
        assertNull(RepairStatus.COLLECTED.next())
    }

    @Test fun unknown_or_null_names_fall_back_to_received() {
        assertEquals(RepairStatus.RECEIVED, RepairStatus.fromName(null))
        assertEquals(RepairStatus.RECEIVED, RepairStatus.fromName("SOMETHING_ELSE"))
        assertEquals(RepairStatus.READY, RepairStatus.fromName("READY"))
    }
}
