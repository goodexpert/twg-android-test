package nz.co.warehouseandroidtest.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import nz.co.warehouseandroidtest.Constants;

public class PreferenceUtil {

    public static String getUserId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREF_USER_ID, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.PREF_USER_ID, null);
    }

    public static void putUserId(Context context, String userId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREF_USER_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREF_USER_ID, userId);
        editor.commit();
    }

}
