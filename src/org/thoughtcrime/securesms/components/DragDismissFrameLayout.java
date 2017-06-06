/*
 * Copyright (C) 2017 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class DragDismissFrameLayout extends FrameLayout {
  private final static float DRAG_DISMISS_DISTANCE_SCREEN_FRACTION = 0.3f;

  private @Nullable GestureDetectorCompat gestureDetector;
  private           DragDismissListener   dismissListener;

  private boolean isBeingDragged;
  private boolean ignoreGesture;
  private float   currentDragDistance;
  private float   dragDismissDistance;

  public DragDismissFrameLayout(Context context) {
    this(context, null, 0);
  }

  public DragDismissFrameLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DragDismissFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      this.gestureDetector = new GestureDetectorCompat(getContext(), new VerticalScrollListener());
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    return gestureDetector != null && dispatchGestureDetector(ev);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return gestureDetector != null && dispatchGestureDetector(event);
  }

  private boolean dispatchGestureDetector(MotionEvent event) {
    final int action = event.getAction() & MotionEventCompat.ACTION_MASK;

    if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
      resetLayout();
      return false;
    }

    return gestureDetector != null && gestureDetector.onTouchEvent(event);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private void resetLayout() {
    if (currentDragDistance != 0f) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
        animate().translationY(0f)
                 .setDuration(200L)
                 .setListener(null);
      } else {
        setTranslationY(0f);
      }
    }

    if (dismissListener != null && Math.abs(currentDragDistance) >= dragDismissDistance) {
      dismissListener.onDragDismissed();
    }

    isBeingDragged = false;
    ignoreGesture = false;
    currentDragDistance = 0f;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    dragDismissDistance = h * DRAG_DISMISS_DISTANCE_SCREEN_FRACTION;
  }

  public void setDragDismissListener(DragDismissListener listener) {
    this.dismissListener = listener;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private void performDrag(float newDragDistance) {
    if (newDragDistance == 0) return;

    currentDragDistance = newDragDistance;
    final float dragFraction    = (float) Math.log10(1 + (Math.abs(newDragDistance) / dragDismissDistance));
    final float viewTranslation = dragFraction * dragDismissDistance * Math.signum(newDragDistance);

    setTranslationY(viewTranslation);
  }

  public interface DragDismissListener {
    void onDragDismissed();
  }

  private class VerticalScrollListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      final float yDiff = e2.getY() - e1.getY();
      final float xDiff = e2.getX() - e1.getX();

      if (!isBeingDragged && !ignoreGesture) {
        isBeingDragged = Math.abs(yDiff) * 0.5f > Math.abs(xDiff);
        ignoreGesture  = !isBeingDragged;
      }

      if (isBeingDragged) performDrag(yDiff);

      return isBeingDragged;
    }
  }
}
