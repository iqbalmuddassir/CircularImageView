# CircularImageView
To dispay circular image with border. Also it supports zoom and panning functionality. 
Special thanks to [Sahil Bajaj](https://github.com/sahilbajaj)

Usage
---------

1. Download the project zip.
2. Copy 2 classes in your project namely CircularImageView.java and GeometryUtils. Also copy circular_image_attr.xml into your values folder.
3. Add the CircularImageView to your layout. e.g.

	```xml
	<muddassir.com.circularimageexample.CircularImageView
		android:id="@+id/CropImageView"
		android:layout_width="300dp"
		android:layout_height="300dp"
		app:zoomable="true"
		app:translatable="true"
		android:layout_centerInParent="true" />
	``` 
4. Now assign the bitmap to it from the java file. like

	```java
	CircularImageView cropImageView = (CircularImageView) findViewById(R.id.CropImageView);
		if (cropImageView != null) {
		cropImageView.setBitmap(resource);
		cropImageView.invalidate();
	}
	```
