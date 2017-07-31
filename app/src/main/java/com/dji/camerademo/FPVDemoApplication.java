package com.dji.camerademo;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import dji.common.battery.AggregationState;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.keysdk.CameraKey;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.camera.Camera;
import dji.sdk.camera.MediaManager;
import dji.sdk.flightcontroller.FlyZoneManager;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKManager;


/**
 * Created by abhishek on 25/07/17.
 */

public class FPVDemoApplication extends Application {
    private static final String TAG = MainActivity.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static BaseProduct mProduct;
    private static AggregationState aggregationStateNew;
    private static FlyZoneManager flyZoneManager;
    private static BaseComponent mComponent;
    private static Aircraft aircraft;
    private Handler mHandler;
    private Button login_button,logout_button,setListeners;
    private CheckBox c1_click,c2_click,c1_double_click,c2_double_click, c1_long_click,c2_long_click,both_c1_c2_click;
    private Handler handler= new Handler();
    private boolean isRemoteConnected=false;
    RemoteController remoteController ;
    private CheckBox checkBox;
    private ProgressBar progressBar;
    private static Battery battery = null;
    private MediaManager mediaManager;



    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();

        }
        return mProduct;
    }

//    public static synchronized Aircraft getAircraftInstance() {
//        if (null == aircraft) {
//            aircraft = DJISDKManager.getInstance().`;
//
//        }
//        return mProduct;
//    }

public static synchronized AggregationState getAggregationState(){
    battery = getProductInstance().getBattery();

     battery.setAggregationStateCallback(new AggregationState.Callback() {
        @Override
        public void onUpdate(AggregationState aggregationState) {
            aggregationStateNew = aggregationState;
        }
    });

    return aggregationStateNew;
}

    public static synchronized MediaManager getMediamMnager(){

        return  getProductInstance().getCamera().getMediaManager();
    }


    public static synchronized Camera getCameraInstance() {

        if (getProductInstance() == null) return null;

        Camera camera = null;

        if (getProductInstance() instanceof Aircraft){

            camera = ((Aircraft) getProductInstance()).getCamera();

        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }

        return camera;
    }

    public static synchronized FlyZoneManager getFlyzoneInstance(){
        if(flyZoneManager==null){
            flyZoneManager = DJISDKManager.getInstance().getFlyZoneManager();
        }

        return flyZoneManager;
    }

//    public static synchronized CameraKey getCameraKeyInstance() {
//        CameraKey cameraKey = null;
//
//        return cameraKey;
//    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        //This is used to start SDK services and initiate SDK.
        DJISDKManager.getInstance().registerApp(this, mDJISDKManagerCallback);
    }



    /**
     * When starting SDK services, an instance of interface DJISDKManager.SDKManagerCallback will be used to listen to
     * the SDK Registration result and the product changing.
     */
    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {
        //Listens to the SDK registration result
        @Override
        public void onRegister(DJIError error) {
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register Success", Toast.LENGTH_LONG).show();
                        Log.d("rg", "register Success");
                    }
                });
                DJISDKManager.getInstance().startConnectionToProduct();
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register sdk fails, check network is available", Toast.LENGTH_LONG).show();
                    }
                });
            }
            Log.e("TAG", error.toString());
        }
        //Listens to the connected product changing, including two parts, component changing or product connection changing.
        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            mProduct = newProduct;
            if(mProduct != null) {
                mProduct.setBaseProductListener(mDJIBaseProductListener);
            }
            notifyStatusChange();
        }
    };
    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
//            Log.d("onCompConnectChange",isConnected+" "+"Connectivity change inside ComponentListener device");
//            Toast.makeText(getApplicationContext(), "Connectivity change inside ComponentListener"+" "+isConnected, Toast.LENGTH_LONG).show();
            notifyStatusChange();

        }


    };

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };
}
