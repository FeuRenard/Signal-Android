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
package org.thoughtcrime.securesms.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class NavigationBarSizeUtil {
  @TargetApi(Build.VERSION_CODES.KITKAT)
  public static Rect getNavigationBarSize(Context context) {
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display       display       = windowManager.getDefaultDisplay();

    Point appUsableSize  = new Point();
    Point realScreenSize = new Point();
    display.getSize(appUsableSize);
    display.getRealSize(realScreenSize);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        display.getRotation() == Surface.ROTATION_270) {
      return new Rect(realScreenSize.x - appUsableSize.x, 0, 0, realScreenSize.y - appUsableSize.y);
    } else {
      return new Rect(0, 0, realScreenSize.x - appUsableSize.x, realScreenSize.y - appUsableSize.y);
    }
  }
}
