/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.unizh.ini.jaer.chip.cochlea;

import ch.unizh.ini.jaer.chip.cochlea.CochleaAMS1cADCSamples.ChannelBuffer;
import ch.unizh.ini.jaer.chip.util.scanner.ScannerHardwareInterfaceProxy;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * 
 * Holds the frame of ADC samples to be displayed for CochleaAMS1c.
 * Double-buffers the samples with a locking mechanism.
 * Includes option for sync input to reset sample counter to start of array, or allows larger number of samples for continuous recording of 
 * data not using scanner with its sync output.
 * @author Tobi
 */
final public class CochleaAMS1cADCSamples implements Observer {

    final static Logger log = Logger.getLogger("CochleaAMS1c");
//    public static final int WIDTH = 64, HEIGHT = 1;
    public static final int MAX_NUM_SAMPLES = 100000; // fixed max size now, with sync() call to reset to start of array // WIDTH * HEIGHT;
    public static final int NUM_CHANNELS = 4;
    public static final int MAX_ADC_VALUE = 1023;
    DataBuffer data1 = new DataBuffer(), data2 = new DataBuffer();
    /** Readers should access the current reading buffer. */
    private DataBuffer currentReadingDataBuffer = data1;
    private DataBuffer currentWritingDataBuffer = data2;
    private Semaphore semaphore = new Semaphore(1);
    public static final int DEFAULT_SCAN_LENGTH = 64;
    private int scanLength = DEFAULT_SCAN_LENGTH;
    private boolean hasScannerData = false; // flag to mark that buffers should wrap at scanLength on write and read
    private CochleaAMS1c cochleaChip;

    public CochleaAMS1cADCSamples(CochleaAMS1c cochleaChip) {
        this.cochleaChip = cochleaChip; // cannot access biasgen/scanner yet, not constructed yet probably
        if (cochleaChip.getScanner() == null) {
            log.warning("cannot bind to Scanner object, it's null.");
            return;
        }
        cochleaChip.getScanner().addObserver(this);
        hasScannerData = cochleaChip.getScanner().isScanContinuouslyEnabled();
    }

    /**
     * @return the hasScannerData
     */
    public boolean isHasScannerData() {
        return hasScannerData;
    }

    /**
     * @param hasScannerData the hasScannerData to set
     */
    public void setHasScannerData(boolean hasScannerData) {
        this.hasScannerData = hasScannerData;
    }

    /**
     * @return the scanLength
     */
    public int getScanLength() {
        return scanLength;
    }

    /**
     * @param scanLength the scanLength to set
     */
    public void setScanLength(int scanLength) {
        this.scanLength = scanLength;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg == ScannerHardwareInterfaceProxy.EVENT_SCAN_CONTINUOUSLY_ENABLED) {
            hasScannerData = cochleaChip.getScanner().isScanContinuouslyEnabled();
        }
    }

    /**
     * @return the currentReadingDataBuffer
     */
    public DataBuffer getCurrentReadingDataBuffer() {
        return currentReadingDataBuffer;
    }

    /**
     * @return the currentWritingDataBuffer
     */
    public DataBuffer getCurrentWritingDataBuffer() {
        return currentWritingDataBuffer;
    }

    /** A single samples */
    public class ADCSample {

        int data, time;
    }

    /** A buffer for a single channel of the ADC which holds an array of samples in the ADCSample array. */
    final public class ChannelBuffer {

        final int channel;
        public ADCSample[] samples = new ADCSample[MAX_NUM_SAMPLES];
        private int writeCounter = 0;

        public ChannelBuffer(final int channel) {
            this.channel = channel;
            for (int i = 0; i < MAX_NUM_SAMPLES; i++) {
                samples[i] = new ADCSample();
            }
        }

        /** Call this when scanning and we see a sync output active from scanner. Clears the buffer and sets syncDetected. */
        public void sync() {
            clear();
        }

        private void clear() {
            if (hasScannerData && writeCounter < getScanLength()) {
                log.warning("cleared when writeCounter is only " + writeCounter);
            }
            writeCounter = 0;
        }

        public boolean hasData() {
            return writeCounter > 0;
        }

        /**
         * 
         * @param time
         * @param val
         * @param sync true if data sample is at scanner sync
         */
        private void put(int time, int val, boolean sync) {
            if (writeCounter >= MAX_NUM_SAMPLES - 1) {
//            log.info("buffer overflowed - missing start frame bit?");
                return;
            }
            if (sync && hasScannerData) {
                if (writeCounter != getScanLength()) {
                    log.warning("writeCounter=" + writeCounter + " at sync but it should be " + getScanLength());
                }
                sync();
            }
            if (!sync) {
                samples[writeCounter].time = time;
                samples[writeCounter].data = val;
                writeCounter++;
            }

            if (writeCounter == MAX_NUM_SAMPLES) {
                writeCounter = 0;
            }
        }

        /** Returns number of samples written if not scanning, otherwise return scanLength if channel has any data */
        public int size() {
            if (!hasScannerData) {
                return writeCounter;
            } else {
                if (writeCounter > 0) {
                    return scanLength;
                } else {
                    return 0;
                }
            }
        }

        /** Returns delta time from start to end, or 0 if less than 2 samples.
         */
        public int deltaTime() {
            if (writeCounter < 2) {
                return 0;
            }
            return samples[writeCounter - 1].time - samples[0].time;
        }
    }

    /** The top data structure that holds data from all channels - some may be empty if conversion is not enabled */
    public final class DataBuffer {

        ChannelBuffer[] channelBuffers = new ChannelBuffer[NUM_CHANNELS];

        public DataBuffer() {
            for (int i = 0; i < NUM_CHANNELS; i++) {
                channelBuffers[i] = new ChannelBuffer(i);
            }
        }

        private void clear() {
            for (int i = 0; i < NUM_CHANNELS; i++) {
                channelBuffers[i].clear();
            }

        }
    }

    /** Acquire this semaphore to prevent buffer swapping. */
    public final void acquire() {
        semaphore.acquireUninterruptibly();
    }

    /** Don't forget to release the semaphore. */
    public final void release() {
        semaphore.release();
    }

    public final ADCSample get(int channel, int x) {
        return currentReadingDataBuffer.channelBuffers[channel].samples[x];
    }

    /** puts a sample with boolean sync that resets to start of buffer
     * 
     * @param channel
     * @param time
     * @param val
     * @param sync true if sync reset to start
     */
    public final void put(int channel, int time, int val, boolean sync) {
        ChannelBuffer c = currentWritingDataBuffer.channelBuffers[channel];
        c.put(time, val, sync);
    }

    /** Swaps the current writing and reading buffers after acquiring the lock. */
    public void swapBuffers() {
        acquire();
        DataBuffer tmp = currentReadingDataBuffer;
        currentReadingDataBuffer = currentWritingDataBuffer;
        currentWritingDataBuffer = tmp;
        currentWritingDataBuffer.clear();
        release();
    }

    private int getMean(int[] dataIn) {
        int mean = 0;
        for (int i = 0; i < dataIn.length; i++) {
            mean += dataIn[i];
        }
        mean = mean / dataIn.length;
        return mean;
    }

    private void subtractMean(int[] dataIn, int[] dataOut) {
        int mean = getMean(dataIn);

        for (int i = 0; i < dataOut.length; i++) {
            dataOut[i] = dataIn[i] - mean;
        }
    }
}
