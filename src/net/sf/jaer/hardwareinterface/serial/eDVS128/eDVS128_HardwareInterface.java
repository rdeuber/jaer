/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.jaer.hardwareinterface.serial.eDVS128;

import java.io.*;
import java.beans.*;
import java.util.Observable;
import java.util.logging.Logger;
import java.util.prefs.*;

import net.sf.jaer.aemonitor.*;
import net.sf.jaer.chip.*;
import net.sf.jaer.biasgen.*;
import net.sf.jaer.hardwareinterface.*;
import net.sf.jaer.hardwareinterface.serial.*;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.Math.*;
import java.util.ArrayList;
import java.util.Observer;

import ch.unizh.ini.jaer.chip.retina.DVS128;

/**
 * Interface to eDVS128 camera.
 * 
 * This camera uses 4Mbaud 8 bits 1 stop no parity with RTS/CTS.
 * 
 * The camera returns the following help
 * <pre>
 * DVS128 - LPC2106/01 Interface Board, V6.0: Apr 25 2011, 13:09:50
System Clock: 64MHz / 64 -> 1000ns event time resolution
Modules: 

Supported Commands:

E+/-       - enable/disable event sending
!Ex        - specify event data format, ??E to list options

B          - send bias settings to DVS
!Bx=y      - set bias register x[0..11] to value y[0..0xFFFFFF]
?Bx        - get bias register x current value

!R+/-      - transmit event rate on/off
0,1,2      - LED off/on/blinking
!S=x       - set baudrate to x

R          - reset board
P          - enter reprogramming mode

??         - display help

??E
!E0   - 2 bytes per event binary 0yyyyyyy.pxxxxxxx (default)
!E1   - 4 bytes per event (as above followed by 16bit timestamp)

!E10  - 3 bytes per event, 6bit encoded
!E11  - 6 bytes per event+timestamp, 6bit encoded 
!E12  - 4 bytes per event, 6bit encoded; new-line
!E13  - 7 bytes per event+timestamp, 6bit encoded; new-line

!E20  - 4 bytes per event, hex encoded
!E21  - 8 bytes per event+timestamp, hex encoded 
!E22  - 5 bytes per event, hex encoded; new-line
!E23  - 8 bytes per event+timestamp, hex encoded; new-line

!E30  - 10 bytes per event, ASCII <1p> <3y> <3x>; new-line
!E31  - 10 bytes per event+timestamp, ASCII <1p> <3y> <3x> <5ts>; new-line
 * </pre>
 * 
 * @author lou
 */
public class eDVS128_HardwareInterface implements SerialInterface, HardwareInterface, AEMonitorInterface, BiasgenHardwareInterface, Observer {

    public static final int BAUD_RATE = 4000000;
    protected static Preferences prefs = Preferences.userNodeForPackage(eDVS128_HardwareInterface.class);
    public PropertyChangeSupport support = new PropertyChangeSupport(this);
    protected Logger log = Logger.getLogger("eDVS128");
    protected AEChip chip;
    /** Timestamp tick on eDVS in us - this is default value */
    public final int TICK_US = 1; 
    protected AEPacketRaw lastEventsAcquired = new AEPacketRaw();
    public static final int AE_BUFFER_SIZE = 100000; // should handle 5Meps at 30FPS
    protected int aeBufferSize = prefs.getInt("eDVS128.aeBufferSize", AE_BUFFER_SIZE);
    protected int interfaceNumber = 0;
    public static boolean isOpen = false;
    public static boolean eventAcquisitionEnabled = false;
    public static boolean overrunOccuredFlag = false;
    protected byte cHighBitMask = (byte) 0x80;
    protected byte cLowerBitsMask = (byte) 0x7F;
    int eventCounter = 0;
    int bCalibrated = 0;
    protected String devicName;
    protected InputStream retina;
    protected OutputStream retinaVendor;
    public final PropertyChangeEvent NEW_EVENTS_PROPERTY_CHANGE = new PropertyChangeEvent(this, "NewEvents", null, null);
    protected AEPacketRawPool aePacketRawPool = new AEPacketRawPool();
    private int lastshortts = 0;
    private final int NUM_BIASES = 12; // number of biases, to renumber biases for bias command
    private boolean DEBUG = false;

    //AEUnicastInput input = null;
    //InetSocketAddress client = null;
    public eDVS128_HardwareInterface(String deviceName) throws FileNotFoundException {
        //this.interfaceNumber = devNumber;
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(deviceName);

            if (portIdentifier.isCurrentlyOwned()) {
                log.warning("Error: Port " + deviceName + " is currently in use");
            } else {
                CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

                if (commPort instanceof SerialPort) {
                    SerialPort serialPort = (SerialPort) commPort;
                    serialPort.setSerialPortParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
                    serialPort.setFlowControlMode(serialPort.FLOWCONTROL_RTSCTS_OUT);

                    retina = serialPort.getInputStream();
                    retinaVendor = serialPort.getOutputStream();

                }
            }
        } catch (Exception e) {
            log.warning("When trying to construct an interface on port " + deviceName + " caught " + e.toString());
             throw new FileNotFoundException("Port "+deviceName+" already owned by another process?");
       } catch(Error er){
            log.warning("Caught error "+er.toString()+"; this usually means port is owned by another process");
            throw new FileNotFoundException("Port "+deviceName+" already owned by another process?");
        }


    }

    @Override
    public void open() throws HardwareInterfaceException {

        if (retinaVendor == null) {
            throw new HardwareInterfaceException("no serial interface to open");
        }

        if (!isOpen) {
            try {


                String s = "!E1\n";
                byte[] b = s.getBytes();
                retinaVendor.write(b, 0, 4);

                s = "E+\n";
                b = s.getBytes();
                retinaVendor.write(b, 0, 3);

                isOpen = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /*
        if (!isOpen){
        try{
        if ( input != null ){
        input.close();
        }
        
        input = new AEUnicastInput(STREAM_PORT);
        input.setSequenceNumberEnabled(false);
        input.setAddressFirstEnabled(true);
        input.setSwapBytesEnabled(true);
        input.set4ByteAddrTimestampEnabled(true);
        input.setTimestampsEnabled(true);
        input.setLocalTimestampEnabled(true);
        input.setBufferSize(1200);
        input.setTimestampMultiplier(0.001f);
        input.open();
        isOpen = true;
        } catch ( IOException ex ){
        throw new HardwareInterfaceException(ex.toString());
        }
        }*/


    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() {
        if (retinaVendor == null) {
            return;
        }
        try {
            String s = "E-\n";
            byte[] b = s.getBytes();
            retinaVendor.write(b, 0, 3);
            isOpen = false;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getTypeName() {
        return "eDVS128";
    }

    @Override
    public void setChip(AEChip chip) {
        this.chip = chip;
        if(chip==null) return;
       if (!addedBiasListeners && chip instanceof DVS128) {
            addedBiasListeners = true;
            DVS128 dvs128=(DVS128)chip;
            ArrayList<Pot> pots = dvs128.getBiasgen().getPotArray().getPots();
            for (Pot p : pots) {
                p.addObserver(this); // TODO first send won't work
            }
        }
    }

    @Override
    public AEChip getChip() {
        return chip;
    }

    @Override
    final public int getTimestampTickUs() {
        return TICK_US;
    }
    private int estimatedEventRate = 0;

    @Override
    public int getEstimatedEventRate() {
        return estimatedEventRate;
    }

    @Override
    public int getMaxCapacity() {
        return 156250000;
    }

    @Override
    public void addAEListener(AEListener listener) {
        support.addPropertyChangeListener(listener);
    }

    @Override
    public void removeAEListener(AEListener listener) {
        support.removePropertyChangeListener(listener);
    }

    @Override
    public void setEventAcquisitionEnabled(boolean enable) throws HardwareInterfaceException {
        if (enable) {
            //startAer();
            startAEReader();
        } else {
            //stopAer();
            stopAEReader();
        }
    }

    @Override
    public boolean isEventAcquisitionEnabled() {
        return eventAcquisitionEnabled;
    }

    @Override
    public int getAEBufferSize() {
        return aeBufferSize; // aePacketRawPool.writeBuffer().getCapacity();
    }

    @Override
    public void setAEBufferSize(int size) {
        if (size < 1000 || size > 1000000) {
            log.warning("ignoring unreasonable aeBufferSize of " + size + ", choose a more reasonable size between 1000 and 1000000");
            return;
        }
        this.aeBufferSize = size;
        prefs.putInt("eDVS128.aeBufferSize", aeBufferSize);
    }

    @Override
    public boolean overrunOccurred() {
        return overrunOccuredFlag;
    }

    /** Resets the timestamp unwrap value, and resets the AEPacketRawPool.
     */
    @Override
    synchronized public void resetTimestamps() {
        wrapAdd=0; //TODO call TDS to reset timestamps
        aePacketRawPool.reset();
    }

    /** returns last events from {@link #acquireAvailableEventsFromDriver}
     *@return the event packet
     */
    @Override
    public AEPacketRaw getEvents() {
        return this.lastEventsAcquired;
    }

    /** Returns the number of events acquired by the last call to {@link
     * #acquireAvailableEventsFromDriver }
     * @return number of events acquired
     */
    @Override
    public int getNumEventsAcquired() {
        return lastEventsAcquired.getNumEvents();
    }

    @Override
    public AEPacketRaw acquireAvailableEventsFromDriver() throws HardwareInterfaceException {
        if (!eventAcquisitionEnabled) {
            setEventAcquisitionEnabled(true);
        }
        int nEvents;
        aePacketRawPool.swap();
        lastEventsAcquired = aePacketRawPool.readBuffer();
        nEvents = lastEventsAcquired.getNumEvents();
        eventCounter = 0;
        computeEstimatedEventRate(lastEventsAcquired);

        if (nEvents != 0) {
            support.firePropertyChange(NEW_EVENTS_PROPERTY_CHANGE); // call listeners  
        }

        return lastEventsAcquired;
    }

    synchronized public void vendorRequest(int cmd) {
        try {
            switch (cmd) {
                case 1:
                    byte[] command = new byte[]{'E', '+', '\r', '\n'};
                    retinaVendor.write(command, 0, 4);
                    break;

                case 2:
                    //byte[] command = new byte[]{'E','-','\n'};
                    //retinaVendor.write(command,0,3);         
                    break;


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** computes the estimated event rate for a packet of events */
    void computeEstimatedEventRate(AEPacketRaw events) {
        if (events == null || events.getNumEvents() < 2) {
            estimatedEventRate = 0;
        } else {
            int[] ts = events.getTimestamps();
            int n = events.getNumEvents();
            int dt = ts[n - 1] - ts[0];
            estimatedEventRate = (int) (1e6f * (float) n / (float) dt);
        }
    }

    @Override
    public void setPowerDown(boolean powerDown) {
        log.warning("Power down not supported by eDVS128 devices.");
    }
    private boolean addedBiasListeners = false;

    @Override
    public void sendConfiguration(Biasgen biasgen) throws HardwareInterfaceException {
        // comes here when bias values should be sent, but we don't know which one should be sent from this call.
    }

    @Override
    public void flashConfiguration(Biasgen biasgen) {
        log.warning("Flash configuration not supported by eDVS128 devices.");
    }

    @Override
    public byte[] formatConfigurationBytes(Biasgen biasgen) {
        throw new UnsupportedOperationException("Not supported yet.");// TODO use this to send all biases at once?  yes, this is how the on-chip bias generator works
    }

    @Override
    public String toString() {
        return "Serial: eDVS128";
    }
    protected boolean running = true;
     final int WRAP = 0x10000; // amount of timestamp wrap to add for each overflow of 1 bit timestamps
   final int WRAP_START = 0; //(int)(0xFFFFFFFFL&(2147483648L-0x400000L)); // set high to test big wrap 1<<30;
    volatile int wrapAdd = WRAP_START; //0;
    protected AEReader aeReader = null;

    public AEReader getAeReader() {
        return aeReader;
    }

    public void setAeReader(AEReader aeReader) {
        this.aeReader = aeReader;
    }

    public void startAEReader() {
        setAeReader(new AEReader(this));
        log.info("Start AE reader...");
        getAeReader().start();
        eventAcquisitionEnabled = true;
    }

    public void stopAEReader() {
        if (getAeReader() != null) {
            // close device
            getAeReader().finish();
            setAeReader(null);
            releaseInterface();
        }
    }

    synchronized void claimInterface() {
    }

    synchronized public void releaseInterface() {
    }

    /** Called when notifyObservers is called in Observable we are watching, e.g. biasgen */
    @Override
    synchronized public void update(Observable o, Object arg) {

        if (o instanceof IPot) {
            if (retinaVendor == null) {
                log.warning("no connection; null serial device");
                return;
            }
            IPot p = (IPot) o;
            int v = p.getBitValue();
            int n = NUM_BIASES-1-p.getShiftRegisterNumber(); // eDVS firmware numbers in reverse order from DVS firmware, we want shift register 0 to become 11 on the eDVS
            String s = String.format("!B%d=%d\n", n, v); // LPC210 has 16-byte serial buffer, hopefully fits
            if(DEBUG) log.info("sending command "+s+" for pot "+p+" at bias position "+n);
           // note that eDVS must have rev1 firmware for BF command to work.
            byte[] b = s.getBytes();
            if(b.length>16) log.warning("sending "+b.length+" bytes, might not fit in eDVS LPC2016 16-byte buffer");
            try {
                retinaVendor.write(b, 0, b.length);
            } catch (IOException ex) {
                log.warning(ex.toString());
            }
            s="!BF\n";
            b=s.getBytes();
            try {
                retinaVendor.write(b, 0, b.length);
            } catch (IOException ex) {
                log.warning(ex.toString());
            }
       }
    }

    public class AEReader extends Thread implements Runnable {

        private byte[] buffer = null;
        eDVS128_HardwareInterface monitor;

        public AEReader(eDVS128_HardwareInterface monitor) {
            this.monitor = monitor;
            /* This is a list of all this interface's endpoints. */
            allocateAEBuffers();

            buffer = new byte[8192 * 4];//UsbUtil.unsignedInt(usbPipe.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize())];
        }

        public void run() {

            int offset = 0;
            int length = 0;
            int len = 0;

            while (running) {
                try {
                    len = retina.available();
                    length = retina.read(buffer, 0, len - (len % 4));
//                           System.out.println(length);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                int nDump = 0;

                if (len > 3) {
//                    try {
//                        length = retina.read(buffer, 0, len - (len % 4));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    translateEvents_code(buffer, length);

                    if (bCalibrated == 0) {
                        int diff = 0;
                        if (length > 100) {
                            for (int i = 0; i <= 3; i++) {
                                //offset=i;
                                diff = 0;
                                for (int m = 0; m < 10; m++) {
                                    diff += (int) (buffer[4 * (m + 1) + i] - buffer[4 * m + i]) * (int) (buffer[4 * (m + 1) + i] - buffer[4 * m + i]);
                                }
                                //System.out.println(diff);
                                if (diff < 20) { //10
                                    offset = i;
                                    //break;
                                }
                            }
                        }

                        //System.out.println("length: " + length + " tail: " + nTail + " offset: " + offset);

                        switch (offset) {
                            case 0:
                                nDump = 2;
                                break;
                            case 1:
                                nDump = 3;
                                break;
                            case 2:
                                nDump = 0;
                                break;
                            case 3:
                                nDump = 1;
                                break;
                            default:
                                log.info("Achtung, error");
                        }

                        if (nDump != 0) {
                            long length1 = 0;
                            long len1 = 0;
                            try {
                                while (length1 != nDump) {
                                    //len = retina.read(buffer, length1, nDump - length1);
                                    len1 = retina.skip(nDump - length1);
                                    length1 = length1 + len1;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            log.info("Dumped: " + length1 + " bytes / " + nDump);
                        } else {
                            bCalibrated = 1;
                            log.info("Calibrated");
                        }
                    }

                }

                if (timestampsReset) {
                    log.info("timestampsReset: flushing aePacketRawPool buffers");
                    aePacketRawPool.reset();
                    timestampsReset = false;
                }

            }


        }

        synchronized private void submit(int BufNumber) {
        }

        /**
         * Stop/abort listening for data events.
         */
        synchronized public void finish() {
            running = false;
        }

        synchronized public void resetTimestamps() {
            log.info(eDVS128_HardwareInterface.this + ": wrapAdd=" + wrapAdd + ", zeroing it");
            wrapAdd = WRAP_START;
            timestampsReset = true; // will inform reader thread that timestamps are reset

        }
        protected boolean running = true;
        volatile boolean timestampsReset = false; // used to tell processData that another thread has reset timestamps
    }
    private int inputProcessingIndex = 0;
    private int pixelX, pixelY, pixelP;
    private String specialData;
    private int bCali = 1;
    
    private long lastWrapTime=0;

    protected void translateEvents_code(byte[] b, int bytesSent) {
        synchronized (aePacketRawPool) {
            eventCounter = 0;

            AEPacketRaw buffer = aePacketRawPool.writeBuffer();
            int shortts;

            int[] addresses = buffer.getAddresses();
            int[] timestamps = buffer.getTimestamps();

            // write the start of the packet
            buffer.lastCaptureIndex = eventCounter;

            StringBuilder sb = null;
            if (DEBUG) {
                sb = new StringBuilder(String.format("%d events: Timestamp deltas are ", bytesSent / 4));
            }
            for (int i = 0; i < bytesSent; i += 4) {
                /* event polarity is encoded in the msb of the second byte. i.e.
                        Byte0, bit 7:         always zero
                        Byte0, bits 6-0:   event address y
                        Byte1, bit 7:         event polarity
                        Byte1, bits 6-0:   event address x
                        Bytes2+3:             16 bit timestamp, MSB first
                 */
                int y_ = (0xff & b[i]); // & with 0xff to prevent sign bit from extending to int (treat byte as unsigned)
                int x_ = (0xff &  b[i + 1]);
                int c_ = (0xff &  b[i + 2]);
                int d_ = (0xff &  b[i + 3]);

//                if ( (y_ & 0x80) != 0){
//                     System.out.println("Data not aligned!");
//                }

                addresses[eventCounter] = (int) ((x_ & cHighBitMask) >> 7 | ((y_ & cLowerBitsMask) << 8) | ((x_ & cLowerBitsMask) << 1)) & 0x7FFF;
                //timestamps[eventCounter] = (c_ | (d_ << 8));

                shortts = ((0xffff & (d_ << 8)) + c_);  // should be unsigned since c_ and d_ are unsigned
                if (lastshortts > shortts) { // timetamp wrapped
                    wrapAdd+=WRAP;
                    long thisWrapTime=System.nanoTime();
                    
                    if(DEBUG) log.info("This timestamp was less than last one by "+(shortts-lastshortts)+" and system deltaT="+(thisWrapTime-lastWrapTime)/1000+"us, wrapAdd="+(wrapAdd/WRAP)+" wraps");
                    lastWrapTime=thisWrapTime;
                }
                timestamps[eventCounter] = (wrapAdd + shortts) * TICK_US;
               if (DEBUG) {
                    sb.append(String.format("%d ", shortts-lastshortts));
                }
               lastshortts = shortts;

                  eventCounter++;
            }
            if (DEBUG) {
                log.info(sb.toString());
            }
            buffer.setNumEvents(eventCounter);

            // write capture size
            buffer.lastCaptureLength = eventCounter - buffer.lastCaptureIndex;
        } // sync on aePacketRawPool

    }

    void allocateAEBuffers() {
        synchronized (aePacketRawPool) {
            aePacketRawPool.allocateMemory();
        }
    }

    private class AEPacketRawPool {

        int capacity;
        AEPacketRaw[] buffers;
        AEPacketRaw lastBufferReference;
        volatile int readBuffer = 0, writeBuffer = 1; // this buffer is the one currently being read from

        AEPacketRawPool() {
            allocateMemory();
            reset();
        }

        synchronized final void swap() {
            lastBufferReference = buffers[readBuffer];
            if (readBuffer == 0) {
                readBuffer = 1;
                writeBuffer = 0;
            } else {
                readBuffer = 0;
                writeBuffer = 1;
            }
            writeBuffer().clear();
            writeBuffer().overrunOccuredFlag = false; // mark new write buffer clean, no overrun happened yet. writer sets this if it happens

        }

        /** @return buffer to read from */
        synchronized final AEPacketRaw readBuffer() {
            return buffers[readBuffer];
        }

        /** @return buffer to write to */
        synchronized final AEPacketRaw writeBuffer() {
            return buffers[writeBuffer];
        }

        /** Set the current buffer to be the first one and clear the write buffer */
        synchronized final void reset() {
            readBuffer = 0;
            writeBuffer = 1;
            buffers[writeBuffer].clear(); // new events go into this buffer which should be empty

            buffers[readBuffer].clear();  // clear read buffer in case this buffer was reset by resetTimestamps
//            log.info("buffers reset");

        }

        // allocates AEPacketRaw each with capacity AE_BUFFER_SIZE
        private void allocateMemory() {
            buffers = new AEPacketRaw[2];
            for (int i = 0; i < buffers.length; i++) {
                buffers[i] = new AEPacketRaw();
                buffers[i].ensureCapacity(getAEBufferSize()); // preallocate this memory for capture thread and to try to make it contiguous

            }
        }
    }
}