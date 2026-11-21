package sw.gram.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.animation.OvershootInterpolator;
import androidx.appcompat.widget.AppCompatTextView;

public class JellyButton extends AppCompatTextView {
    private float scale = 1f;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public JellyButton(Context c, String label) {
        super(c);
        setText(label);
        setTextColor(0xFFFFFFFF);
        setTextSize(16);
        setGravity(Gravity.CENTER);
        setPadding(60, 40, 60, 40);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(2f);
        edge.setColor(0x55FFFFFF);
    }

    @Override protected void onDraw(Canvas c) {
        rect.set(1, 1, getWidth() - 1, getHeight() - 1);
        fill.setShader(new LinearGradient(0, 0, 0, getHeight(), 0x55FFFFFF, 0x22FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, 40, 40, fill);
        c.drawRoundRect(rect, 40, 40, edge);
        super.onDraw(c);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) spring(0.90f, 90, 0f);
        else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) spring(1f, 460, 3.4f);
        return super.onTouchEvent(e);
    }

    private void spring(float to, int ms, float tension) {
        ValueAnimator an = ValueAnimator.ofFloat(scale, to);
        an.setDuration(ms);
        if (tension > 0) an.setInterpolator(new OvershootInterpolator(tension));
        an.addUpdateListener(v -> {
            scale = (Float) v.getAnimatedValue();
            setScaleX(scale); setScaleY(scale);
        });
        an.start();
    }
}
