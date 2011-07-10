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
package net.sf.jpasecurity.security.authentication;

import java.util.Collection;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import net.sf.jpasecurity.configuration.AuthenticationProvider;
import net.sf.jpasecurity.configuration.AuthenticationProviderSecurityContext;
import net.sf.jpasecurity.configuration.SecurityContext;
import net.sf.jpasecurity.mapping.Alias;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class tries to detect the security context for an application.
 * Internally it uses an {@link AuthenticationProviderSecurityContext}.
 * For that security context the following authentication providers are used in the specified order:
 * <ol>
 *   <li>
 *     If an <tt>javax.ejb.EJBContext</tt> is accessible via JNDI lookup,
 *     an {@link EjbAuthenticationProvider} is used.
 *   </li>
 *   <li>
 *     If none of the former conditions is true, a {@link DefaultAuthenticationProvider} is used.
 *   </li>
 * </ol>
 * @author Arne Limburg
 */
public class AutodetectingSecurityContext implements SecurityContext {

    private static final Log LOG = LogFactory.getLog(AutodetectingSecurityContext.class);

    private SecurityContext securityContext;

    public AutodetectingSecurityContext() {
        securityContext = new AuthenticationProviderSecurityContext(autodetectAuthenticationProvider());
    }

    protected AuthenticationProvider autodetectAuthenticationProvider() {
        try {
            InitialContext context = new InitialContext();
            context.lookup("java:comp/EJBContext");
            LOG.info("autodetected presence of EJB, using EJBAuthenticationProvider");
            return new EjbAuthenticationProvider();
        } catch (NamingException ejbSecurityNotFoundException) {
            LOG.info("falling back to DefaultAuthenticationPovider");
            return new DefaultAuthenticationProvider();
        }
    }

    public Collection<Alias> getAliases() {
        return securityContext.getAliases();
    }

    public Object getAliasValue(Alias alias) {
        return securityContext.getAliasValue(alias);
    }

    public Collection<?> getAliasValues(Alias alias) {
        return securityContext.getAliasValues(alias);
    }
}
