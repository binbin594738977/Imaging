package com.jtech.imaging.util;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.IOException;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * 图片通用类
 * Created by wuxubaiyang on 16/4/18.
 */
public class ImageUtils {

    /**
     * 显示裁剪过的图片
     *
     * @param context
     * @param uri
     * @param imageView
     * @param targetWidth
     * @param targetHeight
     */
    public static <T extends ImageView> void showCropImage(Context context, String uri, T imageView, int targetWidth, int targetHeight) {
        Picasso.with(context)
                .load(uri)
                .centerCrop()
                .config(Bitmap.Config.RGB_565)
                .resize(targetWidth, targetHeight)
                .into(imageView);
    }

    /**
     * 显示裁剪过的图片
     *
     * @param context
     * @param uri
     * @param imageView
     * @param errorResId
     * @param placeholderResId
     * @param targetWidth
     * @param targetHeight
     */
    public static <T extends ImageView> void showCropImage(Context context, String uri, T imageView, int errorResId, int placeholderResId, int targetWidth, int targetHeight) {
        Picasso.with(context)
                .load(uri)
                .centerCrop()
                .error(errorResId)
                .config(Bitmap.Config.RGB_565)
                .placeholder(placeholderResId)
                .resize(targetWidth, targetHeight)
                .into(imageView);
    }


    /**
     * 显示圆形图片
     *
     * @param context
     * @param uri
     * @param imageView
     */
    public static <T extends ImageView> void showCircleImage(Context context, String uri, T imageView) {
        Picasso.with(context)
                .load(uri)
                .config(Bitmap.Config.RGB_565)
                .transform(new CircleTransform())
                .into(imageView);
    }

    /**
     * 显示圆形图片
     *
     * @param context
     * @param uri
     * @param imageView
     * @param errorResId
     * @param placeholderResId
     */
    public static <T extends ImageView> void showCircleImage(Context context, String uri, T imageView, int errorResId, int placeholderResId) {
        Picasso.with(context)
                .load(uri)
                .config(Bitmap.Config.RGB_565)
                .error(errorResId)
                .placeholder(placeholderResId)
                .transform(new CircleTransform())
                .into(imageView);
    }

    /**
     * 显示一张图片
     *
     * @param context
     * @param uri
     * @param imageView
     */
    public static <T extends ImageView> void showImage(Context context, String uri, T imageView) {
        Picasso.with(context)
                .load(uri)
                .config(Bitmap.Config.RGB_565)
                .into(imageView);
    }

    /**
     * 显示一张图片
     *
     * @param context
     * @param uri
     * @param imageView
     * @param errorResId
     * @param placeholderResId
     */
    public static <T extends ImageView> void showImage(Context context, String uri, T imageView, int errorResId, int placeholderResId) {
        Picasso.with(context)
                .load(uri)
                .config(Bitmap.Config.RGB_565)
                .error(errorResId)
                .placeholder(placeholderResId)
                .into(imageView);
    }

    /**
     * 请求图片，回调bitmap
     *
     * @param context
     * @param uri
     */
    public static void requestImage(final Context context, String uri, Action1<? super Bitmap> action) {
        Observable.just(uri)
                .subscribeOn(Schedulers.io())
                .map(new Func1<String, Bitmap>() {
                    @Override
                    public Bitmap call(String uri) {
                        if (!TextUtils.isEmpty(uri)) {
                            try {
                                return Picasso.with(context)
                                        .load(uri)
                                        .config(Bitmap.Config.RGB_565)
                                        .get();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(action);
    }

    /**
     * 显示大图，放弃内存缓存
     *
     * @param application
     * @param uri
     * @param imageView
     */
    public static <T extends ImageView> void showLargeImage(Application application, String uri, T imageView) {
        Picasso.with(application)
                .load(uri)
                .skipMemoryCache()
                .into(imageView);
    }

    /**
     * 显示大图，放弃内存缓存
     *
     * @param application
     * @param uri
     * @param imageView
     * @param errorResId
     * @param placeholderResId
     */
    public static <T extends ImageView> void showLargeImage(Application application, String uri, T imageView, int errorResId, int placeholderResId) {
        Picasso.with(application)
                .load(uri)
                .error(errorResId)
                .placeholder(placeholderResId)
                .skipMemoryCache()
                .into(imageView);
    }

    /**
     * 裁剪圆形
     */
    private static class CircleTransform implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }

            Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap,
                    BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);

            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    }
}