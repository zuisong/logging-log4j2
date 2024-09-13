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
package org.apache.logging.log4j.jul.support;

import static org.apache.logging.log4j.jul.LevelTranslator.toLevel;
import static org.apache.logging.log4j.spi.AbstractLogger.ENTRY_MARKER;
import static org.apache.logging.log4j.spi.AbstractLogger.EXIT_MARKER;
import static org.apache.logging.log4j.spi.AbstractLogger.THROWING_MARKER;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.logging.log4j.BridgeAware;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.jul.LevelTranslator;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.LocalizedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Log4j API implementation of the JUL {@link Logger} class. <strong>Note that this implementation does
 * <em>not</em> use the {@link java.util.logging.Handler} class.</strong> Instead, logging is delegated to the
 * underlying Log4j {@link org.apache.logging.log4j.Logger} which may be implemented in one of many different ways.
 * Consult the documentation for your Log4j Provider for more details.
 * <p>Note that the methods {@link #getParent()} and {@link #setLevel(java.util.logging.Level)} are not supported by
 * this implementation. If you need support for these methods, then you'll need to use log4j-core. The
 * {@link #getParent()} method will not fail (thanks to JUL API limitations), but it won't necessarily be
 * accurate!</p>
 * <p>Also note that {@link #setParent(java.util.logging.Logger)} is explicitly unsupported. Parent loggers are
 * determined using the syntax of the logger name; not through an arbitrary graph of loggers.</p>
 *
 * @since 3.0.0
 */
public class AbstractLogger extends Logger {

    private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

    private final ExtendedLogger logger;
    private static final String FQCN = AbstractLogger.class.getName();

    protected AbstractLogger(final ExtendedLogger logger) {
        super(logger.getName(), null);
        final Level javaLevel = LevelTranslator.toJavaLevel(logger.getLevel());
        super.setLevel(javaLevel);
        this.logger = logger;
    }

    @Override
    public void log(final LogRecord record) {
        if (isFiltered(record)) {
            return;
        }
        final org.apache.logging.log4j.Level level = toLevel(record.getLevel());
        final Object[] parameters = record.getParameters();
        final MessageFactory messageFactory = logger.getMessageFactory();
        final Message message = parameters == null
                ? messageFactory.newMessage(record.getMessage()) /* LOG4J2-1251: not formatted case */
                : messageFactory.newMessage(record.getMessage(), parameters);
        final Throwable thrown = record.getThrown();
        logger.logIfEnabled(FQCN, level, null, message, thrown);
    }

    // support for Logger.getFilter()/Logger.setFilter()
    private boolean isFiltered(final LogRecord logRecord) {
        final Filter filter = getFilter();
        return filter != null && !filter.isLoggable(logRecord);
    }

    private boolean isFiltered(Level level, Throwable thrown, String msg, Object... params) {
        final Filter filter = getFilter();
        if (filter == null) {
            return false;
        }
        LogRecord lr =
                new LogRecord(level, params != null && params.length > 0 ? MessageFormat.format(msg, params) : msg);
        lr.setThrown(thrown);
        return !filter.isLoggable(lr);
    }

    private boolean isFiltered(Level level, Throwable thrown, Supplier<String> msgSupplier) {
        final Filter filter = getFilter();
        if (filter == null) {
            return false;
        }
        LogRecord lr = new LogRecord(level, msgSupplier.get());
        lr.setThrown(thrown);
        return !filter.isLoggable(lr);
    }

    @Override
    public boolean isLoggable(final Level level) {
        return logger.isEnabled(toLevel(level));
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public void setLevel(final Level newLevel) throws SecurityException {
        LOGGER.error("Cannot set JUL log level through Log4j API: ignoring call to Logger.setLevel({})", newLevel);
    }

    /**
     * Unsupported operation.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setParent(final Logger parent) {
        throw new UnsupportedOperationException("Cannot set parent logger");
    }

    private org.apache.logging.log4j.util.Supplier<String> toLog4jSupplier(Supplier<String> msgSupplier) {
        return msgSupplier::get;
    }

    private org.apache.logging.log4j.util.Supplier<Message> toMessageSupplier(Supplier<String> msgSupplier) {
        return () -> logger.getMessageFactory().newMessage(msgSupplier.get());
    }

    private org.apache.logging.log4j.util.Supplier<Message> toMessageSupplier(ResourceBundle bundle, String msg) {
        return () -> new LocalizedMessage(bundle, msg);
    }

    private org.apache.logging.log4j.util.Supplier<Message> toMessageSupplier(
            ResourceBundle bundle, String msg, Object[] params) {
        return () -> new LocalizedMessage(bundle, msg, params);
    }

    private StackTraceElement toLocation(String sourceClass, String sourceMethod) {
        return new StackTraceElement(sourceClass, sourceMethod, null, 0);
    }

    @Override
    public void log(final Level level, final String msg) {
        if (isFiltered(level, null, msg)) {
            return;
        }
        logger.logIfEnabled(FQCN, toLevel(level), null, msg);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void log(Level level, Supplier<String> msgSupplier) {
        if (isFiltered(level, null, msgSupplier)) {
            return;
        }
        logger.logIfEnabled(FQCN, toLevel(level), null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void log(final Level level, final String msg, final Object param1) {
        if (isFiltered(level, null, msg, param1)) {
            return;
        }
        logger.logIfEnabled(FQCN, toLevel(level), null, msg, param1);
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        if (isFiltered(level, null, msg, params)) {
            return;
        }
        logger.logIfEnabled(FQCN, toLevel(level), null, msg, params);
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        if (isFiltered(level, thrown, msg)) {
            return;
        }
        logger.logIfEnabled(FQCN, toLevel(level), null, msg, thrown);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
        if (isFiltered(level, thrown, msgSupplier)) {
            return;
        }
        logger.logIfEnabled(FQCN, toLevel(level), null, toLog4jSupplier(msgSupplier), thrown);
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        if (isFiltered(level, null, msg)) {
            return;
        }
        logger.atLevel(toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .log(msg);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, Supplier<String> msgSupplier) {
        if (isFiltered(level, null, msgSupplier)) {
            return;
        }
        logger.atLevel(toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .log(toMessageSupplier(msgSupplier));
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Object param1) {
        if (isFiltered(level, null, msg, param1)) {
            return;
        }
        logger.atLevel(toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .log(msg, param1);
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Object[] params) {
        if (isFiltered(level, null, msg, params)) {
            return;
        }
        logger.atLevel(toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .log(msg, params);
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Throwable thrown) {
        if (isFiltered(level, thrown, msg)) {
            return;
        }
        logger.atLevel(toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withThrowable(thrown)
                .log(msg);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logp(
            Level level, String sourceClass, String sourceMethod, Throwable thrown, Supplier<String> msgSupplier) {
        if (isFiltered(level, thrown, msgSupplier)) {
            return;
        }
        logger.atLevel(toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withThrowable(thrown)
                .log(toMessageSupplier(msgSupplier));
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logrb(
            Level level, String sourceClass, String sourceMethod, ResourceBundle bundle, String msg, Object... params) {
        if (isFiltered(level, null, msg, params)) {
            return;
        }
        logger.atLevel(toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .log(toMessageSupplier(bundle, msg, params));
    }

    @Override
    public void logrb(
            Level level, String sourceClass, String sourceMethod, ResourceBundle bundle, String msg, Throwable thrown) {
        if (isFiltered(level, thrown, msg)) {
            return;
        }
        logger.atLevel(toLevel(level))
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withThrowable(thrown)
                .log(toMessageSupplier(bundle, msg));
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logrb(Level level, ResourceBundle bundle, String msg, Object... params) {
        if (isFiltered(level, null, msg, params)) {
            return;
        }
        logger.logIfEnabled(FQCN, toLevel(level), null, toMessageSupplier(bundle, msg, params), null);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logrb(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
        if (isFiltered(level, thrown, msg)) {
            return;
        }
        LogBuilder builder = logger.atLevel(toLevel(level)).withThrowable(thrown);
        if (builder instanceof BridgeAware bridgeAware) {
            bridgeAware.setEntryPoint(FQCN);
        }
        builder.log(toMessageSupplier(bundle, msg));
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        if (isFiltered(Level.FINER, null, "ENTRY")) {
            return;
        }
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(ENTRY_MARKER)
                .log(DefaultFlowMessageFactory.INSTANCE.newEntryMessage(null, (Object[]) null));
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        if (isFiltered(Level.FINER, null, "ENTRY {0}", param1)) {
            return;
        }
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(ENTRY_MARKER)
                .log(DefaultFlowMessageFactory.INSTANCE.newEntryMessage(null, param1));
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        if (hasFilter()) {
            // Emulate standard behavior
            if (!isLoggable(Level.FINER)) {
                return;
            }
            final StringBuilder b = new StringBuilder("ENTRY");
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    b.append(' ').append('{').append(i).append('}');
                }
            }
            if (isFiltered(Level.FINER, null, b.toString(), params)) {
                return;
            }
        }
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(ENTRY_MARKER)
                .log(DefaultFlowMessageFactory.INSTANCE.newEntryMessage(null, params));
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        if (isFiltered(Level.FINER, null, "RETURN")) {
            return;
        }
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(EXIT_MARKER)
                .log(DefaultFlowMessageFactory.INSTANCE.newExitMessage(null, (Object) null));
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        if (isFiltered(Level.FINER, null, "RETURN {0}", result)) {
            return;
        }
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(EXIT_MARKER)
                .log(DefaultFlowMessageFactory.INSTANCE.newExitMessage(null, result));
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        if (isFiltered(Level.FINER, thrown, "THROW")) {
            return;
        }
        logger.atTrace()
                .withLocation(toLocation(sourceClass, sourceMethod))
                .withMarker(THROWING_MARKER)
                .withThrowable(thrown)
                .log("Throwing");
    }

    @Override
    public void severe(final String msg) {
        if (isFiltered(Level.SEVERE, null, msg)) {
            return;
        }
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.ERROR, null, msg);
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void severe(Supplier<String> msgSupplier) {
        if (isFiltered(Level.SEVERE, null, msgSupplier)) {
            return;
        }
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.ERROR, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void warning(final String msg) {
        if (isFiltered(Level.WARNING, null, msg)) {
            return;
        }
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.WARN, null, msg);
    }

    @Override
    public void warning(Supplier<String> msgSupplier) {
        if (isFiltered(Level.WARNING, null, msgSupplier)) {
            return;
        }
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.WARN, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void info(final String msg) {
        if (isFiltered(Level.INFO, null, msg)) {
            return;
        }
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.INFO, null, msg);
    }

    @Override
    public void info(Supplier<String> msgSupplier) {
        if (isFiltered(Level.INFO, null, msgSupplier)) {
            return;
        }
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.INFO, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void config(final String msg) {
        if (isFiltered(Level.CONFIG, null, msg)) {
            return;
        }
        logger.logIfEnabled(FQCN, LevelTranslator.CONFIG, null, msg);
    }

    @Override
    public void config(Supplier<String> msgSupplier) {
        if (isFiltered(Level.CONFIG, null, msgSupplier)) {
            return;
        }
        logger.logIfEnabled(FQCN, LevelTranslator.CONFIG, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void fine(final String msg) {
        if (isFiltered(Level.FINE, null, msg)) {
            return;
        }
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.DEBUG, null, msg);
    }

    @Override
    public void fine(Supplier<String> msgSupplier) {
        if (isFiltered(Level.FINE, null, msgSupplier)) {
            return;
        }
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.DEBUG, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void finer(final String msg) {
        if (isFiltered(Level.FINER, null, msg)) {
            return;
        }
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.TRACE, null, msg);
    }

    @Override
    public void finer(Supplier<String> msgSupplier) {
        if (isFiltered(Level.FINER, null, msgSupplier)) {
            return;
        }
        logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.TRACE, null, toLog4jSupplier(msgSupplier), null);
    }

    @Override
    public void finest(final String msg) {
        if (isFiltered(Level.FINEST, null, msg)) {
            return;
        }
        logger.logIfEnabled(FQCN, LevelTranslator.FINEST, null, msg);
    }

    @Override
    public void finest(Supplier<String> msgSupplier) {
        if (isFiltered(Level.FINEST, null, msgSupplier)) {
            return;
        }
        logger.logIfEnabled(FQCN, LevelTranslator.FINEST, null, toLog4jSupplier(msgSupplier), null);
    }

    private boolean hasFilter() {
        return getFilter() != null;
    }
}
