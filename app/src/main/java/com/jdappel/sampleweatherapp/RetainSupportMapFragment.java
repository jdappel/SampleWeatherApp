package com.jdappel.sampleweatherapp;

import android.os.Bundle;

import com.google.android.gms.maps.SupportMapFragment;

/**
 * Created by jappel on 4/2/2016.
 */
public class RetainSupportMapFragment extends SupportMapFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
