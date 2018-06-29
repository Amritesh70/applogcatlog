package citylink.com.applogcatloglibrary;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Amritesh Sinha on 4/3/2018.
 */

public class MyLogcat extends Service{
    private static MyLogcat mInstance;
    public static Context mCtx;
    static Timer timer;
    static TimerTask timerTask;
    static final Handler handler = new Handler();
    static DBHandler dbHandler;
    static List<SaveValues> saveValuesList;
    public static String IMEINO1;
    static JSONObject jsonObject;
    static CheckNetwork checkNetwork;
    private static Connection connection;
    private static Channel channel;
    static String applicationPackageName;
    static Context context;
    AppDeviceInfo appDeviceInfo;

    public static void init(Context ctx,String packageName){
        Log.v("My Application Is ","Here1 : "+packageName);
        applicationPackageName = packageName;
        context = ctx;
        new MyLogcat();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mCtx=getApplicationContext();
        checkNetwork = new CheckNetwork(mCtx);
        dbHandler = new DBHandler(mCtx, null, null, 1);
        appDeviceInfo = PrefUtilsInfo.getAppDeviceInfo(getApplicationContext());
        setupConnectionFactory();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("Coming Inside","onStartCommand");
        execute();
        return START_STICKY;
    }


    public MyLogcat() {
        if(context!=null) {
            Intent intent = new Intent(context, MyLogcat.class);
            context.startService(intent);
        }
    }

    public void execute(){
        Log.v("Coming Inside","execute");
        checkNetwork = new CheckNetwork(mCtx);
        dbHandler = new DBHandler(mCtx, null, null, 1);
        startTimer();
    }

    static ConnectionFactory factory = new ConnectionFactory();
    private static void setupConnectionFactory() {
        try {
            factory.setUsername("contract");
            factory.setPassword("contract");
            factory.setVirtualHost("61.16.137.43");
            factory.setHost("61.16.137.43");

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 0000, 10000);
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            if(appDeviceInfo==null) {
                                appDeviceInfo = new AppDeviceInfo();
                                getAllApplicationPackageInformation();
                            }

                            Process process = Runtime.getRuntime().exec("logcat -d");

                            BufferedReader bufferedReader = new BufferedReader(
                                    new InputStreamReader(process.getInputStream()));

                            StringBuilder log = new StringBuilder();
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                log.append(line);
                            }
                            if(log.length()>0) {
                                batteryLevel();
                                appDeviceInfo = PrefUtilsInfo.getAppDeviceInfo(MyLogcat.this);
                                String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                                LinkedHashMap<String, String> params = new LinkedHashMap<>();
                                params.put("logString", log.toString());
                                params.put("packageName",appDeviceInfo.getPackageName());
                                params.put("appName",appDeviceInfo.getAppName());
                                params.put("appVersion",appDeviceInfo.getAppVersion());
                                params.put("apiLevel",appDeviceInfo.getApiLevel());
                                params.put("manufacturer",appDeviceInfo.getManufacturer());
                                params.put("deviceModel",appDeviceInfo.getDeviceModel());
                                params.put("androidOS",appDeviceInfo.getAndroidOS());
                                params.put("brand",appDeviceInfo.getBrand());
                                params.put("unitno", appDeviceInfo.getUnitNo());
                                params.put("carrierName", appDeviceInfo.getCarrierName());
                                params.put("batteryLevel", batteryLevel);
                                params.put("currenttime", currentDateTimeString);
                                params.put("chargePlug", String.valueOf(chargePlug));

                                jsonObject = new JSONObject(params);
                                savePollingDateToDB(jsonObject);
                            }
                            if (checkNetwork.haveNetworkConnection()) {
                                saveValuesList = new ArrayList<>();
                                saveValuesList = dbHandler.getPackets();
                                if (saveValuesList != null) {
                                    if (saveValuesList.size() != 0) {
                                        new ConnectToRabbitMQTask().execute();
                                    }
                                }
                            }
                            clearLog();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
    }

    public static void clearLog(){
        try {
            Process process = new ProcessBuilder()
                    .command("logcat", "-c")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
        }
    }

    private static void savePollingDateToDB(JSONObject jsonObject) {
        //Log.v("AppLogcatLogs","savePollingDataToDB : ");
        SaveValues saveValues = new SaveValues(jsonObject);
        dbHandler.insertData(saveValues);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    String carrierName;
    private void getAllApplicationPackageInformation() {
        try {
            getID();
            TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            if(manager!=null) {
                carrierName = manager.getNetworkOperatorName();
            }

            PackageInfo pInfo = this.getPackageManager().getPackageInfo(applicationPackageName, PackageManager.GET_META_DATA);

            appDeviceInfo.setPackageName(pInfo.packageName);
            appDeviceInfo.setAppName(pInfo.packageName);
            appDeviceInfo.setAppVersion(pInfo.versionName);
            appDeviceInfo.setApiLevel(String.valueOf(android.os.Build.VERSION.SDK_INT));
            appDeviceInfo.setManufacturer(Build.MANUFACTURER);
            appDeviceInfo.setDeviceModel(Build.MODEL);
            appDeviceInfo.setAndroidOS(android.os.Build.VERSION.RELEASE);
            appDeviceInfo.setBrand(Build.BRAND);
            appDeviceInfo.setUnitNo(IMEINO1);
            appDeviceInfo.setCarrierName(carrierName);

            PrefUtilsInfo.setAppDeviceInfo(appDeviceInfo,MyLogcat.this);

        } catch (PackageManager.NameNotFoundException e1) {
            Log.e(this.getClass().getSimpleName(), "Name not found", e1);
        }
    }

    public static class ConnectToRabbitMQTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            try {
                connection = factory.newConnection();
                channel = connection.createChannel();
                channel.basicQos(1);
                channel.queueDeclare().getQueue();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                sendLogDatatoRabbitMq();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendLogDatatoRabbitMq() {
        try {
            if(saveValuesList!=null) {
                for (int i = 0; i < saveValuesList.size(); i++) {
                    channel.basicPublish("LogData", "AppLogcatLogs", null, saveValuesList.get(i).getRequestString().toString().getBytes());
                    dbHandler.deleteData(saveValuesList.get(i).getId());
                }
                saveValuesList = dbHandler.getPackets();

                if (channel != null) {
                    if (channel.isOpen()) {
                        channel.close();
                    }
                }
                if (connection != null) {
                    if (connection.isOpen()) {
                        connection.close();
                    }
                }
            }
            else {
                saveValuesList=dbHandler.getPackets();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private Thread.UncaughtExceptionHandler handleAppCrash = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    ex.printStackTrace();
                    batteryLevel();
                    appDeviceInfo = PrefUtilsInfo.getAppDeviceInfo(MyLogcat.this);
                    StringWriter errors = new StringWriter();
                    ex.printStackTrace(new PrintWriter(errors));
                    LinkedHashMap<String, String> params = new LinkedHashMap<>();
                    params.put("logString", errors.toString());
                    params.put("packageName",appDeviceInfo.getPackageName());
                    params.put("appName",appDeviceInfo.getAppName());
                    params.put("appVersion",appDeviceInfo.getAppVersion());
                    params.put("apiLevel",appDeviceInfo.getApiLevel());
                    params.put("manufacturer",appDeviceInfo.getManufacturer());
                    params.put("deviceModel",appDeviceInfo.getDeviceModel());
                    params.put("androidOS",appDeviceInfo.getAndroidOS());
                    params.put("brand",appDeviceInfo.getBrand());
                    params.put("unitno", appDeviceInfo.getUnitNo());
                    params.put("carrierName", appDeviceInfo.getCarrierName());
                    params.put("batteryLevel", batteryLevel);

                    String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                    params.put("currenttime", currentDateTimeString);
                    params.put("chargePlug", String.valueOf(chargePlug));

                    jsonObject = new JSONObject(params);
                    savePollingDateToDB(jsonObject);
                }
            };

    @TargetApi(23)
    void getID() {
        try {
            TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (manager != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    IMEINO1 = manager.getDeviceId(0);
                }
                else {
                    IMEINO1 = manager.getDeviceId();
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    BroadcastReceiver batteryLevelReceiver;
    int count=0;
    static String batteryLevel;
    static int chargePlug;
    void batteryLevel(){
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.registerReceiver(batteryLevelReceiver, batteryLevelFilter);
        batteryLevelReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int level = -1;
                if (rawlevel >= 0 && scale > 0)
                    level = (rawlevel * 100) / scale;

                batteryLevel = String.valueOf(level);
                chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            }
        };
    }
}
