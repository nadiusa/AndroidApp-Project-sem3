package utm.scanid;

import android.support.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FileUtils {

  @Nullable
  public static byte[] fromFileToBytes(File file) throws Exception {
    byte[] buffer = new byte[4096];
    ByteArrayOutputStream ous = new ByteArrayOutputStream();
    InputStream ios = new FileInputStream(file);
    int read;
    while ((read = ios.read(buffer)) != -1) {
      ous.write(buffer, 0, read);
    }
    ous.close();
    ios.close();
    return ous.toByteArray();
  }
}
