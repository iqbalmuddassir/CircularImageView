package muddassir.com.circularimageexample;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import java.util.List;

/**
 * @author muddassir on 11/17/16.
 * To dispay circular image with border. Also it supports zoom and panning functionality
 */

public class CircularImageView extends ImageView implements View.OnTouchListener {

    private static final float MIN_ZOOM_LEVEL = 1.0f;
    private static final float MAX_ZOOM_LEVEL = 3.0f;
    private Bitmap mBitmap;
    private int mStrokeWidth;
    private Paint mImagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private MotionEvent.PointerCoords pointerCoords1 = new MotionEvent.PointerCoords();
    private MotionEvent.PointerCoords pointerCoords2 = new MotionEvent.PointerCoords();
    private Rect mTargetRect = new Rect();
    private Rect mComputedRect = new Rect();
    private int viewWidth, viewHeight;
    private Shader mBitmapShader;
    private Matrix mMatrix;
    private Matrix matrix = new Matrix();
    private float mScale;
    private int mPosX, mPosY;
    private Rect mSourceRect;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private float mPrevPosX, mPrevPosY;
    private int centerX = 0, centerY = 0;
    private boolean mZooming;
    private boolean translatable;
    private boolean zoomable;

    public CircularImageView(Context context) {
        super(context);
        init(context, null);
    }

    public CircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircularImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setBorderColor(Color.WHITE);
        setBorderWidth(dpToPixel(5));
        setBorderStyle(Paint.Style.STROKE);
        mImagePaint.setAntiAlias(true);
        setOnTouchListener(this);

        setDefaults();
        // receive attribute params
        if (context != null && attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TouchParams, 0, 0);
            setZoomable(a.getBoolean(R.styleable.TouchParams_zoomable, false));
            setTranslatable(a.getBoolean(R.styleable.TouchParams_translatable, false));
            a.recycle();
        }
    }

    private void setDefaults() {
        mMatrix = new Matrix();
        mPosX = 0;
        mPosY = 0;
        mScale = 1.33f;
        mPrevPosX = 0;
        //centerX = 0;
        mPrevPosY = 0;
        //centerY = 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        viewWidth = getWidth();
        viewHeight = getHeight();

        centerX = viewWidth / 2;
        centerY = viewHeight / 2;

        mTargetRect.set(0, 0, viewWidth, viewHeight);

        mMatrix.reset();
        scaleCenter(mSourceRect, mTargetRect, mComputedRect, true);

        if (mSourceRect != null && mTargetRect != null) {
            computeRenderParams();
        }
    }

    public void setBitmap(Bitmap bitmap) {
        setDefaults();
        mBitmap = bitmap;
        mBitmapShader = new BitmapShader(mBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        mImagePaint.setShader(mBitmapShader);
        mSourceRect = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        mMatrix.reset();
        computeRenderParams();
    }

    public void setBorderWidth(int strokeWidth) {
        mStrokeWidth = strokeWidth;
        mBorderPaint.setStrokeWidth(mStrokeWidth);
    }

    public void setBorderColor(int strokeColor) {
        mBorderPaint.setColor(strokeColor);
    }

    public void setBorderStyle(Paint.Style strokeStyle) {
        mBorderPaint.setStyle(strokeStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (viewWidth == 0) {
            viewWidth = getWidth();
            viewHeight = getHeight();

            centerX = viewWidth / 2;
            centerY = viewHeight / 2;

            mTargetRect.set(0, 0, viewWidth, viewHeight);
        }
        canvas.drawARGB(0, 0, 0, 0);
        float r = viewWidth / 2f;
        canvas.drawCircle(r, r, r - mStrokeWidth, mBorderPaint);

        if (mBitmap == null) {
            return;
        }
        canvas.drawCircle(r, r, r - mStrokeWidth, mImagePaint);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getPointerCount() == 2) {
            event.getPointerCoords(0, pointerCoords1);
            event.getPointerCoords(1, pointerCoords2);
            List<GeometryUtils.Point> intersections = GeometryUtils.getCircleLineIntersectionPoint(new GeometryUtils.Point(pointerCoords1.x, pointerCoords1.y), new GeometryUtils.Point(pointerCoords2.x, pointerCoords2.y), new GeometryUtils.Point(viewWidth / 2, viewHeight / 2), viewWidth / 2);
            boolean intersects = intersections != null && (intersections.size() > 0);
            if (intersects && scaleGestureDetector != null) {
                scaleGestureDetector.onTouchEvent(event);
            }
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            mZooming = false;
        }
        if (gestureDetector != null) {
            return gestureDetector.onTouchEvent(event);
        }

        return translatable || zoomable;
    }

    private void computeRenderParams() {
        mScale = Math.max(MIN_ZOOM_LEVEL, Math.min(MAX_ZOOM_LEVEL, mScale));

        scaleCenter(mSourceRect, mTargetRect, mComputedRect, true);

        float zoom = Math.max((float) mComputedRect.width() / mSourceRect.width(), (float) mComputedRect.height() / mSourceRect.height());
        zoom *= mScale;

        int scaledWidth, scaledHeight;
        scaledWidth = (int) (mComputedRect.width() * mScale);
        scaledHeight = (int) (mComputedRect.height() * mScale);

        int cLeft, cRight, cTop, cBottom;
        cLeft = mComputedRect.centerX() - scaledWidth / 2;
        cRight = cLeft + scaledWidth;

        cTop = mComputedRect.centerY() - scaledHeight / 2;
        cBottom = cTop + scaledHeight;

        mComputedRect.set(cLeft, cTop, cRight, cBottom);

        int posX = centerX - scaledWidth / 2 - mSourceRect.left;
        int posY = centerY - scaledHeight / 2 - mSourceRect.top;

        limitPos();

        mMatrix.setTranslate(posX, posY);
        mMatrix.preScale(zoom, zoom);

        centerX = posX + scaledWidth / 2;
        centerY = posY + scaledHeight / 2;

        if (mBitmapShader == null) return;

        matrix.set(mMatrix);
        matrix.postTranslate(mPosX, mPosY);
        mBitmapShader.setLocalMatrix(matrix);
        mImagePaint.setShader(mBitmapShader);
        invalidate();
    }

    private void limitPos() {
        // set x limits
        if (mPosX > 0) {
            mPosX = Math.min(-mComputedRect.left, mPosX);
        } else {
            mPosX = Math.max(-(mComputedRect.right - viewWidth), mPosX);
        }

        // set y limits
        if (mPosY > 0) {
            mPosY = Math.min(-mComputedRect.top, mPosY);
        } else {
            mPosY = Math.max(-(mComputedRect.bottom - viewHeight), mPosY);
        }
    }

    public void setZoomable(boolean zoomable) {
        this.zoomable = zoomable;
        if (zoomable) {
            // Initialise the scale detector
            scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    mZooming = true;
                    mScale *= detector.getScaleFactor();
                    computeRenderParams();
                    return true;
                }

                @Override
                public boolean onScaleBegin(ScaleGestureDetector detector) {
                    return true;
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector detector) {
                }
            });
        }
    }

    public void setTranslatable(boolean translatable) {
        this.translatable = translatable;
        if (translatable) {
            // Initialise the gesture detector
            gestureDetector = new GestureDetector(getContext()
                    , new GestureDetector.SimpleOnGestureListener() {

                public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                        float distanceX, float distanceY) {
                    if (!mZooming && e1.getPointerCount() == 1 && e2.getPointerCount() == 1) {
                        mPosX = (int) (mPrevPosX + e2.getX() - e1.getX());
                        mPosY = (int) (mPrevPosY + e2.getY() - e1.getY());

                        computeRenderParams();
                    }
                    return true;
                }

                public boolean onDown(MotionEvent e) {
                    if (!mZooming && e.getPointerCount() == 1) {
                        mPrevPosX = mPosX;
                        mPrevPosY = mPosY;
                    }
                    return true;
                }
            });
        }
    }

    private int dpToPixel(int size) {

        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size,
                getContext().getResources().getDisplayMetrics());
    }

    /**
     * @param srcRect  original Rect to extract original width and height values
     * @param destRect target Rect (the location specifier).
     * @param crop     true for centerCrop and false for centerInside
     */
    public static void scaleCenter(Rect srcRect, Rect destRect, Rect computedRect, boolean crop) {
        if (srcRect == null || destRect == null) return;

        int destWidth = destRect.width();
        int destHeight = destRect.height();

        int sourceWidth = srcRect.width();
        int sourceHeight = srcRect.height();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) destWidth / sourceWidth;
        float yScale = (float) destHeight / sourceHeight;

        float scale;
        if (crop) {
            scale = Math.max(xScale, yScale);
        } else {
            scale = Math.min(xScale, yScale);
        }

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (destWidth - scaledWidth) / 2;
        float top = (destHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        RectF targetRectF = new RectF(left, top, left + scaledWidth, top + scaledHeight);
        targetRectF.offset(destRect.left, destRect.top);

        Rect targetRect = new Rect();
        targetRectF.roundOut(targetRect);

        computedRect.set(targetRect);
    }
}
