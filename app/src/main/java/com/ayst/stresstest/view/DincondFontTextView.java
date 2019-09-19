/*
 * Copyright(c) 2018 Bob Shen <ayst.shen@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ayst.stresstest.view;

import android.content.Context;
import android.util.AttributeSet;

import com.ayst.stresstest.StressTestApplication;

/**
 * Created by Bob Shen on 2017/1/2.
 */

public class DincondFontTextView extends androidx.appcompat.widget.AppCompatTextView {
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
