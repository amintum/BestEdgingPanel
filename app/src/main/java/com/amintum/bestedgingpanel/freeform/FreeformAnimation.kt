package com.amintum.bestedgingpanel.freeform

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator

object FreeformAnimation {
    fun moveInScreenAnimator(start: Int, end: Int, dur: Long, moveX: Boolean, window: FreeformWindow) {
        AnimatorSet().apply {
            play(
                ValueAnimator.ofInt(start, end).apply {
                    addUpdateListener {
                        try {
                            window.windowManager.updateViewLayout(
                                window.freeformLayout,
                                window.windowParams.apply {
                                    if (moveX) x = it.animatedValue as Int
                                    else y = it.animatedValue as Int
                                }
                            )
                        } catch (_: Exception) {}
                    }
                }
            )
            duration = dur
            start()
        }
    }
}
