package com.guosen.pullupviewdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.customview.widget.ViewDragHelper;

/**
 * <pre>
 *     author : guosenlin
 *     e-mail : guosenlin91@gmail.com
 *     time   : 2019/12/16
 *     desc   : 一个可以上拉的组件示意Layout
 *             采用ViewDragHelper实现
 *     version: 1.0
 * </pre>
 */
public class PullUpDrawerLayout extends ViewGroup implements View.OnClickListener {

    private static String TAG = "PullUpDrawerLayout";
    private ViewDragHelper mBottomDragHelper;
    private View mContentView;//内容布局
    private View mPullView;//抽屉布局
    private int mCurTop;
    private boolean mIsOpenState = true;//当前状态 打开还是关闭
    public PullUpDrawerLayout(Context context) {
        super(context);
    }

    public PullUpDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PullUpDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int meaureWidth = MeasureSpec.getSize(widthMeasureSpec);
        int mesureHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(meaureWidth,mesureHeight);

        //测量背景图层的尺寸
        mContentView = getChildAt(0);
        MarginLayoutParams params = (MarginLayoutParams) mContentView.getLayoutParams();
        int childSpecWidth = MeasureSpec.makeMeasureSpec(meaureWidth - (params.leftMargin  + params.rightMargin),MeasureSpec.EXACTLY);
        int childSpecHeight = MeasureSpec.makeMeasureSpec(mesureHeight - (params.topMargin + params.bottomMargin),MeasureSpec.EXACTLY);
        mContentView.measure(childSpecWidth,childSpecHeight);

        //测量上拉布局的尺寸
        mPullView = getChildAt(1);
        mPullView.measure(childSpecWidth,childSpecHeight);



        //设置监听事件
        mPullView.findViewById(R.id.btnClose).setOnClickListener(this);

    }

    /**
     *   left top right bottom  当前ViewGroup相对于其父控件的坐标位置
     * @param isChanged  该参数指出当前ViewGroup的尺寸或者位置是否发生了改变
     * @param left
     * @param top
     * @param right
     * @param bottom
     */

    @Override
    protected void onLayout(boolean isChanged, int left, int top, int right, int bottom) {

        if (isChanged){
            Log.i(TAG,"onLayout();layout is changed.");
            //测量之后开始布局背景图层
            MarginLayoutParams params = (MarginLayoutParams) mContentView.getLayoutParams();
            mContentView.layout(params.leftMargin,params.topMargin,mContentView.getMeasuredWidth()+ params.leftMargin,mContentView.getMeasuredHeight()+params.topMargin);


            //测量之后开始布局上拉的布局
            params = (MarginLayoutParams) mPullView.getLayoutParams();
            mPullView.layout(params.leftMargin,mCurTop+params.topMargin,mPullView.getMeasuredWidth()+params.leftMargin,mCurTop+mPullView.getMeasuredHeight()+params.topMargin);
        }
    }
    /**
     *
     * @param p
     * @return
     */
    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
         return new MarginLayoutParams(p);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(),attrs);
    }

    private void init(){
        //使用静态方法 构造 ViewDragHelper，这个时候传入一个ViewDragHelper.Callback
        mBottomDragHelper = ViewDragHelper.create(this,1.0f,new ViewDragHelperCallBack());
        mBottomDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_BOTTOM);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnClose:
                closeDrawer();
                default:
        }
    }

    //定义一个回调类
    private class ViewDragHelperCallBack extends ViewDragHelper.Callback {
         //返回ture则表示可以捕获该view,手指摸上一瞬间执行
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child == mPullView;
        }
        /**
         * setEdgeTrackingEnabled设置的边界滑动时触发
         * captureChildView是为了让tryCaptureView返回false依旧生效
         * @param edgeFlags
         * @param pointerId
         */
        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
             mBottomDragHelper.captureChildView(mPullView,pointerId);
        }
        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            /**
             * 计算child垂直方向的位置，top表示y轴坐标（相对于ViewGroup），默认返回0（如果不复写该方法）。这里，你可以控制垂直方向可移动的范围
             * 如果是向下pull:  y: +value
             * 这里是返回View要被拉到的位置
             */
            return -Math.max(Math.min(-top,0),-mPullView.getHeight());
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
             //手指释放的时候调用
            float movePrecent = (releasedChild.getHeight()-releasedChild.getTop())/(float)releasedChild.getHeight();
            int finalTop = (xvel >= 0 && movePrecent>0.5f)?0:releasedChild.getHeight();
            mBottomDragHelper.settleCapturedViewAt(releasedChild.getLeft(),finalTop);
            Log.i(TAG,"precent: " + movePrecent);
            invalidate();
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            Log.d(TAG,"onViewPositionChanged:"+top);
            mPullView.setVisibility(changedView.getHeight()+top == 0 ? GONE:VISIBLE);
            mCurTop = top;
            requestLayout();
        }

        // 这个用来控制垂直移动的边界范围，单位是像素
        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
             if (mPullView == null) return  0;
             return mPullView == child ? mPullView.getHeight() : 0;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (state == ViewDragHelper.STATE_IDLE){
                mIsOpenState = mPullView.getTop() == 0;
            }
        }
    }

    @Override
    public void computeScroll() {
        if (mBottomDragHelper.continueSettling(true)){
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mBottomDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {
         return mBottomDragHelper.shouldInterceptTouchEvent(event);
    }

    public boolean ismIsOpenState(){
        return mIsOpenState;
    }

    public void pullDrawer(){
        if (!mIsOpenState){
            mBottomDragHelper.smoothSlideViewTo(mPullView,mPullView.getLeft(),0);
            invalidate();
        }
    }

    public void closeDrawer(){
        if (mIsOpenState){
            mBottomDragHelper.smoothSlideViewTo(mPullView,mPullView.getLeft(),mPullView.getHeight());
            invalidate();
        }
    }
}
