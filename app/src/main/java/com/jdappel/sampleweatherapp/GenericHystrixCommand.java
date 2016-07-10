package com.jdappel.sampleweatherapp;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;

import rx.Observable;

class GenericHystrixCommand<T> extends HystrixObservableCommand<T> {

    private final Observable<T> obs;

    GenericHystrixCommand(Observable<T> obs, String commandKey) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("WUnderground"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey)));
        this.obs = obs;
    }

    @Override
    protected Observable<T> construct() {
        return obs;
    }
}
