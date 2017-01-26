package biz.borealis.numberpicker;

import android.animation.ArgbEvaluator;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

public class ValuePicker extends FrameLayout {
    private int mItemBigHeight;
    private int mItemSmallHeight;
    private int mAllVerticalScroll;
    private int mMin;
    private int mMax;
    private final RecyclerView mRecyclerView;
    private ValuePickerAdapter mValuePickerAdapter;
    private float mTextSize;
    private float mTextSizeSelected;
    private int mTextColor;
    private int mTextColorSelected;
    private int mNumberVisibleItems;
    private boolean mAnimateTextSize, mTextFadeColor;
    private OnValueChangeListener mOnValueChangeListener;

    private int mDividerHeight;
    private int mDividerColor;
    private boolean mIsDividerVisible;
    private View mTopDivider;
    private View mBottomDivider;
    private int mNumberOfTopPaddingItems = 2;

    public ValuePicker(Context context) {
        this(context, null);
    }

    public ValuePicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ValuePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.np_ValuePicker, defStyleAttr, 0);

        Resources resources = context.getResources();
        mMin = a.getInt(R.styleable.np_ValuePicker_np_min_number, resources.getInteger(R.integer.np_def_min));
        mMax = a.getInt(R.styleable.np_ValuePicker_np_max_number, resources.getInteger(R.integer.np_def_max));
        mNumberVisibleItems = Math.max(1, a.getInt(R.styleable.np_ValuePicker_np_number_visible_items, resources.getInteger(R.integer.np_def_visible_items)));

        mTextColor = a.getColor(R.styleable.np_ValuePicker_np_text_color, ContextCompat.getColor(context, R.color.np_text_color));
        mTextSize = a.getDimension(R.styleable.np_ValuePicker_np_text_size, resources.getDimension(R.dimen.np_text_size));

        mTextColorSelected = a.getColor(R.styleable.np_ValuePicker_np_text_color_selected, ContextCompat.getColor(context, R.color.np_text_color_selected));
        mTextSizeSelected = a.getDimension(R.styleable.np_ValuePicker_np_text_size_selected, resources.getDimension(R.dimen.np_text_size_selected));

        mTextFadeColor = a.getBoolean(R.styleable.np_ValuePicker_np_fade_text_color, resources.getBoolean(R.bool.np_def_fade_color));
        mAnimateTextSize = a.getBoolean(R.styleable.np_ValuePicker_np_animate_text_size, resources.getBoolean(R.bool.np_def_animate_text_size));
        mIsDividerVisible = a.getBoolean(R.styleable.np_ValuePicker_np_divider_visible, resources.getBoolean(R.bool.np_def_divider_visible));
        mDividerColor = a.getColor(R.styleable.np_ValuePicker_np_divider_color, ContextCompat.getColor(context, R.color.np_def_divider_color));
        mDividerHeight = a.getInt(R.styleable.np_ValuePicker_np_divider_height_px, resources.getInteger(R.integer.np_def_divider_height));

        a.recycle();

        setMinimumWidth(context.getResources().getDimensionPixelSize(R.dimen.np_min_width));

        mRecyclerView = new RecyclerView(context);
        setupRecyclerView();

        mTopDivider = new View(context);
        mBottomDivider = new View(context);

        viewRefresh();

        addView(mRecyclerView);
        addView(mTopDivider);
        addView(mBottomDivider);
    }

    private void setupViewSize() {
        Context context = mRecyclerView.getContext();

        mItemSmallHeight = getTextViewHeight(context, false, mTextSize, mTextSize);
        mItemBigHeight = getTextViewHeight(context, true, mTextSizeSelected, mTextSizeSelected);
        int listHeight = (mItemSmallHeight * (mNumberVisibleItems - 1)) + mItemBigHeight;
        mRecyclerView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, listHeight));

        int dividerMargin = listHeight - (mItemBigHeight/2);
        int dividerVisibility = mIsDividerVisible ? View.VISIBLE : View.GONE;

        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mDividerHeight);
        topParams.setMargins(0, dividerMargin, 0, 0);
        mTopDivider.setLayoutParams(topParams);
        mTopDivider.setVisibility(dividerVisibility);

        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mDividerHeight);
        bottomParams.setMargins(0, 0, 0, dividerMargin);
        mBottomDivider.setVisibility(dividerVisibility);
    }

    private void setupRecyclerView() {
        Context context = mRecyclerView.getContext();

        mNumberOfTopPaddingItems = (mNumberVisibleItems - 1) / 2;

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mAllVerticalScroll = 0;
        final LinearLayoutManager dateLayoutManager = new LinearLayoutManager(context);
        dateLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(dateLayoutManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                synchronized (this) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        calculatePositionAndScroll();
                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        if (mValuePickerAdapter != null) {
                            mValuePickerAdapter.setSelectedIndex(ValuePickerAdapter.POSITION_NONE);
                        }
                    }
                }
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mAllVerticalScroll += dy;
            }
        });
        mValuePickerAdapter = new ValuePickerAdapter(context, getMin(), getMax());
        mRecyclerView.setAdapter(mValuePickerAdapter);
        mValuePickerAdapter.setSelectedIndex(0);
    }

    private void viewRefresh() {
        mBottomDivider.setBackgroundColor(mDividerColor);
        mTopDivider.setBackgroundColor(mDividerColor);
        setupViewSize();
        mValuePickerAdapter.notifyDataSetChanged();
    }

    public OnValueChangeListener getOnValueChangeListener() {
        return mOnValueChangeListener;
    }

    public void setOnValueChangeListener(OnValueChangeListener mOnValueChangeListener) {
        this.mOnValueChangeListener = mOnValueChangeListener;
    }

    public void updateValues(List<String> values) {
        this.mValuePickerAdapter = new ValuePickerAdapter(getContext(), values);
        this.mRecyclerView.setAdapter(this.mValuePickerAdapter);
        this.mValuePickerAdapter.setSelectedIndex(0);
    }

    public int getMin() {
        return mMin;
    }

    public void setMin(int min) {
        this.mMin = min;
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        this.mMax = max;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float mTextSize) {
        this.mTextSize = mTextSize;
        viewRefresh();
    }

    public float getTextSizeSelected() {
        return mTextSizeSelected;
    }

    public void setTextSizeSelected(float mTextSizeSelected) {
        this.mTextSizeSelected = mTextSizeSelected;
        viewRefresh();
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int mTextColor) {
        this.mTextColor = mTextColor;
        viewRefresh();
    }

    public int getTextColorSelected() {
        return mTextColorSelected;
    }

    public void setTextColorSelected(int mTextColorSelected) {
        this.mTextColorSelected = mTextColorSelected;
        viewRefresh();
    }

    public String getSelectedValue() {
        return mValuePickerAdapter.getSelectedValue();
    }

    public int getSelectedIndex() {
        return mValuePickerAdapter.selectedItemIndex;
    }

    private void calculatePositionAndScroll() {
        int expectedPosition = Math.round(mAllVerticalScroll / mItemSmallHeight);
        if (expectedPosition == -1) {
            expectedPosition = 0;
        } else if (expectedPosition >= mRecyclerView.getAdapter().getItemCount() - 2) {
            expectedPosition = mRecyclerView.getAdapter().getItemCount() - 2;
            mAllVerticalScroll = Math.round(expectedPosition * mItemSmallHeight);
        }
        scrollListToPosition(expectedPosition);
    }

    private static int dp2px(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    private void scrollListToPosition(int expectedPosition) {
        float targetScrollPosDate = expectedPosition * mItemSmallHeight;
        final float missingPxDate = targetScrollPosDate - mAllVerticalScroll;
        if (missingPxDate != 0) {
            mRecyclerView.smoothScrollBy(0, (int) missingPxDate);
        }
        mValuePickerAdapter.setSelectedAbsoluteIndex(Math.round(mAllVerticalScroll / mItemSmallHeight) + 1);
    }

    @NonNull
    private static TextView getTextView(Context context, boolean isBig, float textSize, float textSizeSelected) {
        TextView number = new TextView(context);
        number.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        number.setGravity(Gravity.CENTER_HORIZONTAL);
        if (isBig) {
            number.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeSelected);
        } else {
            number.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        }

        return number;
    }

    @NonNull
    private TextView getTextView(Context context, float textSize, float textSizeSelected) {
        return getTextView(context, false, textSize, textSizeSelected);
    }

    public static int getTextViewHeight(Context context, boolean isBig, float textSize, float textSizeSelected) {
        TextView textView = getTextView(context, isBig, textSize, textSizeSelected);
        textView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        return textView.getMeasuredHeight();
    }

    private class ValuePickerAdapter extends RecyclerView.Adapter<ValuePickerAdapter.Holder> {
        private static final int VIEW_TYPE_PADDING = 0;
        private static final int VIEW_TYPE_ITEM = 1;
        private Context mContext;
        static final int POSITION_NONE = -1;

        private int selectedItemIndex = POSITION_NONE;
        private List<String> values;


        ValuePickerAdapter(Context context, int min, int max) {
            this.mContext = context;
            values = new LinkedList<>();
            for (int i = min; i <= max; i++)
                values.add("" + i);
        }

        ValuePickerAdapter(Context context, List<String> values) {
            this.mContext = context;
            this.values = values;
        }

        @Override
        public ValuePickerAdapter.Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_ITEM) {
                TextView number = getTextView(mContext, mTextSize, mTextSizeSelected);
                return new ItemHolder(number);
            } else {
                View paddingView = new View(mContext);
                RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(dp2px(mContext, 1), mItemSmallHeight);
                paddingView.setLayoutParams(layoutParams);
                return new PaddingHolder(paddingView);
            }
        }

        @Override
        public void onBindViewHolder(ValuePickerAdapter.Holder holder, int position) {

            if (holder instanceof PaddingHolder) {
                PaddingHolder paddingHolder = (PaddingHolder) holder;
                ViewGroup.LayoutParams params = paddingHolder.itemView.getLayoutParams();
                if (position != 0) {
                    params.height = (mItemSmallHeight + mItemBigHeight - mItemSmallHeight);
                } else {
                    params.height = mItemSmallHeight;
                }
            }
            if (holder instanceof ItemHolder) {
                final ItemHolder itemHolder = (ItemHolder) holder;
                int adjustedPosition = position - mNumberOfTopPaddingItems; // Adjusted position removes the 1st padding

                itemHolder.number.setText(values.get(adjustedPosition)); //minus padding view

                if (adjustedPosition == selectedItemIndex) {
                    if (mTextFadeColor) {
                        final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), itemHolder.number.getCurrentTextColor(), mTextColorSelected);
                        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                itemHolder.number.setTextColor((Integer) animator.getAnimatedValue());
                            }

                        });
                        colorAnimation.start();
                    } else {
                        itemHolder.number.setTextColor(mTextColorSelected);
                    }

                    if (mAnimateTextSize) {
                        ValueAnimator textSizeAnimation = ValueAnimator.ofObject(new FloatEvaluator(), mTextSize, mTextSizeSelected);
                        textSizeAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                itemHolder.number.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (Float) animator.getAnimatedValue());
                            }
                        });
                        textSizeAnimation.start();
                    } else {
                        itemHolder.number.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mTextSizeSelected);
                    }

                } else {
                    itemHolder.number.setTextColor(mTextColor);
                    itemHolder.number.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mTextSize);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position < mNumberOfTopPaddingItems || position >= getItemCount() - mNumberOfTopPaddingItems) {
                return VIEW_TYPE_PADDING;
            }
            return VIEW_TYPE_ITEM;
        }

        void setSelectedAbsoluteIndex(int absoluteIndex) {
            if (getItemViewType(absoluteIndex) == VIEW_TYPE_PADDING) {
                setSelectedIndex(POSITION_NONE);
                return;
            }
            setSelectedIndex(absoluteIndex - mNumberOfTopPaddingItems); // Adjust to position index
        }

        void setSelectedIndex(int selectedIndex) {
            if (selectedIndex != this.selectedItemIndex) {
                if (mOnValueChangeListener != null && selectedIndex != POSITION_NONE) {
                    mOnValueChangeListener.onValueChanged(values.get(selectedIndex));
                }
                this.selectedItemIndex = selectedIndex;
                notifyDataSetChanged();
            }

        }

        String getSelectedValue() {
            if (selectedItemIndex == POSITION_NONE) {
                return "";
            }
            return values.get(selectedItemIndex);
        }

        @Override
        public int getItemCount() {
            return values.size() + mNumberOfTopPaddingItems*2; // calculate number of items plus 2 padding
        }

        private class PaddingHolder extends Holder {
            private PaddingHolder(View itemView) {
                super(itemView);
            }
        }

        private class ItemHolder extends Holder {
            private TextView number;

            private ItemHolder(View itemView) {
                super(itemView);
                number = (TextView) itemView;
            }
        }

        class Holder extends RecyclerView.ViewHolder {
            private Holder(View itemView) {
                super(itemView);
            }
        }
    }

}
