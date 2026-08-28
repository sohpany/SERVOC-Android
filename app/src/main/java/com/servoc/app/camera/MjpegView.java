package com.servoc.app.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MjpegView extends ImageView {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean running = false;
    private volatile HttpURLConnection connection;
    private volatile long generation = 0;

    public MjpegView(Context c) {
        super(c);
        setScaleType(ScaleType.FIT_CENTER);
        setBackgroundColor(0xFF000000);
    }

    public synchronized void start(String url) {
        running = false;
        generation++;
        HttpURLConnection c = connection;
        if (c != null) c.disconnect();
        connection = null;

        running = true;
        final long token = generation;
        worker.execute(() -> stream(url, token));
    }

    public synchronized void stop() {
        running = false;
        generation++;
        HttpURLConnection c = connection;
        if (c != null) c.disconnect();
        connection = null;
    }

    private boolean active(long token){ return running && generation == token; }

    private void stream(String streamUrl, long token) {
        while (active(token)) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(streamUrl).openConnection();
                connection = c;
                c.setConnectTimeout(1800);
                c.setReadTimeout(5000);
                c.setUseCaches(false);
                c.connect();
                try (InputStream in = c.getInputStream()) {
                    int prev = -1, cur;
                    ByteArrayOutputStream frame = null;
                    while (active(token) && (cur = in.read()) != -1) {
                        if (frame == null) {
                            if (prev == 0xFF && cur == 0xD8) {
                                frame = new ByteArrayOutputStream(64 * 1024);
                                frame.write(0xFF); frame.write(0xD8);
                            }
                        } else {
                            frame.write(cur);
                            if (frame.size() > 1024 * 1024) frame = null;
                            else if (prev == 0xFF && cur == 0xD9) {
                                byte[] bytes = frame.toByteArray();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                if (bitmap != null && active(token)) post(() -> { if(active(token)) setImageBitmap(bitmap); });
                                frame = null;
                            }
                        }
                        prev = cur;
                    }
                } finally {
                    c.disconnect();
                    if (connection == c) connection = null;
                }
            } catch (Exception ignored) {
                if(!active(token)) return;
                try { Thread.sleep(800); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        worker.shutdownNow();
        super.onDetachedFromWindow();
    }
}
