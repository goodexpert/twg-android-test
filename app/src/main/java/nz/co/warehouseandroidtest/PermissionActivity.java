package nz.co.warehouseandroidtest;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

public class PermissionActivity extends AppCompatActivity {

    public static int PERMISSION_GRANTED = 0;
    public static int PERMISSION_DENIED = 1;

    public final String PACKAGE_URL_SCHEME = "package:";

    public static final String PERMISSION_EXTRA_FLAG = "nz.co.warehouseandroidtest.permission.extra_permission";

    public final int PERMISSION_REQUEST_CODE = 0;

    public boolean mIsRequiredPermissionCheck = false;

    private PermissionChecker checker = new PermissionChecker(this);

    public static void startActivityForResult(Activity activity, int requestCode, String[] permissions) {
        Intent intent = new Intent();
        intent.setClass(activity, PermissionActivity.class);
        intent.putExtra(PERMISSION_EXTRA_FLAG, permissions);
        ActivityCompat.startActivityForResult(activity, intent, requestCode, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() == null || !getIntent().hasExtra(PERMISSION_EXTRA_FLAG)) {
            throw new RuntimeException("PermissionsActivity needs to be started by static method startActivityForResult!");
        }

        setContentView(R.layout.activity_permission);

        mIsRequiredPermissionCheck = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsRequiredPermissionCheck) {
            if (checker.ifLackPermissions(getPermissions())) {
                requestPermissions(getPermissions());
            } else {
                allPermissionsGranted();
            }
        } else {
            mIsRequiredPermissionCheck = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (PERMISSION_REQUEST_CODE == requestCode && hasAllPermissionsGranted(grantResults)) {
            mIsRequiredPermissionCheck = true;
            allPermissionsGranted();
        } else {
            mIsRequiredPermissionCheck = false;
            showMissingPermissionDialog();
        }
    }

    private void showMissingPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Help");
        builder.setMessage("Current app lacks necessary permissions. \n\nPlease click \"Settings\" - \"Permission\" - to grant permissions. \n\nThen click back button twice to return.");

        builder.setNeutralButton("Quit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setResult(PERMISSION_DENIED);
                finish();
            }
        });

        builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startAppSettings();
            }
        });

        builder.show();
    }

    public void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(PACKAGE_URL_SCHEME + getPackageName()));
        startActivity(intent);
    }

    private boolean hasAllPermissionsGranted(int[] grantResults) {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    private void allPermissionsGranted() {
        setResult(PERMISSION_GRANTED);
        finish();
    }

    public void requestPermissions(String[] permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    public String[] getPermissions() {
        return getIntent().getStringArrayExtra(PERMISSION_EXTRA_FLAG);
    }
}
