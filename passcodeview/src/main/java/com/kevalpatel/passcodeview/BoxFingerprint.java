/*
 * Copyright 2017 Keval Patel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kevalpatel.passcodeview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.support.annotation.ColorInt;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.view.animation.CycleInterpolator;

import com.kevalpatel.passcodeview.interfaces.AuthenticationListener;

/**
 * Created by Keval on 07-Apr-17.
 *
 * @author 'https://github.com/kevalpatel2106'
 */

final class BoxFingerprint extends Box implements FingerPrintAuthHelper.FingerPrintAuthCallback {
    static final String DEF_FINGERPRINT_STATUS = "Scan your finger to authenticate";

    private Boolean isFingerPrintBoxVisible;
    private Rect mBounds = new Rect();

    @Nullable
    private AuthenticationListener mAuthListener;

    @ColorInt
    private int mStatusTextColor;
    @Dimension
    private float mStatusTextSize;
    private String mNormalStatusText;

    private String mCurrentStatusText;

    private TextPaint mStatusTextPaint;

    @Nullable
    private FingerPrintAuthHelper mFingerPrintAuthHelper;

    BoxFingerprint(@NonNull PasscodeView passcodeView) {
        super(passcodeView);
        init();
    }

    private void init() {
        preparePaint();
        isFingerPrintBoxVisible = Utils.isFingerPrintEnrolled(getContext());

        if (isFingerPrintBoxVisible) {
            mFingerPrintAuthHelper = new FingerPrintAuthHelper(getContext(), this);
            mFingerPrintAuthHelper.startAuth();
        }
    }

    void stopFingerprintScanner() {
        if (mFingerPrintAuthHelper != null) mFingerPrintAuthHelper.stopAuth();
    }

    @Override
    @SuppressWarnings("deprecation")
    void setDefaults() {
        mStatusTextSize = getContext().getResources().getDimension(R.dimen.lib_fingerprint_status_text_size);
        mNormalStatusText = DEF_FINGERPRINT_STATUS;
        mCurrentStatusText = mNormalStatusText;
        mStatusTextColor = getContext().getResources().getColor(R.color.lib_key_default_color);
    }

    @Override
    void onAuthenticationFail() {
        //Do nothing
    }

    @Override
    void onAuthenticationSuccess() {
        //Do nothing
    }

    private int mFingerPrintIcon;

    public void setFingerPrintIcon(int drawable){
        mFingerPrintIcon=drawable;
    }

    @SuppressWarnings("deprecation")
    @Override
    void draw(@NonNull Canvas canvas) {
        if (isFingerPrintBoxVisible) {
            //Show fingerprint icon
            //Drawable d = getContext().getResources().getDrawable(R.drawable.ic_fingerprint);
            Drawable d = getContext().getResources().getDrawable(mFingerPrintIcon);
            d.setBounds((int) (mBounds.exactCenterX() - mBounds.height() / 3),
                    mBounds.top + 15,
                    (int) (mBounds.exactCenterX() + mBounds.height() / 3),
                    (int)(mBounds.top + mBounds.height() / 1.5 + 15));
            //d.setColorFilter(new PorterDuffColorFilter(mStatusTextPaint.getColor(), PorterDuff.Mode.SRC_ATOP));
            d.draw(canvas);
            //Show finger print text
            canvas.drawText(mCurrentStatusText,
                    mBounds.exactCenterX(),
                    (float) (mBounds.top + (mBounds.height() / 1.3) - ((mStatusTextPaint.descent() + mStatusTextPaint.ascent()) / 2)),
                    mStatusTextPaint);
        }
    }

    @Override
    void measure(@NonNull Rect rootViewBounds) {
        if (isFingerPrintBoxVisible) {
            mBounds.left = rootViewBounds.left;
            mBounds.right = rootViewBounds.right;
            mBounds.top = (int) (rootViewBounds.bottom - rootViewBounds.height() * (Constants.KEY_BOARD_BOTTOM_WEIGHT));
            mBounds.bottom = rootViewBounds.bottom;
        }
    }

    @Override
    void preparePaint() {
        mStatusTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mStatusTextPaint.setTextAlign(Paint.Align.CENTER);
        mStatusTextPaint.setTextSize(mStatusTextSize);
        mStatusTextPaint.setColor(mStatusTextColor);
    }

    @Override
    public void onFingerprintAuthSuccess(FingerprintManager.CryptoObject cryptoObject) {
        mCurrentStatusText = "Fingerprint recognized";
        getRootView().invalidate();

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mAuthListener != null) mAuthListener.onAuthenticationSuccessful();
                mCurrentStatusText = mNormalStatusText;
                getRootView().invalidate();
            }
        }, 1000);
    }

    @Override
    public void onFingerprintAuthFailed(int errorCode, String errorMessage) {
        switch (errorCode) {
            case FingerPrintAuthHelper.CANNOT_RECOGNIZE_ERROR:
            case FingerPrintAuthHelper.NON_RECOVERABLE_ERROR:
            case FingerPrintAuthHelper.RECOVERABLE_ERROR:
                mStatusTextPaint.setColor(Color.RED);
                mCurrentStatusText = errorMessage;
                playErrorAnimation();
                break;
        }

    }

    /**
     * Apply the error animations which will move key left to right and after right to left for two times.
     */
    private void playErrorAnimation() {
        ValueAnimator goLeftAnimator = ValueAnimator.ofInt(0, 10);
        goLeftAnimator.setInterpolator(new CycleInterpolator(2));
        goLeftAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mBounds.left += (int) animation.getAnimatedValue();
                mBounds.right += (int) animation.getAnimatedValue();
                getRootView().invalidate();
            }
        });
        goLeftAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentStatusText = mNormalStatusText;
                        mStatusTextPaint.setColor(mStatusTextColor);
                        getRootView().invalidate();
                    }
                }, 1000);
                if (mAuthListener != null) mAuthListener.onAuthenticationFailed();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        goLeftAnimator.start();
    }

    ///////////////// SETTERS/GETTERS //////////////

    @NonNull
    String getStatusText() {
        return mNormalStatusText;
    }

    void setStatusText(@NonNull String statusText) {
        this.mNormalStatusText = statusText;
        mCurrentStatusText = mNormalStatusText;
    }

    int getStatusTextColor() {
        return mStatusTextColor;
    }

    void setStatusTextColor(@ColorInt int statusTextColor) {
        this.mStatusTextColor = statusTextColor;
    }

    float getStatusTextSize() {
        return mStatusTextSize;
    }

    void setStatusTextSize(float statusTextSize) {
        this.mStatusTextSize = statusTextSize;
    }

    Boolean isFingerPrintEnable() {
        return isFingerPrintBoxVisible;
    }

    /**
     * Enable/Disable finger print scanning programmatically.
     *
     * @param isEnable true if the fingerprint scanning is enabled.
     */
    void setFingerPrintEnable(boolean isEnable) {
        this.isFingerPrintBoxVisible = isEnable && Utils.isFingerPrintEnrolled(getContext());
    }

    /**
     * Authentication callback listener. If {@link AuthenticationListener} is not set, fingerprint
     * authentication callbacks won't get call.
     *
     * @param authListener {@link AuthenticationListener}
     */
    void setAuthListener(@NonNull AuthenticationListener authListener) {
        this.mAuthListener = authListener;
    }
}
