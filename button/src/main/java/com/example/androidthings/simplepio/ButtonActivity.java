/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.simplepio;

import android.Manifest;
import android.app.Activity;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Sample usage of the Gpio API that logs when a button is pressed.
 *
 */
public class ButtonActivity extends Activity {
    private static final String TAG = ButtonActivity.class.getSimpleName();

    private Gpio mButtonGpio;

// add camera
    private MyCamera mCamera;
    private Handler mCameraHandler;
    private HandlerThread mCameraThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);


        Button clickButton = (Button) findViewById(R.id.buttonTake);
        clickButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture();
            }
        });

        Log.i(TAG, "Starting ButtonActivity");


        // We need permission to access the camera
        Log.e(TAG, Integer.toString(checkSelfPermission(Manifest.permission.CAMERA)));
        
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.e(TAG, "No camera permission");
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            return;
        }
        else {
            Log.e(TAG, "camera permision");

            // Creates new handlers and associated threads for camera and networking operations.
            mCameraThread = new HandlerThread("CameraBackground");
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());
            mCamera = MyCamera.getInstance();
            mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener, (TextView)findViewById(R.id.counter));
        }

        try {
            String pinName = BoardDefaults.getGPIOForButton();
                mButtonGpio = PeripheralManager.getInstance().openGpio(pinName);
                mButtonGpio.setDirection(Gpio.DIRECTION_IN);
                mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
                mButtonGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                Log.i(TAG, "GPIO changed, button pressed");

                takePicture();



                // Return true to continue listening to events
                return true;
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }


    private void takePicture() {
        mCamera.takePicture();

    }



    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();
                    try {
                        onPictureTaken(imageBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };


    /**
     * Upload image data to Firebase as a doorbell event.
     */
    private void onPictureTaken(final byte[] imageBytes) throws IOException {


        if (imageBytes != null) {
            Log.i(TAG,  "image get");

        }
        else{
            Log.i(TAG, "Image not get");
        }

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCameraThread.quitSafely();


        if (mButtonGpio != null) {
            // Close the Gpio pin
            Log.i(TAG, "Closing Button GPIO pin");
            try {
                mButtonGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            } finally {
                mButtonGpio = null;
            }
        }
    }
}
