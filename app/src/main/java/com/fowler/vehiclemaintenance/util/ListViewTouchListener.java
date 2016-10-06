package com.fowler.vehiclemaintenance.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ListView;

public class ListViewTouchListener implements View.OnTouchListener {
    private static final String TAG = ListViewTouchListener.class.getSimpleName();
    private static final int VERTICAL_MIN_DISTANCE = 100;

    private float downX;
    private float downY;

    private ListView listView;
    private View child;
    private Integer childIdx;

    private ClickListener clickListener;
    private DismissListener dismissListener;

    public ListViewTouchListener(ListView listView) {
        this.listView = listView;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setDismissListener(DismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = event.getRawX();
                downY = event.getRawY();
                Object[] childAndIdx = getChild(downX, downY);
                if(childAndIdx != null) {
                    child = (View) childAndIdx[0];
                    childIdx = (Integer) childAndIdx[1];
                }
                if(child != null) {
                    Log.d(TAG, String.format("ACTION_DOWN: (%s,%s)", downX, downY));
                    return true;
                } else {
                    return false;
                }
            }
            case MotionEvent.ACTION_MOVE: {

                if(child == null)
                    return false;

                float deltaX = downX - event.getRawX();
                float deltaY = downY - event.getRawY();

                if(Math.abs(deltaY) < VERTICAL_MIN_DISTANCE) {
                    child.setTranslationX(-deltaX);
                    Log.d(TAG, "ACTION_MOVE: translationX = " + -deltaX);
                } else {
                    child.animate().translationX(0).setDuration(100);
                    Log.d(TAG, "ACTION_MOVE: Vertical threshold exceeded, snap back");
                }

                return true;
            }
            case MotionEvent.ACTION_UP: {

                if(child == null)
                    return false;

                float deltaX = downX - event.getRawX();
                int width = listView.getWidth();
                boolean dismiss = Math.abs(deltaX) > width / 4;
                if(dismiss) {
                    boolean dismissToLeft = deltaX > 0;
                    Log.d(TAG, "ACTION_UP: Dismissing to " + (dismissToLeft ? "left" : "right"));
                    ViewPropertyAnimator animator = child.animate();
                    animator.xBy(dismissToLeft ? -width : width);
                    animator.setDuration(500);
                    animator.setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if(dismissListener != null)
                                dismissListener.onDismiss(child, childIdx);
                            child.setTranslationX(0);
                        }
                    });
                } else {
                    child.animate().translationX(0).setDuration(100);
                    Log.d(TAG, String.format("ACTION_UP: Horizontal threshold not exceeded (|deltaX|=%s <= width/4=%s), snap back", Math.abs(deltaX), width / 4));
                    // If no movement at all, this is a tap.  Return false to indicate the event was not handled so
                    // that way the click listener will get invoked.
                    if(deltaX == 0 && clickListener != null) { // TODO: might need a threshold rather than testing for == 0?
                        clickListener.onClick(child, childIdx);
                    }
                }

                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                Log.d(TAG, "ACTION_CANCEL");
                return true;
            }
            default: {
                Log.d(TAG, "Unknown action: " + event.getAction());
                return true;
            }
        }
    }

    private Object[] getChild(float rawX, float rawY) {

        int[] listViewCoords = new int[2];
        listView.getLocationOnScreen(listViewCoords);
        int x = (int) rawX - listViewCoords[0];
        int y = (int) rawY - listViewCoords[1];

        Rect rect = new Rect();
        int childCount = listView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = listView.getChildAt(i);
            child.getHitRect(rect);
            if (rect.contains(x, y)) {
                return new Object[]{ child, i };
            }
        }

        return null;
    }

    public interface ClickListener {
        void onClick(View child, int childIdx);
    }

    public interface DismissListener {
        void onDismiss(View child, int childIdx);
    }
}
