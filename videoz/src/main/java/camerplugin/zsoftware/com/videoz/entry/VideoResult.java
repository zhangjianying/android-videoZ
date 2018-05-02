package camerplugin.zsoftware.com.videoz.entry;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.Serializable;

/**
 * Created by coolzlay on 2018/4/28 0028.
 */

public class VideoResult implements Serializable {
    private String videoPath; // 视频文件路径
    private long fileSize;


    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileSizeDesc() {
        return getFileSizeStr(fileSize);
    }

    // 字节转描述
    private static String getFileSizeStr(long fileSize) {
        String sFileSize = "0KB";
        if (fileSize > 0) {
            double dFileSize = (double) fileSize;

            double kiloByte = dFileSize / 1024;
            // if (kiloByte < 1) {
            // return sFileSize + "S";
            // }
            double megaByte = kiloByte / 1024;
            if (megaByte < 1) {
                sFileSize = String.format("%.2f", kiloByte);
                return sFileSize + "K";
            }

            double gigaByte = megaByte / 1024;
            if (gigaByte < 1) {
                sFileSize = String.format("%.2f", megaByte);
                return sFileSize + "M";
            }

            double teraByte = gigaByte / 1024;
            if (teraByte < 1) {
                sFileSize = String.format("%.2f", gigaByte);
                return sFileSize + "G";
            }

            sFileSize = String.format("%.2f", teraByte);
            return sFileSize + "TB";
        }
        return sFileSize;
    }


    public Uri getFileUri(Activity activity) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            return Uri.fromFile(new File(this.getVideoPath()));
        } else {
            //android 7.0 以上
            Uri saveFileUri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", new File(this.getVideoPath()));
            return saveFileUri;
        }
    }
}
