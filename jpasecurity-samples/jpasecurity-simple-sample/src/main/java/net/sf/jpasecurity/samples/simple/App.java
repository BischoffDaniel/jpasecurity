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
package net.sf.jpasecurity.samples.simple;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import net.sf.jpasecurity.contacts.Contact;
import net.sf.jpasecurity.contacts.User;

/**
 * @author Arne Limburg
 */
public class App {

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("contacts");
        createUsers(entityManagerFactory);
        displayUserCount(entityManagerFactory);
        createContacts(entityManagerFactory);
        displayContactCount(entityManagerFactory);
    }

    public static void createUsers(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager;

        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(new User("John"));
        entityManager.persist(new User("Mary"));
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    public static void displayUserCount(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager;
        
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        List<User> users = entityManager.createQuery("SELECT user FROM User user").getResultList();
        System.out.println("users.size = " + users.size());
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    public static void createContacts(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager;

        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        User john = (User)entityManager.createQuery("SELECT user FROM User user WHERE user.name = 'John'").getSingleResult();
        User mary = (User)entityManager.createQuery("SELECT user FROM User user WHERE user.name = 'Mary'").getSingleResult();
        entityManager.persist(new Contact(john, "john@jpasecurity.sf.net"));
        entityManager.persist(new Contact(john, "0 12 34 - 56 789"));
        entityManager.persist(new Contact(mary, "mary@jpasecurity.sf.net"));
        entityManager.persist(new Contact(mary, "12 34 56 78 90"));
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    public static void displayContactCount(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager;

        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        List<Contact> contacts = entityManager.createQuery("SELECT contact FROM Contact contact").getResultList();
        System.out.println("contacts.size = " + contacts.size());
        entityManager.getTransaction().commit();
        entityManager.close();
    }
}
