#include "Gfx.h"

#define DOWN	(TRIG_MAX_ANGLE * 0 / 4)
#define LEFT	(TRIG_MAX_ANGLE * 1 / 4)
#define UP		(TRIG_MAX_ANGLE * 2 / 4)
#define RIGHT	(TRIG_MAX_ANGLE * 3 / 4)

static inline int32_t min(int32_t a, int32_t b) {
	return a < b ? a : b;
}

static inline int32_t max(int32_t a, int32_t b) {
	return a > b ? a : b;
}

void graphics_fill_arc(GContext *ctx, GPoint p,
		int16_t radiusInner, int16_t radiusOuter,
		int32_t angleStart, int32_t angleEnd) {
	// Normalise angles.
	angleStart %= TRIG_MAX_ANGLE;
	angleEnd %= TRIG_MAX_ANGLE;
	while (angleEnd < angleStart) angleEnd += TRIG_MAX_ANGLE;
	
	// Common constants.
	int32_t rIrI = radiusInner * radiusInner;
	int32_t rOrO = radiusOuter * radiusOuter;
	
	// Make line equations.
	int32_t cs = cos_lookup(angleStart);
	int32_t ss = sin_lookup(angleStart);
	
	int32_t ce = cos_lookup(angleEnd);
	int32_t se = sin_lookup(angleEnd);
	
	// Bottom left quadrant.
	if (angleStart < LEFT) {
		bool clipStart = angleStart > 0 && angleStart < LEFT;
		bool clipEnd = angleEnd > 0 && angleEnd < LEFT;
		
		int32_t x0 = 0;
		int32_t x1 = 0;
		GPoint s, e;
		for (int16_t y = radiusOuter; y >= 0; --y) {
			int32_t yy = y * y;
			while(yy + x0 * x0 < rOrO) {
				--x0;
			}
			while(yy + x1 * x1 < rIrI) {
				--x1;
			}
			s.x = p.x + (clipEnd ? max(x0, -y * se / ce) : x0);
			e.x = p.x + (clipStart ? min(x1, -y * ss / cs) : x1);
			if (e.x > s.x) {
				s.y = e.y = p.y + y;
				graphics_draw_line(ctx, s, e);
			}
		}
	}
	
	// Top left quadrant.
	if (angleStart < UP && angleEnd > LEFT) {
		bool clipStart = angleStart > LEFT && angleStart < UP;
		bool clipEnd = angleEnd > LEFT && angleEnd < UP;
		
		int32_t x0 = 0;
		int32_t x1 = 0;
		GPoint s, e;
		for (int16_t y = -radiusOuter; y <= 0; ++y) {
			int32_t yy = y * y;
			while(yy + x0 * x0 < rOrO) {
				--x0;
			}
			while(yy + x1 * x1 < rIrI) {
				--x1;
			}
			s.x = p.x + (clipStart ? max(x0, -y * ss / cs) : x0);
			e.x = p.x + (clipEnd ? min(x1, -y * se / ce) : x1);
			if (e.x > s.x) {
				s.y = e.y = p.y + y;
				graphics_draw_line(ctx, s, e);
			}
		}
	}
	
	// Top right quadrant.
	if (angleStart < RIGHT && angleEnd > UP) {
		bool clipStart = angleStart > UP && angleStart < RIGHT;
		bool clipEnd = angleEnd > UP && angleEnd < RIGHT;
		
		int32_t x0 = 0;
		int32_t x1 = 0;
		GPoint s, e;
		for (int16_t y = -radiusOuter; y <= 0; ++y) {
			int32_t yy = y * y;
			while(yy + x1 * x1 < rOrO) {
				++x1;
			}
			while(yy + x0 * x0 < rIrI) {
				++x0;
			}
			s.x = p.x + (clipStart ? max(x0, -y * ss / cs) : x0);
			e.x = p.x + (clipEnd ? min(x1, -y * se / ce) : x1);
			if (e.x > s.x) {
				s.y = e.y = p.y + y;
				graphics_draw_line(ctx, s, e);
			}
		}
	}
	
	// Bottom right quadrant.
	if (angleEnd > RIGHT) {
		bool clipStart = angleStart > RIGHT && angleStart < TRIG_MAX_ANGLE;
		bool clipEnd = angleEnd > RIGHT && angleEnd < TRIG_MAX_ANGLE;
		
		int32_t x0 = 0;
		int32_t x1 = 0;
		GPoint s, e;
		for (int16_t y = radiusOuter; y >= 0; --y) {
			int32_t yy = y * y;
			while(yy + x0 * x0 < rIrI) {
				++x0;
			}
			while(yy + x1 * x1 < rOrO) {
				++x1;
			}
			s.x = p.x + (clipEnd ? max(x0, -y * se / ce) : x0);
			e.x = p.x + (clipStart ? min(x1, -y * ss / cs) : x1);
			if (e.x > s.x) {
				s.y = e.y = p.y + y;
				graphics_draw_line(ctx, s, e);
			}
		}
	}
}

typedef uint32_t gblock_t;

static void bitmap_blit_pattern(GBitmap* bitmap, GPoint p0, GPoint p1, gblock_t pattern) {
	uint8_t *rowAddr = (uint8_t *)bitmap->addr + bitmap->row_size_bytes * p0.y;
	
	gblock_t *blockAddress = (gblock_t *)rowAddr;
	int16_t bitsToSkip = p0.x;
	int16_t remainingBits = p1.x - p0.x;
	
	const uint8_t BITS_PER_BLOCK = sizeof(*blockAddress) * 8;
	
	blockAddress += bitsToSkip / BITS_PER_BLOCK;
	bitsToSkip %= BITS_PER_BLOCK;
	
	while (remainingBits > 0) {
		gblock_t blockMask = ~0;
		if (bitsToSkip > 0) {
			blockMask &= ~0 << bitsToSkip;
			remainingBits += bitsToSkip;
			bitsToSkip = 0;
		}
		if (remainingBits < BITS_PER_BLOCK) {
			blockMask &= ~(~0 << remainingBits);
		}
		
		gblock_t block = *blockAddress;
		block &= ~blockMask;
		block |= blockMask & pattern;
		*blockAddress = block;
		
		remainingBits -= BITS_PER_BLOCK;
		++blockAddress;
	}
}

void graphics_fill_arc2(GContext *ctx, GPoint p,
		int16_t radiusInner, int16_t radiusOuter,
		int32_t angleStart, int32_t angleEnd,
		const gblock_t* fill, size_t fillCount) {
			
	// Normalise angles.
	angleStart %= TRIG_MAX_ANGLE;
	angleEnd %= TRIG_MAX_ANGLE;
	while (angleEnd <= angleStart) angleEnd += TRIG_MAX_ANGLE;
			
	// Setup direct-mode drawing.
	GBitmap *frameBuffer = graphics_capture_frame_buffer(ctx);
	p.y += 12; // Because layout bounds get lost in direct-mode.
	
	// Common constants.
	int32_t rIrI = radiusInner * radiusInner;
	int32_t rOrO = radiusOuter * radiusOuter;
	
	// Make line equations.
	int32_t cs = cos_lookup(angleStart);
	int32_t ss = sin_lookup(angleStart);
	
	int32_t ce = cos_lookup(angleEnd);
	int32_t se = sin_lookup(angleEnd);
	
	// Bottom left quadrant.
	if (angleStart < LEFT) {
		bool clipStart = angleStart > 0 && angleStart < LEFT;
		bool clipEnd = angleEnd > 0 && angleEnd < LEFT;
		
		int32_t x0 = 0;
		int32_t x1 = 0;
		GPoint s, e;
		for (int16_t y = radiusOuter; y >= 0; --y) {
			int32_t yy = y * y;
			while(yy + x0 * x0 < rOrO) {
				--x0;
			}
			while(yy + x1 * x1 < rIrI) {
				--x1;
			}
			s.x = p.x + (clipEnd ? max(x0, -y * se / ce) : x0);
			e.x = p.x + (clipStart ? min(x1, -y * ss / cs) : x1);
			if (e.x > s.x) {
				s.y = e.y = p.y + y;
				bitmap_blit_pattern(frameBuffer, s, e, fill[s.y % fillCount]);
			}
		}
	}
	
	// Top left quadrant.
	if (angleStart < UP && angleEnd > LEFT) {
		bool clipStart = angleStart > LEFT && angleStart < UP;
		bool clipEnd = angleEnd > LEFT && angleEnd < UP;
		
		int32_t x0 = 0;
		int32_t x1 = 0;
		GPoint s, e;
		for (int16_t y = -radiusOuter; y <= 0; ++y) {
			int32_t yy = y * y;
			while(yy + x0 * x0 < rOrO) {
				--x0;
			}
			while(yy + x1 * x1 < rIrI) {
				--x1;
			}
			s.x = p.x + (clipStart ? max(x0, -y * ss / cs) : x0);
			e.x = p.x + (clipEnd ? min(x1, -y * se / ce) : x1);
			if (e.x > s.x) {
				s.y = e.y = p.y + y;
				bitmap_blit_pattern(frameBuffer, s, e, fill[s.y % fillCount]);
			}
		}
	}
	
	// Top right quadrant.
	if (angleStart < RIGHT && angleEnd > UP) {
		bool clipStart = angleStart > UP && angleStart < RIGHT;
		bool clipEnd = angleEnd > UP && angleEnd < RIGHT;
		
		int32_t x0 = 0;
		int32_t x1 = 0;
		GPoint s, e;
		for (int16_t y = -radiusOuter; y <= 0; ++y) {
			int32_t yy = y * y;
			while(yy + x1 * x1 < rOrO) {
				++x1;
			}
			while(yy + x0 * x0 < rIrI) {
				++x0;
			}
			s.x = p.x + (clipStart ? max(x0, -y * ss / cs) : x0);
			e.x = p.x + (clipEnd ? min(x1, -y * se / ce) : x1);
			if (e.x > s.x) {
				s.y = e.y = p.y + y;
				bitmap_blit_pattern(frameBuffer, s, e, fill[s.y % fillCount]);
			}
		}
	}
	
	// Bottom right quadrant.
	if (angleEnd > RIGHT) {
		bool clipStart = angleStart > RIGHT && angleStart < TRIG_MAX_ANGLE;
		bool clipEnd = angleEnd > RIGHT && angleEnd < TRIG_MAX_ANGLE;
		
		int32_t x0 = 0;
		int32_t x1 = 0;
		GPoint s, e;
		for (int16_t y = radiusOuter; y >= 0; --y) {
			int32_t yy = y * y;
			while(yy + x0 * x0 < rIrI) {
				++x0;
			}
			while(yy + x1 * x1 < rOrO) {
				++x1;
			}
			s.x = p.x + (clipEnd ? max(x0, -y * se / ce) : x0);
			e.x = p.x + (clipStart ? min(x1, -y * ss / cs) : x1);
			if (e.x > s.x) {
				s.y = e.y = p.y + y;
				bitmap_blit_pattern(frameBuffer, s, e, fill[s.y % fillCount]);
			}
		}
	}
	
	graphics_release_frame_buffer(ctx, frameBuffer);
}
