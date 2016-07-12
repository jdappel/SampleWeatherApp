package com.jdappel.sampleweatherapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

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
import com.jdappel.android.wunderground.api.impl.WUndergroundTemplate;
import com.jdappel.android.wunderground.model.api.CurrentObservation;
import com.jdappel.android.wunderground.model.api.Forecast;
import com.jdappel.android.wunderground.model.api.TextForecastDetail;
import com.jdappel.sampleweatherapp.databinding.ActivityWeatherBinding;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * Activity class that handles the user interactions for this sample weather application and delegates API calls to the
 * WUndergroundAPILib.
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
    private ForecastListAdapter listAdapter;
    private Marker currentMarker;
    private final WUndergroundTemplate weatherAPI = new WUndergroundTemplate();
    private ActivityWeatherBinding binding;
    private final ForecastHeader header = new ForecastHeader();

    @Bind(R.id.threeDayForecastListView)
    RecyclerView multiDayForecastListView;

    private RecyclerView.LayoutManager listLayoutManager;

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_weather);
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

        listLayoutManager = new LinearLayoutManager(this);
        multiDayForecastListView.setHasFixedSize(true);
        multiDayForecastListView.setLayoutManager(listLayoutManager);
        listAdapter = new ForecastListAdapter(new ArrayList<ForecastItem>(), getLayoutInflater());
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
                                              new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
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
     * Method to handle identification of user's location upon application startup and return API Data germane to that
     * location.  If the user's location isn't available, a default location is used.  Note location updates are removed
     * if the initial location was found.
     */
    private void startLocationUpdates() {

        LocationRequest request =
                LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(1000);
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
     */
    private void processAPIRequest(final LatLng point) {
        Observable<CurrentObservation> conditions = weatherAPI.getConditionsAPI().getCurrentObservationByLatLong(
                point.latitude,
                point.longitude);
        ConditionsHystrixCommand conditionsCommand = new ConditionsHystrixCommand(conditions);

        Observable<Forecast> forecast = weatherAPI.getForecastAPI().getForecastByLatLong(
                point.latitude,
                point.longitude);
        ForecastHystrixCommand forecastCommand = new ForecastHystrixCommand(forecast);

        Observable.zip(conditionsCommand.toObservable(), forecastCommand.toObservable(),
                       new Func2<CurrentObservation, Forecast, Pair<CurrentObservation, Forecast>>() {
                           @Override
                           public Pair<CurrentObservation, Forecast> call(CurrentObservation currentObservation,
                                                                          Forecast forecast) {
                               return new Pair(currentObservation, forecast);

                           }
                       }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
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
     * Handles updating the map marker with the current location's weather data and center the camera
     */
    private void updateMapAndCamera(CurrentObservation conditions, LatLng point) {
        if (currentMarker != null) {
            currentMarker.remove();
        }
        currentMarker = map.addMarker(new MarkerOptions()
                                              .position(point)
                                              .title(conditions.getLocation().getCity() + " " + conditions.getLocation()
                                                                                                          .getState()
                                                             + " " + conditions
                                                      .getLocation().getCountry())
                                              .snippet(conditions.getCurrentWeather() + " " + conditions
                                                      .getCurrentTemperature()));
        currentMarker.showInfoWindow();
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(point)
                .zoom(3.0f)
                .bearing(0)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        header.getTitle()
              .set("3 day forecast for " + conditions.getLocation().getCity() + " " + conditions.getLocation()
                                                                                                .getState());
    }

    /**
     * Updates the views list adapter when a new forecast is returned
     */
    private void updateListAdapter(Forecast forecast) {
        listAdapter.updateList(populateForecastList(forecast));
    }

    /**
     * Populates the listview with the forecast details for the current location.
     *
     * @return {@code List<String>}
     */
    private List<ForecastItem> populateForecastList(
            com.jdappel.android.wunderground.model.api.Forecast currentForecast) {
        List<ForecastItem> items = new ArrayList<>(currentForecast.getTextForecast().getForecastList().size());
        for (TextForecastDetail detail : currentForecast.getTextForecast().getForecastList()) {
            items.add(new ForecastItem(detail.getTitle() + " - " + detail.getText()));
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
