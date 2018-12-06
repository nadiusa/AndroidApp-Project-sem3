package utm.scanid;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.OutputStream;
import java.net.Socket;
import org.json.JSONObject;

import static utm.scanid.FileUtils.fromFileToBytes;

public class TakePicturesActivity extends AppCompatActivity {
  static final String FILE_URL_PARAM = "file_url";

  public static Intent init(Context context, String fileUrl) {
    Intent intent = new Intent(context, TakePicturesActivity.class);
    intent.putExtra(FILE_URL_PARAM, fileUrl);
    return intent;
  }

  CameraView camera;
  private CompositeDisposable compositeDisposable;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    compositeDisposable = new CompositeDisposable();
    camera = findViewById(R.id.camera);
    camera.setLifecycleOwner(this);
    final Button makePhotoButton = findViewById(R.id.makePhoto);
    final Button confirmButton = findViewById(R.id.confirm);
    final Button cancelButton = findViewById(R.id.cancel);
    final TextView quiestionBox = findViewById(R.id.quiestion);
    final ImageView imageView = findViewById(R.id.image);
    makePhotoButton.setVisibility(View.VISIBLE);
    cancelButton.setVisibility(View.INVISIBLE);
    confirmButton.setVisibility(View.INVISIBLE);
    quiestionBox.setVisibility(View.INVISIBLE);
    imageView.setVisibility(View.INVISIBLE);

    camera.addCameraListener(new CameraListener() {
      @Override
      public void onPictureTaken(final byte[] picture) {
        CameraUtils.decodeBitmap(picture, bitmap -> imageView.setImageBitmap(bitmap));
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
    });

    cancelButton.setOnClickListener(v -> {
      makePhotoButton.setVisibility(View.VISIBLE);
      cancelButton.setVisibility(View.VISIBLE);
      confirmButton.setVisibility(View.VISIBLE);
      quiestionBox.setVisibility(View.VISIBLE);
    });
  }

  private void saveImageInFile(String fileUrl) {
    //compositeDisposable.add(sendImageToServer(frontPhotoFilePath, backPhotoFilePath)
    //    .subscribeOn(Schedulers.io())
    //    .observeOn(AndroidSchedulers.mainThread())
    //    .doOnSubscribe(disposable -> onLoad())
    //    .doOnTerminate()
    //    .subscribe(this::doneProcessing, this::onError));
  }


  public Observable<Boolean> sendImageToServer(String firstPhotoPath, String secondPhotoPath) {
    return Observable.create(subscriber -> {
      try {
        File firstPathFile = new File(firstPhotoPath);
        File secondPathFile = new File(secondPhotoPath);
        OutputStream outputStream;
        Socket socket;
        JSONObject jsonObject = new JSONObject();
        String hostname = "192.168.103.158";
        int port = 4000;
        jsonObject.put("firstImage", android.util.Base64.encodeToString(fromFileToBytes(firstPathFile), android.util.Base64.DEFAULT));
        jsonObject.put("secondImage", android.util.Base64.encodeToString(fromFileToBytes(secondPathFile), android.util.Base64.DEFAULT));
        socket = new Socket(hostname, port);
        outputStream = socket.getOutputStream();
        outputStream.write(jsonObject.toString().getBytes());
        socket.close();
        subscriber.onNext(false);
        subscriber.onComplete();
      } catch (Throwable e) {
        subscriber.onError(e);
      }
    });
  }

  public void doneProcessing(Boolean result) {
      //done
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
