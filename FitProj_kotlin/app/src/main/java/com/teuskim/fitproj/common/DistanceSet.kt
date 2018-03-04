package com.teuskim.fitproj.common

/**
 * 거리 정보
 */
class DistanceSet {

    private var walk: Float = 0.toFloat()
    private var run: Float = 0.toFloat()
    private var cycle: Float = 0.toFloat()

    fun getWalk(isConvertUnitOn: Boolean): Float {
        return if (isConvertUnitOn) {
            FitUtil.convertToMi(walk)
        } else walk
    }

    fun setWalk(walk: Float) {
        this.walk = walk
    }

    fun getRun(isConvertUnitOn: Boolean): Float {
        return if (isConvertUnitOn) {
            FitUtil.convertToMi(run)
        } else run
    }

    fun setRun(run: Float) {
        this.run = run
    }

    fun getCycle(isConvertUnitOn: Boolean): Float {
        return if (isConvertUnitOn) {
            FitUtil.convertToMi(cycle)
        } else cycle
    }

    fun setCycle(cycle: Float) {
        this.cycle = cycle
    }
}
