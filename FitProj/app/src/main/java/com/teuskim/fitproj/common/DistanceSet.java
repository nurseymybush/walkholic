package com.teuskim.fitproj.common;

/**
 * 거리 정보
 */
public class DistanceSet {

    private float walk;
    private float run;
    private float cycle;

    public float getWalk(boolean isConvertUnitOn) {
        if (isConvertUnitOn) {
            return FitUtil.convertToMi(walk);
        }
        return walk;
    }

    public void setWalk(float walk) {
        this.walk = walk;
    }

    public float getRun(boolean isConvertUnitOn) {
        if (isConvertUnitOn) {
            return FitUtil.convertToMi(run);
        }
        return run;
    }

    public void setRun(float run) {
        this.run = run;
    }

    public float getCycle(boolean isConvertUnitOn) {
        if (isConvertUnitOn) {
            return FitUtil.convertToMi(cycle);
        }
        return cycle;
    }

    public void setCycle(float cycle) {
        this.cycle = cycle;
    }
}
