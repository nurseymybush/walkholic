package com.teuskim.fitproj.common

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

import com.teuskim.fitproj.R

/**
 * 목표 클래스
 * FitDao의 GoalTable에 저장된 데이터
 */
class Goal : Parcelable {

    var id: Int = 0
    var type: Type? = null
        private set
    private var amount: Float = 0.toFloat()
    private var amountUnit: String? = null
    private var whatDays: BooleanArray? = null
    private var currAmount: Float = 0.toFloat()
    var crtDt: Long = 0

    val typeInt: Int
        get() {
            when (type) {
                Goal.Type.WALK -> return 0
                Goal.Type.RUN -> return 1
                Goal.Type.CYCLE -> return 2
            }
            return 0
        }

    val currAmountRatio: Float
        get() = currAmount / amount

    val color: Int
        get() = getColor(type)

    enum class Type {
        WALK, RUN, CYCLE
    }

    constructor() {}

    constructor(type: Type, amount: Float, currAmount: Float) {
        this.type = type
        this.amount = amount
        this.currAmount = currAmount
    }

    fun getTypeText(context: Context): String {
        when (type) {
            Goal.Type.WALK -> return context.getString(R.string.text_walk)
            Goal.Type.RUN -> return context.getString(R.string.text_run)
            Goal.Type.CYCLE -> return context.getString(R.string.text_cycle)
        }
        return toString()
    }

    fun setType(type: Int) {
        when (type) {
            0 -> this.type = Type.WALK
            1 -> this.type = Type.RUN
            2 -> this.type = Type.CYCLE
        }
    }

    fun getAmount(isConverted: Boolean): Float {
        return if (isConverted) {
            (amount * 0.621371192).toFloat()
        } else amount
    }

    fun setAmount(amount: Float) {
        this.amount = amount
    }

    fun getAmountUnit(isConverted: Boolean): String? {
        return if (isConverted && amountUnit == "km") {
            "mi"
        } else amountUnit
    }

    fun setAmountUnit(amountUnit: String) {
        this.amountUnit = amountUnit
    }

    fun setWhatDays(whatDays: BooleanArray) {
        this.whatDays = whatDays
    }

    fun isCheckedDay(dayPosition: Int): Boolean {
        return whatDays!![dayPosition]
    }

    fun getWhatDaysText(context: Context): String {
        var whatDaysText: String
        if (checkWeekend()) {
            if (checkWeekdays()) {
                whatDaysText = context.getString(R.string.daily)
            } else {
                whatDaysText = context.getString(R.string.weekend)
            }
        } else {
            if (checkWeekdays()) {
                whatDaysText = context.getString(R.string.weekdays)
            } else {
                whatDaysText = ""
                val days = arrayOf("," + context.getString(R.string.sun), "," + context.getString(R.string.mon), "," + context.getString(R.string.tue), "," + context.getString(R.string.wed), "," + context.getString(R.string.thu), "," + context.getString(R.string.fri), "," + context.getString(R.string.sat))
                for (i in 0..6) {
                    if (whatDays!![i]) {
                        whatDaysText += days[i]
                    }
                }
                if (whatDaysText.length > 1) {
                    whatDaysText = whatDaysText.substring(1)
                }
            }
        }
        return whatDaysText
    }

    private fun checkWeekend(): Boolean {
        return whatDays!![0] && whatDays!![6]
    }

    private fun checkWeekdays(): Boolean {
        return whatDays!![1] && whatDays!![2] && whatDays!![3] && whatDays!![4] && whatDays!![5]
    }

    fun getCurrAmount(isConverted: Boolean): Float {
        return if (isConverted) {
            (currAmount * 0.621371192).toFloat()
        } else currAmount
    }

    fun setCurrAmount(currAmount: Float) {
        this.currAmount = currAmount
    }


    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(typeInt)
        dest.writeFloat(amount)
        dest.writeString(amountUnit)
        dest.writeBooleanArray(whatDays)
        dest.writeFloat(currAmount)
        dest.writeLong(crtDt)
    }

    companion object {

        fun getColor(type: Type?): Int {
            when (type) {
                Goal.Type.WALK -> return -0x33d9
                Goal.Type.RUN -> return -0x9e2f1d
                Goal.Type.CYCLE -> return -0xd90b8
                else -> return -0x33d9
            }
        }

        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<Goal> {
            override fun createFromParcel(source: Parcel): Goal {
                val g = Goal()
                g.setType(source.readInt())
                g.setAmount(source.readFloat())
                g.setAmountUnit(source.readString())
                val arr = BooleanArray(7)
                source.readBooleanArray(arr)
                g.setWhatDays(arr)
                g.setCurrAmount(source.readFloat())
                g.crtDt = source.readLong()
                return g
            }

            override fun newArray(size: Int): Array<Goal?> {
                return arrayOfNulls(size)
            }
        }
    }
}
