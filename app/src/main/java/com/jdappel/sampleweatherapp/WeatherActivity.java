package com.jdappel.sampleweatherapp;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jdappel.android.wunderground.api.APIResponseHandler;
import com.jdappel.android.wunderground.api.impl.WUndergroundTemplate;
import com.jdappel.android.wunderground.model.api.CurrentObservation;

public class WeatherActivity extends FragmentActivity implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    /**
     * Request code for location permission request.
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean permissionDenied = false;
    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private LocationRequest locationRequest;
    private final WUndergroundTemplate weatherAPI = new WUndergroundTemplate();

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMapClickListener(this);
        map.setOnMapLongClickListener(this);
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setScrollGesturesEnabled(true);
        hasPermissions();
    }

    private Location getDefaultLocation() {
        Location loc = new Location("");
        loc.setLatitude(39.9612);
        loc.setLongitude(-82.9988);
        return loc;
    }

    private void hasPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    protected void startLocationUpdates() {

        Location updatedLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (updatedLocation != null) {
            lastLocation = updatedLocation;
            LatLng sydney = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            map.addMarker(new MarkerOptions().position(sydney).title("Local Position"));
            map.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        }
    }

    @Override
    public void onMapClick(final LatLng point) {
        Observable.create(new Observable.OnSubscribe<APIResponseHandler<CurrentObservation>>() {
            @Override
            public void call(Subscriber<? super APIResponseHandler<CurrentObservation>> subscriber) {
                try {
                    subscriber.onNext(weatherAPI.getConditionsAPI().getCurrentObservationByLatLong(
                            point.latitude,
                            point.longitude));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<APIResponseHandler<CurrentObservation>>() {
                    @Override
                    public void call(APIResponseHandler<CurrentObservation> obs) {
                        Marker position = map.addMarker(new MarkerOptions()
                                .position(point)
                                .title(obs.getModelData().getCurrentWeather() + " " + obs.getModelData().getCurrentTemperature()));
                        position.showInfoWindow();
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(point)
                                .zoom(3.0f)
                                .bearing(0)
                                .build();
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        System.out.println("Success = " + obs.getModelData().getCurrentWeather());
                        System.out.println("Lat = " + point.latitude + " Long = " + point.longitude);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        System.out.println("Error");
                    }
                });
    }

    @Override
    public void onMapLongClick(LatLng point) {

    }

}
