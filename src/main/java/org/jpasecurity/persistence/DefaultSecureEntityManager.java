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
package org.jpasecurity.persistence;

import static org.jpasecurity.AccessType.READ;

import java.util.Collection;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jpasecurity.AccessManager;
import org.jpasecurity.AccessType;
import org.jpasecurity.ExceptionFactory;
import org.jpasecurity.configuration.Configuration;
import org.jpasecurity.jpql.compiler.MappedPathEvaluator;
import org.jpasecurity.jpql.compiler.PathEvaluator;
import org.jpasecurity.jpql.compiler.SimpleSubselectEvaluator;
import org.jpasecurity.jpql.compiler.SubselectEvaluator;
import org.jpasecurity.mapping.MappingInformation;
import org.jpasecurity.persistence.compiler.EntityManagerEvaluator;
import org.jpasecurity.persistence.security.CriteriaEntityFilter;
import org.jpasecurity.persistence.security.CriteriaFilterResult;
import org.jpasecurity.security.FilterResult;

/**
 * This class handles invocations on proxies of entity managers.
 * @author Arne Limburg
 */
public class DefaultSecureEntityManager extends DelegatingEntityManager
                                        implements SecureEntityManager {

    private static final Log LOG = LogFactory.getLog(DefaultSecureEntityManager.class);

    private SecureEntityManagerFactory entityManagerFactory;
    private MappingInformation mappingInformation;
    private AccessManager accessManager;
    private CriteriaEntityFilter entityFilter;

    protected DefaultSecureEntityManager(SecureEntityManagerFactory parent,
                                         EntityManager entityManager,
                                         Configuration configuration,
                                         MappingInformation mapping) {
        super(entityManager);
        entityManagerFactory = parent;
        this.mappingInformation = mapping;
        ExceptionFactory exceptionFactory = configuration.getExceptionFactory();
        PathEvaluator pathEvaluator = new MappedPathEvaluator(mappingInformation, exceptionFactory);
        SubselectEvaluator simpleSubselectEvaluator = new SimpleSubselectEvaluator(exceptionFactory);
        SubselectEvaluator entityManagerEvaluator
            = new EntityManagerEvaluator(entityManager, pathEvaluator);
        this.entityFilter = new CriteriaEntityFilter(mappingInformation,
                                                     configuration.getSecurityContext(),
                                                     entityManager.getCriteriaBuilder(),
                                                     exceptionFactory,
                                                     configuration.getAccessRulesProvider().getAccessRules(),
                                                     simpleSubselectEvaluator,
                                                     entityManagerEvaluator);
        this.accessManager = configuration.createAccessManager(mapping, entityFilter);
        AccessManager.Instance.register(accessManager);
    }

    @Override
    public SecureEntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public void persist(Object entity) {
        AccessManager.Instance.register(accessManager);
        super.persist(entity);
    }

    public <T> T merge(T entity) {
        AccessManager.Instance.register(accessManager);
        return super.merge(entity);
    }

    public void remove(Object entity) {
        AccessManager.Instance.register(accessManager);
        super.remove(entity);
    }

    public void detach(Object entity) {
        AccessManager.Instance.register(accessManager);
        super.detach(entity);
    }

    public <T> T find(Class<T> type, Object id) {
        return find(type, id, null, null);
    }

    public <T> T find(Class<T> type, Object id, Map<String, Object> properties) {
        return find(type, id, null, properties);
    }

    public <T> T find(Class<T> type, Object id, LockModeType lockMode) {
        return find(type, id, lockMode, null);
    }

    public <T> T find(Class<T> type, Object id, LockModeType lockMode, Map<String, Object> properties) {
        AccessManager.Instance.register(accessManager);
        accessManager.delayChecks();
        T entity;
        if (lockMode != null && properties != null) {
            entity = super.find(type, id, lockMode, properties);
        } else if (lockMode != null) {
            entity = super.find(type, id, lockMode);
        } else if (properties != null) {
            entity = super.find(type, id, properties);
        } else {
            entity = super.find(type, id);
        }
        accessManager.checkNow();
        return entity;
    }

    public void refresh(Object entity) {
        AccessManager.Instance.register(accessManager);
        super.refresh(entity);
    }

    public void refresh(Object entity, LockModeType lockMode) {
        AccessManager.Instance.register(accessManager);
        super.refresh(entity, lockMode);
    }

    public void refresh(Object entity, Map<String, Object> properties) {
        AccessManager.Instance.register(accessManager);
        super.refresh(entity, properties);
    }

    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        AccessManager.Instance.register(accessManager);
        super.refresh(entity, lockMode, properties);
    }

    public <T> T getReference(Class<T> type, Object id) {
        AccessManager.Instance.register(accessManager);
        return super.getReference(type, id);
    }

    public void lock(Object entity, LockModeType lockMode) {
        AccessManager.Instance.register(accessManager);
        super.lock(entity, lockMode);
    }

    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        AccessManager.Instance.register(accessManager);
        super.lock(entity, lockMode, properties);
    }

    public boolean contains(Object entity) {
        AccessManager.Instance.register(accessManager);
        return super.contains(entity);
    }

    public Query createNamedQuery(String name) {
        AccessManager.Instance.register(accessManager);
        String namedQuery = mappingInformation.getNamedQuery(name);
        if (namedQuery != null) {
            return createQuery(namedQuery);
        }
        String namedNativeQuery = mappingInformation.getNamedNativeQuery(name);
        if (namedNativeQuery != null) {
            return super.createNamedQuery(name);
        }
        throw new IllegalArgumentException("No named query with name " + name);
    }

    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        AccessManager.Instance.register(accessManager);
        String namedQuery = mappingInformation.getNamedQuery(name);
        if (namedQuery != null) {
            return createQuery(namedQuery, resultClass);
        }
        String namedNativeQuery = mappingInformation.getNamedNativeQuery(name);
        if (namedNativeQuery != null) {
            return super.createNamedQuery(name, resultClass);
        }
        throw new IllegalArgumentException("No named query with name " + name);
    }

    public void flush() {
        AccessManager.Instance.register(accessManager);
        super.flush();
    }

    public void clear() {
        AccessManager.Instance.register(accessManager);
        super.clear();
    }

    public void close() {
        try {
            super.close();
        } finally {
            AccessManager.Instance.unregister(accessManager);
        }
    }

    /**
     * This implementation filters the query according to the provided security context
     */
    public Query createQuery(String qlString) {
        return createQuery(qlString, Object.class, Query.class);
    }

    /**
     * This implementation filters the query according to the provided security context
     */
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        return createQuery(qlString, resultClass, TypedQuery.class);
    }

    private <T, Q extends Query> Q createQuery(String qlString, Class<T> resultClass, Class<Q> queryClass) {
        AccessManager.Instance.register(accessManager);
        FilterResult<String> filterResult = entityFilter.filterQuery(qlString, READ);
        if (filterResult.getQuery() == null) {
            return (Q)new EmptyResultQuery<T>(createDelegateQuery(qlString, resultClass, queryClass));
        } else {
            Q query;
            if (filterResult.getConstructorArgReturnType() != null) {
                query = (Q)new SecureQuery<T>(createDelegateQuery(filterResult.getQuery(), null, Query.class),
                                              (Class<T>)filterResult.getConstructorArgReturnType(),
                                              filterResult.getSelectedPaths(),
                                              super.getFlushMode());
            } else {
                query = (Q)new SecureQuery<T>(createDelegateQuery(filterResult.getQuery(), resultClass, queryClass),
                                              null,
                                              filterResult.getSelectedPaths(),
                                              super.getFlushMode());
            }
            if (filterResult.getParameters() != null) {
                for (Map.Entry<String, Object> parameter: filterResult.getParameters().entrySet()) {
                    query.setParameter(parameter.getKey(), parameter.getValue());
                }
            }
            return query;
        }
    }

    private <Q extends Query> Q createDelegateQuery(String qlString, Class<?> resultClass, Class<Q> queryClass) {
        AccessManager.Instance.register(accessManager);
        if (TypedQuery.class.equals(queryClass)) {
            return (Q)super.createQuery(qlString, resultClass);
        } else {
            return (Q)super.createQuery(qlString);
        }
    }

    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        AccessManager.Instance.register(accessManager);
        FilterResult<CriteriaQuery<T>> filterResult = entityFilter.filterQuery(criteriaQuery);
        if (filterResult.getQuery() == null) {
            return new EmptyResultQuery<T>(super.createQuery(criteriaQuery));
        } else {
            SecureQuery<T> query = new SecureQuery<T>(super.createQuery(filterResult.getQuery()),
                                      null, // TODO how to extract this?
                                      filterResult.getSelectedPaths(),
                                      super.getFlushMode());
            if (filterResult.getParameters() != null && filterResult instanceof CriteriaFilterResult) {
                CriteriaFilterResult<CriteriaQuery<T>> criteriaResult
                    = (CriteriaFilterResult<CriteriaQuery<T>>)filterResult;
                for (Parameter<?> parameter: criteriaResult.getCriteriaParameters()) {
                    query.setParameter((Parameter<Object>)parameter,
                                       filterResult.getParameters().get(parameter.getName()));
                }
            }
            return query;
        }
    }

    public EntityTransaction getTransaction() {
        AccessManager.Instance.register(accessManager);
        return super.getTransaction();
    }

    public <T> T unwrap(Class<T> cls) {
        AccessManager.Instance.register(accessManager);
        if (cls.isAssignableFrom(getClass())) {
            return (T)this;
        } else {
            return super.unwrap(cls);
        }
    }

    public LockModeType getLockMode(Object entity) {
        AccessManager.Instance.register(accessManager);
        return super.getLockMode(entity);
    }

    public boolean isAccessible(AccessType accessType, String entityName, Object... parameters) {
        AccessManager.Instance.register(accessManager);
        return accessManager.isAccessible(accessType, entityName, parameters);
    }

    public boolean isAccessible(AccessType accessType, Object entity) {
        AccessManager.Instance.register(accessManager);
        return accessManager.isAccessible(accessType, entity);
    }

    @Override
    public void checkAccess(AccessType accessType, Object entity) {
        accessManager.checkAccess(accessType, entity);
    }

    @Override
    public void delayChecks() {
        accessManager.delayChecks();
    }

    @Override
    public void checkNow() {
        accessManager.checkNow();
    }

    @Override
    public void disableChecks() {
        accessManager.disableChecks();
    }

    @Override
    public void enableChecks() {
        accessManager.enableChecks();
    }

    @Override
    public void ignoreChecks(AccessType accessType, Collection<?> entities) {
        accessManager.ignoreChecks(accessType, entities);
    }
}
