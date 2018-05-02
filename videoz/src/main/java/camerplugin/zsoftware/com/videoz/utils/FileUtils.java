package camerplugin.zsoftware.com.videoz.utils;

import android.os.Environment;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by coolzlay on 2018/4/27 0027.
 */

public class FileUtils {
    private static final String DEFAULT_FILE_DIR_DATETIME_PATTERN = "yyyyMMdd_hhmmss";

    /**
     * 对Mp4文件集合进行追加合并(按照顺序一个一个拼接起来)
     *
     * @param mp4PathList [输入]Mp4文件路径的集合(支持m4a)(不支持wav)
     * @param outPutPath  [输出]结果文件全部名称包含后缀(比如.mp4)
     * @throws IOException 格式不支持等情况抛出异常
     */
    public static void appendMp4List(List<String> mp4PathList, String outPutPath) throws IOException {
        List<Movie> mp4MovieList = new ArrayList<>();// Movie对象集合[输入]
        for (String mp4Path : mp4PathList) {// 将每个文件路径都构建成一个Movie对象
            mp4MovieList.add(MovieCreator.build(mp4Path));
        }

        List<Track> audioTracks = new LinkedList<>();// 音频通道集合
        List<Track> videoTracks = new LinkedList<>();// 视频通道集合

        for (Movie mp4Movie : mp4MovieList) {// 对Movie对象集合进行循环
            for (Track inMovieTrack : mp4Movie.getTracks()) {
                if ("soun".equals(inMovieTrack.getHandler())) {// 从Movie对象中取出音频通道
                    audioTracks.add(inMovieTrack);
                }
                if ("vide".equals(inMovieTrack.getHandler())) {// 从Movie对象中取出视频通道
                    videoTracks.add(inMovieTrack);
                }
            }
        }

        Movie resultMovie = new Movie();// 结果Movie对象[输出]
        if (!audioTracks.isEmpty()) {// 将所有音频通道追加合并
            resultMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        if (!videoTracks.isEmpty()) {// 将所有视频通道追加合并
            resultMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }

        Container outContainer = new DefaultMp4Builder().build(resultMovie);// 将结果Movie对象封装进容器
        FileChannel fileChannel = new RandomAccessFile(String.format(outPutPath), "rw").getChannel();
        outContainer.writeContainer(fileChannel);// 将容器内容写入磁盘
        fileChannel.close();
    }

    /**
     * 得到录视频目录
     *
     * @return
     */
    public static String getDataPath(String dirName) {
        String path = null;
        String sdcardPath = null;
        sdcardPath = Environment
                .getExternalStorageDirectory().getPath();
        path = sdcardPath + "/" + dirName + "/";

        File tmp = new File(path);
        if (!tmp.exists()) {
            tmp.mkdir();
        }
        return path;
    }


    /**
     * 获取按时间的文件名<br/>
     * VyyyyMMdd_hhmmss
     *
     * @return
     */
    public static String getDateTimeFileName() {
        return "v" + formatDate(new Date(), DEFAULT_FILE_DIR_DATETIME_PATTERN);
    }

    /**
     * 格式化日期字符串
     *
     * @param date
     * @param pattern
     * @return
     */
    public static String formatDate(Date date, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(date);
    }

    /**
     * 根据秒速获取时间格式
     */
    public static String gennerTime(int totalSeconds) {
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // 取得文件大小
    public static long getFileSize(File f) throws Exception {
        return f.length();
    }
}
