package com.dji.camerademo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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
import static android.R.attr.value;
import static dji.midware.media.d.n;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener {
    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn, mMediaPlayback;
    private ToggleButton mRecordBtn;
    private TextView recordingTime, countView,timeView;
    protected DJICodecManager mCodecManager = null;
    private Handler handler = new Handler();
    private Camera mCamera =  FPVDemoApplication.getCameraInstance();
    private long count;
    private int timeLeft, hours, minutes, seconds;
    private  String timeString;



    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
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
    }
    protected void onProductChange() {
        initPreviewer();
    }
    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        DJIKey djiKey = CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT);
        KeyManager.getInstance().addListener(djiKey, sdcardCountKeyListener);
        DJIKey djiKey1 = CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS);
        KeyManager.getInstance().addListener(djiKey1, sdcardTimeKeyListener);

        onProductChange();
        if(mVideoSurface == null) {
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
        if (camera != null){
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
        super.onStop();
    }
    public void onReturn(View view){
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
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
    private void showToast(String s){
        Toast.makeText(MainActivity.this,s,Toast.LENGTH_LONG).show();


    }




    private void initUI() {
        // init mVideoSurface
        countView = (TextView) findViewById(R.id.count_view);
        timeView= (TextView) findViewById(R.id.time_view);
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
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
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
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
            case R.id.btn_capture:{
                captureAction();
                break;
            }
            case R.id.btn_shoot_photo_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            }
            case R.id.btn_record_video_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            }
            case R.id.view_media:{
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
            default:
                break;
        }
    }

    // Method for taking photo
    private void captureAction(){
        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
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
                                            Toast.makeText(MainActivity.this,"take photo: success",Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(MainActivity.this,djiError.getDescription(),Toast.LENGTH_LONG).show();
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

    private KeyListener sdcardCountKeyListener = new KeyListener() {

        @Override
        public void onValueChange(@Nullable Object o, @Nullable Object newValue) {
             count = ((long) newValue);
            Log.d("count", "Getting on value changed count "+count);
            handler.post(setCount);




        }
    };
    private Runnable setCount = new Runnable(){
        @Override
        public void run() {
            countView.setText(String.valueOf(count));
        }

    };

    private KeyListener sdcardTimeKeyListener = new KeyListener() {

        @Override
        public void onValueChange(@Nullable Object o, @Nullable Object newValue) {
            timeLeft = ((int) newValue);
            Log.d("count", "Getting on value changed count "+count);
            handler.post(setTime);




        }
    };
    private Runnable setTime = new Runnable(){
        @Override
        public void run() {
            hours = timeLeft / 3600;
            minutes = (timeLeft % 3600) / 60;
            seconds = timeLeft % 60;
            timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            timeView.setText(timeString);
        }

    };



    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        Toast.makeText(MainActivity.this,"Switch Camera Mode Succeeded",Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this,error.getDescription(),Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    // Method for starting recording
    private void startRecord(){
        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if (djiError == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }
    // Method for stopping recording
    private void stopRecord(){
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }
    }




}