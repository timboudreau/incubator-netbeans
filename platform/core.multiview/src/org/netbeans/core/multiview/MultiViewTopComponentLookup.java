/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.netbeans.core.multiview;

import javax.swing.Action;
import org.openide.util.Lookup;
import java.util.*;
import javax.swing.ActionMap;

import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

class MultiViewTopComponentLookup extends Lookup {

    private final ProxyLookup proxy;
    private final ProxyLookup initial;
    private final ProxyLookup.Controller proxyController;
    private final ProxyLookup.Controller initialController;
    private final ActionMap initialActionMap;
    private volatile boolean isProxyLookupInitialized;
    public MultiViewTopComponentLookup(ActionMap initialActionMap) {
        super();
        this.initialActionMap = initialActionMap;
        initialController = new ProxyLookup.Controller();
        proxyController = new ProxyLookup.Controller();
        // need to delegate in order to get the correct Lookup.Templates that refresh..
        initial = new ProxyLookup(initialController);
        refreshInitialProxyLookup();
        proxy = new ProxyLookup(proxyController);
        proxyController.setLookups(initial);
    }

    private void refreshInitialProxyLookup() {
        initialController.setLookups(Lookups.fixed(new LookupProxyActionMap(initialActionMap)));
    }
    
    public void setElementLookup(Lookup look) {
        proxyController.setLookups(initial, look);
        refreshInitialProxyLookup();
        isProxyLookupInitialized = true;
    }
    
    @Override
    public <T> Lookup.Item<T> lookupItem(Lookup.Template<T> template) {
        Lookup.Item<T> retValue;
        if (template.getType() == ActionMap.class || (template.getId() != null && template.getId().equals("javax.swing.ActionMap"))) {
            return initial.lookupItem(template);
        }
        // do something here??
        retValue = super.lookupItem(template);
        return retValue;
    }    
    
     
    public <T> T lookup(Class<T> clazz) {
        if (clazz == ActionMap.class) {
            return initial.lookup(clazz);
        }
        return proxy.lookup(clazz);
    }
    
    public <T> Lookup.Result<T> lookup(Lookup.Template<T> template) {
        
        if (template.getType() == ActionMap.class || (template.getId() != null && template.getId().equals("javax.swing.ActionMap"))) {
            return initial.lookup(template);
        }
        Lookup.Result<T> retValue;
        retValue = proxy.lookup(template);
        retValue = new ExclusionResult(retValue);
        return retValue;
    }

    boolean isInitialized() {
        return isProxyLookupInitialized;
    }
    
    /**
     * A lookup result excluding some instances.
     */
    private static final class ExclusionResult<T> extends Lookup.Result<T> implements LookupListener {
        
        private final Lookup.Result<T> delegate;
        private final List<LookupListener> listeners = new ArrayList<>();
        private Collection<? extends T> lastResults;
        
        public ExclusionResult(Lookup.Result<T> delegate) {
            this.delegate = delegate;
        }
        
        public Collection<? extends T> allInstances() {
            // this shall remove duplicates??
            Set s = new HashSet(delegate.allInstances());
            return s;
        }
        
        public Set<Class<? extends T>> allClasses() {
            return delegate.allClasses(); // close enough
        }
        
        public Collection<Lookup.Item<T>> allItems() {
            // remove duplicates..
            Set<Item<T>> s = new HashSet<>(delegate.allItems());
            Iterator it = s.iterator();
            Set instances = new HashSet();
            while (it.hasNext()) {
                Lookup.Item i = (Lookup.Item)it.next();
                if (instances.contains(i.getInstance())) {
                    it.remove();
                } else {
                    instances.add(i.getInstance());
                }
            }
            return s;
        }
        
        public void addLookupListener(LookupListener l) {
            synchronized (listeners) {
                if (listeners.isEmpty()) {
                    if (lastResults == null) {
                        lastResults = allInstances();
                    }
                    delegate.addLookupListener(this);
                }
                listeners.add(l);
            }
        }
        
        public void removeLookupListener(LookupListener l) {
            synchronized (listeners) {
                listeners.remove(l);
                if (listeners.isEmpty()) {
                    delegate.removeLookupListener(this);
                    lastResults = null;
                }
            }
        }
        
        public void resultChanged(LookupEvent ev) {
            synchronized (listeners) {
                Collection<? extends T> current = allInstances();
                boolean equal = lastResults != null && current != null && current.containsAll(lastResults) && lastResults.containsAll(current);
                if (equal) {
                    // the merged list is the same, ignore...
                    return ;
                }
                lastResults = current;
            }
                
            LookupEvent ev2 = new LookupEvent(this);
            LookupListener[] ls;
            synchronized (listeners) {
                ls = (LookupListener[])listeners.toArray(new LookupListener[listeners.size()]);
            }
            for (int i = 0; i < ls.length; i++) {
                ls[i].resultChanged(ev2);
            }
        }
        
    }
    
    /**
     * A proxy ActionMap that delegates to the original one, used because of #47991
     * non private because of tests..
     */
    static class LookupProxyActionMap extends ActionMap  {
        private ActionMap map;
        public LookupProxyActionMap(ActionMap original) {
            map = original;
        }
        
        @Override
        public void setParent(ActionMap map) {
            this.map.setParent(map);
        }
        
        
        @Override
        public ActionMap getParent() {
            return map.getParent();
        }
        
        @Override
        public void put(Object key, Action action) {
            map.put(key, action);
        }
        
        @Override
        public Action get(Object key) {
            return map.get(key);
        }
        
        @Override
        public void remove(Object key) {
            map.remove(key);
        }
        
        @Override
        public void clear() {
            map.clear();
        }
        
        @Override
        public Object[] keys() {
            return map.keys();
        }
        
        @Override
        public int size() {
            return map.size();
        }
        
        @Override
        public Object[] allKeys() {
            return map.allKeys();
        }
        
    }
}
