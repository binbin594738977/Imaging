package com.jtech.imaging.mvp.presenter;

import android.content.Context;
import android.util.Log;

import com.jtech.imaging.cache.PhotoCache;
import com.jtech.imaging.model.PhotoModel;
import com.jtech.imaging.mvp.contract.MainContract;
import com.jtech.imaging.net.API;

import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static android.content.ContentValues.TAG;

/**
 * 演示用逻辑处理实现类
 * Created by wuxubaiyang on 16/4/16.
 */
public class MainPresenter implements MainContract.Presenter {

    private MainContract.View view;
    private Context context;

    public MainPresenter(Context context, MainContract.View view) {
        this.context = context;
        this.view = view;
    }

    @Override
    public void requestCachePhotoList() {
        Observable.just("")
                .subscribeOn(Schedulers.io())
                .map(new Func1<String, List<PhotoModel>>() {
                    @Override
                    public List<PhotoModel> call(String s) {
                        return PhotoCache.get(context).getFirstPagePhotos();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<PhotoModel>>() {
                    @Override
                    public void call(List<PhotoModel> photoModels) {
                        view.cacheSuccess(photoModels);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "call: " + throwable.getMessage());
                    }
                });
    }

    @Override
    public void requestPhotoList(int pageIndex, int displayNumber, String orderBy, final boolean loadMore) {
        API.get()
                .unsplashApi(context)
                .photos(pageIndex, displayNumber, orderBy)
                .subscribeOn(Schedulers.io())
                .map(new Func1<List<PhotoModel>, List<PhotoModel>>() {
                    @Override
                    public List<PhotoModel> call(List<PhotoModel> photoModels) {
                        if (!loadMore) {//下拉刷新的时候才缓存数据
                            //设置缓存
                            PhotoCache.get(context).setFirstPagePhotos(photoModels);
                        }
                        return photoModels;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<PhotoModel>>() {
                    @Override
                    public void call(List<PhotoModel> photoModels) {
                        view.success(photoModels, loadMore);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        view.fail(throwable.getMessage());
                    }
                });
    }
}