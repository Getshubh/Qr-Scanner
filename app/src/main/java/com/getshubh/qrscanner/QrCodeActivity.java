package com.getshubh.qrscanner;

import android.Manifest;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.CalendarContract;
import android.provider.ContactsContract;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.lang.reflect.Field;

import androidx.core.content.ContextCompat;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;


// Begin, DRS-1500, DRS-1503, 7 August 2020, User to Scan QR Code/record audio using a long press of the power key

public class QrCodeActivity extends AppCompatActivity {


    private static final String TAG = QrCodeActivity.class.getSimpleName();
    SurfaceView mSurfaceView;
    private BarcodeDetector mBarcodeDetector;
    private CameraSource mCameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private ImageView mIvFlashLight;
    private TextView mTvFlash;
    private boolean isTonePlayed = false;
    private Camera mCamera;
    private boolean isFlashON = false;
    private Camera.Parameters parameters;
    private ImageView mIvAnimate;
    private TranslateAnimation mAnimation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr_code);
        Window wind = getWindow();
        wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        wind.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);



        initViews();
        if (checkPermissions())
            Log.d(TAG, "Permission Not given");
        else
            requestPermissions();
    }




    public boolean checkPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), INTERNET);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        Intent intent = new Intent(QrCodeActivity.this, TransparentPermissionActivity.class);
        startActivity(intent);
        finish();
    }

    private void initViews() {
        mSurfaceView = findViewById(R.id.surfaceView);
        mIvFlashLight = findViewById(R.id.iv_flash);
        mIvAnimate = findViewById(R.id.iv_animate);
        mTvFlash = findViewById(R.id.tv_flash);

        mAnimation = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.RELATIVE_TO_PARENT, 0f,
                TranslateAnimation.RELATIVE_TO_PARENT, 1.0f);
        mAnimation.setDuration(2000);
        mAnimation.setRepeatCount(-1);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(new LinearInterpolator());
        mIvAnimate.setAnimation(mAnimation);

        mIvFlashLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFlashON) {
                    turnOffFlashLight();
                    isFlashON = false;
                    mTvFlash.setText(getResources().getString(R.string.turn_on_flash));
                } else {
                    turnOnFlashLight();
                    isFlashON = true;
                    mTvFlash.setText(getResources().getString(R.string.turn_off_flash));
                }
            }
        });
    }

    public static Camera getCamera(@androidx.annotation.NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }
                    return null;
                } catch (IllegalAccessException e) {
                    Log.d(TAG, e.toString());
                }

                break;
            }
        }
        return null;
    }

    public void turnOnFlashLight() {
        try {
            mCamera = getCamera(mCameraSource);
            if (mCamera != null) {
                parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                mIvFlashLight.setImageResource(R.drawable.ic_flash_off);
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    public void turnOffFlashLight() {
        try {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
                mIvFlashLight.setImageResource(R.drawable.ic_flash_on);
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }


    private void initialiseDetectorsAndSources() {
        mBarcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        if (!mBarcodeDetector.isOperational()) {
            Toast.makeText(this, getString(R.string.internet_connection_needed), Toast.LENGTH_SHORT).show();
            finish();
        }

        mCameraSource = new CameraSource.Builder(this, mBarcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(QrCodeActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        mCameraSource.start(mSurfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(QrCodeActivity.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }

                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraSource.stop();
                finish();

            }
        });


        mBarcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(final Detector.Detections<Barcode> detections) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                        if (barcodes.size() != 0) {
                            for (int i = 0; i < barcodes.size(); i++) {
                                Barcode barcode = barcodes.valueAt(i);
                                int type = barcode.valueFormat;
                                handler.sendMessage(Message.obtain(handler, type, barcode));
                            }
                        }
                    }
                }).start();


            }

            Handler handler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message message) {
                    if (!isTonePlayed) {
                        playSound();
                        isTonePlayed = true;
                    }
                    Intent intent = null;
                    Barcode barcode = (Barcode) message.obj;
                    switch (message.what) {
                        case Barcode.PHONE:
                            try {
                                String number = barcode.phone.number;
                                Uri u = Uri.parse("tel:" + number);
                                intent = new Intent(Intent.ACTION_DIAL, u);
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                            break;
                        case Barcode.EMAIL:
                            try {
                                intent = new Intent(Intent.ACTION_SEND);
                                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{barcode.email.address});
                                intent.putExtra(Intent.EXTRA_SUBJECT, barcode.email.subject);
                                intent.putExtra(Intent.EXTRA_TEXT, barcode.email.body);
                                intent.setType("message/rfc822");
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                            break;
                        case Barcode.URL:
                            try {
                                String url = barcode.displayValue;
                                intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(url));
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                            break;
                        case Barcode.GEO:
                            try {
                                String geo = "geo:" + barcode.geoPoint.lat + "," + barcode.geoPoint.lng;
                                Uri gmmIntentUri = Uri.parse(geo);
                                intent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                intent.setPackage("com.google.android.apps.maps");
                                if (intent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(intent);
                                }
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                            break;
                        case Barcode.SMS:
                            try {
                                String number = barcode.sms.phoneNumber;
                                Uri uri = Uri.fromParts("sms", number, null);
                                intent = new Intent(Intent.ACTION_VIEW, uri);
                                intent.putExtra("sms_body", barcode.sms.message);
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                            break;
                        case Barcode.WIFI:
                            try {
                                showDetailInfoActivity(barcode);
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                            break;
                        case Barcode.CALENDAR_EVENT:
                            try {
                                intent = new Intent(Intent.ACTION_EDIT);
                                intent.setType("vnd.android.cursor.item/event");
                                String title = barcode.calendarEvent.summary;
                                String startTime = String.valueOf(barcode.calendarEvent.start);
                                String endTime = String.valueOf(barcode.calendarEvent.end);
                                String strDescription = String.valueOf(barcode.calendarEvent.description);
                                intent.putExtra(CalendarContract.Events.TITLE, title);
                                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                        startTime);
                                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                                        endTime);
                                intent.putExtra(CalendarContract.Events.ALL_DAY, false);// periodicity
                                intent.putExtra(CalendarContract.Events.DESCRIPTION, strDescription);
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                            break;
                        case Barcode.CONTACT_INFO:
                            try {
                                intent = new Intent((ContactsContract.Intents.Insert.ACTION));
                                intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                                intent.putExtra(ContactsContract.Intents.Insert.NAME, barcode.contactInfo.name.formattedName);
                                if (barcode.contactInfo.phones != null && barcode.contactInfo.phones.length > 0 && barcode.contactInfo.phones[0] != null && barcode.contactInfo.phones[0].number != null)
                                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, barcode.contactInfo.phones[0].number);
                                if (barcode.contactInfo.emails != null && barcode.contactInfo.emails.length > 0 && barcode.contactInfo.emails[0] != null && barcode.contactInfo.emails[0].address != null)
                                    intent.putExtra(ContactsContract.Intents.Insert.EMAIL, barcode.contactInfo.emails[0].address);
                                intent.putExtra(ContactsContract.Intents.Insert.COMPANY, barcode.contactInfo.organization);
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                            break;
                        case Barcode.DRIVER_LICENSE:
                            try {
                                showDetailInfoActivity(barcode);
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                            break;
                        case Barcode.TEXT:
                            try {
                                intent = new Intent(Intent.ACTION_WEB_SEARCH);
                                intent.putExtra(SearchManager.QUERY, barcode.rawValue);
                                startActivity(intent);

                            } catch (Exception e) {
                                Log.i(TAG, e.toString());
                            }
                            break;
                        default:
                            showDetailInfoActivity(barcode);
                            break;

                    }
                    finish();
                    return false;
                }
            });
        });
    }

    private void playSound() {
        try {
            MediaPlayer player = MediaPlayer.create(QrCodeActivity.this, R.raw.beep);
            player.start();
        } catch (Exception e) {
            Log.i(TAG, e.toString());

        }
    }

    private void showDetailInfoActivity(Barcode barcode) {
        Intent intent = new Intent(QrCodeActivity.this, QrCodeDetailsActivity.class);
        intent.putExtra("mBarCode", barcode);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraSource != null)
            mCameraSource.release();
        if (mBarcodeDetector != null)
            mBarcodeDetector.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions())
            initialiseDetectorsAndSources();

    }

    // End, DRS-1500, DRS-1503, 7 August 2020, User to Scan QR Code/record audio using a long press of the power key

}
