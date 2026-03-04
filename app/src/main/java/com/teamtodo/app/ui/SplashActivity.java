package com.teamtodo.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.teamtodo.app.R;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 120;
    private static final String PREFS_NAME = "teamtodo_prefs";
    private static final String KEY_THEME_COLOR = "themeColor";
    private static final String DEFAULT_THEME_COLOR = "#EB4770";
    private FirebaseAuth mAuth;
    private String sharedUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        applySavedThemeToSplash();

        mAuth = FirebaseAuth.getInstance();

        // URL 공유로 진입한 경우 처리
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            Intent next;
            if (currentUser != null) {
                // 로그인된 경우 → 프로필 선택 화면으로 (WebView)
                next = new Intent(SplashActivity.this, MainActivity.class);
                next.putExtra("screen", "profile_select");
            } else {
                // 미로그인 → 구글 로그인 화면으로 (WebView)
                next = new Intent(SplashActivity.this, MainActivity.class);
                next.putExtra("screen", "login");
            }
            if (sharedUrl != null) {
                next.putExtra("sharedUrl", sharedUrl);
            }
            startActivity(next);
            finish();
        }, SPLASH_DELAY);
    }

    private String normalizeHexColor(String input) {
        if (input == null) return DEFAULT_THEME_COLOR;
        String raw = input.trim();
        if (raw.isEmpty()) return DEFAULT_THEME_COLOR;
        if (!raw.startsWith("#")) raw = "#" + raw;
        if (!raw.matches("^#[0-9a-fA-F]{6}$")) return DEFAULT_THEME_COLOR;
        return raw.toUpperCase();
    }

    private int blendWithWhite(int color, float ratioToWhite) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int nr = (int) (r + (255 - r) * ratioToWhite);
        int ng = (int) (g + (255 - g) * ratioToWhite);
        int nb = (int) (b + (255 - b) * ratioToWhite);
        return Color.rgb(nr, ng, nb);
    }

    private void applySavedThemeToSplash() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String saved = normalizeHexColor(prefs.getString(KEY_THEME_COLOR, DEFAULT_THEME_COLOR));
        int themeColor = Color.parseColor(saved);
        int lightBg = Color.WHITE;
        int subText = blendWithWhite(themeColor, 0.45f);

        getWindow().setStatusBarColor(lightBg);
        getWindow().setNavigationBarColor(lightBg);

        View root = findViewById(R.id.splashRoot);
        TextView title = findViewById(R.id.splashTitle);
        TextView subtitle = findViewById(R.id.splashSubtitle);
        if (root != null) root.setBackgroundColor(lightBg);
        if (title != null) title.setTextColor(themeColor);
        if (subtitle != null) subtitle.setTextColor(subText);
    }
}
