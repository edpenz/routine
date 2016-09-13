#ifndef _ROUTINE_FACE_H
#define _ROUTINE_FACE_H

#include <pebble.h>

void face_Init(Window *window);
void face_Deinit(Window *window);

void face_SetDate(struct tm *date);
void face_SetHours(int16_t hours);
void face_SetMinutes(int16_t minutes);
void face_SetSeconds(int16_t seconds);

void face_SetHand(int16_t hours, int16_t minutes);

void face_ShowSyncError();

#endif

