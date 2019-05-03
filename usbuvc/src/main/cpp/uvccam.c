//
// Created by Tracey Birch on 4/8/19.
//

#include "uvccam.h"

#include <stdio.h>

#include <libuvc/libuvc.h>

#include <libusb/libusb.h>
#include <libusb/libusbi.h>
#include <libuvc/libuvc_internal.h>
uvc_error_t uvc_scan_control(uvc_device_t *dev, uvc_device_info_t *info);

//void uvcCallback(uvc_frame_t *frame, void *ptr);

char frameinfo[5000] = "";


#define ENABLE_DEBUG_LOGGING

JNIEXPORT jstring JNICALL Java_org_sofwerx_usbuvc_MainActivity_00024UVCCam_stringFromJNI(JNIEnv *env, jobject this) {
	return(*env)->NewStringUTF(env, "\nHello from JNI (C library)\n");
}

void uvcCallback(uvc_frame_t *frame, void *ptr) {
	uvc_error_t rc;

	sprintf(frameinfo, "%s\nFrame #%04d - width: %d, height: %d, data bytes: %d", frameinfo, frame->sequence, frame->width, frame->height, frame->data_bytes);

	return;
}

JNIEXPORT jstring JNICALL Java_org_sofwerx_usbuvc_MainActivity_00024UVCCam_camInfo(JNIEnv *env, jobject this, jint fd) {

	const struct libusb_version *usbV = libusb_get_version();
	libusb_context *usbCtx = NULL;
	libusb_device ***usbDevList = NULL;

	libusb_device *camDev = NULL;
	libusb_device_handle *camDevHandle = NULL;
	struct libusb_device_descriptor camDesc;

	char retstr[5000] = "";

	sprintf(retstr, "\nlibusb version %d.%d.%d", usbV->major, usbV->minor, usbV->micro);

	int rc = libusb_init(&usbCtx);
	if (rc < 0) {
		sprintf(retstr, "%s\nFailed to initialize libusb: %s\n", retstr, libusb_error_name(rc));
		return(*env)->NewStringUTF(env, retstr);
	} else {
		sprintf(retstr, "%s\nlibusb initialized", retstr);
	}

	rc = libusb_wrap_sys_device(usbCtx, fd, &camDevHandle);
	if (rc == 0) {
		// Success
		sprintf(retstr, "%s\nGot libusb device handle", retstr);
	} else {
		sprintf(retstr, "%s\nFailed to get libusb device handle: %s\n", retstr, libusb_error_name(rc));
		return(*env)->NewStringUTF(env, retstr);
	}

	camDev = libusb_get_device(camDevHandle);

	rc = libusb_get_device_descriptor(camDev, &camDesc);
	if (rc == 0) {
		// Success
		sprintf(retstr, "%s\nGot libusb device descriptor:", retstr);
		sprintf(retstr, "%s\n   Vendor ID: %#04x\n  Product ID: %#04x", retstr, camDesc.idVendor, camDesc.idProduct);

	} else {
		sprintf(retstr, "%s\nFailed to get libusb device descriptor: %s\n", retstr, libusb_error_name(rc));
		return(*env)->NewStringUTF(env, retstr);
	}


	uvc_context_t *uvcCtx;
	uvc_error_t res;

	uvc_device_t *uvcDev;
	uvc_device_handle_t *uvcDevHandle;

	res = uvc_init(&uvcCtx, usbCtx);
	if (res < 0) {
		sprintf(retstr, "%s\nError in uvc_init: %s\n", retstr, uvc_strerror(res));
		return(*env)->NewStringUTF(env, retstr);
	} else {
		sprintf(retstr, "%s\nUVC initialized", retstr);
	}

	uvcDev = calloc(1, sizeof(*uvcDev));
	uvcDevHandle = calloc(1, sizeof(*uvcDevHandle));

	if(uvcDev == NULL || uvcDevHandle == NULL) {
		sprintf(retstr, "%s\nError trying to allocate UVC device structs\n", retstr);
		return(*env)->NewStringUTF(env, retstr);
	}

	uvcDev->usb_dev = camDev;
	uvcDev->ctx = uvcCtx;
	uvc_ref_device(uvcDev);

	uvcDevHandle->dev = uvcDev;
	uvcDevHandle->usb_devh = camDevHandle;
	uvcDevHandle->next = uvcDevHandle->prev = uvcDevHandle;

	uvcDevHandle->info = calloc(1, sizeof(uvc_device_info_t));
	libusb_get_config_descriptor(uvcDev->usb_dev, 0, &(uvcDevHandle->info->config));

	rc = uvc_scan_control(uvcDev, uvcDevHandle->info);
	if (rc == UVC_SUCCESS) {
		sprintf(retstr, "%s\nuvc_scan_control success", retstr);
	} else {
		sprintf(retstr, "%s\nuvc_scan_control failed: %s\n", retstr, uvc_strerror(rc));
		return(*env)->NewStringUTF(env, retstr);
	}

	uvcCtx->open_devices = uvcDevHandle;
	char diagstr[1000] = "";
	uvc_print_diag(uvcDevHandle, fmemopen(diagstr, 1000, "w+"));
	sprintf(retstr, "%s\nCamera configuration & capabilities:\n%s\n", retstr, diagstr);

	uvc_stream_ctrl_t *uvcCtrl;
	uvcCtrl = calloc(1, sizeof(*uvcCtrl));
	char ctrlstr[1000] = "";

	uvc_get_stream_ctrl_format_size(uvcDevHandle, uvcCtrl, UVC_FRAME_FORMAT_MJPEG, 320, 240, 15);
	uvc_print_stream_ctrl(uvcCtrl, fmemopen(ctrlstr, 1000, "w+"));
	sprintf(retstr, "%s\nStream control block:\n%s\n", retstr, ctrlstr);

	uvc_stream_handle_t **streamHP;
	uvc_frame_t **frameP;
	uvc_frame_t *frame;
	rc = uvc_stream_open_ctrl(uvcDevHandle, streamHP, uvcCtrl);
	rc = uvc_stream_get_frame(streamHP, frameP, 500);
	frame = *frameP;
	sprintf(frameinfo, "%s\nFrame #%04d - width: %d, height: %d, data bytes: %d", frameinfo, frame->sequence, frame->width, frame->height, frame->data_bytes);


//	int secs = 1;
//	sprintf(retstr, "%s\nAbout to start streaming for %d second%s...", retstr, secs, (secs == 1 ? "" : "s"));
//	rc = uvc_start_streaming(uvcDevHandle, uvcCtrl, uvcCallback, 0, 0);
//	sleep(secs);
//	uvc_stop_streaming(uvcDevHandle);

	sprintf(retstr, "%s\n%s\n", retstr, frameinfo);

	sprintf(retstr, "%s\n\n", retstr);
	return(*env)->NewStringUTF(env, retstr);
}

