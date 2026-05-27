package com.smarthome.iot.ui.fragments.devicecontrol;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.views.VolumeArcView;
import com.smarthome.iot.utils.Globals;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment for controlling Stereo Speaker
 * Supports volume control, playback control, and music service integration
 */
public class SpeakerControlFragment extends Fragment {
    private static final String ARG_DEVICE_ID = "device_id";
    private static final String ARG_DEVICE_NAME = "device_name";
    private static final String ARG_ROOM_ID = "room_id";

    private Integer deviceId;
    private String deviceName;
    private Integer roomId;

    private TextView textViewNowPlaying;
    private VolumeArcView volumeArcView;
    private TextView textViewVolume;
    private View buttonPrevious;
    private View buttonPause;
    private View buttonNext;
    private ImageView imageViewPauseIcon;
    private SeekBar seekBarProgress;
    private TextView textViewCurrentTime;
    private TextView textViewTotalTime;

    private ApiService apiService;
    private boolean isPlaying = false;
    private int currentVolume = 65;

    public static SpeakerControlFragment newInstance(Integer deviceId, String deviceName, Integer roomId) {
        SpeakerControlFragment fragment = new SpeakerControlFragment();
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
        
        apiService = ApiClient.getApiService();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_speaker_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupControls();
        loadDeviceState();
    }

    private void initializeViews(View view) {
        textViewNowPlaying = view.findViewById(R.id.textViewNowPlaying);
        volumeArcView = view.findViewById(R.id.volumeArcView);
        textViewVolume = view.findViewById(R.id.textViewVolume);
        buttonPrevious = view.findViewById(R.id.buttonPrevious);
        buttonPause = view.findViewById(R.id.buttonPause);
        buttonNext = view.findViewById(R.id.buttonNext);
        imageViewPauseIcon = view.findViewById(R.id.imageViewPauseIcon);
        seekBarProgress = view.findViewById(R.id.seekBarProgress);
        textViewCurrentTime = view.findViewById(R.id.textViewCurrentTime);
        textViewTotalTime = view.findViewById(R.id.textViewTotalTime);
    }

    private void setupControls() {
        // Volume arc control
        if (volumeArcView != null) {
            volumeArcView.setVolume(currentVolume);
            volumeArcView.setOnVolumeChangeListener(volume -> {
                currentVolume = volume;
                if (textViewVolume != null) {
                    textViewVolume.setText(volume + "%");
                }
                // Update volume via API
                updateVolume(volume);
            });
        }

        if (textViewVolume != null) {
            textViewVolume.setText(currentVolume + "%");
        }

        // Playback controls
        if (buttonPrevious != null) {
            buttonPrevious.setOnClickListener(v -> controlPlayback("previous"));
        }

        if (buttonPause != null) {
            buttonPause.setOnClickListener(v -> {
                if (isPlaying) {
                    controlPlayback("pause");
                } else {
                    controlPlayback("play");
                }
                isPlaying = !isPlaying;
                updatePauseButton();
            });
        }

        if (buttonNext != null) {
            buttonNext.setOnClickListener(v -> controlPlayback("next"));
        }

        // Progress bar
        if (seekBarProgress != null) {
            seekBarProgress.setMax(100);
            seekBarProgress.setProgress(45); // 2:48 / 3:53 ≈ 45%
            seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        // Update current time display (approximate)
                        int totalSeconds = 233; // 3:53 = 233 seconds
                        int currentSeconds = (int) (totalSeconds * progress / 100f);
                        int minutes = currentSeconds / 60;
                        int seconds = currentSeconds % 60;
                        if (textViewCurrentTime != null) {
                            textViewCurrentTime.setText(String.format("%02d:%02d", minutes, seconds));
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int position = seekBar.getProgress();
                    android.util.Log.d("SpeakerControl", "Seek to: " + position);
                    seekToPosition(position);
                }
            });
        }

        if (textViewCurrentTime != null) {
            textViewCurrentTime.setText("02:48");
        }
        if (textViewTotalTime != null) {
            textViewTotalTime.setText("03:53");
        }
    }

    private void updateVolume(int volume) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("volume", volume);
        
        Call<ApiResponse<Map<String, Object>>> call = apiService.controlSpeaker(deviceId, settings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Volume updated successfully
                } else {
                    Toast.makeText(getContext(), "Failed to update volume", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void controlPlayback(String action) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("action", action);
        
        Call<ApiResponse<Map<String, Object>>> call = apiService.controlSpeaker(deviceId, settings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> data = response.body().getData();
                    if (data != null && data.containsKey("nowPlaying")) {
                        // Update now playing info
                        Map<String, Object> nowPlaying = (Map<String, Object>) data.get("nowPlaying");
                        if (nowPlaying != null && textViewNowPlaying != null) {
                            String artist = (String) nowPlaying.get("artist");
                            String title = (String) nowPlaying.get("title");
                            if (artist != null && title != null) {
                                textViewNowPlaying.setText(artist + " - " + title);
                            }
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to control playback", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePauseButton() {
        if (imageViewPauseIcon != null) {
            if (isPlaying) {
                imageViewPauseIcon.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                imageViewPauseIcon.setImageResource(android.R.drawable.ic_media_play);
            }
        }
    }

    /**
     * Load device state from backend metadata and sync to UI controls
     * Reads: volume, nowPlaying, isPlaying from device metadata
     */
    private void loadDeviceState() {
        if (deviceId == null || deviceId == 0) {
            android.util.Log.w("SpeakerControl", "Invalid device ID, cannot load state");
            loadNowPlaying();
            return;
        }

        // Fetch device details from API to get metadata
        Call<ApiResponse<Device>> call = apiService.getDeviceById(deviceId);
        call.enqueue(new Callback<ApiResponse<Device>>() {
            @Override
            public void onResponse(Call<ApiResponse<Device>> call, Response<ApiResponse<Device>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Device device = response.body().getData();
                    if (device != null && device.getMetadata() != null) {
                        Map<String, Object> metadata = device.getMetadata();
                        android.util.Log.d("SpeakerControl", "Loaded device metadata: " + metadata);

                        // Sync volume from metadata (0-100)
                        if (metadata.containsKey("volume")) {
                            Object volumeObj = metadata.get("volume");
                            if (volumeObj instanceof Number) {
                                currentVolume = ((Number) volumeObj).intValue();
                                if (volumeArcView != null) {
                                    volumeArcView.setVolume(currentVolume);
                                }
                                if (textViewVolume != null) {
                                    textViewVolume.setText(currentVolume + "%");
                                }
                                android.util.Log.d("SpeakerControl", "Synced volume: " + currentVolume);
                            }
                        } else {
                            // Set default volume
                            if (textViewVolume != null) {
                                textViewVolume.setText(currentVolume + "%");
                            }
                        }

                        // Sync now playing from metadata
                        if (metadata.containsKey("nowPlaying")) {
                            Object nowPlayingObj = metadata.get("nowPlaying");
                            if (nowPlayingObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> nowPlaying = (Map<String, Object>) nowPlayingObj;
                                if (nowPlaying != null && textViewNowPlaying != null) {
                                    String artist = (String) nowPlaying.get("artist");
                                    String title = (String) nowPlaying.get("title");
                                    if (artist != null && title != null) {
                                        textViewNowPlaying.setText(artist + " - " + title);
                                    } else {
                                        textViewNowPlaying.setText("No track playing");
                                    }
                                }
                            }
                        } else {
                            loadNowPlaying();
                        }

                        // Sync playback state from metadata
                        if (metadata.containsKey("isPlaying")) {
                            Object isPlayingObj = metadata.get("isPlaying");
                            if (isPlayingObj instanceof Boolean) {
                                isPlaying = (Boolean) isPlayingObj;
                                updatePauseButton();
                            }
                        }
                    } else {
                        android.util.Log.w("SpeakerControl", "Device has no metadata, using defaults");
                        loadNowPlaying();
                    }
                } else {
                    android.util.Log.w("SpeakerControl", "Failed to load device from API");
                    loadNowPlaying();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Device>> call, Throwable t) {
                android.util.Log.e("SpeakerControl", "Error loading device", t);
                loadNowPlaying();
            }
        });
    }

    private void loadNowPlaying() {
        // Default fallback
        if (textViewNowPlaying != null) {
            textViewNowPlaying.setText("Ed Sheeran - Shape of You");
        }
    }

    private void seekToPosition(int position) {
        if (deviceId == null || deviceId == 0) {
            return;
        }

        Map<String, Object> settings = new HashMap<>();
        settings.put("action", "seek");
        settings.put("position", position); // Position as percentage (0-100)
        
        Call<ApiResponse<Map<String, Object>>> call = apiService.controlSpeaker(deviceId, settings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("SpeakerControl", "Seeked to position: " + position);
                } else {
                    android.util.Log.w("SpeakerControl", "Failed to seek via API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                android.util.Log.e("SpeakerControl", "Error seeking", t);
            }
        });
    }

}
