/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.io;

import java.nio.channels.FileChannel;

import org.jam.fs.JavaFile;

import sun.nio.ch.FileChannelImpl;

/**
 * A <code>FileInputStream</code> obtains input bytes from a file in a file
 * system. What files are available depends on the host environment.
 *
 * <p>
 * <code>FileInputStream</code> is meant for reading streams of raw bytes such
 * as image data. For reading streams of characters, consider using
 * <code>FileReader</code>.
 *
 * @author Arthur van Hoff
 * @see java.io.File
 * @see java.io.FileDescriptor
 * @see java.io.FileOutputStream
 * @since JDK1.0
 */
public class FileInputStream extends InputStream
{
  /* File Descriptor - handle to the open file */
  private FileDescriptor fd;

  private FileChannel channel = null;

  private Object           closeLock = new Object();
  private volatile boolean closed    = false;

  private static ThreadLocal<Boolean> runningFinalize = new ThreadLocal<Boolean>();

  private JavaFile jfd;

  private static boolean isRunningFinalize()
  {
    Boolean val;
    if ((val = runningFinalize.get()) != null)
      return val.booleanValue();
    return false;
  }

  /**
   * Creates a <code>FileInputStream</code> by opening a connection to an actual
   * file, the file named by the path name <code>name</code> in the file system. A
   * new <code>FileDescriptor</code> object is created to represent this file
   * connection.
   * <p>
   * First, if there is a security manager, its <code>checkRead</code> method is
   * called with the <code>name</code> argument as its argument.
   * <p>
   * If the named file does not exist, is a directory rather than a regular file,
   * or for some other reason cannot be opened for reading then a
   * <code>FileNotFoundException</code> is thrown.
   *
   * @param name
   *          the system-dependent file name.
   * @exception FileNotFoundException
   *              if the file does not exist, is a directory rather than a regular
   *              file, or for some other reason cannot be opened for reading.
   * @exception SecurityException
   *              if a security manager exists and its <code>checkRead</code>
   *              method denies read access to the file.
   * @see java.lang.SecurityManager#checkRead(java.lang.String)
   */
  public FileInputStream(String name) throws FileNotFoundException
  {
    this(name != null ? new File(name) : null);
  }

  /**
   * Creates a <code>FileInputStream</code> by opening a connection to an actual
   * file, the file named by the <code>File</code> object <code>file</code> in the
   * file system. A new <code>FileDescriptor</code> object is created to represent
   * this file connection.
   * <p>
   * First, if there is a security manager, its <code>checkRead</code> method is
   * called with the path represented by the <code>file</code> argument as its
   * argument.
   * <p>
   * If the named file does not exist, is a directory rather than a regular file,
   * or for some other reason cannot be opened for reading then a
   * <code>FileNotFoundException</code> is thrown.
   *
   * @param file
   *          the file to be opened for reading.
   * @exception FileNotFoundException
   *              if the file does not exist, is a directory rather than a regular
   *              file, or for some other reason cannot be opened for reading.
   * @exception SecurityException
   *              if a security manager exists and its <code>checkRead</code>
   *              method denies read access to the file.
   * @see java.io.File#getPath()
   * @see java.lang.SecurityManager#checkRead(java.lang.String)
   */
  public FileInputStream(File file) throws FileNotFoundException
  {
    String name = (file != null ? file.getPath() : null);
    SecurityManager security = System.getSecurityManager();
    if (security != null)
    {
      security.checkRead(name);
    }
    if (name == null)
    {
      throw new NullPointerException();
    }
    fd = new FileDescriptor();
    fd.incrementAndGetUseCount();
    open(name);
  }

  /**
   * Creates a <code>FileInputStream</code> by using the file descriptor
   * <code>fdObj</code>, which represents an existing connection to an actual file
   * in the file system.
   * <p>
   * If there is a security manager, its <code>checkRead</code> method is called
   * with the file descriptor <code>fdObj</code> as its argument to see if it's ok
   * to read the file descriptor. If read access is denied to the file descriptor
   * a <code>SecurityException</code> is thrown.
   * <p>
   * If <code>fdObj</code> is null then a <code>NullPointerException</code> is
   * thrown.
   *
   * @param fdObj
   *          the file descriptor to be opened for reading.
   * @throws SecurityException
   *           if a security manager exists and its <code>checkRead</code> method
   *           denies read access to the file descriptor.
   * @see SecurityManager#checkRead(java.io.FileDescriptor)
   */
  public FileInputStream(FileDescriptor fdObj)
  {
    SecurityManager security = System.getSecurityManager();
    if (fdObj == null)
    {
      throw new NullPointerException();
    }
    if (security != null)
    {
      security.checkRead(fdObj);
    }
    fd = fdObj;

    /*
     * FileDescriptor is being shared by streams. Ensure that it's GC'ed only when
     * all the streams/channels are done using it.
     */
    fd.incrementAndGetUseCount();
  }

  /**
   * Opens the specified file for reading.
   * 
   * @param name
   *          the name of the file
   */
  private void open(String name) throws FileNotFoundException
  {
    jfd = new JavaFile(name);
  }

  /**
   * Reads a byte of data from this input stream. This method blocks if no input
   * is yet available.
   *
   * @return the next byte of data, or <code>-1</code> if the end of the file is
   *         reached.
   * @exception IOException
   *              if an I/O error occurs.
   */
  public int read() throws IOException
  {
    return jfd.read();
  }

  /**
   * Reads a subarray as a sequence of bytes.
   * 
   * @param b
   *          the data to be written
   * @param off
   *          the start offset in the data
   * @param len
   *          the number of bytes that are written
   * @exception IOException
   *              If an I/O error has occurred.
   */
  private int readBytes(byte b[], int off, int len) throws IOException
  {
    return jfd.readBytes(b, off, len);
  }

  /**
   * Reads up to <code>b.length</code> bytes of data from this input stream into
   * an array of bytes. This method blocks until some input is available.
   *
   * @param b
   *          the buffer into which the data is read.
   * @return the total number of bytes read into the buffer, or <code>-1</code> if
   *         there is no more data because the end of the file has been reached.
   * @exception IOException
   *              if an I/O error occurs.
   */
  public int read(byte b[]) throws IOException
  {
    return readBytes(b, 0, b.length);
  }

  /**
   * Reads up to <code>len</code> bytes of data from this input stream into an
   * array of bytes. If <code>len</code> is not zero, the method blocks until some
   * input is available; otherwise, no bytes are read and <code>0</code> is
   * returned.
   *
   * @param b
   *          the buffer into which the data is read.
   * @param off
   *          the start offset in the destination array <code>b</code>
   * @param len
   *          the maximum number of bytes read.
   * @return the total number of bytes read into the buffer, or <code>-1</code> if
   *         there is no more data because the end of the file has been reached.
   * @exception NullPointerException
   *              If <code>b</code> is <code>null</code>.
   * @exception IndexOutOfBoundsException
   *              If <code>off</code> is negative, <code>len</code> is negative,
   *              or <code>len</code> is greater than <code>b.length - off</code>
   * @exception IOException
   *              if an I/O error occurs.
   */
  public int read(byte b[], int off, int len) throws IOException
  {
    return readBytes(b, off, len);
  }

  /**
   * Skips over and discards <code>n</code> bytes of data from the input stream.
   *
   * <p>
   * The <code>skip</code> method may, for a variety of reasons, end up skipping
   * over some smaller number of bytes, possibly <code>0</code>. If <code>n</code>
   * is negative, an <code>IOException</code> is thrown, even though the
   * <code>skip</code> method of the {@link InputStream} superclass does nothing
   * in this case. The actual number of bytes skipped is returned.
   *
   * <p>
   * This method may skip more bytes than are remaining in the backing file. This
   * produces no exception and the number of bytes skipped may include some number
   * of bytes that were beyond the EOF of the backing file. Attempting to read
   * from the stream after skipping past the end will result in -1 indicating the
   * end of the file.
   *
   * @param n
   *          the number of bytes to be skipped.
   * @return the actual number of bytes skipped.
   * @exception IOException
   *              if n is negative, if the stream does not support seek, or if an
   *              I/O error occurs.
   */
  public long skip(long n) throws IOException
  {
    return jfd.skip(n);
  }

  /**
   * Returns an estimate of the number of remaining bytes that can be read (or
   * skipped over) from this input stream without blocking by the next invocation
   * of a method for this input stream. The next invocation might be the same
   * thread or another thread. A single read or skip of this many bytes will not
   * block, but may read or skip fewer bytes.
   *
   * <p>
   * In some cases, a non-blocking read (or skip) may appear to be blocked when it
   * is merely slow, for example when reading large files over slow networks.
   *
   * @return an estimate of the number of remaining bytes that can be read (or
   *         skipped over) from this input stream without blocking.
   * @exception IOException
   *              if this file input stream has been closed by calling
   *              {@code close} or an I/O error occurs.
   */
  public int available() throws IOException
  {
    return jfd.available();
  }

  /**
   * Closes this file input stream and releases any system resources associated
   * with the stream.
   *
   * <p>
   * If this stream has an associated channel then the channel is closed as well.
   *
   * @exception IOException
   *              if an I/O error occurs.
   *
   * @revised 1.4
   * @spec JSR-51
   */
  public void close() throws IOException
  {
    synchronized (closeLock)
    {
      if (closed)
      {
        return;
      }
      closed = true;
    }
    if (channel != null)
    {
      /*
       * Decrement the FD use count associated with the channel The use count is
       * incremented whenever a new channel is obtained from this stream.
       */
      fd.decrementAndGetUseCount();
      channel.close();
    }

    /*
     * Decrement the FD use count associated with this stream
     */
    int useCount = fd.decrementAndGetUseCount();

    /*
     * If FileDescriptor is still in use by another stream, the finalizer will not
     * close it.
     */
    if ((useCount <= 0) || !isRunningFinalize())
    {
      jfd.close();
    }
  }

  /**
   * Returns the <code>FileDescriptor</code> object that represents the connection
   * to the actual file in the file system being used by this
   * <code>FileInputStream</code>.
   *
   * @return the file descriptor object associated with this stream.
   * @exception IOException
   *              if an I/O error occurs.
   * @see java.io.FileDescriptor
   */
  public final FileDescriptor getFD() throws IOException
  {
    if (fd != null)
      return fd;
    throw new IOException();
  }

  /**
   * Returns the unique {@link java.nio.channels.FileChannel FileChannel} object
   * associated with this file input stream.
   *
   * <p>
   * The initial {@link java.nio.channels.FileChannel#position()
   * </code>position<code>} of the returned channel will be equal to the number of
   * bytes read from the file so far. Reading bytes from this stream will
   * increment the channel's position. Changing the channel's position, either
   * explicitly or by reading, will change this stream's file position.
   *
   * @return the file channel associated with this file input stream
   *
   * @since 1.4
   * @spec JSR-51
   */
  public FileChannel getChannel()
  {
    synchronized (this)
    {
      if (channel == null)
      {
        channel = FileChannelImpl.open(fd, true, false, this);

        /*
         * Increment fd's use count. Invoking the channel's close() method will result
         * in decrementing the use count set for the channel.
         */
        fd.incrementAndGetUseCount();
      }
      return channel;
    }
  }

  /**
   * Ensures that the <code>close</code> method of this file input stream is
   * called when there are no more references to it.
   *
   * @exception IOException
   *              if an I/O error occurs.
   * @see java.io.FileInputStream#close()
   */
  protected void finalize() throws IOException
  {
    if ((fd != null) && (fd != fd.in))
    {

      /*
       * Finalizer should not release the FileDescriptor if another stream is still
       * using it. If the user directly invokes close() then the FileDescriptor is
       * also released.
       */
      runningFinalize.set(Boolean.TRUE);
      try
      {
        close();
      } finally
      {
        runningFinalize.set(Boolean.FALSE);
      }
    }
  }
}
