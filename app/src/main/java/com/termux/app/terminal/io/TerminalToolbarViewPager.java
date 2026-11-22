package com.termux.app.terminal.io;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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

                // The view is multi-line (set in the layout) so long text wraps and scrolls
                // vertically, but the keyboard is told it is a plain text field. That keeps the
                // keyboard's Enter key as the "Send" action instead of inserting a new line.
                // A hardware or soft keyboard that sends Shift+Enter as a key event still inserts
                // a new line, because TextView only routes modifier-free Enter to the action listener.
                editText.setRawInputType(InputType.TYPE_CLASS_TEXT);

                // Horizontal swipes switch the toolbar page. Without this, Android's
                // "drag cursor from anywhere" takes them over and moves the caret instead.
                editText.setOnTouchListener(new HorizontalSwipePageSwitcher((ViewPager) collection));

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



    /**
     * Touch listener for the toolbar text input. A clearly horizontal swipe (past twice the touch
     * slop and at least twice as wide as it is tall) cancels the text view's own handling and
     * switches the toolbar page: swipe right goes to the extra keys, swipe left to the text input.
     * Taps, long presses and vertical drags (which scroll wrapped text) are passed through.
     * A drag that starts only after the long-press timeout (long press, then drag to extend the
     * selection) is also left to the text view.
     */
    static class HorizontalSwipePageSwitcher implements View.OnTouchListener {

        private final ViewPager mViewPager;
        private final int mSwipeSlop;
        private final int mLongPressTimeout;
        private float mDownX;
        private float mDownY;
        private boolean mSwiping;
        private boolean mIgnoreGesture;

        HorizontalSwipePageSwitcher(ViewPager viewPager) {
            mViewPager = viewPager;
            mSwipeSlop = 2 * ViewConfiguration.get(viewPager.getContext()).getScaledTouchSlop();
            mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mDownX = event.getRawX();
                    mDownY = event.getRawY();
                    mSwiping = false;
                    mIgnoreGesture = false;
                    return false;

                case MotionEvent.ACTION_MOVE:
                    if (mSwiping) return true;
                    if (mIgnoreGesture) return false;
                    float dx = event.getRawX() - mDownX;
                    float dy = event.getRawY() - mDownY;
                    if (Math.abs(dx) <= mSwipeSlop) return false;
                    if (event.getEventTime() - event.getDownTime() >= mLongPressTimeout) {
                        // The finger stayed still long enough for a long press, so this drag is
                        // extending a text selection, not a page swipe.
                        mIgnoreGesture = true;
                        return false;
                    }
                    if (Math.abs(dx) > 2 * Math.abs(dy)) {
                        mSwiping = true;
                        // End the text view's gesture (cursor drag, selection) before switching.
                        MotionEvent cancel = MotionEvent.obtain(event);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        view.onTouchEvent(cancel);
                        cancel.recycle();
                        mViewPager.setCurrentItem(dx > 0 ? 0 : 1, true);
                        return true;
                    }
                    return false;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    boolean consumed = mSwiping;
                    mSwiping = false;
                    return consumed;

                default:
                    return mSwiping;
            }
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
