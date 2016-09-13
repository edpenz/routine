#ifndef _PEBBLE_PERSIST_SYNC_H
#define _PEBBLE_PERSIST_SYNC_H

typedef enum {
	PERSIST_BOOL		= 0,
	PERSIST_BYTE_ARRAY	= 1,
	PERSIST_INT			= 2,
	PERSIST_CSTRING		= 3
} PersistType;

typedef void(* PersistSyncChangedCallback)(
	const uint32_t key,const PersistType type,
	const void *data, const uint32_t dataLength);

bool persist_sync_init(PersistSyncChangedCallback callback);
void persist_sync_deinit(void);

void persist_sync_poll(void);

#endif
