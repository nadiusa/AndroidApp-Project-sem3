package utm.scanid;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.FileOutputStream;

import static utm.scanid.MainActivity.FILE_URI;

public class TakePicturesActivity extends AppCompatActivity {
  CameraView camera;
  private CompositeDisposable compositeDisposable;
  private Bitmap bitmapResult;
  private ProgressBar progressBar;
  private Button makePhotoButton;
  private Button confirmButton;
  private Button cancelButton;
  private TextView quiestionBox;
  private ImageView imageView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    compositeDisposable = new CompositeDisposable();
    camera = findViewById(R.id.camera);
    camera.setLifecycleOwner(this);
    makePhotoButton = findViewById(R.id.makePhoto);
    confirmButton = findViewById(R.id.confirm);
    cancelButton = findViewById(R.id.cancel);
    quiestionBox = findViewById(R.id.quiestion);
    imageView = findViewById(R.id.image);
    progressBar = findViewById(R.id.progressBar);
    progressBar.setVisibility(View.GONE);
    makePhotoButton.setVisibility(View.VISIBLE);
    cancelButton.setVisibility(View.INVISIBLE);
    confirmButton.setVisibility(View.INVISIBLE);
    quiestionBox.setVisibility(View.INVISIBLE);
    imageView.setVisibility(View.INVISIBLE);
    final Uri fileUri = getIntent().getParcelableExtra(FILE_URI);
    camera.addCameraListener(new CameraListener() {
      @Override
      public void onPictureTaken(final byte[] picture) {
        CameraUtils.decodeBitmap(picture, bitmap -> {
          bitmapResult = bitmap;
          imageView.setImageBitmap(bitmap);
        });
      }
    });

    makePhotoButton.setOnClickListener(v -> {
      camera.capturePicture();
      imageView.setVisibility(View.VISIBLE);
      camera.setVisibility(View.INVISIBLE);
      makePhotoButton.setVisibility(View.INVISIBLE);
      cancelButton.setVisibility(View.VISIBLE);
      confirmButton.setVisibility(View.VISIBLE);
      quiestionBox.setVisibility(View.VISIBLE);
    });

    confirmButton.setOnClickListener(v -> {
      imageView.setVisibility(View.INVISIBLE);
      camera.setVisibility(View.VISIBLE);
      makePhotoButton.setVisibility(View.INVISIBLE);
      cancelButton.setVisibility(View.INVISIBLE);
      confirmButton.setVisibility(View.INVISIBLE);
      quiestionBox.setVisibility(View.INVISIBLE);
      saveImageInFile(bitmapResult, fileUri);
    });

    cancelButton.setOnClickListener(v -> {
      makePhotoButton.setVisibility(View.VISIBLE);
      cancelButton.setVisibility(View.VISIBLE);
      confirmButton.setVisibility(View.VISIBLE);
      quiestionBox.setVisibility(View.VISIBLE);
    });
  }

  private void saveImageInFile(Bitmap bitmap, Uri uri) {
    File file = new File(uri.getPath());
    if (bitmap != null && file.exists()) {
      compositeDisposable.add(sendImageToServer(bitmap, file).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnSubscribe(disposable -> onLoad())
          .doOnTerminate(this::onTerminate)
          .subscribe(result -> doneProcessing(), error -> onError()));
    } else {
      showCameraVisible();
    }
  }

  public void showCameraVisible() {
    makePhotoButton.setVisibility(View.VISIBLE);
    cancelButton.setVisibility(View.VISIBLE);
    confirmButton.setVisibility(View.INVISIBLE);
    camera.setVisibility(View.VISIBLE);
    quiestionBox.setVisibility(View.INVISIBLE);
    imageView.setVisibility(View.INVISIBLE);
  }

  public void onTerminate() {
    progressBar.setVisibility(View.GONE);
  }

  public void onLoad() {
    progressBar.setVisibility(View.VISIBLE);
  }

  public void onError() {
    Toast.makeText(this, R.string.something_went_wrong_label, Toast.LENGTH_LONG).show();
    showCameraVisible();
  }

  public Observable<Boolean> sendImageToServer(Bitmap bitmap, File file) {
    return Observable.create(subscriber -> {
      try {
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        subscriber.onNext(true);
        subscriber.onComplete();
      } catch (Throwable e) {
        subscriber.onError(e);
      }
    });
  }

  public void doneProcessing() {
    setResult(RESULT_OK);
    finish();
  }

  @Override
  protected void onResume() {
    super.onResume();
    camera.start();
  }

  @Override
  protected void onPause() {
    super.onPause();
    camera.stop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    camera.destroy();
  }
}