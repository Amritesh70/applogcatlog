package citylink.com.applogcatloglibrary;

import android.content.Context;

/**
 * Created by Amritesh Sinha on 6/28/2018.
 */
public class PrefUtilsInfo {

    public static void setAppDeviceInfo(AppDeviceInfo appDeviceInfo, Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "appdevice_prefs", 0);
        complexPreferences.putObject("app_device_info", appDeviceInfo);
        complexPreferences.commit();
    }

    public static AppDeviceInfo getAppDeviceInfo(Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "appdevice_prefs", 0);
        AppDeviceInfo appDeviceInfo = complexPreferences.getObject("app_device_info", AppDeviceInfo.class);
        return appDeviceInfo;
    }

    public static void clearAppDeviceInfo(Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "appdevice_prefs", 0);
        complexPreferences.clearObject();
        complexPreferences.commit();
    }
}
