package com.dji.camerademo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SDCardState;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.KeyListener;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.PlaybackManager;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

import static android.R.attr.lockTaskMode;
import static android.R.attr.syncable;
import static android.R.attr.value;
import static dji.midware.media.d.n;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener {
    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn, mMediaPlayback;
    private ToggleButton mRecordBtn;
    private TextView recordingTime, countView, timeView, available_space, total_space;
    protected DJICodecManager mCodecManager = null;
    private Handler handler = new Handler();
    private Camera mCamera = FPVDemoApplication.getCameraInstance();
    private long count;
    private int timeLeft, hours, minutes, seconds, available_space_inMB, total_spac_inMB;
    private String timeString;
    private  Button changeFileFormat,changeVideoFormat;


    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initUI();
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    protected void onProductChange() {
        initPreviewer();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
//        setFrameRate();
        sdStateCall();
        firmwareVersionCheck();


        DJIKey djiKeyForAvailableCount = CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT);
        KeyManager.getInstance().addListener(djiKeyForAvailableCount, sdcardCountKeyListener);

//        djiKeyForAvailableCount.notify();


        DJIKey djiKeyForAvailableRecordtime = CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS);
        KeyManager.getInstance().addListener(djiKeyForAvailableRecordtime, sdcardTimeKeyListener);
//        sdcardTimeKeyListener.notify();
        DJIKey djiKeyForAvailableSpace = CameraKey.create(CameraKey.SDCARD_REMAINING_SPACE_IN_MB);
        KeyManager.getInstance().addListener(djiKeyForAvailableSpace, sdCardAvalableSpaceListener);
//        sdCardAvalableSpaceListener.notify();
        DJIKey djiKeyForTotalSpace = CameraKey.create(CameraKey.SDCARD_TOTAL_SPACE_IN_MB);
        KeyManager.getInstance().addListener(djiKeyForTotalSpace, sdCardTotalSpaceListener);

//        sdCardTotalSpaceListener.notify();


        onProductChange();
        if (mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }


    private void initPreviewer() {
        BaseProduct product = FPVDemoApplication.getProductInstance();
        if (product == null || !product.isConnected()) {
            Toast.makeText(this, getString(R.string.disconnected), Toast.LENGTH_SHORT).show();
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (VideoFeeder.getInstance().getVideoFeeds() != null
                        && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                    VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            // Reset the callback
            VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    public void onReturn(View view) {
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        showToast("onSurfaceTextureAvailable");

        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        showToast("onSurfaceTextureSizeChanged");
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        showToast("onSurfaceTextureDestroyed");
        Log.e(TAG, "onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    private void showToast(String s) {
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();


    }


    private void initUI() {
        // init mVideoSurface
        changeFileFormat = (Button) findViewById(R.id.change_image_format);
        changeVideoFormat = (Button) findViewById(R.id.change_record_format);
        available_space = (TextView) findViewById(R.id.available_space);
        total_space = (TextView) findViewById(R.id.total_space);
        countView = (TextView) findViewById(R.id.count_view);
        timeView = (TextView) findViewById(R.id.time_view);
        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        recordingTime = (TextView) findViewById(R.id.timer);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);
        mMediaPlayback = (Button) findViewById(R.id.view_media);
        mMediaPlayback.setOnClickListener(this);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);
        changeVideoFormat.setOnClickListener(this);
        changeFileFormat.setOnClickListener(this);
        recordingTime.setVisibility(View.INVISIBLE);
        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    recordingTime.setVisibility(View.VISIBLE);
                    startRecord();
                } else {
                    recordingTime.setVisibility(View.INVISIBLE);
                    stopRecord();
                }

            }
        });


        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {
                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;
                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recordingTime.setText(timeString);
                        /*
                         * Update recordingTime TextView visibility and mRecordBtn's check state
                         */
                                if (isVideoRecording) {
                                    recordingTime.setVisibility(View.VISIBLE);
                                } else {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });
        }


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_capture: {
                captureAction();
                break;
            }
            case R.id.btn_shoot_photo_mode: {
                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            }
            case R.id.btn_record_video_mode: {
                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            }
            case R.id.view_media: {
                Log.d("startPlayback", "Switching Start Playback");
                switchCameraMode(SettingsDefinitions.CameraMode.PLAYBACK);
//                mCamera.getPlaybackManager().enterSinglePreviewModeWithIndex(0);
//                mCamera.getPlaybackManager().setPlaybackStateCallback(new PlaybackManager.PlaybackState.CallBack(){
//                    @Override
//                    public void onUpdate(PlaybackManager.PlaybackState playbackState) {
//                        Log.d("play", playbackState+" ");
//
////                        mPlaybackState=playbackState;
//
//                    }
//
//
//                });
//
                Intent intent = new Intent(this, PlaybackActivity.class);
                startActivity(intent);
            }

            case R.id.change_image_format:
                setFileFormat();
                break;
            case R.id.change_record_format:
                setFrameRate();
                break;
            default:
                break;
        }
    }

    // Method for taking photo
    private void captureAction() {
        final Camera camera = FPVDemoApplication.getCameraInstance();
//        SettingsDefinitions.VideoResolution resolution = SettingsDefinitions.VideoResolution.RESOLUTION_1280x720;
//        SettingsDefinitions.VideoFrameRate frameRate = SettingsDefinitions.VideoFrameRate.FRAME_RATE_24_FPS;
//        final ResolutionAndFrameRate r = new ResolutionAndFrameRate();
//        r.setFrameRate(frameRate);
//        r.setResolution(resolution);
////        Camera camera = FPVDemoApplication.getCameraInstance();
//        camera.setVideoResolutionAndFrameRate(r, new CommonCallbacks.CompletionCallback() {
//            @Override
//            public void onResult(DJIError djiError) {
//                if(djiError ==null){
//                    Log.d(TAG, "onResult: Success Setting resolution 640*480");
//
//                }
//                else {
//                    Log.d(TAG, "onResult: Error Setting resolution 640*480"+ djiError.getDescription() +" "+ r.getResolution() +" "+ r.getFrameRate());
//                }
//
//            }
//        });
        //onSuccess: Resolution and fraameRESOLUTION_1920x1080 FRAME_RATE_59_DOT_940_FPS
//        setFrameRate();
        //The key does not match theformat: component/index/key with index being a number or *
        Log.d(TAG, "captureAction: Set 640 frame rate");
        camera.getVideoResolutionAndFrameRate(new CommonCallbacks.CompletionCallbackWith<ResolutionAndFrameRate>() {
            @Override
            public void onSuccess(ResolutionAndFrameRate resolutionAndFrameRate) {
                Log.d(TAG, "onSuccess: Resolution and fraame"+ resolutionAndFrameRate.getResolution()+" "+resolutionAndFrameRate.getFrameRate());
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        });

        if (camera != null) {
            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null) {
                                            Toast.makeText(MainActivity.this, "take photo: success", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, djiError.getDescription(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                        }, 2000);
                    }
                }
            });
        }
    }


    private void setFrameRate(){
        SettingsDefinitions.VideoResolution resolution = SettingsDefinitions.VideoResolution.RESOLUTION_4096x2160;
        SettingsDefinitions.VideoFrameRate frameRate = SettingsDefinitions.VideoFrameRate.FRAME_RATE_23_DOT_976_FPS;
        final ResolutionAndFrameRate r = new ResolutionAndFrameRate();
        r.setFrameRate(frameRate);
        r.setResolution(resolution);
        Camera camera = FPVDemoApplication.getCameraInstance();

        camera.setVideoResolutionAndFrameRate(r, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError ==null){
                    Log.d(TAG, "onResult: Success Setting resolution 1280*720");

                }
                else {
                    Log.d(TAG, "onResult: Error Setting resolution 1280*720"+ djiError.getDescription()+ r.getFrameRate()+ " " + r.getResolution());
                }

            }
        });

    }
    private void setFileFormat(){
//
        Camera camera = FPVDemoApplication.getCameraInstance();
        SettingsDefinitions.PhotoFileFormat phptFormat = SettingsDefinitions.PhotoFileFormat.JPEG;
        camera.setPhotoFileFormat(phptFormat, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError==null){



                }

            }
        });


    }

    private void firmwareVersionCheck(){
        BaseProduct product = FPVDemoApplication.getProductInstance();
        String version;
        version= product.getFirmwarePackageVersion();

        Log.d(TAG, "firmwareVersionCheck: "+ version);
        Toast.makeText(MainActivity.this, "Displaying version " + version , Toast.LENGTH_LONG).show();


    }



    private void sdStateCall() {
        final Camera camera = FPVDemoApplication.getCameraInstance();
        camera.setSDCardStateCallBack(new SDCardState.Callback() {
            @Override
            public void onUpdate(@NonNull SDCardState sdCardState) {
                Log.d(TAG, "onUpdate: "+sdCardState.getAvailableCaptureCount());
                count = sdCardState.getAvailableCaptureCount();
                timeLeft = sdCardState.getAvailableRecordingTimeInSeconds();
                available_space_inMB= sdCardState.getRemainingSpaceInMB();
                total_spac_inMB =  sdCardState.getTotalSpaceInMB();
                handler.post(setAllSDcardStateValues);
                camera.setSDCardStateCallBack(null);
            }
        });


}
    private Runnable setAllSDcardStateValues = new Runnable() {
        @Override
        public void run() {
            countView.setText(String.valueOf(count));
            hours = timeLeft / 3600;
            minutes = (timeLeft % 3600) / 60;
            seconds = timeLeft % 60;
            timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            timeView.setText(timeString);
            available_space.setText(String.valueOf(available_space_inMB));
            total_space.setText(String.valueOf(total_spac_inMB));

        }

    };


    private KeyListener sdcardCountKeyListener = new KeyListener() {

        @Override
        public void onValueChange(@Nullable Object o, @Nullable Object newValue) {
            count = ((long) newValue);
            Log.d("count", "Getting on value changed count " + count);
            handler.post(setCount);


        }
    };
    private Runnable setCount = new Runnable() {
        @Override
        public void run() {
            countView.setText(String.valueOf(count));
        }

    };


    private KeyListener sdcardTimeKeyListener = new KeyListener() {

        @Override
        public void onValueChange(@Nullable Object o, @Nullable Object newValue) {
            timeLeft = ((int) newValue);
            Log.d("count", "Getting on value changed count " + count);
            handler.post(setTime);


        }
    };
    private Runnable setTime = new Runnable() {
        @Override
        public void run() {
            hours = timeLeft / 3600;
            minutes = (timeLeft % 3600) / 60;
            seconds = timeLeft % 60;
            timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            timeView.setText(timeString);
        }

    };


    private KeyListener sdCardAvalableSpaceListener = new KeyListener() {

        @Override
        public void onValueChange(@Nullable Object o, @Nullable Object newValue) {
            available_space_inMB = ((int) newValue);
            Log.d("availSpace", "Getting on value changed availSpace " + count);
            handler.post(setAvailableSpace);


        }
    };
    private Runnable setAvailableSpace = new Runnable() {
        @Override
        public void run() {
            available_space.setText(String.valueOf(available_space_inMB));
        }

    };


    private KeyListener sdCardTotalSpaceListener = new KeyListener() {

        @Override
        public void onValueChange(@Nullable Object o, @Nullable Object newValue) {
            total_spac_inMB = ((int) newValue);
            Log.d("availSpace", "Getting on value changed totalSpace " + count);
            handler.post(setTotalSpace);


        }
    };
    private Runnable setTotalSpace = new Runnable() {
        @Override
        public void run() {
            total_space.setText(String.valueOf(total_spac_inMB));
        }

    };


    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode) {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        Toast.makeText(MainActivity.this, "Switch Camera Mode Succeeded", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, error.getDescription(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    // Method for starting recording
    private void startRecord() {
        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        showToast("Record video: success");
                    } else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        showToast("Stop recording: success");
                    } else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }
    }


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }
}