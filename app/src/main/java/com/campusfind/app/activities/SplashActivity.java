package com.campusfind.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campusfind.app.R;
import com.campusfind.app.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate logo: scale + fade in
        ImageView logo = findViewById(R.id.ivSplashLogo);
        TextView tagline = findViewById(R.id.tvSplashTagline);
        View appName = findViewById(R.id.ivSplashAppName);

        // Scale + fade for logo
        ScaleAnimation scale = new ScaleAnimation(0.7f, 1f, 0.7f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(700);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(700);
        AnimationSet logoAnim = new AnimationSet(true);
        logoAnim.addAnimation(scale);
        logoAnim.addAnimation(fadeIn);
        logo.startAnimation(logoAnim);

        // Fade in text after logo appears
        AlphaAnimation textFade = new AlphaAnimation(0f, 1f);
        textFade.setDuration(600);
        textFade.setStartOffset(500);
        textFade.setFillAfter(true);
        appName.startAnimation(textFade);

        AlphaAnimation tagFade = new AlphaAnimation(0f, 1f);
        tagFade.setDuration(600);
        tagFade.setStartOffset(750);
        tagFade.setFillAfter(true);
        tagline.startAnimation(tagFade);

        // Navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);
            Intent intent;
            if (session.isLoggedIn()) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION_MS);
    }
}
