package com.acgnu.tiktik;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;

/** A rotary dial with an unbounded visual angle and decaying angular momentum. */
public class TempoDialView extends View {
    public interface Listener { void onTempoChanged(int bpm); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final float density;
    private final int touchSlop;
    private Listener listener;
    private int bpm = Tempo.DEFAULT;
    private boolean playing;
    private float rotation, remainder, lastAngle, velocity, downX, downY;
    private long lastTime, animationTime;
    private boolean centerTouch, moved, dragging;
    private final Runnable fling = new Runnable() {
        @Override public void run() {
            long now = SystemClock.uptimeMillis();
            float seconds = Math.min(0.05f, (now - animationTime) / 1000f);
            animationTime = now;
            rotate(velocity * seconds);
            velocity *= (float) Math.exp(-4.2f * seconds);
            if (Math.abs(velocity) > 5f) postOnAnimation(this);
        }
    };

    public TempoDialView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setFocusable(true);
        setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        updateDescription();
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public void setBpm(int bpm) {
        stopRotation();
        this.bpm = Tempo.clamp(bpm);
        remainder = 0;
        updateDescription();
        invalidate();
    }
    public void setPlaying(boolean playing) {
        this.playing = playing;
        updateDescription();
        invalidate();
    }
    public void stopRotation() { removeCallbacks(fling); velocity = 0; }
    private void updateDescription() {
        setContentDescription(bpm + " BPM，" + (playing ? "正在播放，点击暂停" : "已暂停，点击播放")
                + "，顺时针增加，逆时针减少");
    }
    private float radius() { return Math.max(1, Math.min(getWidth(), getHeight()) * 0.36f); }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // A square viewport gives the circular dial equal space on all four sides.
        int width = resolveSize(Math.round(320 * density), widthMeasureSpec);
        int height = resolveSize(width, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = radius();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6 * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        bounds.set(cx - r, cy - r, cx + r, cy + r);
        canvas.drawArc(bounds, rotation - 72, 324, false, paint);
        double dotAngle = Math.toRadians(rotation - 90);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx + r * (float) Math.cos(dotAngle),
                cy + r * (float) Math.sin(dotAngle), 7 * density, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-light", 0));
        paint.setTextSize(Math.min(82 * density, r * 0.72f));
        canvas.drawText(String.valueOf(bpm), cx, cy + paint.getTextSize() * 0.22f, paint);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        paint.setTextSize(13 * density);
        canvas.drawText("BPM", cx, cy + r * 0.43f, paint);
        paint.setTextSize(12 * density);
        canvas.drawText(playing ? "点击暂停" : "点击播放", cx, cy + r * 0.65f, paint);
    }

    private float angle(float x, float y) {
        return (float) Math.toDegrees(Math.atan2(y - getHeight() / 2f, x - getWidth() / 2f));
    }
    private void rotate(float delta) {
        rotation = (rotation + delta) % 360f;
        remainder += delta;
        int steps = (int) (remainder / 6f);
        if (steps != 0) {
            remainder -= steps * 6f;
            int next = Tempo.clamp(bpm + steps);
            if (next != bpm) {
                bpm = next;
                updateDescription();
                if (listener != null) listener.onTempoChanged(bpm);
            }
        }
        invalidate();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stopRotation();
                downX = x; downY = y;
                moved = false;
                dragging = true;
                centerTouch = Math.hypot(x - getWidth() / 2f, y - getHeight() / 2f) < radius() * 0.68f;
                lastAngle = angle(x, y);
                lastTime = event.getEventTime();
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) return true;
                moved |= Math.hypot(x - downX, y - downY) > touchSlop;
                if (!centerTouch) {
                    float nextAngle = angle(x, y);
                    // Ignore the singularity at the dial's center.
                    if (Math.hypot(x - getWidth() / 2f, y - getHeight() / 2f) > radius() * 0.4f) {
                        float delta = Tempo.angleDelta(lastAngle, nextAngle);
                        long dt = Math.max(1, event.getEventTime() - lastTime);
                        velocity = velocity * 0.35f + Math.max(-1080f,
                                Math.min(1080f, delta * 1000 / dt)) * 0.65f;
                        rotate(delta);
                    } else {
                        velocity = 0;
                    }
                    lastAngle = nextAngle;
                    lastTime = event.getEventTime();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging) return true;
                dragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                if (centerTouch && !moved) performClick();
                else if (!centerTouch && moved && event.getEventTime() - lastTime < 100) {
                    animationTime = SystemClock.uptimeMillis();
                    postOnAnimation(fling);
                }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                stopRotation();
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default: return true;
        }
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    @Override protected void onDetachedFromWindow() { stopRotation(); super.onDetachedFromWindow(); }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, Tempo.MIN, Tempo.MAX, bpm));
    }
    @Override public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            stopRotation();
            rotate(action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 6 : -6);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }
}
