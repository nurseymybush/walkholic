package com.teuskim.fitproj.common

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.util.*

/**
 * 로컬 데이터베이스
 */
class FitDao private constructor(private val context: Context) {
    private var db: SQLiteDatabase? = null

    /**
     * 모든 목표들 가져오기
     */
    val goalList: List<Goal>
        get() = getGoalList(null, null)

    private fun open(): Boolean {
        val dbHelper: FitSQLiteOpenHelper
        dbHelper = FitSQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION)

        // 간헐적으로 발생하는 비정상 종료 문제 때문에 아래와 같이 조치한다.
        try {
            db = dbHelper.writableDatabase
            if (db == null) {
                db = dbHelper.writableDatabase
            }
        } catch (e: Exception) {

            // SQLiteDatabase.dbopen() 방어 코드
            val dbDir = File(context.applicationInfo.dataDir + "/databases")
            dbDir.mkdirs()

            db = dbHelper.writableDatabase
        }

        return if (db == null) false else true
    }

    /**
     * FitApplication의 onTerminate()에서만 호출된다.
     */
    fun close() {
        db!!.close()
    }

    class FitSQLiteOpenHelper(context: Context, name: String, factory: SQLiteDatabase.CursorFactory?, version: Int) : SQLiteOpenHelper(context, name, factory, version) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(GoalTable.CREATE_TABLE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // 필요시 추가
        }
    }


    /**
     * 목표 테이블
     */
    object GoalTable {

        val TABLE_NAME = "goals"

        val _ID = "_id"
        val TYPE = "type"
        val AMOUNT = "amount"
        val AMOUNT_UNIT = "amount_unit"
        val SUN = "sun"
        val MON = "mon"
        val TUE = "tue"
        val WED = "wed"
        val THU = "thu"
        val FRI = "fri"
        val SAT = "sat"
        val CRTDT = "crtdt"
        val UPTDT = "uptdt"

        val CREATE_TABLE = ("CREATE TABLE " + TABLE_NAME + "( "
                + _ID + " INTEGER primary key autoincrement, "
                + TYPE + " INTEGER,"
                + AMOUNT + " NUMERIC,"
                + AMOUNT_UNIT + " TEXT,"
                + SUN + " INTEGER," + MON + " INTEGER," + TUE + " INTEGER," + WED + " INTEGER," + THU + " INTEGER," + FRI + " INTEGER," + SAT + " INTEGER,"
                + CRTDT + " INTEGER default 0,"
                + UPTDT + " INTEGER default 0);")
    }

    /**
     * 목표 추가
     */
    fun insertGoal(type: Int, amount: Float, amountUnit: String, whatDays: BooleanArray): Boolean {
        val dt = System.currentTimeMillis()
        val values = ContentValues()
        values.put(GoalTable.TYPE, type)
        values.put(GoalTable.AMOUNT, amount)
        values.put(GoalTable.AMOUNT_UNIT, amountUnit)
        values.put(GoalTable.SUN, if (whatDays[0]) 1 else 0)
        values.put(GoalTable.MON, if (whatDays[1]) 1 else 0)
        values.put(GoalTable.TUE, if (whatDays[2]) 1 else 0)
        values.put(GoalTable.WED, if (whatDays[3]) 1 else 0)
        values.put(GoalTable.THU, if (whatDays[4]) 1 else 0)
        values.put(GoalTable.FRI, if (whatDays[5]) 1 else 0)
        values.put(GoalTable.SAT, if (whatDays[6]) 1 else 0)
        values.put(GoalTable.CRTDT, dt)
        values.put(GoalTable.UPTDT, dt)
        return db!!.insert(GoalTable.TABLE_NAME, null, values) > 0
    }

    /**
     * 조건에 맞는 목표들 가져오기
     */
    private fun getGoalList(selection: String?, selectionArgs: Array<String>?): List<Goal> {
        val list = ArrayList<Goal>()
        val cursor = db!!.query(GoalTable.TABLE_NAME, arrayOf(GoalTable._ID, GoalTable.TYPE, GoalTable.AMOUNT, GoalTable.AMOUNT_UNIT, GoalTable.SUN, GoalTable.MON, GoalTable.TUE, GoalTable.WED, GoalTable.THU, GoalTable.FRI, GoalTable.SAT, GoalTable.CRTDT), selection, selectionArgs, null, null, GoalTable.UPTDT + " DESC")

        if (cursor.moveToFirst()) {
            do {
                val g = Goal()
                g.id = cursor.getInt(0)
                g.setType(cursor.getInt(1))
                g.setAmount(cursor.getFloat(2))
                g.setAmountUnit(cursor.getString(3))
                val whatDays = BooleanArray(7)
                for (i in 0..6) {
                    whatDays[i] = cursor.getInt(i + 4) == 1
                }
                g.setWhatDays(whatDays)
                g.crtDt = cursor.getLong(11)
                list.add(g)
            } while (cursor.moveToNext())
        }
        cursor.close()

        return list
    }

    /**
     * 해당 요일에 달성해야할 목표들 가져오기
     * @param weekDayPosition 일요일(0)~토요일(6)
     */
    fun getGoalList(weekDayPosition: Int): List<Goal> {
        val daysArr = arrayOf(GoalTable.SUN, GoalTable.MON, GoalTable.TUE, GoalTable.WED, GoalTable.THU, GoalTable.FRI, GoalTable.SAT)
        val selection = daysArr[weekDayPosition] + "=?"
        val selectionArgs = arrayOf("1")
        return getGoalList(selection, selectionArgs)
    }

    /**
     * 목표 업데이트하기
     */
    fun updateGoal(g: Goal, isConvertUnitOn: Boolean): Boolean {
        val values = ContentValues()
        values.put(GoalTable.TYPE, g.typeInt)
        values.put(GoalTable.AMOUNT, g.getAmount(isConvertUnitOn))
        values.put(GoalTable.AMOUNT_UNIT, g.getAmountUnit(isConvertUnitOn))
        values.put(GoalTable.SUN, if (g.isCheckedDay(0)) 1 else 0)
        values.put(GoalTable.MON, if (g.isCheckedDay(1)) 1 else 0)
        values.put(GoalTable.TUE, if (g.isCheckedDay(2)) 1 else 0)
        values.put(GoalTable.WED, if (g.isCheckedDay(3)) 1 else 0)
        values.put(GoalTable.THU, if (g.isCheckedDay(4)) 1 else 0)
        values.put(GoalTable.FRI, if (g.isCheckedDay(5)) 1 else 0)
        values.put(GoalTable.SAT, if (g.isCheckedDay(6)) 1 else 0)
        values.put(GoalTable.UPTDT, System.currentTimeMillis())

        val whereClause = GoalTable._ID + "=?"
        val whereArgs = arrayOf("" + g.id)

        return db!!.update(GoalTable.TABLE_NAME, values, whereClause, whereArgs) > 0
    }

    /**
     * 목표 삭제하기
     */
    fun deleteGoal(id: Int): Boolean {
        val whereClause = GoalTable._ID + "=?"
        val whereArgs = arrayOf("" + id)

        return db!!.delete(GoalTable.TABLE_NAME, whereClause, whereArgs) > 0
    }

    companion object {

        private val DATABASE_NAME = "fitproj.db"

        private val DATABASE_VERSION = 1
        private var instance: FitDao? = null

        @Synchronized
        fun getInstance(context: Context): FitDao? {

            if (instance != null && instance!!.db != null) {
                return instance
            }

            instance = FitDao(context)
            if (instance!!.open() == false) {
                instance = null
            }

            return instance
        }

        val TYPE_GOAL_WALKING = 0
        val TYPE_GOAL_RUNNING = 1
        val TYPE_GOAL_CYCLING = 2
    }
}
