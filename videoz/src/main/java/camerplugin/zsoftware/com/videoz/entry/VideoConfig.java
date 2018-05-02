package camerplugin.zsoftware.com.videoz.entry;

import android.os.Environment;

import java.io.File;
import java.io.Serializable;

/**
 * Created by coolzlay on 2018/4/30 0030.
 */

public class VideoConfig implements Serializable {
    private final static int MIN_RECODETIME = 6; // 最少录制的时间 秒


    private int MaxRecordTime;
    private String saveFileDirectory = "smallVideo";

    private int VideoEncodingBitRate = 5000 * 1000; // 视频编码率


    /**
     * 视频编码率
     *
     * @return
     */
    public int getVideoEncodingBitRate() {
        return VideoEncodingBitRate;
    }

    /**
     * 视频编码率
     *
     * @param videoEncodingBitRate
     */
    public void setVideoEncodingBitRate(int videoEncodingBitRate) {
        VideoEncodingBitRate = videoEncodingBitRate;
    }

    /**
     * 获取最大录制时间 单位秒
     *
     * @return
     */
    public int getMaxRecordTime() {
        return MaxRecordTime;
    }

    /**
     * 最大录制时间
     *
     * @param maxRecordTime 单位秒
     */
    public void setMaxRecordTime(int maxRecordTime) {
        maxRecordTime = Math.max(MIN_RECODETIME, maxRecordTime); //最少录制6秒
        MaxRecordTime = maxRecordTime;
    }

    /**
     * 视频保存目录  没有必要提供设置方法.
     * android 7.0以上存储目录必须与xml external-path 中描述一致
     * 因此不提供动态设置方法
     * 这里要注意与 video_paths.xml 中的path值一致
     *
     * @return
     */
    public String getSaveFileDirectory() {
        String sdcardPath = Environment
                .getExternalStorageDirectory().getPath();
        String path = sdcardPath + "/" + saveFileDirectory + "/";
        File saveDirectory = new File(path);
        if (!saveDirectory.exists()) {
            saveDirectory.mkdir();
        }
        return path;
    }
}
