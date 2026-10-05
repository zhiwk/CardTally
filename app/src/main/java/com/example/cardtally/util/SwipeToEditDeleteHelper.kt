package com.example.cardtally.util

import android.animation.ValueAnimator
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.example.cardtally.R
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.ceil

class SwipeToEditDeleteHelper(
    private val cardContent: View,
    private val layoutActions: View,
    private val onEdit: () -> Unit,
    private val onDelete: () -> Unit,
    private val onArchive: (() -> Unit)? = null,
    private val onFork: (() -> Unit)? = null,
    private val onClick: (() -> Unit)? = null,
    private val clipCoveredActions: Boolean = false
) {
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialTranslationX = 0f
    private var lastTouchX = 0f
    private var lastTouchTime = 0L
    private var velocityX = 0f
    private var isSwiping = false
    private var isOpen = false
    private var isTracking = false
    private var isLongPressTriggered = false
    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var animator: ValueAnimator? = null
    private var disposed = false
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        updateActionReveal()
    }
    
    private val touchSlop: Int
    private var maxSwipeDistance: Int
    private val minVelocity: Float
    
    private val decelerateInterpolator = DecelerateInterpolator(1.5f)
    private val overshootInterpolator = OvershootInterpolator(0.5f)

    init {
        val configuration = ViewConfiguration.get(cardContent.context)
        touchSlop = configuration.scaledTouchSlop
        val buttonCount = listOfNotNull(
            layoutActions.findViewById<View>(R.id.btn_edit),
            layoutActions.findViewById<View>(R.id.btn_delete),
            layoutActions.findViewById<View>(R.id.btn_archive)
            ,layoutActions.findViewById<View>(R.id.btn_fork)
        ).count { it.visibility != View.GONE }.coerceAtLeast(1)
        val density = cardContent.context.resources.displayMetrics.density
        val fallbackButtonWidth = 80 * density
        maxSwipeDistance = (fallbackButtonWidth * buttonCount).toInt()
        minVelocity = ViewConfiguration.get(cardContent.context).scaledMinimumFlingVelocity * 2f

        setTranslation(0f)
        if (clipCoveredActions) {
            cardContent.addOnLayoutChangeListener(layoutListener)
            layoutActions.addOnLayoutChangeListener(layoutListener)
        }
        layoutActions.post {
            if (disposed) return@post
            if (layoutActions.width > 0) {
                maxSwipeDistance = layoutActions.width
            }
            updateActionReveal()
        }
        
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

        layoutActions.findViewById<View>(R.id.btn_fork)?.setOnClickListener {
            onFork?.invoke()
            close(true)
        }
    }

    private fun handleTouchEvent(v: View, event: MotionEvent): Boolean {
        if (disposed) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animator?.cancel()
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                initialTranslationX = cardContent.translationX
                lastTouchX = event.rawX
                lastTouchTime = System.currentTimeMillis()
                velocityX = 0f
                isSwiping = false
                isTracking = true
                isLongPressTriggered = false
                longPressRunnable = Runnable {
                    if (isTracking && !isSwiping) {
                        isLongPressTriggered = true
                    }
                }.also { handler.postDelayed(it, ViewConfiguration.getLongPressTimeout().toLong()) }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (!isTracking) return false
                
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY
                val currentDeltaX = event.rawX - lastTouchX
                val currentTime = System.currentTimeMillis()
                val timeDelta = currentTime - lastTouchTime
                
                if (timeDelta > 0) {
                    velocityX = (currentDeltaX / timeDelta) * 1000
                }
                
                lastTouchX = event.rawX
                lastTouchTime = currentTime
                
                if (!isSwiping && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                    cancelLongPress()
                    val isHorizontalSwipe = abs(deltaX) > abs(deltaY)
                    val isSwipeLeft = deltaX < 0
                    val isSwipeRight = deltaX > 0
                    
                    if (isHorizontalSwipe && isSwipeLeft) {
                        isSwiping = true
                        val parent = cardContent.parent
                        if (parent is ViewGroup) {
                            parent.requestDisallowInterceptTouchEvent(true)
                        }
                    } else if (isHorizontalSwipe && isSwipeRight &&
                        (initialTranslationX < 0f || cardContent.translationX < 0f)
                    ) {
                        // A right swipe is meaningful only when this row is open.
                        // It closes the revealed actions instead of being handed
                        // back to the parent list as a no-op gesture.
                        isSwiping = true
                        val parent = cardContent.parent
                        if (parent is ViewGroup) {
                            parent.requestDisallowInterceptTouchEvent(true)
                        }
                    } else if (isHorizontalSwipe && isSwipeRight) {
                        isTracking = false
                        return false
                    } else if (!isHorizontalSwipe) {
                        isTracking = false
                        return false
                    }
                }
                
                if (isSwiping) {
                    val newTranslationX = initialTranslationX + deltaX
                    
                    if (newTranslationX <= 0 && newTranslationX >= -maxSwipeDistance) {
                        setTranslation(newTranslationX)
                    } else if (newTranslationX > 0) {
                        setTranslation(newTranslationX * 0.3f)
                    } else {
                        setTranslation(-maxSwipeDistance + (newTranslationX + maxSwipeDistance) * 0.3f)
                    }
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTracking = false
                cancelLongPress()
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

                if (isLongPressTriggered) {
                    isLongPressTriggered = false
                    return true
                }
                
                val deltaX = abs(event.rawX - initialTouchX)
                val deltaY = abs(event.rawY - initialTouchY)
                if (deltaX < touchSlop && deltaY < touchSlop) {
                    onClick?.invoke()
                    return true
                }
            }
        }
        return true
    }

    private fun cancelLongPress() {
        longPressRunnable?.let(handler::removeCallbacks)
        longPressRunnable = null
    }

    private fun open() {
        activeHelperRef?.get()?.takeIf { it !== this }?.close(false)
        activeHelperRef = WeakReference(this)
        isOpen = true
        animateTo(-maxSwipeDistance.toFloat(), true)
    }

    private fun close(withOvershoot: Boolean) {
        if (disposed) return
        animateTo(0f, withOvershoot)
        isOpen = false
        if (activeHelperRef?.get() === this) activeHelperRef = null
    }

    private fun animateTo(targetX: Float, withOvershoot: Boolean) {
        animator?.cancel()
        val currentX = cardContent.translationX
        val distance = abs(targetX - currentX)
        val duration = (distance / maxSwipeDistance * 250).toLong().coerceIn(100, 300)
        
        animator = ValueAnimator.ofFloat(currentX, targetX).apply {
            this.duration = duration
            interpolator = if (withOvershoot && targetX == 0f) {
                overshootInterpolator
            } else {
                decelerateInterpolator
            }
            
            addUpdateListener { animation ->
                setTranslation(animation.animatedValue as Float)
            }
        }
        
        animator?.start()
    }

    private fun setTranslation(value: Float) {
        cardContent.translationX = value
        updateActionReveal()
    }

    /** Draw only the part beside the card, even when the card fill is translucent. */
    private fun updateActionReveal() {
        if (!clipCoveredActions) return
        val width = layoutActions.width
        val left = ceil((cardContent.x + cardContent.width - layoutActions.x).toDouble())
            .toInt().coerceIn(0, width)
        val revealed = cardContent.translationX < 0f && width > 0 && left < width
        layoutActions.clipBounds = Rect(left, 0, width, layoutActions.height)
        layoutActions.visibility = if (revealed) View.VISIBLE else View.INVISIBLE
    }

    /** Cancel an old row's animation before rebinding or recycling its views. */
    fun dispose() {
        disposed = true
        isTracking = false
        isOpen = false
        cancelLongPress()
        animator?.cancel()
        animator = null
        if (activeHelperRef?.get() === this) activeHelperRef = null
        cardContent.removeOnLayoutChangeListener(layoutListener)
        layoutActions.removeOnLayoutChangeListener(layoutListener)
        cardContent.setOnTouchListener(null)
        setTranslation(0f)
    }

    fun isOpen(): Boolean = isOpen

    companion object {
        private var activeHelperRef: WeakReference<SwipeToEditDeleteHelper>? = null
    }
}
