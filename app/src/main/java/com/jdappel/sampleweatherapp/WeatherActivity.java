package com.jdappel.sampleweatherapp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.RuntimeRemoteException;
import com.jdappel.android.wunderground.api.APIResponseHandler;
import com.jdappel.android.wunderground.api.impl.WUndergroundTemplate;
import com.jdappel.android.wunderground.model.api.CurrentObservation;
import com.jdappel.android.wunderground.model.api.Forecast;
import com.jdappel.android.wunderground.model.api.TextForecastDetail;

/**
 * Activity class that handles the user interactions for this sample weather application
 * and delegates API calls to the WUndergroundAPILib.
 *
 * @author jappel
 */
public class WeatherActivity extends FragmentActivity implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    /**
     * Request code for location permission request.
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private ArrayAdapter<String> listAdapter;
    private Marker currentMarker;
    private final WUndergroundTemplate weatherAPI = new WUndergroundTemplate();

    @Bind(R.id.threeDayForecastListView)
    ListView multiDayForecastListView;

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
        ButterKnife.bind(this);

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        listAdapter = new ArrayAdapter<String>(this, R.layout.forecast_item_layout, new ArrayList<String>());
        multiDayForecastListView.setAdapter(listAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    /**
     * Convenience method for initiating map settings
     */
    private void initiateMap() {
        map.setOnMapClickListener(this);
        map.setOnMapLongClickListener(this);
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setScrollGesturesEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);
        uiSettings.setTiltGesturesEnabled(true);
        uiSettings.setScrollGesturesEnabled(true);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        initiateMap();
        hasPermissions();
    }

    /**
     * Verify that user has granted permission to access location. If not, request permission.
     */
    private void hasPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
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
        Log.e(getClass().getName(), "Connection suspended to Google API Client");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(getClass().getName(), "Connection failed: " + connectionResult.toString());
    }

    /**
     * Method to handle identification of user's location upon application startup and return
     * API Data germane to that location.  If the user's location isn't available, a default location
     * is used.  Note location updates are removed if the initial location was found.
     */
    private void startLocationUpdates() {

        LocationRequest request = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(1000);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, this);
        Location updatedLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (updatedLocation != null) {
            lastLocation = updatedLocation;
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        } else {
            Location defaultLocation = new Location("");
            defaultLocation.setLatitude(39.9682);
            defaultLocation.setLongitude(-82.998);
            lastLocation = defaultLocation;
        }
        processAPIRequest(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
    }

    /**
     * Simple value class to handle the merging of the two API calls during processing
     *
     * @param <CurrentObservation>
     * @param <Forecast>
     */
    class Pair<CurrentObservation, Forecast> {

        private final CurrentObservation currentObservation;
        private final Forecast forecast;

        Pair(CurrentObservation element0, Forecast element1) {
            this.currentObservation = element0;
            this.forecast = element1;
        }

        public CurrentObservation getCurrentObservation() {
            return currentObservation;
        }

        public Forecast getForecast() {
            return forecast;
        }

    }

    @Override
    public void onLocationChanged(Location loc) {
        lastLocation = loc;
        processAPIRequest(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
    }

    /**
     * Handles the processing of the two API calls and updates the view components accordingly.
     *
     * @param point
     */
    private void processAPIRequest(final LatLng point) {
        Callable<APIResponseHandler<CurrentObservation>> conditionsCallable = new Callable<APIResponseHandler<CurrentObservation>>() {
            @Override
            public APIResponseHandler<CurrentObservation> call() throws Exception {
                return weatherAPI.getConditionsAPI().getCurrentObservationByLatLong(
                        point.latitude,
                        point.longitude);
            }
        };
        Callable<APIResponseHandler<Forecast>> forecastCallable = new Callable<APIResponseHandler<Forecast>>() {
            @Override
            public APIResponseHandler<Forecast> call() throws Exception {
                return weatherAPI.getForecastAPI().getForecastByLatLong(
                        point.latitude,
                        point.longitude);
            }
        };
        Observable.zip(ObservableUtils.createObservable(conditionsCallable), ObservableUtils.createObservable(forecastCallable), new Func2<APIResponseHandler<CurrentObservation>, APIResponseHandler<Forecast>, Pair<CurrentObservation, Forecast>>() {
            @Override
            public Pair<CurrentObservation,Forecast> call(APIResponseHandler<CurrentObservation> currentObservationAPIResponseHandler, APIResponseHandler<Forecast> forecastAPIResponseHandler) {
                if (!currentObservationAPIResponseHandler.hasError() && !forecastAPIResponseHandler.hasError())
                    return new Pair(currentObservationAPIResponseHandler.getModelData(), forecastAPIResponseHandler.getModelData());
                else {
                    Log.e(getClass().getName(), "Error zipping API responses");
                }
                throw new RuntimeException("Error returning data from API");
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Pair<CurrentObservation, Forecast>>() {
                    @Override
                    public void call(final Pair<CurrentObservation, Forecast> obs) {
                        updateMapAndCamera(obs.getCurrentObservation(), point);
                        updateListAdapter(obs.getForecast());
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(getClass().getName(), throwable.getMessage());
                    }
                });
    }

    /**
     * Handles updating the map marker with the current location's weather data and
     * center the camera
     * @param conditions
     * @param point
     */
    private void updateMapAndCamera(CurrentObservation conditions, LatLng point) {
        if (currentMarker != null) {
            currentMarker.remove();
        }
        currentMarker = map.addMarker(new MarkerOptions()
                .position(point)
                .title(conditions.getLocation().getCity() + " " + conditions.getLocation().getState() + " " + conditions.getLocation().getCountry())
                .snippet(conditions.getCurrentWeather() + " " + conditions.getCurrentTemperature()));
        currentMarker.showInfoWindow();
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(point)
                .zoom(3.0f)
                .bearing(0)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * Updates the views list adapter when a new forecast is returned
     * @param forecast
     */
    private void updateListAdapter(Forecast forecast) {
        listAdapter.notifyDataSetChanged();
        listAdapter.clear();
        listAdapter.addAll(populateForecastList(forecast));
    }

    /**
     * Populates the listview with the forecast details for the current location.
     * @param currentForecast
     * @return {@code List<String>}
     */
    private List<String> populateForecastList(com.jdappel.android.wunderground.model.api.Forecast currentForecast) {
        List<String> items = new ArrayList<>(currentForecast.getTextForecast().getForecastList().size());
        for (TextForecastDetail detail : currentForecast.getTextForecast().getForecastList()) {
            items.add(detail.getTitle() + " - " + detail.getText());
        }
        return items;
    }

    @Override
    public void onMapClick(final LatLng point) {
        processAPIRequest(point);
    }

    @Override
    public void onMapLongClick(LatLng point) {
        processAPIRequest(point);
    }
}
