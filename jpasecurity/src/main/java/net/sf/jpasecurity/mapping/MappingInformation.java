/*
 * Copyright 2008 Arne Limburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package net.sf.jpasecurity.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.PersistenceException;

/**
 * This class provides mapping information for a specific persistence unit.
 * Initialized with a {@link javax.persistence.spi.PersistenceUnitInfo} it parses
 * the persistence unit and builds the mapping information.
 * @author Arne Limburg
 */
public class MappingInformation {

    private String persistenceUnitName;
    private Map<String, String> namedQueries = new HashMap<String, String>();
    private Map<Class<?>, ClassMappingInformation> entityTypeMappings
        = new HashMap<Class<?>, ClassMappingInformation>();
    private Map<String, ClassMappingInformation> entityNameMappings;

    /**
     * Creates mapping information from the specified persistence unit information.
     * @param persistenceUnitInfo the persistence unit information to create the mapping information for
     * @param mappingParser the parser to parse the persistence mapping 
     */
    public MappingInformation(String persistenceUnitName,
                              Map<Class<?>, ClassMappingInformation> entityTypeMappings,
                              Map<String, String> namedQueries) {
        this.persistenceUnitName = persistenceUnitName;
        this.entityTypeMappings = entityTypeMappings;
        this.namedQueries = namedQueries;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public Set<String> getNamedQueryNames() {
        return Collections.unmodifiableSet(namedQueries.keySet());
    }
    
    public String getNamedQuery(String name) {
        return namedQueries.get(name);
    }

    public Collection<Class<?>> getPersistentClasses() {
        return Collections.unmodifiableSet(entityTypeMappings.keySet());
    }

    public ClassMappingInformation getClassMapping(Class<?> entityType) {
        ClassMappingInformation classMapping = entityTypeMappings.get(entityType);
        while (classMapping == null && entityType != null) {
            entityType = entityType.getSuperclass();
            classMapping = entityTypeMappings.get(entityType);
        }
        return classMapping;
    }

    public ClassMappingInformation getClassMapping(String entityName) {
        if (entityNameMappings == null) {
            initializeEntityNameMappings();
        }
        ClassMappingInformation classMapping = entityNameMappings.get(entityName);
        if (classMapping == null) {
            throw new PersistenceException("Could not find mapping for entity with name \"" + entityName + '"');
        }
        return classMapping;
    }

    public Class<?> getType(String path, Map<String, Class<?>> aliasTypes) {
        try {
            String[] entries = path.split("\\.");
            Class<?> type = aliasTypes.get(entries[0]);
            for (int i = 1; i < entries.length; i++) {
                type = getClassMapping(type).getPropertyMapping(entries[i]).getProperyType();
            }
            return type;
        } catch (NullPointerException e) {
            throw new PersistenceException("Could not determine type of alias \"" + path + "\"", e);
        }
    }

    private void initializeEntityNameMappings() {
        entityNameMappings = new HashMap<String, ClassMappingInformation>();
        for (ClassMappingInformation classMapping: entityTypeMappings.values()) {
            entityNameMappings.put(classMapping.getEntityName(), classMapping);
            entityNameMappings.put(classMapping.getEntityType().getName(), classMapping);
        }
    }
}
