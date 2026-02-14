package com.example.cardtally.util

import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.example.cardtally.R
import kotlin.math.abs

class SwipeToEditDeleteHelper(
    private val cardContent: View,
    private val layoutActions: View,
    private val onEdit: () -> Unit,
    private val onDelete: () -> Unit
) {
    private var initialX = 0f
    private var initialTouchX = 0f
    private var isSwiping = false
    private var isOpen = false
    private val touchSlop: Int
    private val maxSwipeDistance: Int

    init {
        val configuration = ViewConfiguration.get(cardContent.context)
        touchSlop = configuration.scaledTouchSlop
        maxSwipeDistance = (160 * cardContent.context.resources.displayMetrics.density).toInt()
        
        cardContent.setOnTouchListener { v, event ->
            handleTouchEvent(v, event)
        }
        
        layoutActions.findViewById<View>(R.id.btn_edit)?.setOnClickListener {
            onEdit()
            close()
        }
        
        layoutActions.findViewById<View>(R.id.btn_delete)?.setOnClickListener {
            onDelete()
            close()
        }
    }

    private fun handleTouchEvent(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = cardContent.translationX
                initialTouchX = event.rawX
                isSwiping = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                
                if (!isSwiping && abs(deltaX) > touchSlop) {
                    isSwiping = true
                }
                
                if (isSwiping) {
                    val newX = initialX + deltaX
                    if (newX <= 0 && newX >= -maxSwipeDistance) {
                        cardContent.translationX = newX
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isSwiping) {
                    val currentX = cardContent.translationX
                    if (currentX < -maxSwipeDistance / 2) {
                        open()
                    } else {
                        close()
                    }
                    return true
                }
            }
        }
        return false
    }

    private fun open() {
        animateTo(-maxSwipeDistance.toFloat())
        isOpen = true
    }

    private fun close() {
        animateTo(0f)
        isOpen = false
    }

    private fun animateTo(targetX: Float) {
        val animator = ObjectAnimator.ofFloat(cardContent, "translationX", cardContent.translationX, targetX)
        animator.duration = 200
        animator.start()
    }

    fun isOpen(): Boolean = isOpen
}
