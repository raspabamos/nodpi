package org.nodpi.hello;

import android.Manifest;

import android.app.UiModeManager;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.TextView;
import android.net.VpnService;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.PowerManager;
import android.widget.Toast;
import java.util.Map;

import org.nodpi.hello.service.DpiService;
import org.nodpi.hello.service.DpiVpnService;
import org.nodpi.hello.receiver.BReceiver;
import org.nodpi.hello.HttpsTest;
import androidx.lifecycle.Observer;
import android.util.Log;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private HttpsTest httpsTest;
    private Button startButton;
    private Button requestPermissionsButton;
    private TextView statusText;
    private String status = "inactive";
    private static final int REQUEST_POST_NOTIFICATIONS_PERMISSION = 111;
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATIONS_PERMISSION = 112;
    private static final int REQUEST_VPN_PERMISSION = 113;

    private Intent serviceIntent;
    private Intent dpiIntent;

    private ActivityResultLauncher<Intent> requestVpnPermission;
    private ActivityResultLauncher<String[]> requestPostNotificationsPermission;

    private List<String> permissions = new ArrayList<>();

    private BReceiver myReceiver;
    private IntentFilter intentFilter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serviceIntent = new Intent(this, DpiVpnService.class);
        dpiIntent = new Intent(this, DpiService.class);
        startService(dpiIntent);
        registerBroadcasts();

        setContentView(R.layout.activity_tv_main);

        checkTv();
        checkBatteryOptimization();

        TextView linkTextView = findViewById(R.id.link);

        linkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("@string/app_repository"));
                startActivity(intent);
            }
        });

        startButton = findViewById(R.id.start_button);

        statusText = findViewById(R.id.status_text);

        startButton.setFocusableInTouchMode(true);
        startButton.setFocusable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startButton.setFocusedByDefault(true);
        }
        startButton.requestFocus();


        if (status.equals("active")) {
            checkBatteryOptimizationAndStart();
            checkVpnPermission();
        }





        requestPermissionsButton = findViewById(R.id.request_permissions_button);
        requestPermissionsButton.setFocusableInTouchMode(true);
        requestPermissionsButton.setFocusable(true);

        requestPermissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
                    if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
                        findViewById(R.id.request_permissions_container).setVisibility(View.GONE);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    }
                } else {
                    findViewById(R.id.request_permissions_container).setVisibility(View.GONE);
                }
            }
        });

        requestPermissionsButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    requestPermissionsButton.setBackgroundResource(R.color.light_green_focused);
                } else {
                    requestPermissionsButton.setBackgroundResource(R.color.light_green);
                }
            }
        });




        requestVpnPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    startVpn();
                }
            }
        });

        requestPostNotificationsPermission = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.POST_NOTIFICATIONS) != null && Boolean.TRUE.equals(result.get(Manifest.permission.POST_NOTIFICATIONS))) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        String packageName = getPackageName();
                        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                            // Request battery optimization permission, but don't block the flow if it fails
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        }
                    }
                    checkVpnPermission();
                } else {
                   // Toast.makeText(MainActivity.this, "Некоторых разрешений может не хватать для стабильной работы", Toast.LENGTH_SHORT).show();
                    checkVpnPermission();
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status.equals("inactive")) {
                    status = "active";
                    requestPermissions();


                } else if (status.equals("active")) {
                    stopVpn();
                    status = "inactive";

                }
                getSharedPreferences("app_state", MODE_PRIVATE).edit().putString("status", status).apply();
            }
        });

        startButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (status.equals("active")) {
                        startButton.setBackgroundResource(R.color.dark_red_focused);
                    } else {
                        startButton.setBackgroundResource(R.color.light_green_focused);
                    }
                } else {
                    if (status.equals("active")) {
                        startButton.setBackgroundResource(R.color.dark_red);
                    } else {
                        startButton.setBackgroundResource(R.color.light_green);
                    }
                }
            }
        });



        // Load saved state
        status = getSharedPreferences("app_state", MODE_PRIVATE).getString("status", "inactive");
        if (status.equals("active")) {
            statusText.setText("Подключено");
        } else {
            statusText.setText("Выключено");
        }

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("android.intent.action.MAIN")) {
                    String feature = intent.getStringExtra("feature");
                    String featureId = intent.getStringExtra("featureId");
                    if (feature != null && featureId != null) {
                        if (feature.equals("vpn") && featureId.equals("start")) {
                            startVpn();
                        } else if (feature.equals("vpn") && featureId.equals("stop")) {
                            stopVpn();
                        }
                    }
                }
            }
        }
    }

    private void checkTv() {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        LinearLayout layout = findViewById(R.id.activity_tv_main);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            int widthInPixels = (int) (300 * getResources().getDisplayMetrics().density);
            layout.setLayoutParams(new LinearLayout.LayoutParams(widthInPixels, ViewGroup.LayoutParams.MATCH_PARENT));
            ((LinearLayout.LayoutParams) layout.getLayoutParams()).gravity = Gravity.END;
            layout.setLayoutParams(layout.getLayoutParams());

            findViewById(R.id.request_permissions_container).setVisibility(View.GONE);
        } else {
            layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ((LinearLayout.LayoutParams) layout.getLayoutParams()).gravity = Gravity.CENTER;
            layout.setLayoutParams(layout.getLayoutParams());
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            findViewById(R.id.request_permissions_container).setVisibility(View.GONE);
        }


    }

    private void checkBatteryOptimizationAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
            if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
                findViewById(R.id.request_permissions_container).setVisibility(View.GONE);
            } else {
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    findViewById(R.id.request_permissions_container).setVisibility(View.GONE);
                    checkVpnPermission();

                } else {
                    Toast.makeText(MainActivity.this, "Необходимо отключить оптимизацию батареи!", Toast.LENGTH_SHORT).show();
                    // stopVpn();

                }
            }

        } else {
            findViewById(R.id.request_permissions_container).setVisibility(View.GONE);
            checkVpnPermission();
        }
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
            if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
                findViewById(R.id.request_permissions_container).setVisibility(View.GONE);
            } else {
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    findViewById(R.id.request_permissions_container).setVisibility(View.GONE);
                }
            }
        } else {
            findViewById(R.id.request_permissions_container).setVisibility(View.GONE);
        }
    }

    private void requestPermissions() {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && uiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_TELEVISION) {
            permissions.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        }

        if (!permissions.isEmpty()) {
            requestPostNotificationsPermission.launch(permissions.toArray(new String[0]));
        } else {
            checkVpnPermission();
        }
    }

    private void checkVpnPermission() {
        if (status.equals("active")) {
            Intent intent = VpnService.prepare(this);
        if (intent == null) {
            startVpn();
        } else {
            requestVpnPermission.launch(intent);
        }
    }
    }

    public void updateNotification(String message) {
        Intent intent = new Intent("UPDATE_NOTIFICATION");
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    private void startVpn() {
        startService(dpiIntent);
        if (status.equals("active")) {
            Toast.makeText(MainActivity.this, "Запускаем DPI", Toast.LENGTH_SHORT).show();
            startService(serviceIntent);
            //startButton.setBackgroundResource(R.drawable.circle_button_green);

            startButton.setBackgroundResource(R.color.dark_red);


            statusText.setText("Подключаемся");
            startButton.setText("Остановить");
            //startButtonText.setText("Остановить");
            httpsTest = new HttpsTest();
            httpsTest.getStatus().observe(this, new Observer<HttpsTest.Status>() {
                @Override
                public void onChanged(HttpsTest.Status status) {
                    Log.d("HttpsTest", "Status: " + status);
                    // Update your UI here
                    switch (status) {
                        case ERROR:
                            updateNotification("Ошибка проверки связи");
                            statusText.setText("Ошибка проверки связи");
                            stopVpn();
                            break;
                        case IDLE:
                            updateNotification("Проверка связи не начата");
                            statusText.setText("Проверка связи не начата");
                            break;
                        case TESTING:
                            updateNotification("Проверка связи...");
                            statusText.setText("Проверка связи...");
                            break;
                        case SUCCESS:
                            updateNotification("Связь успешно проверена");
                            statusText.setText("Связь успешно проверена");
                            break;
                    }
                }
            });
            httpsTest.testConnection();
        }
    }

    private void stopVpn() {
        Toast.makeText(MainActivity.this, "Останавливаем DPI", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent("STOP_VPN");
        sendBroadcast(intent);
        status = "inactive";
       // startButtonText.setText("Запустить");
        startButton.setText("Запустить");
        getSharedPreferences("app_state", MODE_PRIVATE).edit().putString("status", status).apply();
        //startButton.setBackgroundResource(R.drawable.circle_button_red);
        startButton.setBackgroundResource(R.color.light_green);
        startService(dpiIntent);
        statusText.setText("Выключено. Нажмите Запустить");
        updateNotification("Выключено");
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(dpiIntent);
        checkBatteryOptimization();
        if (status.equals("active")) {
            checkBatteryOptimizationAndStart();
            checkVpnPermission();
        }
        registerBroadcasts();
    }

    private void registerBroadcasts() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        // Add other actions as needed

        myReceiver = new BReceiver();
        registerReceiver(myReceiver, intentFilter);
    }
}