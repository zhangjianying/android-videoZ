package camerplugin.zsoftware.com.videoz.component;

/**
 * Created by coolzlay on 2018/4/30 0030.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import camerplugin.zsoftware.com.videoz.R;
import camerplugin.zsoftware.com.videoz.entry.VideoConfig;
import camerplugin.zsoftware.com.videoz.event.RecordViewListener;
import camerplugin.zsoftware.com.videoz.utils.CameraHelper;
import camerplugin.zsoftware.com.videoz.utils.FileUtils;

import static android.content.Context.MODE_PRIVATE;

/**
 * 录制工具组件
 */
public class RecordView extends SurfaceView implements SurfaceHolder.Callback, MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {
    private static final String TAG = "RecordHelper";
    private static MediaRecorder mRecorder = null;
    private static boolean isRecording = false; // 是否正在录制 状态标识
    private static Context _Context;
    private static SurfaceHolder mHolder = null;
    public final static String STOP_REOCED_ACTION = "RecordView.STOP_RECORDING";


    private Bitmap bitmapFocus; //对焦图片
    private FocusView focusView;//对焦显示框
    private int screenWidth; // 屏幕宽度
    private int screenHeight; // 屏幕高度
    private String _tmpDirName = null;// 临时目录名称
    private SharedPreferences sharedPreferences;
    private Camera cameraInstance = null;
    private RecordViewListener _recordViewListener;
    private int recordTimeSec = 0; //计时器

    //启动延时
    public final static int START_RECORD_DELAY = 200;
    private VideoConfig _videoConfig;
    //计时器
    private android.os.Handler recordingTimehandler = new android.os.Handler();
    private Runnable recordingTimeRunnable = new Runnable() {
        @Override
        public void run() {
            recordTimeSec++;
            if (recordTimeSec >= _maxRecordTime) {
                recordingTimehandler.removeCallbacks(recordingTimeRunnable);
//                stopRecord();
                // 要改成发广播 通知界面层去模拟点击停止按钮
                Intent intent = new Intent(STOP_REOCED_ACTION);
                _Context.sendBroadcast(intent);
                return;
            }


            if (_recordViewListener != null) {
                _recordViewListener.recodingProcess(recordTimeSec, _maxRecordTime);
            }

            recordingTimehandler.postDelayed(this, 1000);
        }
    };

    public void setRecordViewListener(RecordViewListener listener) {
        _recordViewListener = listener;
    }

    public RecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        _Context = context;
        sharedPreferences = _Context.getSharedPreferences(RecordView.class.getSimpleName(), MODE_PRIVATE);
    }

    protected void initHolder() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        if (mHolder == null) {
            mHolder = getHolder();
            mHolder.setFormat(PixelFormat.TRANSPARENT);
            mHolder.setKeepScreenOn(true); //屏幕常亮
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        if (bitmapFocus == null) {
            bitmapFocus = BitmapFactory.decodeResource(getResources(),
                    R.drawable.box_recorder_focus);
        }

    }

    /**
     * 是否正在录制
     *
     * @return
     */
    public static boolean isIsRecording() {
        return isRecording;
    }

    /**
     * 设置录制状态
     *
     * @param isRecording
     */
    public static void setIsRecording(boolean isRecording) {
        RecordView.isRecording = isRecording;
    }

    private static int _maxRecordTime; //最大录制时间


    private static MediaRecorder getMediaRecorderInstance(int maxRecordTime) {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            _maxRecordTime = maxRecordTime;
        }
        return mRecorder;
    }


    public void onPause() {
        Log.i(TAG, "mRecorder onPause");
        if (isIsRecording()) { //录制状态的时候
            sharedPreferences.edit().putBoolean("isRecording", isRecording)
                    .putString("_tmpDirName", _tmpDirName)
                    .putInt("_maxRecordTime", _maxRecordTime)
                    .putInt("recordTimeSec", recordTimeSec)
                    .commit();
            releaseRecorder();
            releaseSurfaceHolder();

            recordingTimehandler.removeCallbacks(recordingTimeRunnable);
        }
    }

    public void onResume() {
        Log.i(TAG, "mRecorder onResume");
        boolean isRecording = sharedPreferences.getBoolean("isRecording", false);
        if (isRecording) {
            this.isRecording = isRecording;
            _tmpDirName = sharedPreferences.getString("_tmpDirName", null);
            _maxRecordTime = sharedPreferences.getInt("_maxRecordTime", -1);
            recordTimeSec = sharedPreferences.getInt("recordTimeSec", 0);
            startRecord(_videoConfig);
        }
    }

    private void releaseSurfaceHolder() {
        if (mHolder != null) {
            mHolder.removeCallback(this);
            mHolder = null;
        }
    }


    public void releaseAll() {
        releaseRecorder();
        releaseSurfaceHolder();
    }

    private void releaseRecorder() {
        if (mRecorder != null) {
            mRecorder.setOnErrorListener(null);
            mRecorder.setPreviewDisplay(null);
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }

        Camera cameraInstance = CameraHelper.getCameraInstance(_Context);
        cameraInstance.stopPreview();
        CameraHelper.releaseCamera();
    }

    public void userExit() {
        sharedPreferences.edit().remove("isRecording")
                .remove("_tmpDirName")
                .remove("_maxRecordTime")
                .remove("recordTimeSec")
                .commit();

        _tmpDirName = null;
        recordTimeSec = 0;
    }

    public void stopRecord() {
        releaseRecorder();


        if (recordingTimehandler != null) {
            recordingTimehandler.removeCallbacks(recordingTimeRunnable);
        }
        setIsRecording(false);
        this.setVisibility(View.GONE);
    }


    /**
     * 删除用户当前记录信息
     */
    public void endRecord() {
        userExit();
    }

    /**
     * 用户确认 停止录制视频
     */
    public void userStopRecordEnd() {
        //搜索本次录制一共有多少个文件
        String path = _videoConfig.getSaveFileDirectory();
        path = path + "/" + _tmpDirName;
        List<String> filePathList = new ArrayList<String>();
        File[] listfile = new File(path).listFiles();
        for (File file : listfile) {
            if (file.isFile()) {
                if (file.getName().toLowerCase().endsWith("mp4")) {
                    filePathList.add(file.getPath());
                }
            }
        }

        if (filePathList.size() > 1) {
            String outMergeMp4 = path + "/" + "merge.mp4";
            try {
                FileUtils.appendMp4List(filePathList, outMergeMp4);
            } catch (IOException e) {
                e.printStackTrace();
            }
            _recordViewListener.endRecord(outMergeMp4);
        } else {
            _recordViewListener.endRecord(filePathList.get(0));
        }
    }

    public void startRecord(VideoConfig _videoConfig) {

        this._videoConfig = _videoConfig;

        //判断是否还可以继续录制

        if (recordTimeSec > _maxRecordTime) {
            return;
        }


        this.setVisibility(View.VISIBLE);

        //不把 开启时候 嘀的一声录入
        ((AudioManager) _Context.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_SYSTEM, true);
        _maxRecordTime = _videoConfig.getMaxRecordTime();
        if (_tmpDirName == null) {
            _tmpDirName = FileUtils.getDateTimeFileName();
        }
        initHolder();

        // 这里必须等surface初始化好 ,
        //  这个地方我承认 有点考运气 . 没找到更好的办法
        new Handler().postDelayed(new Runnable() {
                                      @Override
                                      public void run() {
                                          startMediaRecoder();
                                          setIsRecording(true);

                                          new Handler().postDelayed(new Runnable() {
                                              public void run() {
                                                  recordingTimehandler.post(recordingTimeRunnable);
                                              }
                                          }, 1000);
                                      }
                                  }
                , START_RECORD_DELAY);


    }

    private void startMediaRecoder() {
        mRecorder = getMediaRecorderInstance(_maxRecordTime);
        mRecorder.reset();
        cameraInstance = CameraHelper.getCameraInstance(_Context);
        cameraInstance.setDisplayOrientation(90);
        cameraInstance.setParameters(CameraHelper.OptimizationCameraParams(cameraInstance.getParameters(), _Context));
        cameraInstance.unlock();
        mRecorder.setCamera(cameraInstance);
        mRecorder.setOnInfoListener(this);
        mRecorder.setOnErrorListener(this);

        mRecorder.setPreviewDisplay(mHolder.getSurface());

//        // 这两项需要放在setOutputFormat之前
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        // Set output file format
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        // 这两项需要放在setOutputFormat之后
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

//        if (CameraHelper.getCameraPreviewSize() != null) {
            Log.i(TAG, "设置的分辨率:" + CameraHelper.getCameraPreviewSize().width + " : " + CameraHelper.getCameraPreviewSize().height);
            //这里必须做修正  容易挂  必须按 宽高 比例 3/2
            //fix
            // 有些奇葩机型 不会返回摄像头支持尺寸 或者 返回一个很小的值 比如:160 :120
            // 喂 不用看了 miui 9.5说的就是你. 就你妈的秀
            // 摄像头数据翻转90度数 以后 有些rom 会自动转化 有些rom需要 高宽对调 艹~
            mRecorder.setVideoSize(640, 480);
//            mHolder.setFixedSize(screenWidth, screenHeight);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {  // > android 7.0
//                mRecorder.setVideoSize(960, 720);
//                mHolder.setFixedSize(960, 720);
//            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                // android 4.4w ~ android 6.0
//                mRecorder.setVideoSize(720, 540);
//                mHolder.setFixedSize(720, 540);
//            } else {
//                mRecorder.setVideoSize(640, 480);
//                mHolder.setFixedSize(640, 480);
//            }
//        }


        int w = screenHeight * 480 / 640;
        int margin = (screenWidth - w) / 2;
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(margin, 0, margin, 0);
        this.setLayoutParams(lp);

        mRecorder.setVideoEncodingBitRate(this._videoConfig.getVideoEncodingBitRate());
        mRecorder.setAudioEncodingBitRate(44100);
        mRecorder.setAudioChannels(1);
        if (cameraInstance != null) {
            //如果是前置摄像头
            if (CameraHelper.getCameraDirection() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mRecorder.setOrientationHint(270);
            }
            if (CameraHelper.getCameraDirection() == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mRecorder.setOrientationHint(90);
            }
        }
        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        //设置记录会话的最大持续时间（毫秒）
        mRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        mRecorder.setMaxDuration(_maxRecordTime * 1000);
        String videoFileName = null;
//        mMediaRecorder.setMaxFileSize(0);
        String path = _videoConfig.getSaveFileDirectory();
        if (path != null) {
            //fix
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdir();
            }

            //目录名称
            File tmpDirNameFile = new File(path + _tmpDirName);
            if (!tmpDirNameFile.exists()) {
                tmpDirNameFile.mkdir();
            }
            //视频保存文件名称
            videoFileName = FileUtils.getDateTimeFileName() + ".mp4";
            path = dir + "/" + _tmpDirName + "/" + videoFileName;
            mRecorder.setOutputFile(path);


        }
        try {

            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setIsRecording(true); // 开始录制
        _recordViewListener.startRecord(videoFileName);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("aa", "aaa");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        holder = null;
        this.mHolder = null;
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {

        Log.e(TAG, what + "");
        try {
            if (mr != null)
                mr.reset();
        } catch (IllegalStateException e) {
            Log.w(TAG, "stopRecord", e);
        } catch (Exception e) {
            Log.w(TAG, "stopRecord", e);
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        Log.i(TAG, what + "");
    }

    @Override
    protected void onDetachedFromWindow() {
        releaseAll();
        CameraHelper.releaseCamera();
        super.onDetachedFromWindow();
    }


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
//                focusView = new FocusView(_Context, fx - w / 2, fy
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

}
