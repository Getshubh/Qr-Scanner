package com.getshubh.qrscanner;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;

// Begin, DRS-1500, DRS-1503, 7 August 2020, User to Scan QR Code/record audio using a long press of the power key

public class QrCodeDetailsActivity extends Activity {

    Barcode mBarCode;
    TextView mTvTitle, mTvDescription;
    Button mBtnCopyText;
    private String TAG=QrCodeDetailsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr_code_details);

        Window wind = getWindow();
        wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        wind.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        mTvTitle =findViewById(R.id.tv_title);
        mTvDescription =findViewById(R.id.tv_decription);
        mBtnCopyText =findViewById(R.id.btn_copy_text);
        mBarCode = (Barcode) (getIntent().getExtras()).getParcelable("mBarCode");

        mBtnCopyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(mBarCode.displayValue, mBarCode.rawValue);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(QrCodeDetailsActivity.this, "Text Copied", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        if (mBarCode !=null)
            showData(mBarCode);
        else
            finish();
    }

    private void showData(Barcode barcode) {
        try {
            switch (barcode.valueFormat) {
                case Barcode.WIFI:
                    String ssid = barcode.wifi.ssid;
                    int encryptionType = barcode.wifi.encryptionType;
                    String encryptionvalue = null;
                    if (encryptionType == 1)
                        encryptionvalue = "Open";
                    else if (encryptionType == 2)
                        encryptionvalue = "WPA";
                    else if (encryptionType == 3)
                        encryptionvalue = "WEP";
                    mTvTitle.setText(getString(R.string.qrcode_wifi));
                    mTvDescription.setText(getString(R.string.qrcode_ssid) + ssid + " \n\n " + getString(R.string.qrcode_password)+" ******* \n\n" + getString(R.string.qrcode_security) + encryptionvalue);
                    mBtnCopyText.setVisibility(View.GONE);
                    break;
                case Barcode.DRIVER_LICENSE:
                    String firstName = barcode.driverLicense.firstName;
                    String lastName = barcode.driverLicense.lastName;
                    String licenseNumber = barcode.driverLicense.licenseNumber;
                    String birthDate = barcode.driverLicense.birthDate;
                    String expiryDate = barcode.driverLicense.expiryDate;
                    mTvTitle.setText(getString(R.string.driver_liscense));
                    StringBuilder stringBuilder = new StringBuilder();
                    if (!TextUtils.isEmpty(firstName)) {
                        stringBuilder.append(getString(R.string.qrcode_name));
                        stringBuilder.append(firstName + "\n\n");
                    }
                    if (!TextUtils.isEmpty(lastName)) {
                        stringBuilder.append(getString(R.string.qrcode_last_name));
                        stringBuilder.append(lastName + "\n\n");
                    }
                    if (!TextUtils.isEmpty(licenseNumber)) {
                        stringBuilder.append(getString(R.string.qrcode_license_no));
                        stringBuilder.append(licenseNumber + "\n\n");
                    }
                    if (!TextUtils.isEmpty(birthDate)) {
                        stringBuilder.append(getString(R.string.qrcode_birthdate));
                        stringBuilder.append(birthDate + "\n\n");
                    }
                    if (!TextUtils.isEmpty(firstName)) {
                        stringBuilder.append(getString(R.string.qrcode_dl_exp));
                        stringBuilder.append(expiryDate + "\n\n");
                    }
                    mTvDescription.setText(stringBuilder.toString());
                    break;

                case Barcode.TEXT:
                    String displayValue = barcode.displayValue;
                    mTvTitle.setText(getString(R.string.qrcode_text));
                    mTvDescription.setText(getString(R.string.text) + displayValue);
                    break;

                default:
                    String value = barcode.rawValue;
                    mTvTitle.setText(getResources().getString(R.string.qrcode_details));
                    mTvDescription.setText(value);
                    break;


            }
        }catch (Exception e){
            Log.i(TAG,e.toString());
        }
    }

    // Begin, DRS-1500, DRS-1503, 7 August 2020, User to Scan QR Code/record audio using a long press of the power key

}
