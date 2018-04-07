package citylink.com.applogcatloglibrary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Amritesh Sinha on 4/3/2018.
 */

public class DBHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "applogcatlog.db";
    public static final String TABLE_SAVE = "save";
    public static final String COLUMN_ID = "_ids";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_DATETIME = "date";
    public static final String COLUMN_REQUESTSTRING = "requestString";

    public DBHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_SAVE + "("
                + COLUMN_ID + "  INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_STATUS + " TEXT," + COLUMN_REQUESTSTRING + "  TEXT," + COLUMN_DATETIME + "TEXT" + ")";

        db.execSQL(CREATE_PRODUCTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAVE);
        onCreate(db);
    }

    public void insertData(SaveValues saveValues) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_REQUESTSTRING, saveValues.getRequestString().toString());
        //Log.v("TestValues", "" + values);
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TABLE_SAVE, null, values);
        db.close();
    }

    public boolean deleteData(String id) {
        boolean result = false;
        try {
            String query = "Select * FROM " + TABLE_SAVE + " WHERE " + COLUMN_ID + " =  \"" + id + "\"";
            SQLiteDatabase db = this.getWritableDatabase();
            Cursor cursor = db.rawQuery(query, null);
            Packet packet = new Packet();
            if (cursor.moveToFirst()) {
                packet.setId(cursor.getString(0));
                db.delete(TABLE_SAVE, COLUMN_ID + " = ?", new String[]{String.valueOf(packet.getId())});
                cursor.close();
                result = true;
            }
            db.close();

        }
        catch (IllegalStateException e){
            e.printStackTrace();
        }
        return result;
    }

    Cursor cursor;
    public List<SaveValues> getPackets() {
        List<SaveValues> saveValuesList;
        saveValuesList = new ArrayList<>();
        String query = "Select * FROM " + TABLE_SAVE + "  limit 10";//+ " WHERE " + COLUMN_STATUSPACKET + " Like \"" + packetname + "\"";
        SQLiteDatabase db = this.getWritableDatabase();
        if(db.isOpen()) {
            cursor = db.rawQuery(query, null);
            try {
                if (cursor.moveToFirst()) {
                    do {
                        try {
                            saveValuesList.add(new SaveValues(cursor.getString(0), new JSONObject(cursor.getString(2))));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } while (cursor.moveToNext());
                    cursor.close();
                } else {
                    saveValuesList = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            db.close();
        }
        return saveValuesList;
    }

}
