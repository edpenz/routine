#include "Data.h"

#include "PersistSync.h"

#define SCHEDULE_KEY_FIRST		0
#define SCHEDULE_KEY_LAST		3
#define SCHEDULE_KEY_COUNT		(SCHEDULE_KEY_LAST - SCHEDULE_KEY_FIRST + 1)

static uint8_t loadedSchedule[256] = { 0, 0, 0, 0, 0 };

static uint32_t _JulianDay(uint16_t year, uint8_t month, uint8_t day) {
	// From http://en.wikipedia.org/wiki/Julian_day
	uint32_t a = (14 - month) / 12;
	uint32_t y = year + 4800 - a;
	uint32_t m = month + 12 * a - 3;
	return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
}

static uint32_t _GetJulianDay(struct tm *time) {
	return _JulianDay(time->tm_year + 1900, time->tm_mon + 1, time->tm_mday);
}

static uint32_t _Today() {
	time_t now = time(NULL);
	return _GetJulianDay(localtime(&now));
}

static uint32_t _DecodeBEuint32_t(const uint8_t *data) {
	return
		data[0] << 24 |
		data[1] << 16 |
		data[2] << 8 |
		data[3] << 0;
}

static uint32_t _DecodeBEuint16_t(const uint8_t *data) {
	return
		data[0] << 8 |
		data[1] << 0;
}

static bool _LoadSchedule(uint32_t key);

static void _OnDataUpdated(const uint32_t key,const PersistType type,
					       const void *data, const uint32_t dataLength) {
	if (/*key >= SCHEDULE_KEY_FIRST &&*/ key <= SCHEDULE_KEY_LAST) {
		uint32_t keyOffset = key - SCHEDULE_KEY_FIRST;
		uint32_t scheduleOffset = data_GetScheduleDate() % SCHEDULE_KEY_COUNT;
		
		if (keyOffset != scheduleOffset ) {
			return;
		} else if(data != NULL) {
			memcpy(loadedSchedule, data, dataLength);
		} else {
			_LoadSchedule(key);
		}
	}
}

void data_Init() {
	persist_sync_init(_OnDataUpdated);
}

void data_Deinit() {
	persist_sync_deinit();
}

static bool _LoadSchedule(uint32_t key) {
	if (!persist_exists(key)) return false;
	
	int length = persist_get_size(key);
	persist_read_data(key, loadedSchedule, length);
	APP_LOG(APP_LOG_LEVEL_INFO, "Loaded %dB schedule", length);
	
	return true;
}

bool data_LoadSchedule(struct tm *time) {
	uint32_t day = _GetJulianDay(time);
	APP_LOG(APP_LOG_LEVEL_INFO, "Finding schedule for JD %d", (int)day);
	
	for (uint32_t key = SCHEDULE_KEY_FIRST; key <= SCHEDULE_KEY_LAST; ++key) {
		bool scheduleLoaded = _LoadSchedule(key);
		if (scheduleLoaded && day == data_GetScheduleDate()) {
			return true;
		}
	}
	
	APP_LOG(APP_LOG_LEVEL_WARNING, "Schedule not found");
	return false;
}

uint32_t data_GetScheduleDate() {
	return _DecodeBEuint32_t(loadedSchedule);
}

bool data_GetNextEvent(Event* event, void** iterator) {
	uint8_t* offset = *iterator;
	if (offset == NULL) {
		offset = loadedSchedule + 4;
	}
	
	EventType type = *offset; offset += 1;
	if (type != 0) {
		event->Type = type;
		event->Value = *offset; offset += 1;
		event->Start = _DecodeBEuint16_t(offset); offset += 2;
		event->End = _DecodeBEuint16_t(offset); offset += 2;
		
		*iterator = offset;
		return true;
	} else {
		return false;
	}
}
