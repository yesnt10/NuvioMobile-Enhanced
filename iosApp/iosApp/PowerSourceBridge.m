#include <CoreFoundation/CoreFoundation.h>
#include <stdint.h>

CFTypeRef IOPSCopyPowerSourcesInfo(void);
CFArrayRef IOPSCopyPowerSourcesList(CFTypeRef blob);
CFDictionaryRef IOPSGetPowerSourceDescription(CFTypeRef blob, CFTypeRef ps);

static int NuvioIOPSValidPercent(int percent) {
    return percent >= 0 && percent <= 100;
}

static int NuvioIOPSReadInt(CFDictionaryRef dictionary, const char *key) {
    if (dictionary == NULL || key == NULL) {
        return -1;
    }

    CFStringRef keyRef = CFStringCreateWithCString(
        kCFAllocatorDefault,
        key,
        kCFStringEncodingUTF8
    );
    if (keyRef == NULL) {
        return -1;
    }

    const void *valueRef = CFDictionaryGetValue(dictionary, keyRef);
    CFRelease(keyRef);
    if (valueRef == NULL || CFGetTypeID(valueRef) != CFNumberGetTypeID()) {
        return -1;
    }

    int value = -1;
    return CFNumberGetValue((CFNumberRef)valueRef, kCFNumberIntType, &value) ? value : -1;
}

static int NuvioIOPSReadBool(CFDictionaryRef dictionary, const char *key) {
    if (dictionary == NULL || key == NULL) {
        return -1;
    }

    CFStringRef keyRef = CFStringCreateWithCString(
        kCFAllocatorDefault,
        key,
        kCFStringEncodingUTF8
    );
    if (keyRef == NULL) {
        return -1;
    }

    const void *valueRef = CFDictionaryGetValue(dictionary, keyRef);
    CFRelease(keyRef);
    if (valueRef == NULL || CFGetTypeID(valueRef) != CFBooleanGetTypeID()) {
        return -1;
    }
    return CFBooleanGetValue((CFBooleanRef)valueRef) ? 1 : 0;
}

int NuvioIOPowerSourceBatteryPercent(void) {
    CFTypeRef snapshot = IOPSCopyPowerSourcesInfo();
    if (snapshot == NULL) {
        return -1;
    }

    CFArrayRef sources = IOPSCopyPowerSourcesList(snapshot);
    if (sources == NULL) {
        CFRelease(snapshot);
        return -1;
    }

    int percent = -1;
    CFIndex count = CFArrayGetCount(sources);
    for (CFIndex index = 0; index < count; index++) {
        CFTypeRef source = CFArrayGetValueAtIndex(sources, index);
        CFDictionaryRef description = IOPSGetPowerSourceDescription(snapshot, source);
        if (description == NULL) {
            continue;
        }

        int current = NuvioIOPSReadInt(description, "Current Capacity");
        int maximum = NuvioIOPSReadInt(description, "Max Capacity");
        if (current >= 0 && maximum > 0) {
            percent = (int)(((int64_t)current * 100 + (maximum / 2)) / maximum);
            if (NuvioIOPSValidPercent(percent)) {
                break;
            }
            percent = -1;
        }
    }

    CFRelease(sources);
    CFRelease(snapshot);
    return percent;
}

int NuvioIOPowerSourceBatteryCharging(void) {
    CFTypeRef snapshot = IOPSCopyPowerSourcesInfo();
    if (snapshot == NULL) {
        return -1;
    }

    CFArrayRef sources = IOPSCopyPowerSourcesList(snapshot);
    if (sources == NULL) {
        CFRelease(snapshot);
        return -1;
    }

    int charging = -1;
    CFIndex count = CFArrayGetCount(sources);
    for (CFIndex index = 0; index < count; index++) {
        CFTypeRef source = CFArrayGetValueAtIndex(sources, index);
        CFDictionaryRef description = IOPSGetPowerSourceDescription(snapshot, source);
        if (description == NULL) {
            continue;
        }

        charging = NuvioIOPSReadBool(description, "Is Charging");
        if (charging >= 0) {
            break;
        }
    }

    CFRelease(sources);
    CFRelease(snapshot);
    return charging;
}
