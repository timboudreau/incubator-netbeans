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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import static org.junit.Assert.assertArrayEquals;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.masterfs.filebasedfs.channels.Lease;
import org.netbeans.modules.masterfs.filebasedfs.channels.iofunction.IOBiConsumer;
import org.netbeans.modules.masterfs.filebasedfs.channels.iofunction.IORunnable;

/**
 *
 * @author Tim Boudreau
 */
public class ChannelStreamTest extends NbTestCase {

    private Path file;
    private byte[] bytes;
    public static final String TEXT_1
            = "{ skiddoo : 23, meaningful : true,\n"
            + "meaning: '42', \n"
            + "thing: 51 }";

    public ChannelStreamTest(String name) {
        super(name);
    }

    public void testContentCorrect() throws Throwable {
        Files.write(file, TEXT_1.getBytes(UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        final InputStream is = FileObj.inputStream(file.toFile(), IORunnable.NO_OP);
        assertNotNull(is);
        InputStreamReader reader = new InputStreamReader(
                is,
                UTF_8
        );
        char[] buffer = new char[65536];
        int size = -1;
        StringBuilder sb = new StringBuilder();
        while (-1 != (size = reader.read(buffer, 0, buffer.length))) {
            sb.append(buffer, 0, size);
        }
//        char[] c = new char[TEXT_1.length() + 20];
//        int count = reader.read(c);
//        assertEquals("Wrong length read: " + new String(c, 0, count), count, TEXT_1.length());
//        String nue = new String(c, 0, count);
//        assertEquals(TEXT_1, nue);

        assertEquals(TEXT_1, sb.toString());
    }

    public void testInterruptedThreadsDontCauseSpuriousFailures() throws Throwable {
        int tc = 5;
        AtomicBoolean done = new AtomicBoolean();
        Phaser phaser = new Phaser(1);
        CountDownLatch latch = new CountDownLatch(tc);
        RC[] run = new RC[tc];
        Thread[] threads = new Thread[tc];
        for (int i = 0; i < tc; i++) {
            run[i] = new RC(phaser, done, latch);
            threads[i] = new Thread(run[i], "runner-" + i);
            threads[i].start();
        }
        phaser.arriveAndDeregister();
        long then = System.currentTimeMillis();
        long now = then;
        Random rnd = new Random(103942409);
        try {
            do {
                int tix = rnd.nextInt(tc);
                threads[tix].interrupt();
                Thread.sleep(1);
                now = System.currentTimeMillis();
            } while (now < then + 10000);
        } finally {
            done.set(true);
        }
        for (int i = 0; i < tc; i++) {
            RC rc = run[i];
            rc.rethrow();
//            System.out.println("rc " + i + " ok with " + rc.loop);
        }
        for (RC rc : run) {
            rc.rethrow();
        }
    }

    class RC implements Runnable {

        private final Phaser phaser;
        private final AtomicBoolean done;
        private final CountDownLatch latch;
        private Throwable thrown;
        private Lease lease;
        int loop;

        RC(Phaser phaser, AtomicBoolean done, CountDownLatch exitLatch) throws IOException {
            this.phaser = phaser;
            this.latch = exitLatch;
            this.done = done;
            lease = FileObj.lease(file.toFile(), true);
        }

        void rethrow() throws Throwable {
            if (thrown != null) {
                throw thrown;
            }
        }

        @Override
        @SuppressWarnings("NestedAssignment")
        public void run() {
            phaser.arriveAndAwaitAdvance();
            try {
                for (; !done.get(); loop++) {
                    try {
                        Thread.sleep(3);
                    } catch (InterruptedException ex) {
                        //
                        Thread.currentThread().interrupt();
                    }
                    try {
                        long val = lease.useAsInt(ch -> {
                            return (int) ch.size();
                        });
                        try {
                            boolean wasInt = Thread.interrupted();
                            Thread.sleep(3);
                            if (wasInt) {
                                Thread.currentThread().interrupt();
                            }
                        } catch (InterruptedException ex) {
                            // ignore
                            Thread.currentThread().interrupt();
                        }
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        try (InputStream str = FileObj.inputStream(file.toFile(), IORunnable.NO_OP)) {
                            byte[] bytes = new byte[120];
                            int count;
                            while ((count = str.read(bytes)) > 0) {
                                out.write(bytes, 0, count);
                                Thread.yield();
                            }
                        }
                        byte[] got = out.toByteArray();
                        assertArrayEquals("Bytes do not match on " + Thread.currentThread() + " size "
                                + val + " read " + got.length, bytes, got);
                    } catch (IOException ioe) {
                        if (thrown != null) {
                            thrown.addSuppressed(ioe);
                        } else {
                            thrown = ioe;
                        }
                    }
                }
            } finally {
                latch.countDown();
            }
        }
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
