package uk.co.imallan.jellyrefresh;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;

/**
 * Created by yilun
 * on 09/07/15.
 */
class PullToRefreshLayout extends FrameLayout {

    private float mTouchStartY;

    private PullToRefreshLayout view;
    private float mCurrentY;

    private View mChildView;

    private static DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(10);

    private float mPullHeight;

    private float mHeaderHeight;

    private boolean isRefreshing;

    private PullToRefreshListener mPullToRefreshListener;

    private PullToRefreshPullingListener mPullToRefreshPullingListener;

    private FrameLayout mHeader;

    public PullToRefreshLayout(Context context) {
        super(context);
        // 触发移动事件的最短距离，如果小于这个距离就不触发移动控件
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
        view=this;
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 触发移动事件的最短距离，如果小于这个距离就不触发移动控件
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
        view=this;
    }

    public PullToRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 触发移动事件的最短距离，如果小于这个距离就不触发移动控件
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
        view=this;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PullToRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // 触发移动事件的最短距离，如果小于这个距离就不触发移动控件
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        if (getChildCount() > 1) {
            throw new RuntimeException("You can only attach one child");
        }


        mPullHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                150,
                getContext().getResources().getDisplayMetrics());

        mHeaderHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                100,
                getContext().getResources().getDisplayMetrics());

        this.post(() -> {
            mChildView = getChildAt(0);
            addHeaderContainer();
        });

    }

    public void setHeaderView(View headerView) {
        post(() -> mHeader.addView(headerView));
    }

    public void setPullHeight(float pullHeight) {
        this.mPullHeight = pullHeight;
    }

    public void setHeaderHeight(float headerHeight) {
        this.mHeaderHeight = headerHeight;
    }

    public float getmPullHeight() {
        return mPullHeight;
    }

    public float getmHeaderHeight() {
        return mHeaderHeight;
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    private void addHeaderContainer() {
        FrameLayout headerContainer = new FrameLayout(getContext());
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        layoutParams.gravity = Gravity.TOP;
        headerContainer.setLayoutParams(layoutParams);

        mHeader = headerContainer;
        addViewInternal(headerContainer);
        setUpChildViewAnimator();
    }

    private void setUpChildViewAnimator() {
        if (mChildView == null) {
            return;
        }
        mChildView.animate().setInterpolator(new DecelerateInterpolator());
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT){
            mChildView.animate().setUpdateListener(animation -> {
                        int height = (int) mChildView.getTranslationY();
                        mHeader.getLayoutParams().height = height;
                        mHeader.requestLayout();
                        if (mPullToRefreshPullingListener != null) {
                            mPullToRefreshPullingListener.onReleasing(this, height / mHeaderHeight);
                        }
                    }
            );
        }else
        {
            mChildView.animate().setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    int height = (int) mChildView.getTranslationY();
                    mHeader.getLayoutParams().height = height;
                    mHeader.requestLayout();
                    if (mPullToRefreshPullingListener != null) {
                        mPullToRefreshPullingListener.onReleasing(view, height / mHeaderHeight);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }

    }

    private void addViewInternal(@NonNull View child) {
        super.addView(child);
    }

    @Override
    public void addView(@NonNull View child) {
        if (getChildCount() >= 1) {
            throw new RuntimeException("You can only attach one child");
        }
        mChildView = child;
        super.addView(child);
        setUpChildViewAnimator();
    }

    public boolean canChildScrollUp() {
        if (mChildView == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 14) {
            if (mChildView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mChildView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mChildView, -1) || mChildView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mChildView, -1);
        }
    }

    private int mTouchSlop;
    // 上一次触摸时的X坐标
    private float mPrevX;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (isRefreshing) {
            return true;
        }
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchStartY = e.getY();
                mCurrentY = mTouchStartY;
                mPrevX = e.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float currentY = e.getY();
                float dy = currentY - mTouchStartY;


                final float eventX = e.getX();
                float xDiff = Math.abs(eventX - mPrevX);
                // Log.d("refresh" ,"move----" + eventX + "   " + mPrevX + "   " + mTouchSlop);
//                // debug 增加30的容差，让下拉刷新在竖直滑动时就可以触发
//                if (xDiff > mTouchSlop + 30) {
//                    Log.i("pullrefreshoncaro","into xDiff > mTouchSlop + 60");
//                    if (dy > 0 && !canChildScrollUp()) {
//                        Log.i("pullrefreshoncaro","return true dy > 0 && !canChildScrollUp()");
//                        return true;
//                    }else{
//                        Log.i("pullrefreshoncaro"," return false");
//                        return false;
//                    }
//                }

                // debug 增加5的容差，让下拉刷新在竖直滑动时就可以触发
                if (xDiff > mTouchSlop +5) {
                    Log.i("pullrefreshoncaro","into xDiff > mTouchSlop + 60");
                    if (dy > 0 && !canChildScrollUp()) {
                        Log.i("pullrefreshoncaro","return true dy > 0 && !canChildScrollUp()");
                        return true;
                    }else{
                        Log.i("pullrefreshoncaro"," return false");
                        return false;
                    }
                }
                break;
        }
        return super.onInterceptTouchEvent(e);
    }

    public void autoRefesh(){
        mHeader.getLayoutParams().height = (int) 70;
        mHeader.requestLayout();
        if (mPullToRefreshPullingListener != null) {
            mPullToRefreshPullingListener.onPulling(this, 80 / mHeaderHeight);
        }
    }


    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {
        if (isRefreshing) {
            return super.onTouchEvent(e);
        }
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                mCurrentY = e.getY();
                float dy = MathUtils.constrains(
                        0,
                        mPullHeight * 2,
                        mCurrentY - mTouchStartY);
                if (mChildView != null) {
                    float offsetY = decelerateInterpolator.getInterpolation(dy / mPullHeight / 2) * dy / 2;
                    mChildView.setTranslationY(
                            offsetY
                    );
                    mHeader.getLayoutParams().height = (int) offsetY;
                    mHeader.requestLayout();
                    if (mPullToRefreshPullingListener != null) {
                        mPullToRefreshPullingListener.onPulling(this, offsetY / mHeaderHeight);
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mChildView != null) {
                    if (mChildView.getTranslationY() >= mHeaderHeight) {
                        mChildView.animate().translationY(mHeaderHeight).start();
                        isRefreshing = true;
                        if (mPullToRefreshListener != null) {
                            mPullToRefreshListener.onRefresh(this);
                        }
                    } else {
                        mChildView.animate().translationY(0).start();
                    }

                }
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    public void setPullToRefreshListener(PullToRefreshListener pullToRefreshListener) {
        this.mPullToRefreshListener = pullToRefreshListener;
    }

    public void setPullingListener(PullToRefreshPullingListener pullingListener) {
        this.mPullToRefreshPullingListener = pullingListener;
    }

    public void finishRefreshing() {
        if (mChildView != null) {
            mChildView.animate().translationY(0).start();
        }
        isRefreshing = false;
    }


    interface PullToRefreshListener {

        void onRefresh(PullToRefreshLayout pullToRefreshLayout);

    }

    interface PullToRefreshPullingListener {

        void onPulling(PullToRefreshLayout pullToRefreshLayout, float fraction);

        void onReleasing(PullToRefreshLayout pullToRefreshLayout, float fraction);

    }
}