package com.termux.app.terminal.io;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.terminal.TerminalSession;

public class TerminalToolbarViewPager {

    public static class PageAdapter extends PagerAdapter {

        final TermuxActivity mActivity;
        String mSavedTextInput;

        public PageAdapter(TermuxActivity activity, String savedTextInput) {
            this.mActivity = activity;
            this.mSavedTextInput = savedTextInput;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            View layout;
            if (position == 0) {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_extra_keys, collection, false);
                ExtraKeysView extraKeysView = (ExtraKeysView) layout;
                extraKeysView.setExtraKeysViewClient(mActivity.getTermuxTerminalExtraKeys());
                extraKeysView.setButtonTextAllCaps(mActivity.getProperties().shouldExtraKeysTextBeAllCaps());

                String textColorStr = mActivity.getProperties().getExtraKeysButtonTextColor();
                String activeTextColorStr = mActivity.getProperties().getExtraKeysButtonActiveTextColor();
                String bgColorStr = mActivity.getProperties().getExtraKeysButtonBackgroundColor();
                String activeBgColorStr = mActivity.getProperties().getExtraKeysButtonActiveBackgroundColor();
                String areaBgColorStr = mActivity.getProperties().getExtraKeysButtonAreaBackgroundColor();
                String gapStr = mActivity.getProperties().getExtraKeysButtonGap();
                int textColor = textColorStr != null ? Color.parseColor(textColorStr) : ExtraKeysView.DEFAULT_BUTTON_TEXT_COLOR;
                int activeTextColor = activeTextColorStr != null ? Color.parseColor(activeTextColorStr) : ExtraKeysView.DEFAULT_BUTTON_ACTIVE_TEXT_COLOR;
                int bgColor = bgColorStr != null ? Color.parseColor(bgColorStr) : ExtraKeysView.DEFAULT_BUTTON_BACKGROUND_COLOR;
                int activeBgColor = activeBgColorStr != null ? Color.parseColor(activeBgColorStr) : ExtraKeysView.DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR;
                int areaBgColor = areaBgColorStr != null ? Color.parseColor(areaBgColorStr) : ExtraKeysView.DEFAULT_BUTTON_AREA_BACKGROUND_COLOR;
                int gap = gapStr != null ? Integer.parseInt(gapStr) : ExtraKeysView.DEFAULT_BUTTON_GAP;
                extraKeysView.setButtonColors(textColor, activeTextColor, bgColor, activeBgColor);
                extraKeysView.setButtonAreaBackgroundColor(areaBgColor);
                extraKeysView.setButtonGap(gap);
                collection.setBackgroundColor(areaBgColor);

                mActivity.setExtraKeysView(extraKeysView);
                extraKeysView.reload(mActivity.getTermuxTerminalExtraKeys().getExtraKeysInfo(),
                    mActivity.getTerminalToolbarDefaultHeight());

                // apply extra keys fix if enabled in prefs
                if (mActivity.getProperties().isUsingFullScreen() && mActivity.getProperties().isUsingFullScreenWorkAround()) {
                    FullScreenWorkAround.apply(mActivity);
                }

            } else {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_text_input, collection, false);
                final EditText editText = layout.findViewById(R.id.terminal_toolbar_text_input);

                if (mSavedTextInput != null) {
                    editText.setText(mSavedTextInput);
                    mSavedTextInput = null;
                }

                editText.setOnEditorActionListener((v, actionId, event) -> {
                    TerminalSession session = mActivity.getCurrentSession();
                    if (session != null) {
                        if (session.isRunning()) {
                            String textToSend = editText.getText().toString();
                            if (textToSend.length() == 0) textToSend = "\r";
                            session.write(textToSend);
                        } else {
                            mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                        }
                        editText.setText("");
                    }
                    return true;
                });
            }
            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }

    }



    public static class OnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

        final TermuxActivity mActivity;
        final ViewPager mTerminalToolbarViewPager;

        public OnPageChangeListener(TermuxActivity activity, ViewPager viewPager) {
            this.mActivity = activity;
            this.mTerminalToolbarViewPager = viewPager;
        }

        @Override
        public void onPageSelected(int position) {
            if (position == 0) {
                mActivity.getTerminalView().requestFocus();
            } else {
                final EditText editText = mTerminalToolbarViewPager.findViewById(R.id.terminal_toolbar_text_input);
                if (editText != null) editText.requestFocus();
            }
        }

    }

}
