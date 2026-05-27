package com.smarthome.iot.network;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // Base URL - Use remote server for production, or localhost for emulator
    // For emulator: http://10.0.2.2:3003/api/v1/
    // For physical device: http://<your-local-ip>:3003/api/v1/
    // For remote server: http://172.86.88.76:3003/api/v1/
    private static final String BASE_URL = "http://172.86.88.76:3003/api/v1/";
    // Base URL for health check (no /api/v1 prefix)
    private static final String HEALTH_BASE_URL = "http://172.86.88.76:3003/";
    private static Retrofit retrofit = null;
    private static Retrofit healthRetrofit = null; // Separate Retrofit instance for health checks
    private static Context appContext = null;
    private static final ApiService MOCK_API_SERVICE = new MockApiService();

    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            android.util.Log.d("ApiClient", "Initializing Retrofit with BASE_URL: " + BASE_URL);
            
            // Create logging interceptor
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    android.util.Log.d("OkHttp", message);
                }
            });
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Create Auth interceptor with context
            AuthInterceptor authInterceptor = new AuthInterceptor();
            if (appContext != null) {
                authInterceptor.setContext(appContext);
            }

            // Create OkHttp client
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Create Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            
            android.util.Log.d("ApiClient", "Retrofit client created successfully");
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        if (com.smarthome.iot.utils.MockDataProvider.isMockupMode()) {
            android.util.Log.d("ApiClient", "Using local MockApiService for mockup mode");
            return MOCK_API_SERVICE;
        }
        return getClient().create(ApiService.class);
    }

    /**
     * Get Retrofit client for health check endpoint
     * Health endpoint is at /health (not /api/v1/health)
     */
    public static Retrofit getHealthClient() {
        if (healthRetrofit == null) {
            android.util.Log.d("ApiClient", "Initializing Health Retrofit with BASE_URL: " + HEALTH_BASE_URL);
            
            // Create OkHttp client for health checks (no auth needed, shorter timeout)
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .build();

            // Create Retrofit instance for health checks
            healthRetrofit = new Retrofit.Builder()
                    .baseUrl(HEALTH_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            
            android.util.Log.d("ApiClient", "Health Retrofit client created successfully");
        }
        return healthRetrofit;
    }

    public static ApiService getHealthApiService() {
        if (com.smarthome.iot.utils.MockDataProvider.isMockupMode()) {
            return MOCK_API_SERVICE;
        }
        return getHealthClient().create(ApiService.class);
    }

    public static String getBaseUrl() {

        return BASE_URL;
    }
}

