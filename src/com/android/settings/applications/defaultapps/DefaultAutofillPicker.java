/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.defaultapps;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.service.autofill.AutofillService;
import android.service.autofill.AutofillServiceInfo;
import android.text.Html;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment.ConfirmationDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class DefaultAutofillPicker extends DefaultAppPickerFragment {

    static final String SETTING = Settings.Secure.AUTOFILL_SERVICE;
    static final Intent AUTOFILL_PROBE = new Intent(AutofillService.SERVICE_INTERFACE);

    /**
     * Extra set when the fragment is implementing ACTION_REQUEST_SET_AUTOFILL_SERVICE.
     */
    public static final String EXTRA_PACKAGE_NAME = "package_name";

    /**
     * Set when the fragment is implementing ACTION_REQUEST_SET_AUTOFILL_SERVICE.
     */
    public DialogInterface.OnClickListener mCancelListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        if (activity != null && activity.getIntent().getStringExtra(EXTRA_PACKAGE_NAME) != null) {
            mCancelListener = (d, w) -> {
                activity.setResult(Activity.RESULT_CANCELED);
                activity.finish();
            };
        }
    }

    @Override
    protected ConfirmationDialogFragment newConfirmationDialogFragment(String selectedKey,
            CharSequence confirmationMessage) {
        return ConfirmationDialogFragment.newInstance(this, selectedKey, confirmationMessage,
                mCancelListener);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEFAULT_AUTOFILL_PICKER;
    }

    @Override
    protected boolean shouldShowItemNone() {
        return true;
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        final List<ResolveInfo> resolveInfos = mPm.getPackageManager()
                .queryIntentServices(AUTOFILL_PROBE, PackageManager.GET_META_DATA);
        for (ResolveInfo info : resolveInfos) {
            candidates.add(new DefaultAppInfo(mPm, mUserId, new ComponentName(
                    info.serviceInfo.packageName, info.serviceInfo.name)));
        }
        return candidates;
    }

    public static String getDefaultKey(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), SETTING);
    }

    @Override
    protected String getDefaultKey() {
        return getDefaultKey(getContext());
    }

    @Override
    protected CharSequence getConfirmationMessage(CandidateInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        final CharSequence appName = appInfo.loadLabel();
        final String message = getContext().getString(
                R.string.autofill_confirmation_message, appName);
        return Html.fromHtml(message);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        Settings.Secure.putString(getContext().getContentResolver(), SETTING, key);

        // Check if activity was launched from Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE
        // intent, and set proper result if so...
        final Activity activity = getActivity();
        if (activity != null) {
            final String packageName = activity.getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
            if (packageName != null) {
                final int result = key != null && key.startsWith(packageName) ? Activity.RESULT_OK
                        : Activity.RESULT_CANCELED;
                activity.setResult(result);
                activity.finish();
            }
        }
        return true;
    }

    /**
     * Provides Intent to setting activity for the specified autofill service.
     */
    static final class AutofillSettingIntentProvider implements SettingIntentProvider {

        private final String mSelectedKey;
        private final PackageManager mPackageManager;

        public AutofillSettingIntentProvider(PackageManager packageManager, String key) {
            mSelectedKey = key;
            mPackageManager = packageManager;
        }

        @Override
        public Intent getIntent() {
            final List<ResolveInfo> resolveInfos = mPackageManager.queryIntentServices(
                    AUTOFILL_PROBE, PackageManager.GET_META_DATA);

            for (ResolveInfo resolveInfo : resolveInfos) {
                final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                final String flattenKey = new ComponentName(
                        serviceInfo.packageName, serviceInfo.name).flattenToString();
                if (TextUtils.equals(mSelectedKey, flattenKey)) {
                    final String settingsActivity = new AutofillServiceInfo(
                            mPackageManager, serviceInfo)
                            .getSettingsActivity();
                    if (TextUtils.isEmpty(settingsActivity)) {
                        return null;
                    }
                    return new Intent(Intent.ACTION_MAIN).setComponent(
                            new ComponentName(serviceInfo.packageName, settingsActivity));
                }
            }

            return null;
        }
    }
}
