package cn.wcc.customScroll

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.IdRes
import java.lang.ref.WeakReference

/**
 * <p>Copyright (C),2024/4/11 17:55-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2024/4/11 17:55     </p>
 * <p>PackagePath: cn.wcc.customScroll     </p>
 * <p>Description：       </p>
 */
class VScrollView(context: Context, attributes: AttributeSet?, defStyleAttr: Int) : View(context, attributes, defStyleAttr),
    GestureDetector.OnGestureListener {

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, 0)

    class VScrollItem(
        private val normal: Int,
        private val select: Int,
        private val itemType: ClickItemType,
        private val itemSelectable: Boolean
    ) {
        @IdRes
        var normalImageId: Int = normal

        @IdRes
        var selectImageId: Int = select
        val selectable = itemSelectable
        var selectState = false
        val clickable = !itemSelectable
        var clickState = false
        val type: ClickItemType = itemType
    }


    //点击事件的类型。
    enum class ClickItemType {
        SETTING, ALERT, CLEAR, PHOTO, VIDEO, GALLERY, H312_1, H312_2
    }

    private val TAG = "VScrollView"

    private var mContext: WeakReference<Context>? = null

    //控件 可视范围的宽高
    private var mViewScreenWidth = 0
    private var mViewScreenHeight = 0
    private var mBgColor: Int = Color.WHITE

    //滑动控件的 总长度
    private var mScrollViewTotalLength = 0

    //可滑动 总计长度
    private var mCanScrolledLength = 0

    //每个Item的宽高
    private var mItemViewWidth = 0
    private var mItemViewHeight = 0

    //item 间隔
    private var itemIntervalPixels: Int = 0
    private var itemCount: Int = 6
    private var lastItemDisPlayPercent = 0.5f

    //记录绘制item 的偏移，根据滑动距离计算
    private var itemOffsetIndex = 0

    //画笔
    private val mPaint = Paint()
    private var drawItemRectF = RectF()
    private var mScreenFirstItemRectF = RectF()


    //顶部已滑动长度
    private var mScrollTopLength = 0f

    //底部已滑动长度
    private var mScrollBottomLength = 0f

    //图片资源数组
    private var normalBitmaps: MutableList<Bitmap> = mutableListOf()

    private var selectBitmaps: MutableList<Bitmap> = mutableListOf()

    //    private var callback:
    private var mSelectGlobalIndex = -1

    private var mCallback: ((Int, ClickItemType) -> Unit)? = null

    private var mEnableCallback: (() -> Unit)? = null

    private var mIsEnable: Boolean = false

    fun setDataCallback(callback: (Int, ClickItemType) -> Unit) {
        mCallback = callback
    }

    fun setEnableCallback(callback: (() -> Unit)) {
        mEnableCallback = callback
    }

    fun setViewEnable(enable: Boolean) {
        mIsEnable = enable
    }

    private var items = mutableListOf<VScrollItem>()
    private fun initItems() {
        items.add(
            VScrollItem(
                R.mipmap.preview_left_setting_unselect,
                R.mipmap.preview_left_setting_select, ClickItemType.SETTING, itemSelectable = false
            )
        )
        items.add(
            VScrollItem(
                R.mipmap.preview_left_alert_unselect,
                R.mipmap.preview_left_alert_select, ClickItemType.ALERT, itemSelectable = true
            )
        )
        items.add(
            VScrollItem(
                R.mipmap.preview_left_clear_unselect,
                R.mipmap.preview_left_clear_select, ClickItemType.CLEAR, itemSelectable = false
            )
        )
        items.add(
            VScrollItem(
                R.mipmap.preview_left_photo_unselect,
                R.mipmap.preview_left_photo_select, ClickItemType.PHOTO, itemSelectable = false
            )
        )
        items.add(
            VScrollItem(
                R.mipmap.preview_left_video_unselect,
                R.mipmap.preview_left_video_select, ClickItemType.VIDEO, itemSelectable = true
            )
        )
        items.add(
            VScrollItem(
                R.mipmap.preview_left_gallery_unselect,
                R.mipmap.preview_left_gallery_select, ClickItemType.GALLERY, itemSelectable = false
            )
        )
        items.add(
            VScrollItem(
                R.drawable.icon_time, R.drawable.icon_add,
                ClickItemType.H312_1, itemSelectable = false
            )
        )
        items.add(
            VScrollItem(
                R.drawable.icon_daka, R.drawable.icon_data_jiankong,
                ClickItemType.H312_2, itemSelectable = false
            )
        )

    }

    //点击集合,单选

    //选中集合.能多选
    private var gestureDetector: GestureDetector

    init {
        initItems()
        mContext = WeakReference<Context>(context)
        gestureDetector = GestureDetector(mContext!!.get(), this)
        val attrs = mContext?.get()?.obtainStyledAttributes(attributes, R.styleable.VScrollView, defStyleAttr, 0)
        attrs?.let {
            itemCount = it.getInt(R.styleable.VScrollView_itemCounts, 6)
            lastItemDisPlayPercent = it.getFloat(R.styleable.VScrollView_lastItemPercent, 0.5f)
            mItemViewWidth = it.getDimensionPixelSize(R.styleable.VScrollView_itemWidth, 50)
            mItemViewHeight = it.getDimensionPixelSize(R.styleable.VScrollView_itemHeight, mItemViewWidth)
            mBgColor = it.getColor(R.styleable.VScrollView_bgColor, Color.BLUE)


            normalBitmaps = mutableListOf()
            for (index in items.indices) {
                //                        Log.e(TAG, " arrays item ${imageResourceId[index]} ", )
                normalBitmaps.add(BitmapFactory.decodeResource(mContext!!.get()!!.resources, items[index].normalImageId))
                selectBitmaps.add(BitmapFactory.decodeResource(mContext!!.get()!!.resources, items[index].selectImageId))
            }

//            Log.e(
//                TAG, "itemCount = $itemCount, itemWidth = $mItemViewWidth, " +
//                        "items.size = ${items.size} "
//            )
            it.recycle()
        }
        mPaint.let {
            it.color = Color.RED
            it.strokeWidth = 10f
            it.textSize = 50f
        }
    }

    private var touchX = 0f
    private var touchY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private fun resetTouchCoordinate() {
        touchX = 0f
        touchY = 0f
        lastTouchX = 0f
        lastTouchY = 0f
    }

    /**
     *  顶部已滑动长度 + 底部已滑动长度 = 整体ScrollView长度 - 屏幕高度
     *  mScrollTopLength + mScrollBottomLength = mScrollViewHeight - mViewScreenHeight
     *  lastTouchY - touchY 为负，则向上滑动  mScrollTopLength 加  , mScrollBottomLength 减
     *  lastTouchY - touchY 为正，则向下滑动  mScrollTopLength 减  ,mScrollBottomLength 加
     *  临界： 当一个计算后小于0时，至于上下的边缘。
     */
    private fun doScroll() {
        val change = lastTouchY - touchY
//        Log.e(TAG, "mScrollTopLength:$mScrollTopLength , mScrollBottomLength:$mScrollBottomLength ,change:$change")
        if (change < 0 && (mScrollTopLength - change <= mCanScrolledLength) && (mScrollBottomLength + change) >= 0) {
            mScrollTopLength -= change
            mScrollBottomLength += change
        } else if (change > 0 && (mScrollTopLength - change >= 0) && (mScrollBottomLength + change) <= (mCanScrolledLength)) {
            mScrollTopLength -= change
            mScrollBottomLength += change
        } else if (change > 0) {
            mScrollTopLength = 0f
            mScrollBottomLength = mCanScrolledLength.toFloat()
        } else if (change < 0) {
            mScrollTopLength = mCanScrolledLength.toFloat()
            mScrollBottomLength = 0f
        }
        //计算item 偏移下标
        calculateItemOffsetIndex()
//        Log.e(TAG, "doScroll: itemOffsetIndex  $itemOffsetIndex")

        invalidate()
    }

    private fun calculateItemOffsetIndex() {
        itemOffsetIndex = (mScrollTopLength / (itemIntervalPixels + mItemViewHeight)).toInt()
    }

    private fun inItemRange(xxx: Float, yyy: Float): Boolean {
        var result = false
        val firstLeft = mScreenFirstItemRectF.left
        val firstRight = mScreenFirstItemRectF.right
        val firstTop = mScreenFirstItemRectF.top
        val firstBottom = mScreenFirstItemRectF.bottom
        for (index in 0 until items.size) {
            result = result || (xxx >= firstLeft && xxx < firstRight)
                    && ((yyy >= (firstTop + (mItemViewHeight + itemIntervalPixels) * index))
                    && yyy <= (firstBottom + (mItemViewHeight + itemIntervalPixels) * index))
        }
        return result
    }

    private fun getTouchItemIndex(xx: Float, yy: Float): Int {
        var touchIndex = -1
        val firstLeft = mScreenFirstItemRectF.left
        val firstRight = mScreenFirstItemRectF.right
        val firstTop = mScreenFirstItemRectF.top
        val firstBottom = mScreenFirstItemRectF.bottom

//        Log.e(TAG, "getTouchItemIndex: x:$xx, y:$yy , l $firstLeft r: $firstRight t:$firstTop b:$firstBottom")
        for (index in 0 until items.size) {
            if ((xx in firstLeft..firstRight)
                && ((yy >= (firstTop + (mItemViewHeight + itemIntervalPixels) * index))
                        && yy <= (firstBottom + (mItemViewHeight + itemIntervalPixels) * index))
            ) {
//                Log.e(TAG, "getTouchItemIndex: "+ index )
                touchIndex = index
                return touchIndex
            }
        }
        return touchIndex
    }

    private fun resetClickItemsState() {
        for (item in items) {
            if (item.clickable) {
                item.clickState = false
            }
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            if (!mIsEnable) {
                mEnableCallback?.let { it() }
                return true
            }

//           runBlocking {
//               GlobalScope.launch(Dispatchers.Default){
////               launch {
//                   delay(200)
//            Log.e(TAG, "onTouchEvent:  ACTION_UP================= ")
            resetClickItemsState()
            resetTouchCoordinate()
            invalidate()
//               }
//           }
        }
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDown(event: MotionEvent): Boolean {
        touchX = event.x
        touchY = event.y
        invalidate()
        if (!mIsEnable) {
//            mEnableCallback?.let { it() }
            return true
        }

        if (inItemRange(event.x, event.y)) {
            val ind = getTouchItemIndex(event.x, event.y)
            mSelectGlobalIndex = ind + itemOffsetIndex
            if (items[mSelectGlobalIndex].clickable) {
                items[mSelectGlobalIndex].clickState = true
            }
            invalidate()
        }
        return true
    }


    override fun onShowPress(event: MotionEvent) {
        if (!mIsEnable) {
//            mEnableCallback?.let { it() }
            return
        }

        if (inItemRange(event.x, event.y)) {
            val ind = getTouchItemIndex(event.x, event.y)
            mSelectGlobalIndex = ind + itemOffsetIndex
//            Log.e(TAG, "onShowPress: X ${event.x} Y: ${event.y}  <==>  index :" + (mSelectGlobalIndex))
            if (items[mSelectGlobalIndex].clickable) {
                items[mSelectGlobalIndex].clickState = true
            }
            invalidate()
        }

    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        if (!mIsEnable) {
            mEnableCallback?.let { it() }
            return true
        }
//        Log.e(TAG, "onSingleTapUp: ")
        event.let {
            //判定是否在某个item中
            if (inItemRange(it.x, it.y)) {
                val ind = getTouchItemIndex(it.x, it.y)
                mSelectGlobalIndex = ind + itemOffsetIndex
//                Log.e(TAG, "onSingleTapUp: X ${it.x} Y: ${it.y}  <==>  index :" + (mSelectGlobalIndex))
                if (items[mSelectGlobalIndex].clickable) {
//                    do callback
                    mCallback?.let { it1 -> it1(mSelectGlobalIndex, items[mSelectGlobalIndex].type) }
                }
                if (items[mSelectGlobalIndex].selectable) {
                    items[mSelectGlobalIndex].selectState = !items[mSelectGlobalIndex].selectState
                    //do callback
                    mCallback?.let { it1 -> it1(mSelectGlobalIndex, items[mSelectGlobalIndex].type) }
                }
                invalidate()
            }
        }
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
//       if (inItemRange(e1?.x, e1?.y)) {
//
//       }
//        Log.e(TAG, "onScroll: X${e1?.x} Y${e1?.y}   X${e2.x} Y${e2.y} distanceX:$distanceX ,distanceY:$distanceY")
        lastTouchY = e2.y
        //do render
        doScroll()
        touchY = lastTouchY
        invalidate()
        return true
    }

    override fun onLongPress(e: MotionEvent) {
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        mViewScreenWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        mViewScreenHeight = View.MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(mViewScreenWidth, mViewScreenHeight)

        initItemParams()

    }

    /**
     * private var mScrolledLength = 0
    //上下可滑动长度
    private var mScrollTopLength = 0
    private var mScrollBottomLength = 0
     */
    private fun initItemParams() {
        //间隔
        itemIntervalPixels = (((mViewScreenHeight - ((itemCount + lastItemDisPlayPercent) * mItemViewHeight)) / ((itemCount + 1))).toInt())

        //计算总高度 - 虚拟的
        mScrollViewTotalLength = (mItemViewHeight * items.size) + (itemIntervalPixels * (items.size + 1))
        //可滑动 总计长度
        mCanScrolledLength = mScrollViewTotalLength - mViewScreenHeight
        mScrollTopLength = 0f
        mScrollBottomLength = mCanScrolledLength.toFloat()

        //init first item rect
        resetDrawItemRect()

        /* Log.e(
             TAG, "itemIntervalPixels: $itemIntervalPixels ,mViewScreenHeight: $mViewScreenHeight , mViewScreenWidth:$mViewScreenWidth " +
                     " mScrollViewHeight:$mScrollViewTotalLength mScrollBottomLength: $mScrollBottomLength"
         )*/
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

//        Log.e(TAG, "onLayout: ")
    }

    //calculate next item rect
    private fun calculateNextDrawItemRect() {
        drawItemRectF.top = drawItemRectF.top + mItemViewHeight + itemIntervalPixels
        drawItemRectF.bottom = drawItemRectF.top + mItemViewHeight
    }

    //reset drawItemRectF to first item rect
    private fun resetDrawItemRect() {
        drawItemRectF.left = (mViewScreenWidth - mItemViewWidth) / 2f
        drawItemRectF.right = drawItemRectF.left + mItemViewWidth
        // drawItemRectF.top  (-mItemViewHeight ,itemIntervalPixels)
        drawItemRectF.top = (itemIntervalPixels - mScrollTopLength % (itemIntervalPixels + mItemViewHeight))
        drawItemRectF.bottom = drawItemRectF.top + mItemViewHeight


        mScreenFirstItemRectF.left = drawItemRectF.left
        mScreenFirstItemRectF.right = drawItemRectF.right
        mScreenFirstItemRectF.top = drawItemRectF.top
        mScreenFirstItemRectF.bottom = drawItemRectF.bottom
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
//        Log.e(TAG, "onDraw: ")
        resetDrawItemRect()

        canvas.let {
            //bg
            it.drawColor(mBgColor)

            //draw item
            for (index in itemOffsetIndex until items.size) {
                if (items[index].selectState || items[index].clickState) {
                    canvas.drawBitmap(selectBitmaps[index], null, drawItemRectF, mPaint)
                } else {
                    canvas.drawBitmap(normalBitmaps[index], null, drawItemRectF, mPaint)
                }
                calculateNextDrawItemRect()
            }
        }
    }
}