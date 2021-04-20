package com.getshubh.qrscanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

// Begin, DRS-1500, DRS-1503, 17 July 2020, User to Scan QR Code/record audio using a long press of the power key

public class TransparentPermissionActivity extends Activity {

    private int mPermissionCode = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window wind = getWindow();
        wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        wind.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        init();

    }

    private void init() {
            checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{Manifest.permission.CAMERA};
            boolean permissionDeclined = false;
            for (String s : permissions)
                if (checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED)
                    permissionDeclined = true;

            if (permissionDeclined) {
                requestPermissions(permissions, mPermissionCode);
            } else {
                finish();
            }
        } else
            finish();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == mPermissionCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int i = 0, length = permissions.length; i < length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("TransparentPermission", "Give Permission");
                        Intent intent = new Intent(TransparentPermissionActivity.this, QrCodeActivity.class);
                        startActivity(intent);
                    }
                }
                finish();
            }

        }
    }


    //End, DRS-1500, DRS-1503, 17 July 2020, User to Scan QR Code/record audio using a long press of the power key
}



