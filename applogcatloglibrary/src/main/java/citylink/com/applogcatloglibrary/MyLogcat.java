package citylink.com.applogcatloglibrary;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
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

    @Override
    public void onCreate() {
        super.onCreate();
        mCtx=getApplicationContext();
        checkNetwork = new CheckNetwork(mCtx);
        dbHandler = new DBHandler(mCtx, null, null, 1);
        setupConnectionFactory();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("Coming Inside","onStartCommand");
        execute();
        return START_STICKY;
    }

    public MyLogcat() {

    }

    public void execute(){
        Log.v("Coming Inside","execute");
        checkNetwork = new CheckNetwork(mCtx);
        dbHandler = new DBHandler(mCtx, null, null, 1);
        startTimer();
       //Thread.setDefaultUncaughtExceptionHandler(handleAppCrash);
    }

    static ConnectionFactory factory = new ConnectionFactory();
    private static void setupConnectionFactory() {
        try {
            factory.setUsername("contract");
            factory.setPassword("contract");
            factory.setVirtualHost("192.168.0.246");
            factory.setHost("192.168.0.246");
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
                            Process process = Runtime.getRuntime().exec("logcat -d");

                            BufferedReader bufferedReader = new BufferedReader(
                                    new InputStreamReader(process.getInputStream()));

                            StringBuilder log = new StringBuilder();
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                log.append(line);
                            }
                            if(log.length()>0) {
                                getID();
                                String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                                LinkedHashMap<String, String> params = new LinkedHashMap<>();
                                params.put("logString", log.toString());
                                params.put("unitno", IMEINO1);
                                params.put("time", currentDateTimeString);
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
        catch (Exception e){
            e.printStackTrace();
        }
        //saveValuesList = dbHandler.getPackets();
    }

    private static Thread.UncaughtExceptionHandler handleAppCrash = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    ex.printStackTrace();
                    Toast.makeText(mCtx,"Coming Inside UncaughtExceptionHandler",Toast.LENGTH_SHORT).show();
                    StringWriter errors = new StringWriter();
                    ex.printStackTrace(new PrintWriter(errors));
                    LinkedHashMap<String, String> params = new LinkedHashMap<>();
                    params.put("logString", errors.toString());
                    params.put("unitno", IMEINO1);
                    jsonObject = new JSONObject(params);
                    savePollingDateToDB(jsonObject);
                }
            };

    @TargetApi(23)
    void getID() {
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
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
        IMEINO1 = manager.getDeviceId(0);
    }
}
