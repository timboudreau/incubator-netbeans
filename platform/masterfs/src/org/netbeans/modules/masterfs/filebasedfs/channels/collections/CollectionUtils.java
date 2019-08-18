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
package org.netbeans.modules.masterfs.filebasedfs.channels.collections;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import static org.netbeans.modules.masterfs.filebasedfs.channels.collections.Checks.notNull;

/**
 *
 * @author Tim Boudreau
 */
public class CollectionUtils {

    /**
     * Create a map that, when a call to get() would return null, uses a
     * supplier to create a new value, adds it and returns that.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param valueSupplier The supplier of values
     * @return a map
     */
    public static <T, R> Map<T, R> concurrentSupplierMap(Supplier<R> valueSupplier) {
        return new SupplierMap<>(valueSupplier, new ConcurrentHashMap<>());
    }

    /**
     * Create a map that, when a call to get() would return null, uses a
     * supplier to create a new value, adds it and returns that.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param valueSupplier The supplier of values
     * @return a map
     */
    public static <T, R> Map<T, R> supplierMap(Supplier<R> valueSupplier) {
        return new SupplierMap<>(notNull("valueSupplier", valueSupplier));
    }

}
