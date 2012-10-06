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

package org.compass.gps.device.hibernate.lifecycle;

import java.util.ArrayList;
import java.util.Iterator;

import org.compass.gps.device.hibernate.HibernateGpsDevice;
import org.compass.gps.device.hibernate.HibernateGpsDeviceException;
import org.hibernate.SessionFactory;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.search.event.EventListenerRegister;

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
public class DefaultHibernateEntityLifecycleInjector implements HibernateEntityLifecycleInjector {

    protected boolean registerPostCommitListeneres = false;

    protected boolean marshallIds = false;

    protected boolean pendingCascades = true;

    protected boolean processCollection = true;

    public DefaultHibernateEntityLifecycleInjector() {
        this(false);
    }

    /**
     * Creates a new lifecycle injector. Allows to control if the insert/update/delete
     * even listeners will be registered with post commit listeres (flag it <code>true</code>)
     * or with plain post events (triggered based on Hibrenate flushing logic).
     *
     * @param registerPostCommitListeneres <code>true</code> if post commit listeners will be
     *                                     registered. <code>false</code> for plain listeners.
     */
    public DefaultHibernateEntityLifecycleInjector(boolean registerPostCommitListeneres) {
        this.registerPostCommitListeneres = registerPostCommitListeneres;
    }

    /**
     * Should the listener try and marshall ids for the event listener of post insert. Some
     * Hibernate versions won't put the generated ids in the object that is inserted. Defaults
     * to <code>false</code>.
     */
    public void setMarshallIds(boolean marshallIds) {
        this.marshallIds = marshallIds;
    }

    /**
     * Should the listener try and handle pending cascades avoiding trying to save/update relationships in Compass
     * before they were processed by Hibernate. Default to <code>true<code>.
     *
     * <p>Note, if set, might cause Compass event processing to be a *tad* slower.
     */
    public void setPendingCascades(boolean pendingCascades) {
        this.pendingCascades = pendingCascades;
    }

    /**
     * Should the event listener automatically set the processed flag on collections that are created as a result
     * of the marshalling process of Compass. Defaults to <code>true</code>.
     */
    public void setProcessCollection(boolean processCollection) {
        this.processCollection = processCollection;
    }

    public void injectLifecycle(SessionFactory sessionFactory, HibernateGpsDevice device) throws HibernateGpsDeviceException {

        EventListenerRegistry eventRegistry =
                ((SessionFactoryImpl) sessionFactory).getServiceRegistry().getService(EventListenerRegistry.class);

        Object hibernateEventListener = doCreateListener(device);

        if (hibernateEventListener instanceof PostInsertEventListener) {
            if (registerPostCommitListeneres) {
                eventRegistry.appendListeners(EventType.POST_COMMIT_INSERT, (PostInsertEventListener) hibernateEventListener);
            } else {
                eventRegistry.appendListeners(EventType.POST_INSERT, (PostInsertEventListener) hibernateEventListener);
            }
        }

        if (hibernateEventListener instanceof PostUpdateEventListener) {
            if (registerPostCommitListeneres) {
                eventRegistry.appendListeners(EventType.POST_COMMIT_UPDATE, (PostUpdateEventListener)hibernateEventListener);
            } else {
                eventRegistry.appendListeners(EventType.POST_UPDATE, (PostUpdateEventListener)hibernateEventListener);
            }
        }

        if (hibernateEventListener instanceof PostDeleteEventListener) {
            if (registerPostCommitListeneres) {
                eventRegistry.appendListeners(EventType.POST_COMMIT_DELETE, (PostDeleteEventListener) hibernateEventListener);
            } else {
                eventRegistry.appendListeners(EventType.POST_DELETE, (PostDeleteEventListener) hibernateEventListener);
            }
        }
    }

    public void removeLifecycle(SessionFactory sessionFactory, HibernateGpsDevice device) throws HibernateGpsDeviceException {
        EventListenerRegistry eventRegistry =
                ((SessionFactoryImpl) sessionFactory).getServiceRegistry().getService(EventListenerRegistry.class);

        if(registerPostCommitListeneres){
            EventListenerGroup<PostInsertEventListener> postInsertEventListenerEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_COMMIT_INSERT);
            Iterator<PostInsertEventListener> iter = postInsertEventListenerEventListeners.listeners().iterator();
            ArrayList<PostInsertEventListener> tempPostInsertEventListeners = new ArrayList<PostInsertEventListener>();
            while(iter.hasNext()){
                PostInsertEventListener postInsertEventListener = iter.next();
                if(!(postInsertEventListener instanceof  HibernateEventListener)){
                    tempPostInsertEventListeners.add(postInsertEventListener);
                }
            }
            eventRegistry.setListeners(EventType.POST_COMMIT_INSERT, tempPostInsertEventListeners.toArray(new PostInsertEventListener[tempPostInsertEventListeners.size()]));
        }else{
            EventListenerGroup<PostInsertEventListener> postInsertEventListenerEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_INSERT);
            Iterator<PostInsertEventListener> iter = postInsertEventListenerEventListeners.listeners().iterator();
            ArrayList<PostInsertEventListener> tempPostInsertEventListeners = new ArrayList<PostInsertEventListener>();
            while(iter.hasNext()){
                PostInsertEventListener postInsertEventListener = iter.next();
                if(!(postInsertEventListener instanceof  HibernateEventListener)){
                    tempPostInsertEventListeners.add(postInsertEventListener);
                }
            }
            eventRegistry.setListeners(EventType.POST_INSERT, tempPostInsertEventListeners.toArray(new PostInsertEventListener[tempPostInsertEventListeners.size()]));
        }



        if(registerPostCommitListeneres){
            EventListenerGroup<PostUpdateEventListener> postUpdateEventListenerEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_COMMIT_UPDATE);
            Iterator<PostUpdateEventListener> iter = postUpdateEventListenerEventListeners.listeners().iterator();
            ArrayList<PostUpdateEventListener> tempPostUpdateEventListeners = new ArrayList<PostUpdateEventListener>();
            while(iter.hasNext()){
                PostUpdateEventListener postUpdateEventListener = iter.next();
                if(!(postUpdateEventListener instanceof  HibernateEventListener)){
                    tempPostUpdateEventListeners.add(postUpdateEventListener);
                }
            }
            eventRegistry.setListeners(EventType.POST_COMMIT_UPDATE, tempPostUpdateEventListeners.toArray(new PostUpdateEventListener[tempPostUpdateEventListeners.size()]));
        }else{
            EventListenerGroup<PostUpdateEventListener> postUpdateEventListenerEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_UPDATE);
            Iterator<PostUpdateEventListener> iter = postUpdateEventListenerEventListeners.listeners().iterator();
            ArrayList<PostUpdateEventListener> tempPostUpdateEventListeners = new ArrayList<PostUpdateEventListener>();
            while(iter.hasNext()){
                PostUpdateEventListener postUpdateEventListener = iter.next();
                if(!(postUpdateEventListener instanceof  HibernateEventListener)){
                    tempPostUpdateEventListeners.add(postUpdateEventListener);
                }
            }
            eventRegistry.setListeners(EventType.POST_UPDATE, tempPostUpdateEventListeners.toArray(new PostUpdateEventListener[tempPostUpdateEventListeners.size()]));
        }



        if(registerPostCommitListeneres){
            EventListenerGroup<PostDeleteEventListener> postRemoveEventListenerEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_COMMIT_DELETE);
            Iterator<PostDeleteEventListener> iter = postRemoveEventListenerEventListeners.listeners().iterator();
            ArrayList<PostDeleteEventListener> tempPostDeleteEventListeners = new ArrayList<PostDeleteEventListener>();
            while(iter.hasNext()){
                PostDeleteEventListener postDeleteEventListener = iter.next();
                if(!(postDeleteEventListener instanceof  HibernateEventListener)){
                    tempPostDeleteEventListeners.add(postDeleteEventListener);
                }
            }
            eventRegistry.setListeners(EventType.POST_COMMIT_DELETE, tempPostDeleteEventListeners.toArray(new PostDeleteEventListener[tempPostDeleteEventListeners.size()]));
        }else{
            EventListenerGroup<PostDeleteEventListener> postDeleteEventListenerEventListeners = eventRegistry.getEventListenerGroup(EventType.POST_DELETE);
            Iterator<PostDeleteEventListener> iter = postDeleteEventListenerEventListeners.listeners().iterator();
            ArrayList<PostDeleteEventListener> tempPostDeleteEventListeners = new ArrayList<PostDeleteEventListener>();
            while(iter.hasNext()){
                PostDeleteEventListener postDeleteEventListener = iter.next();
                if(!(postDeleteEventListener instanceof  HibernateEventListener)){
                    tempPostDeleteEventListeners.add(postDeleteEventListener);
                }
            }
            eventRegistry.setListeners(EventType.POST_DELETE, tempPostDeleteEventListeners.toArray(new PostDeleteEventListener[tempPostDeleteEventListeners.size()]));
        }
    }

    protected Object doCreateListener(HibernateGpsDevice device) {
        return new HibernateEventListener(device, marshallIds, pendingCascades, processCollection);
    }
}