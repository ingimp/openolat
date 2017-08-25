/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.resource.accesscontrol.provider.auto.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.olat.repository.RepositoryEntry;
import org.olat.resource.accesscontrol.provider.auto.IdentifierKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * Initial date: 15.08.2017<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
@Component
class IdentifierHandler {

	@Autowired
    private Collection<IdentifierKeyHandler> loadedHandlers;

    private Map<IdentifierKey, IdentifierKeyHandler> handlers = new HashMap<>();

    @PostConstruct
    void initHandlerCache() {
        for(IdentifierKeyHandler handler : loadedHandlers) {
            handlers.put(handler.getItentifierKey(), handler);
        }
    }

    /**
     * Finds the RepositoryEntry for a given identifier key and value.
     *
     * @param key
     * @param value
     * @returns the course or null if not found or too many found
     */
    RepositoryEntry findRepositoryEntry(IdentifierKey key, String value) {
		List<RepositoryEntry> entries = handlers.get(key).find(value);

		RepositoryEntry entry = null;
		if (entries.size() == 1) {
			entry = entries.get(0);
		}
		return entry;
    }

    /**
     * Takes a RepostoryEntry and returns the value for a given IdenifierKey
     *
     * @param key
     * @param entry
     * @return
     */
	String getRepositoryEntryValue(IdentifierKey key, RepositoryEntry entry) {
		return handlers.get(key).getRepositoryEntryValue(entry);
	}

}
