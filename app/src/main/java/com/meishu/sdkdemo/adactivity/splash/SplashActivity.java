package com.meishu.sdkdemo.adactivity.splash;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.meishu.sdk.core.ad.splash.ISplashAd;
import com.meishu.sdk.core.ad.splash.SplashAdListener;
import com.meishu.sdk.core.ad.splash.SplashAdLoader;
import com.meishu.sdk.core.loader.InteractionListener;
import com.meishu.sdkdemo.R;
import com.meishu.sdkdemo.adid.IdProviderFactory;

public class SplashActivity extends AppCompatActivity implements SplashAdListener {
    private static final String TAG = "SplashActivity";

    private ISplashAd splashAd;
    private volatile boolean canJump = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ViewGroup adContainer = findViewById(R.id.splash_container);

        String pid  = getIntent().getStringExtra("alternativePlaceId");
        if (TextUtils.isEmpty(pid)) {
            pid = IdProviderFactory.getDefaultProvider().splash();
        }

        SplashAdLoader splashAdLoader = new SplashAdLoader(this, adContainer, pid, SplashActivity.this, 3000);
        splashAdLoader.loadAd();
    }

    @Override
    public void onAdLoaded(ISplashAd splashAd) {
        Log.d(TAG, "onAdLoaded: 开屏广告填充");
        this.splashAd = splashAd;
        splashAd.setInteractionListener(new InteractionListener() {

            @Override
            public void onAdClicked() {
                Log.d(TAG, "onAdClicked: 开屏广告被点击");
            }
        });
    }

    @Override
    public void onAdExposure() {
        Log.d(TAG, "onAdExposure: 开屏广告曝光");
    }

    @Override
    public void onAdClosed() {
        Log.d(TAG, "onAdClosed: 开屏广告被关闭");

        if (canJump) {
            next();
        }
        canJump = true;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: 暂停");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: 开屏界面停止运行");
        super.onStop();
        canJump=true;
    }

    private void next() {
        // 此处打开需要的 activity 即可
        this.finish();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        if (this.splashAd != null && canJump) {
//            next();
        }
        canJump = true;
    }

    @Override
    public void onAdError() {
        Log.d(TAG, "onError: 没有加载到广告");
        Toast.makeText(this, "没有加载到广告", Toast.LENGTH_SHORT).show();
        this.finish();
    }

    @Override
    public void onAdPresent(ISplashAd splashAd) {
        Log.d(TAG, "onAdPresent: 广告已显示");
    }
}
