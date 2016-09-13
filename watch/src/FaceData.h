#ifndef _ROUTINE_FACE_DATA_H
#define _ROUTINE_FACE_DATA_H

#include "Data.h"
#include "Gfx.h"

// Base event types.
#define EVENT_TYPE_NONE					0x00

#define EVENT_TYPE_PRIORITY_BACKGROUND	0x10
#define EVENT_TYPE_PRIORITY_LOW			0x11
#define EVENT_TYPE_PRIORITY_MEDIUM		0x12
#define EVENT_TYPE_PRIORITY_HIGH		0x13

#define EVENT_TYPE_WEATHER				0x20

#define EVENT_TYPE_ASTRONOMY			0x30

// Weather types.
#define EVENT_VALUE_WEATHER_FINE		0x00
#define EVENT_VALUE_WEATHER_CLOUDY		0x10
#define EVENT_VALUE_WEATHER_RAIN		0x20

// Astronomy types.
#define EVENT_TYPE_ASTRONOMY_DAYTIME	0x10
#define EVENT_TYPE_ASTRONOMY_NIGHTTIME	0x11

#define EVENT_TYPE_ASTRONOMY_TWILIGHT_C	0x20
#define EVENT_TYPE_ASTRONOMY_TWILIGHT_N	0x21
#define EVENT_TYPE_ASTRONOMY_TWILIGHT_A	0x22

// Priority events.
static const int16_t _priorityEventRadii[] = {
	40, 42, 50, 60, 70 };

static uint16_t _PriorityEventInnerRadius() {
	return _priorityEventRadii[0];
}

static uint16_t _PriorityEventOuterRadius(EventType type) {
	size_t index = (type - EVENT_TYPE_PRIORITY_BACKGROUND) + 1;
	return _priorityEventRadii[index];
}

// Weather events.
static const gblock_t FILL_HEAVY_RAIN[] = {
	0xaaaaaaaa, 0x88888888, 0xaaaaaaaa, 0xaaaaaaaa, 0x22222222 };

static const gblock_t FILL_LIGHT_RAIN[] = {
	0x88888888, 0xaaaaaaaa, 0x22222222 };
	
static const uint16_t _weatherEventInnerRadius = 30 - 1; // Overlap hides artifacts.
static const uint16_t _weatherEventOuterRadius = 40;

static void _GetWeatherFill(EventValue weatherType,
							const gblock_t** fill, size_t* fillSize) {
	switch (weatherType) {
	case EVENT_VALUE_WEATHER_FINE:
		*fill = FILL_GS_50;
		*fillSize = 4;
		break;
		
	case EVENT_VALUE_WEATHER_RAIN:
		*fill = FILL_HEAVY_RAIN;
		*fillSize = 5;
		break;
		
	default:
		*fill = FILL_BLACK;
		*fillSize = 4;
		break;
	}
}

static void _GetAstronomyFill(EventValue astronomyType,
							  const gblock_t** fill, size_t* fillSize) {
	switch (astronomyType) {
	case EVENT_TYPE_ASTRONOMY_DAYTIME:
		*fill = FILL_GS_50;
		*fillSize = 4;
		break;
		
	case EVENT_TYPE_ASTRONOMY_TWILIGHT_C:
		*fill = FILL_GS_25;
		*fillSize = 4;
		break;
		
	case EVENT_TYPE_ASTRONOMY_TWILIGHT_N:
	case EVENT_TYPE_ASTRONOMY_TWILIGHT_A:
		*fill = FILL_GS_12;
		*fillSize = 4;
		break;
		
	case EVENT_TYPE_ASTRONOMY_NIGHTTIME:
	default:
		*fill = FILL_BLACK;
		*fillSize = 4;
		break;
	}
}

#endif
