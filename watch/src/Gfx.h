#ifndef _ROUTINE_GFX_H
#define _ROUTINE_GFX_H

#include <pebble.h>

typedef uint32_t gblock_t;

static const gblock_t FILL_WHITE[] = { 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff };
static const gblock_t FILL_GS_87[] = { 0xdddddddd, 0xffffffff, 0x77777777, 0xffffffff };
static const gblock_t FILL_GS_75[] = { 0xaaaaaaaa, 0xffffffff, 0xaaaaaaaa, 0xffffffff };
static const gblock_t FILL_GS_50[] = { 0xaaaaaaaa, 0x55555555, 0xaaaaaaaa, 0x55555555 };
static const gblock_t FILL_GS_25[] = { 0xaaaaaaaa, 0x00000000, 0xaaaaaaaa, 0x00000000 };
static const gblock_t FILL_GS_12[] = { 0x88888888, 0x00000000, 0x22222222, 0x00000000 };
static const gblock_t FILL_BLACK[] = { 0x00000000, 0x00000000, 0x00000000, 0x00000000 };

void graphics_fill_arc(GContext *ctx, GPoint p,
		int16_t radiusInner, int16_t radiusOuter,
		int32_t angleStart, int32_t angleEnd);

void graphics_fill_arc2(GContext *ctx, GPoint p,
		int16_t radiusInner, int16_t radiusOuter,
		int32_t angleStart, int32_t angleEnd,
		const gblock_t* fill, size_t fillCount);

#endif

