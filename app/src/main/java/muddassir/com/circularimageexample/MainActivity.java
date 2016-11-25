package muddassir.com.circularimageexample;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CircularImageView cropImageView = (CircularImageView) findViewById(R.id.CropImageView);
        String imagePath = "http://www.freedigitalphotos.net/images/img/homepage/golf-1-top-82328.jpg";
        Glide.with(this)
                .load(imagePath)
                .asBitmap()
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, String model, com.bumptech.glide.request.target.Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, String model, com.bumptech.glide.request.target.Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        if (cropImageView != null) {
                            cropImageView.setBitmap(resource);
                            cropImageView.invalidate();
                        }
                        return true;
                    }
                }).into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
    }
}
