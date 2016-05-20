package com.jdappel.sampleweatherapp;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jdappel.sampleweatherapp.databinding.ForecastItemLayoutBinding;

import java.util.List;

public class ForecastListAdapter extends RecyclerView.Adapter<ForecastListAdapter.ViewHolder> {
    private List<ForecastItem> dataList;
    private final LayoutInflater inflater;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ForecastItemLayoutBinding binding;
        public ViewHolder(ForecastItemLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            ForecastItemLayoutBinding binding = ForecastItemLayoutBinding
                    .inflate(inflater, parent, false);
            return new ViewHolder(binding);
        }

        public void bindTo(ForecastItem item) {
            binding.setForecastItem(item);
            binding.executePendingBindings();
        }
    }

    ForecastListAdapter(List<ForecastItem> myDataset, LayoutInflater inflater) {
        dataList = myDataset;
        this.inflater = inflater;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ForecastListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        return ViewHolder.create(inflater, parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindTo(dataList.get(position));

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    void updateList(List<ForecastItem> list) {
        dataList = list;
        notifyDataSetChanged();
    }
}


