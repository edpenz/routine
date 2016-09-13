#include <time.h>
#include <pebble.h>

#include "Face.h"
#include "Gfx.h"
#include "Data.h"

Window* window;

static void _HandleTick(struct tm *now, TimeUnits cause) {
    // Find largest TimeUnits change, not just what was registered.
    static struct tm _lastTick = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    cause = SECOND_UNIT;
    if (now->tm_min != _lastTick.tm_min) cause = MINUTE_UNIT;
    if (now->tm_hour != _lastTick.tm_hour) cause = HOUR_UNIT;
    if (now->tm_mday != _lastTick.tm_mday) cause = DAY_UNIT;
    if (now->tm_mon != _lastTick.tm_mon) cause = MONTH_UNIT;
    if (now->tm_year != _lastTick.tm_year) cause = YEAR_UNIT;
    _lastTick = *now;
    
    // Update state depending on changed TimeUnits.
    switch (cause) {
    case YEAR_UNIT:
    case MONTH_UNIT:
    case DAY_UNIT:
		face_SetDate(now);
		
		if (!data_LoadSchedule(now)) face_ShowSyncError();
        
    case HOUR_UNIT:
		face_SetHours(now->tm_hour == 0 ? 12 : now->tm_hour);

    case MINUTE_UNIT:
		face_SetMinutes(now->tm_min);
		face_SetHand(now->tm_hour, now->tm_min);
        
    case SECOND_UNIT:
		face_SetSeconds(now->tm_sec);
        break;
    }
}

static void _Init(void) {
    // Subscribe to data channel.
	data_Init();
	
    //Create the window.
    window = window_create();
    window_set_window_handlers(window, (WindowHandlers) {
        .load = face_Init,
        .unload = face_Deinit,
    });
    
    // Start keeping time.
    tick_timer_service_subscribe(SECOND_UNIT, _HandleTick);
    
    time_t nowTime = time(NULL);
    struct tm* nowDate = localtime(&nowTime);
    _HandleTick(nowDate, YEAR_UNIT);
    
    // Show the watchface.
    window_stack_push(window, true);
}

static void _Deinit(void) {
    data_Deinit();
    
    tick_timer_service_unsubscribe();
    // TODO Error here? window_destroy(window);
}

int main(void) {
    _Init();
    app_event_loop();
    _Deinit();
}
