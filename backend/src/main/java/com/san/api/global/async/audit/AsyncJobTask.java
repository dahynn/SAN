package com.san.api.global.async.audit;

@FunctionalInterface
public interface AsyncJobTask {

    void run() throws Exception;
}
