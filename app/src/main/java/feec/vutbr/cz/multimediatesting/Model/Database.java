package feec.vutbr.cz.multimediatesting.Model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import feec.vutbr.cz.multimediatesting.Contract.ConnectionFragmentContract;
import feec.vutbr.cz.multimediatesting.Contract.HistoryActivityContract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class Database extends SQLiteOpenHelper implements HistoryActivityContract.Database, ConnectionFragmentContract.DatabaseModel {

    private Context mCtx;

    private static final int VERSION = 1;
    private static final String DB_NAME = "multimedia_testing";

    private static final String TABLE_MEASUREMENT = "measeure";
    private static final String TABLE_PACKETS = "packets";

    private static final String TABLE_MEASUREMENT_KEY_ID = "id";
    private static final String TABLE_MEASUREMENT_KEY_NAME = "name";
    private static final String TABLE_MEASUREMENT_KEY_CONNECTION_TYPE = "conn_type";
    private static final String TABLE_MEASUREMENT_KEY_SUBTYPE = "subtype";
    private static final String TABLE_MEASUREMENT_KEY_OPERATOR = "operator";
    private static final String TABLE_MEASUREMENT_KEY_PACKET_LOSS = "packet_loss";
    private static final String TABLE_MEASUREMENT_KEY_CREATED = "created";

    private static final String TABLE_PACKETS_KEY_ID = "id";
    private static final String TABLE_PACKETS_KEY_PARENT_ID = "parent_id";
    private static final String TABLE_PACKETS_KEY_SEQ_NUM = "seq_num";
    private static final String TABLE_PACKETS_KEY_TIME_SENT = "time_sent";
    private static final String TABLE_PACKETS_KEY_TIME_RECEIVED = "time_received";
    private static final String TABLE_PACKETS_KEY_DELAY = "delay";
    // private static final String TABLE_PACKETS_KEY_JITTER = "jitter";

    private static final String CREATE_PACKETS_TABLE = "CREATE TABLE " + TABLE_PACKETS + "("
            + TABLE_PACKETS_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TABLE_PACKETS_KEY_PARENT_ID + " INTEGER,"
            + TABLE_PACKETS_KEY_SEQ_NUM + " INTEGER,"
            + TABLE_PACKETS_KEY_TIME_SENT + " INTEGER,"
            + TABLE_PACKETS_KEY_TIME_RECEIVED + " INTEGER,"
            + TABLE_PACKETS_KEY_DELAY + " INTEGER)";
    //  + TABLE_PACKETS_KEY_JITTER + " INTEGER)";

    private static final String CREATE_MEASUREMENT_TABLE = "CREATE TABLE " + TABLE_MEASUREMENT + "("
            + TABLE_MEASUREMENT_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TABLE_MEASUREMENT_KEY_NAME + " TEXT,"
            + TABLE_MEASUREMENT_KEY_CONNECTION_TYPE + " TEXT,"
            + TABLE_MEASUREMENT_KEY_SUBTYPE + " TEXT,"
            + TABLE_MEASUREMENT_KEY_OPERATOR + " TEXT,"
            + TABLE_MEASUREMENT_KEY_PACKET_LOSS + " INTEGER,"
            + TABLE_MEASUREMENT_KEY_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

    private static final String REMOVE_MEASUREMENT = "DELETE FROM " + TABLE_MEASUREMENT + " WHERE id=";
    private static final String REMOVE_MEASUREMENT_PACKETS = "DELETE FROM " + TABLE_PACKETS + " WHERE parent_id=";

    private static final String GET_ALL_MEASUREMENTS = "SELECT " + TABLE_MEASUREMENT_KEY_NAME + "," + TABLE_MEASUREMENT_KEY_ID + " FROM " + TABLE_MEASUREMENT + " ORDER BY " + TABLE_MEASUREMENT_KEY_CREATED + " ASC";


    public Database(Context ctx) {
        super(ctx, DB_NAME, null, VERSION);
        mCtx = ctx;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MEASUREMENT_TABLE);
        db.execSQL(CREATE_PACKETS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEASUREMENT);
        db.execSQL("DROP TABLE IF EXISTS" + TABLE_PACKETS);
        onCreate(db);
    }


    @Override
    public long insertData(ConnectionFragmentContract.PacketModel packets) {
        ArrayList<Packet> sent = packets.getSent();
        ArrayList<Packet> received = packets.getReceived();

        long parentId = insertMeasurement(100 - packets.getPercentReceived(sent.size()));

        Collections.sort(received);
        for (int i = 0; i < received.size(); i++) {
            Packet receivedPacket = received.get(i);
            int seqNum = receivedPacket.getSeqNum();
            Packet sentPacket = sent.remove(seqNum);

            ContentValues values = new ContentValues();
            values.put(TABLE_PACKETS_KEY_PARENT_ID, parentId);
            values.put(TABLE_PACKETS_KEY_SEQ_NUM, seqNum);
            values.put(TABLE_PACKETS_KEY_TIME_SENT, sentPacket.getTimeStamp());
            values.put(TABLE_PACKETS_KEY_TIME_RECEIVED, receivedPacket.getTimeStamp());
            values.put(TABLE_PACKETS_KEY_DELAY, receivedPacket.getTimeStamp() - sentPacket.getTimeStamp());
        }

        for (int i = 0; i < sent.size(); i++) {
            Packet sentPacket = sent.get(i);
            int seqNum = sentPacket.getSeqNum();

            ContentValues values = new ContentValues();
            values.put(TABLE_PACKETS_KEY_PARENT_ID, parentId);
            values.put(TABLE_PACKETS_KEY_SEQ_NUM, seqNum);
            values.put(TABLE_PACKETS_KEY_TIME_SENT, sentPacket.getTimeStamp());
            values.put(TABLE_PACKETS_KEY_TIME_RECEIVED, 0);
            values.put(TABLE_PACKETS_KEY_DELAY, 0);
        }

        return parentId;
    }

    private long insertMeasurement(int packetLoss) {
        SQLiteDatabase db = getWritableDatabase();
        String connType = getConnectionType();
        String subConn = "";
        String operator = "";
        if (connType.equals("Mobile")) {
            subConn = getMobileNetworkType();
            operator = getOperatorName();
        } else if (connType.equals("WiFi")) {
            subConn = getSSID();
        }
        ContentValues values = new ContentValues();
        values.put(TABLE_MEASUREMENT_KEY_NAME, getMeasurementName());
        values.put(TABLE_MEASUREMENT_KEY_CONNECTION_TYPE, connType);
        values.put(TABLE_MEASUREMENT_KEY_SUBTYPE, subConn);
        values.put(TABLE_MEASUREMENT_KEY_OPERATOR, operator);
        values.put(TABLE_MEASUREMENT_KEY_PACKET_LOSS, packetLoss);

        return db.insert(TABLE_MEASUREMENT, null, values);
    }

    private String getMeasurementName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    private String getSSID() {
        WifiManager wifiMgr = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);
        return wifiMgr.getConnectionInfo().getSSID();
    }

    private String getOperatorName() {
        TelephonyManager telMgr = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);
        return telMgr.getNetworkOperatorName();
    }

    private String getMobileNetworkType() {
        TelephonyManager telMgr = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);
        switch (telMgr.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "Unknown";
        }
    }

    private String getConnectionType() {
        ConnectivityManager connMgr = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        switch (connMgr.getActiveNetworkInfo().getType()) {
            case ConnectivityManager.TYPE_WIMAX:
                return "WiMax";
            case ConnectivityManager.TYPE_WIFI:
                return "WiFi";
            case ConnectivityManager.TYPE_VPN:
                return "VPN";
            case ConnectivityManager.TYPE_MOBILE:
                return "Mobile";
            default:
                return "Other";
        }
    }


    @Override
    public ArrayList<HistoryItem> getMeasurements() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(GET_ALL_MEASUREMENTS, null);
        ArrayList<HistoryItem> list = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                HistoryItem measurement = new HistoryItem(cursor.getString(cursor.getColumnIndex(TABLE_MEASUREMENT_KEY_NAME)), cursor.getLong(cursor.getColumnIndex(TABLE_MEASUREMENT_KEY_ID)));
                list.add(measurement);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    @Override
    public void deleteMeasurement(long id) {
        SQLiteDatabase db = getWritableDatabase();
        String sId = DatabaseUtils.sqlEscapeString(String.valueOf(id));
        String query = REMOVE_MEASUREMENT_PACKETS + sId;
        db.execSQL(query);
        query = REMOVE_MEASUREMENT + sId;
        db.execSQL(query);
    }
}