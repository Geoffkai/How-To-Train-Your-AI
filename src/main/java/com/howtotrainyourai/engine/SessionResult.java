package com.howtotrainyourai.engine;

/**
 * Summary of one finished session — trainer name, final token score,
 * highest capability restored, protocol used, and result (win/fail/
 * returned). Written to the training records at endSession() (see
 * CONTEXT.md §2.5).
 *
 * Empty-bodied on purpose, same reason as Protocol: GameEngine.endSession()
 * needs this type to exist to compile this week, but its real fields are
 * Week 4 work (see R1_Engine_Weekly_Plan.md) once records/highest-capability
 * logic is built. Nothing here should be assumed stable until then.
 */
public class SessionResult {
}
