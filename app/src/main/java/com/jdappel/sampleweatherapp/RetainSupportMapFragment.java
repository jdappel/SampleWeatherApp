package com.jdappel.sampleweatherapp;

import android.os.Bundle;

import com.google.android.gms.maps.SupportMapFragment;

/**
 * Extended SupportMapFragment implementation for maintaining the map's state
 * during runtime changes
 * 
 * @author jappel
 */
public class RetainSupportMapFragment extends SupportMapFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
