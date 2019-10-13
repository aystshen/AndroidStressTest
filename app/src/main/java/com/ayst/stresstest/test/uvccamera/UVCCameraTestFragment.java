package com.ayst.stresstest.test.uvccamera;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseCountTestWithTimerFragment;
import com.ayst.stresstest.test.base.TestType;
import com.serenegiant.encoder.MediaMuxerWrapper;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class UVCCameraTestFragment extends BaseCountTestWithTimerFragment
        implements CameraDialog.CameraDialogParent {

    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     * by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private static final boolean USE_SURFACE_ENCODER = false;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_WIDTH = 640;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_HEIGHT = 480;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 1;

    private static final int DEFAULT_INTERVAL = 15 * 1000;

    @BindView(R.id.spinner_camera)
    Spinner mCameraSpinner;
    @BindView(R.id.edt_interval)
    EditText mIntervalEdt;
    @BindView(R.id.camera_view)
    UVCCameraTextureView mUVCCameraView;
    Unbinder unbinder;

    private boolean mErrorFlag = false;
    private final Object mSync = new Object();
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private Surface mPreviewSurface;
    private UsbDevice mCurUsbDevice;
    private List<UsbDevice> mUsbDevices;
    protected ArrayAdapter<String> mAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.uvccamera_test);
        setType(TestType.TYPE_UVCCAMERA_TEST);

        View contentView = inflater.inflate(R.layout.fragment_uvccamera_test, container, false);
        setContentView(contentView);

        unbinder = ButterKnife.bind(this, contentView);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mUSBMonitor = new USBMonitor(mActivity, mOnDeviceConnectListener);

        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(mActivity,
                com.serenegiant.uvccamera.R.xml.device_filter);
        mUsbDevices = mUSBMonitor.getDeviceList(filter.get(0));

        List<String> cameras = new ArrayList<>();
        for (UsbDevice device : mUsbDevices) {
            cameras.add(device.getVendorId() + ":" + device.getProductId());
        }
        mAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, cameras);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCameraSpinner.setAdapter(mAdapter);

        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float) PREVIEW_HEIGHT);

        mIntervalEdt.setText(DEFAULT_INTERVAL / 1000 + "");
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        synchronized (mSync) {
            releaseCamera();
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }

        super.onDestroy();
    }

    @Override
    public void onStartClicked() {
        if (!isRunning()) {
            int index = mCameraSpinner.getSelectedItemPosition();
            if (index >= 0 && index < mUsbDevices.size()) {
                mCurUsbDevice = mUsbDevices.get(index);
            }

            if (null == mCurUsbDevice) {
                showToast(R.string.uvccamera_test_disconnect_tips);
                return;
            }

            String intervalStr = mIntervalEdt.getText().toString();
            if (TextUtils.isEmpty(intervalStr) || Integer.parseInt(intervalStr) * 1000 < DEFAULT_INTERVAL) {
                showToast(R.string.camera_test_interval_invalid_tips);
                return;
            }
            setPeriod(Integer.parseInt(intervalStr) * 1000);
        }

        super.onStartClicked();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        showCameraSurfaceView(false);

        mCurUsbDevice = null;
        releaseCamera();
        synchronized (mSync) {
            mUSBMonitor.unregister();
        }

        super.stop();
    }

    @Override
    protected boolean testOnce() {
        showCameraSurfaceView(true);

        mErrorFlag = true;
        synchronized (mSync) {
            mUSBMonitor.register();
            mUSBMonitor.requestPermission(mCurUsbDevice);
        }

        try {
            Thread.sleep(mPeriod - 3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        releaseCamera();
        synchronized (mSync) {
            mUSBMonitor.unregister();
        }

        showCameraSurfaceView(false);
        return !mErrorFlag;
    }

    @Override
    public boolean isSupport() {
        return (null != mUsbDevices) && !mUsbDevices.isEmpty();
    }

    private void showCameraSurfaceView(final boolean show) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mUVCCameraView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }

    private synchronized boolean openCamera(final UsbControlBlock ctrlBlock) {
        try {
            final UVCCamera camera = new UVCCamera();
            camera.open(ctrlBlock);

            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }

            try {
                camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
            } catch (final IllegalArgumentException e) {
                // fallback to YUV mode
                try {
                    camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                } catch (final IllegalArgumentException e1) {
                    camera.destroy();
                    Log.e(TAG, "setPreviewSize error: " + e1.getMessage());
                    return false;
                }
            }

            final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
            if (st != null) {
                mPreviewSurface = new Surface(st);
                camera.setPreviewDisplay(mPreviewSurface);
                camera.startPreview();
            }
            synchronized (mSync) {
                mUVCCamera = camera;
            }
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "openCamera error: " + e.getMessage());
            return false;
        }

        return true;
    }

    private synchronized void releaseCamera() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                try {
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCamera = null;
            }
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
        }
    }

    public boolean capture(final String path) {
        Log.i(TAG, "capture...");
        try {
            final Bitmap bitmap = mUVCCameraView.captureStillImage();
            if (validBitmap(bitmap)) {
                // get buffered output stream for saving a captured still image as a file on external storage.
                // the file name is came from current time.
                // You should use extension name as same as CompressFormat when calling Bitmap#compress.
                final File outputFile = TextUtils.isEmpty(path)
                        ? MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".png")
                        : new File(path);
                final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
                try {
                    try {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.flush();
                        MediaScannerConnection.scanFile(mActivity, new String[]{path}, null, null);
                    } catch (final IOException e) {
                        Log.e(TAG, "capture, Bitmap to png error: " + e.getMessage());
                        return false;
                    }
                } finally {
                    os.close();
                }
            } else {
                Log.e(TAG, "capture, bitmap is invalid");
                return false;
            }
        } catch (final Exception e) {
            Log.e(TAG, "capture, error: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Whether all the pixel data is the same by bitmap to determine
     * whether it is a valid bitmap
     * @param bitmap
     * @return
     */
    private boolean validBitmap(Bitmap bitmap) {
        if (null != bitmap && !bitmap.isRecycled()) {
            int bytes = bitmap.getByteCount();
            if (bytes > 0) {
                ByteBuffer buf = ByteBuffer.allocate(bytes);
                bitmap.copyPixelsToBuffer(buf);
                byte[] byteArray = buf.array();
                byte preByte = byteArray[0];
                for (int i = 0; i < bytes; ) {
                    if (byteArray[i] != preByte) {
                        return true;
                    }
                    preByte = byteArray[i];
                    i += bytes / 20;
                }
            } else {
                Log.w(TAG, "validBitmap, Bitmap byte length is 0.");
            }
        } else {
            Log.w(TAG, "validBitmap, Bitmap is null.");
        }
        return false;
    }

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Log.i(TAG, "onAttach, " + device.getVendorId() + ":" + device.getProductId());
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            Log.i(TAG, "onConnect, " + device.getVendorId() + ":" + device.getProductId());
            if (null != mCurUsbDevice
                && device.getVendorId() == mCurUsbDevice.getVendorId()
                && device.getProductId() == mCurUsbDevice.getProductId()) {
                mErrorFlag = false;
                releaseCamera();
                if (openCamera(ctrlBlock)) {
                    mErrorFlag = !capture("");
                } else {
                    mErrorFlag = true;
                }
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            Log.i(TAG, "onDisconnect, " + device.getVendorId() + ":" + device.getProductId());
            releaseCamera();
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Log.i(TAG, "onDettach, " + device.getVendorId() + ":" + device.getProductId());
        }

        @Override
        public void onCancel(final UsbDevice device) {
            Log.i(TAG, "onCancel, " + device.getVendorId() + ":" + device.getProductId());
        }
    };

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {

    }
}
