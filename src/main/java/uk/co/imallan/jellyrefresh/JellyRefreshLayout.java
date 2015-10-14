package uk.co.imallan.jellyrefresh;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;


/**
 * User: Yilun Chen
 * Date: 15/7/9
 */
public class JellyRefreshLayout extends PullToRefreshLayout {
    private AnimationDrawable mAnimationDrawable=null;
    JellyRefreshListener mJellyRefreshListener;
    private JellyRefreshLayout mJellyRefreshLayout;

    private String mLoadingText = "Loading...";

    private int mLoadingTextColor;

    private int mJellyColor;

    public JellyRefreshLayout(Context context) {
        super(context);
        mJellyRefreshLayout=this;
        setupHeader();
    }

    public JellyRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mJellyRefreshLayout=this;
        setAttributes(attrs);
        setupHeader();
    }

    public JellyRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mJellyRefreshLayout=this;
        setAttributes(attrs);
        setupHeader();
    }

    public JellyRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mJellyRefreshLayout=this;
        setAttributes(attrs);
        setupHeader();
    }

    private void setAttributes(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.JellyRefreshLayout);
        try {
            Resources resources = getResources();
            mLoadingText = a.getString(R.styleable.JellyRefreshLayout_android_text);
            mLoadingTextColor = a.getColor(R.styleable.JellyRefreshLayout_android_textColor,
                    resources.getColor(android.R.color.white));
            mJellyColor = a.getColor(R.styleable.JellyRefreshLayout_jellyColor,
                    resources.getColor(android.R.color.holo_blue_bright));

        } finally {
            a.recycle();
        }
    }

    public void setRefreshListener(JellyRefreshListener jellyRefreshListener) {
        this.mJellyRefreshListener = jellyRefreshListener;
    }

    private void setupHeader() {
        if (isInEditMode()) {
            return;
        }

        View headerView = LayoutInflater.from(getContext()).inflate(R.layout.jelly_view_pull_header, null);
        final  uk.co.imallan.jellyrefresh.JellyView jellyView = ( uk.co.imallan.jellyrefresh.JellyView) headerView.findViewById(R.id.jelly_header);
        final ImageView textLoading = (ImageView) headerView.findViewById(R.id.text_loading);
        jellyView.setJellyColor(mJellyColor);
        final float headerHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        setHeaderHeight(headerHeight);
        final float pullHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
        setPullHeight(pullHeight);
        setHeaderView(headerView);
        setPullToRefreshListener(
                new PullToRefreshListener() {
                    @Override
                    public void onRefresh(PullToRefreshLayout pullToRefreshLayout) {
                        if (mJellyRefreshListener != null) {
                            mJellyRefreshListener.onRefresh(JellyRefreshLayout.this);
                        }
                        jellyView.setMinimumHeight((int) (headerHeight));
                        ValueAnimator animator = ValueAnimator.ofInt(jellyView.getJellyHeight(), 0);
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT){
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    jellyView.setJellyHeight((int) animation.getAnimatedValue());
                                    jellyView.invalidate();
                                }
                            });
                        }
                        else
                        {
                            animator.addListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    jellyView.setJellyHeight((int)animator.getAnimatedValue());

                                    jellyView.invalidate();
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            });

                        }

                        animator.setInterpolator(new OvershootInterpolator(3));
                        animator.setDuration(200);
                        animator.start();
                        pullToRefreshLayout.postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        textLoading.setVisibility(View.VISIBLE);
                                        //textLoading加载动画
                                        textLoading.setImageResource(R.drawable.loading);
                                        mAnimationDrawable = (AnimationDrawable) textLoading.getDrawable();
                                        mAnimationDrawable.start();
                                    }
                                }, 120
                        );
                    }
                }
        );
        setPullingListener(new PullToRefreshLayout.PullToRefreshPullingListener() {
            @Override
            public void onPulling(PullToRefreshLayout pullToRefreshLayout, float fraction) {
                textLoading.setVisibility(View.VISIBLE);//md
                jellyView.setMinimumHeight((int) (headerHeight * MathUtils.constrains(0, 1, fraction)));
                jellyView.setJellyHeight((int) (pullHeight * Math.max(0, fraction - 1)));
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN){
                    mJellyRefreshLayout.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.translate_backgroud));
                }else {
                    mJellyRefreshLayout.setBackground(getContext().getResources().getDrawable(R.drawable.translate_backgroud));
                }

                jellyView.invalidate();

            }

            @Override
            public void onReleasing(PullToRefreshLayout pullToRefreshLayout, float fraction) {
                if (!pullToRefreshLayout.isRefreshing()) {
                    textLoading.setVisibility(View.VISIBLE);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN){
                        mJellyRefreshLayout.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.translate_backgroud));
                    }else {
                        mJellyRefreshLayout.setBackground(getContext().getResources().getDrawable(R.drawable.translate_backgroud));
                    }

                    // //textLoading结束动画
                    if(mAnimationDrawable !=null){
                        mAnimationDrawable.stop();
                    }

                }
            }
        });
    }


    @Override
    public void setPullHeight(float pullHeight) {
        super.setPullHeight(pullHeight);
    }

    @Override
    public void setHeaderHeight(float headerHeight) {
        super.setHeaderHeight(headerHeight);
    }

    @Override
    public boolean isRefreshing() {
        return super.isRefreshing();
    }

    @Override
    public void finishRefreshing() {
        super.finishRefreshing();
    }

    public interface JellyRefreshListener {

        void onRefresh(JellyRefreshLayout jellyRefreshLayout);

    }

}
