package com.jtech.imaging.view.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.jtech.imaging.R;
import com.jtech.imaging.mvp.contract.WallpaperContract;
import com.jtech.imaging.mvp.presenter.WallpaperPresenter;
import com.jtech.imaging.strategy.PhotoResolutionStrategy;
import com.jtech.imaging.view.widget.LoadingView;
import com.jtech.imaging.view.widget.RxCompat;
import com.jtech.imaging.view.widget.dialog.WallpaperDialog;
import com.jtechlib.Util.DeviceUtils;
import com.jtechlib.Util.ImageUtils;
import com.jtechlib.view.activity.BaseActivity;
import com.umeng.analytics.MobclickAgent;

import butterknife.Bind;
import rx.functions.Action1;
import uk.co.senab.photoview.PhotoView;

/**
 * 壁纸设置页面
 * Created by jianghan on 2016/10/14.
 */

public class WallpaperActivity extends BaseActivity implements WallpaperContract.View {
    public static final String KEY_IMAGE_URL = "IMAGE_URL";
    public static final long IMAGE_ANIMATION_DURATION = 450;

    @Bind(R.id.contentloading)
    LoadingView loadingView;
    @Bind(R.id.photoview_wallpaper)
    PhotoView photoView;
    @Bind(R.id.fab)
    FloatingActionButton floatingActionButton;
    @Bind(R.id.tablayout_wallpaper)
    TabLayout tabLayout;

    private WallpaperContract.Presenter presenter;

    @Override
    protected void initVariables(Bundle bundle) {
        //获取图片url
        String imageUrl = bundle.getString(KEY_IMAGE_URL);
        //实例化P类
        this.presenter = new WallpaperPresenter(getActivity(), this, imageUrl);
    }

    @Override
    protected void initViews(Bundle bundle) {
        setContentView(R.layout.activity_wallpaper);
        //设置tab
        setupTab();
        //设置fab的点击事件
        RxCompat.clickThrottleFirst(floatingActionButton, new FabClick());
    }

    @Override
    protected void loadData() {
        int screenWidth = DeviceUtils.getScreenWidth(getActivity());
        if (!presenter.isLocalImage()) {
            //显示加载动画
            loadingView.show();
            //默认加载详情页设置的分辨率
            final long startTimeMillis = System.currentTimeMillis();
            //图片请求
            ImageUtils.requestImage(getActivity(), presenter.getUrl(screenWidth), new Action1<Bitmap>() {
                @Override
                public void call(Bitmap bitmap) {
                    if (presenter.isRightBitmap(getActivity(), bitmap)) {
                        //隐藏加载对象
                        loadingView.hide();
                        //设置图片
                        photoView.setImageBitmap(bitmap);
                        if (System.currentTimeMillis() - startTimeMillis > 500) {
                            photoView.setAlpha(0f);
                            photoView
                                    .animate()
                                    .alpha(1f)
                                    .setDuration(IMAGE_ANIMATION_DURATION)
                                    .setInterpolator(new AccelerateDecelerateInterpolator())
                                    .start();
                        }
                    }
                }
            });
        } else {
            ImageUtils.requestLocalImage(getActivity(), presenter.getUrl(screenWidth), screenWidth, -1, new Action1<Bitmap>() {
                @Override
                public void call(Bitmap bitmap) {
                    if (null != bitmap) {
                        loadingView.hide();
                        photoView.setImageBitmap(bitmap);
                    }
                }
            });
        }
    }

    /**
     * 设置分辨率选择的tab
     */
    private void setupTab() {
        if (!presenter.isLocalImage()) {
            //添加tab
            for (String tabName : getResources().getStringArray(R.array.imageWallpaperResolutiorn)) {
                tabLayout.addTab(tabLayout.newTab().setText(tabName));
            }
            //选择默认项
            tabLayout
                    .getTabAt(PhotoResolutionStrategy.getSelectStrategyPosition(getActivity()))
                    .select();
            //设置tab选择事件
            tabLayout.addOnTabSelectedListener(new TabSelectedListener());
        } else {
            tabLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void setWallpaperSuccess() {
        loadingView.hide();
        Snackbar.make(tabLayout, "Wallpaper set success", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void setWallpaperFail() {
        loadingView.hide();
        Snackbar.make(tabLayout, "Wallpaper set fail", Snackbar.LENGTH_SHORT).show();
    }

    /**
     * fab的点击事件
     */
    private class FabClick implements Action1<Void> {
        @Override
        public void call(Void aVoid) {
            photoView.invalidate();
            WallpaperDialog
                    .build(getActivity(), photoView.getVisibleRectangleBitmap())
                    .setDoneClick(new WallpaperSetState())
                    .show();
        }
    }

    /**
     * 壁纸设置结果
     */
    private class WallpaperSetState implements WallpaperDialog.OnWallpaperSetState {
        @Override
        public void done(Bitmap bitmap) {
            loadingView.show();
            //设置为壁纸
            presenter.setWallpaper(bitmap);
        }
    }

    /**
     * tab选择事件
     */
    private class TabSelectedListener implements TabLayout.OnTabSelectedListener {

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            //设置加载图片的清晰度
            presenter.setSelectPosition(tab.getPosition());
            //重新加载图片
            loadData();
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    }

    public void onResume() {
        super.onResume();
        //友盟统计
        MobclickAgent.onPageStart(TAG);
        MobclickAgent.onResume(this);
    }

    public void onPause() {
        super.onPause();
        //友盟统计
        MobclickAgent.onPageEnd(TAG);
        MobclickAgent.onPause(this);
    }
}