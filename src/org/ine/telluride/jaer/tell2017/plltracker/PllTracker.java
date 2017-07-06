/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ine.telluride.jaer.tell2017.plltracker;

import ch.ethz.hest.balgrist.microscopetracker.MicroscopeTrackerRCT;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.gl2.GLUT;
import net.sf.jaer.aemonitor.AEConstants;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.BasicEvent;
import net.sf.jaer.event.OutputEventIterator;
import net.sf.jaer.event.PolarityEvent;
import net.sf.jaer.event.TypedEvent;
import static net.sf.jaer.eventprocessing.EventFilter.log;
import net.sf.jaer.eventprocessing.tracking.RectangularClusterTracker;

/**
 * From Telluride 2017, subclasses RCT to better tracking flashing LED markers.
 *
 * @author Tobi Delbruck, Jorg Conradt
 */
public class PllTracker extends RectangularClusterTracker {

    public PllTracker(AEChip chip) {
        super(chip);
    }

    // Override Factory Methods
    @Override
    public Cluster createCluster(BasicEvent ev) {
        return new Cluster(ev);
    }

    @Override
    public Cluster createCluster(RectangularClusterTracker.Cluster one, RectangularClusterTracker.Cluster two) {
        return new Cluster((Cluster) one, (Cluster) two);
    }

    @Override
    public Cluster createCluster(BasicEvent ev, OutputEventIterator itr) {
        return new Cluster(ev, itr);
    }

    public class Cluster extends RectangularClusterTracker.Cluster {

        public float intervalUs = 1000;
        public float phaseRad = 0;
        private HarmonicOscillator harmonicOscillator = new HarmonicOscillator();

        // new Constructors
        public Cluster() {
            super();
        }

        public Cluster(BasicEvent ev) {
            super(ev);
        }

        public Cluster(BasicEvent ev, OutputEventIterator outItr) {
            super(ev, outItr);
        }

        public Cluster(Cluster one, Cluster two) {
            super(one, two);
            Cluster stronger = one.getMass() > two.getMass() ? one : two; // one.firstEventTimestamp < two.firstEventTimestamp ?

            harmonicOscillator = stronger.harmonicOscillator;
        }

        @Override
        public void addEvent(BasicEvent event) {
            PolarityEvent e = (PolarityEvent) event;
            harmonicOscillator.update(e);
            super.addEvent(event);
        }

        @Override
        public void draw(GLAutoDrawable drawable) {
            super.draw(drawable);
            GL2 gl = drawable.getGL().getGL2();

            harmonicOscillator.draw(gl);
        }

        public class HarmonicOscillator {

            final float GEARRATIO = 20; // chop up times between spikes by tau/GEARRATIO timesteps
            final int POWER_AVERAGING_CYCLES = 10; // number of cycles to smooth power measurement
            boolean wasReset = true;
            private float f0 = prefs().getFloat("HarmonicFilter.frequency", 1000); // natural frequency in Hz
            private float tau, omega, tauoverq, reciptausq;
            private float dtlim;
            private float quality = prefs().getFloat("HarmonicFilter.quality", 3); // quality factor
            private float amplitude;
            float y = 0, x = 0;  // x =position, y=velocity
            private int t = 0;  // present time in timestamp ticks, used for dt in update, then stores this last timestamp
            float lastx, lasty;
            float meansq;
            float power = 0;
            float maxx, minx, maxy, miny;
            private float maxPower = 0;
            private int lastPositiveZeroCrossingTime;
            final float PI = (float) Math.PI;
            //        private float measuredFreq=0;
            private float threshold = 0.1f; // TODO move to outer and make the transmission probabilistic
            final float TICK = 1e-6f;

            public HarmonicOscillator() {
                setQuality(quality);
                setNaturalFrequency(f0); // needed to init vars
            }

            synchronized void setNaturalFrequency(float f) {
                f0 = f; // hz
                omega = (float) (2 * Math.PI * f0);  // radians/sec
                tau = 1f / omega; // seconds
                tauoverq = tau / quality;
                reciptausq = 1f / (tau * tau);
                dtlim = tau / GEARRATIO;  // timestep must be at most this long or unstable numerically
                prefs().putFloat("HarmonicFilter.frequency", f0);
            }

            synchronized void setQuality(float q) {
                quality = q;
                tauoverq = tau / quality;
                prefs().putFloat("HarmonicFilter.quality", quality);
            }

            synchronized public void update(PolarityEvent e) {
                int ts = e.timestamp;
                int pol = (e.polarity == PolarityEvent.Polarity.On ? 1 : -1);
                if (wasReset) {
                    t = ts;
                    wasReset = false;
                    return;
                }
                lastx = x;
                lasty = y;

                // apply the momentum imparted by the event. this directly affects velocity (y)
                // each event kicks velocity by 1 either pos or neg
                y = y + pol;

                // compute the delta time since last event.
                // check if it is too long for numerical stability, if so, integrate multiple steps
                float dt = TICK * (ts - t); // dt is in seconds now... if TICK is correct
                if (dt < 0) {
                    log.warning("negative delta time (" + dt + "), not processing this update");
                    wasReset = true;
                    return;
                }
                int nsteps = (int) Math.ceil(dt / dtlim); // dtlim comes from natural freq; if dt is too large, then nsteps>1
                float ddt = (dt / nsteps) * reciptausq;  // dimensionless timestep
                float ddt2 = dt / nsteps;  // real timestep
                for (int i = 0; i < nsteps; i++) {
                    y = y - (ddt * ((tauoverq * y) + x));
                    x = x + (ddt2 * y);
                    //            System.out.println(ddt2+","+x+","+y);
                }
                //            System.out.println(dt+","+x+","+y);

                float sq = x * x; // instantaneous power
                // compute avg power by lowpassing instantaneous power over POWER_AVERAGING_CYCLES time
                // TODO is this a valid measure of instantaneous power?  shouldn't we be using amplitude and current position to filter events?
                float alpha = (dt * f0) / POWER_AVERAGING_CYCLES; // mixing factor, take this much of new, 1-alpha of old
                power = (power * (1 - alpha)) + (sq * alpha);
                if (Float.isNaN(power)) {
                    log.warning("power is NaN, resetting oscillator");
                    reset();
                }
                if (power > maxPower) {
                    maxPower = power;
                }
                amplitude = (float) Math.sqrt(power);

                if (x > maxx) {
                    maxx = x;
                } else if (x < minx) {
                    minx = x;
                }
                if (y > maxy) {
                    maxy = y;
                } else if (y < miny) {
                    miny = y;
                }

                // update timestamp
                t = ts;

                if ((x > 0) && (lastx <= 0)) {
                    lastPositiveZeroCrossingTime = ts;
                }
            }

            synchronized public void reset() {
                y = 0;
                x = 0;
                power = 0;
                maxx = 0;
                minx = 0;
                maxy = 0;
                miny = 0;
                wasReset = true;
            }

            public void draw(GL2 gl) {
                GLUT cGLUT = chip.getCanvas().getGlut();
                final int font = GLUT.BITMAP_HELVETICA_18;
                cGLUT.glutBitmapString(font, String.format("amp=%.3g ", amplitude)); // annotate
                gl.glRasterPos3f(location.x, location.y, 0);
                float amplitude = 1000 * getAmplitude();
                float phase = getPhase(0);
                gl.glPushMatrix();
                gl.glTranslatef(location.x, location.y, 0);
                gl.glLineWidth(3);
                gl.glColor3f(1, 1, 1);
                gl.glBegin(GL.GL_LINES);
                gl.glVertex2f(0, 0);
                gl.glVertex2d(amplitude * Math.cos(phase), amplitude * Math.sin(phase));
                gl.glEnd();
                gl.glPopMatrix();
            }

            /**
             * Returns the phase of this particular time, based on the measured
             * last zero crossing time and time t, using the natural frequency
             * f0.
             *
             * @param t the time to measure
             * @return the phase relative to the last positive zero crossing, in
             * range 0-2*Pi, in radians.
             */
            public float getPhase(int t) {
                float ph = PI * 2 * (t - lastPositiveZeroCrossingTime) * f0 * 1e-6f; // TODO needs TICK
                return ph;
            }

            /**
             * @return the current 'position' value of the oscillator
             */
            public float getPosition() {
                return x;
            }

            /**
             * @return the current 'velocity' value of the osciallator
             */
            public float getVelocity() {
                return y;
            }

            public float getNaturalFrequency() {
                return f0;
            }

            public float getQuality() {
                return quality;
            }

            /**
             * @return the last amplitude of the oscillator, i.e., the last
             * magnitude of the last peak of activity
             */
            public float getAmplitude() {
                return amplitude;
            }

            float getMeanPower() {
                return power;
            }

            public int getT() {
                return t;
            }

            @Override
            public String toString() {
                String s = String.format("bestFreq=%.1f Q=%.2g pos=%.1g vel=%.1g meanPower=%.1g maxPower=%.1g", f0, quality, x, y, power, maxPower);
                return s;
                //            return  "bestFreq="+f0+" t=" + t + " pos=" +x+" vel="+y+" ampl="+amplitude + " meanPower=" + getMeanPower();
            }

            /**
             * @return the maxPower
             */
            public float getMaxPower() {
                return maxPower;
            }

            /**
             * @param maxPower the maxPower to set
             */
            public void setMaxPower(float maxPower) {
                this.maxPower = maxPower;
            }
        }
    }

}