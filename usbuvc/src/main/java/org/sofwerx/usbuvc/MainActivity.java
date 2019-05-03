package org.sofwerx.usbuvc;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("uvccam");
    }

    private static final String ACTION_USB_PERMISSION = "org.sofwerx.uvccam.USB_PERMISSION";
    private static final int PERM_REQ = 1;

    static final String TAG = "MainActivity";

    private BottomNavigationView mNavbar;
    private TextView mTextMessage;
    private Context myContext;
    private UsbManager mUsbMgr;

    /*************
     * handlers
     */

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nav_devices:
                    mTextMessage.setText(R.string.title_blank);
                    showUSBList(myContext, mTextMessage);
                    return true;
                case R.id.nav_sensors:
                    mTextMessage.setText(R.string.title_blank);
                    showSensorList(mTextMessage);
                    return true;
                case R.id.nav_clear:
                    mTextMessage.setText(R.string.title_blank);
                    return true;
            }
            return false;
        }
    };

    public final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent rcvdIntent) {
            String action = rcvdIntent.getAction();
            mTextMessage.append("Received intent broadcast " + rcvdIntent.toString() + "\n");
            Log.d(TAG, "Received intent broadcast " + rcvdIntent.toString());

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = rcvdIntent.getParcelableExtra(mUsbMgr.EXTRA_DEVICE);

                    if (rcvdIntent.getBooleanExtra(mUsbMgr.EXTRA_PERMISSION_GRANTED, true)) {
                        if(device != null){
                            //call method to set up device communication
                            usbComm(device, mTextMessage);
                        } else {
                            Log.e(TAG, "Unable to open device " + device.toString());
                        }
                    } else {
                        Log.e(TAG, "No permission to access device " + device.toString());
                    }

                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int reqCode, String[] perms, int[] grantResults) {
        boolean perms_granted = true;
        switch (reqCode) {
            case PERM_REQ: {
                for (int i = 0; i < grantResults.length; ++i) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "No permission granted for " + perms[i]);
                        mTextMessage.append("Need permission granted for " + perms[i] + "\n");
                        perms_granted = false;
                    }
                }


                if (perms_granted) {
                    setupNavBar();
                } else {
                    Log.e(TAG, "Required permissions missing");
                    mTextMessage.append("Required permissions missing.\n");
                }
            }
        }
    }


    /****************
     * onCreate (Main Activity)
     * @param savedInstanceState
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = findViewById(R.id.message);
        myContext = this;
        mUsbMgr = getSystemService(UsbManager.class);

        String[] perms = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        };

        boolean have_perms = true;
        for (String p : perms) {
            if (this.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                have_perms = false;
            }
        }
        if (have_perms) {
            setupNavBar();
        } else {
            ActivityCompat.requestPermissions(this, perms, PERM_REQ);
        }
    }

    private void setupNavBar() {

        mNavbar = findViewById(R.id.navbar);
        mNavbar.setSelectedItemId(R.id.nav_clear);
        mNavbar.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

    }


    /******
     * helpers
     *******/


    public void usbComm(UsbDevice usbdev, TextView tv) {
        UsbDeviceConnection devconn = null;
        int usbfd = 0;  // for jni lib

        if ((devconn = mUsbMgr.openDevice(usbdev)) == null) {
            tv.append("FAILED to open USB connection to device " + usbdev.toString() + "\n");
            Log.e(TAG, "Failed to open usb connection to device " + usbdev.toString());
        } else {
            usbfd = devconn.getFileDescriptor();
            UVCCam cam = new UVCCam();
            Log.d(TAG, "Calling JNI camInfo()...");
            tv.append("Calling JNI camInfo()...\n");
            tv.append(cam.camInfo(usbfd));
        }
    }


    /******
     * showUSBList
     * @param context
     * @param tv
     */

    public void showUSBList(Context context, TextView tv) {
        HashMap<String, UsbDevice> usbDevs;

        mTextMessage.setText("USB Devices:\n");

        if (mUsbMgr == null) {
            tv.append("FAILED to get UsbManager object.\n");
            Log.e(TAG, "Failed to get UsbManager object");
        } else {
            usbDevs = mUsbMgr.getDeviceList();

            for (String k : usbDevs.keySet()) {
                tv.append("\n");
                UsbDevice ud = usbDevs.get(k);

                if (ud.getDeviceClass() == UsbConstants.USB_CLASS_VIDEO) {
                    tv.append("*** USB Video Camera ***\n");

                    if (mUsbMgr.hasPermission(ud)) {
                        usbComm(ud, tv);
                    } else {
                        Intent usbIntent = new Intent(ACTION_USB_PERMISSION);
                        PendingIntent pi = PendingIntent.getBroadcast(context, 0, usbIntent, FLAG_UPDATE_CURRENT);
                        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                        registerReceiver(usbReceiver, filter);

                        mUsbMgr.requestPermission(ud, pi);
                        tv.append("Requested permission to open USB connection to device " + ud.toString() + "\n");
                        Log.i(TAG, "Requested permission to open USB connection to device " + ud.toString() + "\n");
                    }
                }

                tv.append("Device Name: " + ud.getDeviceName()+ "\n");
                tv.append("Product Name: " + ud.getProductName() + "\n");
                tv.append("Device Class: " + ud.getDeviceClass() + "\n");
                tv.append("Device Subclass: " + ud.getDeviceSubclass() + "\n");
                tv.append("Manufacturer Name: " + ud.getManufacturerName() + "\n");
                tv.append("Serial Number: " + ud.getSerialNumber() + "\n");
                tv.append("Device ID: " + ud.getDeviceId() + "\n");
                tv.append("Vendor ID: " + ud.getVendorId() + "\n");
                tv.append("Device Protocol: " + ud.getDeviceProtocol() + "\n");
                tv.append("Version: " + ud.getVersion() + "\n");
                tv.append("Interface Count: " + ud.getInterfaceCount() + "\n");
                tv.append("Configuration Count: " + ud.getConfigurationCount() + "\n");
            }

        }
    }

    public void showSensorList(TextView tv) {
        tv.append("\nDevice Built-In Sensors:\n");

        SensorManager sensorMgr;
        sensorMgr = getSystemService(SensorManager.class);

        for (Sensor s : sensorMgr.getSensorList(Sensor.TYPE_ALL)) {
            tv.append("\n");
            tv.append("Sensor Name: " + s.getName() + "\n");
            tv.append("Sensor Type (string): " + s.getStringType() + "\n");
            tv.append("Sensor ID: " + s.getId() + "\n");
            tv.append("Sensor Vendor: " + s.getVendor() + "\n");
            tv.append("Sensor Type: " + s.getType() + "\n");
            tv.append("Sensor Version: " + s.getVersion() + "\n");
            tv.append("Sensor Power: " + s.getPower() + "\n");
//            tv.append("Highest Direct Report Rate: " + s.getHighestDirectReportRateLevel() + "\n");

        }

    }

    /*******
     * JNI
     */


    class UVCCam {
        native String stringFromJNI();

        native String camInfo(int fd);
    }


}
