package utm.scanmyid;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

import static utm.scanmyid.FileUtils.fromFileToBytes;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView imageView;
    private Button scanIdButton;
    private TextView uploadImageLabel;
    private String currentPhotoPath;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        compositeDisposable = new CompositeDisposable();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        imageView = findViewById(R.id.passport_image_scan);
        Button takePictureButton = findViewById(R.id.take_picture_button);
        scanIdButton = findViewById(R.id.scan_id_button);
        uploadImageLabel = findViewById(R.id.info_label);

        takePictureButton.setOnClickListener(view -> takePictureIntent());
        scanIdButton.setOnClickListener(view -> sendToServer());

        scanIdButton.setEnabled(false);

        //Check for camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
    }

    private void takePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "utm.scanmyid",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void sendToServer() {
        compositeDisposable.add(sendImageToServer(currentPhotoPath)
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

    public Observable<Boolean> sendImageToServer(String currentPhotoPath) {
        return Observable.create(subscriber -> {
            try {
                File file = new File(currentPhotoPath);
                OutputStream outputStream;
                Socket socket;
                JSONObject jsonObject = new JSONObject();
                String hostname = "192.168.103.158";
                int port = 4000;
                jsonObject.put("firstImage", android.util.Base64.encodeToString(fromFileToBytes(file), android.util.Base64.DEFAULT));
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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            loadImage(currentPhotoPath);
        }
    }

    private void loadImage(String pathImagePath) {
        uploadImageLabel.setVisibility(View.GONE);
        scanIdButton.setEnabled(true);
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
