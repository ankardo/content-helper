package com.contenthelper.observe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** One run, recorded in the metrics and in the structured log line. */
public final class RunRecorder {

    private static final Logger log = LoggerFactory.getLogger(RunRecorder.class);

    private RunRecorder() {}

    public static void record(
            RunMetrics metrics, String command, boolean ai, int outputs, long start, boolean fallback) {
        String mode = ai ? "ai" : "canned";
        long millis = (System.nanoTime() - start) / 1_000_000;
        metrics.run(command, mode, millis, fallback);
        log.info(
                "run command={} mode={} duration_ms={} outputs={} fallback={}",
                command,
                mode,
                millis,
                outputs,
                fallback);
    }
}
