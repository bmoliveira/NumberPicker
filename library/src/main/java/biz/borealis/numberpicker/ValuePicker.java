package biz.borealis.numberpicker;

import android.animation.ArgbEvaluator;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

public class ValuePicker extends LinearLayout {
    private int mItemBigHeight;
    private int mItemSmallHeight;
    private int mMin;
    private int mMax;
    private final RecyclerView mRecyclerView;
    private ValuePickerAdapter mValuePickerAdapter;

    private float mAllVerticalScroll;
    private int mTextSizePx;
    private int mTextSizeSelectedPx;

    private int mTextColor;
    private int mTextColorSelected;
    private int mNumberVisibleItems;
    private boolean mAnimateTextSize, mTextFadeColor;

    private OnValueChangeListener mOnValueChangeListener;


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
        mTextSizePx = a.getDimensionPixelSize(R.styleable.np_ValuePicker_np_text_size, resources.getDimensionPixelSize(R.dimen.np_text_size));

        mTextColorSelected = a.getColor(R.styleable.np_ValuePicker_np_text_color_selected, ContextCompat.getColor(context, R.color.np_text_color_selected));
        mTextSizeSelectedPx = a.getDimensionPixelSize(R.styleable.np_ValuePicker_np_text_size_selected, resources.getDimensionPixelSize(R.dimen.np_text_size_selected));

        mTextFadeColor = a.getBoolean(R.styleable.np_ValuePicker_np_fade_text_color, resources.getBoolean(R.bool.np_def_fade_color));
        mAnimateTextSize = a.getBoolean(R.styleable.np_ValuePicker_np_animate_text_size, resources.getBoolean(R.bool.np_def_animate_text_size));

        a.recycle();

        setMinimumWidth(context.getResources().getDimensionPixelSize(R.dimen.np_min_width));

        mRecyclerView = new RecyclerView(context);
        setupRecyclerView();

        setupViewSize();
        viewRefresh();

        addView(mRecyclerView, recyclerViewLayoutParams());
    }


    //
    // Update views state
    private void setupViewSize() {
        mItemSmallHeight = ValuePickerHelper.getTextViewHeight(getContext(), false, mTextSizePx, mTextSizeSelectedPx);
        mItemBigHeight = ValuePickerHelper.getTextViewHeight(getContext(), true, mTextSizePx, mTextSizeSelectedPx);
    }

    private void viewRefresh() {
        setupViewSize();
        mValuePickerAdapter.notifyDataSetChanged();
    }


    //
    // Inner views View Setup
    private void setupRecyclerView() {
        Context context = mRecyclerView.getContext();

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mAllVerticalScroll = 0;

        mRecyclerView.addOnScrollListener(generateOnScrollListener());
        mValuePickerAdapter = new ValuePickerAdapter(context, getMin(), getMax());
        mRecyclerView.setAdapter(mValuePickerAdapter);
        mValuePickerAdapter.setSelectedIndex(0);
    }


    //
    // Value calc helpers
    private int recyclerViewHeight() {
        return (mItemSmallHeight * (mNumberVisibleItems - 1)) + mItemBigHeight;
    }


    //
    // Layout params generators
    private LayoutParams recyclerViewLayoutParams() {
        LayoutParams recyclerLayoutParams = generateDefaultLayoutParams();
        recyclerLayoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        recyclerLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        recyclerLayoutParams.height = recyclerViewHeight();
        return recyclerLayoutParams;
    }


    //
    // Computed properties
    private int totalNumberOfPaddingItems() {
        return mNumberVisibleItems - 1;
    }

    private int topNumberOfPAddingItems() {
        return (int) Math.floor(totalNumberOfPaddingItems() / 2);
    }

    @NonNull
    private TextView getTextView(Context context) {
        TextView valueTextView = new TextView(context);
        RecyclerView.LayoutParams params = mRecyclerView.getLayoutManager().generateDefaultLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        int textViewPadding = ValuePickerHelper.textViewPadding;
        valueTextView.setPadding(textViewPadding,textViewPadding,textViewPadding,textViewPadding);
        valueTextView.setLayoutParams(params);
        valueTextView.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            valueTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        }
        return valueTextView;
    }

    private float scrollAmountPlusBigItemDiff() {
        return mAllVerticalScroll + smallBigItemSizeDiff();
    }

    private float smallBigItemSizeDiff() {
        return mItemBigHeight - mItemSmallHeight;
    }

    //
    // Custom getters and setters
    public void updateValues(List<String> values) {
        this.mValuePickerAdapter = new ValuePickerAdapter(getContext(), values);
        this.mRecyclerView.setAdapter(this.mValuePickerAdapter);
        this.mValuePickerAdapter.setSelectedIndex(0);
    }

    public String getSelectedValue() {
        return mValuePickerAdapter.getSelectedValue();
    }

    public int getSelectedIndex() {
        return mValuePickerAdapter.selectedItemIndex;
    }


    //
    // Getters and setters to properties
    public OnValueChangeListener getOnValueChangeListener() {
        return mOnValueChangeListener;
    }

    public void setOnValueChangeListener(OnValueChangeListener mOnValueChangeListener) {
        this.mOnValueChangeListener = mOnValueChangeListener;
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
        return mTextSizePx;
    }

    public void setTextSize(int mTextSize) {
        this.mTextSizePx = mTextSize;
        viewRefresh();
    }

    public int getTextSizeSelected() {
        return mTextSizeSelectedPx;
    }

    public void setTextSizeSelected(int mTextSizeSelected) {
        this.mTextSizeSelectedPx = mTextSizeSelected;
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


    //
    // On scroll changes Helpers
    /**
     * Helper
     *
     * @return  Recycler view OnScrollListener to select the center item of RecyclerView
     */
    private RecyclerView.OnScrollListener generateOnScrollListener() {
        return new RecyclerView.OnScrollListener() {
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
        };
    }

    private void calculatePositionAndScroll() {
        int selectedItemScrollPosition = selectedItemForCurrentScroll();
        if (getSelectedIndex() == selectedItemScrollPosition) {
            return;
        }
        scrollListToPosition(selectedItemScrollPosition);
    }

    private int selectedItemForCurrentScroll() {
        int selectedPosition = Math.round(scrollAmountPlusBigItemDiff() / mItemSmallHeight);
        if (getSelectedIndex() == ValuePickerAdapter.POSITION_NONE) {
            selectedPosition = Math.round(mAllVerticalScroll / mItemSmallHeight);
        }
        // Safe check to be a valid position because of rounds
        return Math.min(mValuePickerAdapter.getValuesCount()-1, Math.max(0, selectedPosition));
    }

    private void scrollListToPosition(int selectedItemScrollPosition) {
        float expectedScrollPosition = (selectedItemScrollPosition * mItemSmallHeight);
//        if (getSelectedIndex() != ValuePickerAdapter.POSITION_NONE) {
//            expectedScrollPosition -= smallBigItemSizeDiff()/2;
//        }

        final float missingPxDate = expectedScrollPosition - mAllVerticalScroll;
        if (missingPxDate != 0) {
            mRecyclerView.smoothScrollBy(0, (int) missingPxDate);
        }
        mValuePickerAdapter.setSelectedIndex(selectedItemScrollPosition);
    }


    //
    // Custom Adapter
    /**
     * Recycler view Adapter
     */
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
                return new ItemHolder(getTextView(mContext));
            } else {
                return new PaddingHolder(new View(mContext));
            }
        }

        @Override
        public void onBindViewHolder(ValuePickerAdapter.Holder holder, int position) {

            if (holder instanceof PaddingHolder) {
                RecyclerView.LayoutParams layoutParams = mRecyclerView.getLayoutManager().generateDefaultLayoutParams();
                layoutParams.width = ValuePickerHelper.dp2px(mContext, 1);
                layoutParams.height = mItemSmallHeight;
                ((PaddingHolder) holder).itemView.setLayoutParams(layoutParams);
            }

            if (holder instanceof ItemHolder) {
                final ItemHolder itemHolder = (ItemHolder) holder;
                int adjustedPosition = position - topNumberOfPAddingItems(); // Adjusted position removes the 1st padding

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
                        ValueAnimator textSizeAnimation = ValueAnimator.ofObject(new FloatEvaluator(), mTextSizePx, mTextSizeSelectedPx);
                        textSizeAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                ValuePickerHelper.setTextViewTextSize(itemHolder.number, ((Float) animator.getAnimatedValue()).intValue());
                            }
                        });
                        textSizeAnimation.start();
                    } else {
                        ValuePickerHelper.setTextViewTextSize(itemHolder.number, mTextSizeSelectedPx);
                    }
                    itemHolder.number.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    itemHolder.number.setTypeface(Typeface.DEFAULT);
                    itemHolder.number.setTextColor(mTextColor);
                    ValuePickerHelper.setTextViewTextSize(itemHolder.number, mTextSizePx);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position < topNumberOfPAddingItems() || position >= (getItemCount() - topNumberOfPAddingItems())) {
                return VIEW_TYPE_PADDING;
            }
            return VIEW_TYPE_ITEM;
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
            return  getValuesCount() + totalNumberOfPaddingItems();
        }

        int getValuesCount() {
            return values.size();
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

    //
    // Helpers to Value picker
    private static class ValuePickerHelper {
        public static int textViewPadding = 10;

        private static void setTextViewTextSize(TextView textView, int size) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }

        private static int dp2px(Context context, int dp) {
            return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
        }

        private static int getTextViewHeight(Context context, boolean isSelectedTextView, int textSizePx, int textSizeSelectedPx) {
            TextView textView = new TextView(context);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(params);
            textView.setPadding(textViewPadding,textViewPadding,textViewPadding,textViewPadding);
            textView.setGravity(Gravity.CENTER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                textView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            }
            if (isSelectedTextView) {
                textView.setTypeface(Typeface.DEFAULT_BOLD);
                setTextViewTextSize(textView, textSizeSelectedPx);
            } else {
                textView.setTypeface(Typeface.DEFAULT);
                setTextViewTextSize(textView, textSizePx);
            }
            textView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            return textView.getMeasuredHeight();
        }
    }
}
