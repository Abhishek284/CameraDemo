package com.dji.camerademo;

import android.content.Intent;
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

import dji.common.battery.AggregationState;
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

import static android.R.attr.breadCrumbShortTitle;
import static android.R.attr.cacheColorHint;
import static android.R.attr.type;
import static dji.midware.media.d.i;
import static dji.midware.media.d.n;

public class PlaybackActivity extends AppCompatActivity implements View.OnClickListener,TextureView.SurfaceTextureListener {


private Button back, next, previous, play_video, getCaptureCount, multi_preview, preview1, preview2, preview3, preview4, preview5, preview6, preview7, preview8;
    private TextView countView, isVideoText;
    private TextureView textureView=null;
    protected DJICodecManager mCodecManager = null;
    private boolean isSinglePreview = false;
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
        isSinglePreview = true;








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
        preview1 = (Button) findViewById(R.id.previewButton1);
        preview2 = (Button) findViewById(R.id.previewButton2);
        preview3 = (Button) findViewById(R.id.previewButton3);
        preview4 = (Button) findViewById(R.id.previewButton4);
        preview5 = (Button) findViewById(R.id.previewButton5);
        preview6 = (Button) findViewById(R.id.previewButton6);
        preview7 = (Button) findViewById(R.id.previewButton7);
        preview8 = (Button) findViewById(R.id.previewButton8);

        multi_preview = (Button) findViewById(R.id.multiple_preview);
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
        multi_preview.setOnClickListener(this);


        preview1.setOnClickListener(this);
        preview2.setOnClickListener(this);
        preview3.setOnClickListener(this);
        preview4.setOnClickListener(this);
        preview5.setOnClickListener(this);
        preview6.setOnClickListener(this);
        preview7.setOnClickListener(this);
        preview8.setOnClickListener(this);



        if (null != textureView) {
            textureView.setSurfaceTextureListener(this);
        }



    }
    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.btn_next:
                if(!isSinglePreview){
                    mCamera.getPlaybackManager().proceedToNextSinglePreviewPage();
                }
                else {
                    mCamera.getPlaybackManager().proceedToNextMultiplePreviewPage();
                }
                break;

            case R.id.btn_previous:
                if(!isSinglePreview){
                    mCamera.getPlaybackManager().proceedToPreviousSinglePreviewPage();
                }
                else {
                    mCamera.getPlaybackManager().proceedToPreviousMultiplePreviewPage();
                }

                break;
            case R.id.btn_play_video:
                mCamera.getPlaybackManager().playVideo();
                break;

            case R.id.multiple_preview:
                mCamera.getPlaybackManager().enterMultiplePreviewMode();
                isSinglePreview = false;
                break;

            case R.id.previewButton1:
                previewAction(0);
                break;

            case R.id.previewButton2:
                previewAction(1);
                break;
            case R.id.previewButton3:
                previewAction(2);
                break;
            case R.id.previewButton4:
                previewAction(3);
                break;
            case R.id.previewButton5:
                previewAction(4);
                break;
            case R.id.previewButton6:
                previewAction(5);
                break;
            case R.id.previewButton7:
                previewAction(6);
                break;
            case R.id.previewButton8:
                previewAction(7);
                break;



            case R.id.btn_camera_view:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;

        }

    }


    private void previewAction(int i){
        if(mPlaybackState!=null && mCamera!=null){
            if(mPlaybackState.getPlaybackMode().equals(SettingsDefinitions.PlaybackMode.MULTIPLE_MEDIA_FILE_PREVIEW)){
                mCamera.getPlaybackManager().enterSinglePreviewModeWithIndex(i);
            }
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
