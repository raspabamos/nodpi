package org.nodpi.hello;

import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpsTest extends ViewModel {
    public enum Status {
        IDLE,
        TESTING,
        SUCCESS,
        ERROR
    }

    private MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);

    public MutableLiveData<Status> getStatus() {
        return status;
    }

    public void testConnection() {
        cancelTest();
        status.setValue(Status.TESTING);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://cp.cloudflare.com");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Connection", "close");
                    conn.setInstanceFollowRedirects(false);
                    conn.setUseCaches(false);
                    long start = SystemClock.elapsedRealtime();
                    int code = conn.getResponseCode();
                    long elapsed = SystemClock.elapsedRealtime() - start;
                    long contentLength = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        contentLength = conn.getContentLengthLong();
                    } else {
                        contentLength = conn.getContentLength();
                    }
                    if (code == 204 || code == 200 && contentLength == 0L) {
                        status.postValue(Status.SUCCESS);
                    } else {
                        status.postValue(Status.ERROR);
                    }
                } catch (IOException e) {
                    status.postValue(Status.ERROR);
                }
            }
        });
        thread.start();
    }

    private void cancelTest() {
        // No need to cancel the test in this case
    }

    public void invalidate() {
        cancelTest();
        status.setValue(Status.IDLE);
    }
}