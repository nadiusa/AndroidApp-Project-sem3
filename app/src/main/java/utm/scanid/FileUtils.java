package utm.scanid;

import android.content.Context;
import android.support.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

class FileUtils {

  @Nullable
  static byte[] fromFileToBytes(File file) throws Exception {
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

  public static void deleteCache(Context context) {
    try {
      File dir = context.getCacheDir();
      deleteDir(dir);
    } catch (Exception e) { e.printStackTrace();}
  }

  public static boolean deleteFilesWithName(File dir, String name) {
    if (dir != null && dir.isDirectory()) {
      String[] children = dir.list();
      for (String fileChildren : children) {
        final File file = new File(dir, fileChildren);
        if (file.getName().startsWith(name)) {
          boolean success = deleteDir(file);
          if (!success) {
            return false;
          }
        }
      }
      return true;
    } else if(dir!= null && dir.isFile()) {
      return dir.delete();
    } else {
      return false;
    }
  }

  public static boolean deleteAllFilesInDir(File dir) {
    if (dir != null && dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
        }
      }
      return true;
    } else if(dir!= null && dir.isFile()) {
      return dir.delete();
    } else {
      return false;
    }
  }

  public static boolean deleteDir(File dir) {
    if (dir != null && dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
        }
      }
      return dir.delete();
    } else if(dir!= null && dir.isFile()) {
      return dir.delete();
    } else {
      return false;
    }
  }
}
