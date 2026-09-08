package nz.co.warehouseandroidtest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

public class PermissionChecker {

    Context context;

    public PermissionChecker(Context context) {
        this.context = context;
    }

    public Boolean ifLackPermissions(String[] permissions) {
        int i = 0;
        while (i < permissions.length) {
            if (ifLackPermission(permissions[i])) {
                return true;
            }
            i++;
        }
        return false;
    }

    Boolean ifLackPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED;
    }
}
