package com.jdappel.sampleweatherapp;

import android.databinding.ObservableField;

public class ForecastHeader {

    private final ObservableField<String> title = new ObservableField<>();

    public ObservableField<String> getTitle() {
        return title;
    }
}
