package cn.wcc.customScroll;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Copyright (C),2024/4/16 19:38-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2024/4/16 19:38     </p>
 * <p>PackagePath: cn.wcc.customScroll     </p>
 * <p>Description：       </p>
 */
public class VScrollView extends View implements GestureDetector.OnGestureListener {
	public VScrollView (Context context) {
		this(context, null);
	}

	public VScrollView (Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public VScrollView (Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public VScrollView (Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initItems();
		init(context, attrs);
	}

	private void initItems () {
		items.add(new VScrollItem(R.mipmap.preview_left_interconnect_unselect, R.mipmap.preview_left_interconnect_select,
				ClickItemType.H312_INTERCONNECT, false));
		items.add(new VScrollItem(R.mipmap.preview_left_monitor_unselect, R.mipmap.preview_left_monitor_select, ClickItemType.H312_MONITOR
				, false));
		items.add(new VScrollItem(R.mipmap.preview_left_setting_unselect, R.mipmap.preview_left_setting_select, ClickItemType.SETTING,
				false));
		items.add(new VScrollItem(R.mipmap.preview_left_alert_unselect, R.mipmap.preview_left_alert_select, ClickItemType.ALERT, true));
		items.add(new VScrollItem(R.mipmap.preview_left_clear_unselect, R.mipmap.preview_left_clear_select, ClickItemType.CLEAR, false));
		items.add(new VScrollItem(R.mipmap.preview_left_photo_unselect, R.mipmap.preview_left_photo_select, ClickItemType.PHOTO, false));
		items.add(new VScrollItem(R.mipmap.preview_left_video_unselect, R.mipmap.preview_left_video_select, ClickItemType.VIDEO, true));
		items.add(new VScrollItem(R.mipmap.preview_left_gallery_unselect, R.mipmap.preview_left_gallery_select, ClickItemType.GALLERY,
				false));
	}

	private void init (Context context, @Nullable AttributeSet attributes) {
		mContext = new WeakReference(context);
		if (mContext != null) {
			gestureDetector = new GestureDetector(mContext.get(), this);
			TypedArray attrs = mContext.get().obtainStyledAttributes(attributes, R.styleable.VScrollView);
			if (attrs != null) {
				itemCount = attrs.getInt(R.styleable.VScrollView_itemCounts, 6);
				lastItemDisPlayPercent = attrs.getFloat(R.styleable.VScrollView_lastItemPercent, 0.5f);
				mItemViewWidth = attrs.getDimensionPixelSize(R.styleable.VScrollView_itemWidth, 50);
				mItemViewHeight = attrs.getDimensionPixelSize(R.styleable.VScrollView_itemHeight, mItemViewWidth);
				mBgColor = attrs.getColor(R.styleable.VScrollView_bgColor, Color.BLUE);

				for (int index = 0; index < items.size(); index++) {
					//                        Log.e(TAG, " arrays item ${imageResourceId[index]} ", )
					normalBitmaps.add(BitmapFactory.decodeResource(mContext.get().getResources(), items.get(index).normalImageId));
					selectBitmaps.add(BitmapFactory.decodeResource(mContext.get().getResources(), items.get(index).selectImageId));
				}
				attrs.recycle();
			}
		}
		drawItemRectF = new RectF();
		mScreenFirstItemRectF = new RectF();


		mPaint = new Paint();
		mPaint.setColor(Color.RED);
		mPaint.setStrokeWidth(10f);
		mPaint.setTextSize(50f);
	}

	public enum ClickItemType {
		SETTING, ALERT, CLEAR, PHOTO, VIDEO, GALLERY, H312_INTERCONNECT, H312_MONITOR
	}

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (!mIsEnable) {
				if (mCallback != null) {
					mCallback.viewDisableClicked();
				}
				return true;
			}
			resetClickItemsState();
			resetTouchCoordinate();
			invalidate();
		}
		return gestureDetector.onTouchEvent(event);
	}

	private void resetClickItemsState () {
		for (VScrollItem item : items) {
			if (item.clickable) {
				item.clickState = false;
			}
		}
	}

	private void resetTouchCoordinate () {
		touchX = 0f;
		touchY = 0f;
		lastTouchX = 0f;
		lastTouchY = 0f;
	}

	@Override
	protected void onDraw (@NonNull Canvas canvas) {
		super.onDraw(canvas);
		resetDrawItemRect();

		//bg
		canvas.drawColor(mBgColor);

		//draw item
		for (int index = itemOffsetIndex; index < items.size(); index++) {
			if (items.get(index).selectState || items.get(index).clickState) {
				canvas.drawBitmap(selectBitmaps.get(index), null, drawItemRectF, mPaint);
			} else {
				canvas.drawBitmap(normalBitmaps.get(index), null, drawItemRectF, mPaint);
			}
			calculateNextDrawItemRect();
		}
	}

	//calculate next item rect
	private void calculateNextDrawItemRect () {
		drawItemRectF.top += mItemViewHeight + itemIntervalPixels;
		drawItemRectF.bottom = drawItemRectF.top + mItemViewHeight;
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		mViewScreenWidth = MeasureSpec.getSize(widthMeasureSpec);
		mViewScreenHeight = MeasureSpec.getSize(heightMeasureSpec);

		setMeasuredDimension(mViewScreenWidth, mViewScreenHeight);

		initItemParams();
	}

	/**
	 * private var mScrolledLength = 0
	 * //上下可滑动长度
	 * private var mScrollTopLength = 0
	 * private var mScrollBottomLength = 0
	 */
	private void initItemParams () {
		//间隔
		itemIntervalPixels = (int) ((mViewScreenHeight - ((itemCount + lastItemDisPlayPercent) * mItemViewHeight)) / ((itemCount + 1)));

		//计算总高度 - 虚拟的
		mScrollViewTotalLength = (mItemViewHeight * items.size()) + (itemIntervalPixels * (items.size() + 1));
		//可滑动 总计长度
		mCanScrolledLength = mScrollViewTotalLength - mViewScreenHeight;
		mScrollTopLength = 0f;
		mScrollBottomLength = mCanScrolledLength;

		//init first item rect
		resetDrawItemRect();
	}

	//reset drawItemRectF to first item rect
	private void resetDrawItemRect () {
		drawItemRectF.left = (mViewScreenWidth - mItemViewWidth) / 2f;
		drawItemRectF.right = drawItemRectF.left + mItemViewWidth;
		// drawItemRectF.top  (-mItemViewHeight ,itemIntervalPixels)
		drawItemRectF.top = (itemIntervalPixels - mScrollTopLength % (itemIntervalPixels + mItemViewHeight));
		drawItemRectF.bottom = drawItemRectF.top + mItemViewHeight;


		mScreenFirstItemRectF.left = drawItemRectF.left;
		mScreenFirstItemRectF.right = drawItemRectF.right;
		mScreenFirstItemRectF.top = drawItemRectF.top;
		mScreenFirstItemRectF.bottom = drawItemRectF.bottom;
	}


	private WeakReference<Context> mContext;

	//控件 可视范围的宽高
	private int                    mViewScreenWidth;
	private int                    mViewScreenHeight;
	private int                    mBgColor;
	//滑动控件的 总长度
	private int                    mScrollViewTotalLength;
	//可滑动 总计长度
	private int                    mCanScrolledLength;
	//每个Item的宽高
	private int                    mItemViewWidth;
	private int                    mItemViewHeight;
	//item 间隔
	private int                    itemIntervalPixels;
	private int                    itemCount;
	private float                  lastItemDisPlayPercent;
	//记录绘制item 的偏移，根据滑动距离计算
	private int                    itemOffsetIndex    = 0;
	private Paint                  mPaint;
	private RectF                  drawItemRectF;
	private RectF                  mScreenFirstItemRectF;
	//顶部已滑动长度
	private float                  mScrollTopLength;
	//底部已滑动长度
	private float                  mScrollBottomLength;
	private List<Bitmap>           normalBitmaps      = new ArrayList();
	private List<Bitmap>           selectBitmaps      = new ArrayList();
	private int                    mSelectGlobalIndex = -1;
	//    private var callback:
	private OnStateChangedCallback mCallback;
	//	private Function0              mEnableCallback;
	//设置是否可用。不可用时，仅可滑动和进入相册。可用时，所有空间都可以点击，选中。
	private boolean                mIsEnable          = false;
	private List<VScrollItem>      items              = new ArrayList<VScrollItem>();
	private GestureDetector        gestureDetector;
	private float                  touchX             = 0;
	private float                  touchY             = 0;
	private float                  lastTouchX         = 0;
	private float                  lastTouchY         = 0;

	public interface OnStateChangedCallback {
		void onItemClick (int index, ClickItemType type);

		void viewDisableClicked ();
	}

	public void setDataCallback (OnStateChangedCallback callback) {
		mCallback = callback;
	}

	public void setViewEnable (Boolean enable) {
		mIsEnable = enable;
	}

	/**
	 * 顶部已滑动长度 + 底部已滑动长度 = 整体ScrollView长度 - 屏幕高度
	 * mScrollTopLength + mScrollBottomLength = mScrollViewHeight - mViewScreenHeight
	 * lastTouchY - touchY 为负，则向上滑动  mScrollTopLength 加  , mScrollBottomLength 减
	 * lastTouchY - touchY 为正，则向下滑动  mScrollTopLength 减  ,mScrollBottomLength 加
	 * 临界： 当一个计算后小于0时，至于上下的边缘。
	 */
	private void doScroll () {
		float change = lastTouchY - touchY;
		//        Log.e(TAG, "mScrollTopLength:$mScrollTopLength , mScrollBottomLength:$mScrollBottomLength ,change:$change")
		if (change < 0 && (mScrollTopLength - change <= mCanScrolledLength) && (mScrollBottomLength + change) >= 0) {
			mScrollTopLength -= change;
			mScrollBottomLength += change;
		} else if (change > 0 && (mScrollTopLength - change >= 0) && (mScrollBottomLength + change) <= (mCanScrolledLength)) {
			mScrollTopLength -= change;
			mScrollBottomLength += change;
		} else if (change > 0) {
			mScrollTopLength = 0f;
			mScrollBottomLength = mCanScrolledLength;
		} else if (change < 0) {
			mScrollTopLength = mCanScrolledLength;
			mScrollBottomLength = 0f;
		}
		//计算item 偏移下标
		calculateItemOffsetIndex();

		invalidate();
	}

	private void calculateItemOffsetIndex () {
		itemOffsetIndex = (int) (mScrollTopLength / (itemIntervalPixels + mItemViewHeight));
	}

	private boolean inItemRange (float xxx, float yyy) {
		boolean result = false;
		float firstLeft = mScreenFirstItemRectF.left;
		float firstRight = mScreenFirstItemRectF.right;
		float firstTop = mScreenFirstItemRectF.top;
		float firstBottom = mScreenFirstItemRectF.bottom;
		for (int index = 0; index < items.size(); index++) {
			result =
					result || (xxx >= firstLeft && xxx < firstRight) && ((yyy >= (firstTop + (mItemViewHeight + itemIntervalPixels) * index)) && yyy <= (firstBottom + (mItemViewHeight + itemIntervalPixels) * index));
		}
		return result;
	}


	private int getTouchItemIndex (float xx, float yy) {
		int touchIndex = -1;
		float firstLeft = mScreenFirstItemRectF.left;
		float firstRight = mScreenFirstItemRectF.right;
		float firstTop = mScreenFirstItemRectF.top;
		float firstBottom = mScreenFirstItemRectF.bottom;

		//        Log.e(TAG, "getTouchItemIndex: x:$xx, y:$yy , l $firstLeft r: $firstRight t:$firstTop b:$firstBottom")
		for (int index = 0; index < items.size(); index++) {
			if ((xx >= firstLeft && xx <= firstRight) && ((yy >= (firstTop + (mItemViewHeight + itemIntervalPixels) * index)) && yy <= (firstBottom + (mItemViewHeight + itemIntervalPixels) * index))) {
				//                Log.e(TAG, "getTouchItemIndex: "+ index )
				touchIndex = index;
				return touchIndex;
			}
		}
		return touchIndex;
	}

	@Override
	public boolean onDown (@NonNull MotionEvent event) {
		touchX = event.getX();
		touchY = event.getY();
		invalidate();
		if (!mIsEnable) {
			//判断是否在相册
			int pressDownIndex = getTouchItemIndex(event.getX(), event.getY()) + itemOffsetIndex;
			Log.e("TAG", "onSingleTapUp: pressDownIndex" + pressDownIndex);
			//判断是否在相册
			if (pressDownIndex >= 0 && pressDownIndex < items.size()) {
				if (items.get(pressDownIndex).type == ClickItemType.GALLERY) {
					Log.e("TAG", "onSingleTapUp: pressDownIndex 111" + pressDownIndex);
					if (mCallback != null) {
						Log.e("TAG", "onSingleTapUp: pressDownIndex 222" + pressDownIndex);
						mCallback.onItemClick(pressDownIndex, ClickItemType.GALLERY);
					}
				}
			}
			return true;
		}
		if (inItemRange(event.getX(), event.getY())) {
			int ind = getTouchItemIndex(event.getX(), event.getY());
			mSelectGlobalIndex = ind + itemOffsetIndex;
			if (items.get(mSelectGlobalIndex).clickable) {
				items.get(mSelectGlobalIndex).clickState = true;
			}
			invalidate();
		}
		return true;
	}

	@Override
	public void onShowPress (@NonNull MotionEvent event) {
		if (!mIsEnable) {
			return;
		}
		if (inItemRange(event.getX(), event.getY())) {
			int ind = getTouchItemIndex(event.getX(), event.getY());
			mSelectGlobalIndex = ind + itemOffsetIndex;
			if (items.get(mSelectGlobalIndex).clickable) {
				items.get(mSelectGlobalIndex).clickState = true;
			}
			invalidate();
		}
	}

	@Override
	public boolean onSingleTapUp (@NonNull MotionEvent event) {
		if (!mIsEnable) {
			if (mCallback != null) {
				mCallback.viewDisableClicked();
			}
			return true;
		}
		//        Log.e(TAG, "onSingleTapUp: ")
		//判定是否在某个item中
		if (inItemRange(event.getX(), event.getY())) {
			int ind = getTouchItemIndex(event.getX(), event.getY());
			mSelectGlobalIndex = ind + itemOffsetIndex;
			//                Log.e(TAG, "onSingleTapUp: X ${it.x} Y: ${it.y}  <==>  index :" + (mSelectGlobalIndex))
			if (items.get(mSelectGlobalIndex).clickable) {
				//                    do callback
				if (mCallback != null)
					mCallback.onItemClick(mSelectGlobalIndex, items.get(mSelectGlobalIndex).type);
				//					mCallback?.let { it1 -> it1(mSelectGlobalIndex, items.get(mSelectGlobalIndex).type) };
			}
			if (items.get(mSelectGlobalIndex).selectable) {
				items.get(mSelectGlobalIndex).selectState = !items.get(mSelectGlobalIndex).selectState;
				//do callback
				if (mCallback != null)
					mCallback.onItemClick(mSelectGlobalIndex, items.get(mSelectGlobalIndex).type);
				//					mCallback?.let { it1 -> it1(mSelectGlobalIndex, items[mSelectGlobalIndex].type) };
			}
			invalidate();
		}
		return true;
	}

	@Override
	public boolean onScroll (@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
		lastTouchY = e2.getY();
		//do render
		doScroll();
		touchY = lastTouchY;
		invalidate();
		return true;
	}

	@Override
	public void onLongPress (@NonNull MotionEvent e) {

	}

	@Override
	public boolean onFling (@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	public static final class VScrollItem {
		/**
		 * @IdRes var normalImageId: Int = normal
		 * var selectImageId: Int = select
		 * val selectable = itemSelectable
		 * var selectState = false
		 * val clickable = !itemSelectable
		 * var clickState = false
		 * val type: ClickItemType = itemType
		 */
		@IdRes
		private       int           normalImageId;
		@IdRes
		private       int           selectImageId;
		private final boolean       selectable;
		private       boolean       selectState;
		private final boolean       clickable;
		private       boolean       clickState;
		@NotNull
		private final ClickItemType type;

		public final int getNormalImageId () {
			return this.normalImageId;
		}

		public final void setNormalImageId (int var1) {
			this.normalImageId = var1;
		}

		public final int getSelectImageId () {
			return this.selectImageId;
		}

		public final void setSelectImageId (int var1) {
			this.selectImageId = var1;
		}

		public final boolean getSelectable () {
			return this.selectable;
		}

		public final boolean getSelectState () {
			return this.selectState;
		}

		public final void setSelectState (boolean var1) {
			this.selectState = var1;
		}

		public final boolean getClickable () {
			return this.clickable;
		}

		public final boolean getClickState () {
			return this.clickState;
		}

		public final void setClickState (boolean var1) {
			this.clickState = var1;
		}

		@NotNull
		public final ClickItemType getType () {
			return this.type;
		}

		public VScrollItem (int normal, int select, @NotNull ClickItemType itemType, boolean itemSelectable) {
			this.normalImageId = normal;
			this.selectImageId = select;
			this.selectable = itemSelectable;
			this.type = itemType;
			this.clickable = !this.selectable;
		}
	}
}
