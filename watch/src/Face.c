#include "Face.h"
#include "FaceData.h"

#include <time.h>
#include <pebble_fonts.h>

#include "Gfx.h"
#include "Data.h"

// -------------------------------- Static -------------------------------------

static const GPathInfo _shapeHand = {
    .num_points = 5,
    .points = (GPoint []) {
		{40-40, 0-40}, // Top
		{60-40, 15-40}, {60-40, 17-40}, // Right
		{20-40, 17-40}, {20-40, 15-40} // Left
	}
};

static const int16_t _textX = 23;
static const int16_t _textY = 12;

static const int16_t _priorities[] = { 40, 42, 50, 60, 70 };

// --------------------------------- State -------------------------------------

static Layer        *_layerRoot = NULL;

// Schedule.
static Layer        *_layerSchedule = NULL;

// Nucleus.
static int32_t      _angleHand = 0;
static GPath        *_pathHand = NULL;

static Layer        *_layerNucleus = NULL;

// Time.
static char         _textDate[9] = "10th Jan";
static GFont        _fontDate = NULL;
static TextLayer    *_layerDate = NULL;

static char         _textHours[3] = "00";
static GFont        _fontHours = NULL;
static TextLayer    *_layerHours = NULL;

static char         _textMinutes[3] = "00";
static GFont        _fontMinutes = NULL;
static TextLayer    *_layerMinutes = NULL;

static char         _textSeconds[3] = "00";
static GFont        _fontSeconds = NULL;
static TextLayer    *_layerSeconds = NULL;

// Sync.
static bool			_hiddenSync = true;
static const char	_textSync[] = "Out-of-sync";
static TextLayer    *_layerSync = NULL;

// ---------------------------- Private functions ------------------------------

static int32_t _AngleForTime(int16_t hour, int16_t minute) {
    uint32_t angle = (hour * 60 + minute) * TRIG_MAX_ANGLE / (24 * 60);
    angle = angle % TRIG_MAX_ANGLE;
    return angle;
}

static void _DrawPriorityEvent(Event* event, GContext* g) {
	int16_t inner = _PriorityEventInnerRadius();
	int16_t outer = _PriorityEventOuterRadius(event->Type);
	
	int32_t start = _AngleForTime(0, event->Start);
	int32_t end = _AngleForTime(0, event->End);
	
	graphics_fill_arc2(g, GPoint(72, 72),
		inner, outer,
		start, end,
		FILL_WHITE, 4);
}

static void _DrawWeatherEvent(Event* event, GContext* g) {
	int16_t inner = _weatherEventInnerRadius;
	int16_t outer = _weatherEventOuterRadius;
	
	int32_t start = _AngleForTime(0, event->Start);
	int32_t end = _AngleForTime(0, event->End);
	
	const gblock_t* fill;
	size_t fillSize;
	_GetWeatherFill(event->Value, &fill, &fillSize);
	
	graphics_fill_arc2(g, GPoint(72, 72),
		inner, outer,
		start, end,
		fill, fillSize);
}

static void _DrawAstronomyEvent(Event* event, GContext* g) {
	int16_t inner = _weatherEventInnerRadius;
	int16_t outer = _weatherEventOuterRadius;
	
	int32_t start = _AngleForTime(0, event->Start);
	int32_t end = _AngleForTime(0, event->End);
	
	const gblock_t* fill;
	size_t fillSize;
	_GetAstronomyFill(event->Value, &fill, &fillSize);
	
	graphics_fill_arc2(g, GPoint(72, 72),
		inner, outer,
		start, end,
		fill, fillSize);
}

static void _DrawSchedule(Layer *layer, GContext* g) {
    // Draw event arcs.
	Event event; void* it = NULL;
	while(data_GetNextEvent(&event, &it)) {
		switch (event.Type) {
		case EVENT_TYPE_PRIORITY_BACKGROUND:
		case EVENT_TYPE_PRIORITY_LOW:
		case EVENT_TYPE_PRIORITY_MEDIUM:
		case EVENT_TYPE_PRIORITY_HIGH:
			_DrawPriorityEvent(&event, g);
			break;
			
		case EVENT_TYPE_WEATHER:
			_DrawWeatherEvent(&event, g);
			break;
			
		case EVENT_TYPE_ASTRONOMY:
			_DrawAstronomyEvent(&event, g);
			break;
		}
	}
	
    // Draw event padding.
    /*graphics_context_set_stroke_color(g, GColorBlack);
    for (int i = 0; i < MAX_SCHEDULES; ++i) {
		Event *event = &_events[i];
		if (event->priority == PRIORITY_NONE) continue;
		
		int32_t angle = (100 + event->end + (TRIG_MAX_ANGLE / 4)) % TRIG_MAX_ANGLE;
		
		int32_t ce = cos_lookup(angle);
		int32_t se = sin_lookup(angle);
		
		int16_t ex = 72 + ce * _priorities[event->priority] / TRIG_MAX_RATIO;
		int16_t ey = 72 + se * _priorities[event->priority] / TRIG_MAX_RATIO;
		
		graphics_draw_line(g,
						   GPoint(72, 72),
						   GPoint(ex, ey));
	}*/
}

static void _DrawNucleus(Layer *layer, GContext* g) {
    // Draw circle.
    graphics_context_set_fill_color(g, GColorWhite);
    graphics_fill_circle(g, GPoint(40, 40), 30);

	// Draw hand.
    gpath_move_to(_pathHand, GPoint(40, 40));
    gpath_rotate_to(_pathHand, _angleHand);

    graphics_context_set_fill_color(g, GColorWhite);
    gpath_draw_filled(g, _pathHand);
}

// ---------------------------- Public functions -------------------------------

void face_Init(Window *window) {
    _layerRoot = window_get_root_layer(window);
    
    window_set_background_color(window, GColorBlack);

	// Schedule.
    _layerSchedule = layer_create(GRect(0, 12, 144, 144));
    layer_set_update_proc(_layerSchedule, _DrawSchedule);
    layer_add_child(_layerRoot, _layerSchedule);
    
	// Nucleus.
    _pathHand = gpath_create(&_shapeHand);

    _layerNucleus = layer_create(GRect(32, 44, 80, 80));
    layer_set_update_proc(_layerNucleus, _DrawNucleus);
    layer_add_child(_layerRoot, _layerNucleus);
	
	// Time.
    _fontHours = fonts_load_custom_font(
		resource_get_handle(RESOURCE_ID_FONT_TIME_32));

    _fontMinutes = fonts_load_custom_font(
		resource_get_handle(RESOURCE_ID_FONT_TIME_18));

    _fontSeconds = _fontDate = fonts_load_custom_font(
		resource_get_handle(RESOURCE_ID_FONT_TIME_12));
    
    _layerDate = text_layer_create(GRect(0, 0, 140, 12));
    text_layer_set_text_color(_layerDate, GColorWhite);
    text_layer_set_background_color(_layerDate, GColorBlack);
    text_layer_set_font(_layerDate, _fontDate);
    text_layer_set_text_alignment(_layerDate, GTextAlignmentCenter);
    text_layer_set_text(_layerDate, _textDate);
    layer_add_child(_layerRoot, text_layer_get_layer(_layerDate));
    
    _layerHours = text_layer_create(GRect(_textX + 0, _textY + 0, 36, 32));
    text_layer_set_text_color(_layerHours, GColorBlack);
    text_layer_set_background_color(_layerHours, GColorClear);
    text_layer_set_font(_layerHours, _fontHours);
    text_layer_set_text(_layerHours, _textHours);
    layer_add_child(_layerNucleus, text_layer_get_layer(_layerHours));
    
    _layerMinutes = text_layer_create(GRect(_textX + 0, _textY + 32, 20, 18));
    text_layer_set_text_color(_layerMinutes, GColorBlack);
    text_layer_set_background_color(_layerMinutes, GColorWhite);
    text_layer_set_font(_layerMinutes, _fontMinutes);
    text_layer_set_text(_layerMinutes, _textMinutes);
    layer_add_child(_layerNucleus, text_layer_get_layer(_layerMinutes));
    
    _layerSeconds = text_layer_create(GRect(_textX + 21, _textY + 36, 14, 12));
    text_layer_set_text_color(_layerSeconds, GColorBlack);
    text_layer_set_background_color(_layerSeconds, GColorWhite);
    text_layer_set_font(_layerSeconds, _fontSeconds);
    text_layer_set_text(_layerSeconds, _textSeconds);
    layer_add_child(_layerNucleus, text_layer_get_layer(_layerSeconds));
    
    // Sync.
    _layerSync = text_layer_create(GRect(0, 166 - 32, 144, 16));
    text_layer_set_text_color(_layerSync, GColorWhite);
    text_layer_set_background_color(_layerSync, GColorBlack);
    text_layer_set_text_alignment(_layerSync, GTextAlignmentCenter);
    text_layer_set_font(_layerSync, _fontSeconds);
    text_layer_set_text(_layerSync, _textSync);
    layer_set_hidden(text_layer_get_layer(_layerSync), _hiddenSync);
    layer_add_child(_layerRoot, text_layer_get_layer(_layerSync));
}

void face_Deinit(Window *window) {
	fonts_unload_custom_font(_fontHours); _fontHours = NULL;
	fonts_unload_custom_font(_fontMinutes); _fontMinutes = NULL;
	fonts_unload_custom_font(_fontSeconds); _fontSeconds = NULL; _fontDate = NULL;
	
    text_layer_destroy(_layerSync); _layerSync = NULL;

    text_layer_destroy(_layerDate); _layerDate = NULL;
    text_layer_destroy(_layerHours); _layerHours = NULL;
    text_layer_destroy(_layerMinutes); _layerMinutes = NULL;
    text_layer_destroy(_layerSeconds); _layerSeconds = NULL;

    layer_destroy(_layerSchedule); _layerSchedule = NULL;
    
	gpath_destroy(_pathHand); _pathHand = NULL;
    layer_destroy(_layerRoot); _layerNucleus = NULL;
}

void face_SetDate(struct tm *date) {
	strftime(_textDate, sizeof(_textDate), "%eth %h", date);
	if (date->tm_mday < 10 || date->tm_mday > 20) {
		if (date->tm_mday % 10 == 1) { _textDate[2] = 's'; _textDate[3] = 't'; }
		else if (date->tm_mday % 10 == 2) { _textDate[2] = 'n'; _textDate[3] = 'd'; }
		else if (date->tm_mday % 10 == 3) { _textDate[2] = 'r'; _textDate[3] = 'd'; }
	}
	
	if (_layerDate) text_layer_set_text(_layerDate, _textDate);
}

void face_SetHours(int16_t hours) {
	if (!clock_is_24h_style() && hours > 12) hours -= 12;

	_textHours[0] = '0' + (hours / 10);
	_textHours[1] = '0' + (hours % 10);

    if (_layerHours) text_layer_set_text(_layerHours, _textHours);
}

void face_SetMinutes(int16_t minutes) {
	_textMinutes[0] = '0' + (minutes / 10);
	_textMinutes[1] = '0' + (minutes % 10);

    if (_layerMinutes) text_layer_set_text(_layerMinutes, _textMinutes);

}

void face_SetSeconds(int16_t seconds) {
	_textSeconds[0] = '0' + (seconds / 10);
	_textSeconds[1] = '0' + (seconds % 10);

    if (_layerSeconds) text_layer_set_text(_layerSeconds, _textSeconds);
}

void face_SetHand(int16_t hours, int16_t minutes) {
    _angleHand = _AngleForTime(hours, minutes) - TRIG_MAX_ANGLE / 2;
    if (_layerNucleus) layer_mark_dirty(_layerNucleus);
}

void face_ShowSyncError() {
	_hiddenSync = false;
	if (_layerSync) layer_set_hidden(text_layer_get_layer(_layerSync), false);
}
