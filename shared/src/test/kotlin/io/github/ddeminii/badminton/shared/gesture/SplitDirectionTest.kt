package io.github.ddeminii.badminton.shared.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.ddeminii.badminton.shared.model.Side
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitDirectionTest {

    private val size = IntSize(100, 200)

    @Test
    fun `each layout maps its halves to the right player`() {
        assertEquals(Side.ME, SplitDirection.TOP_BOTTOM.sideAt(Offset(50f, 10f), size))
        assertEquals(Side.OPPONENT, SplitDirection.TOP_BOTTOM.sideAt(Offset(50f, 190f), size))
        assertEquals(Side.ME, SplitDirection.LEFT_RIGHT.sideAt(Offset(10f, 100f), size))
        assertEquals(Side.OPPONENT, SplitDirection.LEFT_RIGHT.sideAt(Offset(90f, 100f), size))
        // 폰 세로: 아래 = 나, 위 = 상대
        assertEquals(Side.OPPONENT, SplitDirection.BOTTOM_TOP.sideAt(Offset(50f, 10f), size))
        assertEquals(Side.ME, SplitDirection.BOTTOM_TOP.sideAt(Offset(50f, 190f), size))
    }
}
