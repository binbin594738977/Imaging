package com.jtech.imaging.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.jtech.imaging.R;
import com.jtech.imaging.model.ResultsModel;
import com.jtech.imaging.model.SearchPhotoModel;
import com.jtech.imaging.mvp.contract.SearchContract;
import com.jtech.imaging.mvp.presenter.SearchPresenter;
import com.jtech.imaging.util.ActivityJump;
import com.jtech.imaging.view.adapter.LoadMoreFooterAdapter;
import com.jtech.imaging.view.adapter.SearchAdapter;
import com.jtech.imaging.view.widget.CoverView;
import com.jtech.imaging.view.widget.RxCompat;
import com.jtech.imaging.view.widget.popup.SearchRecordPopup;
import com.jtech.listener.OnItemClickListener;
import com.jtech.listener.OnLoadListener;
import com.jtech.view.JRecyclerView;
import com.jtech.view.RecyclerHolder;
import com.jtech.view.RefreshLayout;
import com.jtechlib.Util.DeviceUtils;
import com.jtechlib.Util.PairChain;
import com.jtechlib.view.activity.BaseActivity;
import com.umeng.analytics.MobclickAgent;

import butterknife.Bind;
import rx.functions.Action1;

/**
 * 搜索页
 * Created by jianghan on 2016/9/27.
 */

public class SearchActivity extends BaseActivity implements SearchContract.View, SearchView.OnQueryTextListener, View.OnFocusChangeListener, View.OnClickListener, RefreshLayout.OnRefreshListener, OnLoadListener, OnItemClickListener, CoverView.OnCoverCancelListener, SearchRecordPopup.OnSearchRecordClick {

    public static final String KEY_SEARCH_QUERY = "SEARCH_QUERY";
    private static final int REQUEST_PHOTO_DETAIL_CODE = 0x0123;

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.refreshlayout)
    RefreshLayout refreshLayout;
    @Bind(R.id.jrecyclerview)
    JRecyclerView jRecyclerView;
    @Bind(R.id.content)
    CoordinatorLayout content;
    @Bind(R.id.fab)
    FloatingActionButton floatingActionButton;
    @Bind(R.id.content_cover)
    CoverView coverView;

    private SearchView searchView;
    private SearchAdapter searchAdapter;
    private SearchContract.Presenter presenter;
    private SearchRecordPopup searchRecordPopup;

    @Override
    protected void initVariables(Bundle bundle) {
        //获取搜索信息
        String query = bundle.getString(KEY_SEARCH_QUERY, "");
        //绑定P类
        presenter = new SearchPresenter(getActivity(), this, query);
    }

    @Override
    protected void initViews(Bundle bundle) {
        setContentView(R.layout.activity_search);
        //设置toolbar
        setupToolbar(toolbar)
                .setContentInsetStartWithNavigation(0)
                .setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp, this);
        //实例化popup
        searchRecordPopup = new SearchRecordPopup(getActivity());
        //实例化适配器并设置
        searchAdapter = new SearchAdapter(getActivity(), DeviceUtils.getScreenWidth(getActivity()));
        jRecyclerView.setAdapter(searchAdapter, new LoadMoreFooterAdapter());
        //设置layoutmanagaer
        jRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        //设置搜索事件
        searchRecordPopup.setOnSearchRecordClick(this);
        //设置下拉刷新
        refreshLayout.setOnRefreshListener(this);
        //设置加载更多
        jRecyclerView.setLoadMore(true);
        //设置加载更多监听
        jRecyclerView.setOnLoadListener(this);
        //设置图片点击事件
        jRecyclerView.setOnItemClickListener(this);
        //列表滚动事件
        jRecyclerView.addOnScrollListener(new OnScrollListener());
        //设置fab的点击事件
        RxCompat.clickThrottleFirst(floatingActionButton, new FabClick());
        //设置覆盖层取消监听
        coverView.setOnCoverCancelListener(this);
    }

    @Override
    protected void loadData() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //实例化menu
        toolbar.inflateMenu(R.menu.menu_search);
        //获取到搜索框的视图
        MenuItem menuItem = menu.findItem(R.id.menu_search_search);
        searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        //设置搜索确定监听
        searchView.setOnQueryTextListener(this);
        //设置焦点变化监听
        searchView.setOnQueryTextFocusChangeListener(this);
        //设置默认信息
        searchView.setQueryHint(getResources().getString(R.string.search_hint));
        //设置搜索内容
        searchSubmit(presenter.getSearchQuery());
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (searchSubmit(query)) {
            //存储搜索记录
            searchRecordPopup.addSearchRecord(query);
            return true;
        }
        return false;
    }

    /**
     * 搜索提交
     *
     * @param query
     * @return
     */
    private boolean searchSubmit(String query) {
        if (!TextUtils.isEmpty(query.trim())) {
            presenter.setSearchQuery(query);
            //设置标题栏
            toolbar.setTitle(query);
            //关闭搜索状态
            searchView.onActionViewCollapsed();
            //发起下拉刷新
            refreshLayout.startRefreshing();
            return true;
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            //失去焦点则收回搜索框
            searchView.onActionViewCollapsed();
            //隐藏覆盖层
            coverView.hideContentCover();
            //显示fab
            floatingActionButton.show();
            //取消popup
            searchRecordPopup.dismiss();
        } else {
            //显示覆盖层
            coverView.showContentCover();
            //隐藏fab
            floatingActionButton.hide();
            //显示popup
            searchRecordPopup.showSearchRecord(toolbar);
        }
    }

    @Override
    public void onClick(View v) {
        //后退
        onBackPressed();
    }

    @Override
    public void success(SearchPhotoModel searchPhotoModel, boolean loadMore) {
        refreshLayout.refreshingComplete();
        jRecyclerView.setLoadCompleteState();
        searchAdapter.setSearchPhotoModel(searchPhotoModel, loadMore);
        //显示搜索结果
        if (!loadMore) {
            if (searchPhotoModel.getTotal() > 0) {
                Snackbar.make(content, searchAdapter.getTotalCount() + " image and " + searchAdapter.getTotalPage() + " page", Snackbar.LENGTH_SHORT).show();
            } else {
                jRecyclerView.setLoadFinishState();
            }
        }
    }

    @Override
    public void fail(String message) {
        Snackbar.make(content, message, Snackbar.LENGTH_SHORT).show();
        refreshLayout.refreshingComplete();
        jRecyclerView.setLoadFailState();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ActivityCompat.finishAfterTransition(getActivity());
    }

    @Override
    public void onRefresh() {
        presenter.searchPhotoList(presenter.getSearchQuery(), searchAdapter.getPage(false), false);
    }

    @Override
    public void loadMore() {
        if (searchAdapter.getPage(true) > searchAdapter.getTotalPage()) {
            //设置为没有更多
            jRecyclerView.setLoadFinishState();
        } else {
            //发起请求更多
            presenter.searchPhotoList(presenter.getSearchQuery(), searchAdapter.getPage(true), true);
        }
    }

    @Override
    public void onItemClick(RecyclerHolder recyclerHolder, View view, int position) {
        //获得当前的数据对象
        ResultsModel resultsModel = searchAdapter.getItem(position);
        //跳转到详情页
        Bundle bundle = new Bundle();
        bundle.putString(PhotoDetailActivity.KEY_IMAGE_ID, resultsModel.getId());
        bundle.putString(PhotoDetailActivity.KEY_IMAGE_NAME, resultsModel.getUser().getName());
        bundle.putString(PhotoDetailActivity.KEY_IMAGE_URL, resultsModel.getUrls().getRaw());
        Pair[] pairs = PairChain
                .build(floatingActionButton, getString(R.string.fab))
                .addPair(searchAdapter.getParallaxView(recyclerHolder), getString(R.string.image))
                .toArray();
        ActivityJump.build(getActivity(), PhotoDetailActivity.class)
                .addBundle(bundle)
                .makeSceneTransitionAnimation(pairs)
                .jumpForResult(REQUEST_PHOTO_DETAIL_CODE);
    }

    @Override
    public void onRecordClick(String keyword) {
        searchSubmit(keyword);
    }

    @Override
    public void onCancel() {
        searchView.onActionViewCollapsed();
    }

    /**
     * fab的点击事件
     */
    private class FabClick implements Action1<Void> {
        @Override
        public void call(Void aVoid) {
            //点击开启搜索
            searchView.onActionViewExpanded();
        }
    }

    /**
     * 列表的滚动监听
     */
    private class OnScrollListener extends RecyclerView.OnScrollListener {
        private boolean fabShowing = true;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            //视差滚动
            searchAdapter.animateImage(recyclerView);
            //隐藏或显示fab
            if (dy > 0 && fabShowing) {
                fabShowing = false;
                floatingActionButton.hide();
            } else if (dy < 0 && !fabShowing) {
                fabShowing = true;
                floatingActionButton.show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PHOTO_DETAIL_CODE) {//从详情跳转回来的
            searchAdapter.animateImage(jRecyclerView);
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