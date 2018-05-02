package camerplugin.zsoftware.com.videoz.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

import java.io.IOException;

import camerplugin.zsoftware.com.videoz.R;
import camerplugin.zsoftware.com.videoz.event.CameraViewListener;
import camerplugin.zsoftware.com.videoz.utils.CameraHelper;

/**
 * Created by coolzlay on 2018/4/29 0029.
 */

public class CameraView extends SurfaceView implements Callback,
        Camera.PreviewCallback {
    private static final String TAG = CameraView.class.getName();

    private Context supperContext;
    private SurfaceHolder mHolder;


    private CameraViewListener _cameraViewListener;
    private Camera cameraInstance;

    private Bitmap bitmapFocus; //对焦图片
    private FocusView focusView;//对焦显示框
    private int screenWidth; // 屏幕宽度
    private int screenHeight; // 屏幕高度


    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        supperContext = context;
        initHolder();




    }

    public void setCameraRecordListener(CameraViewListener cameraRecordListener) {
        _cameraViewListener = cameraRecordListener;
    }

    int _cameraDirection = Camera.CameraInfo.CAMERA_FACING_FRONT;

    /**
     * 更换摄像头方向
     */
    public void changeCamera() {

        if (CameraHelper.getCameraDirection() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            _cameraDirection = Camera.CameraInfo.CAMERA_FACING_BACK;
        } else {
            _cameraDirection = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        CameraHelper.releaseCamera();
        cameraInstance = null;
        initCamera(_cameraDirection);
    }


    protected void initHolder() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        if (mHolder == null) {
            mHolder = getHolder();
            mHolder.setFormat(PixelFormat.TRANSPARENT);
            mHolder.setKeepScreenOn(true); //屏幕常亮
            mHolder.addCallback(CameraView.this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        if (bitmapFocus == null) {
            bitmapFocus = BitmapFactory.decodeResource(getResources(),
                    R.drawable.box_recorder_focus);
        }

    }


    public void startPreview() {
        CameraHelper.releaseCamera();
        cameraInstance = null;
        initCamera(CameraHelper.getCameraDirection());
//        initHolder();
        this.setVisibility(View.VISIBLE);
    }

    public void stopPreview() {
        if (mHolder != null) {
            mHolder.removeCallback(this);
            mHolder = null;
        }
        CameraHelper.releaseCamera();
        cameraInstance = null;
        this.setVisibility(View.GONE);
    }

    /**
     * 初始化相机
     */
    private void initCamera(int cameraDirection) {
        cameraInstance = CameraHelper.getCameraInstance(cameraDirection, supperContext);
        if (cameraInstance != null) {
            cameraInstance.setDisplayOrientation(90);
            if (mHolder == null) {
                initHolder();
            }

            final CameraView cameraView_tmp = this;

            // 有些手机要在Holder初始化完才能设置响应
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    try {
                        cameraInstance.setPreviewCallback(CameraView.this);
                        cameraInstance.setPreviewDisplay(mHolder);
                        cameraInstance.startPreview();
                        Camera.Size cameraPreviewSize = CameraHelper.getCameraPreviewSize();
                        _cameraViewListener.CameraViewReady(cameraPreviewSize.width, cameraPreviewSize.height);

                        int w = screenHeight *  cameraPreviewSize.height /cameraPreviewSize.width;
                        int margin = (screenWidth - w) / 2;
                        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                RelativeLayout.LayoutParams.MATCH_PARENT);
                        lp.setMargins(margin, 0, margin, 0);
                        cameraView_tmp.setLayoutParams(lp);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 500);


        }
    }




//    private void touch2Focus(int x, int y) {
//        Rect focusRect = CameraHelper.getFocusArea(x, y, screenWidth,
//                screenWidth, 300);
//        List<Camera.Area> areas = new ArrayList<Camera.Area>();
//        areas.add(new Camera.Area(focusRect, 1000));
//        Camera.Parameters cameraParameters = CameraHelper.getCameraParameters();
//        if (cameraParameters.getMaxNumFocusAreas() > 0) {
//            cameraParameters.setFocusAreas(areas);// 设置对焦区域
//        }
//        if (cameraParameters.getMaxNumMeteringAreas() > 0) {
//            cameraParameters.setMeteringAreas(areas);// 设置测光区域
//        }
//        if (cameraInstance != null) {
//            cameraInstance.cancelAutoFocus();
//            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//            try {
//                cameraInstance.setParameters(cameraParameters);
//                cameraInstance.autoFocus(null);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//
//    }

    // 触摸定点对焦
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        RelativeLayout root = (RelativeLayout) this.getParent();
//        if (cameraInstance == null) {
//            return true;
//        }
//        if (mHolder == null) {
//            return true;
//        }
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN: //按下
//                int fx = (int) event.getX();
//                int fy = (int) event.getY();
//                int w = bitmapFocus.getWidth(),
//                        h = bitmapFocus.getHeight();
//                fx = CameraHelper.clamp(fx, w / 2, screenWidth - w / 2);
//                fy = CameraHelper.clamp(fy, h / 2, screenHeight - h / 2);
//                focusView = new FocusView(supperContext, fx - w / 2, fy
//                        - h / 2, bitmapFocus);
//
//                //移除对焦
//                CameraHelper.removeFocusView(root, focusView);
//                CameraHelper.addFocusView(root, focusView);
//                touch2Focus(fx, fy);
//                break;
//            case MotionEvent.ACTION_UP:
//                try {
//                    Thread.sleep(800);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                CameraHelper.removeFocusView(root, focusView);
//                break;
//        }
//        return true;
//    }


    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

        if (_cameraViewListener != null) {
            byte[] tempData = null;
            Camera.Size cameraPreviewSize = CameraHelper.getCameraPreviewSize();
            _cameraViewListener.onPreviewFrame(bytes, camera, cameraPreviewSize.width, cameraPreviewSize.height);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        handleSurfaceChanged(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        CameraHelper.releaseCamera();
        cameraInstance = null;
    }

    private void handleSurfaceChanged(CameraView view) {

    }

    @Override
    protected void onDetachedFromWindow() {
        if (mHolder != null) {
            mHolder.removeCallback(this);
            mHolder = null;
        }
        CameraHelper.releaseCamera();
        cameraInstance = null;
        super.onDetachedFromWindow();
    }
}
