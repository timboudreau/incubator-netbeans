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

/**
 *
 * @author Tim Boudreau
 */
public class Checks {

    public static <T> T notNull(String name, T obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Null " + name);
        }
        return obj;
    }

    public static long greaterThanZero(String name, long val) {
        if (val <= 0) {
            throw new IllegalArgumentException("Name must be > 0: " + val);
        }
        return val;
    }

    public static int greaterThanZero(String name, int val) {
        if (val <= 0) {
            throw new IllegalArgumentException("Name must be > 0: " + val);
        }
        return val;
    }

    public static long nonNegative(String name, long val) {
        if (val < 0) {
            throw new IllegalArgumentException("Name must be > 0: " + val);
        }
        return val;
    }

    public static int nonNegative(String name, int val) {
        if (val < 0) {
            throw new IllegalArgumentException("Name must be > 0: " + val);
        }
        return val;
    }
}
