package com.visualticks.config;

/**
 * Shape drawn for each tick. RuneLite stores a chosen value by its constant name and reads it back with {@code valueOf},
 * so renaming a constant resets that setting for every user who picked it.
 */
public enum TickShape {
    SQUARE,
    CIRCLE,
    ROUNDED_SQUARE
}
