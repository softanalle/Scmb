package com.softanalle.scmb.ui;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by t2r on 8.1.2014.
 * @author Tommi Rintala <t2r@iki.fi>
 * @license GNU GPL 3.0
 *
Area selector component for SCMB

Copyright (C) 2013  Tommi Rintala <t2r@iki.fi>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


public class Preview extends SurfaceView implements SurfaceHolder.Callback {
    // logger instance
    private static final Logger logger = LoggerFactory.getLogger("Preview");

    private boolean isPreview_ = false;
    private boolean isReady_ = false;

    private String mImageFilenameSuffix = "focus";

    // support option arrays
    private List<String> whiteBalanceModes_ = null;
    private List<Integer> supportedPictureFormats_ = null;


    SurfaceHolder mHolder;
    public Camera camera;

    /*
     * @param context Control context
     */
    public Preview(Context context) {
        super(context);

        mHolder = getHolder();
        mHolder.addCallback(this);

        // mPreviewRectPaint.setColor(Color.RED);

        //mHolder.setType(SurfaceHolder.)
        //Log.d(TAG, "create OK");
        logger.debug("Preview() completed");
    }

    /*
             * Called when surface is created
             * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
             */
    public void surfaceCreated(SurfaceHolder holder) {
        //Log.d(TAG, "surfaceCreated(holder)");
        logger.debug("Preview.surfaceCreated()");
        if ( camera == null ) {
            logger.debug("-camera was null, opening");
            // camera = getCameraInstance();
            reclaimCamera();
        }
        if ( camera == null ) {
            logger.debug("Unable to obtain camera!");
        } else {
            try {

                camera.setPreviewDisplay(holder);

                isReady_ = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Called when surface is destroyed
     * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Log.d(TAG, "surfaceDestroyed(holder)");
        logger.debug("preview.surfaceDestroyed()", null);
        if ( camera != null ) {
            //Log.d(TAG, "-camera was not null, releasing");
            logger.debug("preview.surfaceDestroyed(): camera was not null, releasing");
            stopPreview();
            camera.release();
            isPreview_ = false;
        }
        camera = null;
        isReady_ = false;
    }

    /*
        * on surface change event
        * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
        */
    public void surfaceChanged(SurfaceHolder holder, int format, int height, int width) {
        logger.debug("preview.surfaceChanged()", null);
        if (mHolder.getSurface() == null){
            // preview surface does not exist
            logger.debug("preview.surfaceChanged(): preview surface does not exist yet");
            return;
        }

        // Log.d(TAG, "surfaceChanged(holder)");
        if ( camera == null ) {
            //Log.d(TAG, "-camera was null, opening");
            logger.debug("preview.surfaceChanged(): camera was null, opening");
            camera = getCameraInstance();
        }

        // stop preview before making changes
        try {
            stopPreview();
            isPreview_ = false;
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
            logger.debug("preview.surfaceChanged(): tried to stop a non-existing preview");
        }



        try {
            if (camera != null) {

                logger.debug("preview.surfaceChanged(): set camera parameters");
                Camera.Parameters params = camera.getParameters();
                params.setPreviewSize(width,  height);

                whiteBalanceModes_ = params.getSupportedWhiteBalance();
                supportedPictureFormats_ = params.getSupportedPictureFormats();

                //params.set("rawsave-mode", "1");

                // no compression!
                params.setJpegQuality(100);

                List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
                for (Size p : previewSizes) {
                    if ( p.width == 640 ) {
                        params.setPreviewSize(p.width,  p.height);
                        break;
                    }
                }

                // turn autofocus off
                //params.setFocusMode(Parameters.FOCUS_MODE_FIXED);

                params.setFocusMode(Parameters.FOCUS_MODE_MACRO);

                if ( supportedPictureFormats_.contains(ImageFormat.JPEG)) {
                    params.setPictureFormat(ImageFormat.JPEG);
                    logger.debug("image format: JPEG");
                } else if (supportedPictureFormats_.contains(ImageFormat.RGB_565)) {
                    params.setPictureFormat(ImageFormat.RGB_565);
                    logger.debug("image format: RGB_565");
                } else if ( supportedPictureFormats_.contains(ImageFormat.YV12)) {
                    params.setPictureFormat(ImageFormat.YV12);
                    logger.debug("image format: YV12");
                } else if ( supportedPictureFormats_.contains(ImageFormat.YUY2)) {
                    logger.debug("image format: YU12");
                    params.setPictureFormat(ImageFormat.YUY2);
                } else {
                    params.setPictureFormat(ImageFormat.NV21);
                    logger.debug("image format: NV21");
                }

                // turn flash off
                params.setFlashMode(Parameters.FLASH_MODE_OFF);
                logger.debug("setFlashMode(OFF)");

                if ( params.isAutoWhiteBalanceLockSupported()) {
                    params.setAutoWhiteBalanceLock(true);
                } else {
                    //Toast.makeText(getContext(), "Unable to lock AutoWhiteBalance", Toast.LENGTH_LONG).show();
                    logger.debug("preview.surfaceChanged(): unable to lock autoWhiteBalance");
                }

                if ( params. isAutoExposureLockSupported() ) {
                    params.setAutoExposureLock(true);
                } else {
                    //Toast.makeText(getContext(), "Unable to lock AutoExposure", Toast.LENGTH_LONG).show();
                    logger.debug("unable to lock AutoExposure");
                }

                // we don't work with GPS data
                params.removeGpsData();
                camera.setParameters(params);
                startPreview();
                isReady_ = true;
            } else {
                //Log.e(TAG, "-camera is still null, failed to retrieve");
                logger.error("-camera is still null, failed to retrieve");
            }
        } catch (Exception e) {
            logger.error("Error starting camera preview: " + e.getMessage());
            //Log.e(TAG, "Error starting camera preview: " + e.getMessage());

        }
    }

    /**
     * Is the control ready
     * @return
     */
    public boolean isReady() {
        return isReady_;
    }
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        logger.debug("preview.getCameraInstance()");
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            logger.error("preview.getCameraInstance() : camera not available");
        }
        return c; // returns null if camera is unavailable
    }

    /**
     * Activate camera preview
     */
    public void startPreview() {
        logger.debug("startPreview");
        if ( camera != null && !isPreview_) {
            logger.debug("preview.startPreview(): do stuff");
            camera.startPreview();
            isPreview_ = true;
        }
    }

    /**
     * Stop camera preview
     */
    public void stopPreview() {
        logger.debug("stopPreview");
        if ( camera != null ) {
            logger.debug("preview.stopPreview(): do stuff");
            camera.stopPreview();
            isPreview_ = false;
        }
    }
    /*
     * @param cb autofocus callback function
     */
    protected void doFocus(Camera.AutoFocusCallback cb) {
        logger.debug("preview.doFocus(Camera.AutoFocusCallback)");
        camera.autoFocus(cb);
    }

    protected void doFocus() {
        logger.debug("preview.doFocus()");
        camera.autoFocus(null);
    }

        /*
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
                Toast.makeText(getContext(), "Focus complete", Toast.LENGTH_LONG).show();
                //

        }
*/
    /**
     * @return the whiteBalanceModes_
     */
    public List<String> getWhiteBalanceModes() {
        return whiteBalanceModes_;
    }


    /**
     * Save a byte vector to a file
     * @param filename the name of file to write
     * @param data byte vector of data
     * @return success of operation
     */
    private boolean writeImageToDisc(String filename, byte[] data) {
        //Log.d(TAG, "writeImageToDisc - begin");
        logger.debug("preview.writeImageToDisc(" + filename + ", " + data.length + " bytes)");
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream( filename );
            outStream.write(data);
            outStream.close();
            //Log.d(TAG, "writeImageToDisc - wrote bytes: " + data.length);
            //Toast.makeText(getContext(), filename + " - wrote bytes: " + data.length, Toast.LENGTH_SHORT).show();
            logger.debug("preview.writeImageToDisc(" + filename + ")");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logger.debug("preview.writeImageToDisc(): FileNotFoundExeption: " + e.toString());
            return false;
        } catch (IOException e) {
            logger.error("preview.writeImageToDisc(): " + e.toString());
            e.printStackTrace();
            return false;
        } finally {
        }
        //Log.d(TAG, "writeImageToDisc - complete");
        return true;
    }

    /**
     * Take the calibration picture and save
     * @param raw
     * @param storagePath
     */
    public void takeCalibrationPicture(final boolean raw, final String storagePath) {
        PictureCallback jpegCallback = null;
        PictureCallback rawCallback = null;
        logger.debug("preview.takeCalibrationPicture(" + raw + ", " + storagePath + ")");
        final String filename = storagePath + "/whiteref";
        if ( camera != null ) {
            startPreview();

            if ( raw ) {
                logger.debug("- write RAW image");
                // set the raw-info data to image
                Camera.Parameters parameters = camera.getParameters();
                parameters.set("rawsave-mode",  "1");
                parameters.set("rawfname",  filename + ".raw");
                camera.setParameters(parameters);
            }

            jpegCallback = new PictureCallback() {

                private String mJpegFilename = filename;
                @Override public void onPictureTaken(byte[] data, Camera camera) {
                    //try {
                    writeImageToDisc(mJpegFilename + ".JPG", data);
                    //Thread.sleep(200);
                    //} catch (InterruptedException e) {
                    //Toast.makeText(getContext(), "Error while saving JPEG file: " + e, Toast.LENGTH_LONG).show();
                    //}
                }
            };

            camera.takePicture(null,  rawCallback, jpegCallback);
            isPreview_ = false;
                        /*
                        try {
                                Thread.sleep(1000);
                        } catch (InterruptedException e) {
                                // if sleep fails, what can you do?
                        }
                        */
            startPreview();
        }
    }

    /**
     * take picture and store JPEG and RAW images
     * @param doRAW store RAW image
     * @param filename the full filename for image, without suffix (.jpg, .raw)
     */
    public void takePicture(final boolean doRAW, final String filename) {
        PictureCallback jpegCallback = null;
        PictureCallback rawCallback = null;

        //if ( doJPEG ) {
        if ( doRAW ) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.set("rawsave-mode",  "1");
            parameters.set("rawfname",  filename + ".raw");
            camera.setParameters(parameters);
        }
        //startPreview();
        jpegCallback = new PictureCallback() {

            private String mJpegFilename = filename;
            @Override public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    writeImageToDisc(mJpegFilename + ".JPG", data);

                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Toast.makeText(getContext(), "Error while saving JPEG file: " + e, Toast.LENGTH_LONG).show();
                }
            }
        };
        camera.takePicture(null,  rawCallback, jpegCallback);
        isPreview_ = false;
    }

    /**
     * ShutterCallback - if we wish to accomplish something on shutter click, we do it here.
     * remember to activate call in takePicture() -function
     */

    public ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            logger.debug("preview.onShutter()");
            //Log.d(TAG, "onShutter");
        }
    };



    /**
     * release camera handler, called on application onResume()
     */
    protected void releaseCamera() {
        stopPreview();
        logger.debug("preview.releaseCamera()");
        if ( camera != null ) {
            camera.release();
            camera = null;
        }
    }
    /**
     * reclaim camera, used usually on application onPause()
     */
    protected Camera reclaimCamera() {
        logger.debug("preview.reclaimCamera()");
        if ( camera == null ) {
            camera = getCameraInstance();
            if ( camera == null ) {
                Toast.makeText(getContext(), "Unable to obtain camera", Toast.LENGTH_LONG).show();
            }
        }
        return camera;
    }

}
