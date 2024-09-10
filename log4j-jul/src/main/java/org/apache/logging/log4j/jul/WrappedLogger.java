/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.jul;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

/**
 * Wrapper class to ensure proper FQCN support in Logger calls.
 *
 * @since 2.1
 */
class WrappedLogger extends ExtendedLoggerWrapper {

    private static final long serialVersionUID = 1L;
    private static final String FQCN = ApiLogger.class.getName();

    WrappedLogger(final ExtendedLogger logger) {
        super(logger, logger.getName(), logger.getMessageFactory());
    }

    @Override
    public void log(final Level level, final String message, final Throwable t) {
        logIfEnabled(FQCN, level, null, message, t);
    }

    @Override
    public void log(final Level level, final String message, final Object... params) {
        logIfEnabled(FQCN, level, null, message, params);
    }

    @Override
    public void log(final Level level, final String message) {
        logIfEnabled(FQCN, level, null, message);
    }

    @Override
    public EntryMessage traceEntry() {
        return enter(FQCN, null, (Object[]) null);
    }

    @Override
    public EntryMessage traceEntry(final String message, final Object... params) {
        return enter(FQCN, message, params);
    }

    @Override
    public <T extends Throwable> T throwing(final T t) {
        return throwing(FQCN, LevelTranslator.toLevel(java.util.logging.Level.FINER), t);
    }

    @Override
    protected void log(
            Level level, Marker marker, String fqcn, StackTraceElement location, Message message, Throwable throwable) {
        logger.logMessage(level, marker, fqcn, location, message, throwable);
    }
}
