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
package org.xwiki.job.internal;

import java.io.File;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * URL-encoding based {@link JobStatusFolderResolver} that implements the encoding used before XWiki 16.10.x.
 *
 * @version $Id$
 * @since 17.2.0RC1
 * @since 16.10.6
 */
@Component
@Singleton
@Named("version1")
@Priority(10000)
public class Version1JobStatusFolderResolver extends AbstractJobStatusFolderResolver
{
    @Override
    protected File addIDElement(String idElement, File folder)
    {
        return new File(folder, nullAwareURLEncode(idElement));
    }
}
