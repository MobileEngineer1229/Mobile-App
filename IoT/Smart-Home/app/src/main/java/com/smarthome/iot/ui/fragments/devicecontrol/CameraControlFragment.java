package com.smarthome.iot.ui.fragments.devicecontrol;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment for controlling CCTV Camera
 * Supports live feed, playback, snapshot, record, and other camera controls
 */
public class CameraControlFragment extends Fragment {
    private static final String ARG_DEVICE_ID = "device_id";
    private static final String ARG_DEVICE_NAME = "device_name";
    private static final String ARG_ROOM_ID = "room_id";

    private Integer deviceId;
    private String deviceName;
    private Integer roomId;

    private ImageView imageViewLiveFeed;
    private TextView textViewLiveIndicator;
    private MaterialButton buttonPlayback;
    private MaterialButton buttonSnapshot;
    private MaterialButton buttonSpeak;
    private MaterialButton buttonRecord;
    private MaterialButton buttonGallery;
    private MaterialButton buttonPrivateMode;
    private MaterialButton buttonNightMode;
    private MaterialButton buttonMore;
    private View buttonMoveLeft;
    private View buttonMovePause;
    private View buttonMoveRight;
    private ImageView imageViewAudio;
    private ImageView imageViewFullscreen;

    private ApiService apiService;
    private boolean isRecording = false;
    private boolean isAudioEnabled = true;

    public static CameraControlFragment newInstance(Integer deviceId, String deviceName, Integer roomId) {
        CameraControlFragment fragment = new CameraControlFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DEVICE_ID, deviceId);
        args.putString(ARG_DEVICE_NAME, deviceName);
        args.putInt(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deviceId = getArguments().getInt(ARG_DEVICE_ID);
            deviceName = getArguments().getString(ARG_DEVICE_NAME);
            roomId = getArguments().getInt(ARG_ROOM_ID);
        }
        
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupControls();
        loadLiveFeed();
    }

    private void initializeViews(View view) {
        imageViewLiveFeed = view.findViewById(R.id.imageViewLiveFeed);
        textViewLiveIndicator = view.findViewById(R.id.textViewLiveIndicator);
        buttonPlayback = view.findViewById(R.id.buttonPlayback);
        buttonSnapshot = view.findViewById(R.id.buttonSnapshot);
        buttonSpeak = view.findViewById(R.id.buttonSpeak);
        buttonRecord = view.findViewById(R.id.buttonRecord);
        buttonGallery = view.findViewById(R.id.buttonGallery);
        buttonPrivateMode = view.findViewById(R.id.buttonPrivateMode);
        buttonNightMode = view.findViewById(R.id.buttonNightMode);
        buttonMore = view.findViewById(R.id.buttonMore);
        buttonMoveLeft = view.findViewById(R.id.buttonMoveLeft);
        buttonMovePause = view.findViewById(R.id.buttonMovePause);
        buttonMoveRight = view.findViewById(R.id.buttonMoveRight);
        imageViewAudio = view.findViewById(R.id.imageViewAudio);
        imageViewFullscreen = view.findViewById(R.id.imageViewFullscreen);
    }

    private void setupControls() {
        buttonPlayback.setOnClickListener(v -> executeCameraCommand("playback"));
        
        buttonSnapshot.setOnClickListener(v -> executeCameraCommand("snapshot"));
        
        buttonSpeak.setOnClickListener(v -> executeCameraCommand("speak"));
        
        buttonRecord.setOnClickListener(v -> {
            if (isRecording) {
                executeCameraCommand("record");
            } else {
                executeCameraCommand("record");
            }
            isRecording = !isRecording;
            updateRecordButton();
        });
        
        buttonGallery.setOnClickListener(v -> executeCameraCommand("gallery"));
        
        buttonPrivateMode.setOnClickListener(v -> executeCameraCommand("private_mode"));
        
        buttonNightMode.setOnClickListener(v -> executeCameraCommand("night_mode"));
        
        buttonMore.setOnClickListener(v -> {
            // TODO: Show more options dialog
            Toast.makeText(getContext(), "More options coming soon", Toast.LENGTH_SHORT).show();
        });
        
        buttonMoveLeft.setOnClickListener(v -> executeCameraMove("left"));
        
        buttonMovePause.setOnClickListener(v -> executeCameraMove("stop"));
        
        buttonMoveRight.setOnClickListener(v -> executeCameraMove("right"));
        
        if (imageViewAudio != null) {
            imageViewAudio.setOnClickListener(v -> {
                isAudioEnabled = !isAudioEnabled;
                imageViewAudio.setAlpha(isAudioEnabled ? 1.0f : 0.5f);
                toggleAudio(isAudioEnabled);
            });
        }
        
        if (imageViewFullscreen != null) {
            imageViewFullscreen.setOnClickListener(v -> {
                // TODO: Enter fullscreen mode
                Toast.makeText(getContext(), "Fullscreen mode coming soon", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void executeCameraCommand(String action) {
        Map<String, Object> command = new HashMap<>();
        command.put("action", action);
        
        Call<ApiResponse<Map<String, Object>>> call = apiService.controlCamera(deviceId, command);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(getContext(), "Command executed successfully", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMessage = "Failed to execute command";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMessage = response.body().getError().getMessage();
                    }
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executeCameraMove(String direction) {
        Map<String, Object> command = new HashMap<>();
        command.put("action", "move");
        command.put("direction", direction);
        
        Call<ApiResponse<Map<String, Object>>> call = apiService.controlCamera(deviceId, command);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Movement command executed
                } else {
                    Toast.makeText(getContext(), "Failed to move camera", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecordButton() {
        if (buttonRecord != null) {
            buttonRecord.setText(isRecording ? "Stop" : "Record");
        }
    }

    private void loadLiveFeed() {
        // Load camera stream URL
        Call<ApiResponse<Map<String, String>>> call = apiService.getCameraStream(deviceId);
        call.enqueue(new Callback<ApiResponse<Map<String, String>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, String>>> call, Response<ApiResponse<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, String> data = response.body().getData();
                    if (data != null && data.containsKey("streamUrl")) {
                        String streamUrl = data.get("streamUrl");
                        // TODO: Load video stream using ExoPlayer or similar
                        // For now, just show placeholder
                        if (imageViewLiveFeed != null) {
                            imageViewLiveFeed.setImageResource(R.drawable.ic_camera);
                        }
                        if (textViewLiveIndicator != null) {
                            textViewLiveIndicator.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    // Fallback to placeholder
                    if (imageViewLiveFeed != null) {
                        imageViewLiveFeed.setImageResource(R.drawable.ic_camera);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, String>>> call, Throwable t) {
                // Fallback to placeholder
                if (imageViewLiveFeed != null) {
                    imageViewLiveFeed.setImageResource(R.drawable.ic_camera);
                }
            }
        });
    }

    private void toggleAudio(boolean enabled) {
        if (deviceId == null || deviceId == 0) {
            return;
        }

        Map<String, Object> command = new HashMap<>();
        command.put("action", "toggle_audio");
        command.put("audioEnabled", enabled);
        
        Call<ApiResponse<Map<String, Object>>> call = apiService.controlCamera(deviceId, command);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("CameraControl", "Audio toggled: " + enabled);
                } else {
                    // Revert on failure
                    isAudioEnabled = !enabled;
                    if (imageViewAudio != null) {
                        imageViewAudio.setAlpha(isAudioEnabled ? 1.0f : 0.5f);
                    }
                    android.util.Log.w("CameraControl", "Failed to toggle audio via API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                // Revert on failure
                isAudioEnabled = !enabled;
                if (imageViewAudio != null) {
                    imageViewAudio.setAlpha(isAudioEnabled ? 1.0f : 0.5f);
                }
                android.util.Log.e("CameraControl", "Error toggling audio", t);
            }
        });
    }
}
