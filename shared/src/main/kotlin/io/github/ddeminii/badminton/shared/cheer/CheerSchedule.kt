package io.github.ddeminii.badminton.shared.cheer

import kotlin.random.Random

/**
 * 응원을 "가끔씩" 내보내는 간격을 정한다.
 * 점수가 날 때마다 [onPoint]를 부르면, [everyPoints] 범위에서 무작위로 고른 점수마다 한 번 true를 돌려준다.
 */
class CheerSchedule(
    private val everyPoints: IntRange = 4..7,
    private val random: Random = Random.Default,
) {
    private var pointsLeft = everyPoints.random(random)

    fun onPoint(): Boolean {
        pointsLeft--
        if (pointsLeft > 0) return false
        pointsLeft = everyPoints.random(random)
        return true
    }
}
