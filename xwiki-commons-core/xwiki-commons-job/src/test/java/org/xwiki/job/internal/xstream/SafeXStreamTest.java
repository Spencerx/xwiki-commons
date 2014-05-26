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
package org.xwiki.job.internal.xstream;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.job.test.CustomObject;

public class SafeXStreamTest
{
    static class RecursiveObject
    {
        RecursiveObject recurse;

        public RecursiveObject()
        {
            this.recurse = this;
        }

        @Override
        public String toString()
        {
            return "recursive object";
        }
    }

    private SafeXStream xstream;

    @Before
    public void before() throws ParserConfigurationException
    {
        this.xstream = new SafeXStream();
    }

    private Object writeread(Object obj) throws IOException
    {
        StringWriter writer = new StringWriter();

        this.xstream.toXML(obj, writer);

        return this.xstream.fromXML(writer.toString());
    }

    private Object assertReadwrite(String resource) throws IOException
    {
        String content = IOUtils.toString(getClass().getResourceAsStream(resource));
        
        Object obj = this.xstream.fromXML(content);

        String str = this.xstream.toXML(obj);

        assertEquals(content, str);

        return obj;
    }

    // Tests

    @Test
    public void testRecursiveObject() throws IOException
    {
        RecursiveObject obj = (RecursiveObject) writeread(new RecursiveObject());

        Assert.assertNotNull(obj);
    }

    @Test
    public void testReference() throws IOException
    {
        assertReadwrite("/xstream/arraywithreference.xml");
    }
}
