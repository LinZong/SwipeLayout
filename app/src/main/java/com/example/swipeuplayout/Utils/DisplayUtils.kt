package com.example.swipeuplayout.Utils

import android.content.Context

data class ScreenResolution(var Width: Int,var Height:Int)


fun Context.GetScreenResolution() : ScreenResolution {
    val height = this.resources.displayMetrics.heightPixels
    val width = this.resources.displayMetrics.widthPixels
    return ScreenResolution(width,height)
}

fun Context.Dp2Px(dp : Int) : Float = (dp.toFloat() * this.resources.displayMetrics.density) + 0.5f

fun Context.Px2Dp(px : Int) : Float = (px.toFloat() / this.resources.displayMetrics.density) + 0.5f
