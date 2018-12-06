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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
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

import static utm.scanid.FileUtils.fromFileToBytes;
import static utm.scanid.SettingsDialog.getIpAddress;
import static utm.scanid.SettingsDialog.getPort;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE_FRONT = 1;
    static final int REQUEST_IMAGE_CAPTURE_BACK = 2;
    public static final int SERVER_PORT = 4000;
    static final String FIRST_IMAGE_ID = "firstImage";
    static final String SECOND_IMAGE_ID = "secondImage";
    public static final String DEFAULT_IP_ADDRESS = "192.168.103.158";
    static final String SETTINGS_SHARED_PREFERENCES = "settings_shared_preferences";
    static final String IP_ADDRESS_PARAM = "ip_address";
    static final String PORT_ADDRESS_PARAM = "port_param";
    static final String SETTINGS_TAG = "settings_tag";

    private ImageView frontCardIdView;
    private ImageView backCardIdView;
    private Button sendToServerButton;
    private String frontPhotoFilePath;
    private String backPhotoFilePath;
    private CompositeDisposable compositeDisposable;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_image);
        compositeDisposable = new CompositeDisposable();
        Toolbar toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        setSupportActionBar(toolbar);
        frontCardIdView = findViewById(R.id.front_image);
        backCardIdView = findViewById(R.id.back_image);
        Button frontCardImage = findViewById(R.id.take_front_card_image);
        Button backCardImage = findViewById(R.id.take_back_card_image_button);
        sendToServerButton = findViewById(R.id.scan_id_button);
        progressBar.setVisibility(View.GONE);
        frontCardImage.setOnClickListener(view -> takeFrontImageIntent());
        backCardImage.setOnClickListener(view -> takeBackCardImageIntent());
        sendToServerButton.setOnClickListener(view -> sendToServer());
        sendToServerButton.setEnabled(false);
        View frontCard = findViewById(R.id.front_card);
        View backCard = findViewById(R.id.back_card);
        frontCard.setOnClickListener(view -> {
          if (!TextUtils.isEmpty(frontPhotoFilePath)) {
            showImage(frontPhotoFilePath);
          }
        });
        backCard.setOnClickListener(view -> {
          if (!TextUtils.isEmpty(backPhotoFilePath)) {
            showImage(backPhotoFilePath);
          }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
    }

    private void takeFrontImageIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
              File photoFile = createImageFile();
              frontPhotoFilePath = photoFile.getAbsolutePath();
              Uri photoURI = FileProvider.getUriForFile(this, "utm.scanid", photoFile);
              takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
              startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_FRONT);
            } catch (IOException ignored) {
            }
        }
    }

    private void takeBackCardImageIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
              File photoFile = createImageFile();
              backPhotoFilePath = photoFile.getAbsolutePath();
              Uri photoURI = FileProvider.getUriForFile(this, "utm.scanid", photoFile);
              takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
              startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_BACK);
            } catch (IOException ignored) {
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void sendToServer() {
        compositeDisposable.add(sendImageToServer(frontPhotoFilePath, backPhotoFilePath)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> onLoad())
                .doOnTerminate(this::onLoadComplete)
                .subscribe(this::doneProcessing, error -> onError()));
    }

    public void onLoad() {
        sendToServerButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void onLoadComplete() {
        sendToServerButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);
    }

    public void onError() {
        Toast.makeText(this, R.string.something_went_wrong_label, Toast.LENGTH_LONG).show();
    }

    public void doneProcessing(Boolean result) {
        if (result) {
            Toast.makeText(this, R.string.imges_were_sent_successful, Toast.LENGTH_LONG).show();
        }
    }

    public Observable<Boolean> sendImageToServer(String firstPhotoPath, String secondPhotoPath) {
        return Observable.create(subscriber -> {
            try {
                File firstPathFile = new File(firstPhotoPath);
                File secondPathFile = new File(secondPhotoPath);
                OutputStream outputStream;
                Socket socket;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(FIRST_IMAGE_ID, android.util.Base64.encodeToString(fromFileToBytes(firstPathFile), android.util.Base64.DEFAULT));
                jsonObject.put(SECOND_IMAGE_ID, android.util.Base64.encodeToString(fromFileToBytes(secondPathFile), android.util.Base64.DEFAULT));
                String ipAddress = getIpAddress(this);
                int port = getPort(this);
                socket = new Socket(ipAddress, port);
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
            SettingsDialog settingsDialog = new SettingsDialog();
            settingsDialog.show(getSupportFragmentManager(), SETTINGS_TAG);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

  public void showImage(String uri) {
    Intent intent = new Intent(this, OpenImage.class);
    intent.setData(Uri.fromFile(new File(uri)));
    startActivity(intent);
  }
}