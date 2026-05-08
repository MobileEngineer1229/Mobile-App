package com.smarthome.iot.network;

import android.util.Log;
import com.smarthome.iot.models.HealthResponse;
import com.smarthome.iot.utils.Globals;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Service for checking API health status
 */
public class HealthCheckService {
    private static final String TAG = "HealthCheckService";
    private static HealthCheckService instance;
    private ApiService healthApiService;

    private HealthCheckService() {
        // Create Retrofit instance for health checks (uses different base URL)
        Retrofit healthRetrofit = ApiClient.getHealthClient();
        healthApiService = healthRetrofit.create(ApiService.class);
    }

    public static synchronized HealthCheckService getInstance() {
        if (instance == null) {
            instance = new HealthCheckService();
        }
        return instance;
    }

    /**
     * Check API health status
     * Updates Globals with the result
     */
    public void checkHealth() {
        checkHealth(null);
    }

    /**
     * Check API health status with callback
     * @param callback Optional callback to be notified when check completes
     */
    public void checkHealth(HealthCheckCallback callback) {
        Log.d(TAG, "Checking API health...");
        
        Call<HealthResponse> call = healthApiService.checkHealth();
        call.enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(Call<HealthResponse> call, Response<HealthResponse> response) {
                boolean isHealthy = false;
                if (response.isSuccessful() && response.body() != null) {
                    HealthResponse healthResponse = response.body();
                    isHealthy = healthResponse.isHealthy();
                    Globals.setMqttBrokerAvailable(healthResponse.isMqttConnected());
                    Log.d(TAG, "Health check response: " + healthResponse.getStatus() +
                          " (healthy: " + isHealthy + ", mqtt: " + healthResponse.getMqtt() + ")");
                } else {
                    Log.w(TAG, "Health check failed: HTTP " + response.code());
                    Globals.setMqttBrokerAvailable(false);
                }

                Globals.setApiHealthStatus(isHealthy);
                
                if (callback != null) {
                    callback.onHealthCheckComplete(isHealthy);
                }
            }

            @Override
            public void onFailure(Call<HealthResponse> call, Throwable t) {
                Log.e(TAG, "Health check network error", t);
                Globals.setApiHealthStatus(false);
                Globals.setMqttBrokerAvailable(false);
                
                if (callback != null) {
                    callback.onHealthCheckComplete(false);
                }
            }
        });
    }

    /**
     * Callback interface for health check results
     */
    public interface HealthCheckCallback {
        void onHealthCheckComplete(boolean isHealthy);
    }
}
