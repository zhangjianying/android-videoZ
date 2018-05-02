package camerplugin.zsoftware.com.videoz;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import camerplugin.zsoftware.com.videoz.component.CameraView;
import camerplugin.zsoftware.com.videoz.component.RecordView;
import camerplugin.zsoftware.com.videoz.entry.VideoConfig;
import camerplugin.zsoftware.com.videoz.entry.VideoResult;
import camerplugin.zsoftware.com.videoz.event.CameraViewListener;
import camerplugin.zsoftware.com.videoz.event.RecordViewListener;
import camerplugin.zsoftware.com.videoz.utils.FileUtils;


/**
 * Created by coolzlay on 2018/4/29 0029.
 */

public class SmallVideoActivity extends Activity implements CameraViewListener, View.OnClickListener, RecordViewListener {
    private static final String TAG = SmallVideoActivity.class.getName();

    private RecordView _recordView;
    private CameraView _cameraView;

    private LinearLayout back_btn;
    private ImageView _switch_camera_btn; // 切换摄像头
    private ImageView _video_start_btn; //开始或者停止
    private TextView recordTime_text; //计时器 显示组件
    private ImageView re_record_btn; //重新录制组件
    private ImageView upload_btn; //去上传组件
    private TextView recording_text;

    private boolean immersiveMode; //沉浸式布局窗体  更好的用户体验

    //参数传递
    public final static String RECORDER_CONFIG_KEY = "RECORDER_CONFIG_KEY_7889000";
    public final static int RECORDER_RESULT_CODE = 1234;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            recordStop();
            //显示重新录制或者去上传
            re_record_btn.setVisibility(View.VISIBLE);
            upload_btn.setVisibility(View.VISIBLE);
            //结束录制视频要显示 切换摄像头

            recording_text.setVisibility(View.GONE);
            _video_start_btn.setVisibility(View.GONE);

        }
    };
    private VideoConfig _videoConfig; //输入参数

    private VideoResult _videoResult; //返回结果

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.smallvideo_main);

        // 设置竖屏显示
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // 选择支持半透明模式,在有surfaceview的activity中使用。
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            /**
             * 好处:
             *  只有自己界面定义的按钮可以操作 不会被第三方消息打扰
             */
//            immersiveMode = true;
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        Intent intent = getIntent();
        VideoConfig videoConfig = (VideoConfig) intent.getSerializableExtra(RECORDER_CONFIG_KEY);
        if (videoConfig == null) {
            //默认值
            _videoConfig = new VideoConfig();
            _videoConfig.setMaxRecordTime(60);
            _videoConfig.setVideoEncodingBitRate(1500 * 1024);
        } else {
            //从Intent 中拿到的视频参数变量
            _videoConfig = videoConfig;
        }

        //初始化摄像头布幕
        _cameraView = (CameraView) findViewById(R.id.cameraView);
        _cameraView.setCameraRecordListener(this);

        _recordView = (RecordView) findViewById(R.id.recordView);
        _recordView.setRecordViewListener(this);

        // 切换摄像头
        _switch_camera_btn = (ImageView) findViewById(R.id.switch_camera_btn);
        _switch_camera_btn.setOnClickListener(this);

        // 开始或停止
        _video_start_btn = (ImageView) findViewById(R.id.video_start_btn);
        _video_start_btn.setOnClickListener(this);


        //回退按钮
        back_btn = (LinearLayout) findViewById(R.id.back_btn);
        back_btn.setOnClickListener(this);

        //记时显示面板
        recordTime_text = (TextView) findViewById(R.id.recordTime_text);

        //重新录制
        re_record_btn = (ImageView) findViewById(R.id.re_record_btn);
        re_record_btn.setOnClickListener(this);

        //去上传
        upload_btn = (ImageView) findViewById(R.id.upload_btn);
        upload_btn.setOnClickListener(this);

        //正在录制的提示
        recording_text = (TextView) findViewById(R.id.recording_text);


        //默认情况下不显示  重新录制 与 去上传组件
        re_record_btn.setVisibility(View.GONE);
        upload_btn.setVisibility(View.GONE);
        recording_text.setVisibility(View.GONE);

        //开始渲染 摄像头面板
        _cameraView.startPreview();


        IntentFilter filter = new IntentFilter(RecordView.STOP_REOCED_ACTION);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        //如果没开启录制
        if (_recordView.isIsRecording() == false) {
            _cameraView.stopPreview();
        } else {
            _recordView.onPause();
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        //如果没开启录制
        if (_recordView.isIsRecording() == false) {
            _cameraView.startPreview();
        } else {
            _recordView.onResume();
        }

        super.onResume();
    }

    /**
     * 摄像头通知
     * 如果需要实施推流 可以考虑扩展这里
     *
     * @param bytes
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera, int PreviewWidth, int PreviewHeight) {
    }

    @Override
    public void CameraViewReady(int PreviewWidth, int PreviewHeight) {

    }

    private void ExitActivity() {
        if (!_recordView.isIsRecording()) { //如果没有录制
            finish();
            return;
        }

        new AlertDialog.Builder(this).setTitle("确认退出吗？")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“确认”后的操作
                        _recordView.userExit();
                        finish();

                    }
                })
                .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“返回”后的操作,这里不设置没有任何操作
                    }
                }).show();
    }

    private void recordStart(VideoConfig _videoConfig) {

        _cameraView.stopPreview();
        _recordView.startRecord(_videoConfig);
        cancelBtnClick();
    }


    /**
     * 注销 开始按钮的事件处理.
     * 做几秒延时再注册上. 反正用户快速点击造成资源还没有初始化好就停止
     */
    private void cancelBtnClick() {
        final SmallVideoActivity _activityTemp = this;
        _video_start_btn.setOnClickListener(null);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                _video_start_btn.setOnClickListener(_activityTemp);
            }
        }, RecordView.START_RECORD_DELAY + 3000); // 做三秒延时再恢复
    }

    private void recordStop() {
        // 停止录制

        _recordView.stopRecord();
        _cameraView.startPreview();
        cancelBtnClick();
    }

    @Override
    public void onBackPressed() {
        ExitActivity();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        // 切换摄像头
        if (id == R.id.switch_camera_btn) {
            _cameraView.changeCamera();
        } else if (id == R.id.video_start_btn) {
            if (_recordView.isIsRecording() == false) {
                //开始录制
                recordStart(_videoConfig);
                //开始录制视频要隐藏 切换摄像头
                _switch_camera_btn.setVisibility(View.GONE);
                recording_text.setVisibility(View.VISIBLE);
                _video_start_btn.setImageResource(R.drawable.record_pause_2);

                re_record_btn.setVisibility(View.GONE);
                upload_btn.setVisibility(View.GONE);

            } else {
                recordStop();
                //显示重新录制或者去上传
                re_record_btn.setVisibility(View.VISIBLE);
                upload_btn.setVisibility(View.VISIBLE);
                //结束录制视频要显示 切换摄像头

                recording_text.setVisibility(View.GONE);
                _video_start_btn.setImageResource(R.drawable.record_play);

            }


        } else if (id == R.id.back_btn) { // 退出按钮
            ExitActivity();
        } else if (id == R.id.re_record_btn) { // 重新录制按钮
            new AlertDialog.Builder(this).setTitle("放弃这段录制视频吗？")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 点击“确认”后的操作

                            _recordView.userExit();

                            recordTime_text.setText(FileUtils.gennerTime(0));
                            re_record_btn.setVisibility(View.GONE);
                            upload_btn.setVisibility(View.GONE);
                            _video_start_btn.setImageResource(R.drawable.record_play);
                            _video_start_btn.setVisibility(View.VISIBLE);
                            _switch_camera_btn.setVisibility(View.VISIBLE);
                        }
                    })
                    .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 点击“返回”后的操作,这里不设置没有任何操作
                        }
                    }).show();
        } else if (id == R.id.upload_btn) { //去上传

            _recordView.userStopRecordEnd();
            Intent intent = new Intent();
            if (_videoResult != null) {
                intent.putExtra(RECORDER_CONFIG_KEY, _videoResult);
                setResult(0, intent);
            }
            ExitActivity();
        }
    }

    /**
     * 开始录制
     *
     * @param RecordFileName
     */
    @Override
    public void startRecord(String RecordFileName) {

        Log.i(TAG, "开始录制视频: 视频文件:" + RecordFileName);
        _videoResult = null;

    }


    /**
     * 录制进度
     *
     * @param currentSec
     * @param maxSec
     */
    @Override
    public void recodingProcess(int currentSec, int maxSec) {
        recordTime_text.setText(FileUtils.gennerTime(currentSec));
    }


    /**
     * 录制结束
     *
     * @param filePath
     */
    @Override
    public void endRecord(String filePath) {
        Log.i(TAG, "结束录制视频: 视频文件:");
        Log.i(TAG, "视频文件:" + filePath);


        _videoResult = new VideoResult();
        _videoResult.setVideoPath(filePath);
        _videoResult.setFileSize(new File(filePath).length());

    }


    /**
     * 沉浸式 最大化窗体
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && immersiveMode) {
            final int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }


}
