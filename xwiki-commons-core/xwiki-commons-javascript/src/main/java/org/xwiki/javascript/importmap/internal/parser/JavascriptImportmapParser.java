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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.xwiki.webjars.WebjarPathDescriptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Parse the importmap declaration format.
 *
 * @version $Id$
 * @since 18.0.0RC1
 */
public class JavascriptImportmapParser
{
    /**
     * Javascript Import map property name.
     */
    public static final String JAVASCRIPT_IMPORTMAP_PROPERTY = "xwiki.extension.javascript.modules.importmap";

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    /**
     * Parse an importmap declaration.
     *
     * @param importMapJSON the importmap declaration as a JSON string
     * @return the resolve mapping of importmap module id and their corresponding {@link ImportmapPathDescriptor}
     *     definitions
     * @throws JavascriptImportmapException in case of issue when parsing the provided JSON string
     */
    public Map<String, ImportmapPathDescriptor> parse(String importMapJSON) throws JavascriptImportmapException
    {
        Map<String, ImportmapPathDescriptor> extensionImportMap;
        Object parsedJSON;
        try {
            parsedJSON = OBJECT_MAPPER.readValue(importMapJSON, Object.class);
        } catch (JsonProcessingException e) {
            throw new JavascriptImportmapException("Malformed importmap definition", e);
        }
        extensionImportMap = new LinkedHashMap<>();
        if (parsedJSON instanceof Map parsedJSONMap) {
            for (Map.Entry o : (Set<Map.Entry>) parsedJSONMap.entrySet()) {
                Object value = o.getValue();
                var key = String.valueOf(o.getKey());
                if (value instanceof Map valueMap) {
                    boolean eager = Boolean.parseBoolean(String.valueOf(valueMap.getOrDefault("eager",
                        Boolean.FALSE.toString())));
                    boolean anonymous = Boolean.parseBoolean(String.valueOf(valueMap.getOrDefault("anonymous",
                        Boolean.FALSE.toString())));
                    WebjarPathDescriptor webjarPathDescriptor;
                    try {
                        webjarPathDescriptor = new WebjarPathDescriptor(
                            (String) valueMap.get("webjarId"),
                            (String) valueMap.get("namespace"),
                            (String) valueMap.get("path"),
                            (Map<String, ?>) valueMap.get("params"));
                    } catch (NullPointerException | IllegalArgumentException e) {
                        throw new JavascriptImportmapException("Malformed value for key [%s]".formatted(key), e);
                    }
                    ImportmapPathDescriptor importmapPathDescriptor = new ImportmapPathDescriptor(
                        webjarPathDescriptor, eager, anonymous);
                    extensionImportMap.put(key, importmapPathDescriptor);
                } else {
                    var anonymous = key.startsWith("_");
                    extensionImportMap.put(anonymous ? key.substring(1) : key, parseValue(String.valueOf(value),
                        anonymous));
                }
            }
        }
        return extensionImportMap;
    }

    private ImportmapPathDescriptor parseValue(String value, boolean anonymous) throws JavascriptImportmapException
    {
        var separator = "/";
        if (!value.contains(separator)) {
            throw new JavascriptImportmapException("Invalid importmap value: %s".formatted(value));
        }
        var eager = value.startsWith("!");
        var split = (eager ? value.substring(1) : value).split(separator, 2);
        var webjarId = split[0];
        var path = split[1];

        return new ImportmapPathDescriptor(new WebjarPathDescriptor(webjarId, path), eager, anonymous);
    }
}
