package utm.scanid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static utm.scanid.FileUtils.fromFileToBytes;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE_FRONT = 1;
    static final int REQUEST_IMAGE_CAPTURE_BACK = 2;
    private ImageView frontCardIdView;
    private ImageView backCardIdView;
    private Button sendToServerButton;
    private String frontPhotoFilePath;
    private String backPhotoFilePath;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        compositeDisposable = new CompositeDisposable();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        frontCardIdView = findViewById(R.id.front_image);
        backCardIdView = findViewById(R.id.back_image);
        Button frontCardImage = findViewById(R.id.take_front_card_image);
        Button backCardImage = findViewById(R.id.take_back_card_image_button);
        sendToServerButton = findViewById(R.id.scan_id_button);
        frontCardImage.setOnClickListener(view -> takeFrontImageIntent());
        backCardImage.setOnClickListener(view -> takeBackCardImageIntent());
        sendToServerButton.setOnClickListener(view -> sendToServer());
        sendToServerButton.setEnabled(false);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
    }

    private void takeFrontImageIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    frontPhotoFilePath = photoFile.getAbsolutePath();
                    Uri photoURI = FileProvider.getUriForFile(this, "utm.scanid", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_FRONT);
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void takeBackCardImageIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    backPhotoFilePath = photoFile.getAbsolutePath();
                    Uri photoURI = FileProvider.getUriForFile(this, "utm.scanid", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_BACK);
                }
            } catch (IOException ignored) {
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    private void sendToServer() {
        compositeDisposable.add(sendImageToServer(frontPhotoFilePath, backPhotoFilePath)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> onLoad())
                .doOnTerminate(() -> onLoadComplete())
                .subscribe(this::doneProcessing, this::onError));
    }

    public void onLoad() {

    }

    public void onLoadComplete() {

    }

    public void onError(Throwable throwable) {

    }

    public void doneProcessing(Boolean result) {

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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE_FRONT && resultCode == RESULT_OK) {
            loadImage(frontPhotoFilePath, frontCardIdView);
        } else if (requestCode == REQUEST_IMAGE_CAPTURE_BACK && resultCode == RESULT_OK) {
            loadImage(backPhotoFilePath, backCardIdView);
        }
    }

    private void loadImage(String pathImagePath, ImageView imageView) {
        if (!TextUtils.isEmpty(frontPhotoFilePath) && !TextUtils.isEmpty(backPhotoFilePath)) {
            sendToServerButton.setEnabled(true);
        }
        Glide.with(this)
                .load(pathImagePath)
                .apply(new RequestOptions().placeholder(R.drawable.ic_camera))
                .into(imageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}