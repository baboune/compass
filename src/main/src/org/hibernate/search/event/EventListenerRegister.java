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

package org.hibernate.search.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.compass.gps.device.hibernate.embedded.CompassEventListener;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;

/**
 * Allows to hack automatic support for Compass in Hibernate when used with Hiberante annotations.
 *
 * @author kimchy
 */
public class EventListenerRegister {

    /**
     * Add the FullTextIndexEventListener to all listeners, if enabled in configuration
     * and if not already registered.
     *
     * @param eventRegister
     * @param properties the Search configuration
     */
    public static void enableHibernateSearch(EventListenerRegistry eventRegister, Properties properties) {
        boolean foundCompass = false;
        for (Map.Entry entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith(CompassEventListener.COMPASS_PREFIX) || key.startsWith(CompassEventListener.COMPASS_GPS_INDEX_PREFIX)) {
                foundCompass = true;
                break;
            }
        }
        if (foundCompass) {
            CompassEventListener.log.debug("Found Compass settings, enabling Compass listener");
        } else {
            CompassEventListener.log.debug("No Compass properties found, disabling Compass listener");
            return;
        }
        final CompassEventListener searchListener = new CompassEventListener();
        // PostInsertEventListener
        List<PostInsertEventListener> postInsertEventListeners = addIfNeeded(
                eventRegister.getEventListenerGroup(EventType.POST_INSERT),
                searchListener,
                new PostInsertEventListener[]{searchListener}
        );
        eventRegister.setListeners(EventType.POST_INSERT,
                postInsertEventListeners .toArray(new PostInsertEventListener[postInsertEventListeners .size()])

        );
        // PostUpdateEventListener
        List<PostUpdateEventListener> postUpdateEventListeners = addIfNeeded(
                eventRegister.getEventListenerGroup(EventType.POST_UPDATE),
                searchListener,
                new PostUpdateEventListener[]{searchListener}
        );
        eventRegister.setListeners(EventType.POST_UPDATE,
                postUpdateEventListeners .toArray(new PostUpdateEventListener[postUpdateEventListeners .size()])

        );
        // PostDeleteEventListener
        List<PostDeleteEventListener> postDeleteEventListeners = addIfNeeded(
                eventRegister.getEventListenerGroup(EventType.POST_DELETE),
                searchListener,
                new PostDeleteEventListener[]{searchListener}
        );
        eventRegister.setListeners(EventType.POST_DELETE,
                postDeleteEventListeners .toArray(new PostDeleteEventListener[postDeleteEventListeners .size()])

        );

        // PostCollectionRecreateEventListener
        List<PostCollectionRecreateEventListener> postCollectionRecreateEventListeners = addIfNeeded(
                eventRegister.getEventListenerGroup(EventType.POST_COLLECTION_RECREATE),
                searchListener,
                new PostCollectionRecreateEventListener[]{searchListener}
        );
        eventRegister.setListeners(EventType.POST_COLLECTION_RECREATE,
                postCollectionRecreateEventListeners.toArray(new PostCollectionRecreateEventListener[postCollectionRecreateEventListeners.size()])

        );
        // PostCollectionRemoveEventListener
        List<PostCollectionRemoveEventListener> postCollectionRemoveEventListeners = addIfNeeded(
                eventRegister.getEventListenerGroup(EventType.POST_COLLECTION_REMOVE),
                searchListener,
                new PostCollectionRemoveEventListener[]{searchListener}
        );
        eventRegister.setListeners(EventType.POST_COLLECTION_REMOVE,
                postCollectionRemoveEventListeners.toArray(new PostCollectionRemoveEventListener[postCollectionRemoveEventListeners.size()])

        );
        // PostCollectionUpdateEventListener
        List<PostCollectionUpdateEventListener> postCollectionUpdateEventListeners = addIfNeeded(
                eventRegister.getEventListenerGroup(EventType.POST_COLLECTION_UPDATE),
                searchListener,
                new PostCollectionUpdateEventListener[]{searchListener}
        );
        eventRegister.setListeners(EventType.POST_COLLECTION_UPDATE,
                postCollectionUpdateEventListeners.toArray(new PostCollectionUpdateEventListener[postCollectionUpdateEventListeners.size()])

        );

    }

    /**
     * Verifies if a Search listener is already present; if not it will return
     * a grown address adding the listener to it.
     *
     * @param <T>                 the type of listeners
     * @param listeners
     * @param searchEventListener
     * @param toUseOnNull         this is returned if listeners==null
     * @return
     */
    private static <T> List<T> addIfNeeded(EventListenerGroup<T> listeners, T searchEventListener, T[] toUseOnNull) {


        if (listeners == null||listeners.isEmpty()) {
            List<T> arrayUseOnNull = new ArrayList<T>();
            return Arrays.asList(toUseOnNull);
        }
        List<T> listenersArray = new ArrayList<T>();
        Iterator<T> iterator =listeners.listeners().iterator();
        while(iterator.hasNext()){
            listenersArray.add(iterator.next());
        }

        if (!isPresentInListeners(listenersArray)) {
            return appendToArray(listenersArray, searchEventListener);
        } else {
            return listenersArray;
        }
    }

    /**
     * Will add one element to the end of an array.
     *
     * @param <T>        The array type
     * @param listeners  The original array
     * @param newElement The element to be added
     * @return A new array containing all listeners and newElement.
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> appendToArray(List<T> listeners, T newElement) {
        listeners.add(newElement);
        return listeners;
    }

    /**
     * Verifies if a FullTextIndexEventListener is contained in the array.
     *
     * @param listeners
     * @return true if it is contained in.
     */
    @SuppressWarnings("deprecation")
    private static boolean isPresentInListeners(List listeners) {
        for (Object eventListener : listeners) {
            if (FullTextIndexEventListener.class == eventListener.getClass()) {
                return true;
            }
            if (FullTextIndexCollectionEventListener.class == eventListener.getClass()) {
                return true;
            }
        }
        return false;
    }
}
