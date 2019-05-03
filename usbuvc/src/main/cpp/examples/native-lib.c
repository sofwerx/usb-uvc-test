//
// Created by Tracey Birch on 6/28/18.
//

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <jni.h>

#include <android/sensor.h>
#include <libusb.h>
#include <libusbi.h>
#include <hackrf.h>
#include <fftw3.h>

/*************
 * From hackrf.c
 */
#define TRANSFER_COUNT 4
#define TRANSFER_BUFFER_SIZE 262144
#define USB_MAX_SERIAL_LENGTH 32
struct hackrf_device {
    libusb_device_handle* usb_device;
    struct libusb_transfer** transfers;
    hackrf_sample_block_cb_fn callback;
    volatile bool transfer_thread_started; /* volatile shared between threads (read only) */
    pthread_t transfer_thread;
    volatile bool streaming; /* volatile shared between threads (read only) */
    void* rx_ctx;
    void* tx_ctx;
    unsigned char buffer[TRANSFER_COUNT * TRANSFER_BUFFER_SIZE];
};

/*************
 * From hackrf.c
 */

int usb_fd_to_hackrf(libusb_device_handle* usb_device, hackrf_device** device)
{
    int result;
    hackrf_device* lib_device;

    lib_device = NULL;
    lib_device = (hackrf_device*)malloc(sizeof(struct hackrf_device));
    if( lib_device == NULL )
    {
        return HACKRF_ERROR_NO_MEM;
    }

    lib_device->usb_device = usb_device;
    lib_device->transfers = NULL;
    lib_device->callback = NULL;
    lib_device->transfer_thread_started = false;
    lib_device->streaming = false;

    *device = lib_device;

    return HACKRF_SUCCESS;
}

JNIEXPORT jstring JNICALL Java_org_sofwerx_hrfsweepnative_HelloFromNative_stringFromJNI(JNIEnv *env, jobject this) {
                return(*env)->NewStringUTF(env, "Hello from JNI (C library)");
}


JNIEXPORT jstring JNICALL Java_org_sofwerx_hrfsweepnative_SensorInterface_showHackRF(JNIEnv *env, jobject this, jint fd) {

    const struct libusb_version *usbV = libusb_get_version();
    libusb_context *usbCtx;
    libusb_device ***usbDevs;
    libusb_device_handle realDevHandle;
    libusb_device_handle *ptrDevHandle = &realDevHandle;
    libusb_device_handle **hrfDevHandle = &ptrDevHandle;
    libusb_device *hrfDev;
    hackrf_device *radioDev;
    struct libusb_device_descriptor hrfDevDesc;
    unsigned char *hrfMfr, *hrfProd, *hrfSN;
    char *sweepBuf;
    int sweepBufLen = 5000;
    hrfMfr = calloc(1000, sizeof(char));
    hrfProd = calloc(1000, sizeof(char));
    hrfSN = calloc(1000, sizeof(char));
    sweepBuf = calloc(sweepBufLen, sizeof(char));
    read_partid_serialno_t read_partid_serialno;

    int n;
    char retstr[5000] = "";

    sprintf(retstr, "\n\nlibusb version %d.%d.%d", usbV->major, usbV->minor, usbV->micro);

    int rc = libusb_init(&usbCtx);
    if (rc < 0) {
        sprintf(retstr, "%s\nFAILED to initialize libusb: %s", retstr, libusb_error_name(rc));
    } else {
        sprintf(retstr, "%s\nlibusb initialized", retstr);

        libusb_wrap_fd(usbCtx, fd, hrfDevHandle);
        hrfDev = libusb_get_device(*hrfDevHandle);
        (*hrfDevHandle)->dev = hrfDev;
        libusb_ref_device(hrfDev);

        // TODO: WHY IS hrfDevHandle.dev a null ptr????
        libusb_get_device_descriptor(hrfDev, &hrfDevDesc);

        sprintf(retstr, "%s\nFrom libusb...", retstr);
        sprintf(retstr, "%s\nVendor ID: %d\nProduct ID: %d", retstr, hrfDevDesc.idVendor,
                hrfDevDesc.idProduct);

        sprintf(retstr, "%s\n\nFrom libhackrf...", retstr);
        rc = usb_fd_to_hackrf(*hrfDevHandle, &radioDev);

        char *ver = NULL;
        uint8_t len = 0;
        uint8_t bid = 0xff;

        rc = hackrf_board_id_read(radioDev, &bid);
        rc = hackrf_version_string_read(radioDev, ver, len);
        rc = hackrf_board_partid_serialno_read(radioDev, &read_partid_serialno);

        sprintf(retstr, "%s\nBoard ID: %d (%s)", retstr, bid, hackrf_board_id_name(bid));
        sprintf(retstr, "%s\nVersion: %s", retstr, ver);

        sprintf(retstr, "%s\nS/N: %x %x %x %x %x %x", retstr, read_partid_serialno.part_id[0],
                read_partid_serialno.part_id[0],
                read_partid_serialno.serial_no[0], read_partid_serialno.serial_no[1],
                read_partid_serialno.serial_no[2], read_partid_serialno.serial_no[3]);

        uint16_t scanFreqs[2] = {1,7200};
        //rc = hackrf_init_sweep(radioDev, scanFreqs, 1, (8192 * 2), 20000000, 7500000, INTERLEAVED);

        rc = sweep_main(sweepBuf, sweepBufLen);

        if (rc == EXIT_SUCCESS) {
            sprintf(retstr, "%s\nSweep mode initialized successfully", retstr);
        } else {
            sprintf(retstr, "%s\nERROR initializing sweep mode: %s (%d)", retstr, hackrf_error_name(rc), rc);
        }
        sprintf(retstr, "%s\n\n%s", retstr, sweepBuf);
    }
    return (*env)->NewStringUTF(env, retstr);
}

JNIEXPORT jstring JNICALL Java_org_sofwerx_hrfsweepnative_SensorInterface_listSensors(JNIEnv *env, jobject this) {
    ASensorManager *sensman = ASensorManager_getInstance();
    ASensorList senslist;
    char retstr[5000] = "";
    int n;


    n = ASensorManager_getSensorList(sensman, &senslist);
    for (int i = 0; i < n; ++i) {
        const ASensor *s = senslist[i];
        sprintf(retstr, "%s\n%s", retstr, ASensor_getName(s));
    }

    return (*env)->NewStringUTF(env, retstr);
}



