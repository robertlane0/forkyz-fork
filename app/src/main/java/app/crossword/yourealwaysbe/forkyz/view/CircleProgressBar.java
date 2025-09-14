package app.crossword.yourealwaysbe.forkyz.view;

import java.util.Objects;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.theme.CircleProgressBarColorScheme;
import app.crossword.yourealwaysbe.forkyz.util.ColorUtilsKt;

/**
 * Created by rcooper on 10/26/14.
 */
public class CircleProgressBar extends View {
    private CircleProgressBarColorScheme colors;
    private int height;
    private int width;
    private int percentFilled;
    private boolean complete;
    private DisplayMetrics metrics;
    private float circleStroke;
    private float circleFine;
    private Paint paint;
    private RectF pcntFilledRect;

    public CircleProgressBar(
        Context context, CircleProgressBarColorScheme colors
    ) {
        super(context);
        initMetrics(context);
        this.colors = colors;
        paint = new Paint();
    }

    public void setColors(CircleProgressBarColorScheme colors) {
        if (!Objects.equals(this.colors, colors)) {
            this.colors = colors;
            invalidate();
        }
    }

    public void setPercentFilled(int percentFilled) {
        this.percentFilled = percentFilled;
        this.invalidate();
    }

    public int getPercentFilled() {
        return percentFilled;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
        this.invalidate();
    }

    public boolean getComplete() {
        return complete;
    }

    @Override
    protected void onMeasure(int widthSpecId, int heightSpecId) {
        this.height = View.MeasureSpec.getSize(heightSpecId);
        this.width = View.MeasureSpec.getSize(widthSpecId);
        setMeasuredDimension(this.width, this.height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (colors == null)
            return;

        float halfWidth = width / 2;
        float halfHeight = height / 2;
        float halfStroke = circleStroke / 2;
        float textSize = halfWidth * 0.75f;

        paint.setAntiAlias(true);
        paint.setColor(colors.progressNull());
        paint.setStyle(Paint.Style.STROKE);
        paint.setTypeface(Typeface.SANS_SERIF);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize);

        if (this.complete) {
            paint.setColor(colors.progressDone());
            paint.setStrokeWidth(circleStroke);
            canvas.drawCircle(
                halfWidth, halfWidth,
                halfWidth - halfStroke - metrics.density * 2f,
                paint
            );
            drawDrawable(
                R.drawable.ic_done,
                halfWidth,
                halfWidth,
                ColorUtilsKt.colorToInt(colors.progressDone()),
                canvas
            );
        } else if (this.percentFilled < 0) {
            paint.setColor(colors.progressError());
            paint.setStrokeWidth(circleStroke);
            canvas.drawCircle(
                halfWidth,
                halfWidth,
                halfWidth - halfStroke - metrics.density * 2f,
                paint
            );
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("?", halfWidth, halfWidth + textSize / 3f, paint);
        } else if (this.percentFilled == 0) {
            drawDrawable(
                R.drawable.ic_play,
                halfWidth,
                textSize,
                ColorUtilsKt.colorToInt(colors.progressNull()),
                canvas
            );
        } else {
            paint.setColor(colors.progressInProgress());
            paint.setStrokeWidth(circleFine);
            canvas.drawCircle(
                halfWidth,
                halfWidth,
                halfWidth - halfStroke - 1f,
                paint
            );
            paint.setStrokeWidth(circleStroke);

            pcntFilledRect.set(
                0 + circleStroke, 0 + circleStroke,
                width - circleStroke , width - circleStroke
            );
            canvas.drawArc(
                pcntFilledRect, -90,  360F * percentFilled / 100F, false, paint
            );
            paint.setStyle(Paint.Style.FILL);
            textSize = halfWidth * 0.5f;
            paint.setTextSize(textSize);
            canvas.drawText(
                percentFilled+"%",
                halfWidth,
                halfHeight + textSize / 3f,
                paint
            );
        }
    }

    private void drawDrawable(
        int resId,
        float halfWidth,
        float diameter,
        int color,
        Canvas canvas
    ) {
        Drawable icon = ContextCompat.getDrawable(getContext(), resId);
        Drawable iconWrapped = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(iconWrapped, color);
        iconWrapped.setBounds(
            (int) (halfWidth - 0.5 * diameter),
            (int) (halfWidth - 0.5 * diameter),
            (int) (halfWidth + 0.5 * diameter),
            (int) (halfWidth + 0.5 * diameter)
        );
        iconWrapped.draw(canvas);
    }

    private final void initMetrics(Context context){
        metrics = context.getResources().getDisplayMetrics();
        circleStroke = metrics.density * 6F;
        circleFine = metrics.density * 2f;
        pcntFilledRect = new RectF(0, 0, 0, 0);
    }
}
