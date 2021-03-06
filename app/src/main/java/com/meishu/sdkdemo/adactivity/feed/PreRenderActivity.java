package com.meishu.sdkdemo.adactivity.feed;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.meishu.sdk.core.ad.prerender.PreRenderAdLoader;
import com.meishu.sdk.core.ad.recycler.RecyclerAdData;
import com.meishu.sdk.core.ad.recycler.RecyclerAdListener;
import com.meishu.sdk.core.ad.recycler.RecylcerAdInteractionListener;
import com.meishu.sdk.core.utils.MsAdPatternType;
import com.meishu.sdkdemo.R;
import com.meishu.sdkdemo.adid.IdProviderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class PreRenderActivity extends AppCompatActivity implements RecyclerAdListener {
    private static final String TAG = "PreRenderActivity";
    private PreRenderAdLoader preRenderAdLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_recycler_list);
        initView();

        String pid  = getIntent().getStringExtra("alternativePlaceId");
        if (TextUtils.isEmpty(pid)) {
            pid = IdProviderFactory.getDefaultProvider().feedPreRender();
        }

        // 不指定宽度，默认使用屏幕宽度
        preRenderAdLoader = new PreRenderAdLoader(this, pid, 2,this);
        preRenderAdLoader.loadAd();
    }

    private CustomAdapter mAdapter;
    private AQuery mAQuery;
    private boolean mIsLoading = true;

    private void initView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);
        List<NormalItem> list = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            list.add(new NormalItem("No." + i + " Init Data"));
        }
        mAdapter = new CustomAdapter(this, list);
        recyclerView.setAdapter(mAdapter);
        mAQuery = new AQuery(this);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

                if (!mIsLoading && newState == SCROLL_STATE_IDLE && !recyclerView.canScrollVertically(1)) {
                    mIsLoading = true;
                    PreRenderActivity.this.preRenderAdLoader.loadAd();
                }

            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    private List<RecyclerAdData> loadedAdDatas = new ArrayList<>();
    private static final int MSG_REFRESH_LIST = 1;
    private H mHandler = new H();

    @Override
    public void onAdLoaded(List<RecyclerAdData> adDatas) {
        this.mIsLoading = false;
        loadedAdDatas.addAll(adDatas);
        Message msg = mHandler.obtainMessage(MSG_REFRESH_LIST, adDatas);
        mHandler.sendMessage(msg);
    }

    @Override
    public void onAdExposure() {
        Log.d(TAG, "onAdExposure: 广告曝光");
    }

    @Override
    public void onAdClosed() {
        Log.d(TAG, "onAdClosed: 广告被关闭");
    }

    @Override
    public void onAdError() {
        Log.d(TAG, "onAdError: 没有加载到广告");
        this.mIsLoading = false;
    }

    class NormalItem {
        private String mTitle;

        public NormalItem(int index) {
            this("No." + index + " Normal Data");
        }

        public NormalItem(String title) {
            this.mTitle = title;
        }

        public String getTitle() {
            return mTitle;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.loadedAdDatas != null) {
            for (RecyclerAdData adData : loadedAdDatas) {
                adData.destroy();
            }
        }
    }

    private static final int TYPE_AD = 1;
    private static final int TYPE_DATA = 0;

    class CustomAdapter extends RecyclerView.Adapter<CustomHolder> {

        private List<Object> mData;
        private Context mContext;
        private TreeSet mADSet = new TreeSet();

        public CustomAdapter(Context context, List list) {
            mContext = context;
            mData = list;
        }

        public void addNormalItem(NormalItem item) {
            mData.add(item);
        }

        public void addAdToPosition(RecyclerAdData recyclerAdData, int position) {
            if (position >= 0 && position < mData.size()) {
                mData.add(position, recyclerAdData);
                mADSet.add(position);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return mADSet.contains(position) ? TYPE_AD : TYPE_DATA;
        }

        @Override
        public CustomHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_AD:
                    view = LayoutInflater.from(mContext).inflate(R.layout.item_ad_unified_pre_render, null);
                    break;
                case TYPE_DATA:
                    view = LayoutInflater.from(mContext).inflate(R.layout.item_data, null);
                    break;
                default:
                    view = null;
            }
            return new CustomHolder(view, viewType);
        }

        @Override
        public void onBindViewHolder(CustomHolder holder, int position) {
            switch (getItemViewType(position)) {
                case TYPE_AD:
                    initItemView(position, holder);
                    break;
                case TYPE_DATA:
                    holder.title.setText(((NormalItem) mData.get(position)).getTitle());
                    break;
            }
        }

        private void initItemView(int position, final CustomHolder holder) {
            final RecyclerAdData ad = (RecyclerAdData) mData.get(position);
            View adView;
            if (ad.getAdPatternType() == MsAdPatternType.PRE_RENDER) {
                adView = ad.getAdView();
                if (adView == null) {
                    holder.title.setText("渲染未完成或失败");
                    return;
                }
            } else {
                TextView tv = new TextView(PreRenderActivity.this);
                tv.setText("不是预渲染广告，请检查广告配置");
                adView = tv;
            }
            holder.container.removeAllViews();
            holder.container.addView(adView);
            List<View> clickableViews = new ArrayList<>();
            clickableViews.add(holder.container);
            ad.bindAdToView(PreRenderActivity.this, holder.container,
                    clickableViews, new RecylcerAdInteractionListener() {
                        @Override
                        public void onAdClicked() {
                            Log.d(TAG, "onAdClicked: 广告被点击");
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    class CustomHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public ViewGroup container;

        public CustomHolder(View itemView, int adType) {
            super(itemView);
            switch (adType) {
                case TYPE_AD:
                    container = (ViewGroup) itemView;
                case TYPE_DATA:
                    title = itemView.findViewById(R.id.title);
                    break;

            }
        }
    }

    private static final int ITEM_COUNT = 30;
    private static final int FIRST_AD_POSITION = 5;
    private static final int AD_DISTANCE = 10;

    private class H extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH_LIST:

                    int count = mAdapter.getItemCount();
                    for (int i = 0; i < ITEM_COUNT; i++) {
                        mAdapter.addNormalItem(new NormalItem(count + i));
                    }

                    List<RecyclerAdData> ads = (List<RecyclerAdData>) msg.obj;
                    if (ads != null && ads.size() > 0 && mAdapter != null) {
                        for (int i = 0; i < ads.size(); i++) {
                            mAdapter.addAdToPosition(ads.get(i), count + i * AD_DISTANCE + FIRST_AD_POSITION);
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                    break;

                default:
            }
        }
    }
}
