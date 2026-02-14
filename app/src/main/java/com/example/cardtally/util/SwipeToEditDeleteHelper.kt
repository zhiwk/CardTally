package com.example.cardtally.util

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.example.cardtally.R
import kotlin.math.abs

class SwipeToEditDeleteHelper(
    private val cardContent: View,
    private val layoutActions: View,
    private val onEdit: () -> Unit,
    private val onDelete: () -> Unit,
    private val onArchive: (() -> Unit)? = null
) {
    private var initialTouchX = 0f
    private var initialTranslationX = 0f
    private var lastTouchX = 0f
    private var lastTouchTime = 0L
    private var velocityX = 0f
    private var isSwiping = false
    private var isOpen = false
    
    private val touchSlop: Int
    private val maxSwipeDistance: Int
    private val minVelocity: Float
    
    private val decelerateInterpolator = DecelerateInterpolator(1.5f)
    private val overshootInterpolator = OvershootInterpolator(0.5f)

    init {
        val configuration = ViewConfiguration.get(cardContent.context)
        touchSlop = configuration.scaledTouchSlop
        val buttonCount = if (onArchive != null) 3 else 2
        maxSwipeDistance = (80 * buttonCount * cardContent.context.resources.displayMetrics.density).toInt()
        minVelocity = ViewConfiguration.get(cardContent.context).scaledMinimumFlingVelocity * 2f
        
        cardContent.setOnTouchListener { v, event ->
            handleTouchEvent(v, event)
        }
        
        layoutActions.findViewById<View>(R.id.btn_edit)?.setOnClickListener {
            onEdit()
            close(true)
        }
        
        layoutActions.findViewById<View>(R.id.btn_delete)?.setOnClickListener {
            onDelete()
            close(true)
        }
        
        layoutActions.findViewById<View>(R.id.btn_archive)?.setOnClickListener {
            onArchive?.invoke()
            close(true)
        }
    }

    private fun handleTouchEvent(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTranslationX = cardContent.translationX
                lastTouchX = event.rawX
                lastTouchTime = System.currentTimeMillis()
                velocityX = 0f
                isSwiping = false
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val currentDeltaX = event.rawX - lastTouchX
                val currentTime = System.currentTimeMillis()
                val timeDelta = currentTime - lastTouchTime
                
                if (timeDelta > 0) {
                    velocityX = (currentDeltaX / timeDelta) * 1000
                }
                
                lastTouchX = event.rawX
                lastTouchTime = currentTime
                
                if (!isSwiping && abs(deltaX) > touchSlop) {
                    isSwiping = true
                    val parent = cardContent.parent
                    if (parent is ViewGroup) {
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                }
                
                if (isSwiping) {
                    val newTranslationX = initialTranslationX + deltaX
                    
                    if (newTranslationX <= 0 && newTranslationX >= -maxSwipeDistance) {
                        cardContent.translationX = newTranslationX
                    } else if (newTranslationX > 0) {
                        cardContent.translationX = newTranslationX * 0.3f
                    } else {
                        cardContent.translationX = -maxSwipeDistance + (newTranslationX + maxSwipeDistance) * 0.3f
                    }
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isSwiping) {
                    val currentX = cardContent.translationX
                    
                    if (abs(velocityX) > minVelocity) {
                        if (velocityX < 0) {
                            open()
                        } else {
                            close(false)
                        }
                    } else {
                        if (currentX < -maxSwipeDistance / 2) {
                            open()
                        } else {
                            close(false)
                        }
                    }
                    return true
                }
                
                if (isOpen && !isSwiping) {
                    close(false)
                    return true
                }
            }
        }
        return false
    }

    private fun open() {
        animateTo(-maxSwipeDistance.toFloat(), true)
        isOpen = true
    }

    private fun close(withOvershoot: Boolean) {
        animateTo(0f, withOvershoot)
        isOpen = false
    }

    private fun animateTo(targetX: Float, withOvershoot: Boolean) {
        val currentX = cardContent.translationX
        val distance = abs(targetX - currentX)
        val duration = (distance / maxSwipeDistance * 250).toLong().coerceIn(100, 300)
        
        val animator = ValueAnimator.ofFloat(currentX, targetX).apply {
            this.duration = duration
            interpolator = if (withOvershoot && targetX == 0f) {
                overshootInterpolator
            } else {
                decelerateInterpolator
            }
            
            addUpdateListener { animation ->
                cardContent.translationX = animation.animatedValue as Float
            }
        }
        
        animator.start()
    }

    fun isOpen(): Boolean = isOpen
}
