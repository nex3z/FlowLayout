package com.nex3z.flowlayout.sample;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.ViewHolder> {

    private int mCount = 0;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View contactView = inflater.inflate(R.layout.item, parent, false);

        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mTvCount.setText(String.valueOf(position));
    }

    @Override
    public int getItemCount() {
        return mCount;
    }

    public void setCount(int count) {
        mCount = count;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTvCount;
        public ViewHolder(final View itemView) {
            super(itemView);
            mTvCount = (TextView) itemView.findViewById(R.id.tv_count);
        }
    }
}
