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
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.internal.SessionFactoryImpl;

/**
 * @author kimchy
 */
public class DefaultHibernateEntityCollectionLifecycleInjector extends DefaultHibernateEntityLifecycleInjector {

    private Object eventListener;

    public DefaultHibernateEntityCollectionLifecycleInjector() {
        super();
    }

    public DefaultHibernateEntityCollectionLifecycleInjector(boolean registerPostCommitListeneres) {
        super(registerPostCommitListeneres);
    }

    public void injectLifecycle(SessionFactory sessionFactory, HibernateGpsDevice device) throws HibernateGpsDeviceException {
        super.injectLifecycle(sessionFactory, device);

       EventListenerRegistry eventRegistry =
                ((SessionFactoryImpl) sessionFactory).getServiceRegistry().getService(EventListenerRegistry.class);

        if (registerPostCommitListeneres) {
            return;
        }

        if (eventListener instanceof PostCollectionRecreateEventListener) {
            eventRegistry.appendListeners(EventType.POST_COLLECTION_RECREATE, (PostCollectionRecreateEventListener)eventListener);

        }

        if (eventListener instanceof PostCollectionRemoveEventListener) {
            eventRegistry.appendListeners(EventType.POST_COLLECTION_REMOVE, (PostCollectionRemoveEventListener) eventListener);
        }

        if (eventListener instanceof PostCollectionUpdateEventListener) {
            eventRegistry.appendListeners(EventType.POST_COLLECTION_UPDATE, (PostCollectionUpdateEventListener)eventListener);
        }
    }

    public void removeLifecycle(SessionFactory sessionFactory, HibernateGpsDevice device) throws HibernateGpsDeviceException {
        super.removeLifecycle(sessionFactory, device);

        if (registerPostCommitListeneres) {
            return;
        }

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


        eventListener = null;
    }

    protected Object doCreateListener(HibernateGpsDevice device) {
        eventListener = new HibernateCollectionEventListener(device, marshallIds, pendingCascades, processCollection);
        return eventListener;
    }
}
