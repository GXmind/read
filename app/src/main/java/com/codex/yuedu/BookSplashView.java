package com.codex.yuedu;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

final class BookSplashView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float progress;

    BookSplashView(Context context) {
        super(context);
        setBackgroundColor(0xfffbf7ef);
        paint.setShadowLayer(dp(14), 0, dp(7), 0x30000000);
        setLayerType(LAYER_TYPE_SOFTWARE, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1150);
        animator.setStartDelay(180);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a -> { progress = (float)a.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() * .43f;
        float pageW = Math.min(getWidth() * .31f, dp(142)), pageH = pageW * 1.35f;
        RectF left = new RectF(cx - pageW, cy - pageH / 2, cx, cy + pageH / 2);
        RectF right = new RectF(cx, cy - pageH / 2, cx + pageW, cy + pageH / 2);

        paint.setColor(0xffd94a3d); canvas.drawRoundRect(new RectF(left.left-dp(5),left.top-dp(6),right.right+dp(5),right.bottom+dp(7)),dp(18),dp(18),paint);
        paint.clearShadowLayer(); paint.setColor(0xfffffdf8); canvas.drawRoundRect(left, dp(11), dp(11), paint); canvas.drawRoundRect(right, dp(11), dp(11), paint);
        paint.setColor(0xffd8cfc1); paint.setStrokeWidth(dp(1)); canvas.drawLine(cx, left.top+dp(5), cx, left.bottom-dp(5), paint);
        for (int i=0;i<5;i++) {
            float y=left.top+dp(28)+i*dp(17); paint.setColor(0xffddd6ca);
            canvas.drawLine(left.left+dp(20),y,left.right-dp(18),y,paint); canvas.drawLine(right.left+dp(18),y,right.right-dp(20),y,paint);
        }

        float scale = Math.abs((float)Math.cos(Math.PI * progress));
        canvas.save(); canvas.scale(Math.max(.025f, scale), 1f, cx, cy);
        paint.setShadowLayer(dp(8), progress < .5f ? dp(5) : -dp(5), dp(3), 0x28000000);
        paint.setColor(0xfffffaf0);
        RectF flipping = progress < .5f ? right : left;
        canvas.drawRoundRect(flipping, dp(10), dp(10), paint);
        paint.clearShadowLayer(); canvas.restore();

        textPaint.setColor(0xff302c28); textPaint.setTextSize(dp(30)); textPaint.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));
        canvas.drawText("悦读",cx,cy+pageH/2+dp(70),textPaint);
        textPaint.setColor(0xff9a8f84); textPaint.setTextSize(dp(13)); textPaint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("翻开一页，进入一个世界",cx,cy+pageH/2+dp(100),textPaint);
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
