# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
#set(GCC_COVERAGE_LINK_FLAGS -L/Users/tbirch/SOFWERX/EDGES/dev/EDGES/hrfSweepNative/app/libs/arm64-v8a)

set(LINK_LIB_DIR lib)
set(INCLUDE_DIR include)

set(LIBUSB_ROOT_REL libusb)
set(LIBUVC_ROOT_REL libuvc)

#
set(LIBUSB_LIBRARIES    ${LIBUSB_ROOT_REL}/android/libs)
set(LIBUSB_ANDROID_DIR  ${LIBUSB_ROOT_REL}/android)
set(LIBUSB_OS_DIR  ${LIBUSB_ROOT_REL}/libusb/os)
set(LIBUSB_INCLUDE_DIR  ${LIBUSB_ROOT_REL}/libusb)

set(LIBUVC_INCLUDE_DIR  ${LIBUVC_ROOT_REL}/include)
set(LIBUVC_SRC_DIR ${LIBUVC_ROOT_REL}/src)

#
link_directories(${LINK_LIB_DIR})
include_directories(${INCLUDE_DIR} ${LIBUSB_INCLUDE_DIR} ${LIBUSB_ANDROID_DIR} ${LIBUSB_OS_DIR} ${LIBUVC_INCLUDE_DIR})


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(USB_SOURCES
            ${LIBUSB_ROOT_REL}/libusb/core.c
            ${LIBUSB_ROOT_REL}/libusb/descriptor.c
            ${LIBUSB_ROOT_REL}/libusb/hotplug.c
            ${LIBUSB_ROOT_REL}/libusb/io.c
            ${LIBUSB_ROOT_REL}/libusb/sync.c
            ${LIBUSB_ROOT_REL}/libusb/strerror.c
            ${LIBUSB_ROOT_REL}/libusb/os/linux_usbfs.c
            ${LIBUSB_ROOT_REL}/libusb/os/linux_netlink.c
            ${LIBUSB_ROOT_REL}/libusb/os/poll_posix.c
            ${LIBUSB_ROOT_REL}/libusb/os/threads_posix.c
        )

set(UVC_SOURCES
            ${LIBUVC_SRC_DIR}/ctrl.c
            ${LIBUVC_SRC_DIR}/ctrl-gen.c
            ${LIBUVC_SRC_DIR}/device.c
            ${LIBUVC_SRC_DIR}/diag.c
            ${LIBUVC_SRC_DIR}/frame.c
            ${LIBUVC_SRC_DIR}/init.c
            ${LIBUVC_SRC_DIR}/stream.c
            ${LIBUVC_SRC_DIR}/misc.c
        )

add_library( # Sets the name of the library.
             uvccam

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
            ${USB_SOURCES}
            ${UVC_SOURCES}
             uvccam.c
)





###
#include_directories(${LIBUSB_ROOT_REL}/libusb)
#include_directories(${LIBUSB_ROOT_REL}/android)
#include_directories(${LIBUSB_ROOT_REL}/arm64-v8a)
#
#link_directories(${LIBUSB_ROOT_REL}/android/libs/arm64-v8a)


#add_library( # Sets the name of the library.
#                        usb-lib
#
#                        # Sets the library as a shared library.
#                        SHARED
#
#                        # Provides a relative path to your source file(s).
#                    ${LIBUSB_ROOT_REL}/libusb/core.c
#                    ${LIBUSB_ROOT_REL}/libusb/descriptor.c
#                    ${LIBUSB_ROOT_REL}/libusb/hotplug.c
#                    ${LIBUSB_ROOT_REL}/libusb/io.c
#                    ${LIBUSB_ROOT_REL}/libusb/sync.c
#                    ${LIBUSB_ROOT_REL}/libusb/strerror.c
#                    ${LIBUSB_ROOT_REL}/libusb/os/linux_usbfs.c
#                    ${LIBUSB_ROOT_REL}/libusb/os/linux_netlink.c
#                    ${LIBUSB_ROOT_REL}/libusb/os/linux_udev.c
#                    ${LIBUSB_ROOT_REL}/libusb/os/poll_posix.c
#                    ${LIBUSB_ROOT_REL}/libusb/os/threads_posix.c
#        )
#
#
#
#set (HACKRF_SRC ${HACKRF_ROOT_REL}/host/libhackrf/src)
#include_directories(${HACKRF_SRC})
#
#add_library( # Sets the name of the library.
#                        hackrf-lib
#
#                        # Sets the library as a shared library.
#                        STATIC
#
#                        # Provides a relative path to your source file(s).
#                        ${HACKRF_SRC}/hackrf.c
#            )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
#
#
#set(UVC_SOURCES ${LIBUVC_SRC_DIR}/ctrl.c ${LIBUVC_SRC_DIR}/ctrl-gen.c ${LIBUVC_SRC_DIR}/device.c ${LIBUVC_SRC_DIR}/diag.c
#        ${LIBUVC_SRC_DIR}/frame.c ${LIBUVC_SRC_DIR}/init.c ${LIBUVC_SRC_DIR}/stream.c
#        ${LIBUVC_SRC_DIR}/misc.c)
#add_library(
#        uvc-lib
#        STATIC
#        ${UVC_SOURCES}
#)

#
#find_library( # Sets the name of the path variable.
#        usb-lib
#        libusb1.0
#        )
#find_library( # Sets the name of the path variable.
#        uvc-lib
#        libuvc
#        )
find_library( # Sets the name of the path variable.
              android-lib
              android
            )

find_library(log-lib log)

#find_library(fftw3-lib fftw3)


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
#add_library(  hackrf-lib    STATIC   )

#
target_link_libraries( # Specifies the target library.
                       uvccam

                       # Links the target library to the library
                       # included in the NDK.
                       ${android-lib}
        )
#target_link_libraries( # Specifies the target library.
#        uvccam
#        ${usb-lib}
#        )
#target_link_libraries( # Specifies the target library.
#        uvccam
#                        ${uvc-lib}
#                     )


#target_link_libraries(usb-lib ${log-lib})
target_link_libraries(uvccam ${log-lib})
