package camerplugin.zsoftware.com.cameraplugin;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.util.List;

import camerplugin.zsoftware.com.videoz.SmallVideoActivity;
import camerplugin.zsoftware.com.videoz.entry.VideoConfig;
import camerplugin.zsoftware.com.videoz.entry.VideoResult;
import pub.devrel.easypermissions.EasyPermissions;

@ContentView(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private static final String TAG = MainActivity.class.getName();

    @ViewInject(R.id.testBtn)
    Button testButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        x.view().inject(this);
        //所要申请的权限
        String[] perms = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.WAKE_LOCK
        };


        if (EasyPermissions.hasPermissions(this, perms)) {//检查是否获取该权限
            Log.i(TAG, "已获取权限");
        } else {
            EasyPermissions.requestPermissions(this, "必要的权限", 0, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //把申请权限的回调交由EasyPermissions处理
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    //下面两个方法是实现EasyPermissions的EasyPermissions.PermissionCallbacks接口
    //分别返回授权成功和失败的权限
    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.i(TAG, "获取成功的权限" + perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.i(TAG, "获取失败的权限" + perms);
    }


    /*
       启动录屏Activity
    */
    @Event(type = View.OnClickListener.class, value = R.id.testBtn)
    private void startVideo2(View v) {
        Intent intent = new Intent();
        //设置录像参数
        VideoConfig config = new VideoConfig();
        config.setMaxRecordTime(180); //最大录制时间
        config.setVideoEncodingBitRate(4000 * 1024); //视频码率

        intent.putExtra(SmallVideoActivity.RECORDER_CONFIG_KEY, config);
        intent.setClass(this, SmallVideoActivity.class);
        //设置返回时获取的接收码
        startActivityForResult(intent, SmallVideoActivity.RECORDER_RESULT_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SmallVideoActivity.RECORDER_RESULT_CODE) {
            if (data == null) {
                //未正常返回
                return;
            }
            if (resultCode == 0) { //获取录制返回结果
                final VideoResult videoResult = (VideoResult) data.getSerializableExtra(SmallVideoActivity.RECORDER_CONFIG_KEY);
                Toast.makeText(this, videoResult.getVideoPath() + " 文件大小:" + videoResult.getFileSizeDesc(), Toast.LENGTH_LONG).show();

                //3秒后调用系统播放器 播放录制的视频
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        startPlayer(videoResult);
                    }
                }, 3000);

            }

        }
    }


    private void startPlayer(VideoResult videoResult) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = videoResult.getFileUri(this);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }

        intent.setDataAndType(uri, "video/*");
        startActivity(intent);
    }
}
