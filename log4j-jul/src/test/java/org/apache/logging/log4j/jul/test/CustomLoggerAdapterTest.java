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

import static org.apache.logging.log4j.jul.test.JulTestProperties.JUL_LOGGER_ADAPTER;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;
import org.apache.logging.log4j.jul.AbstractLoggerAdapter;
import org.apache.logging.log4j.jul.ApiLogger;
import org.apache.logging.log4j.jul.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests if the logger adapter can be customized.
 */
public class CustomLoggerAdapterTest {

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
        System.setProperty(JUL_LOGGER_ADAPTER, CustomLoggerAdapter.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
        System.clearProperty(JUL_LOGGER_ADAPTER);
    }

    @Test
    public void testCustomLoggerAdapter() {
        Logger logger = Logger.getLogger(CustomLoggerAdapterTest.class.getName());
        assertTrue("CustomLoggerAdapter is used", logger instanceof CustomLogger);
    }

    public static class CustomLoggerAdapter extends AbstractLoggerAdapter {

        @Override
        protected Logger newLogger(String name, LoggerContext context) {
            return new CustomLogger(context.getLogger(name));
        }
    }

    private static class CustomLogger extends ApiLogger {

        CustomLogger(ExtendedLogger logger) {
            super(logger);
        }
    }
}
