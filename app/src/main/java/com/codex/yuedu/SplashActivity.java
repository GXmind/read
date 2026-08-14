package com.codex.yuedu;

import android.app.Activity;
import android.content.Intent;
import android.os.*;

public final class SplashActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openLibrary = () -> {
        if (isFinishing()) return;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        UiInsets.immersive(this);
        setContentView(new BookSplashView(this));
        handler.postDelayed(openLibrary, 1650);
    }

    @Override protected void onDestroy() { handler.removeCallbacks(openLibrary); super.onDestroy(); }
}
