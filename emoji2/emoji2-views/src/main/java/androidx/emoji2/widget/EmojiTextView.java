/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.emoji2.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.widget.TextViewCompat;
import androidx.emoji2.helpers.EmojiTextViewHelper;

/**
 * TextView widget enhanced with emoji capability by using {@link EmojiTextViewHelper}. When used
 * on devices running API 18 or below, this widget acts as a regular {@link TextView}.
 */
public class EmojiTextView extends TextView {
    private EmojiTextViewHelper mEmojiTextViewHelper;

    /**
     * Prevent calling {@link #init()} multiple times in case super() constructors
     * call other constructors.
     */
    private boolean mInitialized;

    public EmojiTextView(@NonNull Context context) {
        super(context);
        init();
    }

    public EmojiTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EmojiTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint("UnsafeNewApiCall")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public EmojiTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        if (!mInitialized) {
            mInitialized = true;
            getEmojiTextViewHelper().updateTransformationMethod();
        }
    }

    @Override
    public void setFilters(@NonNull InputFilter[] filters) {
        super.setFilters(getEmojiTextViewHelper().getFilters(filters));
    }

    @Override
    public void setAllCaps(boolean allCaps) {
        super.setAllCaps(allCaps);
        getEmojiTextViewHelper().setAllCaps(allCaps);
    }

    private EmojiTextViewHelper getEmojiTextViewHelper() {
        if (mEmojiTextViewHelper == null) {
            mEmojiTextViewHelper = new EmojiTextViewHelper(this);
        }
        return mEmojiTextViewHelper;
    }

    /**
     * See
     * {@link TextViewCompat#setCustomSelectionActionModeCallback(TextView, ActionMode.Callback)}
     */
    @Override
    public void setCustomSelectionActionModeCallback(
            @NonNull ActionMode.Callback actionModeCallback
    ) {
        super.setCustomSelectionActionModeCallback(TextViewCompat
                .wrapCustomSelectionActionModeCallback(this, actionModeCallback));
    }
}
