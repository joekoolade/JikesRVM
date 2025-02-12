/**
 * Created on Dec 27, 2017
 *
 * Copyright (C) Joe Kulig, 2017 All rights reserved.
 */
package org.jam.net;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.NoRouteToHostException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;

import org.jam.driver.net.Packet;
import org.jikesrvm.VM;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Offset;

/**
 * @author Joe Kulig
 *
 */
/**
 * @author Joe Kulig
 *
 */
public class Udp {
    private static final Offset DESTINATION_PORT = Offset.fromIntSignExtend(2);
    private static final Offset LENGTH = Offset.fromIntSignExtend(4);
    private static final Offset CHECKSUM = Offset.fromIntSignExtend(6);
    static final int UDP_HEADER_SIZE = 8;
    
    private static final boolean DEBUG_PSEUDOHEADER = true;
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_TRACE = true;
    private static InetConnections connectionTable = new InetConnections();
    
    InetSocketAddress localAddress;
    InetSocketAddress remoteAddress;
    int ttl;
    private Connection connection;
    private int pseudoHeaderSum;
    private int offset;
    private InetPacket packet;
    private Ip ip;
    private short packetChecksum;
    private boolean disableCheckSum = false;
    private boolean pseudoSum = false;
    private static UdpStats stats;
    
    private ArrayDeque<Packet> packetFifo;
    
    public Udp()
    {
        ip = new Ip();
        if(stats == null) stats = new UdpStats();
        packetFifo = new ArrayDeque<Packet>();
    }

    /**
     * @param inetSocketAddress
     */
    public void bind(InetSocketAddress inetSocketAddress) throws SocketException, IOException
    {
        localAddress = inetSocketAddress;
        // put it in the connection table
        connectionTable.add(inetSocketAddress, this);
    }

    /**
     * @param inetSocketAddress
     * @param i
     */
    public void connect(InetSocketAddress inetSocketAddress, int i)
    {
        remoteAddress = inetSocketAddress;
        if (localAddress == null)
        {
            localAddress = new InetSocketAddress(10000);
        }
        connectionTable.add(localAddress, this);
        // check if address is routable
        // Create a new connection
    }

    /**
     * @param ttl
     */
    public void setTimeToLive(int ttl)
    {
        this.ttl = ttl;
    }

    /**
     * @return
     */
    public int getTimeToLive()
    {
        return ttl;
    }

    /**
     * @return
     */
    public InetSocketAddress getLocalAddress() throws IOException
    {
        return localAddress;
    }

    /**
     * @param packet
     * @throws IOException
     */
    public void send(DatagramPacket packet) throws IOException
    {
        /*
         * Get a new connection
         */
        if(DEBUG_TRACE) VM.sysWriteln("udp send0");
        if (connection == null)
        {
            initConnection(packet);
        }
        if(DEBUG_TRACE) VM.sysWriteln("udp length: " + packet.getLength());
        if (packet.getLength() > 0xFFFF)
        {
            throw new IOException("Packet too big");
        }
        if(DEBUG_TRACE) VM.sysWriteln("get new inet packet");
        this.packet = new InetPacket(packet, connection);
        this.packet.setHeadroom(UDP_HEADER_SIZE);
        if(DEBUG_TRACE) VM.sysWriteln("New inet packet");
        send();
    }

    private void send()
    {
        Address udpPacket = packet.getPacketAddress();
        // Setup the udp packet header
        // source port
        udpPacket.store(ByteOrder.hostToNetwork((short) localAddress.getPort()));
        // desination port
        udpPacket.store(ByteOrder.hostToNetwork((short) remoteAddress.getPort()), DESTINATION_PORT);
        // packet length
        udpPacket.store(ByteOrder.hostToNetwork((short) packet.getSize()), LENGTH);
        // packet checksum
        udpPacket.store((short)0, CHECKSUM);
        computeChecksum();
        udpPacket.store(ByteOrder.hostToNetwork(packetChecksum), CHECKSUM);
        // send it on for IP processing
        if(DEBUG_TRACE) System.out.println("private send "+packet.getOffset()+" "+packet.getSize());
        packet.setHeadroom(20);
        VM.hexDump(packet.getArray(),0,packet.getBufferSize());
        ip.send(packet);
    }

    static final void receive(InetPacket packet, int sourceAddress, int destinationAddress)
    {
        if(DEBUG_TRACE) System.out.println("udp.receive "+Integer.toHexString(sourceAddress));
        Address udpHeader = packet.getPacketAddress();
        int destinationPort = ByteOrder.networkToHost(udpHeader.loadShort(DESTINATION_PORT));
        
        if(DEBUG_TRACE) System.out.println("udp.rx "+Integer.toHexString(destinationAddress)+" "+destinationPort);
        Udp udp = connectionTable.find(destinationAddress, destinationPort);
        if(udp == null)
        {
            stats.noPort();
            return;
        }
        
        udp.packet(packet);
        udp.receive(sourceAddress, destinationAddress);
    }
    
    private final void receive(int sourceAddress, int destinationAddress)
    {
        if(DEBUG_TRACE) System.out.println("udp.receive2");
        Address udpHeader = packet.getPacketAddress();
        int ulen = ByteOrder.networkToHost(udpHeader.loadShort(LENGTH));
        if(DEBUG_TRACE) System.out.println("udp.receive2 "+ulen);
        /*
         * Look for a short packet
         */
        if(ulen > packet.getSize())
        {        
            if(DEBUG_TRACE) System.out.println("udp.receive2 "+packet.getSize());
            stats.inError();
            return;
        }
        int csum = computePseudoHeaderSum2(sourceAddress, destinationAddress, ulen);
        if(!verifyChecksum(csum))
        {
            stats.inError();
            return;
        }
        put(packet);
    }
    
    /**
     * Puts a packet the end of the UDP buffer fifo
     * @param packet UDP rx packet
     */
    public final synchronized void put(Packet packet)
    {
        if(DEBUG_TRACE) System.out.println("udp.put");
        packetFifo.add(packet);
        if(DEBUG_TRACE) System.out.println("udp.put notify");
        notify();
    }
    
    /**
     * Retrieves a packet at the head of the fifo
     * @return a packet
     */
    public final synchronized Packet get()
    {
        return packetFifo.poll();
    }
    
    public final synchronized boolean hasPacket()
    {
        return !packetFifo.isEmpty();
    }
    /**
     * Sets packet field to received packet
     * @param packet
     */
    private final void packet(InetPacket packet)
    {
        this.packet = packet;
    }
    private void initConnection(DatagramPacket packet) throws NoRouteToHostException
    {
        if(DEBUG_TRACE) VM.sysWriteln("udp initConnection");
        if(packet != null)
        {
            if(remoteAddress==null)
            {
                remoteAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
            }
        }
        connection = new Connection(localAddress, remoteAddress, IpProto.UDP);
    }

    /**
     * The is the 16 bit sum of the source, destination, and protocol values
     */
    private void computePseudoHeaderSum()
    {
        // Sum up the source IP address
        int val = connection.getLocalInet();
        pseudoHeaderSum = ((val>>16) & 0xFFFF) + (val & 0xFFFF);
        // Add in the destination IP address
        val = connection.getRemoteInet();
        pseudoHeaderSum += ((val>>16) & 0xFFFF) + (val & 0xFFFF);
        // Add in the protocol
        pseudoHeaderSum += IpProto.UDP.protocol();
        // add in the size
        pseudoHeaderSum += packet.getSize();
        // Add the carry over
        val = (pseudoHeaderSum >> 16) + (pseudoHeaderSum & 0xFFFF);
        pseudoHeaderSum = val;
        if (DEBUG_PSEUDOHEADER) System.out.println("computePseudoHeaderSum "+Integer.toHexString(pseudoHeaderSum));
    }

    /**
     * This one is called from the udp receive
     * @param saddr
     * @param daddr
     * @param len
     */
    private int computePseudoHeaderSum2(int saddr, int daddr, int len)
    {
        if(DEBUG_TRACE) System.out.println("udp.pseduo2");
        long csum = 0;
        csum = ((long)saddr & 0xFFFFFFFFL)  + ((long)daddr & 0xFFFFFFFFL) + len + IpProto.UDP.protocol();
        if(DEBUG_PSEUDOHEADER) System.out.println("pseudo2 "+Long.toHexString(csum));
        // add 32 bit carry overs
        //csum = (csum & 0xFFFF) + (csum>>16);
        csum = (csum>>32) + (csum & 0xFFFFFFFFL);
        // add the 16 bit carry overs
        csum = (csum & 0xFFFF) + (csum>>16);
        if(DEBUG_PSEUDOHEADER) System.out.println("pseudo2 "+Integer.toHexString((int)csum));
        return (int)csum;
    }
    
    private boolean verifyChecksum(int pseudoSum)
    {
        int csum = pseudoSum;
        
        if(disableCheckSum)
        {
            return true;
        }
        Address data = packet.getPacketAddress();
        if(DEBUG_TRACE) System.out.println("udp.verifychecksum "+packet.getSize()+ " "+Integer.toHexString(data.toInt()));
        for (int words = packet.getSize() >> 1; words > 0; words--)
        {
            int val = ByteOrder.networkToHost(data.loadShort());
            csum += (val & 0xFFFF);
            data = data.plus(2);
        }
        if ((packet.getSize() & 0x1) != 0)
        {
            csum += (ByteOrder.networkToHost(data.loadShort()) & 0xFF00);
        }
        // Add the carry overs
        if(DEBUG_TRACE) System.out.println("udp.verifyChecksum# "+ Integer.toHexString(csum));
        csum = (csum >> 16) + (csum & 0xFFFF);
        if(DEBUG_TRACE) System.out.println("udp.verifyChecksum##  "+ Integer.toHexString(csum));
        return ~(short)csum==0;
    }
    private void computeChecksum()
    {
        int csum = 0;

        if(disableCheckSum)
        {
            packetChecksum = 0;
            return;
        }
        if(!pseudoSum)
        {
            computePseudoHeaderSum();
            pseudoSum = true;
        }
        Address data = packet.getPacketAddress();
        csum = pseudoHeaderSum;
        for (int words = packet.getSize() >> 1; words > 0; words--)
        {
            int val = ByteOrder.hostToNetwork(data.loadShort());
            csum += (val & 0xFFFF);
            data = data.plus(2);
        }
        if ((packet.getSize() & 0x1) != 0)
        {
            csum += (ByteOrder.hostToNetwork(data.loadShort()) & 0xFF00);
        }
        // Add the carry overs
        csum = (csum >> 16) + (csum & 0xFFFF);
        packetChecksum = (short) ~csum;
    }

    /**
     * @param packet
     * @return
     */
    public SocketAddress receive(DatagramPacket packet) throws SocketTimeoutException, InterruptedIOException
    {
        Packet p;
        int waiting=0;
        while((p=get())==null)
        { 
            waiting++;
            synchronized(this)
            {
                try
                {
                    this.wait();
                } catch (IllegalMonitorStateException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        System.out.println("udp.receive3 "+waiting);
        packet.setLength(p.getSize()-UDP_HEADER_SIZE);
        System.arraycopy(p.getArray(), p.getOffset()+UDP_HEADER_SIZE, packet.getData(), packet.getOffset(), p.getSize()-UDP_HEADER_SIZE);
        return remoteAddress;
    }

    /**
     * @param optionId
     * @param value
     */
    public void setMulticastInterface(int optionId, InetAddress value)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @param optionId
     * @param value
     */
    public void setOption(int optionId, Object value)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @param optionId
     * @return
     */
    public Object getMulticastInterface(int optionId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param optionId
     * @return
     */
    public Object getOption(int optionId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     */
    public void close() throws IOException
    {
        // TODO Auto-generated method stub

    }

    /**
     * @param addr
     */
    public void join(InetAddress addr)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @param addr
     */
    public void leave(InetAddress addr)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @param address
     * @param netIf
     */
    public void joinGroup(InetSocketAddress address, NetworkInterface netIf)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @param address
     * @param netIf
     */
    public void leaveGroup(InetSocketAddress address, NetworkInterface netIf)
    {
        // TODO Auto-generated method stub

    }

    /**
     * 
     */
    public void shutdownInput()
    {
        // TODO Auto-generated method stub

    }

    /**
     * 
     */
    public void shutdownOutput()
    {
        // TODO Auto-generated method stub

    }

    /**
     * @param data
     */
    public void sendUrgentData(int data)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @param queuelen
     */
    public void listen(int queuelen)
    {
        // TODO Auto-generated method stub

    }

    public void setBlocking(boolean blocking)
    {
        // TODO Auto-generated method stub

    }

    public void disconnect()
    {
        // TODO Auto-generated method stub

    }

    public InetSocketAddress getPeerAddress()
    {
        // TODO Auto-generated method stub
        return remoteAddress;
    }

    public int write(ByteBuffer src) throws IOException
    {
        VM.sysWriteln("udp write");
//        disableCheckSum = true;
        if (connection == null)
        {
            try
            {
                initConnection(null);
            }
            catch (NoRouteToHostException e)
            {
                throw new IOException("No Route");
            }
        }
        if (src.capacity() > 0xFFFF)
        {
            throw new IOException("Packet too big");
        }
        packet = new InetPacket(src, connection, UDP_HEADER_SIZE);
        send();
        return 0;
    }

    public long writeGathering(ByteBuffer[] srcs, int offset2, int length2)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public int read(ByteBuffer dst)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public long readScattering(ByteBuffer[] dsts, int offset2, int length2)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public SocketAddress receive(ByteBuffer dst)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int send(ByteBuffer src, InetSocketAddress dst)
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
