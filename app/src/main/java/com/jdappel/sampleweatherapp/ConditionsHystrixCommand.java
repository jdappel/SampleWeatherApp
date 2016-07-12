package com.jdappel.sampleweatherapp;

import com.jdappel.android.wunderground.model.api.CurrentObservation;
import com.jdappel.android.wunderground.model.api.Location;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;

import rx.Observable;

class ConditionsHystrixCommand extends HystrixObservableCommand<CurrentObservation> {

    private final Observable<CurrentObservation> obs;

    ConditionsHystrixCommand(Observable<CurrentObservation> obs) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("WUnderground"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("conditions"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                                                          .withExecutionTimeoutInMilliseconds(
                                                                                  1000)));
        this.obs = obs;
    }

    @Override
    protected Observable<CurrentObservation> construct() {
        return obs;
    }

    @Override
    protected Observable<CurrentObservation> resumeWithFallback() {
        return Observable
                .just(new CurrentObservation("Sunny", "77 F (25 C)", "12 MPH NW",
                                             new Location("Columbus", "Ohio", "US"),
                                             null));
    }


}
