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

import java.util.ArrayList;
import java.util.Iterator;
import javax.persistence.EntityManagerFactory;

import org.compass.gps.device.hibernate.lifecycle.HibernateCollectionEventListener;
import org.compass.gps.device.jpa.AbstractDeviceJpaEntityListener;
import org.compass.gps.device.jpa.JpaGpsDevice;
import org.compass.gps.device.jpa.JpaGpsDeviceException;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;

/**
 * Injects lifecycle listeners directly into Hibernate for mirroring operations.
 *
 * <p>By default, registers with plain insert/update/delete listeners, which will be triggered
 * by Hibernate before committing (and up to Hibernate flushing logic). Also allows to be created
 * with setting the <code>registerPostCommitListeneres</code> to <code>true</code> which will cause
 * the insert/update/delete listeneres to be registered as post commit events.
 * 
 * @author kimchy
 */
public class HibernateJpaEntityLifecycleInjector implements JpaEntityLifecycleInjector {

    public static class HibernateEventListener extends AbstractDeviceJpaEntityListener implements PostInsertEventListener,
            PostUpdateEventListener, PostDeleteEventListener {

        private JpaGpsDevice device;

        public HibernateEventListener(JpaGpsDevice device) {
            this.device = device;
        }

        protected JpaGpsDevice getDevice() {
            return this.device;
        }

        public void onPostInsert(PostInsertEvent postInsertEvent) {
            postPersist(postInsertEvent.getEntity());
        }

        public void onPostUpdate(PostUpdateEvent postUpdateEvent) {
            postUpdate(postUpdateEvent.getEntity());
        }

        public void onPostDelete(PostDeleteEvent postDeleteEvent) {
            postRemove(postDeleteEvent.getEntity());
        }
    }

    protected final boolean registerPostCommitListeneres;

    public HibernateJpaEntityLifecycleInjector() {
        this(false);
    }

    /**
     * Creates a new lifecycle injector. Allows to control if the insert/update/delete
     * even listeners will be registered with post commit listeres (flag it <code>true</code>)
     * or with plain post events (triggered based on Hibrenate flushing logic).
     *
     * @param registerPostCommitListeneres <code>true</code> if post commit listeners will be
     * registered. <code>false</code> for plain listeners.
     */
    public HibernateJpaEntityLifecycleInjector(boolean registerPostCommitListeneres) {
        this.registerPostCommitListeneres = registerPostCommitListeneres;
    }

    public boolean requireRefresh() {
        return false;
    }

    public void injectLifecycle(EntityManagerFactory entityManagerFactory, JpaGpsDevice device)
            throws JpaGpsDeviceException {

        HibernateEntityManagerFactory hibernateEntityManagerFactory =
                (HibernateEntityManagerFactory) entityManagerFactory;

        EventListenerRegistry eventRegistry =
                ((SessionFactoryImpl) hibernateEntityManagerFactory.getSessionFactory()).getServiceRegistry().getService(EventListenerRegistry.class);
        Object hibernateEventListener = doCreateListener(device);

        if (hibernateEventListener instanceof PostInsertEventListener) {
            if (registerPostCommitListeneres) {
                eventRegistry.appendListeners(EventType.POST_COMMIT_INSERT, (PostInsertEventListener)hibernateEventListener);
            } else {
                eventRegistry.appendListeners(EventType.POST_INSERT, (PostInsertEventListener)hibernateEventListener);
            }
        }

        if (hibernateEventListener instanceof PostUpdateEventListener) {
            if (registerPostCommitListeneres) {
                eventRegistry.appendListeners(EventType.POST_COMMIT_UPDATE, (PostUpdateEventListener) hibernateEventListener);
            } else {
                eventRegistry.appendListeners(EventType.POST_UPDATE, (PostUpdateEventListener) hibernateEventListener);
            }
        }

        if (hibernateEventListener instanceof PostDeleteEventListener) {
            if (registerPostCommitListeneres) {
                eventRegistry.appendListeners(EventType.POST_COMMIT_DELETE, (PostDeleteEventListener)hibernateEventListener);
            } else {
                eventRegistry.appendListeners(EventType.POST_DELETE, (PostDeleteEventListener)hibernateEventListener);
            }
        }
    }

    public void removeLifecycle(EntityManagerFactory entityManagerFactory, JpaGpsDevice device) throws JpaGpsDeviceException {
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
            if (!(postCollectionRecreateEventListener instanceof HibernateCollectionEventListener)) {
                tempPostCollectionRecreateEventListeners.add(postCollectionRecreateEventListener);
            }
        }
        eventRegistry.setListeners(EventType.POST_COLLECTION_RECREATE, tempPostCollectionRecreateEventListeners.toArray(new PostCollectionRecreateEventListener[tempPostCollectionRecreateEventListeners.size()]));

        EventListenerGroup<PostCollectionUpdateEventListener> postCollectionUpdateEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_COLLECTION_UPDATE);
        ArrayList<PostCollectionUpdateEventListener> tempPostCollectionUpdateEventListeners = new ArrayList<PostCollectionUpdateEventListener>();
        Iterator<PostCollectionUpdateEventListener> postCollectionUpdateEventListenerIterator = postCollectionUpdateEventListeners.listeners().iterator();
        while(postCollectionUpdateEventListenerIterator.hasNext()){
            PostCollectionUpdateEventListener postCollectionUpdateEventListener = postCollectionUpdateEventListenerIterator.next();
            if(!(postCollectionUpdateEventListener instanceof  HibernateCollectionEventListener)){
                tempPostCollectionUpdateEventListeners.add(postCollectionUpdateEventListener);
            }
        }
        eventRegistry.setListeners(EventType.POST_COLLECTION_UPDATE, tempPostCollectionUpdateEventListeners.toArray(new PostCollectionUpdateEventListener[tempPostCollectionUpdateEventListeners.size()]));

        EventListenerGroup<PostCollectionRemoveEventListener> postCollectionRemoveEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_COLLECTION_REMOVE);
        ArrayList<PostCollectionRemoveEventListener> tempPostCollectionRemoveEventListeners = new ArrayList<PostCollectionRemoveEventListener>();
        Iterator<PostCollectionRemoveEventListener> postCollectionRemoveEventListenerIterator = postCollectionRemoveEventListeners.listeners().iterator();
        while(postCollectionRemoveEventListenerIterator.hasNext()){
            PostCollectionRemoveEventListener postCollectionRemoveEventListener = postCollectionRemoveEventListenerIterator.next();
            if(!(postCollectionRemoveEventListener instanceof  HibernateCollectionEventListener)){
                tempPostCollectionRemoveEventListeners.add(postCollectionRemoveEventListener);
            }
        }
        eventRegistry.setListeners(EventType.POST_COLLECTION_REMOVE, tempPostCollectionRemoveEventListeners.toArray(new PostCollectionRemoveEventListener[tempPostCollectionRemoveEventListeners.size()]));

    }

    protected Object doCreateListener(JpaGpsDevice device) {
        return new HibernateEventListener(device);
    }
}
