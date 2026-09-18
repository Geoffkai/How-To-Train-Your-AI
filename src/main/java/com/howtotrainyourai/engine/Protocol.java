package com.howtotrainyourai.engine;

/**
 * Which restoration protocol the player picked for this session.
 *
 * Empty-bodied on purpose: GameEngine's interface (this week, Week 1) needs
 * this type to exist so its method signatures compile, but the actual
 * behavioral differences — lifeline count, checkpoint questions, score
 * multiplier, timer (see CONTEXT.md §2.2) — are Week 2 work. Nothing here
 * should be assumed stable until then.
 */
public enum Protocol {
    STANDARD,
    HIGH_RISK
}
