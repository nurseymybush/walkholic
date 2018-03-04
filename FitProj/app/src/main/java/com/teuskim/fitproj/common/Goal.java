package com.teuskim.fitproj.common;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.teuskim.fitproj.R;

/**
 * 목표 클래스
 * FitDao의 GoalTable에 저장된 데이터
 */
public class Goal implements Parcelable {

    public static enum Type {
        WALK, RUN, CYCLE
    }

    private int id;
    private Type type;
    private float amount;
    private String amountUnit;
    private boolean[] whatDays;
    private float currAmount;
    private long crtDt;

    public Goal() {}

    public Goal(Type type, float amount, float currAmount) {
        this.type = type;
        this.amount = amount;
        this.currAmount = currAmount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public String getTypeText(Context context) {
        switch (type) {
            case WALK: return context.getString(R.string.text_walk);
            case RUN: return context.getString(R.string.text_run);
            case CYCLE: return context.getString(R.string.text_cycle);
        }
        return toString();
    }

    public int getTypeInt() {
        switch (type) {
            case WALK: return 0;
            case RUN: return 1;
            case CYCLE: return 2;
        }
        return 0;
    }

    public void setType(int type) {
        switch (type) {
            case 0: this.type = Type.WALK; break;
            case 1: this.type = Type.RUN; break;
            case 2: this.type = Type.CYCLE; break;
        }
    }

    public float getAmount(boolean isConverted) {
        if (isConverted) {
            return (float)(amount * 0.621371192);
        }
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public String getAmountUnit(boolean isConverted) {
        if (isConverted && amountUnit.equals("km")) {
            return "mi";
        }
        return amountUnit;
    }

    public void setAmountUnit(String amountUnit) {
        this.amountUnit = amountUnit;
    }

    public void setWhatDays(boolean[] whatDays) {
        this.whatDays = whatDays;
    }

    public boolean isCheckedDay(int dayPosition) {
        return whatDays[dayPosition];
    }

    public String getWhatDaysText(Context context) {
        String whatDaysText;
        if (checkWeekend()) {
            if (checkWeekdays()) {
                whatDaysText = context.getString(R.string.daily);
            } else {
                whatDaysText = context.getString(R.string.weekend);
            }
        } else {
            if (checkWeekdays()) {
                whatDaysText = context.getString(R.string.weekdays);
            } else {
                whatDaysText = "";
                String[] days = {","+context.getString(R.string.sun)
                                ,","+context.getString(R.string.mon)
                                ,","+context.getString(R.string.tue)
                                ,","+context.getString(R.string.wed)
                                ,","+context.getString(R.string.thu)
                                ,","+context.getString(R.string.fri)
                                ,","+context.getString(R.string.sat)};
                for (int i=0; i<7; i++) {
                    if (whatDays[i]) {
                        whatDaysText += days[i];
                    }
                }
                if (whatDaysText.length() > 1) {
                    whatDaysText = whatDaysText.substring(1);
                }
            }
        }
        return whatDaysText;
    }

    private boolean checkWeekend() {
        return (whatDays[0] && whatDays[6]);
    }

    private boolean checkWeekdays() {
        return (whatDays[1] && whatDays[2] && whatDays[3] && whatDays[4] && whatDays[5]);
    }

    public float getCurrAmount(boolean isConverted) {
        if (isConverted) {
            return (float)(currAmount * 0.621371192);
        }
        return currAmount;
    }

    public void setCurrAmount(float currAmount) {
        this.currAmount = currAmount;
    }

    public float getCurrAmountRatio() {
        return currAmount / amount;
    }

    public long getCrtDt() {
        return crtDt;
    }

    public void setCrtDt(long crtDt) {
        this.crtDt = crtDt;
    }

    public int getColor() {
        return getColor(type);
    }

    public static int getColor(Type type) {
        switch (type) {
            case WALK:default: return 0xffffcc27;
            case RUN: return 0xff61d0e3;
            case CYCLE: return 0xfff26f48;
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getTypeInt());
        dest.writeFloat(amount);
        dest.writeString(amountUnit);
        dest.writeBooleanArray(whatDays);
        dest.writeFloat(currAmount);
        dest.writeLong(crtDt);
    }

    public static final Creator CREATOR = new Creator<Goal>() {
        @Override
        public Goal createFromParcel(Parcel source) {
            Goal g = new Goal();
            g.setType(source.readInt());
            g.setAmount(source.readFloat());
            g.setAmountUnit(source.readString());
            boolean[] arr = new boolean[7];
            source.readBooleanArray(arr);
            g.setWhatDays(arr);
            g.setCurrAmount(source.readFloat());
            g.setCrtDt(source.readLong());
            return g;
        }

        @Override
        public Goal[] newArray(int size) {
            return new Goal[size];
        }
    };
}
