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
package org.apache.logging.log4j.jul.test;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.jul.LogManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class CallerInformationTest {

    private static final String PARAM_1 = "PARAM_1";
    private static final String[] PARAMS = {PARAM_1, "PARAM_2"};
    private static final String SOURCE_CLASS = "SourceClass";
    private static final String SOURCE_METHOD = "sourceMethod";

    @Rule
    public final LoggerContextRule ctx = new LoggerContextRule("CallerInformationTest.xml");

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
    }

    @Test
    public void testClassLogger() throws Exception {
        final ListAppender app = ctx.getListAppender("Class").clear();
        final Logger logger = Logger.getLogger("ClassLogger");
        logger.info("Ignored message contents.");
        logger.warning("Verifying the caller class is still correct.");
        logger.severe("Hopefully nobody breaks me!");
        List<String> messages = app.getMessages();
        assertEquals("Incorrect number of messages.", 3, messages.size());
        for (final String message : messages) {
            assertEquals("Incorrect caller class name.", this.getClass().getName(), message);
        }

        // Test passing the location information directly
        app.clear();
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello!");
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello {1}!", PARAM_1);
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello {1} and {2}!", PARAMS);
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello!", new RuntimeException());
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, () -> "Hello" + PARAM_1 + "!");
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, new RuntimeException(), () -> "Hello " + PARAM_1 + "!");
        messages = app.getMessages();
        assertEquals("Incorrect number of messages.", 6, messages.size());
        for (final String message : messages) {
            assertEquals("Incorrect caller class name.", SOURCE_CLASS, message);
        }
    }

    @Test
    public void testMethodLogger() throws Exception {
        final ListAppender app = ctx.getListAppender("Method").clear();
        final Logger logger = Logger.getLogger("MethodLogger");
        logger.info("More messages.");
        logger.warning("CATASTROPHE INCOMING!");
        logger.severe("ZOMBIES!!!");
        logger.warning("brains~~~");
        logger.info("Itchy. Tasty.");
        List<String> messages = app.getMessages();
        assertEquals("Incorrect number of messages.", 5, messages.size());
        for (final String message : messages) {
            assertEquals("Incorrect caller method name.", "testMethodLogger", message);
        }

        // Test passing the location information directly
        app.clear();
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello!");
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello {1}!", PARAM_1);
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello {1} and {2}!", PARAMS);
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello!", new RuntimeException());
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, () -> "Hello " + PARAM_1 + "!");
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, new RuntimeException(), () -> "Hello " + PARAM_1 + "!");
        messages = app.getMessages();
        assertEquals("Incorrect number of messages.", 6, messages.size());
        for (final String message : messages) {
            assertEquals("Incorrect caller method name.", SOURCE_METHOD, message);
        }
    }
}
