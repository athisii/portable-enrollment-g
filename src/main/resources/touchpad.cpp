#include <iostream>
#include <string>
#include <cstring>
#include <fcntl.h>
#include <unistd.h>
#include <libevdev-1.0/libevdev/libevdev.h>

using namespace std;

struct TouchpadEvent
{
    int x = -1;
    int y = -1;
    int code = -1;
    int touch = -1;
};

struct TouchpadInfo
{
    int width = -1;
    int height = -1;
};

constexpr int DONT_CARE_CODE = -1;
constexpr int RESET_CODE = 0;
constexpr int X_CODE = 1;
constexpr int Y_CODE = 2;
constexpr int TOUCH_CODE = 3;
constexpr int IS_TOUCHED = 1;

class Touchpad
{
    // initial values
    libevdev* dev = nullptr;
    int fd = -1;
    input_event ev{};
    TouchpadInfo touchpadInfo{};
    TouchpadEvent touchpadEvent{};

public:
    explicit Touchpad(const char* deviceName)
    {
        initDevice(deviceName);
        fflush(stdout);
    }

    ~Touchpad()
    {
        closeDevice();
        fflush(stdout);
    }

    // return 0 if initialized, 1 if not initialized
    int isInitialized()
    {
        return (dev != nullptr && fd >= 0) ? 0 : 1;
    }

    // return 0 if closed, 1 if open
    int isClosed()
    {
        return (dev == nullptr && fd < 0) ? 0 : 1;
    }

    // close fd, dev
    void closeDevice()
    {
        if (dev)
        {
            libevdev_free(dev);
            dev = nullptr;
        }
        if (fd >= 0)
        {
            close(fd);
            fd = -1;
        }
    }

    const char* getDeviceName()
    {
        if (isInitialized() != 0)
        {
            perror("Error: Device not intialized");
            throw runtime_error("Error: Device not intialized.");
        }
        return libevdev_get_name(dev);
    }

    // intializes the device
    int initDevice(const char* devicePath)
    {
        fd = open(devicePath, O_RDONLY);
        string errorMessage;
        if (fd < 0)
        {
            errorMessage = "error opening " + string(devicePath);
            perror(errorMessage.c_str());
            return 1;
        }
        if (libevdev_new_from_fd(fd, &dev) < 0)
        {
            errorMessage = "Failed to initialize device " + string(devicePath);
            perror(errorMessage.c_str());
            close(fd);
            return 1;
        }
        return 0;
    }

    TouchpadInfo* getDeviceInfo()
    {
        if (isInitialized() != 0)
        {
            perror("Error: Device not intialized");
            throw runtime_error("Error: Device not intialized.");
        }
        // Get the ABS capabilities
        const input_absinfo* abs_x = libevdev_get_abs_info(dev, ABS_MT_POSITION_X);
        const input_absinfo* abs_y = libevdev_get_abs_info(dev, ABS_MT_POSITION_Y);

        if (abs_x && abs_y)
        {
            touchpadInfo.width = abs_x->maximum;
            touchpadInfo.height = abs_y->maximum;
            return &touchpadInfo;
        }
        perror("Failed to get ABS_MT_POSITION_X or ABS_MT_POSITION_Y info.");
        throw runtime_error("Failed to get ABS_MT_POSITION_X or ABS_MT_POSITION_Y info.");
    }

    TouchpadEvent* getNextEvent()
    {
        if (isInitialized() != 0)
        {
            perror("Error: Device not intialized");
            throw runtime_error("Error: Device not intialized.");
        }
        int rc = libevdev_next_event(dev, LIBEVDEV_READ_FLAG_NORMAL, &ev);

        if (rc == LIBEVDEV_READ_STATUS_SUCCESS || rc == LIBEVDEV_READ_STATUS_SYNC)
        {
            if (ev.type == EV_ABS)
            {
                if (ev.code == ABS_MT_TRACKING_ID)
                {
                    // cout << "\ntime: " << ev.time.tv_usec << " type 3 (EV_ABS), code " << ev.code <<" (ABS_MT_TRACKING_ID), value " << ev.value;
                    // fflush(stdout);
                    touchpadEvent.code = DONT_CARE_CODE;
                    return &touchpadEvent;
                }
                if (ev.code == ABS_MT_POSITION_X)
                {
                    // cout << "\ntime: " << ev.time.tv_usec << " type 3 (EV_ABS), code " << ev.code <<" (ABS_MT_POSITION_X), value " << ev.value;
                    // fflush(stdout);
                    touchpadEvent.code = DONT_CARE_CODE;
                    return &touchpadEvent;
                }
                if (ev.code == ABS_MT_POSITION_Y)
                {
                    // cout << "\ntime: " << ev.time.tv_usec << " type 3 (EV_ABS), code " << ev.code <<" (ABS_MT_POSITION_Y), value " << ev.value;
                    // fflush(stdout);
                    touchpadEvent.code = DONT_CARE_CODE;
                    return &touchpadEvent;
                }
                if (ev.code == ABS_X)
                {
                    // cout << "\ntime: " << ev.time.tv_usec << " type 3 (EV_ABS), code " << ev.code << " (ABS_X), value " <<ev.value;
                    // // fflush(stdout);
                    touchpadEvent.code = X_CODE;
                    touchpadEvent.x = ev.value;
                    return &touchpadEvent;
                }
                if (ev.code == ABS_Y)
                {
                    // cout << "\ntime: " << ev.time.tv_usec << " type 3 (EV_ABS), code " << ev.code << " (ABS_Y), value "<< ev.value;
                    // fflush(stdout);
                    touchpadEvent.code = Y_CODE;
                    touchpadEvent.y = ev.value;
                    return &touchpadEvent;
                }
                touchpadEvent.code = DONT_CARE_CODE;
                return &touchpadEvent;
            }
            if (ev.type == EV_KEY)
            {
                if (ev.code == BTN_TOUCH)
                {
                    // check value
                    // 1 means touching the touchpad
                    // 0 means not touching the touchpad
                    // cout << "\ntime: " << ev.time.tv_usec << " type 1 (EV_KEY), code " << ev.code <<" (BTN_TOUCH), value "<<ev.value;
                    // fflush(stdout);
                    touchpadEvent.code = TOUCH_CODE;
                    touchpadEvent.touch = ev.value;
                    return &touchpadEvent;
                }
                if (ev.code == BTN_TOOL_FINGER)
                {
                    // cout << "\ntime: " << ev.time.tv_usec << " type 1 (EV_KEY), code " << ev.code <<" (BTN_TOOL_FINGER), value " << ev.value;
                    // fflush(stdout);
                    touchpadEvent.code = DONT_CARE_CODE;
                    return &touchpadEvent;
                }
                // cout << "\nDon't care event encouterd.\nEvent type: EV_KEY; code: " << ev.code << "; value: " << ev.
                //     value << endl;
                touchpadEvent.code = DONT_CARE_CODE;
                return &touchpadEvent;
            }
            if (ev.type == EV_SYN)
            {
                // cout << "\ntime: " << ev.time.tv_usec << "  -------------- SYN_REPORT ------------";
                // fflush(stdout);
                touchpadEvent.code = RESET_CODE;
                return &touchpadEvent;
            }
            if (ev.type == EV_MSC)
            {
                // cout << "\ntime: " << ev.time.tv_usec << " type 4 (EV_MSC), code " << ev.code <<" (MSC_TIMESTAMP), value "<<ev.value;
                // fflush(stdout);
                touchpadEvent.code = DONT_CARE_CODE;
                return &touchpadEvent;
            }
        }
        // cout << "\ntime: " << ev.time.tv_usec << " type" << ev.type << ", code " << ev.code << " , value " <<ev.value;
        // fflush(stdout);
        touchpadEvent.code = DONT_CARE_CODE;
        return &touchpadEvent;
    }
};

extern "C" {
int getDontCareCode()
{
    return DONT_CARE_CODE;
}

int getResetCode()
{
    return RESET_CODE;
}

int getXCode()
{
    return X_CODE;
}

int getYCode()
{
    return Y_CODE;
}

int getTouchCode()
{
    return TOUCH_CODE;
}

int isTouched()
{
    return IS_TOUCHED;
}

Touchpad* initDevice(const char* deviceName)
{
    return new Touchpad(deviceName);
}

int isInitialized(Touchpad* touchpad)
{
    return touchpad->isInitialized();
}

int isClosed(Touchpad* touchpad)
{
    return touchpad->isClosed();
}

void closeDevice(Touchpad* touchpad)
{
    touchpad->closeDevice();
    delete touchpad;
}

const char* getDeviceName(Touchpad* touchpad)
{
    return touchpad->getDeviceName();
}

TouchpadInfo* getDeviceInfo(Touchpad* touchpad)
{
    return touchpad->getDeviceInfo();
}

TouchpadEvent* getNextEvent(Touchpad* touchpad)
{
    return touchpad->getNextEvent();
}

int main()
{
    Touchpad* touchpad = initDevice("/dev/input/event5");
    const TouchpadInfo* touchpadInfo = touchpad->getDeviceInfo();
    cout << "Max X: " << touchpadInfo->width << "; Max Y: " << touchpadInfo->height << endl;
    while (true)
    {
        TouchpadEvent* touchpadEvent = touchpad->getNextEvent();
        if (touchpadEvent->code == X_CODE)
        {
            cout << "\nx: " << touchpadEvent->x;
        }
        else if (touchpadEvent->code == Y_CODE)
        {
            cout << "\ny: " << touchpadEvent->y;
        }
        else if (touchpadEvent->code == RESET_CODE)
        {
            {
                cout << "\n********************** one meaningful event ****************************";
                cout << "\nx: " << touchpadEvent->x << "; y: " << touchpadEvent->y;
                cout << "\n********************** one meaningful event ****************************\n";
            }
        }
        else if (touchpadEvent->code == TOUCH_CODE)
        {
            if (touchpadEvent->touch == IS_TOUCHED)
            {
                cout << "\nTouched started.";
            }
            else
            {
                cout << "\nTouch ended";
            }
        }
    }
    closeDevice(touchpad);
    return 0;
}
}
