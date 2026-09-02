#include <CoreFoundation/CoreFoundation.h>
#include <stdint.h>

CFTypeRef IOPSCopyPowerSourcesInfo(void);
CFArrayRef IOPSCopyPowerSourcesList(CFTypeRef blob);
CFDictionaryRef IOPSGetPowerSourceDescription(CFTypeRef blob, CFTypeRef ps);

int NuvioIOPowerSourceBatteryPercent(void);
int NuvioIOPowerSourceBatteryCharging(void);
