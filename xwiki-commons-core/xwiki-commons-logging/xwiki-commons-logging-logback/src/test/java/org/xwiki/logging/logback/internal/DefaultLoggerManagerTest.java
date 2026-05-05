/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.logging.logback.internal;

import java.io.File;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.LoggerManager;
import org.xwiki.logging.event.LoggerListener;
import org.xwiki.logging.internal.tail.XStreamFileLoggerTail;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.test.XWikiTempDirUtil;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.xstream.internal.SafeXStream;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLoggerManager}.
 *
 * @version $Id$
 * @since 3.2M3
 */
// @formatter:off
@ComponentTest
@ComponentList({
    DefaultLoggerManager.class,
    DefaultObservationManager.class,
    LogbackEventGenerator.class,
    XStreamFileLoggerTail.class
})
// @formatter:on
class DefaultLoggerManagerTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(org.xwiki.test.LogLevel.WARN);

    private DefaultLoggerManager loggerManager;

    private Logger logger;

    private ListAppender<ILoggingEvent> listAppender;

    private LogbackUtils utils = new LogbackUtils();

    @BeforeEach
    void setUp() throws Exception
    {
        ch.qos.logback.classic.Logger rootLogger = this.utils.getRootLogger();

        // Disable all appenders to avoid unnecessary log
        Filter<ILoggingEvent> filter = new Filter<ILoggingEvent>()
        {
            @Override
            public FilterReply decide(ILoggingEvent event)
            {
                if (event.getMessage() != null && event.getMessage().startsWith("[test]")) {
                    return FilterReply.DENY;
                }

                return FilterReply.NEUTRAL;
            }
        };
        Iterator<Appender<ILoggingEvent>> appendersIterator = rootLogger.iteratorForAppenders();
        while (appendersIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appendersIterator.next();
            appender.addFilter(filter);
        }

        // Add appender
        this.listAppender = new ListAppender<>();
        this.listAppender.start();
        rootLogger.addAppender(this.listAppender);

        this.logger = LoggerFactory.getLogger(getClass());
        // Get the component after adding the listAppender so that initialize() also applies ForbiddenThreadsFilter
        // to the listAppender, which is required by tests that verify logs are not sent to it after pushLogListener()
        this.loggerManager = this.componentManager.getInstance(LoggerManager.class);
    }

    @Test
    void pushPopLogListener() throws InterruptedException
    {
        this.logger.error("[test] before push");

        // Make sure the log has been sent to the logback appender
        assertEquals("[test] before push", this.listAppender.list.get(0).getMessage());

        LogQueue queue = new LogQueue();

        this.loggerManager.pushLogListener(new LoggerListener("loglistenerid", queue));

        this.logger.error("[test] after push");

        // Make sure the log has been added to the queue
        assertEquals("[test] after push", queue.poll().getMessage());

        // Make sure the log has not been sent to the logback appender
        assertEquals(1, this.listAppender.list.size());

        Thread thread = new Thread(() -> this.logger.error("[test] other thread"));
        thread.start();
        thread.join();

        // Make sure the log has been sent to the logback appender
        assertEquals("[test] other thread", this.listAppender.list.get(1).getMessage());

        this.logger.error(org.xwiki.logging.Logger.ROOT_MARKER, "[test] root log");

        // Make sure the log has been added to the queue
        assertEquals("[test] root log", queue.poll().getMessage());
        // Make sure the log also been sent to the logback appender
        assertEquals("[test] root log", this.listAppender.list.get(2).getMessage());

        this.logger.error("[test] stack overflow error", new StackOverflowError());

        // Make sure the log has been added to the queue
        assertEquals("[test] stack overflow error", queue.poll().getMessage());
        // Make sure the log also been sent to the logback appender
        assertEquals("[test] stack overflow error", this.listAppender.list.get(3).getMessage());

        this.logger.error("[test] caused by stack overflow error", new Exception(new StackOverflowError()));

        // Make sure the log has been added to the queue
        assertEquals("[test] caused by stack overflow error", queue.poll().getMessage());
        // Make sure the log also been sent to the logback appender
        assertEquals("[test] caused by stack overflow error", this.listAppender.list.get(4).getMessage());

        this.loggerManager.popLogListener();

        this.logger.error("[test] after pop");

        assertTrue(queue.isEmpty());
        assertEquals("[test] after pop", this.listAppender.list.get(5).getMessage());
    }

    @Test
    void stackedListeners()
    {
        this.logger.error("[test] before push");

        // Make sure the log has been sent to the logback appender
        assertEquals("[test] before push", this.listAppender.list.getFirst().getMessage());

        LogQueue queue1 = new LogQueue();

        this.loggerManager.pushLogListener(new LoggerListener("loglistenerid1", queue1));

        LogQueue queue2 = new LogQueue();

        this.loggerManager.pushLogListener(new LoggerListener("loglistenerid2", queue2));

        this.logger.error("[test] log queue2");

        // Make sure the log has not been sent to the stacked listener
        assertTrue(queue1.isEmpty());

        // Make sure the log has been sent to the current listener
        assertEquals("[test] log queue2", queue2.poll().getMessage());

        this.loggerManager.popLogListener();

        this.logger.error("[test] log queue1");

        // Make sure the log has been sent to the current listener
        assertEquals("[test] log queue1", queue1.poll().getMessage());

        this.loggerManager.popLogListener();
    }

    @Test
    void nullListeners()
    {
        this.logger.error("[test] before push");

        // Make sure the log has been sent to the logback appender
        assertEquals("[test] before push", this.listAppender.list.get(0).getMessage());

        this.loggerManager.pushLogListener(null);

        this.logger.error("[test] log to null");

        // Make sure the log has not been sent to the logback appender
        assertEquals(1, this.listAppender.list.size());

        this.loggerManager.popLogListener();

        this.logger.error("[test] after pop");

        // Make sure the log has been sent to the logback appender
        assertEquals("[test] after pop", this.listAppender.list.get(1).getMessage());
    }

    @Test
    void getSetLoggerLevel()
    {
        assertNull(this.loggerManager.getLoggerLevel(getClass().getName()));

        LogQueue queue = new LogQueue();

        this.loggerManager.pushLogListener(new LoggerListener("loglistenerid", queue));

        this.loggerManager.setLoggerLevel(getClass().getName(), LogLevel.WARN);
        assertSame(LogLevel.WARN, this.loggerManager.getLoggerLevel(getClass().getName()));

        this.logger.debug("[test] debug message 1");
        // Provide information when the Assert fails
        if (!queue.isEmpty()) {
            fail("Should have contained no message but got [" + queue.peek().getFormattedMessage()
                + "] instead (last message, there might be more)");
        }

        this.loggerManager.setLoggerLevel(getClass().getName(), LogLevel.DEBUG);
        assertSame(LogLevel.DEBUG, this.loggerManager.getLoggerLevel(getClass().getName()));

        this.logger.debug("[test] debug message 2");
        assertEquals(1, queue.size());

        this.loggerManager.setLoggerLevel(getClass().getName(), null);
        assertNull(this.loggerManager.getLoggerLevel(getClass().getName()));
    }

    @Test
    void getLoggers()
    {
        assertNotNull(this.loggerManager.getLoggers());
    }

    @Test
    void initializeWhenNoLogback() throws Exception
    {
        // Simulate that the Logging implementation is not Logback
        DefaultLoggerManager spyLoggerManager = spy(this.loggerManager);
        when(spyLoggerManager.getRootLogger()).thenReturn(null);

        spyLoggerManager.initialize();

        assertEquals(
            "Could not find any Logback root logger. All logging module advanced features will be disabled.",
            this.logCapture.getMessage(0));
    }

    @Test
    void getLoggerLevelWhenNoLogback()
    {
        // Simulate that the Logging implementation is not Logback
        DefaultLoggerManager spyLoggerManager = spy(this.loggerManager);
        when(spyLoggerManager.getLoggerContext()).thenReturn(null);

        assertNull(spyLoggerManager.getLoggerLevel("whatever"));
    }

    @Test
    void createLoggerTail() throws Exception
    {
        this.componentManager.registerMockComponent(SafeXStream.class);

        File logFile = new File(XWikiTempDirUtil.createTemporaryDirectory(), "log");

        assertFalse(this.loggerManager.createLoggerTail(logFile.toPath(), true) instanceof XStreamFileLoggerTail);

        assertInstanceOf(XStreamFileLoggerTail.class, this.loggerManager.createLoggerTail(logFile.toPath(), false));

        assertInstanceOf(XStreamFileLoggerTail.class, this.loggerManager.createLoggerTail(logFile.toPath(), true));
    }
}
