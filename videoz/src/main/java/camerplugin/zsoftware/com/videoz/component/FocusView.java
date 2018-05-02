package camerplugin.zsoftware.com.videoz.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

/**
 * 对焦框
 * Created by coolzlay on 2018/4/29 0029.
 */
public class FocusView extends View {
    int left, top;
    Bitmap bitmap;

    public FocusView(Context context, int left, int top, Bitmap bitmap) {
        super(context);
        this.left = left;
        this.top = top;
        this.bitmap = bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, left, top, null);
        super.onDraw(canvas);
    }
}
