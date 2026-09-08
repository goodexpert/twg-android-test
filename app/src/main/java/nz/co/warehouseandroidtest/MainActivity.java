package nz.co.warehouseandroidtest;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import nz.co.warehouseandroidtest.Utils.PreferenceUtil;
import nz.co.warehouseandroidtest.data.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    String[] permissions = {Manifest.permission.CAMERA};

    PermissionChecker checker = new PermissionChecker(this);

    public final int REQUEST_PERMISSION_CODE = 0;

    private TextView tvScan;
    private TextView tvSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvScan = (TextView) findViewById(R.id.tv_scan_barcode);
        tvSearch = (TextView) findViewById(R.id.tv_search);

        tvScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, BarScanActivity.class);
                startActivity(intent);
            }
        });

        tvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checker.ifLackPermissions(permissions)) {
            PermissionActivity.startActivityForResult(this, REQUEST_PERMISSION_CODE, permissions);
        }

        if (PreferenceUtil.getUserId(this) == null) {
            ((WarehouseTestApp) getApplicationContext()).getWarehouseService().getNewUserId().enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            if (response.isSuccessful()) {
                                User user = response.body();
                                if (null != user && null != user.UserID) {
                                    PreferenceUtil.putUserId(MainActivity.this, user.UserID);
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Get User failed!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            Toast.makeText(MainActivity.this, "Get User failed!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (Activity.RESULT_OK != resultCode) {
            return;
        }

        if (requestCode == REQUEST_PERMISSION_CODE && resultCode == PermissionActivity.PERMISSION_DENIED) {
            finish();
        }
    }
}
