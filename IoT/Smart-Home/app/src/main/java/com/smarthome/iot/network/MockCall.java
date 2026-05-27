package com.smarthome.iot.network;

import java.io.IOException;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Retrofit Call implementation that immediately returns a local mock response.
 */
public class MockCall<T> implements Call<T> {
    private final T body;
    private boolean executed;
    private boolean canceled;

    public MockCall(T body) {
        this.body = body;
    }

    @Override
    public Response<T> execute() throws IOException {
        executed = true;
        return Response.success(body);
    }

    @Override
    public void enqueue(Callback<T> callback) {
        executed = true;
        if (!canceled && callback != null) {
            callback.onResponse(this, Response.success(body));
        }
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public Call<T> clone() {
        return new MockCall<>(body);
    }

    @Override
    public Request request() {
        return new Request.Builder()
                .url("http://localhost/mock")
                .build();
    }

    @Override
    public Timeout timeout() {
        return Timeout.NONE;
    }
}
