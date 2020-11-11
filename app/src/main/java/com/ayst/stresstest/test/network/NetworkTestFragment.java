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

package com.ayst.stresstest.test.network;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseCountTestWithTimerFragment;
import com.ayst.stresstest.test.base.TestType;
import com.ayst.stresstest.util.NetworkUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class NetworkTestFragment extends BaseCountTestWithTimerFragment {

    @BindView(R.id.edt_url)
    EditText mUrlEdt;
    Unbinder unbinder;

    private String mTestUrl = "www.baidu.com";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.network_test);
        setType(TestType.TYPE_NETWORK_TEST);

        View contentView = inflater.inflate(R.layout.fragment_network_test, container, false);
        setContentView(contentView);

        unbinder = ButterKnife.bind(this, contentView);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mUrlEdt.setText(mTestUrl);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onStartClicked() {
        mTestUrl = mUrlEdt.getText().toString();
        if (TextUtils.isEmpty(mTestUrl)) {
            Toast.makeText(mActivity, R.string.network_test_url_empty_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        super.onStartClicked();
    }

    @Override
    public boolean isSupport() {
        return true;
    }

    @Override
    protected boolean testOnce() {
        return NetworkUtils.isAvailableByDns(mTestUrl);
    }
}
