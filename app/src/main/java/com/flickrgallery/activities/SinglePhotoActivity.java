package com.flickrgallery.activities;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.flickrgallery.R;
import com.flickrgallery.imageLoader.ImageLoader;


public class SinglePhotoActivity extends AppCompatActivity {

    private static final int ANIM_DURATION = 200;
    private int mLeftDelta;
    private int mTopDelta;
    private float mWidthScale;
    private float mHeightScale;
    private int thumbnailTop;
    private int thumbnailLeft;
    private int thumbnailWidth;
    private int thumbnailHeight;
    private ColorDrawable colorDrawable;
    private ImageView ivSingle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_single_photo);

        Context context     = this;
        TextView tv_heading = findViewById(R.id.tv_heading);
        ImageView iv_back   = findViewById(R.id.iv_back);
        ivSingle            = findViewById(R.id.single_image);
        String imageUrl     = getIntent().getStringExtra("image_url");
        String title        = getIntent().getStringExtra("title");

        thumbnailTop          = getIntent().getIntExtra("top",0);
        thumbnailLeft         = getIntent().getIntExtra("left",0);
        thumbnailWidth        = getIntent().getIntExtra("width",0);
        thumbnailHeight       = getIntent().getIntExtra("height",0);
        ImageLoader imgLoader = new ImageLoader(context.getApplicationContext());

        imgLoader.DisplayImage(imageUrl, ivSingle);
        tv_heading.setText(title);

        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if (savedInstanceState == null) {
            ViewTreeObserver observer = ivSingle.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                @Override
                public boolean onPreDraw() {
                    ivSingle.getViewTreeObserver().removeOnPreDrawListener(this);

                    // Figure out where the thumbnail and full size versions are, relative
                    // to the screen and each other
                    int[] screenLocation = new int[2];
                    ivSingle.getLocationOnScreen(screenLocation);
                    mLeftDelta = thumbnailLeft - screenLocation[0];
                    mTopDelta = thumbnailTop - screenLocation[1];

                    // Scale factors to make the large version the same size as the thumbnail
                    mWidthScale = (float) thumbnailWidth / ivSingle.getWidth();
                    mHeightScale = (float) thumbnailHeight / ivSingle.getHeight();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) enterAnimation();

                    return true;
                }
            });
        }

    }

    public void enterAnimation() {

        // Set starting values for properties we're going to animate. These
        // values scale and position the full size version down to the thumbnail
        // size/location, from which we'll animate it back up
        ivSingle.setPivotX(0);
        ivSingle.setPivotY(0);
        ivSingle.setScaleX(mWidthScale);
        ivSingle.setScaleY(mHeightScale);
        ivSingle.setTranslationX(mLeftDelta);
        ivSingle.setTranslationY(mTopDelta);

        // interpolator where the rate of change starts out quickly and then decelerates.
        TimeInterpolator sDecelerator = new DecelerateInterpolator();

        // Animate scale and translation to go from thumbnail to full size
        ivSingle.animate().setDuration(ANIM_DURATION).scaleX(1).scaleY(1).translationX(0).translationY(0).setInterpolator(sDecelerator);

        // Fade in the black background
        ObjectAnimator bgAnim = ObjectAnimator.ofInt(colorDrawable, "alpha", 0, 255);
        bgAnim.setDuration(ANIM_DURATION);
        bgAnim.start();
    }

    /**
     * The exit animation is basically a reverse of the enter animation.
     * This Animate image back to thumbnail size/location as relieved from bundle.
     *
     * @param endAction This action gets run after the animation completes (this is
     *                  when we actually switch activities)
     */
    public void exitAnimation(final Runnable endAction) {
        TimeInterpolator sInterpolator = new AccelerateInterpolator();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ivSingle.animate().setDuration(ANIM_DURATION).scaleX(mWidthScale).scaleY(mHeightScale).
                    translationX(mLeftDelta).translationY(mTopDelta)
                    .setInterpolator(sInterpolator).withEndAction(endAction);
        }

        // Fade out background
        ObjectAnimator bgAnim = ObjectAnimator.ofInt(colorDrawable, "alpha", 0);
        bgAnim.setDuration(ANIM_DURATION);
        bgAnim.start();
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            exitAnimation(new Runnable() {
                public void run() {
                    finish();
                    overridePendingTransition(0, 0);
                }
            });
        }
        else {
            finish();
            overridePendingTransition(0, 0);
        }
    }
}
