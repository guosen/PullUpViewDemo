# ViewDragHelper简介

ViewDragHelper is a utility class for writing custom ViewGroups. It offers a number of useful operations and state tracking for allowing a user to drag and reposition views within their parent ViewGroup

ViewDragHelper可以帮助我们解决负责的手势操作。它是官方所提供的一个专门为自定义ViewGroup处理拖拽的手势类。

# 首先我们看下效果图

![test](/Users/guosenlin/Desktop/test.gif)

## 开始编码

布局文件

```xml
<?xml version="1.0" encoding="utf-8"?>
    <com.guosen.pullupviewdemo.PullUpDrawerLayout
        android:layout_width="wrap_content"
        xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_height="wrap_content">
        <ImageView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:scaleType="centerCrop"
            android:src="@mipmap/ic_bg"/>
        <include layout="@layout/view_pull"/>
    </com.guosen.pullupviewdemo.PullUpDrawerLayout>
```

看下布局文件，我们其实就是自定义一个ViewGroup,然后里面包括一个内容布局（就是背景），一个是上拉的View。

自定义就不多说了，新建一个类继承ViewGroup，里面持有ViewDragHelper。如下：

```java
public class PullUpDrawerLayout extends ViewGroup implements View.OnClickListener {

    private static String TAG = "PullUpDrawerLayout";
    private ViewDragHelper mBottomDragHelper;
    private View mContentView;//内容布局
    private View mPullView;//抽屉布局
    private int mCurTop;
    private boolean mIsOpenState = true;//当前状态 打开还是关闭
    }
```

初始化 ViewDragHelper

```java
private void init(){
    //使用静态方法 构造 ViewDragHelper，这个时候传入一个ViewDragHelper.Callback
    mBottomDragHelper = ViewDragHelper.create(this,1.0f,new ViewDragHelperCallBack());
    mBottomDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_BOTTOM);

}
```

这里比较重要的一个参数就是CallBack回调了，这个其实是整个ViewDragHelper的核心，待会儿说下，我们先把大概Vew画出来。

## 测量

```java
//测量背景图层的尺寸
mContentView = getChildAt(0);
MarginLayoutParams params = (MarginLayoutParams) mContentView.getLayoutParams();
int childSpecWidth = MeasureSpec.makeMeasureSpec(meaureWidth - (params.leftMargin  + params.rightMargin),MeasureSpec.EXACTLY);
int childSpecHeight = MeasureSpec.makeMeasureSpec(mesureHeight - (params.topMargin + params.bottomMargin),MeasureSpec.EXACTLY);
mContentView.measure(childSpecWidth,childSpecHeight);

//测量上拉布局的尺寸
mPullView = getChildAt(1);
mPullView.measure(childSpecWidth,childSpecHeight);
```

## 布局

```java
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
```

主要的还是刚才说的那个回调类

```java
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
```

tryCaptureView：如果返回true表示捕获相关View，你可以根据第一个参数child决定捕获哪个View。

clampViewPositionVertical：计算child垂直方向的位置，top表示y轴坐标（相对于ViewGroup），默认返回0（如果不复写该方法）。这里，你可以控制垂直方向可移动的范围。

clampViewPositionHorizontal：与clampViewPositionVertical类似，只不过是控制水平方向的位置

 

## 重写自定义控件的事件拦截

把事件交给dragHelper处理

```Java
@Override
public boolean onTouchEvent(MotionEvent event) {
    mBottomDragHelper.processTouchEvent(event);
    return true;
}

@Override
public boolean onInterceptHoverEvent(MotionEvent event) {
     return mBottomDragHelper.shouldInterceptTouchEvent(event);
}
```

 # 源码粗略解读

## 初始化

```Java
public static ViewDragHelper create(@NonNull ViewGroup forParent, float sensitivity,
        @NonNull Callback cb) {
    final ViewDragHelper helper = create(forParent, cb);
    helper.mTouchSlop = (int) (helper.mTouchSlop * (1 / sensitivity));
    return helper;
}
```

 源码里面并没有暴露构造方法，而是 采用内部静态方法创建对象。sensitivity 这个引人注目，其实就是灵敏度的意思，可以看到他的值越大，mTouchSlop的值越小，mTouchSlop 意思就是触发系统触摸感知的最小距离。

## smoothSlideViewTo方法

该方法用于平顺地滑动控件到指定位置。 child代表子控件对象, finalLeft代表滑动结束时，子控件左边所处的位置， finalTop 代表子控件顶部的位置

## tryCaptureView

该方法返回布尔值来判断当前操作的view是否可以进行捕获。demo中需要这三个view都能被捕获到，所以很简单只需与参数的child做对比即可

```java
boolean tryCaptureViewForDrag(View toCapture, int pointerId) {
    if (toCapture == mCapturedView && mActivePointerId == pointerId) {
        // Already done!
        return true;
    }
    if (toCapture != null && mCallback.tryCaptureView(toCapture, pointerId)) {
        mActivePointerId = pointerId;
        captureChildView(toCapture, pointerId);
        return true;
    }
    return false;
}
```

 ## clampViewPositionHorizontal



- child：当前操作的view

- left: 将要到达的水平方向的距离

- dx: 相对于当前位置的偏移量

- return：所处的水平距 

  ## onViewReleased

  手指释放的时候

  ```java
  private void dispatchViewReleased(float xvel, float yvel) {
      mReleaseInProgress = true;
      mCallback.onViewReleased(mCapturedView, xvel, yvel);
      mReleaseInProgress = false;
  
      if (mDragState == STATE_DRAGGING) {
          // onViewReleased didn't call a method that would have changed this. Go idle.
          setDragState(STATE_IDLE);
      }
  }
  ```

可以看到 在分发的时候调用

## 事件冲突

其实会存在问题，那就是事件冲突，比如你向上拉的View的滑动与内容的滑动，这个可以自己想想

源码地址 <https://github.com/guosen/PullUpViewDemo>

参考链接 <https://blog.csdn.net/qq_22393017/article/details/78197442>

<https://segmentfault.com/a/1190000016128693>


