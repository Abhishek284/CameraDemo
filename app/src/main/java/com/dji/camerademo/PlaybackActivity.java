package com.dji.camerademo;

import android.drm.DrmStore;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.security.PrivateKey;

import dji.common.camera.SDCardState;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.remotecontroller.HardwareState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.KeyListener;
import dji.logic.album.model.DJIAlbumFileInfo;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;
import dji.sdk.camera.PlaybackManager;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

import static android.R.attr.type;
import static dji.midware.media.d.i;
import static dji.midware.media.d.n;

public class PlaybackActivity extends AppCompatActivity implements View.OnClickListener,TextureView.SurfaceTextureListener {


private Button back, next, previous, play_video, getCaptureCount;
    private TextView countView, isVideoText;
    private TextureView textureView=null;
    protected DJICodecManager mCodecManager = null;
    private boolean isSinglePreview = true;
    private PlaybackManager.PlaybackState mPlaybackState;
    private Camera mCamera;
    protected VideoFeeder.VideoDataCallback videoCallback = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        initUI();
        videoCallback = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
        startMediaPlayback();
    }
    private void showToast(String s){
        Toast.makeText(PlaybackActivity.this,s,Toast.LENGTH_LONG).show();



    }



//    private KeyListener sdcardCountKeyListener = new KeyListener() {
//
//        @Override
//        public void onValueChange(@Nullable Object o, @Nullable Object newValue) {
//            long count = ((long) newValue);
//
//            countView.setText(String.valueOf(count));
//
//
//        }
//    };



    @Override
    public void onResume() {
        super.onResume();
        initCameraCallBacks();
//Many conditions neeed to be verified before the below statement. see MainActivity
        VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(videoCallback);
//        DJIKey djiKey = CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT);
//        KeyManager.getInstance().addListener(djiKey, sdcardCountKeyListener);

    }
    private void startMediaPlayback(){
        Log.d("play", "Starting media playback");
        BaseProduct product = FPVDemoApplication.getProductInstance();
//        mCamera = FPVDemoApplication.getCameraInstance();
        mCamera = product.getCamera();
        SettingsDefinitions.CameraMode cameraMode = SettingsDefinitions.CameraMode.PLAYBACK;
        mCamera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error == null) {
                            Toast.makeText(PlaybackActivity.this,"Switch Camera Mode Succeeded",Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(PlaybackActivity.this,error.getDescription(),Toast.LENGTH_LONG).show();
                        }
                    }
                });
        Log.d("play",mCamera.getPlaybackManager().enterSinglePreviewModeWithIndex(0)+" ");
        mCamera.getPlaybackManager().enterSinglePreviewModeWithIndex(0);








    }

    private void initCameraCallBacks(){
        if(mCamera!=null){

            mCamera.getPlaybackManager().setPlaybackStateCallback(new PlaybackManager.PlaybackState.CallBack(){
                @Override
                public void onUpdate(PlaybackManager.PlaybackState playbackState) {
                    Log.d("play", playbackState+" ");

                    mPlaybackState=playbackState;


                    PlaybackActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("video", "Getting type :"+mPlaybackState.getFileType());

                            if(mPlaybackState.getPlaybackMode().equals(SettingsDefinitions.PlaybackMode.SINGLE_PHOTO_PREVIEW)) {
                                if(mPlaybackState.getFileType().equals(SettingsDefinitions.FileType.VIDEO)){
                                    play_video.setVisibility(View.VISIBLE);
                                    isVideoText.setVisibility(View.VISIBLE);
                                } else {
                                    play_video.setVisibility(View.GONE);
                                    isVideoText.setVisibility(View.GONE);
                                }

                            }
                            if(mPlaybackState.getPlaybackMode().equals(SettingsDefinitions.PlaybackMode.SINGLE_VIDEO_PLAYBACK_START)){
                                    isVideoText.setVisibility(View.GONE);

                            }

                            }


                    });


                }


            });
        }

    }


    private void initUI(){
//        countView = (TextView) findViewById(R.id.count_view);
        isVideoText = (TextView) findViewById(R.id.is_video_check);
        back = (Button) findViewById(R.id.btn_camera_view);
        next = (Button) findViewById(R.id.btn_next);
        previous = (Button) findViewById(R.id.btn_previous);
        textureView = (TextureView) findViewById(R.id.media_previewer_surface);
        play_video = (Button) findViewById(R.id.btn_play_video);
        play_video.setOnClickListener(this);
        back.setOnClickListener(this);
        next.setOnClickListener(this);
        previous.setOnClickListener(this);

        if (null != textureView) {
            textureView.setSurfaceTextureListener(this);
        }



    }
    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.btn_next:
                mCamera.getPlaybackManager().proceedToNextSinglePreviewPage();
                break;

            case R.id.btn_previous:
                mCamera.getPlaybackManager().proceedToPreviousSinglePreviewPage();

                break;
            case R.id.btn_play_video:
                mCamera.getPlaybackManager().playVideo();

            case R.id.btn_camera_view:
                break;

        }

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
