package com.ayst.stresstest.view;

import android.content.Context;
import android.util.AttributeSet;

import com.ayst.stresstest.StressTestApplication;

/**
 * Created by Habo Shen on 2017/1/2.
 */

public class DincondFontTextView extends android.support.v7.widget.AppCompatTextView {
    public DincondFontTextView(Context context) {
        super(context);
        setTypeface();
    }

    public DincondFontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeface();
    }

    public DincondFontTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTypeface();
    }

    private void setTypeface() {
        // 如果自定义typeface初始化失败，就用原生的typeface
        if (StressTestApplication.sMIUIBoldTextType == null) {
            setTypeface(getTypeface());
        } else {
            setTypeface(StressTestApplication.sMIUIBoldTextType);
        }
    }
}
