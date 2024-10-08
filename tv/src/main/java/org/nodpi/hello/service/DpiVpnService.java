package org.nodpi.hello.service;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import android.net.VpnService;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileDescriptor;
import android.content.BroadcastReceiver;
import android.os.Handler;



import java.io.File;
import java.io.IOException;
import java.util.Objects;

import android.content.IntentFilter;

import androidx.annotation.RequiresApi;

public class DpiVpnService extends VpnService implements ServiceControl {

    private static final String TAG = "DpiVpnService";
    private static final String VPN_MTU = "1500";
    private static final String PRIVATE_VLAN4_CLIENT = "26.26.26.1";
    private static final String PRIVATE_VLAN4_ROUTER = "26.26.26.2";
    private static final String TUN2SOCKS = "libtun2socks.so";
    private static final int SOCKS_PORT = 1080;

    private ParcelFileDescriptor mInterface;
    private Process process;
    private boolean isRunning;


    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter("STOP_VPN");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(stopReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        startService();
        return START_STICKY;
    }

    private BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "STOP_VPN")) {
                stopTun2socks();
                stopSelf();
            }
        }
    };

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        unregisterReceiver(stopReceiver);
        stopService();
    }

    @Override
    public void startService() {
        Log.d(TAG, "startService");
        setup();
        runTun2socks();
    }

public void stopService() {
    Log.d(TAG, "stopService");
    stopTun2socks();
    stopSelf();
}

    @Override
    public boolean vpnProtect(int socket) {
        return protect(socket);
    }

    private void setup() {
        Log.d(TAG, "setup");
        Intent intent = VpnService.prepare(this);
        if (intent == null) {
            establishVpn();
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setup();
                }
            }, 1500);
        }
    }

    private void establishVpn() {
        Builder builder = new Builder();
        builder.setMtu(Integer.parseInt(VPN_MTU));
        builder.addAddress(PRIVATE_VLAN4_CLIENT, 30);
        builder.addRoute("0.0.0.0", 0);

        try {
            builder.addDisallowedApplication(getApplication().getPackageName().toString());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Error", "Package name not found", e);
        }




        try {
            mInterface = builder.establish();
            if (mInterface == null) {
                Log.w(TAG, "Failed to establish VPN interface");
                stopTun2socks();
                return;
            }
            isRunning = true;
        } catch (Exception e) {
            Log.e(TAG, "Error establishing VPN interface", e);
            stopTun2socks();
        }
    }

    private void runTun2socks() {
        Log.d(TAG, "runTun2socks");
        String[] cmd = new String[] {
                new File(getApplicationInfo().nativeLibraryDir, TUN2SOCKS).getAbsolutePath(),
                "--netif-ipaddr", PRIVATE_VLAN4_ROUTER,
                "--netif-netmask", "255.255.255.252",
                "--socks-server-addr", "127.0.0.1:" + SOCKS_PORT,
                "--tunmtu", VPN_MTU,
                "--sock-path", "sock_path",
                "--enable-udprelay",
                "--loglevel", "notice"
        };

        try {
            ProcessBuilder proBuilder = new ProcessBuilder(cmd);
            proBuilder.redirectErrorStream(true);
            proBuilder.directory(getFilesDir());
            process = proBuilder.start();

            sendFd();

            Thread thread = new Thread(() -> {
                try {
                    process.waitFor();
                    Log.d(TAG, TUN2SOCKS + " exited");
                    if (isRunning) {
                        Thread.sleep(1000);
                        Log.d(TAG, TUN2SOCKS + " restart");
                        runTun2socks();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            thread.start();
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    private void sendFd() {
        FileDescriptor fd = mInterface.getFileDescriptor();
        String path = new File(getFilesDir(), "sock_path").getAbsolutePath();
        Log.d(TAG, path);

        new Thread(() -> {
            int tries = 0;
            while (true) {
                try {
                    Thread.sleep(50L << tries);
                    Log.d(TAG, "sendFd tries: " + tries);
                    LocalSocket localSocket = new LocalSocket();
                    localSocket.connect(new LocalSocketAddress(path, LocalSocketAddress.Namespace.FILESYSTEM));
                    localSocket.setFileDescriptorsForSend(new FileDescriptor[] { fd });
                    localSocket.getOutputStream().write(42);
                    break;
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                    if (tries > 5) break;
                    tries++;
                }
            }
        }).start();
    }

    private void stopTun2socks() {
        Log.d(TAG, "stopTun2socks");
        isRunning = false;
        try {
            process.destroy();
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
        if (mInterface != null) {
            try {
                mInterface.close();
            } catch (IOException e) {
                Log.d(TAG, e.toString());
            }
        }
    }
}