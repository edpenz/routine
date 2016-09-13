#include <pebble.h>

#include "PersistSync.h"

#define KEY_PERSIT_TYPE							0
#define KEY_PERSIT_KEY							1
#define KEY_PERSIT_DATA							2

#define PERSIST_SYNC_DESIRED_INBOX_SIZE			128
#define PERSIST_SYNC_DESIRED_OUTBOX_SIZE		4

static void _receive(DictionaryIterator *, void *);

static PersistSyncChangedCallback _callback = NULL;

bool persist_sync_init(PersistSyncChangedCallback callback) {
	_callback = callback;
	
	app_message_register_inbox_received(_receive);
	
	uint32_t inboxSize = app_message_inbox_size_maximum();
	if (inboxSize > PERSIST_SYNC_DESIRED_INBOX_SIZE) {
		inboxSize = PERSIST_SYNC_DESIRED_INBOX_SIZE;
	}
	
	uint32_t outboxSize = app_message_outbox_size_maximum();
	if (outboxSize > PERSIST_SYNC_DESIRED_OUTBOX_SIZE) {
		outboxSize = PERSIST_SYNC_DESIRED_OUTBOX_SIZE;
	}
	
	AppMessageResult result = app_message_open(inboxSize, outboxSize);
	return (result == APP_MSG_OK);
}

void persist_sync_deinit(void) {
	app_message_deregister_callbacks();
}

void persist_sync_poll(void) {
	
}

static void _receive(DictionaryIterator *iterator, void *context) {
	Tuple *type = dict_find(iterator, KEY_PERSIT_TYPE);
	if (type == NULL || type->type != TUPLE_UINT || type->length != 1) return;
	
	Tuple *key = dict_find(iterator, KEY_PERSIT_KEY);
	if (key == NULL || key->type != TUPLE_UINT || key->length != 4) return;
	
	Tuple *data = dict_find(iterator, KEY_PERSIT_DATA);
	if (data == NULL || data->type != TUPLE_BYTE_ARRAY) return;
	
	PersistType persistType = type->value[0].uint8;
	uint32_t persistKey = key->value[0].uint32;
	uint32_t persistLength = data->length;
	
	// TODO Verify correct length of data was written.
	switch (persistType) {
	case PERSIST_BOOL: {
		bool boolValue = (data->value[0].uint8 != 0);
		persist_write_bool(persistKey, boolValue);
		if (_callback) _callback(persistKey, persistType, &boolValue, 0);
		break;
	}
	case PERSIST_BYTE_ARRAY: {
		uint8_t *dataValue = data->value[0].data;
		persist_write_data(persistKey, dataValue, persistLength);
		if (_callback) _callback(persistKey, persistType, dataValue, persistLength);
		break;
	}
	case PERSIST_INT: {
		uint32_t intValue = data->value[0].int32;
		persist_write_int(persistKey, intValue);
		if (_callback) _callback(persistKey, persistType, &intValue, 0);
		break;
	}
	case PERSIST_CSTRING: {
		char *stringValue = data->value[0].cstring;
		persist_write_string(persistKey, stringValue);
		if (_callback) _callback(persistKey, persistType, stringValue, persistLength);
		break;
	}
	}
}
