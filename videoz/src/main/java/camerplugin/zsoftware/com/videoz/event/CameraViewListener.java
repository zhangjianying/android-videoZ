package camerplugin.zsoftware.com.videoz.event;

import android.hardware.Camera;

/**
 * 摄像头开始录制事件通知
 * Created by coolzlay on 2018/4/29 0029.
 */

public interface CameraViewListener {
    //摄像头开启录制通知
    public void onPreviewFrame(byte[] bytes, Camera camera, int PreviewWidth, int PreviewHeight);

    public void CameraViewReady(int PreviewWidth, int PreviewHeight);
}
