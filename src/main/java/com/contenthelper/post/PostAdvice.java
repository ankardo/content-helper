package com.contenthelper.post;

/** The recommended type, shape, reason, and next command for a set of notes. */
public record PostAdvice(PostType type, PostShape shape, String reason, String nextStep) {

    public PostAdvice {
        reason = reason == null ? "" : reason.trim();
        nextStep = nextStep == null ? "" : nextStep.trim();
    }
}
