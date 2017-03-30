/*
 * Copyright 2014 - 2016 Arne Limburg
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
package org.jpasecurity.model.client;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.jpasecurity.persistence.AbstractEntityTestCase;
import org.jpasecurity.security.authentication.TestSecurityContext;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
@Ignore
public class CriteriaBuilderTest extends AbstractEntityTestCase {

    private static final String EMAIL = "test@test.org";

    @BeforeClass
    public static void createEntityManagerFactory() throws SQLException {
        TestSecurityContext.authenticate(EMAIL);
        createEntityManagerFactory("client");
        dropForeignKey("jdbc:hsqldb:mem:test", "sa", "", "CLIENTOPERATIONSTRACKING", "CLIENT");
    }

    @Test
    public void criteria() {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ClientEntityBrowserDto> c = cb.createQuery(ClientEntityBrowserDto.class);
        Root<Client> client = c.from(Client.class);

        Join<Client, ClientGroup> clientGroup = client.join("group", JoinType.LEFT);
        Join<Client, ClientStructure> clientStructure = client.join("structure", JoinType.LEFT);
        Join<ClientType, ClientTypeGroup> clientTypeGroup
            = client.join("type", JoinType.LEFT).join("clientTypeGroup", JoinType.LEFT);

        Selection<ClientEntityBrowserDto> selection
            = cb.construct(ClientEntityBrowserDto.class,
                           client.<Integer>get("id"),
                           client.<String>get("number"),
                           client.<String>get("name"),
                           clientTypeGroup.<String>get("name"),
                           clientGroup.<String>get("name"),
                           clientStructure.<String>get("name"));

        c.select(selection);
        c.distinct(true);
        TypedQuery<ClientEntityBrowserDto> query = getEntityManager().createQuery(c);
        assertTrue(query.getResultList().isEmpty());
    }
}
