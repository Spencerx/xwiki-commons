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
package org.xwiki.netflux.internal;

import jakarta.inject.Inject;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

/**
 * Base class for implementing {@link Bot} components for Netflux channels.
 *
 * @version $Id$
 * @since 15.10.11
 * @since 16.4.1
 * @since 16.5.0RC1
 */
public abstract class AbstractBot implements Bot, Initializable
{
    @Inject
    private IdGenerator idGenerator;

    private String id;

    @Override
    public void initialize() throws InitializationException
    {
        this.id = this.idGenerator.generateBotId();
    }

    @Override
    public String getId()
    {
        return this.id;
    }
}
