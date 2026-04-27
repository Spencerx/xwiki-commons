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
package org.xwiki.javascript.importmap.internal.parser;

import java.util.Map;

import org.xwiki.webjars.WebjarPathDescriptor;

/**
 * Contains all the information needed to describe a WebJar resource location for importmap, including eager loading
 * information.
 *
 * @param descriptor the WebJar path descriptor
 * @param eager whether the module should be eagerly loaded
 * @param anonymous whether the module should be loaded anonymously
 * @version $Id$
 * @since 18.4.0RC1
 */
public record ImportmapPathDescriptor(WebjarPathDescriptor descriptor, boolean eager, boolean anonymous)
{
    /**
     * Create a new ImportmapPathDescriptor with the given WebJar path descriptor.
     *
     * @param descriptor the WebJar path descriptor
     */
    public ImportmapPathDescriptor(WebjarPathDescriptor descriptor)
    {
        this(descriptor, false, false);
    }

    /**
     * Create a new ImportmapPathDescriptor with the given WebJar path descriptor and eager loading flag.
     *
     * @param descriptor the WebJar path descriptor
     * @param eager whether the module should be eagerly loaded
     */
    public ImportmapPathDescriptor(WebjarPathDescriptor descriptor, boolean eager)
    {
        this(descriptor, eager, false);
    }

    /**
     * Create a new ImportmapPathDescriptor with eager loading.
     *
     * @param webjarId the id of the WebJar that contains the resource
     * @param namespace the namespace in which the webjars resources will be loaded from
     * @param path the path within the WebJar
     * @param params additional query string parameters
     * @param eager whether the module should be eagerly loaded
     */
    public ImportmapPathDescriptor(String webjarId, String namespace, String path, Map<String, ?> params,
        boolean eager)
    {
        this(new WebjarPathDescriptor(webjarId, namespace, path, params), eager, false);
    }

    /**
     * Create a new ImportmapPathDescriptor with eager loading.
     *
     * @param webjarId the id of the WebJar that contains the resource
     * @param path the path within the WebJar
     * @param eager whether the module should be eagerly loaded
     */
    public ImportmapPathDescriptor(String webjarId, String path, boolean eager)
    {
        this(new WebjarPathDescriptor(webjarId, path), eager, false);
    }
}
