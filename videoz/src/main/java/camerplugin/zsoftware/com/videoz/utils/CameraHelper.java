package camerplugin.zsoftware.com.videoz.utils;

/**
 * Created by coolzlay on 2018/4/29 0029.
 */

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import camerplugin.zsoftware.com.videoz.component.FocusView;

public class CameraHelper {

    private static Camera camera = null;
    private static Camera.Parameters cameraParams = null;
    private static Size cameraPreviewSize;
    private static int _cameraDirection = Camera.CameraInfo.CAMERA_FACING_BACK;


    public static int getCameraDirection() {
        return _cameraDirection;
    }

    public static Size getCameraPreviewSize() {
        return cameraPreviewSize;
    }

    /**
     * 获取相机参数
     *
     * @return
     */
    public static Camera.Parameters getCameraParameters() {
        return cameraParams;
    }

    public synchronized static Camera getCameraInstance(Context context) {
        return getCameraInstance(_cameraDirection, context);
    }

    // 单例模式获取相机
    public synchronized static Camera getCameraInstance(int cameraDirection, Context context) {
        if (camera != null) {
            releaseCamera();
        }
        if (camera == null) {
            camera = Camera.open(cameraDirection);
            _cameraDirection = cameraDirection;
        }
        if (camera != null) {

            //优化相机参数
            cameraParams = OptimizationCameraParams(camera.getParameters(), context);
            camera.setParameters(cameraParams);
        }
        return camera;
    }

    /**
     * 释放相机资源
     */
    public synchronized static void releaseCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            try {
                camera.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    /**
     * 根据预设宽高获取匹配的相机分辨率,若没有则返回中间值
     *
     * @param camera 相机
     * @return
     */
    public static Size getOptimalPreviewSize(Camera camera, Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        Size previewSize = null;
        List<Size> supportedSizes = camera.getParameters()
                .getSupportedPreviewSizes();
        Collections.sort(supportedSizes, new SizeComparator());
        if (null != supportedSizes && supportedSizes.size() > 0) {
            boolean hasSize = false;
            for (Size size : supportedSizes) {
                Log.d("wzy.size", "当前手机支持的分辨率：" + size.width + "*" + size.height);
                if (null != size && size.width == screenWidth
                        && size.height == screenHeight) {
                    previewSize = size;
                    hasSize = true;
                    break;
                }
            }
            if (!hasSize) {
                previewSize = supportedSizes.get(supportedSizes.size() / 2);
            }
        }
        return previewSize;
    }

    private static class SizeComparator implements Comparator<Size> {
        @Override
        public int compare(Size size1, Size size2) {
            if (size1.height != size2.height)
                return size1.height - size2.height;
            else
                return size1.width - size2.width;
        }
    }

    /**
     * 将屏幕坐标系转化成对焦坐标系,返回要对焦的矩形框
     *
     * @param x        横坐标
     * @param y        纵坐标
     * @param w        相机宽度
     * @param h        相机高度
     * @param areaSize 对焦区域大小
     * @return
     */
    public static Rect getFocusArea(int x, int y, int w, int h, int areaSize) {
        int centerX = x / w * 2000 - 1000;
        int centerY = y / h * 2000 - 1000;
        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int right = clamp(left + areaSize, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        int bottom = clamp(top + areaSize, -1000, 1000);
        return new Rect(left, top, right, bottom);
    }

    /**
     * 限定x取值范围为[min,max]
     *
     * @param x
     * @param min
     * @param max
     * @return
     */
    public static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    /**
     * 移除对焦框
     *
     * @param rootView
     * @param fouceView
     */
    public static void removeFocusView(RelativeLayout rootView, FocusView fouceView) {
        for (int i = 0; i < rootView.getChildCount(); i++) {
            if (fouceView == rootView.getChildAt(i)) {
                rootView.removeViewAt(i);
                break;
            }
        }
    }

    public static void addFocusView(RelativeLayout rootView, FocusView fouceView) {
        rootView.addView(fouceView, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
    }

    /**
     * 优化相机参数
     */
    public static Camera.Parameters OptimizationCameraParams(Camera.Parameters cameraParams, Context context) {
        //设置自动连续对焦
        String mode = getAutoFocusMode(cameraParams);
        if (StringUtils.isNotEmpty(mode)) {
            cameraParams.setFocusMode(mode);
        }

        List<String> focusModes = cameraParams.getSupportedFocusModes();
        if(focusModes.contains("continuous-video")){
            cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        //白平衡补偿
        if (isSupported(cameraParams.getSupportedWhiteBalance(), "auto"))
            cameraParams.setWhiteBalance("auto");

        //是否支持视频防抖
        if ("true".equals(cameraParams.get("video-stabilization-supported")))
            cameraParams.set("video-stabilization", "true");

        if (!DeviceUtils.isDevice("GT-N7100", "GT-I9308", "GT-I9300")) {
            cameraParams.set("cam_mode", 1);
            cameraParams.set("cam-mode", 1);
        }

        //缩短Recording启动时间  不要开启 出鬼 华为机型居多,画面会自动抖动
        // cameraParams.setRecordingHint(true);
        //是否支持影像稳定能力，支持则开启
        if (cameraParams.isVideoStabilizationSupported()) {
            cameraParams.setVideoStabilization(true);
        }
        cameraParams.setPreviewFormat(ImageFormat.YV12);

        cameraPreviewSize = CameraHelper.getOptimalPreviewSize(camera, context);
        if (cameraPreviewSize != null) {
            cameraParams.setPreviewSize(cameraPreviewSize.width, cameraPreviewSize.height);
        }
        return cameraParams;
    }

    /**
     * 连续自动对焦
     */
    private static String getAutoFocusMode(Camera.Parameters cameraParams) {
        if (cameraParams != null) {
            //持续对焦是指当场景发生变化时，相机会主动去调节焦距来达到被拍摄的物体始终是清晰的状态。
            List<String> focusModes = cameraParams.getSupportedFocusModes();
            if ((Build.MODEL.startsWith("GT-I950") || Build.MODEL.endsWith("SCH-I959") || Build.MODEL.endsWith("MEIZU MX3")) && isSupported(focusModes, "continuous-picture")) {
                return "continuous-picture";
            } else if (isSupported(focusModes, "continuous-video")) {
                return "continuous-video";
            } else if (isSupported(focusModes, "auto")) {
                return "auto";
            }
        }
        return null;
    }

    /**
     * 检测是否支持指定特性
     */
    private static boolean isSupported(List<String> list, String key) {
        return list != null && list.contains(key);
    }
}
