/*
 * Copylight (C) 2009, Shunichi Yamamoto, tkrworks.net
 *
 * This file is part of PicnomeSerial.
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
 * PicnomeCommunication.java,v.0.98 2009/06/18
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

class PicnomeCommunication
{
  Vector<String> device_vec = new Vector<String>();
  JComboBox protocol_cb, device_cb;
  JTextField hostaddress_tf, prefix_tf,startcolumn_tf,startrow_tf, hostport_tf, listenport_tf, hex_tf;
  JButton hex_b, update_b;
  JProgressBar update_pb;

/* for DEBUG
  JTextField debug_tf;
  JTextField debug2_tf;
*/
  CommPortIdentifier portId;
  SerialPort port;
  InputStream in;
  InputStreamReader inr;
  OutputStream out;

  OSCPortIn oscpin;
  OSCPortOut oscpout;

  //for Windows XP/Vista
  MidiDevice myjin,myjout;
  Receiver myjr;
  Transmitter myjt;

  PicnomeCommunication()
  {
    this.in = null;
    this.out = null;
    this.initDeviceList();
  }

  void initDeviceList()
  {
    String device_name;
    Enumeration e = CommPortIdentifier.getPortIdentifiers();
    while(e.hasMoreElements())
    {
      device_name = ((CommPortIdentifier)e.nextElement()).getName();
      if(System.getProperty("os.name").startsWith("Mac OS X"))
      {
	if(device_name.indexOf("/dev/cu.") != -1)
	  this.device_vec.add(device_name);
      }
      else if(System.getProperty("os.name").startsWith("Windows"))
      {
	this.device_vec.add(device_name);
      }
    }
  }

  boolean openSerialPort()
  {
    try
    {
      this.portId = CommPortIdentifier.getPortIdentifier((String)this.device_cb.getSelectedItem());
      this.port = (SerialPort)portId.open("PICnomeSerial", 2000);
    }
    catch (NoSuchPortException e)
    {
      e.printStackTrace();
      return false;
    }
    catch (PortInUseException e)
    {
      e.printStackTrace();
      return false;
    }

    try
    {
      this.in = this.port.getInputStream();
      this.inr = new InputStreamReader(this.in);
      this.out = this.port.getOutputStream();
      this.initSerialListener();

      if(((String)this.protocol_cb.getSelectedItem()).equals("Open Sound Control"))
      {
	this.initOSCPort();
	this.initOSCListener();
      }
      else
      {
	try
	{
	  javax.sound.midi.MidiDevice.Info[] info = javax.sound.midi.MidiSystem.getMidiDeviceInfo();
	  for(int i = 0; i < info.length; i++)
	  {
	    if(info[i].getName().equals("In From MIDI Yoke:  1"))
	    {
	      this.myjin = javax.sound.midi.MidiSystem.getMidiDevice(info[i]);
	      this.myjin.open();
	      this.myjt = this.myjin.getTransmitter();
	    }
	    else if(info[i].getName().equals("Out To MIDI Yoke:  1"))
	    {
	      this.myjout = javax.sound.midi.MidiSystem.getMidiDevice(info[i]);
	      this.myjout.open();
	      this.myjr = this.myjout.getReceiver();
	    }
	  }
	}
	catch(MidiUnavailableException mue){}

	this.initMidiListener();
      }
    }
    catch(IOException e){}
    return true;
  }

  boolean setSerialPort()
  {
    try
    {
      this.port.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
      this.port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
    }
    catch (UnsupportedCommOperationException e)
    {
      e.printStackTrace();
      return false;
    }
    this.port.setDTR(true);
    this.port.setRTS(false);
    return true;
  }

  boolean closeSerialPort()
  {
    try
    {
      if(((String)this.protocol_cb.getSelectedItem()).equals("Open Sound Control"))
	this.oscpin.stopListening();
      else
      {
	this.myjin.close();
	this.myjout.close();
	this.myjr.close();
	this.myjt.close();
      }
      this.inr.close();
      this.in.close();
      this.out.flush();
      this.out.close();
      this.port.close();

    }
    catch(Exception e)
    {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  void initOSCPort()
  {
    byte[] hostaddress = new byte[4];
    String ha_str = this.hostaddress_tf.getText();

    int idx = 0, idx2;
    for(int i = 0; i < 3; i++)
    {
      idx2 = ha_str.indexOf(".", idx);
      hostaddress[i] = Byte.parseByte(ha_str.substring(idx, idx2));
      idx = idx2 + 1;
    }
    hostaddress[3] = Byte.parseByte(ha_str.substring(idx, ha_str.length()));

    try
    {
      this.oscpin = new OSCPortIn(Integer.parseInt(this.listenport_tf.getText()));
      this.oscpout = new OSCPortOut(InetAddress.getByAddress(hostaddress), Integer.parseInt(this.hostport_tf.getText()));
    }
    catch(UnknownHostException e){}
    catch(SocketException e){}
  }

  void sendOSCMessageFromHw(String str)
  {
    StringTokenizer st = new StringTokenizer(str);
    Object[] args;
    OSCMessage msg;
    String token = st.nextToken();
    if(token.equals("press"))
    {
      if(((String)this.protocol_cb.getSelectedItem()).equals("Open Sound Control"))
      {
	args = new Object[3];

	args[0] = Integer.valueOf(st.nextToken()); // X
	args[1] = Integer.valueOf(st.nextToken()); // Y
	args[2] = Integer.valueOf(st.nextToken()); // State

	msg = new OSCMessage(this.prefix_tf.getText() + "/press", args);
	try
	{
	  this.oscpout.send(msg);
	}
	catch(IOException e){}
      }
      else // for MIDI
      {
	int notex = Integer.valueOf(st.nextToken());
	int notey = Integer.valueOf(st.nextToken());
	int state = Integer.valueOf(st.nextToken());
	int note = notex + (notey * 8);

	try
	{
	  ShortMessage sm = new ShortMessage();
	  if(state == 1)
	    sm.setMessage(ShortMessage.NOTE_ON, (byte)note, 127);
	  else
	    sm.setMessage(ShortMessage.NOTE_OFF, (byte)note, 0);
	  this.myjr.send(sm, 1);
	}
	catch(InvalidMidiDataException imde){}
      }
    }
    else if(token.equals("adc"))
    {
      args = new Object[2];

      args[0] = Integer.valueOf(st.nextToken()); // Pin
      args[1] = Float.valueOf(st.nextToken());   // Value

      msg = new OSCMessage(this.prefix_tf.getText() + "/adc", args);
      try
      {
	this.oscpout.send(msg);
      }
      catch(IOException e){}
    }
    else if(token.equals("report"))
    {
      int v1,v2;

      v1 = Integer.valueOf(st.nextToken());
      v2 = Integer.valueOf(st.nextToken());

      if(v2 == 1)
      {
	this.hex_tf.setEnabled(false);
	this.hex_b.setEnabled(false);
	this.update_b.setEnabled(false);
      }
    }
  }

  public void enableMsgLed()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();
	  int sc = Integer.parseInt(startcolumn_tf.getText());
	  int sr = Integer.parseInt(startrow_tf.getText());
	  sc = (Integer)args[0] - sc;
	  sr = (Integer)args[1] - sr;
	  if(sc < 0) sc = 0;
	  if(sr < 0) sr = 0;

	  try
	  {
	    String str =new String("led " + sc + " " + sr + " " + (Integer)args[2] + (char)0x0D);
	    //debug debug_tf.setText(str);
	    out.write(str.getBytes());
	  }
	  catch(IOException e){}
	}
      };
    this.oscpin.addListener(this.prefix_tf.getText() + "/led", listener);
  }

  public void enableMidiLed()
  {
    Receiver rcv = new Receiver()
      {
	public void close(){}

	public void send(MidiMessage message, long timeStamp)
	{

	  byte[] data = message.getMessage();

	  if((256 + data[0]) == 144 || (256 + data[0]) == 128)// NOTE_ON -> 144, NOTE_OFF -> 128
	  {
	    int sc = Integer.parseInt(startcolumn_tf.getText());
	    int sr = Integer.parseInt(startrow_tf.getText());
	    sc = (data[1] % 8) - sc;
	    sr = (data[1] / 8) - sr;
	    if(sc < 0) sc = 0;
	    if(sr < 0) sr = 0;

	    try
	    {
	      String str;
	      if(data[2] == 0)
		str =new String("led " + sc + " " + sr + " " + 0 + (char)0x0D);
	      else
		str =new String("led " + sc + " " + sr + " " + 1 + (char)0x0D);
	      out.write(str.getBytes());
	    }
	    catch(IOException e){}
	  }
	}
      };
    this.myjt.setReceiver(rcv);
  }


  public void enableMsgLedCol()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();
	  int sc = Integer.parseInt(startcolumn_tf.getText());
	  sc = (Integer)args[0] - sc;
	  if(sc < 0) sc = 0;

	  int shift = Integer.parseInt(startrow_tf.getText()) % 8;

	  int sr = (short)(((Integer)args[1]).shortValue() >> shift);

	  try
	  {
	    String str =new String("led_col " + sc + " " + sr + (char)0x0D);
	    //debug debug_tf.setText(str);
	    out.write(str.getBytes());
	  }
	  catch(IOException e){}
	}
      };
    this.oscpin.addListener(this.prefix_tf.getText() + "/led_col", listener);
  }

  public void enableMsgLedRow()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();
	  int sr = Integer.parseInt(startrow_tf.getText());
	  sr = (Integer)args[0] - sr;
	  if(sr < 0) sr = 0;

	  int shift = Integer.parseInt(startcolumn_tf.getText()) % 8;

	  int sc = (short)(((Integer)args[1]).shortValue() >> shift);

	  try
	  {
	    String str =new String("led_row " + sr + " " + sc + (char)0x0D);
	    //debug debug_tf.setText(str);
	    out.write(str.getBytes());
	  }
	  catch(IOException e){}
	}
      };
    this.oscpin.addListener(this.prefix_tf.getText() + "/led_row", listener);
  }

  public void enableMsgLedFrame()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();

	  int sr = Integer.parseInt(startrow_tf.getText());
	  int shift = Integer.parseInt(startcolumn_tf.getText()) % 8;

	  for(int i = 0; i < 8; i++)
	  {
	    if(i < sr) return;

	    int sc = (short)(((Integer)args[i]).shortValue() >> shift);

	    try
	    {
	      String str =new String("led_row " + (i - sr) + " " + sc + (char)0x0D);
	      //debug debug_tf.setText(str);
	      out.write(str.getBytes());
	    }
	    catch(IOException e){}
	  }
	}
      };
    this.oscpin.addListener(this.prefix_tf.getText() + "/frame", listener);
  }

  public void enableMsgClear()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();
	  for(int i = 0; i < 8; i++)
	  {
	    short state;
	    if(((Integer)args[0]).intValue() == 0)
	      state = (short)0x00;
	    else
	      state = (short)0xFF;

	    try
	    {
	      String str =new String("led_row " + i + " " + state + (char)0x0D);
	      //debug debug_tf.setText(str);
	      out.write(str.getBytes());
	    }
	    catch(IOException e){}
	  }
	}
      };
    this.oscpin.addListener(this.prefix_tf.getText() + "/clear", listener);
  }

  public void enableMsgAdcEnable()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();

	  try
	  {
	    String str =new String("adc_enable " + (Integer)args[0] + " " + (Integer)args[1] + (char)0x0D);
	    //debug debug_tf.setText(str);
	    out.write(str.getBytes());
	  }
	  catch(IOException e){}
	}
      };
    this.oscpin.addListener(this.prefix_tf.getText() + "/adc_enable", listener);
  }

  public void enableMsgPrefix()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();
	  prefix_tf.setText((String)args[0]);
	  PicnomeCommunication.this.enableMsgLed();
	  PicnomeCommunication.this.enableMsgLedCol();
	  PicnomeCommunication.this.enableMsgLedRow();
	  PicnomeCommunication.this.enableMsgLedFrame();
	  PicnomeCommunication.this.enableMsgClear();
	  PicnomeCommunication.this.enableMsgAdcEnable();
	}
      };
    this.oscpin.addListener("/sys/prefix", listener);
  }

  public void enableMsgIntensity()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();

	  try
	  {
	    String str =new String("intensity " + (Integer)args[0] + (char)0x0D);
	    //debug debug_tf.setText(str);
	    out.write(str.getBytes());
	  }
	  catch(IOException e){}
	}
      };
    this.oscpin.addListener("/sys/intensity", listener);
  }

  public void enableMsgTest()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();

	  try
	  {
	    String str =new String("test " + (Integer)args[0] + (char)0x0D);
	    //debug debug_tf.setText(str);
	    out.write(str.getBytes());
	  }
	  catch(IOException e){}
	}
      };
    this.oscpin.addListener("/sys/test", listener);
  }

  public void enableMsgShutdown()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();

	  try
	  {
	    String str =new String("shutdown " + (Integer)args[0] + (char)0x0D);
	    //debug debug_tf.setText(str);
	    out.write(str.getBytes());
	  }
	  catch(IOException e){}
	}
      };
    this.oscpin.addListener("/sys/shutdown", listener);
  }

  public void enableMsgReport()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();

	  try
	  {
	    String str =new String("report " + (Integer)args[0] + (char)0x0D);
	    //debug debug_tf.setText(str);
	    out.write(str.getBytes());
	  }
	  catch(IOException e){}
	}
      };
    this.oscpin.addListener("/sys/report", listener);
  }


  public void enableMsgOffset()
  {
    OSCListener listener = new OSCListener()
      {
	public void acceptMessage(java.util.Date time, OSCMessage message)
	{
	  Object[] args = message.getArguments();
	  startcolumn_tf.setText(((Integer)args[0]).toString());
	  startrow_tf.setText(((Integer)args[1]).toString());
	}
      };
    this.oscpin.addListener("/sys/offset", listener);
  }

  public void initOSCListener()
  {
    this.enableMsgLed();
    this.enableMsgLedCol();
    this.enableMsgLedRow();
    this.enableMsgLedFrame();
    this.enableMsgClear();
    this.enableMsgAdcEnable();
    this.enableMsgPrefix();
    this.enableMsgIntensity();
    this.enableMsgTest();
    this.enableMsgShutdown();
    this.enableMsgReport();
    this.enableMsgOffset();
    this.oscpin.startListening();
  }

  public void initMidiListener()
  {
    this.enableMidiLed();
  }

  class SerialPortListener implements SerialPortEventListener
  {
    public void serialEvent(SerialPortEvent event)
    {
      if(event.getEventType() == SerialPortEvent.DATA_AVAILABLE)
      {
	int buffer = 0;
	try
	{
	  StringBuffer sb = new StringBuffer();
	  while((buffer = inr.read()) != -1)
	  {
	    if(buffer != 0x0A || buffer != 0x0D)
	      sb.append((char)buffer);
	    if(buffer == 0x0A || buffer == 0x0D)
	      break;
	  }
	  //sy debug2_tf.setText(sb.toString());
	  sendOSCMessageFromHw(sb.toString());
	}
	catch(IOException e){}
      }
    }
  }

  void initSerialListener()
  {
    try
    {
      this.port.addEventListener(new SerialPortListener());
      this.port.notifyOnDataAvailable(true);
    }
    catch (TooManyListenersException e){}
  }
}