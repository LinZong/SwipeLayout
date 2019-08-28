package com.example.swipeuplayout.Model

import android.util.Log
import android.view.animation.Interpolator


enum class AnchorType {
    ANCHOR_TO_SCREEN_TOP,
    ANCHOR_TO_MIDDLE_FROM_UPPER,
    ANCHOR_TO_MIDDLE_FROM_UNDER,
    ANCHOR_TO_SCREEN_BOTTOM;

    // direction: true -> swipe down, false -> swipe up

     companion object {
         fun DetectAnchorType(ScreenBottomPixel:Int, MiddleAnchorPixel:Int, MoveDirection:Boolean, CurrentPositionY:Int) : AnchorType {
             Log.d("AnchorDetector","$ScreenBottomPixel, $MiddleAnchorPixel, $MoveDirection, $CurrentPositionY")
             when (CurrentPositionY) {
                 in 0..MiddleAnchorPixel -> {
                     return if (MoveDirection) ANCHOR_TO_MIDDLE_FROM_UPPER
                            else ANCHOR_TO_SCREEN_TOP
                 }
                 in MiddleAnchorPixel..ScreenBottomPixel -> {
                     return if (MoveDirection) ANCHOR_TO_SCREEN_BOTTOM
                            else ANCHOR_TO_MIDDLE_FROM_UNDER
                 }
             }
            return ANCHOR_TO_SCREEN_BOTTOM
         }
     }
}

class QuinticInterpolator : Interpolator {

    override fun getInterpolation(t: Float): Float {
        var x = t
        x -= 1.0f

        for (i in 0 until 4) {
            x *= x
        }
        return x + 1.0f
    }
}