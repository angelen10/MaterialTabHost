/*
 Copyright (C) 2014 Yuki Anzai anzai.y.aa@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.yanzm.mth;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

/**
 * MaterialTabHost make easy to create Material Design Fixed Tabs
 * http://www.google.com/design/spec/components/tabs.html
 */
public class MaterialTabHost extends TabHost implements ViewPager.OnPageChangeListener {

    // http://www.google.com/design/spec/what-is-material/objects-in-3d-space.html#objects-in-3d-space-elevation
    private static final int APP_TAB_ELEVATION = 4; // 4dp

    public static enum Type {
        FullScreenWidth, Centered, LeftOffset;
    }

    public interface OnTabChangeListener {
        public void onTabSelected(int position);
    }

    private final LayoutInflater inflater;
    private final TabWidget tabWidget;

    private final ShapeDrawable indicator;
    private final int indicatorHeight;
    private final int leftOffset;
    private final int colorControlActivated;

    private Type type = Type.FullScreenWidth;
    private OnTabChangeListener listener;

    private int maxTabWidth = Integer.MIN_VALUE;
    private int scrollingState = ViewPager.SCROLL_STATE_IDLE;
    private int position = 0;
    private float positionOffset = 0;

    public MaterialTabHost(Context context) {
        this(context, null);
    }

    public MaterialTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflater = LayoutInflater.from(context);

        TypedValue outValue = new TypedValue();
        Resources.Theme theme = context.getTheme();

        theme.resolveAttribute(R.attr.colorPrimary, outValue, true);
        int colorPrimary = outValue.data;
        setBackgroundColor(colorPrimary);

        theme.resolveAttribute(R.attr.colorControlActivated, outValue, true);
        colorControlActivated = outValue.data;
        // ColorDrawable on 2.x does not use getBounds() so use ShapeDrawable
        indicator = new ShapeDrawable();
        indicator.setColorFilter(colorControlActivated, PorterDuff.Mode.SRC_ATOP);

        Resources res = context.getResources();
        indicatorHeight = res.getDimensionPixelSize(R.dimen.mth_tab_indicator_height);
        leftOffset = res.getDimensionPixelSize(R.dimen.mth_tab_left_offset);
        int tabHeight = res.getDimensionPixelSize(R.dimen.mth_tab_height);

        tabWidget = new TabWidget(context);
        tabWidget.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, tabHeight));
        tabWidget.setId(android.R.id.tabs);
        tabWidget.setStripEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            tabWidget.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        }
        addView(tabWidget);

        FrameLayout fl = new FrameLayout(context);
        fl.setLayoutParams(new LayoutParams(0, 0));
        fl.setId(android.R.id.tabcontent);
        addView(fl);

        setup();

        setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (listener != null) {
                    listener.onTabSelected(Integer.valueOf(tabId));
                }
            }
        });

        float density = getResources().getDisplayMetrics().density;

        // set elevation for App bar
        // http://www.google.com/design/spec/what-is-material/objects-in-3d-space.html#objects-in-3d-space-elevation
        ViewCompat.setElevation(this, APP_TAB_ELEVATION * density);
    }

    protected int getLayoutId(Type type) {
        switch (type) {
            case FullScreenWidth:
                return R.layout.tab_widget_full;
            case Centered:
                return R.layout.tab_widget;
            case LeftOffset:
                return R.layout.tab_widget;
            default:
                return R.layout.tab_widget_full;
        }
    }

    public void addTab(CharSequence title) {
        int layoutId = getLayoutId(type);
        TextView tv = (TextView) inflater.inflate(layoutId, tabWidget, false);
        tv.setText(title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tv.setBackgroundResource(R.drawable.tab_widget_background_ripple);

        } else {
            // create background using colorControlActivated
            StateListDrawable d = new StateListDrawable();
            d.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(colorControlActivated));
            d.setAlpha(180);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                tv.setBackground(d);
            } else {
                tv.setBackgroundDrawable(d);
            }
        }

        int tabId = tabWidget.getTabCount();

        addTab(newTabSpec(String.valueOf(tabId))
                .setIndicator(tv)
                .setContent(android.R.id.tabcontent));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (type == Type.Centered) {
            if (maxTabWidth == Integer.MIN_VALUE) {
                for (int i = 0; i < tabWidget.getTabCount(); i++) {
                    View tabView = tabWidget.getChildTabViewAt(i);
                    if (tabView.getMeasuredWidth() > maxTabWidth) {
                        maxTabWidth = tabView.getMeasuredWidth();
                    }
                }

                if (maxTabWidth > 0) {
                    for (int i = 0; i < tabWidget.getTabCount(); i++) {
                        View tabView = tabWidget.getChildTabViewAt(i);
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tabView.getLayoutParams();
                        params.width = maxTabWidth;
                        tabView.setLayoutParams(params);
                    }
                }
            }
        }

        super.onLayout(changed, left, top, right, bottom);
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (getChildCount() == 0) {
            return;
        }

        final Drawable d = indicator;

        View tabView = tabWidget.getChildTabViewAt(position);
        View nextTabView = position + 1 < tabWidget.getTabCount()
                ? tabWidget.getChildTabViewAt(position + 1)
                : null;

        int tabWidth = tabView.getWidth();
        int nextTabWidth = nextTabView == null ? tabWidth : nextTabView.getWidth();

        int indicatorWidth = (int) (nextTabWidth * positionOffset + tabWidth * (1 - positionOffset));
        int indicatorLeft = (int) (getPaddingLeft() + tabView.getLeft() + positionOffset * tabWidth);

        int height = getHeight();
        d.setBounds(indicatorLeft, height - indicatorHeight, indicatorLeft + indicatorWidth, height);
        d.draw(canvas);
    }

    public void setOnTabChangeListener(OnTabChangeListener l) {
        listener = l;
    }

    public void setType(Type type) {
        this.type = type;

        switch (type) {
            case FullScreenWidth:
                tabWidget.setGravity(Gravity.LEFT);
                setPadding(0, 0, 0, 0);
                break;
            case Centered:
                tabWidget.setGravity(Gravity.CENTER_HORIZONTAL);
                setPadding(0, 0, 0, 0);
                break;
            case LeftOffset:
                tabWidget.setGravity(Gravity.LEFT);
                setPadding(leftOffset, 0, 0, 0);
                break;
            default:
                tabWidget.setGravity(Gravity.LEFT);
                setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (scrollingState == ViewPager.SCROLL_STATE_IDLE) {
            updateIndicatorPosition(position, 0);
        }
        tabWidget.setCurrentTab(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        scrollingState = state;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        updateIndicatorPosition(position, positionOffset);
    }

    private void updateIndicatorPosition(int position, float positionOffset) {
        this.position = position;
        this.positionOffset = positionOffset;
        invalidate();
    }
}
