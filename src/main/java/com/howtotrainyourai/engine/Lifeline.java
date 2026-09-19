package com.howtotrainyourai.engine;

/**
 * The three lifelines a player can use during a session (CONTEXT.md §2.4).
 * Which ones a Protocol allows is decided in Protocol, not here -- this enum
 * just names them.
 *
 * TODO: add the three constants -- Binary Choice, Predict, Override. Name
 * them however reads best as Java identifiers (enum constants are
 * conventionally ALL_CAPS).
 */
public enum Lifeline {
    BINARY_CHOICE, PREDICT, OVERRIDE
}
