/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.netbeans.modules.masterfs.filebasedfs.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.netbeans.modules.masterfs.filebasedfs.channels.iofunction.IOIntSupplier;
import org.netbeans.modules.masterfs.filebasedfs.channels.iofunction.IOLongSupplier;

/**
 * A wrapper for FileChannels which prevents clients from closing them, which
 * they should not do with pooled channels.
 *
 * @author Tim Boudreau
 */
final class FileChannelWrapper extends FileChannel {

    private final FileChannel channel;
    private long safePosition;

    FileChannelWrapper(FileChannel channel) {
        this.channel = channel;
    }

    static FileChannel unwrap(FileChannel ch) {
        if (ch instanceof FileChannelWrapper) {
            return ((FileChannelWrapper) ch).channel;
        }
        return ch;
    }

    int updatingPositionInt(IOIntSupplier supp) throws IOException {
        int add = supp.getAsInt();
        if (add > 0) {
            synchronized (this) {
                safePosition += add;
            }
        }
        return add;
    }

    long updatingPosition(IOLongSupplier supp) throws IOException {
        long add = supp.getAsLong();
        if (add > 0) {
            synchronized (this) {
                safePosition += add;
            }
        }
        return add;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            return updatingPositionInt(() -> channel.read(dst));
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            return updatingPosition(() -> channel.read(dsts, offset, length));
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }

    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            return updatingPositionInt(() -> channel.write(src));
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            return updatingPosition(() -> channel.write(srcs, offset, length));
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public long position() throws IOException {
//        boolean wasInterrupted = Thread.interrupted();
//        try {
        synchronized (this) {
            return safePosition;
        }
//            return channel.position();
//        } finally {
//            if (wasInterrupted) {
//                Thread.currentThread().interrupt();
//            }
//        }
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            channel.position(newPosition);
            synchronized (this) {
                safePosition = newPosition;
            }
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
        return this;
    }

    @Override
    public long size() throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            return channel.size();
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            channel.truncate(size);
            synchronized (this) {
                safePosition = channel.position();
            }
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
        return this;
    }

    @Override
    public void force(boolean metadata) throws IOException {
        channel.force(metadata);
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            return channel.transferTo(position, count, target);
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            return channel.transferFrom(src, position, count);
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            return updatingPositionInt(() -> channel.read(dst, position));
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        boolean wasInterrupted = Thread.interrupted();
        try {
            return updatingPositionInt(() -> channel.write(src, position));
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        return channel.map(mode, position, size);
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return channel.lock(position, size, shared);
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return channel.tryLock(position, size, shared);
    }

    @Override
    protected void implCloseChannel() throws IOException {
        throw new IOException("This channel is managed by a FileChannelPool - do "
                + "not close it directly");
    }

    void closeUnderlying() throws IOException {
        channel.close();
    }
}
