package com.nex3z.flowlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FlowLayout extends ViewGroup {
    private static final String LOG_TAG = FlowLayout.class.getSimpleName();

    /**
     * Special value for the child view spacing.
     * SPACING_AUTO means that the actual spacing is calculated according to the size of the
     * container and the number of the child views, so that the child views are placed evenly in
     * the container.
     */
    public static final int SPACING_AUTO = -65536;

    /**
     * Special value for the horizontal spacing of the child views in the last row
     * SPACING_ALIGN means that the horizontal spacing of the child views in the last row keeps
     * the same with the spacing used in the row above. If there is only one row, this value is
     * ignored and the spacing will be calculated according to childSpacing.
     */
    public static final int SPACING_ALIGN = -65537;

    private static final int SPACING_UNDEFINED = -65538;

    private static final int UNSPECIFIED_GRAVITY = -1;

    private static final int ROW_VERTICAL_GRAVITY_AUTO = -65536;

    private static final boolean DEFAULT_FLOW = true;
    private static final int DEFAULT_CHILD_SPACING = 0;
    private static final int DEFAULT_CHILD_SPACING_FOR_LAST_ROW = SPACING_UNDEFINED;
    private static final float DEFAULT_ROW_SPACING = 0;
    private static final boolean DEFAULT_RTL = false;
    private static final int DEFAULT_MAX_ROWS = Integer.MAX_VALUE;
    public static final String SHOW_MORE_BUTTON_TAG = "show_more_button_tag";

    private boolean mFlow = DEFAULT_FLOW;
    private int mChildSpacing = DEFAULT_CHILD_SPACING;
    private int mMinChildSpacing = DEFAULT_CHILD_SPACING;
    private int mChildSpacingForLastRow = DEFAULT_CHILD_SPACING_FOR_LAST_ROW;
    private float mRowSpacing = DEFAULT_ROW_SPACING;
    private float mAdjustedRowSpacing = DEFAULT_ROW_SPACING;
    private boolean mRtl = DEFAULT_RTL;
    private int mMaxRows = DEFAULT_MAX_ROWS;
    private int mGravity = UNSPECIFIED_GRAVITY;
    private int mRowVerticalGravity = ROW_VERTICAL_GRAVITY_AUTO;
    private int mExactMeasuredHeight;
    private int showMoreButtonIndex = 0;

    private List<Float> mHorizontalSpacingForRow = new ArrayList<>();
    private List<Integer> mHeightForRow = new ArrayList<>();
    private List<Integer> mWidthForRow = new ArrayList<>();
    private List<Integer> mChildNumForRow = new ArrayList<>();
    private List<View> removedViews = new ArrayList<>();

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.FlowLayout, 0, 0);
        try {
            mFlow = a.getBoolean(R.styleable.FlowLayout_flFlow, DEFAULT_FLOW);
            try {
                mChildSpacing = a.getInt(R.styleable.FlowLayout_flChildSpacing, DEFAULT_CHILD_SPACING);
            } catch (NumberFormatException e) {
                mChildSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_flChildSpacing, (int) dpToPx(DEFAULT_CHILD_SPACING));
            }
            try {
                mMinChildSpacing = a.getInt(R.styleable.FlowLayout_flMinChildSpacing, DEFAULT_CHILD_SPACING);
            } catch (NumberFormatException e) {
                mMinChildSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_flMinChildSpacing, (int) dpToPx(DEFAULT_CHILD_SPACING));
            }
            try {
                mChildSpacingForLastRow = a.getInt(R.styleable.FlowLayout_flChildSpacingForLastRow, SPACING_UNDEFINED);
            } catch (NumberFormatException e) {
                mChildSpacingForLastRow = a.getDimensionPixelSize(R.styleable.FlowLayout_flChildSpacingForLastRow, (int) dpToPx(DEFAULT_CHILD_SPACING));
            }
            try {
                mRowSpacing = a.getInt(R.styleable.FlowLayout_flRowSpacing, 0);
            } catch (NumberFormatException e) {
                mRowSpacing = a.getDimension(R.styleable.FlowLayout_flRowSpacing, dpToPx(DEFAULT_ROW_SPACING));
            }
            mMaxRows = a.getInt(R.styleable.FlowLayout_flMaxRows, DEFAULT_MAX_ROWS);
            mRtl = a.getBoolean(R.styleable.FlowLayout_flRtl, DEFAULT_RTL);
            mGravity = a.getInt(R.styleable.FlowLayout_android_gravity, UNSPECIFIED_GRAVITY);
            mRowVerticalGravity = a.getInt(R.styleable.FlowLayout_flRowVerticalGravity, ROW_VERTICAL_GRAVITY_AUTO);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        mHorizontalSpacingForRow.clear();
        mHeightForRow.clear();
        mWidthForRow.clear();
        mChildNumForRow.clear();

        TextView childShowMoreBtn = null;
        // If the child view at showMoreButtonIndex is the +X button, we remove it and store its
        // reference
        if (this.getChildAt(showMoreButtonIndex) != null &&
                this.getChildAt(showMoreButtonIndex).getTag() != null &&
                this.getChildAt(showMoreButtonIndex).getTag().toString().equals(SHOW_MORE_BUTTON_TAG)) {
            childShowMoreBtn = (TextView) this.getChildAt(showMoreButtonIndex);
            this.removeViewAt(showMoreButtonIndex);
        }

        int measuredHeight = 0, measuredWidth = 0, childCount = getChildCount();
        int rowWidth = 0, maxChildHeightInRow = 0, childNumInRow = 0;
        final int rowSize = widthSize - getPaddingLeft() - getPaddingRight();
        int rowTotalChildWidth = 0;
        final boolean allowFlow = widthMode != MeasureSpec.UNSPECIFIED && mFlow;
        final int childSpacing = mChildSpacing == SPACING_AUTO && widthMode == MeasureSpec.UNSPECIFIED
                ? 0 : mChildSpacing;
        final float tmpSpacing = childSpacing == SPACING_AUTO ? mMinChildSpacing : childSpacing;
        boolean showMoreButtonAdded = false;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            // For Error handling: in the case that i exceeds childCount and returns a null View
            if (child == null) {
                break;
            }

            if (child.getVisibility() == GONE) {
                continue;
            }

            LayoutParams childParams = child.getLayoutParams();
            int horizontalMargin = 0, verticalMargin = 0;
            if (childParams instanceof MarginLayoutParams) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, measuredHeight);
                MarginLayoutParams marginParams = (MarginLayoutParams) childParams;
                horizontalMargin = marginParams.leftMargin + marginParams.rightMargin;
                verticalMargin = marginParams.topMargin + marginParams.bottomMargin;
            } else {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }

            int childWidth = child.getMeasuredWidth() + horizontalMargin;
            int childHeight = child.getMeasuredHeight() + verticalMargin;
            if (allowFlow && rowWidth + childWidth > rowSize) { // Need flow to next row
                // Used to store the margin for +X button
                int horizontalMarginForBtn = 0;
                int verticalMarginForBtn = 0;

                if (childShowMoreBtn != null) {
                    // Check if the max row limit is reached (i.e. +X button needs to be added)
                    if (mChildNumForRow.size() == getMaxRows() - 1) {
                        // Set the original remaining number of children for +X button
                        childShowMoreBtn.setText(getRemainingChildrenCountString((childCount - getCurrentChildrenCount() - childNumInRow)));
                        // Calculate the width of the +X button
                        LayoutParams childParamsForBtnAtLastRow = childShowMoreBtn.getLayoutParams();
                        if (childParamsForBtnAtLastRow instanceof MarginLayoutParams) {
                            measureChildWithMargins(childShowMoreBtn, widthMeasureSpec, 0, heightMeasureSpec, measuredHeight);
                            MarginLayoutParams marginParamsForBtnAtLastRow = (MarginLayoutParams) childParamsForBtnAtLastRow;
                            horizontalMarginForBtn = marginParamsForBtnAtLastRow.leftMargin + marginParamsForBtnAtLastRow.rightMargin;
                            verticalMarginForBtn = marginParamsForBtnAtLastRow.topMargin + marginParamsForBtnAtLastRow.bottomMargin;
                        } else {
                            measureChild(childShowMoreBtn, widthMeasureSpec, heightMeasureSpec);
                        }

                        // Calculate width and height of the +X button, including the margins
                        int childShowMoreBtnWidthAtLastRow = childShowMoreBtn.getMeasuredWidth() + horizontalMarginForBtn;
                        int childShowMoreBtnHeightAtLastRow = childShowMoreBtn.getMeasuredHeight() + verticalMarginForBtn;

                        int swapCount = 0;
                        int originalChildNumInRow = childNumInRow;
                        removedViews.clear();

                        // If the maximum width of the row is exceeded with the current children of
                        // the last row plus the +X button, then we need to remove another child and
                        // repeat the process. Since the remaining number of children has changed
                        // within the +X button, the +X button needs to be remeasured. We need to
                        // repeat this process until enough children are moved for the +X button
                        // to fit within the last (i.e. max) row
                        while (rowWidth + childShowMoreBtnWidthAtLastRow > rowSize) {
                            // Get the last element of the last row
                            View temp = this.getChildAt(getCurrentChildrenCount() + childNumInRow - 1);
                            // Insert last element into first position to maintain ordering
                            removedViews.add(0, temp);
                            // Remove last element of the last row
                            this.removeViewAt(getCurrentChildrenCount() + childNumInRow - 1);
                            // Increment the number of views removed
                            swapCount++;
                            // Decrement the number of visible children left in the row
                            childNumInRow--;
                            // Remeasure the row width without the newly removed element, including spacing
                            rowWidth -= temp.getMeasuredWidth() + horizontalMargin + tmpSpacing;
                            // Remeasure the row width without the newly removed element, without spacing
                            rowTotalChildWidth -= temp.getMeasuredWidth() + horizontalMargin;

                            // Since the remaining children count has changed, we need to set the
                            // text again and remeasure the +X button
                            childShowMoreBtn.setText(getRemainingChildrenCountString((childCount - getCurrentChildrenCount() - originalChildNumInRow + swapCount)));
                            // Calculate the width of the +X button with the new children count
                            childParamsForBtnAtLastRow = childShowMoreBtn.getLayoutParams();
                            if (childParamsForBtnAtLastRow instanceof MarginLayoutParams) {
                                measureChildWithMargins(childShowMoreBtn, widthMeasureSpec, 0, heightMeasureSpec, measuredHeight);
                                MarginLayoutParams marginParamsForBtnAtLastRow = (MarginLayoutParams) childParamsForBtnAtLastRow;
                                horizontalMarginForBtn = marginParamsForBtnAtLastRow.leftMargin + marginParamsForBtnAtLastRow.rightMargin;
                                verticalMarginForBtn = marginParamsForBtnAtLastRow.topMargin + marginParamsForBtnAtLastRow.bottomMargin;
                            } else {
                                measureChild(childShowMoreBtn, widthMeasureSpec, heightMeasureSpec);
                            }

                            // Calculate width and height of the +X button with the new children
                            // count, including the margins
                            childShowMoreBtnWidthAtLastRow = childShowMoreBtn.getMeasuredWidth() + horizontalMarginForBtn;
                            childShowMoreBtnHeightAtLastRow = childShowMoreBtn.getMeasuredHeight() + verticalMarginForBtn;
                        }

                        // At this point, enough children views have been removed to fit the +X
                        // button within the last row. Now, add the +X button
                        this.addView(childShowMoreBtn, getCurrentChildrenCount() + childNumInRow);

                        // Update the flag for +X button
                        showMoreButtonAdded = true;

                        // Update the index position of the +X button so that it can be found on
                        // subsequent onMeasures (e.g. when the device is rotated)
                        showMoreButtonIndex = getCurrentChildrenCount() + childNumInRow;

                        // Update the row width with the newly added +X button, including spacing
                        rowWidth += childShowMoreBtnWidthAtLastRow + tmpSpacing;

                        // Update the row width with the newly added +X button, without spacing
                        rowTotalChildWidth += childShowMoreBtnWidthAtLastRow;
                        // Update the number of children views in the last row, including the +X
                        // button
                        childNumInRow++;
                        // Update the index i to the index of the +X button so that it can
                        // iterate to the element immediately proceeding the +X button
                        i = getCurrentChildrenCount() + childNumInRow - 1;

                        maxChildHeightInRow = Math.max(maxChildHeightInRow, childShowMoreBtnHeightAtLastRow);
                        // Add back the views that were temporarily removed. When the max rows are
                        // rendered, the children views past the +X button are merely hidden from
                        // view
                        for (int index = 0; index < removedViews.size(); index++) {
                            this.addView(removedViews.get(index), getCurrentChildrenCount() + childNumInRow + index);
                        }
                        // Update the total children count, including the +X button
                        childCount++;
                    }
                }

                // Save parameters for current row
                mHorizontalSpacingForRow.add(
                        getSpacingForRow(childSpacing, rowSize, rowTotalChildWidth, childNumInRow));
                mChildNumForRow.add(childNumInRow);
                mHeightForRow.add(maxChildHeightInRow);
                mWidthForRow.add(rowWidth - (int) tmpSpacing);
                if (mHorizontalSpacingForRow.size() <= mMaxRows) {
                    measuredHeight += maxChildHeightInRow;
                }
                measuredWidth = Math.max(measuredWidth, rowWidth);

                if (showMoreButtonAdded) {
                    // When the +X button is added, we will begin a new row beginning with the
                    // element immediately proceeding the +X button so we need to reinitialize the
                    // elements
                    childNumInRow = 0;
                    rowWidth = 0;
                    rowTotalChildWidth = 0;
                    maxChildHeightInRow = 0;
                    showMoreButtonAdded = false;
                } else {
                    // Place the child view to next row
                    childNumInRow = 1;
                    rowWidth = childWidth + (int) tmpSpacing;
                    rowTotalChildWidth = childWidth;
                    maxChildHeightInRow = childHeight;
                }
            } else {
                childNumInRow++;
                rowWidth += childWidth + tmpSpacing;
                rowTotalChildWidth += childWidth;
                maxChildHeightInRow = Math.max(maxChildHeightInRow, childHeight);
            }
        }

        // Measure remaining child views in the last row
        if (mChildSpacingForLastRow == SPACING_ALIGN) {
            // For SPACING_ALIGN, use the same spacing from the row above if there is more than one
            // row.
            if (mHorizontalSpacingForRow.size() >= 1) {
                mHorizontalSpacingForRow.add(
                        mHorizontalSpacingForRow.get(mHorizontalSpacingForRow.size() - 1));
            } else {
                mHorizontalSpacingForRow.add(
                        getSpacingForRow(childSpacing, rowSize, rowTotalChildWidth, childNumInRow));
            }
        } else if (mChildSpacingForLastRow != SPACING_UNDEFINED) {
            // For SPACING_AUTO and specific DP values, apply them to the spacing strategy.
            mHorizontalSpacingForRow.add(
                    getSpacingForRow(mChildSpacingForLastRow, rowSize, rowTotalChildWidth, childNumInRow));
        } else {
            // For SPACING_UNDEFINED, apply childSpacing to the spacing strategy for the last row.
            mHorizontalSpacingForRow.add(
                    getSpacingForRow(childSpacing, rowSize, rowTotalChildWidth, childNumInRow));
        }

        mChildNumForRow.add(childNumInRow);
        mHeightForRow.add(maxChildHeightInRow);
        mWidthForRow.add(rowWidth - (int) tmpSpacing);
        if (mHorizontalSpacingForRow.size() <= mMaxRows) {
            measuredHeight += maxChildHeightInRow;
        }
        measuredWidth = Math.max(measuredWidth, rowWidth);

        if (childSpacing == SPACING_AUTO) {
            measuredWidth = widthSize;
        } else if (widthMode == MeasureSpec.UNSPECIFIED) {
            measuredWidth = measuredWidth + getPaddingLeft() + getPaddingRight();
        } else {
            measuredWidth = Math.min(measuredWidth + getPaddingLeft() + getPaddingRight(), widthSize);
        }

        measuredHeight += getPaddingTop() + getPaddingBottom();
        int rowNum = Math.min(mHorizontalSpacingForRow.size(), mMaxRows);
        float rowSpacing = mRowSpacing == SPACING_AUTO && heightMode == MeasureSpec.UNSPECIFIED
                ? 0 : mRowSpacing;
        if (rowSpacing == SPACING_AUTO) {
            if (rowNum > 1) {
                mAdjustedRowSpacing = (heightSize - measuredHeight) / (rowNum - 1);
            } else {
                mAdjustedRowSpacing = 0;
            }
            measuredHeight = heightSize;
        } else {
            mAdjustedRowSpacing = rowSpacing;
            if (rowNum > 1) {
                measuredHeight = heightMode == MeasureSpec.UNSPECIFIED
                        ? ((int) (measuredHeight + mAdjustedRowSpacing * (rowNum - 1)))
                        : (Math.min((int) (measuredHeight + mAdjustedRowSpacing * (rowNum - 1)),
                        heightSize));
            }
        }

        mExactMeasuredHeight = measuredHeight;

        measuredWidth = widthMode == MeasureSpec.EXACTLY ? widthSize : measuredWidth;
        measuredHeight = heightMode == MeasureSpec.EXACTLY ? heightSize : measuredHeight;

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private String getRemainingChildrenCountString(int remainingChildrenCount) {
        return "+" + remainingChildrenCount;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft(), paddingRight = getPaddingRight(),
                paddingTop = getPaddingTop(), paddingBottom = getPaddingBottom();

        int x = mRtl ? (getWidth() - paddingRight) : paddingLeft;
        int y = paddingTop;

        int verticalGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        int horizontalGravity = mGravity & Gravity.HORIZONTAL_GRAVITY_MASK;

        switch (verticalGravity) {
            case Gravity.CENTER_VERTICAL: {
                int offset = (b - t - paddingTop - paddingBottom - mExactMeasuredHeight) / 2;
                y += offset;
                break;
            }
            case Gravity.BOTTOM: {
                int offset = b - t - paddingTop - paddingBottom - mExactMeasuredHeight;
                y += offset;
                break;
            }
            default:
                break;
        }

        int horizontalPadding = paddingLeft + paddingRight, layoutWidth = r - l;
        x += getHorizontalGravityOffsetForRow(horizontalGravity, layoutWidth, horizontalPadding, 0);

        int verticalRowGravity = mRowVerticalGravity & Gravity.VERTICAL_GRAVITY_MASK;

        int rowCount = mChildNumForRow.size(), childIdx = 0;
        for (int row = 0; row < rowCount; row++) {
            int childNum = mChildNumForRow.get(row);
            int rowHeight = mHeightForRow.get(row);
            float spacing = mHorizontalSpacingForRow.get(row);
            for (int i = 0; i < childNum && childIdx < getChildCount(); ) {
                View child = getChildAt(childIdx++);
                if (child.getVisibility() == GONE) {
                    continue;
                } else {
                    i++;
                }

                LayoutParams childParams = child.getLayoutParams();
                int marginLeft = 0, marginTop = 0, marginBottom = 0, marginRight = 0;
                if (childParams instanceof MarginLayoutParams) {
                    MarginLayoutParams marginParams = (MarginLayoutParams) childParams;
                    marginLeft = marginParams.leftMargin;
                    marginRight = marginParams.rightMargin;
                    marginTop = marginParams.topMargin;
                    marginBottom = marginParams.bottomMargin;
                }

                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                int tt = y + marginTop;
                if (verticalRowGravity == Gravity.BOTTOM) {
                    tt = y + rowHeight - marginBottom - childHeight;
                } else if (verticalRowGravity == Gravity.CENTER_VERTICAL) {
                    tt = y + marginTop + (rowHeight - marginTop - marginBottom - childHeight) / 2;
                }
                int bb = tt + childHeight;
                if (mRtl) {
                    int l1 = x - marginRight - childWidth;
                    int r1 = x - marginRight;
                    child.layout(l1, tt, r1, bb);
                    x -= childWidth + spacing + marginLeft + marginRight;
                } else {
                    int l2 = x + marginLeft;
                    int r2 = x + marginLeft + childWidth;
                    child.layout(l2, tt, r2, bb);
                    x += childWidth + spacing + marginLeft + marginRight;
                }
            }
            x = mRtl ? (getWidth() - paddingRight) : paddingLeft;
            x += getHorizontalGravityOffsetForRow(
                    horizontalGravity, layoutWidth, horizontalPadding, row + 1);
            y += rowHeight + mAdjustedRowSpacing;
        }
    }

    public int getVisbleChildrenCount() {
        int count = 0;
        for(int i=0; i<Math.min(getRowsCount(), getMaxRows()); i++) {
            count += mChildNumForRow.get(i);
        }
        return count;
    }

    private int getCurrentChildrenCount() {
        int count = 0;
        for(int eachCount: mChildNumForRow)
            count += eachCount;
        return count;
    }

    private int getHorizontalGravityOffsetForRow(int horizontalGravity, int parentWidth, int horizontalPadding, int row) {
        if (mChildSpacing == SPACING_AUTO || row >= mWidthForRow.size()
                || row >= mChildNumForRow.size() || mChildNumForRow.get(row) <= 0) {
            return 0;
        }

        int offset = 0;
        switch (horizontalGravity) {
            case Gravity.CENTER_HORIZONTAL:
                offset = (parentWidth - horizontalPadding - mWidthForRow.get(row)) / 2;
                break;
            case Gravity.RIGHT:
                offset = parentWidth - horizontalPadding - mWidthForRow.get(row);
                break;
            default:
                break;
        }
        return offset;
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    /**
     * Returns whether to allow child views flow to next row when there is no enough space.
     *
     * @return Whether to flow child views to next row when there is no enough space.
     */
    public boolean isFlow() {
        return mFlow;
    }

    /**
     * Sets whether to allow child views flow to next row when there is no enough space.
     *
     * @param flow true to allow flow. false to restrict all child views in one row.
     */
    public void setFlow(boolean flow) {
        mFlow = flow;
        requestLayout();
    }

    /**
     * Returns the horizontal spacing between child views.
     *
     * @return The spacing, either {@link FlowLayout#SPACING_AUTO}, or a fixed size in pixels.
     */
    public int getChildSpacing() {
        return mChildSpacing;
    }

    /**
     * Sets the horizontal spacing between child views.
     *
     * @param childSpacing The spacing, either {@link FlowLayout#SPACING_AUTO}, or a fixed size in
     *                     pixels.
     */
    public void setChildSpacing(int childSpacing) {
        mChildSpacing = childSpacing;
        requestLayout();
    }

    /**
     * Returns the horizontal spacing between child views of the last row.
     *
     * @return The spacing, either {@link FlowLayout#SPACING_AUTO},
     * {@link FlowLayout#SPACING_ALIGN}, or a fixed size in pixels
     */
    public int getChildSpacingForLastRow() {
        return mChildSpacingForLastRow;
    }

    /**
     * Sets the horizontal spacing between child views of the last row.
     *
     * @param childSpacingForLastRow The spacing, either {@link FlowLayout#SPACING_AUTO},
     *                               {@link FlowLayout#SPACING_ALIGN}, or a fixed size in pixels
     */
    public void setChildSpacingForLastRow(int childSpacingForLastRow) {
        mChildSpacingForLastRow = childSpacingForLastRow;
        requestLayout();
    }

    /**
     * Returns the vertical spacing between rows.
     *
     * @return The spacing, either {@link FlowLayout#SPACING_AUTO}, or a fixed size in pixels.
     */
    public float getRowSpacing() {
        return mRowSpacing;
    }

    /**
     * Sets the vertical spacing between rows in pixels. Use SPACING_AUTO to evenly place all rows
     * in vertical.
     *
     * @param rowSpacing The spacing, either {@link FlowLayout#SPACING_AUTO}, or a fixed size in
     *                   pixels.
     */
    public void setRowSpacing(float rowSpacing) {
        mRowSpacing = rowSpacing;
        requestLayout();
    }

    /**
     * Returns the maximum number of rows of the FlowLayout.
     *
     * @return The maximum number of rows.
     */
    public int getMaxRows() {
        return mMaxRows;
    }

    /**
     * Sets the height of the FlowLayout to be at most maxRows tall.
     *
     * @param maxRows The maximum number of rows.
     */
    public void setMaxRows(int maxRows) {
        mMaxRows = maxRows;
        requestLayout();
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            requestLayout();
        }
    }

    public void setRowVerticalGravity(int rowVerticalGravity) {
        if (mRowVerticalGravity != rowVerticalGravity) {
            mRowVerticalGravity = rowVerticalGravity;
            requestLayout();
        }
    }

    public boolean isRtl() {
        return mRtl;
    }

    public void setRtl(boolean rtl) {
        mRtl = rtl;
        requestLayout();
    }

    public int getMinChildSpacing() {
        return mMinChildSpacing;
    }

    public void setMinChildSpacing(int minChildSpacing) {
        this.mMinChildSpacing = minChildSpacing;
        requestLayout();
    }

    public int getRowsCount() {
        return mChildNumForRow.size();
    }

    private float getSpacingForRow(int spacingAttribute, int rowSize, int usedSize, int childNum) {
        float spacing;
        if (spacingAttribute == SPACING_AUTO) {
            if (childNum > 1) {
                spacing = (rowSize - usedSize) / (childNum - 1);
            } else {
                spacing = 0;
            }
        } else {
            spacing = spacingAttribute;
        }
        return spacing;
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
