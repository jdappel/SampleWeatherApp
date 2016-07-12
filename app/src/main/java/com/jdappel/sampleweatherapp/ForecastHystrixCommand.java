package com.jdappel.sampleweatherapp;

import com.jdappel.android.wunderground.model.api.Forecast;
import com.jdappel.android.wunderground.model.api.TextForecast;
import com.jdappel.android.wunderground.model.api.TextForecastDetail;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;

import java.util.Arrays;

import rx.Observable;

class ForecastHystrixCommand extends HystrixObservableCommand<Forecast> {

    private final Observable<Forecast> obs;

    ForecastHystrixCommand(Observable<Forecast> obs) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("WUnderground"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("forecast"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                                                          .withExecutionTimeoutInMilliseconds(
                                                                                  1000)));
        this.obs = obs;
    }

    @Override
    protected Observable<Forecast> construct() {
        return obs;
    }

    @Override
    protected Observable<Forecast> resumeWithFallback() {
        return Observable
                .just(new Forecast(new TextForecast(Arrays.asList(
                        new TextForecastDetail("Partly Cloudy with slight wind. Lows overnight near 40 F",
                                               "Wednesday")))));
    }


}
