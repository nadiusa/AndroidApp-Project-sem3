package utm.scanid;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import static android.content.Context.MODE_PRIVATE;
import static utm.scanid.MainActivity.DEFAULT_IP_ADDRESS;
import static utm.scanid.MainActivity.IP_ADDRESS_PARAM;
import static utm.scanid.MainActivity.PORT_ADDRESS_PARAM;
import static utm.scanid.MainActivity.SERVER_PORT;
import static utm.scanid.MainActivity.SETTINGS_SHARED_PREFERENCES;

public class SettingsDialog extends DialogFragment {

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = getActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.settings_dialog, null);
    EditText ipAddress = view.findViewById(R.id.ip_address);
    EditText ipPort = view.findViewById(R.id.port_field);
    String ipAddressValue = getIpAddress(getActivity());
    ipAddress.setText(ipAddressValue);
    ipAddress.setSelection(ipAddressValue.length());
    ipPort.setText(String.valueOf(getPort(getActivity())));
    builder.setView(view)
        // Add action buttons
        .setPositiveButton(R.string.set_label,
            (dialog, id) -> saveSettings(ipAddress, ipPort))
        .setNegativeButton(R.string.cancel_label, (dialog, id) -> dialog.dismiss());
    return builder.create();
  }


  public static String getIpAddress(Activity activity) {
    SharedPreferences settings = activity.getSharedPreferences(SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
    return settings.getString(IP_ADDRESS_PARAM, DEFAULT_IP_ADDRESS);
  }

  public static int getPort(Activity activity) {
    SharedPreferences settings = activity.getSharedPreferences(SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
    return settings.getInt(PORT_ADDRESS_PARAM, SERVER_PORT);
  }

  public void saveSettings(EditText ipAddress, EditText port) {
    if (!TextUtils.isEmpty(ipAddress.getText().toString()) && !TextUtils.isEmpty(port.getText().toString())) {
      final String ipAddressValue = ipAddress.getText().toString();
      final Integer portValue = Integer.valueOf(port.getText().toString());
      if (getActivity() != null) {
        SharedPreferences settings = getActivity().getSharedPreferences(SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
        settings.edit().putString(IP_ADDRESS_PARAM, ipAddressValue).apply();
        settings.edit().putInt(PORT_ADDRESS_PARAM, portValue).apply();
      }
    }
  }
}
