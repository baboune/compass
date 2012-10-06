/*
 * Copyright 2004-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.compass.gps.device.jpa.lifecycle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import javax.persistence.EntityManagerFactory;

import org.compass.gps.device.hibernate.lifecycle.HibernateCollectionEventListener;
import org.compass.gps.device.jpa.JpaGpsDevice;
import org.compass.gps.device.jpa.JpaGpsDeviceException;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;

/**
 * @author kimchy
 */
public class HibernateJpaEntityCollectionLifecycleInjector extends HibernateJpaEntityLifecycleInjector {

    public static class HibernateCollectionEventListener extends HibernateEventListener
            implements PostCollectionRecreateEventListener, PostCollectionRemoveEventListener, PostCollectionUpdateEventListener {

        public HibernateCollectionEventListener(JpaGpsDevice device) {
            super(device);
        }

        public void onPostRecreateCollection(PostCollectionRecreateEvent postCollectionRecreateEvent) {
            processCollectionEvent(postCollectionRecreateEvent);
        }

        public void onPostRemoveCollection(PostCollectionRemoveEvent postCollectionRemoveEvent) {
            processCollectionEvent(postCollectionRemoveEvent);
        }

        public void onPostUpdateCollection(PostCollectionUpdateEvent postCollectionUpdateEvent) {
            processCollectionEvent(postCollectionUpdateEvent);
        }

        private void processCollectionEvent(AbstractCollectionEvent event) {
            final Object entity = event.getAffectedOwnerOrNull();
            if (entity == null) {
                //Hibernate cannot determine every single time the owner especially incase detached objects are involved
                // or property-ref is used
                //Should log really but we don't know if we're interested in this collection for indexing
                return;
            }
            Serializable id = getId(entity, event);
            if (id == null) {
                log.warn("Unable to reindex entity on collection change, id cannot be extracted: " + event.getAffectedOwnerEntityName());
                return;
            }

            postUpdate(entity);
        }

        private Serializable getId(Object entity, AbstractCollectionEvent event) {
            Serializable id = event.getAffectedOwnerIdOrNull();
            if (id == null) {
                //most likely this recovery is unnecessary since Hibernate Core probably try that
                EntityEntry entityEntry = event.getSession().getPersistenceContext().getEntry(entity);
                id = entityEntry == null ? null : entityEntry.getId();
            }
            return id;
        }
    }

    private Object eventListener;

    public HibernateJpaEntityCollectionLifecycleInjector() {
        super();
    }

    public HibernateJpaEntityCollectionLifecycleInjector(boolean registerPostCommitListeneres) {
        super(registerPostCommitListeneres);
    }

    public void injectLifecycle(EntityManagerFactory entityManagerFactory, JpaGpsDevice device) throws JpaGpsDeviceException {
        super.injectLifecycle(entityManagerFactory, device);

        if (registerPostCommitListeneres) {
            return;
        }

        HibernateEntityManagerFactory hibernateEntityManagerFactory =
                (HibernateEntityManagerFactory) entityManagerFactory;
        SessionFactoryImpl sessionFactory = (SessionFactoryImpl) hibernateEntityManagerFactory.getSessionFactory();
        EventListenerRegistry eventRegistry =
                ((SessionFactoryImpl) sessionFactory).getServiceRegistry().getService(EventListenerRegistry.class);

        if (eventListener instanceof PostCollectionRecreateEventListener) {

            eventRegistry.appendListeners(EventType.POST_COLLECTION_RECREATE, (PostCollectionRecreateEventListener)eventListener);

        }

        if (eventListener instanceof PostCollectionRemoveEventListener) {
            eventRegistry.appendListeners(EventType.POST_COLLECTION_REMOVE, (PostCollectionRemoveEventListener) eventListener);
        }

        if (eventListener instanceof PostCollectionUpdateEventListener) {
            eventRegistry.appendListeners(EventType.POST_COLLECTION_UPDATE, (PostCollectionUpdateEventListener) eventListener);
        }
    }

    public void removeLifecycle(EntityManagerFactory entityManagerFactory, JpaGpsDevice device) throws JpaGpsDeviceException {
        super.removeLifecycle(entityManagerFactory, device);

        if (registerPostCommitListeneres) {
            return;
        }

        HibernateEntityManagerFactory hibernateEntityManagerFactory =
                (HibernateEntityManagerFactory) entityManagerFactory;
        SessionFactoryImpl sessionFactory = (SessionFactoryImpl) hibernateEntityManagerFactory.getSessionFactory();
        EventListenerRegistry eventRegistry =
                ((SessionFactoryImpl) sessionFactory).getServiceRegistry().getService(EventListenerRegistry.class);

        EventListenerGroup<PostCollectionRecreateEventListener> postCollectionRecreateEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_COLLECTION_RECREATE);
        ArrayList<PostCollectionRecreateEventListener> tempPostCollectionRecreateEventListeners = new ArrayList<PostCollectionRecreateEventListener>();
        Iterator<PostCollectionRecreateEventListener> postCollectionRecreateEventListenerIterator= postCollectionRecreateEventListeners.listeners().iterator();
        while(postCollectionRecreateEventListenerIterator.hasNext()){
            PostCollectionRecreateEventListener postCollectionRecreateEventListener = postCollectionRecreateEventListenerIterator.next();
            if (!(postCollectionRecreateEventListener instanceof org.compass.gps.device.hibernate.lifecycle.HibernateCollectionEventListener)) {
                tempPostCollectionRecreateEventListeners.add(postCollectionRecreateEventListener);
            }
        }
        eventRegistry.setListeners(EventType.POST_COLLECTION_RECREATE, tempPostCollectionRecreateEventListeners.toArray(new PostCollectionRecreateEventListener[tempPostCollectionRecreateEventListeners.size()]));

        EventListenerGroup<PostCollectionUpdateEventListener> postCollectionUpdateEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_COLLECTION_UPDATE);
        ArrayList<PostCollectionUpdateEventListener> tempPostCollectionUpdateEventListeners = new ArrayList<PostCollectionUpdateEventListener>();
        Iterator<PostCollectionUpdateEventListener> postCollectionUpdateEventListenerIterator = postCollectionUpdateEventListeners.listeners().iterator();
        while(postCollectionUpdateEventListenerIterator.hasNext()){
            PostCollectionUpdateEventListener postCollectionUpdateEventListener = postCollectionUpdateEventListenerIterator.next();
            if(!(postCollectionUpdateEventListener instanceof org.compass.gps.device.hibernate.lifecycle.HibernateCollectionEventListener)){
                tempPostCollectionUpdateEventListeners.add(postCollectionUpdateEventListener);
            }
        }
        eventRegistry.setListeners(EventType.POST_COLLECTION_UPDATE, tempPostCollectionUpdateEventListeners.toArray(new PostCollectionUpdateEventListener[tempPostCollectionUpdateEventListeners.size()]));

        EventListenerGroup<PostCollectionRemoveEventListener> postCollectionRemoveEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_COLLECTION_REMOVE);
        ArrayList<PostCollectionRemoveEventListener> tempPostCollectionRemoveEventListeners = new ArrayList<PostCollectionRemoveEventListener>();
        Iterator<PostCollectionRemoveEventListener> postCollectionRemoveEventListenerIterator = postCollectionRemoveEventListeners.listeners().iterator();
        while(postCollectionRemoveEventListenerIterator.hasNext()){
            PostCollectionRemoveEventListener postCollectionRemoveEventListener = postCollectionRemoveEventListenerIterator.next();
            if(!(postCollectionRemoveEventListener instanceof org.compass.gps.device.hibernate.lifecycle.HibernateCollectionEventListener)){
                tempPostCollectionRemoveEventListeners.add(postCollectionRemoveEventListener);
            }
        }
        eventRegistry.setListeners(EventType.POST_COLLECTION_REMOVE, tempPostCollectionRemoveEventListeners.toArray(new PostCollectionRemoveEventListener[tempPostCollectionRemoveEventListeners.size()]));

        eventListener = null;
    }

    protected Object doCreateListener(JpaGpsDevice device) {
        eventListener = new HibernateCollectionEventListener(device);
        return eventListener;
    }
}
