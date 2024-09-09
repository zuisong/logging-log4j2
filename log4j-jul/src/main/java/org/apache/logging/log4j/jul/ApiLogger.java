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

import static org.apache.logging.log4j.jul.LevelTranslator.toLevel;

import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
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
 * @since 2.1
 */
public class ApiLogger extends Logger {

    private final WrappedLogger logger;
    private static final String FQCN = ApiLogger.class.getName();

    ApiLogger(final ExtendedLogger logger) {
        super(logger.getName(), null);
        final Level javaLevel = LevelTranslator.toJavaLevel(logger.getLevel());
        super.setLevel(javaLevel);
        this.logger = new WrappedLogger(logger);
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
    boolean isFiltered(final LogRecord logRecord) {
        final Filter filter = getFilter();
        return filter != null && !filter.isLoggable(logRecord);
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
        StatusLogger.getLogger()
                .error(
                        "Cannot set JUL log level through log4j-api: " + "ignoring call to Logger.setLevel({})",
                        newLevel);
    }

    /**
     * Provides access to {@link Logger#setLevel(java.util.logging.Level)}. This method should only be used by child
     * classes.
     *
     * @see Logger#setLevel(java.util.logging.Level)
     */
    protected void doSetLevel(final Level newLevel) throws SecurityException {
        super.setLevel(newLevel);
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

    private org.apache.logging.log4j.util.Supplier<LocalizedMessage> toLocalizedMessageSupplier(
            ResourceBundle bundle, String msg) {
        return () -> new LocalizedMessage(bundle, msg);
    }

    private org.apache.logging.log4j.util.Supplier<LocalizedMessage> toLocalizedMessageSupplier(
            ResourceBundle bundle, String msg, Object[] params) {
        return () -> new LocalizedMessage(bundle, msg, params);
    }

    private StackTraceElement toLocation(String sourceClass, String sourceMethod) {
        return new StackTraceElement(sourceClass, sourceMethod, null, 0);
    }

    @Override
    public void log(final Level level, final String msg) {
        if (hasFilter()) {
            super.log(level, msg);
        } else {
            logger.log(toLevel(level), msg);
        }
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void log(Level level, Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.log(level, msgSupplier);
        } else {
            logger.log(toLevel(level), toLog4jSupplier(msgSupplier));
        }
    }

    @Override
    public void log(final Level level, final String msg, final Object param1) {
        if (hasFilter()) {
            super.log(level, msg, param1);
        } else {
            logger.log(toLevel(level), msg, param1);
        }
    }

    @Override
    public void log(final Level level, final String msg, final Object[] params) {
        if (hasFilter()) {
            super.log(level, msg, params);
        } else {
            logger.log(toLevel(level), msg, params);
        }
    }

    @Override
    public void log(final Level level, final String msg, final Throwable thrown) {
        if (hasFilter()) {
            super.log(level, msg, thrown);
        } else {
            logger.log(toLevel(level), msg, thrown);
        }
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.log(level, thrown, msgSupplier);
        } else {
            logger.log(toLevel(level), toLog4jSupplier(msgSupplier), thrown);
        }
    }

    @Override
    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        if (hasFilter()) {
            super.logp(level, sourceClass, sourceMethod, msg);
        } else {
            logger.atLevel(toLevel(level))
                    .withLocation(toLocation(sourceClass, sourceMethod))
                    .log(msg);
        }
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.logp(level, sourceClass, sourceMethod, msgSupplier);
        } else {
            logger.atLevel(toLevel(level))
                    .withLocation(toLocation(sourceClass, sourceMethod))
                    .log(toMessageSupplier(msgSupplier));
        }
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Object param1) {
        if (hasFilter()) {
            super.logp(level, sourceClass, sourceMethod, msg, param1);
        } else {
            logger.atLevel(toLevel(level))
                    .withLocation(toLocation(sourceClass, sourceMethod))
                    .log(msg, param1);
        }
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Object[] params) {
        if (hasFilter()) {
            super.logp(level, sourceClass, sourceMethod, msg, params);
        } else {
            logger.atLevel(toLevel(level))
                    .withLocation(toLocation(sourceClass, sourceMethod))
                    .log(msg, params);
        }
    }

    @Override
    public void logp(
            final Level level,
            final String sourceClass,
            final String sourceMethod,
            final String msg,
            final Throwable thrown) {
        if (hasFilter()) {
            super.logp(level, sourceClass, sourceMethod, msg, thrown);
        } else {
            logger.atLevel(toLevel(level))
                    .withLocation(toLocation(sourceClass, sourceMethod))
                    .withThrowable(thrown)
                    .log(msg);
        }
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logp(
            Level level, String sourceClass, String sourceMethod, Throwable thrown, Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.logp(level, sourceClass, sourceMethod, thrown, msgSupplier);
        } else {
            logger.atLevel(toLevel(level))
                    .withLocation(toLocation(sourceClass, sourceMethod))
                    .withThrowable(thrown)
                    .log(toMessageSupplier(msgSupplier));
        }
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logrb(
            Level level, String sourceClass, String sourceMethod, ResourceBundle bundle, String msg, Object... params) {
        if (hasFilter()) {
            super.logrb(level, sourceClass, sourceMethod, bundle, msg, params);
        } else {
            logger.atLevel(toLevel(level))
                    .withLocation(toLocation(sourceClass, sourceMethod))
                    .log(toLocalizedMessageSupplier(bundle, msg, params));
        }
    }

    @Override
    public void logrb(
            Level level, String sourceClass, String sourceMethod, ResourceBundle bundle, String msg, Throwable thrown) {
        if (hasFilter()) {
            super.logrb(level, sourceClass, sourceMethod, bundle, msg, thrown);
        } else {
            logger.atLevel(toLevel(level))
                    .withLocation(toLocation(sourceClass, sourceMethod))
                    .withThrowable(thrown)
                    .log(toLocalizedMessageSupplier(bundle, msg));
        }
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logrb(Level level, ResourceBundle bundle, String msg, Object... params) {
        if (hasFilter()) {
            super.logrb(level, bundle, msg, params);
        } else {
            logger.log(toLevel(level), toLocalizedMessageSupplier(bundle, msg, params));
        }
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void logrb(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
        if (hasFilter()) {
            super.logrb(level, bundle, msg, thrown);
        } else {
            logger.atLevel(toLevel(level)).withThrowable(thrown).log(toLocalizedMessageSupplier(bundle, msg));
        }
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod) {
        logger.traceEntry();
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        logger.traceEntry(null, param1);
    }

    @Override
    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        logger.traceEntry(null, params);
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod) {
        logger.traceExit();
    }

    @Override
    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        logger.traceExit(result);
    }

    @Override
    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        logger.throwing(thrown);
    }

    @Override
    public void severe(final String msg) {
        if (hasFilter()) {
            super.severe(msg);
        } else {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.ERROR, null, msg);
        }
    }

    /**
     * @since 3.0.0
     */
    @Override
    public void severe(Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.severe(msgSupplier);
        } else {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.ERROR, null, toMessageSupplier(msgSupplier), null);
        }
    }

    @Override
    public void warning(final String msg) {
        if (hasFilter()) {
            super.warning(msg);
        } else {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.WARN, null, msg);
        }
    }

    @Override
    public void warning(Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.warning(msgSupplier);
        } else {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.WARN, null, toMessageSupplier(msgSupplier), null);
        }
    }

    @Override
    public void info(final String msg) {
        if (hasFilter()) {
            super.info(msg);
        } else {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.INFO, null, msg);
        }
    }

    @Override
    public void info(Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.info(msgSupplier);
        } else {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.INFO, null, toMessageSupplier(msgSupplier), null);
        }
    }

    @Override
    public void config(final String msg) {
        if (hasFilter()) {
            super.config(msg);
        } else {
            logger.logIfEnabled(FQCN, LevelTranslator.CONFIG, null, msg);
        }
    }

    @Override
    public void config(Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.config(msgSupplier);
        } else {
            logger.logIfEnabled(FQCN, LevelTranslator.CONFIG, null, toMessageSupplier(msgSupplier), null);
        }
    }

    @Override
    public void fine(final String msg) {
        if (hasFilter()) {
            super.fine(msg);
        } else {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.DEBUG, null, msg);
        }
    }

    @Override
    public void fine(Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.fine(msgSupplier);
        } else {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.DEBUG, null, toMessageSupplier(msgSupplier), null);
        }
    }

    @Override
    public void finer(final String msg) {
        if (hasFilter()) {
            super.finer(msg);
        } else {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.TRACE, null, msg);
        }
    }

    @Override
    public void finer(Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.finer(msgSupplier);
        } else {
            logger.logIfEnabled(FQCN, org.apache.logging.log4j.Level.TRACE, null, toMessageSupplier(msgSupplier), null);
        }
    }

    @Override
    public void finest(final String msg) {
        if (hasFilter()) {
            super.finest(msg);
        } else {
            logger.logIfEnabled(FQCN, LevelTranslator.FINEST, null, msg);
        }
    }

    @Override
    public void finest(Supplier<String> msgSupplier) {
        if (hasFilter()) {
            super.finest(msgSupplier);
        } else {
            logger.logIfEnabled(FQCN, LevelTranslator.FINEST, null, toMessageSupplier(msgSupplier), null);
        }
    }

    private boolean hasFilter() {
        return getFilter() != null;
    }
}
