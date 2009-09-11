/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * DynamixelController.java
 *
 * Created on Aug 31, 2009, 12:37:21 PM
 */
package dynamixel;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Random;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import  sun.audio.*;    //import the sun.audio package
import  java.io.*;


/**
 *
 * @author hfinger
 */
public class DynamixelDirect extends javax.swing.JFrame {

    private static Logger log = Logger.getLogger("Dynamixel");
    public OutputStream out;
    public InputStream in;
    private SerialReader serialReader;
    private int[] servoPos = {0, 0, 0};
    private boolean isCheckMovement = false;
    private boolean willCheckMovement = false;
    private boolean isGettingPositions = false;
    Random rndGen = new Random();
    private Timer timeoutTimer;
    private Timer timerBetweenMovements;
    private boolean waitingForStatusPacket = false;
    private byte[] nextCommand;
    private int repetitionsLeft = 0;

    /** Creates new form DynamixelController */
    public DynamixelDirect() {
        initComponents();
        Enumeration pList = CommPortIdentifier.getPortIdentifiers();
        while (pList.hasMoreElements()) {
            CommPortIdentifier cpi = (CommPortIdentifier) pList.nextElement();
            if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                cbxPort.addItem(cpi.getName());
            }
        }

        cbxPort.setSelectedIndex(-1);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        cbxPort = new javax.swing.JComboBox();
        btnConnect = new javax.swing.JButton();
        sld3 = new javax.swing.JSlider();
        sld2 = new javax.swing.JSlider();
        sld1 = new javax.swing.JSlider();
        btnCheckMoving = new javax.swing.JButton();
        txtPause = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        btnRandomPos = new javax.swing.JButton();
        sldSpeed = new javax.swing.JSlider();
        jLabel3 = new javax.swing.JLabel();
        btnGetCurrentPos = new javax.swing.JButton();
        btnMax1 = new javax.swing.JButton();
        btnMin1 = new javax.swing.JButton();
        btnMax2 = new javax.swing.JButton();
        btnMax3 = new javax.swing.JButton();
        btnMin2 = new javax.swing.JButton();
        btnMin3 = new javax.swing.JButton();
        btnRun = new javax.swing.JToggleButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtPauseLong = new javax.swing.JTextField();
        txtRepetitions = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtRepetitionsLeft = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Port:");

        cbxPort.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnConnect.setText("Connect");
        btnConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectActionPerformed(evt);
            }
        });

        sld3.setMaximum(871);
        sld3.setMinimum(493);
        sld3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sld3MouseReleased(evt);
            }
        });

        sld2.setMaximum(714);
        sld2.setMinimum(373);
        sld2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sld2MouseReleased(evt);
            }
        });

        sld1.setMaximum(1023);
        sld1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sld1MouseReleased(evt);
            }
        });

        btnCheckMoving.setText("Is Moving?");
        btnCheckMoving.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCheckMovingActionPerformed(evt);
            }
        });

        txtPause.setText("1");

        jLabel2.setText("Pause:");

        btnRandomPos.setText("Random Positions");
        btnRandomPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRandomPosActionPerformed(evt);
            }
        });

        sldSpeed.setMaximum(1023);
        sldSpeed.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sldSpeedMouseReleased(evt);
            }
        });

        jLabel3.setText("Speed");

        btnGetCurrentPos.setText("Get Current Positions");
        btnGetCurrentPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGetCurrentPosActionPerformed(evt);
            }
        });

        btnMax1.setText("set Max");
        btnMax1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMax1ActionPerformed(evt);
            }
        });

        btnMin1.setText("set Min");
        btnMin1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMin1ActionPerformed(evt);
            }
        });

        btnMax2.setText("set Max");
        btnMax2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMax2ActionPerformed(evt);
            }
        });

        btnMax3.setText("set Max");
        btnMax3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMax3ActionPerformed(evt);
            }
        });

        btnMin2.setText("set Min");
        btnMin2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMin2ActionPerformed(evt);
            }
        });

        btnMin3.setText("set Min");
        btnMin3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMin3ActionPerformed(evt);
            }
        });

        btnRun.setText("Run");
        btnRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunActionPerformed(evt);
            }
        });

        jLabel4.setText("LongPause:");

        jLabel5.setText("Repetitions:");

        txtPauseLong.setText("5");

        txtRepetitions.setText("10");

        jLabel6.setText("Left: ");

        txtRepetitionsLeft.setEditable(false);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(66, 66, 66)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jLabel2)
                            .add(jLabel4))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(txtPauseLong, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(txtPause, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cbxPort, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(btnConnect))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(8, 8, 8)
                                .add(btnRandomPos)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(btnGetCurrentPos))
                            .add(layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                                        .add(btnMin1)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(sld1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                                        .add(btnMin2)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(sld2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 190, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                                        .add(btnMin3)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(sld3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(btnMax1)
                                    .add(btnMax2)
                                    .add(btnMax3)))
                            .add(layout.createSequentialGroup()
                                .add(145, 145, 145)
                                .add(btnRun)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(btnCheckMoving))
                            .add(layout.createSequentialGroup()
                                .add(57, 57, 57)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                    .add(jLabel5)
                                    .add(jLabel3))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(sldSpeed, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(layout.createSequentialGroup()
                                        .add(txtRepetitions, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(18, 18, 18)
                                        .add(jLabel6)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(txtRepetitionsLeft, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))))
                .addContainerGap(82, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(cbxPort, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(btnConnect))
                .add(12, 12, 12)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(txtPause, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(txtPauseLong, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(txtRepetitions, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6)
                    .add(txtRepetitionsLeft, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(27, 27, 27)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createSequentialGroup()
                        .add(jLabel3)
                        .add(7, 7, 7))
                    .add(sldSpeed, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(btnRandomPos)
                    .add(btnGetCurrentPos))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(btnMin3)
                    .add(sld3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(btnMax3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(btnMin2)
                    .add(btnMax2)
                    .add(sld2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(btnMax1)
                    .add(btnMin1)
                    .add(sld1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(btnRun)
                    .add(btnCheckMoving))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectActionPerformed
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier((String) this.cbxPort.getSelectedItem());
            if (portIdentifier.isCurrentlyOwned()) {
                log.warning("Error: Port for Dynamixel-Communication is currently in use");
            } else {
                CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);
                if (commPort instanceof SerialPort) {
                    SerialPort serialPort = (SerialPort) commPort;
                    serialPort.setSerialPortParams(57000, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    in = serialPort.getInputStream();
                    out = serialPort.getOutputStream();
                    serialReader = new SerialReader(in);
                    serialPort.addEventListener(serialReader);
                    serialPort.notifyOnDataAvailable(true);
                    log.info("Connected to Dynamixel!");
                } else {
                    log.warning("Error: Cannot connect to Dynamixel!");
                }
            }
        } catch (TooManyListenersException ex) {
            Logger.getLogger(DynamixelDirect.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DynamixelDirect.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedCommOperationException ex) {
            Logger.getLogger(DynamixelDirect.class.getName()).log(Level.SEVERE, null, ex);
        } catch (PortInUseException ex) {
            Logger.getLogger(DynamixelDirect.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPortException ex) {
            Logger.getLogger(DynamixelDirect.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btnConnectActionPerformed

    private void setAllServosToPos() {
        byte[] parameter = new byte[11];
        parameter[0] = 30; //destination address
        parameter[1] = 2; //length of data to write to each servo

        for (int i = 0; i < 3; i++) {
            parameter[i * 3 + 2] = (byte) (i + 1);
            short pos = (short) servoPos[i];
            ByteBuffer bbPos = ByteBuffer.allocate(2);
            bbPos.order(ByteOrder.LITTLE_ENDIAN);
            bbPos.putShort(pos);
            parameter[i * 3 + 3] = bbPos.get(0);
            parameter[i * 3 + 4] = bbPos.get(1);
        }
        command((byte) 254, (byte) 131, parameter, false);
    }

    private void setAllServosToSpeed() {
        byte[] parameter = new byte[11];
        parameter[0] = 32; //destination address
        parameter[1] = 2; //length of data to write to each servo

        for (int i = 0; i < 3; i++) {
            parameter[i * 3 + 2] = (byte) (i + 1);
            short speed = (short) sldSpeed.getValue();
            ByteBuffer bbSpeed = ByteBuffer.allocate(2);
            bbSpeed.order(ByteOrder.LITTLE_ENDIAN);
            bbSpeed.putShort(speed);
            parameter[i * 3 + 3] = bbSpeed.get(0);
            parameter[i * 3 + 4] = bbSpeed.get(1);
        }
        command((byte) 254, (byte) 131, parameter, false);
    }

    public void move(byte ID, short pos, short speed) {
        log.info("Set servo " + ID + " to position " + pos);
        ByteBuffer bbPos = ByteBuffer.allocate(2);
        bbPos.order(ByteOrder.LITTLE_ENDIAN);
        bbPos.putShort(pos);
        byte posH = bbPos.get(0);
        byte posL = bbPos.get(1);
        ByteBuffer bbSpeed = ByteBuffer.allocate(2);
        bbSpeed.order(ByteOrder.LITTLE_ENDIAN);
        bbSpeed.putShort(speed);
        byte speedH = bbSpeed.get(0);
        byte speedL = bbSpeed.get(1);

        byte[] parameter = {30, posH, posL, speedH, speedL};
        command(ID, (byte) 3, parameter, false);
    }

    public void command(byte id, byte instruction, byte[] parameter, boolean checkMovement) {
        byte[] packet = new byte[parameter.length + 6];
        packet[0] = (byte) 255;
        packet[1] = (byte) 255;
        packet[2] = id;
        packet[3] = (byte) (parameter.length + 2);
        packet[4] = instruction;
        byte sum = (byte) (packet[2] + packet[3] + packet[4]);
        for (int i = 0; i < parameter.length; i++) {
            sum += parameter[i];
            packet[i + 5] = parameter[i];
        }
        packet[packet.length - 1] = (byte) ~sum;

        if (waitingForStatusPacket) {
            willCheckMovement = checkMovement;
            nextCommand = packet;
            //log.info("added next command to queue");
        } else {
            isCheckMovement = checkMovement;
            sendCommand(packet);
        }
    }

    private void sendCommand(byte[] packet) {
        try {
            if (packet[2] != (byte) 254) {
                //log.info("starting time for timeout of status packet.");
                timeoutTimer = new Timer(1000, new ActionListener() {

                    public void actionPerformed(ActionEvent evt) {
                        log.warning("Error: Timeout while waiting for Status packet from Servos!!!");
                        waitingForStatusPacket = false;
                        timeoutTimer.stop();
                        if (isCheckMovement) {
                            log.info("Restarting to check movement!");
                            btnCheckMovingActionPerformed(null);
                        }
                    }
                });
                timeoutTimer.start();
                waitingForStatusPacket = true;
            }
            this.out.write(packet);

            String test3 = "command: ";
            for (int i = 0; i < packet.length; i++) {
                test3 += " " + (packet[i] & 0xFF);
            }
            log.info("Send to RS232: " + test3);

        } catch (IOException ex) {
            log.warning("In command(...) caught IOexception " + ex);
        }
    }

    public void checkNextIfMoving(byte ID) {
        //log.info("Start to check if Servo " + ID + " is moving.");
        byte[] parameter = {46, 1};
        command(ID, (byte) 2, parameter, true);
    }

    private void getNextPos(byte ID) {
        byte[] parameter = {36, 2};
        command(ID, (byte) 2, parameter, false);
    }

    private void btnCheckMovingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCheckMovingActionPerformed

        checkNextIfMoving((byte) 1);
    }//GEN-LAST:event_btnCheckMovingActionPerformed

    private void sld1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sld1MouseReleased
        if (servoPos[0] != sld1.getValue()) {
            servoPos[0] = sld1.getValue();
            move((byte) 1, (short) servoPos[0], (short) sldSpeed.getValue());
        }
    }//GEN-LAST:event_sld1MouseReleased

    private void sld2MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sld2MouseReleased
        if (servoPos[1] != sld2.getValue()) {
            servoPos[1] = sld2.getValue();
            move((byte) 2, (short) servoPos[1], (short) sldSpeed.getValue());
        }
    }//GEN-LAST:event_sld2MouseReleased

    private void sld3MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sld3MouseReleased
        if (servoPos[2] != sld3.getValue()) {
            servoPos[2] = sld3.getValue();
            move((byte) 3, (short) servoPos[2], (short) sldSpeed.getValue());
        }
    }//GEN-LAST:event_sld3MouseReleased

    private void btnGetCurrentPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGetCurrentPosActionPerformed
        isGettingPositions = true;
        getNextPos((byte) 1);
    }//GEN-LAST:event_btnGetCurrentPosActionPerformed

    private void btnMin1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMin1ActionPerformed
        sld1.setMinimum(sld1.getValue());
    }//GEN-LAST:event_btnMin1ActionPerformed

    private void btnMin2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMin2ActionPerformed
        sld2.setMinimum(sld2.getValue());
    }//GEN-LAST:event_btnMin2ActionPerformed

    private void btnMin3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMin3ActionPerformed
        sld3.setMinimum(sld3.getValue());
    }//GEN-LAST:event_btnMin3ActionPerformed

    private void btnMax1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMax1ActionPerformed
        sld1.setMaximum(sld1.getValue());
    }//GEN-LAST:event_btnMax1ActionPerformed

    private void btnMax2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMax2ActionPerformed
        sld2.setMaximum(sld2.getValue());
    }//GEN-LAST:event_btnMax2ActionPerformed

    private void btnMax3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMax3ActionPerformed
        sld3.setMaximum(sld3.getValue());
    }//GEN-LAST:event_btnMax3ActionPerformed

    private void btnRandomPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRandomPosActionPerformed
        //log.info("generate new random servo positions!");
        sld1.setValue(randGenBetween(sld1.getMinimum(), sld1.getMaximum()));
        sld2.setValue(randGenBetween(sld2.getMinimum(), sld2.getMaximum()));
        sld3.setValue(randGenBetween(sld3.getMinimum(), sld3.getMaximum()));
        servoPos[0] = sld1.getValue();
        servoPos[1] = sld2.getValue();
        servoPos[2] = sld3.getValue();
        setAllServosToPos();
        if (btnRun.isSelected()) {

            checkNextIfMoving((byte) 1);
        }
    }//GEN-LAST:event_btnRandomPosActionPerformed

    private void btnRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunActionPerformed
        if (btnRun.isSelected()) {
            btnRandomPosActionPerformed(null);
        }
    }//GEN-LAST:event_btnRunActionPerformed

    private void sldSpeedMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sldSpeedMouseReleased
        setAllServosToSpeed();
    }//GEN-LAST:event_sldSpeedMouseReleased

    private int randGenBetween(int a, int b) {
        return a + (int) ((b - a) * rndGen.nextDouble());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new DynamixelDirect().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCheckMoving;
    private javax.swing.JButton btnConnect;
    private javax.swing.JButton btnGetCurrentPos;
    private javax.swing.JButton btnMax1;
    private javax.swing.JButton btnMax2;
    private javax.swing.JButton btnMax3;
    private javax.swing.JButton btnMin1;
    private javax.swing.JButton btnMin2;
    private javax.swing.JButton btnMin3;
    private javax.swing.JButton btnRandomPos;
    private javax.swing.JToggleButton btnRun;
    private javax.swing.JComboBox cbxPort;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSlider sld1;
    private javax.swing.JSlider sld2;
    private javax.swing.JSlider sld3;
    private javax.swing.JSlider sldSpeed;
    private javax.swing.JTextField txtPause;
    private javax.swing.JTextField txtPauseLong;
    private javax.swing.JTextField txtRepetitions;
    private javax.swing.JTextField txtRepetitionsLeft;
    // End of variables declaration//GEN-END:variables

    /**
     *
     */
    private class SerialReader implements gnu.io.SerialPortEventListener {

        private InputStream in;
        private StatusPacket currentPacket;
        private int PositionInPacket = 1;
        /*
         * 1 = waiting for first FF
         * 2 = waiting for second FF
         * 3 = waiting for ID
         * 4 = waiting for LENGTH
         * 5 = waiting for ERROR
         * 5+i = waiting for PARAMETER i
         * 4+LENGTH = waiting for CHECKSUM
         */

        /**
         *
         * @param in
         */
        public SerialReader(InputStream in) {
            this.in = in;
        }

        /**
         *
         * @param arg0
         */
        public void serialEvent(gnu.io.SerialPortEvent arg0) {
            try {
                int data;
                while ((data = in.read()) > -1) {

                    //log.info("Received:" + ((byte) data & 0xFF));
                    switch (PositionInPacket) {
                        case 1:
                            if (data == 255) {
                                PositionInPacket = 2;
                                currentPacket = new StatusPacket();
                            } else {
                                log.warning("Dynamixel Communication Error: Beginning of a new Packet was expected! But Received:" + ((byte) data & 0xFF));
                            }
                            break;
                        case 2:
                            if (data == 255) {
                                PositionInPacket = 3;
                            } else {
                                log.warning("Dynamixel Communication Error: Second FF of new Packet was expected! But Received:" + ((byte) data & 0xFF));
                            }
                            break;
                        case 3:
                            PositionInPacket = 4;
                            currentPacket.ID = data;
                            break;
                        case 4:
                            PositionInPacket = 5;
                            currentPacket.LENGTH = data;
                            currentPacket.PARAMETER = new int[currentPacket.LENGTH - 2];
                            break;
                        case 5:
                            PositionInPacket = 6;
                            currentPacket.ERROR = data;
                            if (currentPacket.ERROR != 0) {
                                log.warning("Received error code " + currentPacket.ERROR + " from Servo " + currentPacket.ID);
                            }
                            break;
                        default:
                            try {
                                if (PositionInPacket > 5 && PositionInPacket < currentPacket.LENGTH + 4) {
                                    currentPacket.PARAMETER[PositionInPacket - 6] = data;
                                    PositionInPacket++;
                                } else if (PositionInPacket == currentPacket.LENGTH + 4) {
                                    currentPacket.CHECKSUM = data;
                                    log.info("full packet received. Now evaluate.");
                                    evaluateStatusPacket(currentPacket);
                                    PositionInPacket = 1;
                                } else {
                                    log.warning("Position in Packet is out of range!");
                                }
                            } catch (Exception ex) {
                                log.info("Exception: " + ex);
                            }
                            break;
                    }

                }
            } catch (IOException ex) {
                log.warning("Exception while reading from Serial Port:" + ex);
            }

        }

        public void evaluateStatusPacket(StatusPacket packet) {
            // Parse Status Packet:
            try {
                timeoutTimer.stop();
                timeoutTimer = null;
                if (isCheckMovement) {
                    //log.info("check movements");
                    if (packet.PARAMETER[0] == 0) {
                        //log.info("servo " + packet.ID + " is not moving!");
                        if (packet.ID >= servoPos.length) {
                            log.info("No servo is moving anymore!!!!!!!!");
                            isCheckMovement = false;

                            if (btnRun.isSelected()) {

                                ActionListener whenWaitDone = new ActionListener() {

                                    public void actionPerformed(ActionEvent evt) {
                                        timerBetweenMovements.stop();
                                        log.info("Wait Done!");
                                        if (btnRun.isSelected()) {
                                            btnRandomPosActionPerformed(null);
                                        }
                                    }
                                };

                                if (repetitionsLeft <= 1) {
                                    repetitionsLeft = Integer.parseInt(txtRepetitions.getText());
                                    timerBetweenMovements = new Timer(1000 * Integer.parseInt(txtPauseLong.getText()), whenWaitDone);
                                    //Signal
                                    
                                    if (true) {
                                        
                                        // Open an input stream  to the audio file.
InputStream inAudio = new FileInputStream("/Users/hfinger/NetBeansProjects/Dynamixel/media/beep.au");
// Create an AudioStream object from the input stream.
AudioStream as = new AudioStream(inAudio);
// Use the static class member "player" from class AudioPlayer to play
// clip.
AudioPlayer.player.start(as);      
// Similarly, to stop the audio.
//AudioPlayer.player.stop(as);


                                    }
                                    else {
                                        Toolkit.getDefaultToolkit().beep();
                                    }
                                } else {
                                    repetitionsLeft--;
                                    timerBetweenMovements = new Timer(1000 * Integer.parseInt(txtPause.getText()), whenWaitDone);
                                }
                                txtRepetitionsLeft.setText(String.valueOf(repetitionsLeft));
                                timerBetweenMovements.start();
                            }
                        } else {

                            checkNextIfMoving((byte) (packet.ID + 1));
                        }
                    } else {
                        checkNextIfMoving((byte) (packet.ID));
                    }
                }
                if (isGettingPositions) {
                    log.info("get pos of one servo:");
                    ByteBuffer bbPos = ByteBuffer.allocate(2);
                    bbPos.order(ByteOrder.LITTLE_ENDIAN);
                    bbPos.put((byte) packet.PARAMETER[0]);
                    bbPos.put((byte) packet.PARAMETER[1]);

                    short servopos = bbPos.getShort(0);
                    if (packet.ID == 1) {
                        sld1.setValue(servopos);
                        getNextPos((byte) 2);
                    } else if (packet.ID == 2) {
                        sld2.setValue(servopos);
                        getNextPos((byte) 3);
                    } else if (packet.ID == 3) {
                        sld3.setValue(servopos);
                        log.info("Done!!!!!!!!");
                        isGettingPositions = false;
                    }
                }

                waitingForStatusPacket = false;

                //log.info("waiting for status packet = false");
                if (nextCommand != null) {
                    //log.info("send the queued command");
                    sendCommand(nextCommand);
                    isCheckMovement = willCheckMovement;
                    willCheckMovement = false;
                    nextCommand = null;
                }
            } catch (Exception ex) {
                log.warning("got exception in evaluatePacket(): " + ex);
            }
        }

        private class StatusPacket {

            public int ID;
            public int LENGTH;
            public int ERROR;
            public int[] PARAMETER;
            public int CHECKSUM;
        }
    }
}
