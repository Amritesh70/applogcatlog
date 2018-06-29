package citylink.com.applogcatlog;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;

import citylink.com.applogcatloglibrary.MyLogcat;

public class MainActivity extends AppCompatActivity {
    String unitNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getID();
        Log.v("My Application Is ","Here : "+BuildConfig.APPLICATION_ID);
        MyLogcat.init(this,BuildConfig.APPLICATION_ID);
    }

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
                    unitNo = manager.getDeviceId(0);
                }
                else {
                    unitNo = manager.getDeviceId();
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
