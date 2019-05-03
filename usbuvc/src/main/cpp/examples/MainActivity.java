package examples;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.*;

import java.util.*;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.view.View.*;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
//        System.loadLibrary("fftw3f");
//        System.loadLibrary("usb");
//        System.loadLibrary("hackrf");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.main_text);
        tv.setVisibility(VISIBLE);
        tv.setText("Welcome");
    }

    public void showSensorList(View view) {
        TextView tv = findViewById(R.id.main_text);
        tv.setText("\n\n\n\nSensors:\n========\n\n");

        SensorInterface si = new SensorInterface();
        tv.append(si.listSensors());
    }

    public void showUSBList(View view) {
        TextView tv = findViewById(R.id.main_text);
        tv.setText("\n\n\n\nUSB devices (from Java):\n=========================\n");

        UsbManager usbMgr;
        HashMap<String, UsbDevice> usbDevs;
        usbMgr = getSystemService(UsbManager.class);

        if (usbMgr == null) {
            tv.append("FAILED to get UsbManager object.\n");
        } else {
            usbDevs = usbMgr.getDeviceList();

            for (String k : usbDevs.keySet()) {
                UsbDevice ud = usbDevs.get(k);
                tv.append("\n");
                tv.append(ud.getDeviceName());
                tv.append("\n");
                tv.append(ud.getProductName());
                tv.append("\n");
                }
            }
        }

    public void showHackRF(View view) {
        TextView tv = findViewById(R.id.main_text);
        tv.setText("\n\n\n\nHackRF:\n========\n\n");

        Intent myIntent = new Intent();
        UsbDeviceConnection devconn = null;
        int hrfFD = 0; // HackRF file descriptor to pass to libusb
        UsbManager usbMgr;
        HashMap<String, UsbDevice> usbDevs;
        usbMgr = getSystemService(UsbManager.class);

        PendingIntent pi = PendingIntent.getActivity(this, 1, myIntent, FLAG_UPDATE_CURRENT);
        if (usbMgr == null) {
            tv.append("FAILED to get UsbManager object.\n");
        } else {
            usbDevs = usbMgr.getDeviceList();

            for (String k : usbDevs.keySet()) {
                UsbDevice ud = usbDevs.get(k);

                if (ud.getProductName().endsWith("HackRF One")) {
                    usbMgr.requestPermission(ud, pi);
                    devconn = usbMgr.openDevice(ud);
                    if (devconn == null) {
                        tv.append("FAILED to find HackRF USB device.\n");

                    } else {
                        hrfFD = devconn.getFileDescriptor();

                        // call to native method
                        SensorInterface si = new SensorInterface();
                        tv.append(si.showHackRF(hrfFD));

                        devconn.close();
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
//    public native String stringFromJNI();

}

class HelloFromNative {
    native String stringFromJNI();
}

class SensorInterface {
    native String listSensors();

    native String showHackRF(int fd);
}
