package com.teuskim.fitproj.common;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 로컬 데이터베이스
 */
public class FitDao {

    private static final String DATABASE_NAME = "fitproj.db";

    private static final int DATABASE_VERSION = 1;
    private static FitDao instance;

    private Context context;
    private SQLiteDatabase db;

    private FitDao(Context context) {
        this.context = context;
    }

    public synchronized static FitDao getInstance(Context context) {

        if (instance != null && instance.db != null) {
            return instance;
        }

        instance = new FitDao(context);
        if (instance.open() == false) {
            instance = null;
        }

        return instance;
    }

    private boolean open() {
        FitSQLiteOpenHelper dbHelper;
        dbHelper = new FitSQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);

        // 간헐적으로 발생하는 비정상 종료 문제 때문에 아래와 같이 조치한다.
        try {
            db = dbHelper.getWritableDatabase();
            if (db == null) {
                db = dbHelper.getWritableDatabase();
            }
        } catch (Exception e) {

            // SQLiteDatabase.dbopen() 방어 코드
            File dbDir = new File(context.getApplicationInfo().dataDir + "/databases");
            dbDir.mkdirs();

            db = dbHelper.getWritableDatabase();
        }

        return (db == null) ? false : true;
    }

    /**
     * FitApplication의 onTerminate()에서만 호출된다.
     */
    public void close() {
        db.close();
    }

    public static class FitSQLiteOpenHelper extends SQLiteOpenHelper {

        public FitSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(GoalTable.CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 필요시 추가
        }
    }


    /**
     * 목표 테이블
     */
    public static class GoalTable {

        public static final String TABLE_NAME = "goals";

        public static final String _ID = "_id";
        public static final String TYPE = "type";
        public static final String AMOUNT = "amount";
        public static final String AMOUNT_UNIT = "amount_unit";
        public static final String SUN = "sun";
        public static final String MON = "mon";
        public static final String TUE = "tue";
        public static final String WED = "wed";
        public static final String THU = "thu";
        public static final String FRI = "fri";
        public static final String SAT = "sat";
        public static final String CRTDT = "crtdt";
        public static final String UPTDT = "uptdt";

        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "( "
                + _ID + " INTEGER primary key autoincrement, "
                + TYPE + " INTEGER,"
                + AMOUNT + " NUMERIC,"
                + AMOUNT_UNIT + " TEXT,"
                + SUN + " INTEGER," + MON + " INTEGER," + TUE + " INTEGER," + WED + " INTEGER," + THU + " INTEGER," + FRI + " INTEGER," + SAT + " INTEGER,"
                + CRTDT + " INTEGER default 0,"
                + UPTDT + " INTEGER default 0);";
    }

    public static final int TYPE_GOAL_WALKING = 0;
    public static final int TYPE_GOAL_RUNNING = 1;
    public static final int TYPE_GOAL_CYCLING = 2;

    /**
     * 목표 추가
     */
    public boolean insertGoal(int type, float amount, String amountUnit, boolean[] whatDays) {
        long dt = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(GoalTable.TYPE, type);
        values.put(GoalTable.AMOUNT, amount);
        values.put(GoalTable.AMOUNT_UNIT, amountUnit);
        values.put(GoalTable.SUN, whatDays[0] ? 1 : 0);
        values.put(GoalTable.MON, whatDays[1] ? 1 : 0);
        values.put(GoalTable.TUE, whatDays[2] ? 1 : 0);
        values.put(GoalTable.WED, whatDays[3] ? 1 : 0);
        values.put(GoalTable.THU, whatDays[4] ? 1 : 0);
        values.put(GoalTable.FRI, whatDays[5] ? 1 : 0);
        values.put(GoalTable.SAT, whatDays[6] ? 1 : 0);
        values.put(GoalTable.CRTDT, dt);
        values.put(GoalTable.UPTDT, dt);
        return (db.insert(GoalTable.TABLE_NAME, null, values) > 0);
    }

    /**
     * 조건에 맞는 목표들 가져오기
     */
    private List<Goal> getGoalList(String selection, String[] selectionArgs) {
        List<Goal> list = new ArrayList<Goal>();
        Cursor cursor = db.query(GoalTable.TABLE_NAME
                , new String[]{ GoalTable._ID,GoalTable.TYPE,GoalTable.AMOUNT,GoalTable.AMOUNT_UNIT
                    ,GoalTable.SUN,GoalTable.MON,GoalTable.TUE,GoalTable.WED,GoalTable.THU,GoalTable.FRI,GoalTable.SAT,GoalTable.CRTDT }
                , selection, selectionArgs
                , null, null
                , GoalTable.UPTDT+" DESC");

        if (cursor.moveToFirst()) {
            do {
                Goal g = new Goal();
                g.setId(cursor.getInt(0));
                g.setType(cursor.getInt(1));
                g.setAmount(cursor.getFloat(2));
                g.setAmountUnit(cursor.getString(3));
                boolean[] whatDays = new boolean[7];
                for (int i=0; i<7; i++) {
                    whatDays[i] = (cursor.getInt(i+4)==1);
                }
                g.setWhatDays(whatDays);
                g.setCrtDt(cursor.getLong(11));
                list.add(g);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return list;
    }

    /**
     * 모든 목표들 가져오기
     */
    public List<Goal> getGoalList() {
        return getGoalList(null, null);
    }

    /**
     * 해당 요일에 달성해야할 목표들 가져오기
     * @param weekDayPosition 일요일(0)~토요일(6)
     */
    public List<Goal> getGoalList(int weekDayPosition) {
        String[] daysArr = {GoalTable.SUN,GoalTable.MON,GoalTable.TUE,GoalTable.WED,GoalTable.THU,GoalTable.FRI,GoalTable.SAT};
        String selection = daysArr[weekDayPosition]+"=?";
        String[] selectionArgs = {"1"};
        return getGoalList(selection, selectionArgs);
    }

    /**
     * 목표 업데이트하기
     */
    public boolean updateGoal(Goal g, boolean isConvertUnitOn) {
        ContentValues values = new ContentValues();
        values.put(GoalTable.TYPE, g.getTypeInt());
        values.put(GoalTable.AMOUNT, g.getAmount(isConvertUnitOn));
        values.put(GoalTable.AMOUNT_UNIT, g.getAmountUnit(isConvertUnitOn));
        values.put(GoalTable.SUN, g.isCheckedDay(0) ? 1 : 0);
        values.put(GoalTable.MON, g.isCheckedDay(1) ? 1 : 0);
        values.put(GoalTable.TUE, g.isCheckedDay(2) ? 1 : 0);
        values.put(GoalTable.WED, g.isCheckedDay(3) ? 1 : 0);
        values.put(GoalTable.THU, g.isCheckedDay(4) ? 1 : 0);
        values.put(GoalTable.FRI, g.isCheckedDay(5) ? 1 : 0);
        values.put(GoalTable.SAT, g.isCheckedDay(6) ? 1 : 0);
        values.put(GoalTable.UPTDT, System.currentTimeMillis());

        String whereClause = GoalTable._ID+"=?";
        String[] whereArgs = {""+g.getId()};

        return (db.update(GoalTable.TABLE_NAME, values, whereClause, whereArgs) > 0);
    }

    /**
     * 목표 삭제하기
     */
    public boolean deleteGoal(int id) {
        String whereClause = GoalTable._ID+"=?";
        String[] whereArgs = {""+id};

        return (db.delete(GoalTable.TABLE_NAME, whereClause, whereArgs) > 0);
    }
}
