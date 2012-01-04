/*
 * Copylight (C) 2009, Shunichi Yamamoto, tkrworks.net
 *
 * This file is part of PICnomeSerial.
 *
 * PicnomeSerial is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option ) any later version.
 *
 * PicnomeSerial is distributed in the hope that it will be useful,
 * but WITHIOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.   See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PicnomeSerial. if not, see <http:/www.gnu.org/licenses/>.
 *
 * PicnomeCommunication.java,v.1.5.0(140) 2012/01/02
 */

// RXTX
import gnu.io.*;

// JavaOSC
import com.illposed.osc.*;
import com.illposed.osc.utility.*;
import java.nio.channels.*;

// JavaMIDI
import javax.sound.midi.*;

import javax.swing.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class PicnomeCommunication implements PicnomeSystems {
  private static final String APP_VERSION = "1.5.0";
  private static final int MAX_CONNECTABLE_NUM = 2;
  private static final int MAX_ADCON_NUM = 11;

  private OSCReader oscr;

  private String fwver = "";
  private boolean fwver_flag = false;

  private Vector<String> device_list = new Vector<String>();
  private Vector<String> midiinput_list = new Vector<String>();
  private Vector<String> midioutput_list = new Vector<String>();
  private List<MidiDevice.Info> midiinputdevices = new ArrayList<MidiDevice.Info>();
  private List<MidiDevice.Info> midioutputdevices = new ArrayList<MidiDevice.Info>();

  public JButton mididetail_b;
  public JButton led_clear_b;
  public JButton led_test_b;
  public JButton hex_b;
  public JButton update_b;
  public JComboBox protocol_cb;
  public JComboBox device_cb;
  public JComboBox cable_cb;
  public JComboBox midiinput_cb;
  public JComboBox midioutput_cb;
  public JComboBox midiparameter_cb;
  public JTextField hostaddress_tf;
  public JTextField prefix_tf;
  public JTextField hostport_tf;
  public JTextField listenport_tf;
  public JTextField hex_tf;
  public JSpinner intensity_s;
  public JSpinner startcolumn_s;
  public JSpinner startrow_s;
  public JCheckBox[] adc_ck = new JCheckBox[MAX_ADCON_NUM];
  public JComboBox[] adc_cmb0 = new JComboBox[MAX_ADCON_NUM];
  public JComboBox[] adc_cmb1 = new JComboBox[MAX_ADCON_NUM];
  public JProgressBar update_pb;

  private int current_picnome_num;
  private int msg_index = 0;
  private CommPortIdentifier[] portId = new CommPortIdentifier[MAX_CONNECTABLE_NUM];
  private SerialPort[] port = new SerialPort[MAX_CONNECTABLE_NUM];
  private InputStream[] in = new InputStream[MAX_CONNECTABLE_NUM];
  private OutputStream[] out = new OutputStream[MAX_CONNECTABLE_NUM];

  private OSCPortIn oscpin;
  private OSCPortOut oscpout;

  private MidiDevice[] midiin = new MidiDevice[MAX_CONNECTABLE_NUM];
  private MidiDevice[] midiout = new MidiDevice[MAX_CONNECTABLE_NUM];
  private Receiver[] midi_r = new Receiver[MAX_CONNECTABLE_NUM];
  private Transmitter[] midi_t = new Transmitter[MAX_CONNECTABLE_NUM];
  private int midi_pgm_number;

  private String[] device = new String[MAX_CONNECTABLE_NUM];
  private String[] device2 = new String[MAX_CONNECTABLE_NUM];
  private String[] protocol_type = new String[MAX_CONNECTABLE_NUM];
  private String[] host_address = new String[MAX_CONNECTABLE_NUM];
  private String[] host_port = new String[MAX_CONNECTABLE_NUM];
  private String[] listen_port = new String[MAX_CONNECTABLE_NUM];
  private String[] connect_state = new String[MAX_CONNECTABLE_NUM];
  private String[] ledtest_state = new String[MAX_CONNECTABLE_NUM];
  private String[] cable_orientation = new String[MAX_CONNECTABLE_NUM];
  private String[] address_pattern_prefix = new String[MAX_CONNECTABLE_NUM];
  private int[] midi_in = new int[MAX_CONNECTABLE_NUM];
  private int[] midi_out = new int[MAX_CONNECTABLE_NUM];
  private int[] intensity = new int[MAX_CONNECTABLE_NUM];
  private int[] starting_column = new int[MAX_CONNECTABLE_NUM];
  private int[] starting_row = new int[MAX_CONNECTABLE_NUM];
  private int[] co_max_num = new int[MAX_CONNECTABLE_NUM];
  private boolean[][] adc_enable = new boolean[MAX_CONNECTABLE_NUM][MAX_ADCON_NUM];
  private int[][] adc_type = new int[MAX_CONNECTABLE_NUM][MAX_ADCON_NUM];
  private int[][] adc_curve = new int[MAX_CONNECTABLE_NUM][MAX_ADCON_NUM];
  private int[][][] midi_parameter = new int[16][8][3];

  private int count_ma = 0;
  private int[] atb = new int[MAX_ADCON_NUM];
  private double[][] atb_box = new double[MAX_ADCON_NUM][32];

  private StringBuilder msgled_buf = new StringBuilder("l" + 0 + (char)0 + (char)0 + (char)0x0D);

  private boolean isPrB = false;

  public PicnomeCommunication() {
    current_picnome_num = 0;
    initDeviceList();
    for(int i = 0; i < MAX_CONNECTABLE_NUM; i++) {
      in[i] = null;
      out[i] = null;
      protocol_type[i] = "Open Sound Control";
      midi_in[i] = 0;
      midi_out[i] = 0;
      host_address[i] = "127.0.0.1";
      host_port[i] = "8000";
      listen_port[i] = "8080";
      connect_state[i] = "Open";
      ledtest_state[i] = "LED Test On";
      cable_orientation[i] = "left";
      address_pattern_prefix[i] = "/test";
      intensity[i] = 15;
      starting_column[i] = 0;
      starting_row[i] = 0;
      co_max_num[i] = 7;
      for(int j = 0; j < MAX_ADCON_NUM; j++) {
        adc_enable[i][j] = false;
      }
    }
    initMIDIPort();
  }

  public boolean getIsPrB() {
    return isPrB;
  }

  public void enableAllAdcPorts() {
    for(int i = 0; i < 11; i++) {
      String str =new String("ae " + i + " " + 1 + (char)0x0D);
      adc_ck[i].setSelected(true);
      adc_cmb0[i].setEnabled(true);
      if(adc_cmb0[i].getSelectedIndex() < 2)
        adc_cmb1[i].setEnabled(true);
      
      if(((String)device_cb.getSelectedItem()).equals(getCurrentDevice(0))) {
        setAdcEnable(0, i, true);
        sendDataToSerial(0, str);
      }
      else if(((String)device_cb.getSelectedItem()).equals(getCurrentDevice(1))) {
        setAdcEnable(1, i, true);
        sendDataToSerial(1, str);
      }

      if(i != 1)
        adc_cmb0[i].setSelectedIndex(1);
      adc_cmb1[i].setSelectedIndex(2);
    }
  }

  private List<String> getUsbInfo(String name) {
    String id = "none";
    String iousbdevices = new String();
    List<String> sfx = new ArrayList<String>();

    if(System.getProperty("os.name").startsWith("Mac OS X")) {
      try {
        ProcessBuilder pb = new ProcessBuilder("ioreg", "-w", "0", "-S", "-p", "IOUSB", "-n", name, "-r");
        Process p = pb.start();
        InputStream is = p.getInputStream();

        int c;
        while((c = is.read()) != -1)
          iousbdevices += (new Character((char)c)).toString();
        is.close();
      }catch(IOException e) {}
      
      List<String> vid = new ArrayList<String>();
      List<String> pid = new ArrayList<String>();

      while(iousbdevices.indexOf(name) != -1) {
        int pos_start = iousbdevices.indexOf(name);
        int pos_end = iousbdevices.indexOf(" }");
        String iousbdevice = iousbdevices.substring(pos_start, pos_end);
        
        if(iousbdevice.indexOf(name + "@") != -1) {
          pos_start = iousbdevice.indexOf(name + "@") + name.length() + 1;
          pos_end = iousbdevice.indexOf("00");
          if(pos_start != -1)
            id = iousbdevice.substring(pos_start, pos_end);
          sfx.add(id);
        }

        if((pos_start = iousbdevice.indexOf("idVendor")) != -1) {
          pos_end = iousbdevice.length();
          
          iousbdevice = iousbdevice.substring(pos_start, pos_end);
          pos_end = iousbdevice.indexOf("\n");
          id = iousbdevice.substring(iousbdevice.indexOf(" = ") + 3, pos_end);
          vid.add(id);
        }

        iousbdevice = iousbdevices.substring(iousbdevices.indexOf(name), iousbdevices.indexOf(" }"));
        if((pos_start = iousbdevice.indexOf("idProduct")) != -1) {
          pos_end = iousbdevice.length();
          
          iousbdevice = iousbdevice.substring(pos_start, pos_end);
          pos_end = iousbdevice.indexOf("\n");
          id = iousbdevice.substring(iousbdevice.indexOf(" = ") + 3, pos_end);
          pid.add(id);
        }
        iousbdevices = iousbdevices.substring(iousbdevices.indexOf(" }") + 2, iousbdevices.length());
        
        if(!(vid.get(vid.size() - 1).equals("1240") && (pid.get(pid.size() - 1).equals("65477") || pid.get(pid.size() - 1).equals("64768") || pid.get(pid.size() - 1).equals("63865")))) {
          sfx.remove(sfx.size() - 1);
          vid.remove(vid.size() - 1);
          pid.remove(pid.size() - 1);
        }
      }
    }
    else if(System.getProperty("os.name").startsWith("Windows")) {
      try {
        ProcessBuilder pb = new ProcessBuilder("powercfg", "/devicequery", "all_devices");
        Process p = pb.start();
        InputStream is = p.getInputStream();

        int c;
        while((c = is.read()) != -1)
          iousbdevices += (new Character((char)c)).toString();
        is.close();
      }catch(IOException e) {}

      while(iousbdevices.indexOf(name) != -1) {
        int pos_start = iousbdevices.indexOf(name);
        iousbdevices = iousbdevices.substring(pos_start, iousbdevices.length());
        int pos_end = iousbdevices.indexOf(")");
        String iousbdevice = iousbdevices.substring(0, pos_end + 1);

        pos_start = iousbdevice.indexOf("(");
        pos_end = iousbdevice.indexOf(")");
        id = iousbdevice.substring(pos_start + 1, pos_end);

        if((iousbdevice.indexOf("PICnome128") == -1 && name.indexOf("PICnome128") == -1) ||
           (iousbdevice.indexOf("PICnome128") != -1 && name.indexOf("PICnome128") != -1))
          sfx.add(id);

        iousbdevices = iousbdevices.substring(iousbdevices.indexOf(")") + 2, iousbdevices.length());
      }
    }
    return sfx;
  }

  private void initDeviceList() {
    if(System.getProperty("os.name").startsWith("Mac OS X")) {
      String device_name;
      List<String> suffix0 = getUsbInfo("IOUSBDevice");
      List<String> suffix1 = getUsbInfo("PICnome");
      List<String> suffix2 = getUsbInfo("PICnome128");
      List<String> suffix3 = getUsbInfo("PICratchBOX");
      Enumeration e = CommPortIdentifier.getPortIdentifiers();

      for(int i = 0; i < MAX_CONNECTABLE_NUM; i++) {
        device[i] = "";
        device2[i] = "";
      }
      while(e.hasMoreElements()) {
        device_name = ((CommPortIdentifier)e.nextElement()).getName();
        if(device_name.indexOf("/dev/cu.usbmodem") != -1) {
          for(String s0str: suffix0) {
            if(device_name.indexOf(s0str) != -1) {
              if(current_picnome_num >= MAX_CONNECTABLE_NUM)
                break;
              device[current_picnome_num] = "tkrworks-PICnome64-" + s0str;
              device2[current_picnome_num] = device_name;
              current_picnome_num++;
              device_list.add("tkrworks-PICnome64-" + s0str);
            }
          }
          for(String s3str: suffix3) {//for picratchbox
            if(device_name.indexOf(s3str) != -1) {
              if(current_picnome_num >= MAX_CONNECTABLE_NUM)
                break;
              device[current_picnome_num] = "tkrworks-PICratchBOX-" + s3str;
              device2[current_picnome_num] = device_name;
              current_picnome_num++;
              device_list.add("tkrworks-PICratchBOX-" + s3str);
              isPrB = true;
            }
          }
          for(String s2str: suffix2) {//for one twenty eight
            if(device_name.indexOf(s2str) != -1) {
              if(current_picnome_num >= MAX_CONNECTABLE_NUM)
                break;
              device[current_picnome_num] = "tkrworks-PICnome128-" + s2str;
              device2[current_picnome_num] = device_name;
              current_picnome_num++;
              device_list.add("tkrworks-PICnome128-" + s2str);
            }
          }
          for(String s1str: suffix1) {//for sixty four
            if(device_name.indexOf(s1str) != -1) {
              if(current_picnome_num >= MAX_CONNECTABLE_NUM)
                break;
              device[current_picnome_num] = "tkrworks-PICnome64-" + s1str;
              device2[current_picnome_num] = device_name;
              current_picnome_num++;
              device_list.add("tkrworks-PICnome64-" + s1str);
            }
          }
        }
      }
    }
    else if(System.getProperty("os.name").startsWith("Windows")) {
      int dev_num = 0;
      String device_name;
      List<String> comport0 = this.getUsbInfo("tkrworks PICnome");
      List<String> comport1 = this.getUsbInfo("tkrworks PICnome128");
      List<String> comport2 = this.getUsbInfo("tkrworks PICratchBOX");
      Enumeration e = CommPortIdentifier.getPortIdentifiers();

      for(int i = 0; i < MAX_CONNECTABLE_NUM; i++) {
        device[i] = "";
        device2[i] = "";
      }

      while(e.hasMoreElements()) {
        device_name = ((CommPortIdentifier)e.nextElement()).getName();
        
        for(String c0str: comport0) {
          if(device_name.indexOf(c0str) != -1) {
            if(current_picnome_num >= MAX_CONNECTABLE_NUM)
              break;
            device[current_picnome_num] = "tkrworks-PICnome64-" + c0str;
            device2[current_picnome_num] = device_name;
            current_picnome_num++;
            device_list.add("tkrworks-PICnome64-" + c0str);
          }
        }
        for(String c1str: comport1) {
          if(device_name.indexOf(c1str) != -1) {
            if(current_picnome_num >= MAX_CONNECTABLE_NUM)
              break;
            device[current_picnome_num] = "tkrworks-PICnome128-" + c1str;
            device2[current_picnome_num] = device_name;
            current_picnome_num++;
            device_list.add("tkrworks-PICnome128-" + c1str);
          }
        }
        for(String c2str: comport2) {
          if(device_name.indexOf(c2str) != -1) {
            if(current_picnome_num >= MAX_CONNECTABLE_NUM)
              break;
            device[current_picnome_num] = "tkrworks-PICratchBOX-" + c2str;
            device2[current_picnome_num] = device_name;
            current_picnome_num++;
            device_list.add("tkrworks-PICratchBOX-" + c2str);
          }
        }
      }
    }
  }

  public void changeDeviceSettings(int index) {
    protocol_type[1 - index] = (String)protocol_cb.getSelectedItem();
    midi_in[1 - index] = midiinput_cb.getSelectedIndex();
    midi_out[1 - index] = midioutput_cb.getSelectedIndex();
    host_address[1 - index] = hostaddress_tf.getText();
    host_port[1 - index] = hostport_tf.getText();
    listen_port[1 - index] = listenport_tf.getText();
    ledtest_state[1 - index] = led_test_b.getText();
    for(int i = 0; i < 6; i++) {
      adc_enable[1 - index][i] = adc_ck[i].isSelected();
      adc_type[1 - index][i] = adc_cmb0[i].getSelectedIndex();
      adc_curve[1 - index][i] = adc_cmb1[i].getSelectedIndex();
    }

    protocol_cb.setSelectedItem(protocol_type[index]);
    midiinput_cb.setSelectedIndex(midi_in[index]);
    midioutput_cb.setSelectedIndex(midi_out[index]);
    hostaddress_tf.setText(host_address[index]);
    hostport_tf.setText(host_port[index]);
    listenport_tf.setText(listen_port[index]);
    led_test_b.setText(ledtest_state[index]);
    cable_cb.setSelectedItem(cable_orientation[index]);
    prefix_tf.setText(address_pattern_prefix[index]);
    intensity_s.setValue(intensity[index]);
    startcolumn_s.setValue(starting_column[index]);
    startrow_s.setValue(starting_row[index]);
    for(int i = 0; i < 6; i++) {
      adc_ck[i].setSelected(adc_enable[index][i]);
      if(adc_ck[i].isSelected())
        adc_cmb0[i].setEnabled(true);
      else
        adc_cmb0[i].setEnabled(false);
      adc_cmb0[i].setSelectedIndex(adc_type[index][i]);
      if(adc_cmb0[i].getSelectedIndex() < 2)
        adc_cmb1[i].setEnabled(true);
      else
        adc_cmb1[i].setEnabled(false);
      adc_cmb1[i].setSelectedIndex(adc_curve[index][i]);
    }

    for(int i = 0; i < MAX_ADCON_NUM; i++)
      adc_ck[i].setSelected(adc_enable[index][i]);
  }

  public boolean openSerialPort(int index) {
    try {
      if(System.getProperty("os.name").startsWith("Mac OS X")) {
        if(device[index].indexOf("PICnome128") != -1)
          co_max_num[index] = 15;
        else
          co_max_num[index] = 7;
        portId[index] = CommPortIdentifier.getPortIdentifier(device2[index]);
      }
      else if(System.getProperty("os.name").startsWith("Windows")) {
        if(device[index].indexOf("PICnome128") != -1) {
          co_max_num[index] = 15;
          portId[index] = CommPortIdentifier.getPortIdentifier(device2[index]);

        }
        else {
          co_max_num[index] = 7;
          portId[index] = CommPortIdentifier.getPortIdentifier(device2[index]);
        }
      }
      port[index] = (SerialPort)portId[index].open("PICnomeSerial", 2000);

      in[index] = port[index].getInputStream();
      out[index] = port[index].getOutputStream();
      (new Thread(new SerialReader(index, in[index]))).start();

      initOSCPort(index);
      initOSCListener();
      openMIDIPort(index);
    }
    catch (NoSuchPortException e) {
      e.printStackTrace();
      return false;
    }
    catch (PortInUseException e) {
      e.printStackTrace();
      return false;
    }
    catch(IOException e) {
      e.printStackTrace();
      return false;
    }
    catch (NullPointerException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public boolean setSerialPort(int index) {
    try {
      if(System.getProperty("os.name").startsWith("Mac OS X"))
        //sy port[index].setSerialPortParams(230400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        port[index].setSerialPortParams(460800, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
      else if(System.getProperty("os.name").startsWith("Windows"))
        //sy port[index].setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        port[index].setSerialPortParams(256000, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
      port[index].setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
    
      port[index].setDTR(true);
      port[index].setRTS(false);
      
      //check firmware version
      sendDataToSerial(index, new String("f" + (char)0x0D));
    }
    catch (UnsupportedCommOperationException e) {
      e.printStackTrace();
      return false;
    }
    catch (NullPointerException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public boolean closeSerialPort(int index) {
    try {
      in[index].close();
      out[index].flush();
      out[index].close();
      port[index].close();
    }
    catch(Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public void initOSCPort(int index) {
    byte[] hostaddress = new byte[4];
    String ha_str = host_address[index];

    int idx = 0, idx2;
    for(int i = 0; i < 3; i++) {
      idx2 = ha_str.indexOf(".", idx);
      hostaddress[i] = Byte.parseByte(ha_str.substring(idx, idx2));
      idx = idx2 + 1;
    }
    hostaddress[3] = Byte.parseByte(ha_str.substring(idx, ha_str.length()));

    try {
      oscr = new OSCReader(Integer.valueOf(listen_port[index]));
      //sy (new Thread(new OSCReader(Integer.valueOf(listen_port[index])))).start();
      (new Thread(oscr)).start();
      oscpout = new OSCPortOut(InetAddress.getByAddress(hostaddress), Integer.valueOf(host_port[index]));
    }
    catch(IOException ioe){}
  }

  public void setOSCHostInfo(int index, String newHostPort, String newListenPort) {
    byte[] hostaddress = new byte[4];
    String ha_str = hostaddress_tf.getText();

    int idx = 0, idx2;
    for(int i = 0; i < 3; i++) {
      idx2 = ha_str.indexOf(".", idx);
      hostaddress[i] = Byte.parseByte(ha_str.substring(idx, idx2));
      idx = idx2 + 1;
    }
    hostaddress[3] = Byte.parseByte(ha_str.substring(idx, ha_str.length()));

    try {
      host_port[index] = newHostPort;
      listen_port[index] = newListenPort;
      //debug System.out.println(host_port[index] + " " + listen_port[index]);
      hostport_tf.setText(host_port[index]);
      listenport_tf.setText(listen_port[index]);
      //debug System.out.println("oscr.setPort");
      oscr.setPort(Integer.valueOf(listen_port[index]));
      oscpout = new OSCPortOut(InetAddress.getByAddress(hostaddress), Integer.valueOf(host_port[index]));
    }
    catch(UnknownHostException uhe) {}
    catch(SocketException se) {}
  }

  //sy MIDI Setup
  private void initMIDIPort() {
    MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
    MidiDevice device;
    for(MidiDevice.Info info : infos) {
      try {
        device = MidiSystem.getMidiDevice(info);
      }
      catch(MidiUnavailableException e) {
        e.printStackTrace();
        continue;
      }
      if(device.getMaxTransmitters() == 0)
        continue;
      midiinput_list.add(info.getName());
      midiinputdevices.add(info);
    }

    for(MidiDevice.Info info : infos) {
      try {
        device = MidiSystem.getMidiDevice(info);
      }
      catch(MidiUnavailableException e) {
        e.printStackTrace();
        continue;
      }
      if(device.getMaxReceivers() == 0)
        continue;
      midioutput_list.add(info.getName());
      midioutputdevices.add(info);
    }
  }

  public void openMIDIPort(int index) {
    try {
      midiin[index] = MidiSystem.getMidiDevice(midiinputdevices.get(midi_in[index]));
      if(midiin[index].isOpen())
        midiin[index].close();
      midiin[index].open();
      midi_t[index] = midiin[index].getTransmitter();
      enableMidiLed(index);

      midiout[index] = MidiSystem.getMidiDevice(midioutputdevices.get(midi_out[index]));
      if(midiout[index].isOpen())
        midiout[index].close();
      midiout[index].open();
      midi_r[index] = midiout[index].getReceiver();
    }
    catch(MidiUnavailableException mue){}
  }

  public Vector<String> getDeviceList() {
    return device_list;
  }

  public int getCurrentNum() {
    return current_picnome_num;
  }

  public String getCurrentDevice(int index) {
    return device[index];
  }

  public void setCurrentProtocol(int index, String name) {
    protocol_type[index] = name;
  }

  public void setCurrentMidiIn(int index, int id) {
    midi_in[index] = id;
  }

  public void setCurrentMidiOut(int index, int id) {
    midi_out[index] = id;
  }

  public Vector<String> getMidiInputList() {
    return midiinput_list;
  }

  public Vector<String> getMidiOutputList() {
    return midioutput_list;
  }

  public int getCurrentMidiIn(int index) {
    return midi_in[index];
  }

  public int getCurrentMidiOut(int index) {
    return midi_out[index];
  }

  public int getMidiNoteChannel(int nx, int ny) {
    return midi_parameter[nx][ny][0];
  }

  public int getMidiNoteOnVelocity(int nx, int ny) {
    return midi_parameter[nx][ny][1];
  }

  public int getMidiNoteOffVelocity(int nx, int ny) {
    return midi_parameter[nx][ny][2];
  }

  public void setMidiNoteChannel(int nx, int ny, int ch) {
    midi_parameter[nx][ny][0] = ch;
  }

  public void setMidiNoteOnVelocity(int nx, int ny, int vel) {
    midi_parameter[nx][ny][1] = vel;
  }

  public void setMidiNoteOffVelocity(int nx, int ny, int vel) {
    midi_parameter[nx][ny][2] = vel;
  }

  public void setCurrentCable(int index, String orientation) {
    cable_orientation[index] = orientation;
  }

  public void setCurrentStartingColumn(int index, int column) {
    starting_column[index] = column;
  }

  public void setCurrentStartingRow(int index, int row) {
    starting_row[index] = row;
  }

  public int getCurrentMaxColumn(int index) {
    return co_max_num[index];
  }

  public int getCurrentIntensity(int index) {
    return intensity[index];
  }

  public void setCurrentIntensity(int index, int value) {
    intensity[index] = value;
  }

  public void setAdcEnable(int index0, int index1, boolean b) {
    adc_enable[index0][index1] = b;
  }

  public String getAppVersion() {
    return APP_VERSION;
  }

  public int getMaxAnalogNum() {
    return MAX_ADCON_NUM;
  }

  public boolean isFirmwareVersion() {
    return fwver_flag;
  }

  public String getFirmwareVersion() {
    return fwver;
  }

  public boolean checkPortState(int index) {
    if(portId[index] != null && portId[index].isCurrentlyOwned())
      return true;
    else
      return false;
  }

  public void sendDataToSerial(int index, int data) {
    try {
      out[index].write(data);
    }
    catch(IOException e) {}
  }

  public void sendDataToSerial(int index, String str) {
    try {
      if(str == null )
        return;

      out[index].write(str.getBytes());
    }
    catch(IOException e) {}
  }

  public void sendDataToSerial(int index, String[] str) {
    try {
      if(str == null )
        return;

      for(String str0 : str)
        out[index].write(str0.getBytes());
    }
    catch(IOException e) {}
  }

  private int getHexStringToInt(String str) {
    int value = 0;
    int c = str.charAt(0);
    if(c >= 65)
      value = (c - 65) + 10;
    else
      value = c - 48;
    return value;
  }

  private void sendOSCMessageFromHw(int index, int[] chs) {
    Object[] args;
    OSCMessage msg;

    if((char)chs[0] == 'p' || (char)chs[0] == 'r') {

      if(protocol_type[index].equals("Open Sound Control") || protocol_type[index].equals("OSC/MIDI(ext.)")) {
        args = new Object[3];

        int x0 = chs[1] & 0x0F;
        int y0 = (chs[1] >> 4) & 0x0F;

        int sc = starting_column[index];
        int sr = starting_row[index];

        if(cable_orientation[index].equals("left")) {
          args[0] = x0 + sc; // X
          args[1] = y0 + sr; // Y
        }
        else if(cable_orientation[index].equals("right")) {
          args[0] = co_max_num[index] - x0 + sc; // X
          args[1] = 7 - y0 + sr; // Y
        }
        else if(cable_orientation[index].equals("up")) {
          args[1] = co_max_num[index] - x0 + sr; // Y
          args[0] = y0 + sc;     // X
        }
        else if(cable_orientation[index].equals("down")) {
          args[1] = x0 + sr;     // Y
          args[0] = 7 - y0 + sc; // X
        }

        if((char)chs[0] == 'p')
          args[2] = 1; // State
        else if((char)chs[0] == 'r')
          args[2] = 0; // State

        msg = new OSCMessage(address_pattern_prefix[index] + "/press", args);

        try {
          oscpout.send(msg);
        }
        catch(IOException e) {
          e.printStackTrace();
          return;
        }
        catch(NullPointerException e) {
          e.printStackTrace();
          return;
        }
      }
      else if(protocol_type[index].equals("DORAnome")) {// for DORAnome
        int x0 = chs[1] & 0x0F;
        int y0 = (chs[1] >> 4) & 0x0F;

        // send MIDI note
        if(x0 == 11) {
          int note_number = y0 + 1;
          try {
            ShortMessage sm = new ShortMessage();
            if((char)chs[0] == 'p')
              sm.setMessage(ShortMessage.NOTE_ON, 1,// MIDI ch.2
                            (byte)note_number, 127);
            else if((char)chs[0] == 'r')
              sm.setMessage(ShortMessage.NOTE_OFF, 1,// MIDI ch.2
                            (byte)note_number, 0);
            midi_r[index].send(sm, 1);
          } catch(InvalidMidiDataException imde) {}
        }
        else if(x0 == 10) {
          int note_number = y0 + 1;
          try {
            ShortMessage sm = new ShortMessage();
            if((char)chs[0] == 'p')
              sm.setMessage(ShortMessage.NOTE_ON, 2,// MIDI ch.3
                            (byte)note_number, 127);
            else if((char)chs[0] == 'r')
              sm.setMessage(ShortMessage.NOTE_OFF, 2,// MIDI ch.3
                            (byte)note_number, 0);
            midi_r[index].send(sm, 1);
          } catch(InvalidMidiDataException imde) {}
        }
        else if((x0 == 12 && y0 == 2) || (x0 == 13 && y0 == 2) || (x0 == 12 && y0 == 3) || (x0 == 13 && y0 == 3)) {
          int note_number = (y0 + 7) + 2 * (x0 - 12);
          try {
            ShortMessage sm = new ShortMessage();
            if((char)chs[0] == 'p')
              sm.setMessage(ShortMessage.NOTE_ON, 1,// MIDI ch.2
                            (byte)note_number, 127);
            else if((char)chs[0] == 'r')
              sm.setMessage(ShortMessage.NOTE_OFF, 1,// MIDI ch.2
                            (byte)note_number, 0);
            midi_r[index].send(sm, 1);
          } catch(InvalidMidiDataException imde) {}
        }

        args = new Object[3];

        int sc = starting_column[index];
        int sr = starting_row[index];

        if(cable_orientation[index].equals("left")) {
          args[0] = x0 + sc; // X
          args[1] = y0 + sr; // Y
        }
        else if(cable_orientation[index].equals("right")) {
          args[0] = co_max_num[index] - x0 + sc; // X
          args[1] = 7 - y0 + sr; // Y
        }
        else if(cable_orientation[index].equals("up")) {
          args[1] = co_max_num[index] - x0 + sr; // Y
          args[0] = y0 + sc;     // X
        }
        else if(cable_orientation[index].equals("down")) {
          args[1] = x0 + sr;     // Y
          args[0] = 7 - y0 + sc; // X
        }
        if((char)chs[0] == 'p')
          args[2] = 1; // State
        else if((char)chs[0] == 'r')
          args[2] = 0; // State
          
        msg = new OSCMessage(address_pattern_prefix[index] + "/press", args);
        try {
          oscpout.send(msg);
        }
        catch(IOException e) {
          e.printStackTrace();
          return;
        }
        catch(NullPointerException e) {
          e.printStackTrace();
          return;
        }
      }
      else {// for MIDI
        int notex = chs[1] & 0x0F;
        int notey = (chs[1] >> 4) & 0x0F;
        int note_number = notex + (notey * (co_max_num[index] + 1));

        try {
          ShortMessage sm = new ShortMessage();
          if((char)chs[0] == 'p')
            sm.setMessage(ShortMessage.NOTE_ON, getMidiNoteChannel(notex, notey),
                          (byte)note_number, getMidiNoteOnVelocity(notex, notey));
          else if((char)chs[0] == 'r')
            sm.setMessage(ShortMessage.NOTE_OFF, getMidiNoteChannel(notex, notey),
                          (byte)note_number, getMidiNoteOffVelocity(notex, notey));
          midi_r[index].send(sm, 1);
        } catch(InvalidMidiDataException imde) {}
      }
    }
    else if((char)chs[0] == 'a') {
      float f = 0.0f;
      double f1 = 0.0;
      int adc_id = chs[1] >> 4;
      int ac0 = adc_cmb0[adc_id].getSelectedIndex();
      double ac1 = adc_cmb1[adc_id].getSelectedIndex();

      if(!adc_ck[adc_id].isSelected())
        return;

      atb[adc_id] = (((chs[1] & 0x03) << 8) + chs[2]);

      if(atb[adc_id] > 1000) atb[adc_id] = 1023;
      else if(atb[adc_id] < 9) atb[adc_id] = 0;

      //debug System.out.printf("%d = %d%n", adc_id, atb[adc_id]);

      if(ac0 == 0) {//IF
        atb[adc_id] = atb[adc_id] >> 3;
        f = (float)(Math.pow(atb[adc_id] / 127.0, Math.pow(2.0, (2.0 * ac1) - 4.0)));
      }
      else if(ac0 == 1) {//CF
        atb[adc_id] = atb[adc_id] >> 3;
        f1 = atb[adc_id] * 1.008;
        if(f1 < 2.0)
          f1 = 0.0;
        else if(f1 > 127.0)
          f1 = 127.0;
        if(atb[adc_id] < 64)
          //test f = (float)(0.5 * Math.pow(atb[adc_id] / 64.0, Math.pow(2.0, (2.0 * ac1) - 8.0)));
          f = (float)(0.5 * Math.pow(f1 / 64.0, Math.pow(2.0, (2.0 * ac1) - 8.0)));
        else
          //test f = (float)(1.0 - (0.5 * Math.pow((127.0 - atb[adc_id]) / 64.0, Math.pow(2.0, (2.0 * ac1) - 8.0))));
          f = (float)(1.0 - (0.5 * Math.pow((127.0 - f1) / 64.0, Math.pow(2.0, (2.0 * ac1) - 8.0))));
      }
      else {
        atb_box[adc_id][count_ma] = atb[adc_id];
        double sum = 0;
        for(int i = 0; i < 32; i++)
          sum += atb_box[adc_id][i];
        f = (float)((sum / 32.0) / 1023.0);
        count_ma++;
        if(count_ma > 31)
          count_ma = 0;
      }
      if(protocol_type[index].equals("Open Sound Control")) {
        args = new Object[2];
        args[0] = adc_id;
        args[1] = f;//sy (float)(((int)(f * 250.0)) / 250.0);
        if(getIsPrB()) {
          if(adc_id < 3) {
            if(adc_id == 1)
              args[0] = 2;
            else if(adc_id == 2)
              args[0] = 1;
            msg = new OSCMessage(address_pattern_prefix[index] + "/fader", args);
          }
          else {
            Object[] argsv = new Object[3];
            if(adc_id < 7) {
              argsv[0] = 1;
              argsv[1] = 3 - (adc_id - 3);
            }
            else {
              argsv[0] = 0;
              argsv[1] = 3 - (adc_id - 7);
            }
            argsv[2] = 1.0f - f;
            msg = new OSCMessage(address_pattern_prefix[index] + "/vol", argsv);
          }
        }
        else
          msg = new OSCMessage(address_pattern_prefix[index] + "/adc", args);
        try {
          oscpout.send(msg);
        }
        catch(IOException e) {
          e.printStackTrace();
          return;
        }
        catch(NullPointerException e) {
          e.printStackTrace();
          return;
        }
      }
      else if(protocol_type[index].equals("DORAnome")) {// for DORAnome
        if(adc_id < 3) {
          args = new Object[2];
          args[0] = adc_id;
          args[1] = f;//sy (float)(((int)(f * 250.0)) / 250.0);
          msg = new OSCMessage(address_pattern_prefix[index] + "/adc", args);
          try {
            oscpout.send(msg);
          }
          catch(IOException e) {
            e.printStackTrace();
            return;
          }
          catch(NullPointerException e) {
            e.printStackTrace();
            return;
          }
        }
        else {
          try {
            ShortMessage sm = new ShortMessage();
            if(protocol_type[index].equals("DORAnome"))
              sm.setMessage(ShortMessage.CONTROL_CHANGE, 1, adc_id, (int)(f * 127));
            else
              sm.setMessage(ShortMessage.CONTROL_CHANGE, 0, adc_id, (int)(f * 127));
            midi_r[index].send(sm, 1);
          } catch(InvalidMidiDataException imde){}
        }
      }
      else {// for MIDI
        try {
          ShortMessage sm = new ShortMessage();
          if(protocol_type[index].equals("DORAnome"))
            sm.setMessage(ShortMessage.CONTROL_CHANGE, 1, adc_id, (int)(f * 127));
          else
            sm.setMessage(ShortMessage.CONTROL_CHANGE, 0, adc_id, (int)(f * 127));
          midi_r[index].send(sm, 1);
        } catch(InvalidMidiDataException imde){}
      }
    }
  }

  private void sendOSCMessageFromHw(int index, String str) {
    StringTokenizer st = new StringTokenizer(str);
    Object[] args;
    OSCMessage msg;
    String token = st.nextToken();

    if(str.substring(0, 1).equals("p") || str.substring(0, 1).equals("r")) {
      if(((String)protocol_cb.getSelectedItem()).equals("Open Sound Control")) {
        args = new Object[3];

        int x0 = getHexStringToInt(str.substring(1, 2));
        int y0 = getHexStringToInt(str.substring(2, 3));
        int state0 = -1;
        if(str.substring(0, 1).equals("p"))
          state0 = 1;
        else if(str.substring(0, 1).equals("r"))
          state0 = 0;

        int sc = starting_column[index];
        int sr = starting_row[index];

        if(cable_orientation[index].equals("left")) {
          args[0] = x0 + sc; // X
          args[1] = y0 + sr; // Y
        }
        else if(cable_orientation[index].equals("right")) {
          args[0] = co_max_num[index] - x0 + sc; // X
          args[1] = 7 - y0 + sr; // Y
        }
        else if(cable_orientation[index].equals("up")) {
          args[1] = co_max_num[index] - x0 + sr; // Y
          args[0] = y0 + sc;     // X
        }
        else if(cable_orientation[index].equals("down")) {
          args[1] = x0 + sr;     // Y
          args[0] = 7 - y0 + sc; // X
        }
        args[2] = state0; // State

        msg = new OSCMessage(address_pattern_prefix[index] + "/press", args);
        try {
          oscpout.send(msg);
        }
        catch(IOException e){}
      }
      else {// for MIDI
        int notex = getHexStringToInt(str.substring(1, 2));
        int notey = getHexStringToInt(str.substring(2, 3));
        int state = -1;
        if(str.substring(0, 1).equals("p"))
          state = 1;
        else if(str.substring(0, 1).equals("r"))
          state = 0;

        int note_number = notex + (notey * (co_max_num[index] + 1));

        try {
          ShortMessage sm = new ShortMessage();
          if(state == 1)
            sm.setMessage(ShortMessage.NOTE_ON, getMidiNoteChannel(notex, notey),
                          (byte)note_number, getMidiNoteOnVelocity(notex, notey));
          else
            sm.setMessage(ShortMessage.NOTE_OFF, getMidiNoteChannel(notex, notey),
                          (byte)note_number, getMidiNoteOffVelocity(notex, notey));
          midi_r[index].send(sm, 1);
        }
        catch(InvalidMidiDataException imde) {}
      }
    }
    else if(token.equals("adc")) {
      if(((String)protocol_cb.getSelectedItem()).equals("Open Sound Control")) {
        args = new Object[2];

        args[0] = Integer.parseInt(st.nextToken()); // Pin
        args[1] = Float.parseFloat(st.nextToken());   // Value

        msg = new OSCMessage(address_pattern_prefix[index] + "/adc", args);

        try {
          oscpout.send(msg);
        }
        catch(IOException e){}
      }
/*
      else//for MIDI
      {
        int ctrl_number = Integer.parseInt(st.nextToken()); // Pin
        int ctrl_value = (int)(127.0 * Float.parseFloat(st.nextToken()));   // Value
        Controller cc = new Controller(ctrl_number, ctrl_value);

        //sy midiout.sendController(new Controller(ctrl_number, ctrl_value));
        midiout[0].sendController(new Controller(ctrl_number, ctrl_value));
      }
*/
    }
    else if(token.equals("report")) {
      int v1,v2;

      v1 = Integer.parseInt(st.nextToken());
      v2 = Integer.parseInt(st.nextToken());

      if(v2 == 1) {
        hex_tf.setEnabled(false);
        hex_b.setEnabled(false);
        update_b.setEnabled(false);
      }
    }
  }

  private String controlMsgLed(int index, OSCMessage message) {
    String address = message.getAddress();
    Object[] args = message.getArguments();

    //sy0 if(args.length < 3)
    if(message.getArgumentsLength() < 3)
      return null;

    int args0 = (int)Float.parseFloat(args[0].toString());
    int args1 = (int)Float.parseFloat(args[1].toString());
    int args2 = (int)Float.parseFloat(args[2].toString());
      
    int sc = starting_column[index];
    int sr = starting_row[index];
                
    //debug System.out.printf("%d %d %d %d", args0, args1, args2, message.getArgumentsLength());
    //debug System.out.println(args0 + " " + args1 + " " + args2);

    if(cable_orientation[index].equals("left")) {
      sc = args0 - sc;
      sr = args1 - sr;
    }
    else if(cable_orientation[index].equals("right")) {
      sc = co_max_num[index] - args0 + sc;
      sr = 7 - args1 + sr;
    }
    else if(cable_orientation[index].equals("up")) {
      int sc1 = co_max_num[index] - args1 + sr;
      int sr1 = args0 - sc;
      sc = sc1;
      sr = sr1;
    }
    else if(cable_orientation[index].equals("down")) {
      int sc1 = args1 - sr;
      int sr1 = 7 - args0 + sc;
      sc = sc1;
      sr = sr1;
    }

    if(sc < 0 || sr < 0)
      return null;

    sc = (sc >= 10)?('A' + (sc - 10)):(sc + '0');
    sr = (sr >= 10)?('A' + (sr - 10)):(sr + '0');

    msgled_buf.setCharAt(1, (char)(args2 + '0'));
    msgled_buf.setCharAt(2, (char)sc);
    msgled_buf.setCharAt(3, (char)sr);
    msgled_buf.setCharAt(4, (char)0x0D);
    return msgled_buf.toString();

    //sy return "l" + args2 + (char)sc + (char)sr + (char)0x0D;
  }

  private void enableMidiLed(int index) {
    final int index2 = index;
    Receiver rcv = new Receiver() {
        int index = index2;

        public void close() {}
 
        public void send(MidiMessage message, long timeStamp) {
          if(!protocol_type[index].equals("MIDI"))
            return ;

          byte[] data = message.getMessage();

          if((256 + data[0]) == 144 || (256 + data[0]) == 128) {// NOTE_ON -> 144, NOTE_OFF -> 128
            int sc = (data[1] % (co_max_num[index] + 1));
            int sr = (data[1] / (co_max_num[index] + 1));
            if(sc < 0) sc = 0;
            if(sr < 0) sr = 0;

            String ssc, ssr;
            if(sc >= 10)
              ssc = String.valueOf((char)('A' + (sc - 10)));
            else
              ssc = String.valueOf(sc);
            
            if(sr >= 10)
              ssr = String.valueOf((char)('A' + (sr - 10)));
            else
              ssr = String.valueOf(sr);

            String str = "";
            if((data[0] + 256) == 144 && data[2] > 0)
              str =new String("l1" + ssc + ssr + (char)0x0D);
            else if((data[0] + 256) == 128 || data[2] == 0)
              str =new String("l0" + ssc + ssr + (char)0x0D);

            if(checkPortState(index))
              sendDataToSerial(index, str);
          }
        }
      };
    midi_t[index].setReceiver(rcv);
  }

  private String controlMsgLedCol(int index, OSCMessage message) {
    Object[] args = message.getArguments();

    int args0 = (int)Float.parseFloat(args[0].toString());
    int args1 = (int)Float.parseFloat(args[1].toString());

    //debug System.out.println("led_col " + args0 + " " + args1);

    //sy0 if(args.length < 2)
    if(message.getArgumentsLength() < 2)
      return null;

    int sc = 0, sr = 0;
      
    if(cable_orientation[index].equals("left")) {
      sc = args0 - starting_column[index];
    }
    else if(cable_orientation[index].equals("right")) {
      sc = co_max_num[index] - args0 + starting_column[index];
    }
    else if(cable_orientation[index].equals("up")) {
      sc = args0 - starting_column[index];
    }
    else if(cable_orientation[index].equals("down")) {
      sc= 7 - args0 + starting_column[index];
    }
        
    if(sc < 0) return null;
        
    int shift = starting_row[index] % (co_max_num[index] + 1);
        
    if(cable_orientation[index].equals("left")) {
      sr = (char)(args1 >> shift);
    }
    else if(cable_orientation[index].equals("right")) {
      char sr0 = (char)args1;
      char sr1 = 0;
      for(int i = 0; i < 8; i++)
        if((sr0 & (0x01 << i)) == (0x01 << i))
          sr1 |= (0x01 << (7 - i));
      sr = (char)(sr1 << shift);
    }
    else if(cable_orientation[index].equals("up")) {
      char sr0 = (char)args1;
      char sr1 = 0;
      for(int i = 0; i < co_max_num[index] + 1; i++)
        if((sr0 & (0x01 << i)) == (0x01 << i))
          sr1 |= (0x01 << (co_max_num[index] - i));
      sr = (char)(sr1 << shift);
    }
    else if(cable_orientation[index].equals("down")) {
      sr = (char)(args1 >> shift);
    }
      
    String str;
    if(cable_orientation[index].equals("left") || cable_orientation[index].equals("right"))
      str =new String("lc " + sc + " " + sr + (char)0x0D); // (l)ed_(c)ol
    else
      str =new String("lr " + sc + " " + sr + (char)0x0D); // (l)ed_(r)ow
    return str;
  }

  private String controlMsgLedRow(int index, OSCMessage message) {
    Object[] args = message.getArguments();

    int args0 = (int)Float.parseFloat(args[0].toString());
    int args1 = (int)Float.parseFloat(args[1].toString());

    //debug System.out.println("led_row " + args0 + " " + args1);

    //sy0 if(args.length < 2)
    if(message.getArgumentsLength() < 2)
      return null;

    int sc = 0, sr = 0;
        
    if(cable_orientation[index].equals("left")) {
      sr = args0 - starting_row[index];
      if(sr > 7)
        return null;
    }
    else if(cable_orientation[index].equals("right")) {
      sr = 7 - args0 + starting_row[index];
      if(sr > 7)
        return null;
    }
    else if(cable_orientation[index].equals("up"))
      sr = co_max_num[index] - args0 + starting_row[index];
    else if(cable_orientation[index].equals("down"))
      sr = args0 - starting_row[index];
    
    if(sr < 0) return null;
        
    //sy int shift = starting_column[index] % (co_max_num[index] + 1);
    int shift = starting_column[index];
        
    if(cable_orientation[index].equals("left")) {
      sc = (char)(args1 >> shift);
    }
    else if(cable_orientation[index].equals("right")) {
      char sc0 = (char)args1;
      char sc1 = 0;
      for(int i = 0; i < co_max_num[index] + 1; i++)
        if((sc0 & (0x01 << i)) == (0x01 << i))
          sc1 |= (0x01 << (co_max_num[index] - i));
      sc = (char)(sc1 << shift);
    }
    else if(cable_orientation[index].equals("up")) {
      sc = (char)(args1 >> shift);
    }
    else if(cable_orientation[index].equals("down")) {
      char sc0 = (char)args1;
      char sc1 = 0;
      for(int i = 0; i < 8; i++)
        if((sc0 & (0x01 << i)) == (0x01 << i))
          sc1 |= (0x01 << (7 - i));
      sc = (char)(sc1 << shift);
    }
    
    String str;
    if(cable_orientation[index].equals("left") || cable_orientation[index].equals("right"))
      str =new String("lr " + sr + " " + sc + (char)0x0D); // (l)ed_(r)ow
    else
      str =new String("lc " + sr + " " + sc + (char)0x0D); // (l)ed_(c)o0
    return str;
  }

  private String[] controlMsgFrame(int index, OSCMessage message) {
    Object[] args0 = message.getArguments();
    String[] str = new String[co_max_num[index] + 1];
    int sc = 0, sr = 0;
    int[] args = new int[16];
    int shift;
    if(cable_orientation[index].equals("left") || cable_orientation[index].equals("right"))
      shift = starting_column[index] % (co_max_num[index] + 1);
    else
      shift = starting_column[index] % (7 + 1);

    //sy0 if(args.length < 16)
    if(message.getArgumentsLength() < 16)
      return null;

    //sy0 for(int i = 0; i < args0.length; i++) {
    for(int i = 0; i < message.getArgumentsLength(); i++) {
      args[i] = (int)Float.parseFloat(args0[i].toString());
    }
      
    for(int i = 0; i < (co_max_num[index] + 1); i++) {
/*
      if((cable_orientation[index].equals("left") || cable_orientation[index].equals("right")) && i > 7)
        break;
*/
      if(cable_orientation[index].equals("left")) {
        sr = i - starting_row[index];
        if(sr > 7)
          return null;
      }
      else if(cable_orientation[index].equals("right")) {
        sr = 7 - i + starting_row[index];
      }
      else if(cable_orientation[index].equals("up"))
        sr = co_max_num[index] - i + starting_column[index];
      else if(cable_orientation[index].equals("down"))
        sr = i - starting_column[index];
      
      if(co_max_num[index] == 7) {
        if(cable_orientation[index].equals("left") || cable_orientation[index].equals("right")) {
          if(i < starting_row[index] || ((i - starting_row[index]) > 7))
            continue;
        }
        else {
          if(i < starting_column[index] || ((i - starting_column[index]) > 7))
            continue;
        }
      }
      else if(co_max_num[index] == 15) {
        if(cable_orientation[index].equals("left") || cable_orientation[index].equals("right")) {
          if(i < starting_row[index] || ((i - starting_row[index]) > 7))
            continue;
        }
        else {
          if(i < starting_column[index] || ((i - starting_column[index]) > 15))
            continue;
        }
      }
        
      if(cable_orientation[index].equals("left"))
        sc = (char)(args[i] >> shift);
      else if(cable_orientation[index].equals("right")) {
        char sc0 = (char)args[i];
        char sc1 = 0;
        for(int j = 0; j < co_max_num[index] + 1; j++)
          if((sc0 & (0x01 << j)) == (0x01 << j))
            sc1 |= (0x01 << (co_max_num[index] - j));
        sc = (char)(sc1 << shift);
      }
      else if(cable_orientation[index].equals("up"))
        sc = (char)(args[i] >> shift);
      else if(cable_orientation[index].equals("down")) {
        char sc0 = (char)args[i];
        char sc1 = 0;
        for(int j = 0; j < (7 + 1); j++)
          if((sc0 & (0x01 << j)) == (0x01 << j))
            sc1 |= (0x01 << (7 - j));
        sc = (char)(sc1 << shift);
      }
          
      if(cable_orientation[index].equals("left") || cable_orientation[index].equals("right"))
        str[i] = new String("lr " + sr + " " + sc + (char)0x0D); // (l)ed_(r)ow
      else
        str[i] = new String("lc " + sr + " " + sc + (char)0x0D); // (l)ed_(c)ol
    }//end for i
    return str;
  }

  private String[] controlMsgClear(int index, OSCMessage message) {
    Object[] args = message.getArguments();
    String[] str = new String[8];

    int args0 = 0;
    //sy0 if(args.length == 1)
    if(message.getArgumentsLength() == 1)
      args0 = (int)Float.parseFloat(args[0].toString());
    
    for(int i = 0; i < 8; i++) {
      int state;
      if(co_max_num[index] == 7) {//sixty four
        if(message.getArgumentsLength() == 0 || args0 == 0)
          state = 0;
        else
          state = 255;
      }
      else {//one twenty eight
        if(message.getArgumentsLength() == 0 || args0 == 0)
          state = 0;
        else
          state = 65535;
      }
        
      str[i] = new String("lr " + i + " " + state + (char)0x0D);
    }
    return str;
  }

  private String controlMsgAdcEnable(int index, OSCMessage message) {
    Object[] args0 = message.getArguments();
    
    int[] args = new int[message.getArgumentsLength()];
    for(int i = 0; i < message.getArgumentsLength(); i++) {
      args[i] = (int)Float.parseFloat(args0[i].toString());
    }
    
    String str =new String("ae " + args[0] + " " + args[1] + (char)0x0D);
    if((Integer)args[1] == 1) {
      adc_ck[(Integer)args[0]].setSelected(true);
      adc_cmb0[(Integer)args[0]].setEnabled(true);
      if(adc_cmb0[(Integer)args[0]].getSelectedIndex() < 2)
        adc_cmb1[(Integer)args[0]].setEnabled(true);
    }
    else {
      adc_ck[(Integer)args[0]].setSelected(false);
      adc_cmb0[(Integer)args[0]].setEnabled(false);
      adc_cmb1[(Integer)args[0]].setEnabled(false);
    }
    return str;
  }

  private void controlMsgAdcType(int index, OSCMessage message) {
    Object[] args0 = message.getArguments();
    
    int[] args = new int[message.getArgumentsLength()];
    for(int i = 0; i < message.getArgumentsLength(); i++) {
      args[i] = (int)Float.parseFloat(args0[i].toString());
    }
    adc_cmb0[args[0]].setSelectedIndex(args[1]);
  }

  private void controlMsgAdcCurve(int index, OSCMessage message) {
    Object[] args0 = message.getArguments();
    
    int[] args = new int[message.getArgumentsLength()];
    for(int i = 0; i < message.getArgumentsLength(); i++) {
      args[i] = (int)Float.parseFloat(args0[i].toString());
    }
    adc_cmb1[args[0]].setSelectedIndex(args[1]);
  }

  private void controlMsgDevice(OSCMessage message) {
    Object[] args = message.getArguments();
    int device_no = ((Integer)args[0]).intValue();
    if(device_no == 0 || device_no == 1) {
      device_cb.setSelectedIndex(device_no);
      changeDeviceSettings(device_no);
    }
  }

  private void controlMsgOscconfig(OSCMessage message) {
    Object[] args = message.getArguments();
    address_pattern_prefix[(Integer)args[2]] = (String)args[0];
    host_port[(Integer)args[2]] = (String)args[1];
    if(device_cb.getSelectedIndex() == (Integer)args[2]) {
      prefix_tf.setText((String)args[0]);
      hostport_tf.setText(((Integer)args[1]).toString());
    }

    //sy0 if(args.length == 4) {
    if(message.getArgumentsLength() == 4) {
      host_address[(Integer)args[2]] = Integer.toString((Integer)args[3]);
      if(device_cb.getSelectedIndex() == (Integer)args[2])
        hostaddress_tf.setText(Integer.toString((Integer)args[3]));
    }
    initOSCPort((Integer)args[2]);
    initOSCListener();
  }

  private void controlMsgPrefix(OSCMessage message) {
    Object[] args = message.getArguments();
    //sy0 if(args.length == 0) {
    if(message.getArgumentsLength() == 0) {
      for(int i = 0; i < device_list.size(); i++) {
        OSCMessage msg;
        Object[] args0 = new Object[2];
        args0[0] = String.valueOf(i);
        args0[1] = address_pattern_prefix[i];
        msg = new OSCMessage("/sys/prefix", args0);
        try {
          oscpout.send(msg);
        }
        catch(IOException e) {}
      }
    }
    //sy0 else if(args.length == 2) {
    else if(message.getArgumentsLength() == 2) {
      address_pattern_prefix[(Integer)args[0]] = (String)args[1];
      if(device_cb.getSelectedIndex() == (Integer)args[0])
        prefix_tf.setText((String)args[1]);
      initOSCListener();
    }
    else {
      prefix_tf.setText((String)args[0]);
      initOSCListener();
    }
  }

  private String controlMsgIntensity(int index, OSCMessage message) {
    Object[] args = message.getArguments();
    String str = null;

    //sy0 if(args.length == 1)
    if(message.getArgumentsLength() == 1)
      str = new String("i " + (Integer)args[0] + (char)0x0D);
    //sy0 else if(args.length == 2 && (Integer)args[0] == index)
    else if(message.getArgumentsLength() == 2 && (Integer)args[0] == index)
      str = new String("i " + (Integer)args[1] + (char)0x0D);
    return str;
  }

  private String controlMsgTest(int index, OSCMessage message) {
    Object[] args = message.getArguments();
    String str = null;

    //sy0 if(args.length == 1)
    if(message.getArgumentsLength() == 1)
      str = new String("t " + (Integer)args[0] + (char)0x0D);
    //sy0 else if(args.length == 2 && (Integer)args[0] == index)
    else if(message.getArgumentsLength() == 2 && (Integer)args[0] == index)
      str = new String("t " + (Integer)args[1] + (char)0x0D);
    return str;
  }

  private String controlMsgShutdown(int index, OSCMessage message) {
    Object[] args = message.getArguments();
    String str = null;

    //sy0 if(args.length == 1)
    if(message.getArgumentsLength() == 1)
      str = new String("s " + (Integer)args[0] + (char)0x0D);
    //sy0 else if(args.length == 2 && (Integer)args[0] == index)
    else if(message.getArgumentsLength() == 2 && (Integer)args[0] == index)
      str = new String("s " + (Integer)args[1] + (char)0x0D);

    return str;
  }

  private String controlMsgVersion(OSCMessage message) {
    String str = new String("f" + (char)0x0D);
    return str;
  }

  private void controlMsgReport(OSCMessage message) {
    Object[] args = message.getArguments();
    Object[] args0;
    String str;
    OSCMessage msg;

    try {
      //sy0 if(args.length == 0) {
      if(message.getArgumentsLength() == 0) {
        args0 = new Object[1];
        args0[0] = String.valueOf(device_list.size());
        msg = new OSCMessage("/sys/devices", args0);
        oscpout.send(msg);
        
        for(int i = 0; i < device_list.size(); i++) {
          args0 = new Object[2];
          args0[0] = String.valueOf(i);
          if(device[i].indexOf("128") != -1)
            args0[1] = "128";
          else
            args0[1] = "64";
          msg = new OSCMessage("/sys/type", args0);
          oscpout.send(msg);
        }

        for(int i = 0; i < device_list.size(); i++) {
          args0 = new Object[2];
          args0[0] = String.valueOf(i);
          args0[1] = address_pattern_prefix[i];
          msg = new OSCMessage("/sys/prefix", args0);
          oscpout.send(msg);
        }
        
        for(int i = 0; i < device_list.size(); i++) {
          args0 = new Object[2];
          args0[0] = String.valueOf(i);
          args0[1] = cable_orientation[i];
          msg = new OSCMessage("/sys/cable", args0);
          oscpout.send(msg);
        }
        
        for(int i = 0; i < device_list.size(); i++) {
          args0 = new Object[3];
          args0[0] = String.valueOf(i);
          args0[1] = starting_column[i];
          args0[2] = starting_row[i];
          msg = new OSCMessage("/sys/offset", args0);
          oscpout.send(msg);
        }
      }
    }
    catch(IOException e) {}
  }

  private void controlMsgType(OSCMessage message) {
    Object[] args = message.getArguments();
    Object[] args0;
    String str;
    OSCMessage msg;

    try {
      //sy0 if(args.length == 0) {
      if(message.getArgumentsLength() == 0) {
        for(int i = 0; i < device_list.size(); i++) {
          args0 = new Object[2];
          args0[0] = String.valueOf(i);
          if(device[i].indexOf("128") != -1)
            args0[1] = "128";
          else
            args0[1] = "64";
          msg = new OSCMessage("/sys/type", args0);
          oscpout.send(msg);
        }
      }
    }
    catch(IOException e) {}
  }

  private void controlMsgOffset(OSCMessage message) {
    Object[] args = message.getArguments();
    //sy0 if(args.length == 2) {
    if(message.getArgumentsLength() == 2) {
      startcolumn_s.setValue((Integer)args[0]);
      startrow_s.setValue((Integer)args[1]);
    }
    //sy0 else if(args.length == 3) {
    else if(message.getArgumentsLength() == 3) {
      starting_column[(Integer)args[0]] = (Integer)args[1];
      starting_row[(Integer)args[0]] = (Integer)args[2];
      if(device_cb.getSelectedIndex() == (Integer)args[0]) {
        startcolumn_s.setValue((Integer)args[1]);
        startrow_s.setValue((Integer)args[2]);
      }
    }
  }

  private void controlMsgCable(OSCMessage message) {
    Object[] args = message.getArguments();
    //sy0 if(args.length == 0) {
    if(message.getArgumentsLength() == 0) {
      for(int i = 0; i < device_list.size(); i++) {
        OSCMessage msg;
        Object[] args0 = new Object[2];
        args0[0] = String.valueOf(i);
        args0[1] = cable_orientation[i];
        msg = new OSCMessage("/sys/cable", args0);
        try {
          oscpout.send(msg);
        }
        catch(IOException e){}
      }
    }
    //sy0 else if(args.length == 2) {
    else if(message.getArgumentsLength() == 2) {
      String costr = "";
      switch((Integer)args[1]) {
      case 0:
        costr = "left";
        break;
      case 1:
        costr = "up";
        break;
      case 2:
        costr = "right";
        break;
      case 3:
        costr = "down";
        break;
      }
      cable_orientation[(Integer)args[0]] = costr;
      if(device_cb.getSelectedIndex() == (Integer)args[0])
        cable_cb.setSelectedItem(costr);
      initOSCListener();
    }
    else
      cable_cb.setSelectedItem(((String)args[0]));
  }

  public void initOSCListener() {
    if(((String)device_cb.getSelectedItem()).equals(device[0]))
      address_pattern_prefix[0] = prefix_tf.getText();
    else if(((String)device_cb.getSelectedItem()).equals(device[1]))
      address_pattern_prefix[1] = prefix_tf.getText();
  }


  private class OSCReader implements Runnable {
    private DatagramSocket ds;
    private OSCByteArrayToJavaConverter converter = new OSCByteArrayToJavaConverter();
    private OSCPacketDispatcher dispatcher = new OSCPacketDispatcher();
    private OSCMessage om;
    private String address;
    private int resetPort;
    private boolean resetPortFlag = false;

    OSCReader(int port) throws SocketException {
      ds = new DatagramSocket(port);
    }

    public void setPort(int port) throws SocketException {
      if(port > 0) {
        resetPort = port;
        resetPortFlag = true;
      }
    }

    @Override
    public synchronized void run() {
      try {
        String sermsg = "";
        String[] sermsgs = new String[16];
        byte[] buffer = new byte[1536];
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);

        while(true) {
          if(resetPortFlag) {
            ds.close();
            ds = new DatagramSocket(resetPort);
            //sy wait(10);
            resetPortFlag = false;
          }

          ds.receive(dp);
          om = converter.convert(buffer, dp.getLength());
          address = om.getAddress();
      
          //Prefix Messages
          for(int i = 0; i < current_picnome_num; i++) {
            if(protocol_type[i].equals("MIDI") || !checkPortState(i))
              continue;

            if(address.equals(address_pattern_prefix[i] + "/led")) {
              sermsg = controlMsgLed(i, om);
              sendDataToSerial(i, sermsg);
            }
            else if(address.equals(address_pattern_prefix[i] + "/led_col")) {
              sermsg = controlMsgLedCol(i, om);
              sendDataToSerial(i, sermsg);
            }
            else if(address.equals(address_pattern_prefix[i] + "/led_row")) {
              sermsg = controlMsgLedRow(i, om);
              sendDataToSerial(i, sermsg);
            }
            else if(address.equals(address_pattern_prefix[i] + "/frame")) {
              sermsgs = controlMsgFrame(i, om);
              sendDataToSerial(i, sermsgs);
            }
            else if(address.equals(address_pattern_prefix[i] + "/clear")) {
              sermsgs = controlMsgClear(i, om);
              sendDataToSerial(i, sermsgs);
            }
            else if(address.equals(address_pattern_prefix[i] + "/adc_enable")) {
              sermsg = controlMsgAdcEnable(i, om);
              sendDataToSerial(i, sermsg);
            }
            else if(address.equals(address_pattern_prefix[i] + "/adc/enable")) {
              sermsg = controlMsgAdcEnable(i, om);
              sendDataToSerial(i, sermsg);
            }
            else if(address.equals(address_pattern_prefix[i] + "/adc/type")) {
              controlMsgAdcType(i, om);
            }
            else if(address.equals(address_pattern_prefix[i] + "/adc/curve")) {
              controlMsgAdcCurve(i, om);
            }
            else if(address.equals("/sys/intensity")) {
              sermsg = controlMsgIntensity(i, om);
              sendDataToSerial(i, sermsg);
            }
            else if(address.equals("/sys/test")) {
              sermsg = controlMsgTest(i, om);
              sendDataToSerial(i, sermsg);
            }
            else if(address.equals("/sys/shutdown")) {
              sermsg = controlMsgShutdown(i, om);
              sendDataToSerial(i, sermsg);
            }
            else if(address.equals("/sys/version")) {
              sermsg = controlMsgVersion(om);
              sendDataToSerial(i, sermsg);
            }
          }
          
          //System Messages
          if(address.equals("/sys/device"))
            controlMsgDevice(om);
          else if(address.equals("/sys/oscconfig"))
            controlMsgOscconfig(om);
          else if(address.equals("/sys/prefix"))
            controlMsgPrefix(om);
          else if(address.equals("/sys/report"))
            controlMsgReport(om);
          else if(address.equals("/sys/type"))
            controlMsgType(om);
          else if(address.equals("/sys/offset"))
            controlMsgOffset(om);
          else if(address.equals("/sys/cable"))
            controlMsgCable(om);
          
          //You have to comment out if you compile win version.
          //mac wait(0, 1);//mac
        }
      }
      catch(IOException e) {}
      //You have to comment out if you compile win version.
      //mac catch(InterruptedException ioe) {}//mac
    }
  }

  private class SerialReader implements Runnable {
    private int index;
    private BufferedInputStream bis;

    SerialReader(int index, InputStream in) {
      this.index = index;
      this.bis = new BufferedInputStream(in);
    }

    @Override
    public void run() {
      int buffer = 0;
      int msg_index = 0;
      int[] chs = new int[10];
      
      //for old picnome
      //old StringBuilder sb = new StringBuilder(10);//macke

      while(true) {
        try {
          buffer = bis.read();
          if(buffer == -1)
            continue;
          else {
            chs[msg_index] = buffer;
            if(msg_index == 0 && (chs[msg_index] != 'p' && chs[msg_index] != 'r' && chs[msg_index] != 'a' && chs[msg_index] != 'f'))
              continue;
            msg_index++;
          }
        }
        catch(IOException e) {
          e.printStackTrace();
          continue;
        }

        if(((char)chs[0] == 'p' || (char)chs[0] == 'r') && msg_index == 2) {
          sendOSCMessageFromHw(this.index, chs);
          msg_index = 0;
        }
        else if((char)chs[0] == 'a' && msg_index == 3) {
          sendOSCMessageFromHw(this.index, chs);
          msg_index = 0;
        }
        else if((char)chs[0] == 'f' && msg_index == 3) {
          fwver = String.valueOf(((float)chs[1] / 10.0) + ((float)chs[2] / 1000.0));
          fwver_flag = true;
          msg_index = 0;
        }
        else if(msg_index > 3){
          //debug System.out.println("error : too long message");
          msg_index = 0;
        }
      }
    }
  }
}