package com.jdappel.sampleweatherapp;

import android.databinding.ObservableField;

/**
 * Created by o654402 on 5/20/16.
 */
public class ForecastHeader {

    private final ObservableField<String> title = new ObservableField<>();

    public ObservableField<String> getTitle() {
        return title;
    }
}
