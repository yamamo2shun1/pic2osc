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
 * PicnomeCommunication.java,v.1.3.16 2010/04/03
 */

// RXTX
import gnu.io.*;

// JavaOSC
import com.illposed.osc.*;
import com.illposed.osc.utility.*;

// JavaMIDI
import javax.sound.midi.*;

import javax.swing.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class PicnomeCommunication implements PCInterface {
  public Vector<String> device_list = new Vector<String>();
  public Vector<String> midiinput_list = new Vector<String>();
  public Vector<String> midioutput_list = new Vector<String>();
  private List<MidiDevice.Info> midiinputdevices = new ArrayList<MidiDevice.Info>();
  private List<MidiDevice.Info> midioutputdevices = new ArrayList<MidiDevice.Info>();

  public JButton openclose_b;
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
  public JCheckBox[] adc_ck = new JCheckBox[7];
  public JProgressBar update_pb;
/* for DEBUG
  private JTextField debug_tf;
  private JTextField debug2_tf;
*/

  private final int max_picnome_num = 2;
  private int current_picnome_num;
  private CommPortIdentifier[] portId = new CommPortIdentifier[max_picnome_num];
  private SerialPort[] port = new SerialPort[max_picnome_num];
  private InputStream[] in = new InputStream[max_picnome_num];
  private InputStreamReader[] inr = new InputStreamReader[max_picnome_num];
  private OutputStream[] out = new OutputStream[max_picnome_num];

  private OSCPortIn oscpin;
  private OSCPortOut oscpout;

  //for Mac OS X
  private MidiDevice midiin;
  private MidiDevice midiout;
  private Receiver midi_r;
  private Transmitter midi_t;
  private int midi_in_port;
  private int midi_out_port;
  private int midi_pgm_number;

  private String[] device = new String[max_picnome_num];
  private String[] device2 = new String[max_picnome_num];
  private String[] host_address = new String[max_picnome_num];
  private int[] host_port = new int[max_picnome_num];
  private int[] listen_port = new int[max_picnome_num];
  private String[] connect_state = new String[max_picnome_num];
  private String[] cable_orientation = new String[max_picnome_num];
  private String[] address_pattern_prefix = new String[max_picnome_num];
  private int[] intensity = new int[max_picnome_num];
  private int[] starting_column = new int[max_picnome_num];
  private int[] starting_row = new int[max_picnome_num];
  private int[] co_max_num = new int[max_picnome_num];
  private boolean[][] adc_enable = new boolean[max_picnome_num][7];
  public int[][][] midi_parameter = new int[16][8][5];;

  public PicnomeCommunication() {
    current_picnome_num = 0;
    initDeviceList();
    for(int i = 0; i < max_picnome_num; i++) {
      in[i] = null;
      out[i] = null;
      host_address[i] = "127.0.0.1";
      host_port[i] = 8000;
      listen_port[i] = 8080;
      connect_state[i] = "Open";
      cable_orientation[i] = "left";
      address_pattern_prefix[i] = "/test";
      intensity[i] = 15;
      starting_column[i] = 0;
      starting_row[i] = 0;
      co_max_num[i] = 7;
      for(int j = 0; j < 7; j++) {
        adc_enable[i][j] = false;
      }
    }
    initMIDIPort();
  }

  private List<String> getUsbInfo(String name) {
    String id = "none";
    String iousbdevices = new String();

    try {
      ProcessBuilder pb = new ProcessBuilder("ioreg", "-w", "0", "-S", "-p", "IOUSB", "-n", name, "-r");
      Process p = pb.start();
      InputStream is = p.getInputStream();

      int c;
      while((c = is.read()) != -1)
        iousbdevices += (new Character((char)c)).toString();
      is.close();
    }catch(IOException e) {}

    List<String> sfx = new ArrayList<String>();
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

      if(!(vid.get(vid.size() - 1).equals("1240") && (pid.get(pid.size() - 1).equals("65477") || pid.get(pid.size() - 1).equals("64768")))) {
        sfx.remove(sfx.size() - 1);
        vid.remove(vid.size() - 1);
        pid.remove(pid.size() - 1);
      }
    }
    return sfx;
  }

  private void initDeviceList() {
    String device_name;
    List<String> suffix0 = getUsbInfo("IOUSBDevice");
    List<String> suffix1 = getUsbInfo("PICnome");
    List<String> suffix2 = getUsbInfo("PICnome128");
    Enumeration e = CommPortIdentifier.getPortIdentifiers();

    for(int i = 0; i < max_picnome_num; i++) {
      device[i] = "";
      device2[i] = "";
    }

    while(e.hasMoreElements()) {
      device_name = ((CommPortIdentifier)e.nextElement()).getName();

      if(device_name.indexOf("/dev/cu.usbmodem") != -1) {
        for(String s0str: suffix0) {
          if(device_name.indexOf(s0str) != -1) {
            if(current_picnome_num >= max_picnome_num)
              break;
            device[current_picnome_num] = "tkrworks-PICnome-" + s0str;
            device2[current_picnome_num] = device_name;
            current_picnome_num++;
            device_list.add("tkrworks-PICnome-" + s0str);
          }
        }
        for(String s2str: suffix2) {//for one twenty eight
          if(device_name.indexOf(s2str) != -1) {
            if(current_picnome_num >= max_picnome_num)
              break;
            device[current_picnome_num] = "tkrworks-PICnome128-" + s2str;
            device2[current_picnome_num] = device_name;
            current_picnome_num++;
            device_list.add("tkrworks-PICnome128-" + s2str);
          }
        }
        for(String s1str: suffix1) {//for sixty four
          if(device_name.indexOf(s1str) != -1) {
            if(current_picnome_num >= max_picnome_num)
              break;
            device[current_picnome_num] = "tkrworks-PICnome-" + s1str;
            device2[current_picnome_num] = device_name;
            current_picnome_num++;
            device_list.add("tkrworks-PICnome-" + s1str);
          }
        }
      }
    }
  }

  public void changeDeviceSettings(int index) {
    connect_state[1 - index] = openclose_b.getText();

    hostaddress_tf.setText(host_address[index]);
    hostport_tf.setText(Integer.toString(host_port[index]));
    listenport_tf.setText(Integer.toString(listen_port[index]));
    openclose_b.setText(connect_state[index]);
    cable_cb.setSelectedItem(cable_orientation[index]);
    prefix_tf.setText(address_pattern_prefix[index]);
    intensity_s.setValue(intensity[index]);
    startcolumn_s.setValue(starting_column[index]);
    startrow_s.setValue(starting_row[index]);

    for(int i = 0; i < 7; i++)
      adc_ck[i].setSelected(adc_enable[index][i]);
  }

  public boolean openSerialPort(int index) {
    try {
      if(device[index].indexOf("PICnome128") != -1)
        co_max_num[index] = 15;
      else
        co_max_num[index] = 7;
      portId[index] = CommPortIdentifier.getPortIdentifier(device2[index]);
      port[index] = (SerialPort)portId[index].open("PICnomeSerial", 2000);
    }
    catch (NoSuchPortException e) {
      e.printStackTrace();
      return false;
    }
    catch (PortInUseException e) {
      e.printStackTrace();
      return false;
    }

    try {
      in[index] = port[index].getInputStream();
      inr[index] = new InputStreamReader(in[index]);
      out[index] = port[index].getOutputStream();
      //sy0 initSerialListener(index);
      (new Thread(new SerialReader(index, inr[index]))).start();


      if(((String)protocol_cb.getSelectedItem()).equals("Open Sound Control")) {
        initOSCPort();
        initOSCListener("all");
      }
      else//for MIDI
        openMIDIPort();

      //sy initOSCPort();
      //sy initOSCListener("all");
      //sy openMIDIPort();

    }
    catch(IOException e) {}
    return true;
  }

  public boolean setSerialPort(int index) {
    try {
      port[index].setSerialPortParams(230400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
      port[index].setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
    }
    catch (UnsupportedCommOperationException e) {
      e.printStackTrace();
      return false;
    }
    port[index].setDTR(true);
    port[index].setRTS(false);
    return true;
  }

  public boolean closeSerialPort(int index) {
    try {
      if(((String)protocol_cb.getSelectedItem()).equals("MIDI")) {
        //sy midiio.closeInput(midi_in_port);
      }

      inr[index].close();
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

  private void initOSCPort() {
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
      oscpin = new OSCPortIn(Integer.parseInt(listenport_tf.getText()));
      oscpout = new OSCPortOut(InetAddress.getByAddress(hostaddress), Integer.parseInt(hostport_tf.getText()));
    }
    catch(UnknownHostException e){}
    catch(SocketException e){}
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
/*
  public void initMIDIPort()
  {
    midiio = MidiIO.getInstance();
    midiio.printDevices();
    int in_num = midiio.numberOfInputDevices();
    for(int i = 0; i < in_num; i++)
      midiinput_list.add(midiio.getInputDeviceName(i));
    int out_num = midiio.numberOfOutputDevices();
    for(int i = 0; i < out_num; i++)
      midioutput_list.add(midiio.getOutputDeviceName(i));
  }
*/

  private void openMIDIPort() {
    try {
      midiin = MidiSystem.getMidiDevice(midiinputdevices.get(midiinput_cb.getSelectedIndex()));
      midiin.open();
      midi_t = midiin.getTransmitter();
      enableMidiLed();

      midiout = MidiSystem.getMidiDevice(midioutputdevices.get(midioutput_cb.getSelectedIndex()));
      midiout.open();
      midi_r = midiout.getReceiver();
    }
    catch(MidiUnavailableException mue){}
  }
/*
  public void openMIDIPort()
  {
    midi_in_port = midiinput_cb.getSelectedIndex();
    midiin.plug(this, "enableMIDILed", midi_in_port, 0);

    midi_out_port = midioutput_cb.getSelectedIndex();
    for(int i = 0; i < 128; i++)
      midiout[i] = midiio.getMidiOut(0, midi_out_port);
  }
*/

  public String getCurrentDevice(int index) {
    return device[index];
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

  private boolean checkAddressPatternPrefix(OSCMessage message, int index) {
    boolean b;
    String address = message.getAddress();
    String prefix = address.substring(0, address.lastIndexOf("/"));

    if(address_pattern_prefix[index] != null && prefix.equals(address_pattern_prefix[index]))
      b = true;
    else
      b = false;

    return b;
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
      out[index].write(str.getBytes());
    }
    catch(IOException e) {}
  }

  private int getHexStringToInt(String str) {
    int value = 0;
    if(str.equals("A"))
      value = 10;
    else if(str.equals("B"))
      value = 11;
    else if(str.equals("C"))
      value = 12;
    else if(str.equals("D"))
      value = 13;
    else if(str.equals("E"))
      value = 14;
    else if(str.equals("F"))
      value = 15;
    else
      value = Integer.parseInt(str);
    return value;
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

        //debug debug_tf.setText(args[0] + " " + args[1] + " " + args[2]);
        msg = new OSCMessage(address_pattern_prefix[index] + "/press", args);
        try {
          oscpout.send(msg);
        }
        catch(IOException e){}
      }
      else {// for MIDI
        int notex = Integer.parseInt(st.nextToken());
        int notey = Integer.parseInt(st.nextToken());
        int state = Integer.parseInt(st.nextToken());
        int note_number = notex + (notey * (co_max_num[index] + 1));
        //sy debug_tf.setText(Integer.toString(note_number));

        try {
          ShortMessage sm = new ShortMessage();
          if(state == 1)
            sm.setMessage(ShortMessage.NOTE_ON, (byte)note_number, 127);
          else
            sm.setMessage(ShortMessage.NOTE_OFF, (byte)note_number, 0);
          midi_r.send(sm, 1);
        }
        catch(InvalidMidiDataException imde) {}
/*
        Note note;
        if(state == 1)
        {
          note = new Note(note_number, midi_parameter[notex][notey][1], midi_parameter[notex][notey][3]);
        }
        else
        {
          note = new Note(note_number, midi_parameter[notex][notey][2], midi_parameter[notex][notey][4]);
        }
        midiout[note_number].sendNote(note);
*/
      }
    }
    else if(token.equals("input")) {
      if(((String)protocol_cb.getSelectedItem()).equals("Open Sound Control")) {
        args = new Object[2];

        args[0] = Integer.parseInt(st.nextToken()); // Pin
        args[1] = Integer.parseInt(st.nextToken()); // State

        msg = new OSCMessage(address_pattern_prefix[index] + "/input", args);

        try {
          oscpout.send(msg);
        }
        catch(IOException e) {}
      }
/*
      else//for MIDI
      {
        int pin = Integer.parseInt(st.nextToken()); // Pin
        int state = Integer.parseInt(st.nextToken()); // State

        if(pin == 0 && state == 1)
          midi_pgm_number++;
        else if(pin == 1 && state == 1)
          midi_pgm_number--;

        if(midi_pgm_number > 127)
          midi_pgm_number = 127;
        else if(midi_pgm_number < 0)
          midi_pgm_number = 0;

        //sy midiout.sendProgramChange(new ProgramChange(midi_pgm_number));
        midiout[0].sendProgramChange(new ProgramChange(midi_pgm_number));
      }
*/
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

  private void enableMsgLed() {
    OSCListener listener = new OSCListener() {
        public synchronized void acceptMessage(java.util.Date time, OSCMessage message) {
          try {
            String address = message.getAddress();
            Object[] args = message.getArguments();

            int args0 = (int)Float.parseFloat(args[0].toString());
            int args1 = (int)Float.parseFloat(args[1].toString());
            int args2 = (int)Float.parseFloat(args[2].toString());

            int[] sc = new int[current_picnome_num];
            int[] sr = new int[current_picnome_num];

            for(int i = 0; i < current_picnome_num; i++) {
              if(!checkAddressPatternPrefix(message, i))
                continue;

              if(device[i].indexOf("PICnome128") == -1) {
                if(args0 > 7 || args1 > 7)
                  return ;
              }
              else if(device[i].indexOf("PICnome128") != -1) {
                if(cable_orientation[i].equals("right") || cable_orientation[i].equals("left")) {
                  if(args1 > 7) return ;
                }
                else {
                  if(args0 > 7) return ;
                }
              }

              sc[i] = starting_column[i];
              sr[i] = starting_row[i];

              if(cable_orientation[i].equals("left")) {
                sc[i] = args0 - sc[i];
                sr[i] = args1 - sr[i];
              }
              else if(cable_orientation[i].equals("right")) {
                sc[i] = co_max_num[i] - args0 + sc[i];
                sr[i] = 7 - args1 + sr[i];
              }
              else if(cable_orientation[i].equals("up")) {
                int sc1 = co_max_num[i] - args1 + sr[i];
                int sr1 = args0 - sc[i];
                sc[i] = sc1;
                sr[i] = sr1;
              }
              else if(cable_orientation[i].equals("down")) {
                int sc1 = args1 - sr[i];
                int sr1 = 7 - args0 + sc[i];
                sc[i] = sc1;
                sr[i] = sr1;
              }

              if(sc[i] < 0 || sr[i] < 0) continue ;

              String ssc, ssr;
              switch(sc[i]){
              case 10:
                ssc = "A";
                break;
              case 11:
                ssc = "B";
                break;
              case 12:
                ssc = "C";
                break;
              case 13:
                ssc = "D";
                break;
              case 14:
                ssc = "E";
                break;
              case 15:
                ssc = "F";
                break;
              default:
                ssc = String.valueOf(sc[i]);
                break;
              }
              switch(sr[i]){
              case 10:
                ssr = "A";
                break;
              case 11:
                ssr = "B";
                break;
              case 12:
                ssr = "C";
                break;
              case 13:
                ssr = "D";
                break;
              case 14:
                ssr = "E";
                break;
              case 15:
                ssr = "F";
                break;
              default:
                ssr = String.valueOf(sr[i]);
                break;
              }

              //sy String str =new String("led " + ssc + ssr + args2 + (char)0x0D);
              String str =new String("l" + args2 + ssc + ssr + (char)0x0D);
/*
              String str;
              if(args2 == 1) // (l)ighting
                str =new String("l" + ssc + ssr + (char)0x0D);
              else // (e)xtinction
                str =new String("e" + ssc + ssr + (char)0x0D);
*/
              //debug debug_tf.setText(str);
              if(checkPortState(i)) {
                sendDataToSerial(i, str);
                wait(0, 5);
              }
            }//end for
          } catch(NullPointerException e) {
            System.err.println("/led");
            System.err.println(message.getAddress());
            System.err.println(message.getArguments().length);
            System.err.println("NullPointerException: " + e.getMessage());
            e.printStackTrace();
          } catch(ArrayIndexOutOfBoundsException e) {
            System.err.println("/led");
            System.err.println(message.getAddress());
            System.err.println(message.getArguments());
            System.err.println("ArrayIndexOutOfBoundsException: " + e.getMessage());
            e.printStackTrace();
          } catch(InterruptedException e) {
            System.err.println("/led");
            System.err.println("InterruptedException: " + e.getMessage());
            e.printStackTrace();
          }
        }
      };
    oscpin.addListener(prefix_tf.getText() + "/led", listener);
  }

  private void enableMidiLed() {
    Receiver rcv = new Receiver() {
        public void close() {}
 
        public void send(MidiMessage message, long timeStamp) {
          byte[] data = message.getMessage();
 
          if((256 + data[0]) == 144 || (256 + data[0]) == 128) {// NOTE_ON -> 144, NOTE_OFF -> 128
            int sc = (Integer)startcolumn_s.getValue();
            int sr = (Integer)startrow_s.getValue();
            sc = (data[1] % 8) - sc;
            sr = (data[1] / 8) - sr;
            if(sc < 0) sc = 0;
            if(sr < 0) sr = 0;
 
            String str;
            if(data[2] == 0)
              str =new String("led " + sc + " " + sr + " " + 0 + (char)0x0D);
            else
              str =new String("led " + sc + " " + sr + " " + 1 + (char)0x0D);
            
            //debug debug_tf.setText(str);
            sendDataToSerial(0, str);
          }
        }
      };
    midi_t.setReceiver(rcv);
  }
/*
  //sy void enableMIDILed(Note note)
  public void noteOnReceived(Note note)
  {
    debug_tf.setText("test");
    if(((String)protocol_cb.getSelectedItem()).equals("MIDI"))
    {
      int pitch = note.getPitch();
      int velocity = note.getVelocity();

      int[] sc = new int[2];
      int[] sr = new int[2];

      for(int i = 0; i < 2; i++)
      {
        if(co_max_num[i] == 7)
        {
          if(pitch > 63) return;

          sc[i] = (pitch % 8);
          sr[i] = (pitch / 8);
        }
        else if(co_max_num[i] == 15)
        {
          if(pitch > 127) return;

          sc[i] = (pitch % 16);
          sr[i] = (pitch / 16);
        }

        try
        {
          String str;
          if(velocity == 0)
            str =new String("led " + sc[i] + " " + sr[i] + " " + 0 + (char)0x0D);
          else
            str =new String("led " + sc[i] + " " + sr[i] + " " + 1 + (char)0x0D);
          out[i].write(str.getBytes());
        }
        catch(IOException e){}
      }
    }
  }
*/

  private void enableMsgLedCol() {
    OSCListener listener = new OSCListener() {
        public synchronized void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();

          int args0 = (int)Float.parseFloat(args[0].toString());
          int args1 = (int)Float.parseFloat(args[1].toString());

          int[] sc = new int[current_picnome_num];
          int[] sr = new int[current_picnome_num];

          for(int j = 0; j < current_picnome_num; j++) {
            if(!checkAddressPatternPrefix(message, j))
              continue;

            if(cable_orientation[j].equals("left"))
              sc[j] = args0 - starting_column[j];
            else if(cable_orientation[j].equals("right"))
              sc[j] = co_max_num[j] - args0 + starting_column[j];
            else if(cable_orientation[j].equals("up"))
              sc[j] = args0 - starting_column[j];
            else if(cable_orientation[j].equals("down"))
              sc [j]= 7 - args0 + starting_column[j];

            if(sc[j] < 0) continue ;

            int shift = starting_row[j] % (co_max_num[j] + 1);

            if(cable_orientation[j].equals("left"))
              sr[j] = (char)(args1 >> shift);
            else if(cable_orientation[j].equals("right")) {
              char sr0 = (char)args1;
              char sr1 = 0;
              for(int i = 0; i < 8; i++)
                if((sr0 & (0x01 << i)) == (0x01 << i))
                  sr1 |= (0x01 << (7 - i));
              sr[j] = (char)(sr1 << shift);
            }
            else if(cable_orientation[j].equals("up")) {
              char sr0 = (char)args1;
              char sr1 = 0;
              for(int i = 0; i < co_max_num[j] + 1; i++)
                if((sr0 & (0x01 << i)) == (0x01 << i))
                  sr1 |= (0x01 << (co_max_num[j] - i));
              sr[j] = (char)(sr1 << shift);
            }
            else if(cable_orientation[j].equals("down"))
              sr[j] = (char)(args1 >> shift);

            try {
              String str;
              if(cable_orientation[j].equals("left") || cable_orientation[j].equals("right"))
                str =new String("lc " + sc[j] + " " + sr[j] + (char)0x0D); // (l)ed_(c)ol
              else
                str =new String("lr " + sc[j] + " " + sr[j] + (char)0x0D); // (l)ed_(r)ow
              //debug debug_tf.setText(str);
              if(checkPortState(j)) {
                sendDataToSerial(j, str);
                wait(0, 5);
              }
            }
            catch(InterruptedException e) {}
          }//end for
        }
      };
    oscpin.addListener(prefix_tf.getText() + "/led_col", listener);
  }

  private void enableMsgLedRow() {
    OSCListener listener = new OSCListener() {
        public synchronized void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();

          int args0 = (int)Float.parseFloat(args[0].toString());
          int args1 = (int)Float.parseFloat(args[1].toString());

          int[] sc = new int[current_picnome_num];
          int[] sr = new int[current_picnome_num];

          for(int j = 0; j < current_picnome_num; j++) {
            if(!checkAddressPatternPrefix(message, j))
              continue;

            if(cable_orientation[j].equals("left"))
              sr[j] = args0 - starting_row[j];
            else if(cable_orientation[j].equals("right"))
              sr[j] = 7 - args0 + starting_row[j];
            else if(cable_orientation[j].equals("up"))
              sr[j] = co_max_num[j] - args0 + starting_row[j];
            else if(cable_orientation[j].equals("down"))
              sr[j] = args0 - starting_row[j];
            
            if(sr[j] < 0) continue;
            
            int shift = starting_column[j] % (co_max_num[j] + 1);
            
            if(cable_orientation[j].equals("left"))
              sc[j] = (char)(args1 >> shift);
            else if(cable_orientation[j].equals("right")) {
              char sc0 = (char)args1;
              char sc1 = 0;
              for(int i = 0; i < co_max_num[j] + 1; i++)
                if((sc0 & (0x01 << i)) == (0x01 << i))
                  sc1 |= (0x01 << (co_max_num[j] - i));
              sc[j] = (char)(sc1 << shift);
            }
            else if(cable_orientation[j].equals("up"))
              sc[j] = (char)(args1 >> shift);
            else if(cable_orientation[j].equals("down")) {
              char sc0 = (char)args1;
              char sc1 = 0;
              for(int i = 0; i < 8; i++)
                if((sc0 & (0x01 << i)) == (0x01 << i))
                  sc1 |= (0x01 << (7 - i));
              sc[j] = (char)(sc1 << shift);
            }

            try {
              String str;
              if(cable_orientation[j].equals("left") || cable_orientation[j].equals("right"))
                str =new String("lr " + sr[j] + " " + sc[j] + (char)0x0D); // (l)ed_(r)ow
              else
                str =new String("lc " + sr[j] + " " + sc[j] + (char)0x0D); // (l)ed_(c)o0

              //debug debug_tf.setText(str);
              if(checkPortState(j)) {
                sendDataToSerial(j, str);
                wait(0, 5);
              }
            }
            catch(InterruptedException e) {}
          }//end for
        }
      };
    oscpin.addListener(prefix_tf.getText() + "/led_row", listener);
  }

  private void enableMsgLedFrame() {
    OSCListener listener = new OSCListener() {
        public synchronized void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args0 = message.getArguments();
          int[] sc = new int[current_picnome_num];
          int[] sr = new int[current_picnome_num];

          int[] args = new int[16];
          for(int i = 0; i < args0.length; i++)
            args[i] = (int)Float.parseFloat(args0[i].toString());

          for(int k = 0; k < current_picnome_num; k++) {
            if(!checkAddressPatternPrefix(message, k))
              continue;

            int shift = starting_column[k] % (co_max_num[k] + 1);

            for(int i = 0; i < 16; i++) {
              if(cable_orientation[k].equals("left"))
                sr[k] = i - starting_row[k];
              else if(cable_orientation[k].equals("right"))
                sr[k] = 7 - i + starting_row[k];
              else if(cable_orientation[k].equals("up"))
                sr[k] = co_max_num[k] - i + starting_row[k];
              else if(cable_orientation[k].equals("down"))
                sr[k] = i - starting_row[k];

              if(i < starting_row[k] || (i - starting_row[k]) > 7) continue;

              if(cable_orientation[k].equals("left"))
                sc[k] = (char)(args[i] >> shift);
              else if(cable_orientation[k].equals("right")) {
                char sc0 = (char)args[i];
                char sc1 = 0;
                for(int j = 0; j < co_max_num[k] + 1; j++)
                  if((sc0 & (0x01 << j)) == (0x01 << j))
                    sc1 |= (0x01 << (co_max_num[k] - j));
                sc[k] = (char)(sc1 << shift);
              }
              else if(cable_orientation[k].equals("up"))
                sc[k] = (char)(args[i] >> shift);
              else if(cable_orientation[k].equals("down")) {
                char sc0 = (char)args[i];
                char sc1 = 0;
                for(int j = 0; j < 8; j++)
                  if((sc0 & (0x01 << j)) == (0x01 << j))
                    sc1 |= (0x01 << (7 - j));
                sc[k] = (char)(sc1 << shift);
              }
              
              try {
                String str;
                if(cable_orientation[k].equals("left") || cable_orientation[k].equals("right"))
                  str =new String("lr " + sr[k] + " " + sc[k] + (char)0x0D); // (l)ed_(r)ow
                else
                  str =new String("lc " + sr[k] + " " + sc[k] + (char)0x0D); // (l)ed_(c)ol
                
                //debug debug_tf.setText(str);
                if(checkPortState(k)) {
                  sendDataToSerial(k, str);
                  wait(0, 5);
                }
              }
              catch(InterruptedException e) {}
            }//end for i
          }//end for j
        }
      };
    oscpin.addListener(prefix_tf.getText() + "/frame", listener);
  }

  private void enableMsgClear() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();

          int args0 = 0;
          if(args.length == 1)
            args0 = (int)Float.parseFloat(args[0].toString());

          for(int j = 0; j < current_picnome_num; j++) {
            if(!checkAddressPatternPrefix(message, j))
              continue;

            for(int i = 0; i < 8; i++) {
              int state;
              if(co_max_num[j] == 7) {//sixty four
                if(args.length == 0 || args0 == 0)
                  state = 0;
                else
                  state = 255;
              }
              else {//one twenty eight
                if(args.length == 0 || args0 == 0)
                  state = 0;
                else
                  state = 65535;
              }

              String str =new String("lr " + i + " " + state + (char)0x0D);
              //debug debug_tf.setText(str);
              if(checkPortState(j))
                sendDataToSerial(j, str);
            }
          }
        }
      };
    oscpin.addListener(prefix_tf.getText() + "/clear", listener);
  }

  private void enableMsgAdcEnable() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args0 = message.getArguments();

          int[] args = new int[args0.length];
          for(int i = 0; i < args.length; i++)
            args[i] = (int)Float.parseFloat(args0[i].toString());

          //sy String str =new String("adc_enable " + (Integer)args[0] + " " + (Integer)args[1] + (char)0x0D);
          String str =new String("ae " + args[0] + " " + args[1] + (char)0x0D);
          //debug debug_tf.setText(str);
          if(checkPortState(0))
            sendDataToSerial(0, str);
          if(checkPortState(1))
            sendDataToSerial(1, str);
        }
      };
    oscpin.addListener(prefix_tf.getText() + "/adc_enable", listener);
  }

/*
  private void enableMsgPwm() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args0 = message.getArguments();

          int[] args = new int[2];
          for(int i = 0; i < 2; i++)
            args[i] = (int)Float.parseFloat(args0[i].toString());
          float args_f = Float.parseFloat(args0[2].toString());

          try {
            //sy String str =new String("pwm " + (Integer)args[0] + " " + (Integer)args[1] + " " + (Float)args[2] + (char)0x0D);
            String str =new String("pwm " + args[0] + " " + args[1] + " " + args_f + (char)0x0D);
            //debug debug_tf.setText(str);
            if(portId[0] != null && portId[0].isCurrentlyOwned())
              out[0].write(str.getBytes());
            if(portId[1] != null && portId[1].isCurrentlyOwned())
              out[1].write(str.getBytes());
          }
          catch(IOException e) {}
        }
      };
    oscpin.addListener(prefix_tf.getText() + "/pwm", listener);
  }
*/

/*sy
  private void enableMsgOutput()
  {
    OSCListener listener = new OSCListener()
      {
        public void acceptMessage(java.util.Date time, OSCMessage message)
        {
          Object[] args = message.getArguments();

          try
          {
            String str =new String("output " + (Integer)args[0] + " " + (Integer)args[1] + (char)0x0D);
            //debug debug_tf.setText(str);
            out.write(str.getBytes());
          }
          catch(IOException e){}
        }
      };
    oscpin.addListener(prefix_tf.getText() + "/output", listener);
  }
*/

  private void enableMsgDevice() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();
          int device_no = ((Integer)args[0]).intValue();
          if(device_no == 0 || device_no == 1) {
            device_cb.setSelectedIndex(device_no);
            changeDeviceSettings(device_no);
          }
        }
      };
    oscpin.addListener("/sys/device", listener);
  }

  private void enableMsgOscconfig() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();
          address_pattern_prefix[(Integer)args[2]] = (String)args[0];
          host_port[(Integer)args[2]] = (Integer)args[1];
          if(device_cb.getSelectedIndex() == (Integer)args[2]) {
            prefix_tf.setText((String)args[0]);
            hostport_tf.setText(((Integer)args[1]).toString());
          }

          if(args.length == 4) {
            host_address[(Integer)args[2]] = Integer.toString((Integer)args[3]);
            if(device_cb.getSelectedIndex() == (Integer)args[2])
              hostaddress_tf.setText(Integer.toString((Integer)args[3]));
          }
          initOSCPort();
          initOSCListener("all");
        }
      };
    oscpin.addListener("/sys/oscconfig", listener);
  }

  private void enableMsgPrefix() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();
          if(args.length == 0) {
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
          else if(args.length == 2) {
            address_pattern_prefix[(Integer)args[0]] = (String)args[1];
            if(device_cb.getSelectedIndex() == (Integer)args[0])
              prefix_tf.setText((String)args[1]);
            initOSCListener("prefix");
          }
          else {
            prefix_tf.setText((String)args[0]);
            initOSCListener("prefix");
          }
        }
      };
    oscpin.addListener("/sys/prefix", listener);
  }

  private void enableMsgIntensity() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();

          for(int i = 0; i < current_picnome_num; i++) {
            if(args.length == 1) {
              String str =new String("i " + (Integer)args[0] + (char)0x0D);
              //debug debug_tf.setText(str);
              if(checkPortState(i))
                sendDataToSerial(i, str);
            }
            else if(args.length == 2 && (Integer)args[0] == i) {
              String str =new String("i " + (Integer)args[1] + (char)0x0D);
              //debug debug_tf.setText(str);
              if(checkPortState(i))
                sendDataToSerial(i, str);
            }
          }
        }
      };
    oscpin.addListener("/sys/intensity", listener);
  }

  private void enableMsgTest() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();

          for(int i = 0; i < current_picnome_num; i++) {
            if(args.length == 1) {
              String str =new String("t " + (Integer)args[0] + (char)0x0D);
              if(checkPortState(i))
                sendDataToSerial(i, str);
            }
            else if(args.length == 2 && (Integer)args[0] == i) {
              String str =new String("t " + (Integer)args[1] + (char)0x0D);
              if(checkPortState(i))
                sendDataToSerial(i, str);
            }
          }
        }
      };
    oscpin.addListener("/sys/test", listener);
  }

  private void enableMsgShutdown() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();

          for(int i = 0; i < current_picnome_num; i++) {
            String str =new String("s " + (Integer)args[0] + (char)0x0D);
            //debug debug_tf.setText(str);
            if(checkPortState(i))
              sendDataToSerial(i, str);
          }
        }
      };
    oscpin.addListener("/sys/shutdown", listener);
  }

  private void enableMsgReport() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();
          Object[] args0;
          String str;
          OSCMessage msg;

          //debug debug_tf.setText(String.valueOf(args.length));

          try {
            if(args.length == 0) {
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
            else {
              str = new String("r " + (Integer)args[0] + (char)0x0D);
              //debug debug_tf.setText(str);
              if(checkPortState(0))
                sendDataToSerial(0, str);
              if(checkPortState(1))
                sendDataToSerial(1, str);
            }
          }
          catch(IOException e) {}
        }
      };
    oscpin.addListener("/sys/report", listener);
  }

  private void enableMsgType() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();
          Object[] args0;
          String str;
          OSCMessage msg;

          try {
            if(args.length == 0) {
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
      };
    oscpin.addListener("/sys/type", listener);
  }

  private void enableMsgOffset() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();
          if(args.length == 2) {
            startcolumn_s.setValue((Integer)args[0]);
            startrow_s.setValue((Integer)args[1]);
          }
          else if(args.length == 3) {
            starting_column[(Integer)args[0]] = (Integer)args[1];
            starting_row[(Integer)args[0]] = (Integer)args[2];
            if(device_cb.getSelectedIndex() == (Integer)args[0]) {
              startcolumn_s.setValue((Integer)args[1]);
              startrow_s.setValue((Integer)args[2]);
            }
          }
        }
      };
    oscpin.addListener("/sys/offset", listener);
  }

  private void enableMsgCable() {
    OSCListener listener = new OSCListener() {
        public void acceptMessage(java.util.Date time, OSCMessage message) {
          Object[] args = message.getArguments();
          if(args.length == 0) {
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
          else if(args.length == 2) {
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
            initOSCListener("prefix");
          }
          else
            cable_cb.setSelectedItem(((String)args[0]));
        }
      };
    oscpin.addListener("/sys/cable", listener);
  }

  public void initOSCListener(String str) {
    if(((String)device_cb.getSelectedItem()).equals(device[0]))
      address_pattern_prefix[0] = prefix_tf.getText();
    else if(((String)device_cb.getSelectedItem()).equals(device[1]))
      address_pattern_prefix[1] = prefix_tf.getText();

    if(str.equals("all") || str.equals("prefix")) {
      enableMsgLed();
      enableMsgLedCol();
      enableMsgLedRow();
      enableMsgLedFrame();
      enableMsgClear();
      enableMsgAdcEnable();
      //sy enableMsgPwm();
      //sy enableMsgOutput();
    }
    if(str.equals("all")) {
      enableMsgDevice();
      enableMsgOscconfig();
      enableMsgPrefix();
      enableMsgIntensity();
      enableMsgTest();
      enableMsgShutdown();
      enableMsgReport();
      enableMsgType();
      enableMsgOffset();
      enableMsgCable();

      oscpin.startListening();
    }
  }

  public class SerialReader implements Runnable {
    private int index;
    private InputStreamReader inr;

    SerialReader(int index, InputStreamReader inr) {
      this.index = index;
      this.inr = inr;
    }

    public void run() {
      int buffer = 0;
      StringBuilder sb = new StringBuilder();
      try {
        while((buffer = inr.read()) != -1) {
          if(buffer == 0x0A || buffer == 0x0D) {
            //debug System.out.println(sb.toString());
            sendOSCMessageFromHw(index, sb.toString());
            sb = new StringBuilder();
          }
          else
            sb.append((char)buffer);
        }
      }
      catch(IOException e) {}
    }
  }
}