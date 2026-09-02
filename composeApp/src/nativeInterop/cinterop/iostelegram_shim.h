#include <stdint.h>

int32_t NuvioTelegramStart(int32_t api_id, const char *api_hash, const char *app_version);
char *NuvioTelegramRequest(const char *json, double timeout_seconds);
char *NuvioTelegramPlaybackURL(
    int32_t file_id,
    int64_t file_size,
    const char *file_name,
    const char *mime_type
);
int64_t NuvioTelegramCacheSize(void);
void NuvioTelegramClearCache(void);
void NuvioTelegramFree(char *value);
