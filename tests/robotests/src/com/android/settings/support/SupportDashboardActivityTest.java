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

package com.android.settings.support;


import android.content.Context;
import android.content.Intent;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search.SearchIndexableRaw;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SupportDashboardActivityTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void shouldIndexSearchActivityForSearch() {
        final List<SearchIndexableRaw> indexables =
                SupportDashboardActivity.SEARCH_INDEX_DATA_PROVIDER
                        .getRawDataToIndex(mContext, true /* enabled */);

        assertThat(indexables).hasSize(1);

        final SearchIndexableRaw value = indexables.get(0);

        assertThat(value.title).isEqualTo(mContext.getString(R.string.page_tab_title_support));
        assertThat(value.screenTitle).isEqualTo(mContext.getString(R.string.settings_label));
        assertThat(value.intentTargetPackage).isEqualTo(mContext.getPackageName());
        assertThat(value.intentTargetClass).isEqualTo(SupportDashboardActivity.class.getName());
        assertThat(value.intentAction).isEqualTo(Intent.ACTION_MAIN);
    }
}
