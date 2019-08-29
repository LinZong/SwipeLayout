package com.example.swipeuplayout.View

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.RelativeLayout
import com.example.swipeuplayout.Model.AnchorType
import com.example.swipeuplayout.R
import com.example.swipeuplayout.Utils.Dp2Px
import com.example.swipeuplayout.Utils.GetScreenResolution
import com.example.swipeuplayout.Utils.ScreenResolution
import kotlin.math.abs
import kotlin.math.max

class SwipeUpLayout : RelativeLayout {

    private val TAG = "SwipeUpLayout"
    private var RootViewRef: Int = -1
    private lateinit var CurrentLayoutRootView: ViewGroup

    // anchor value
    var AnchorBottom: Float = 100.0f
    var AnchorMiddle: Float = 0.5f
    var DimBackground: Boolean = true
    private var CurrentScreenResolution: ScreenResolution = ScreenResolution(1080, 1920)

    // touch event tracking variables

    private var MoveY: Int = 0
    private var DownY: Int = 0
    private var BeginMoveMargin: Int = 0
    private var DetectArea: Int = 0
    private var BaseMargin: Int = 0
    private var MoveDirection = true

    private lateinit var ScrollToAnchorAnimator: ValueAnimator
    private var DimBackgroundModal: View? = null


    // computed value

    private val CurrentMarginTop: Int
        get() = max((layoutParams as ViewGroup.MarginLayoutParams).topMargin, 0)

    private val MiddleAnchorPixel: Int
        get() = (BaseMargin * AnchorMiddle).toInt()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        ObtainTypedValue(context, attrs)
        Init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        ObtainTypedValue(context, attrs)
        Init()
    }


    override fun onFinishInflate() {
        super.onFinishInflate()
        ApplyInitMargin()
    }

    private fun Init() {
        CurrentScreenResolution = context!!.GetScreenResolution()
        BaseMargin = (CurrentScreenResolution.Height - AnchorBottom).toInt()

        ScrollToAnchorAnimator = ValueAnimator.ofInt(0,300) as ValueAnimator
        ScrollToAnchorAnimator.interpolator = AccelerateInterpolator()
    }

    private fun ApplyInitMargin() {
        post {
            SetMarginTop(BaseMargin)
        }
    }

    private fun ObtainTypedValue(context: Context?, attrs: AttributeSet?) {
        val tr = context!!.obtainStyledAttributes(attrs, R.styleable.SwipeUpLayout)

        DimBackground = tr.getBoolean(R.styleable.SwipeUpLayout_dimBackground, true)
        AnchorBottom = tr.getDimension(R.styleable.SwipeUpLayout_anchorBottom, 100f)
        AnchorMiddle = tr.getFloat(R.styleable.SwipeUpLayout_anchorMiddle, 0.5f)
        DetectArea = tr.getDimension(R.styleable.SwipeUpLayout_detectArea, context.Dp2Px(100)).toInt()
        RootViewRef = tr.getResourceId(R.styleable.SwipeUpLayout_rootView, -1)

        tr.recycle()

        // log obtained values
        Log.d(TAG, "Values are: $DimBackground, $AnchorBottom, $AnchorMiddle, $DetectArea")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        CurrentLayoutRootView = rootView.findViewById(RootViewRef)
        if (DimBackground) {
            DimBackgroundModal = CreateModalView()
            val CurrentViewIndex = CurrentLayoutRootView.indexOfChild(this)
            CurrentLayoutRootView.addView(DimBackgroundModal, max(0, CurrentViewIndex))
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (IsTouchSwiper(event!!.rawY.toInt()) || MoveY != 0) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    DimBackgroundModal?.visibility = View.VISIBLE
                    BeginMoveMargin = CurrentMarginTop
                    DownY = event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val NewDownY = event.rawY.toInt()
                    // Update scroll direction
                    MoveDirection = NewDownY - DownY > 0
                    // Update  axis Y coord
                    MoveY += NewDownY - DownY
                    DownY = NewDownY

                    val MovedMarginTop = BeginMoveMargin + MoveY
                    SetMarginTop(Limit(MovedMarginTop, 0..BaseMargin))

                    // Dim parent background if needed
                }
                MotionEvent.ACTION_UP -> {
                    // 计算Y轴的velocity, 准备靠近中央锚点，或者是两边
                    val AnchorTo = AnchorType.DetectAnchorType(
                        CurrentScreenResolution.Height,
                        MiddleAnchorPixel,
                        MoveDirection,
                        CurrentMarginTop
                    )
                    // 计算当前Y轴速度
                    val FlingScrollDuration: Int

                    when (AnchorTo) {
                        AnchorType.ANCHOR_TO_SCREEN_BOTTOM -> {
                            val Remain = BaseMargin - CurrentMarginTop
                            val Total = BaseMargin - MiddleAnchorPixel

                            FlingScrollDuration = CalculateDuration(Remain, Total).toInt()
                            MoveToAnchor(FlingScrollDuration.toLong(), CurrentMarginTop..BaseMargin)
                        }

                        AnchorType.ANCHOR_TO_SCREEN_TOP -> {
                            val Remain = CurrentMarginTop
                            val Total = MiddleAnchorPixel
                            FlingScrollDuration = CalculateDuration(Remain, Total).toInt()
                            MoveToAnchor(FlingScrollDuration.toLong(), CurrentMarginTop..0)
                        }
                        AnchorType.ANCHOR_TO_MIDDLE_FROM_UPPER -> {
                            val Remain = MiddleAnchorPixel - CurrentMarginTop
                            val Total = MiddleAnchorPixel
                            FlingScrollDuration = CalculateDuration(Remain, Total).toInt()
                            MoveToAnchor(FlingScrollDuration.toLong(), CurrentMarginTop..MiddleAnchorPixel)
                        }
                        AnchorType.ANCHOR_TO_MIDDLE_FROM_UNDER -> {
                            val Remain = CurrentMarginTop - MiddleAnchorPixel
                            val Total = BaseMargin - MiddleAnchorPixel
                            FlingScrollDuration = CalculateDuration(Remain, Total).toInt()
                            MoveToAnchor(FlingScrollDuration.toLong(), CurrentMarginTop..MiddleAnchorPixel)
                        }
                    }

                    // Reset movement tracker.
                    MoveY = 0
                    DownY = 0
                }
            }
        }
        return true
    }


    private fun IsTouchSwiper(touchY: Int): Boolean =
        CurrentMarginTop < touchY && touchY <= CurrentMarginTop + DetectArea

    private fun SetMarginTop(marginTop: Int) {

        if (DimBackground) {
            val DimPercentage = (1.0f - marginTop.toFloat() / BaseMargin) * 0.8f
            DimBackgroundModal!!.background =
                ColorDrawable(Color.argb((Limit(DimPercentage, 0f..1f) * 255).toInt(), 0, 0, 0))
        }

        if (layoutParams is ViewGroup.MarginLayoutParams) {
            val lp = layoutParams as ViewGroup.MarginLayoutParams
            lp.setMargins(0, marginTop, 0, 0)
            layoutParams = lp
        } else throw IllegalArgumentException("This layout isn't currently under a view group, it is under a $parent")
    }

    private fun <T : Comparable<T>> Limit(number: T, range: ClosedRange<T>): T {
        return when {
            number in range -> number
            number < range.start -> range.start
            else -> range.endInclusive
        }
    }

    private fun CalculateDuration(RemainDistance: Int, TotalDistance: Int) =
        200 * (RemainDistance * 1.0f / TotalDistance)

    private fun MoveToAnchor(duration: Long, range: ClosedRange<Int>) {

        ScrollToAnchorAnimator.end()
        ScrollToAnchorAnimator.Ranges(range)
        ScrollToAnchorAnimator.duration = duration
        ScrollToAnchorAnimator.removeAllUpdateListeners()
        ScrollToAnchorAnimator.addUpdateListener { va ->
            val CurrentMarginTop = va.animatedValue as Int
            SetMarginTop(CurrentMarginTop)
        }
        ScrollToAnchorAnimator.addListener(ModalUpdateListener())
        ScrollToAnchorAnimator.start()
    }

    private fun CreateModalView(): View {
        val modal = View(context)
        modal.background = ColorDrawable(Color.argb(0, 0, 255, 0))
        modal.visibility = GONE

        val lp = ViewGroup.MarginLayoutParams(
            ViewGroup.MarginLayoutParams.MATCH_PARENT,
            ViewGroup.MarginLayoutParams.MATCH_PARENT
        )
        modal.layoutParams = lp
        return modal
    }

    fun ValueAnimator.Ranges(range: ClosedRange<Int>) {
        val begin = max(0, range.start)
        val end = max(0, range.endInclusive)

        this.setIntValues(begin, end)
    }

    inner class ModalUpdateListener : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {
        }

        override fun onAnimationCancel(animation: Animator?) {
        }

        override fun onAnimationEnd(animation: Animator?) {
            if (abs(CurrentMarginTop - BaseMargin) < 5) DimBackgroundModal?.visibility = GONE
        }

        override fun onAnimationStart(animation: Animator?) {
            DimBackgroundModal?.visibility = View.VISIBLE
        }

    }

}

