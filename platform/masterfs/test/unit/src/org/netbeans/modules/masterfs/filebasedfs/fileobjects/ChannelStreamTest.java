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
package org.netbeans.modules.masterfs.filebasedfs.fileobjects;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Assert;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.masterfs.filebasedfs.channels.iofunction.IOBiConsumer;
import org.netbeans.modules.masterfs.filebasedfs.channels.iofunction.IORunnable;

/**
 *
 * @author Tim Boudreau
 */
public class ChannelStreamTest extends NbTestCase {

    private Path file;
    private byte[] bytes;

    public ChannelStreamTest(String name) {
        super(name);
    }

    public void testSimpleRead() throws Exception {
        ByteArrayInputStream control = new ByteArrayInputStream(bytes);
        InputStream test = FileObj.inputStream(file.toFile(), IORunnable.NO_OP);

        for (int i = 0;; i++) {
            int a = control.read();
            int b = test.read();
            assertEquals("Mismatch at byte " + i, a, b);
            if (a == -1) {
                break;
            }
        }
    }

    public void testStreams() throws Exception {
        StreamChewer chewer = new StreamChewer();
        byte[] cbuf = new byte[128];
        byte[] tbuf = new byte[128];

        IOBiConsumer<InputStream, InputStream> readAndCompare = (ctrl, test) -> {
            int cread = ctrl.read(cbuf);
            int tread = test.read(tbuf);
            assertEquals(cread, tread);
            Assert.assertArrayEquals(cbuf, tbuf);
        };

        IOBiConsumer<InputStream, InputStream> readOne = (ctrl, test) -> {
            int aCtrl = ctrl.available();
            int aTest = test.available();
            assertEquals(aCtrl, aTest);

            int cOneByte = ctrl.read();
            int tOneByte = test.read();
            assertEquals(cOneByte, tOneByte);
        };

        IOBiConsumer<InputStream, InputStream> skipAndTest = (ctrl, test) -> {
            ctrl.skip(53);
            test.skip(53);
            int a1 = ctrl.available();
            int a2 = test.available();
            assertEquals(a1, a2);
        };

        chewer.add(readAndCompare).add(readOne)
                .add((ctrl, test) -> {
                    assertTrue(ctrl.markSupported());
                    assertTrue(test.markSupported());
                    ctrl.mark(132);
                    test.mark(132);
                    int a1 = ctrl.available();
                    int a2 = test.available();
                    readAndCompare.accept(ctrl, test);
                    assertEquals(a1, a2);
                    ctrl.reset();
                    test.reset();
                    byte[] old = Arrays.copyOf(cbuf, cbuf.length);
                    int a1b = ctrl.available();
                    int a2b = test.available();
                    assertEquals(a1, a1b);
                    assertEquals(a2, a2b);
                }).add(readAndCompare)
                .add(skipAndTest)
                .add(readAndCompare)
                .add((ctrl, test) -> {
                    test.close();
                    ctrl.close();
                });

        ByteArrayInputStream control = new ByteArrayInputStream(bytes);
        InputStream test = FileObj.inputStream(file.toFile(), IORunnable.NO_OP);

        chewer.test(control, test);
    }

    static final class StreamChewer {

        private final List<IOBiConsumer<? super InputStream, ? super InputStream>> all = new ArrayList<>();

        StreamChewer add(IOBiConsumer<? super InputStream, ? super InputStream> c) {
            all.add(c);
            return this;
        }

        void test(InputStream control, InputStream test) throws IOException {
            for (IOBiConsumer<? super InputStream, ? super InputStream> c : all) {
                c.accept(control, test);
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (file != null && Files.exists(file)) {
            Files.delete(file);
        }
    }

    @Override
    protected void setUp() throws Exception {
        file = newFile();
        bytes = new byte[4096];
        ThreadLocalRandom.current().nextBytes(bytes);
        Files.write(file, bytes, StandardOpenOption.WRITE);
    }

    static Path newFile() throws IOException {
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        assertTrue(Files.exists(tmp));
        String nm = ChannelStreamTest.class.getSimpleName().toLowerCase()
                + "-" + Long.toString(System.currentTimeMillis(), 36);
        for (int i = 0;; i++) {
            Path create = tmp.resolve(nm + "_" + i);
            try {
                Files.createFile(create);
            } catch (FileAlreadyExistsException ex) {
                // runnning concurrently?
            }
            return create;
        }
    }

}
