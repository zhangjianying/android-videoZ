package camerplugin.zsoftware.com.videoz.event;

/**
 * Created by coolzlay on 2018/4/30 0030.
 */

import java.io.File;
import java.util.Queue;

/**
 * 视频面板事件
 */
public interface RecordViewListener {

    public void startRecord(String RecordFileName);

    public void recodingProcess(int currentSec, int maxSec);

    public void endRecord(String filePath);
}
