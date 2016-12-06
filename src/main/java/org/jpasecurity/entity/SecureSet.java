/*
 * Copyright 2008 - 2016 Arne Limburg
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
package org.jpasecurity.entity;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A set-implementation of secure collection.
 * @author Arne Limburg
 */
public class SecureSet<E> extends AbstractSecureCollection<E, Set<E>> implements Set<E> {

    public SecureSet(Set<E> set) {
        super(set);
    }

    SecureSet(Set<E> original, Set<E> filtered) {
        super(original, filtered);
    }

    protected Set<E> createFiltered() {
        return new LinkedHashSet<E>();
    }
}
