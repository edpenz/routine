#ifndef _ROUTINE_DATA_H
#define _ROUTINE_DATA_H

#include <pebble.h>

// Event data wrapper.
typedef uint8_t EventType;
typedef uint8_t EventValue;
typedef uint16_t EventTime;

typedef struct {
	EventType Type;
	EventValue Value;
	
	EventTime Start;
	EventTime End;
} Event;

void data_Init();
void data_Deinit();

bool data_LoadSchedule(struct tm *time);

uint32_t data_GetScheduleDate();
bool data_GetNextEvent(Event* event, void** iterator);

#endif
