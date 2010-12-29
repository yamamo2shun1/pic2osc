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
 * PicnomeSerial.java,v.1.4.12(127) 2010/12/29
 */

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
//You have to comment out if you compile win version.
import com.apple.eawt.*;

public class PicnomeSerial extends JFrame implements ActionListener, ChangeListener {
  final static int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  PicnomeCommunication pserial = new PicnomeCommunication();
  private MidiDetailFrame mdf = new MidiDetailFrame();

  private JPanel psd_p;
  private CardLayout psd_cl;
  private File hex_f;
  private FileReader hex_fr;
  private Timer timer;
  private int ch;
  private int size;
  private int count;
  private int bar;
  private int prev_index;
  private boolean para_change_flag;

  public PicnomeSerial() {
    super("PICnome Serial ver. / f/w ver.");

    init();
    if(System.getProperty("os.name").startsWith("Mac OS X")) {
      setSize(450, 695);
      //You have to comment out if you compile win version.
      Application app = Application.getApplication();
      app.addApplicationListener(new ApplicationAdapter() {
          public void handleQuit(ApplicationEvent arg0) {
            for(int i = 0; i < 6; i++) {
              String str =new String("ae " + i + " " + 0 + (char)0x0D);
              if(pserial.getCurrentNum() == 1) {
                pserial.setAdcEnable(0, i, false);
                pserial.sendDataToSerial(0, str);
              }
              if(pserial.getCurrentNum() == 2) {
                pserial.setAdcEnable(1, i, false);
                pserial.sendDataToSerial(1, str);
              }
            }
            System.exit(0);
          }
        });

    }
    else if(System.getProperty("os.name").startsWith("Windows"))
      setSize(470, 740);
    addWindowListener(
      new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          for(int i = 0; i < 6; i++) {
            String str =new String("ae " + i + " " + 0 + (char)0x0D);
            if(pserial.getCurrentNum() == 1) {
              pserial.setAdcEnable(0, i, false);
              pserial.sendDataToSerial(0, str);
            }
            if(pserial.getCurrentNum() == 2) {
              pserial.setAdcEnable(1, i, false);
              pserial.sendDataToSerial(1, str);
            }
          }
          System.exit(0);
        }
      }
      );
    mdf.init();
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      mdf.setBounds(450, 0, 890, 620);// mac
    else if(System.getProperty("os.name").startsWith("Windows"))
      mdf.setBounds(470, 40, 990, 665);// win
  }

  public static void main(String[] args) {
    PicnomeSerial psgui = new PicnomeSerial();

    psgui.setVisible(true);

    for(int i = 0; i < psgui.pserial.getCurrentNum(); i++) {
        psgui.pserial.openSerialPort(i);
        psgui.pserial.setSerialPort(i);
        if(psgui.pserial.getCurrentMaxColumn(i) == 7)
          psgui.mdf.setHalfVisible();
    }
    int timeout_count = 0;
    while(!psgui.pserial.isFirmwareVersion()) {
      timeout_count++;
      if(timeout_count > 65536)
        break;
    }
    psgui.setTitle("PICnomeSerial " + psgui.pserial.getAppVersion() + " /  " + psgui.pserial.getFirmwareVersion());
  }

  private void init() {
    SpringLayout sl = new SpringLayout();
    Container c = getContentPane();
    c.setLayout(sl);

    JMenuBar main_mb = new JMenuBar();
    JMenu file_m = new JMenu("File");
    main_mb.add(file_m);
    JMenuItem open_mi = new JMenuItem("Open");
    JMenuItem save_mi = new JMenuItem("Save");
    JMenuItem saveas_mi = new JMenuItem("Save As...");
    file_m.add(open_mi);
    file_m.add(save_mi);
    file_m.add(saveas_mi);
    setJMenuBar(main_mb);

    //Protocol Settings
    JPanel ps_p = new JPanel();
    SpringLayout ps_sl = new SpringLayout();
    ps_p.setLayout(ps_sl);
    SoftBevelBorder ps_inborder = new SoftBevelBorder(SoftBevelBorder.LOWERED);
    TitledBorder ps_outborder = new TitledBorder(ps_inborder, "Protocol Settings", TitledBorder.LEFT, TitledBorder.ABOVE_TOP);
    ps_p.setPreferredSize(new Dimension(430, 160));
    ps_p.setBorder(ps_outborder);
    sl.putConstraint(SpringLayout.NORTH, ps_p, 10, SpringLayout.NORTH, c);
    sl.putConstraint(SpringLayout.WEST, ps_p, 10, SpringLayout.WEST, c);
    c.add(ps_p);

    JLabel ioprotocol_l = new JLabel("I/O Protocol :");
    ps_sl.putConstraint(SpringLayout.NORTH, ioprotocol_l, 10, SpringLayout.NORTH, ps_p);
    ps_sl.putConstraint(SpringLayout.WEST, ioprotocol_l, 38, SpringLayout.WEST, ps_p);
    ps_p.add(ioprotocol_l);
    String[] protocol_str = {"Open Sound Control", "MIDI", "OSC/MIDI(ext.)", "OSC(LEDs)/MIDI(Pads)"};
    pserial.protocol_cb = new JComboBox(protocol_str);
    pserial.protocol_cb.setActionCommand("ProtocolChanged");
    pserial.protocol_cb.addActionListener(this);
    ps_sl.putConstraint(SpringLayout.NORTH, pserial.protocol_cb, -4, SpringLayout.NORTH, ioprotocol_l);
    ps_sl.putConstraint(SpringLayout.WEST, pserial.protocol_cb, 10, SpringLayout.EAST, ioprotocol_l);
    ps_p.add(pserial.protocol_cb);

    psd_p = new JPanel();
    psd_cl = new CardLayout();
    psd_p.setLayout(psd_cl);

    psd_p.setPreferredSize(new Dimension(400, 90));
    ps_sl.putConstraint(SpringLayout.NORTH, psd_p, 40, SpringLayout.NORTH, ps_p);
    ps_sl.putConstraint(SpringLayout.WEST, psd_p, 10, SpringLayout.WEST, ps_p);
    ps_p.add(psd_p);

    //Open Sound Control Setting
    JPanel osc_p = new JPanel();
    SpringLayout osc_sl = new SpringLayout();
    osc_p.setLayout(osc_sl);
    osc_p.setPreferredSize(new Dimension(400, 90));
    psd_p.add(osc_p, "osc");

    JLabel hostaddress_l = new JLabel("Host Address :");
    osc_sl.putConstraint(SpringLayout.NORTH, hostaddress_l, 5, SpringLayout.NORTH, osc_p);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_sl.putConstraint(SpringLayout.WEST, hostaddress_l, 20, SpringLayout.WEST, osc_p);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_sl.putConstraint(SpringLayout.WEST, hostaddress_l, 17, SpringLayout.WEST, osc_p);
    osc_p.add(hostaddress_l);

    pserial.hostaddress_tf = new JTextField("127.0.0.1", 10);
    pserial.hostaddress_tf.addActionListener(this);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_sl.putConstraint(SpringLayout.NORTH, pserial.hostaddress_tf, -6, SpringLayout.NORTH, hostaddress_l);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_sl.putConstraint(SpringLayout.NORTH, pserial.hostaddress_tf, -1, SpringLayout.NORTH, hostaddress_l);
    osc_sl.putConstraint(SpringLayout.WEST, pserial.hostaddress_tf, 10, SpringLayout.EAST, hostaddress_l);
    osc_p.add(pserial.hostaddress_tf);

    JLabel hostport_l = new JLabel("Host Port :");
    osc_sl.putConstraint(SpringLayout.NORTH, hostport_l, 35, SpringLayout.NORTH, osc_p);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_sl.putConstraint(SpringLayout.WEST, hostport_l, 46, SpringLayout.WEST, osc_p);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_sl.putConstraint(SpringLayout.WEST, hostport_l, 41, SpringLayout.WEST, osc_p);
    osc_p.add(hostport_l);

    pserial.hostport_tf = new JTextField("8000", 3);
    pserial.hostport_tf.addActionListener(this);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_sl.putConstraint(SpringLayout.NORTH, pserial.hostport_tf, -6, SpringLayout.NORTH, hostport_l);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_sl.putConstraint(SpringLayout.NORTH, pserial.hostport_tf, -1, SpringLayout.NORTH, hostport_l);
    osc_sl.putConstraint(SpringLayout.WEST, pserial.hostport_tf, 10, SpringLayout.EAST, hostport_l);
    osc_p.add(pserial.hostport_tf);

    JLabel listenport_l = new JLabel("Listen Port :");
    osc_sl.putConstraint(SpringLayout.NORTH, listenport_l, 65, SpringLayout.NORTH, osc_p);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_sl.putConstraint(SpringLayout.WEST, listenport_l, 38, SpringLayout.WEST, osc_p);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_sl.putConstraint(SpringLayout.WEST, listenport_l, 32, SpringLayout.WEST, osc_p);
    osc_p.add(listenport_l);

    pserial.listenport_tf = new JTextField("8080", 3);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_sl.putConstraint(SpringLayout.NORTH, pserial.listenport_tf, -6, SpringLayout.NORTH, listenport_l);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_sl.putConstraint(SpringLayout.NORTH, pserial.listenport_tf, -1, SpringLayout.NORTH, listenport_l);
    osc_sl.putConstraint(SpringLayout.WEST, pserial.listenport_tf, 10, SpringLayout.EAST, listenport_l);
    osc_p.add(pserial.listenport_tf);

    //MIDI Setting
    JPanel midi_p = new JPanel();
    SpringLayout midi_sl = new SpringLayout();
    midi_p.setLayout(midi_sl);
    midi_p.setPreferredSize(new Dimension(400, 90));
    psd_p.add(midi_p, "midi");

    JLabel midiinput_l = new JLabel("MIDI Input :");
    midi_sl.putConstraint(SpringLayout.NORTH, midiinput_l, 5, SpringLayout.NORTH, midi_p);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      midi_sl.putConstraint(SpringLayout.WEST, midiinput_l, 38, SpringLayout.WEST, midi_p);
    else if(System.getProperty("os.name").startsWith("Windows"))
      midi_sl.putConstraint(SpringLayout.WEST, midiinput_l, 39, SpringLayout.WEST, midi_p);
    midi_p.add(midiinput_l);
    pserial.midiinput_cb = new JComboBox(pserial.getMidiInputList());
    pserial.midiinput_cb.setActionCommand("MidiInChanged");
    pserial.midiinput_cb.addActionListener(this);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      pserial.midiinput_cb.setPreferredSize(new Dimension(250, 30));
    midi_sl.putConstraint(SpringLayout.NORTH, pserial.midiinput_cb, -4, SpringLayout.NORTH, midiinput_l);
    midi_sl.putConstraint(SpringLayout.WEST, pserial.midiinput_cb, 10, SpringLayout.EAST, midiinput_l);
    midi_p.add(pserial.midiinput_cb);

    JLabel midioutput_l = new JLabel("MIDI Output :");
    midi_sl.putConstraint(SpringLayout.NORTH, midioutput_l, 35, SpringLayout.NORTH, midi_p);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      midi_sl.putConstraint(SpringLayout.WEST, midioutput_l, 27, SpringLayout.WEST, midi_p);
    else if(System.getProperty("os.name").startsWith("Windows"))
      midi_sl.putConstraint(SpringLayout.WEST, midioutput_l, 29, SpringLayout.WEST, midi_p);
    midi_p.add(midioutput_l);
    pserial.midioutput_cb = new JComboBox(pserial.getMidiOutputList());
    pserial.midioutput_cb.setActionCommand("MidiOutChanged");
    pserial.midioutput_cb.addActionListener(this);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      pserial.midioutput_cb.setPreferredSize(new Dimension(250, 30));
    midi_sl.putConstraint(SpringLayout.NORTH, pserial.midioutput_cb, -4, SpringLayout.NORTH, midioutput_l);
    midi_sl.putConstraint(SpringLayout.WEST, pserial.midioutput_cb, 10, SpringLayout.EAST, midioutput_l);
    midi_p.add(pserial.midioutput_cb);

    pserial.mididetail_b = new JButton("Detail...");
    pserial.mididetail_b.addActionListener(this);
    midi_sl.putConstraint(SpringLayout.NORTH, pserial.mididetail_b, 60, SpringLayout.NORTH, midi_p);
    midi_sl.putConstraint(SpringLayout.WEST, pserial.mididetail_b, 300, SpringLayout.WEST, midi_p);
    midi_p.add(pserial.mididetail_b);

    //OSC/MIDI(ext.) Setting
    JPanel osc_ext_midi_p = new JPanel();
    SpringLayout osc_ext_midi_sl = new SpringLayout();
    osc_ext_midi_p.setLayout(osc_ext_midi_sl);
    osc_ext_midi_p.setPreferredSize(new Dimension(400, 90));
    psd_p.add(osc_ext_midi_p, "osc-ext-midi");

    JLabel hostaddress_ext_midi_l = new JLabel("Host Address :");
    osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, hostaddress_ext_midi_l, 5, SpringLayout.NORTH, osc_ext_midi_p);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_ext_midi_sl.putConstraint(SpringLayout.WEST, hostaddress_ext_midi_l, 20, SpringLayout.WEST, osc_ext_midi_p);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_ext_midi_sl.putConstraint(SpringLayout.WEST, hostaddress_ext_midi_l, 17, SpringLayout.WEST, osc_ext_midi_p);
    osc_ext_midi_p.add(hostaddress_ext_midi_l);

    pserial.hostaddress_tf = new JTextField("127.0.0.1", 10);
    pserial.hostaddress_tf.addActionListener(this);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, pserial.hostaddress_tf, -6, SpringLayout.NORTH, hostaddress_ext_midi_l);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, pserial.hostaddress_tf, -1, SpringLayout.NORTH, hostaddress_ext_midi_l);
    osc_ext_midi_sl.putConstraint(SpringLayout.WEST, pserial.hostaddress_tf, 10, SpringLayout.EAST, hostaddress_ext_midi_l);
    osc_ext_midi_p.add(pserial.hostaddress_tf);

    JLabel hostport_ext_midi_l = new JLabel("Host Port :");
    osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, hostport_ext_midi_l, 35, SpringLayout.NORTH, osc_ext_midi_p);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_ext_midi_sl.putConstraint(SpringLayout.WEST, hostport_ext_midi_l, 46, SpringLayout.WEST, osc_ext_midi_p);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_ext_midi_sl.putConstraint(SpringLayout.WEST, hostport_ext_midi_l, 41, SpringLayout.WEST, osc_ext_midi_p);
    osc_ext_midi_p.add(hostport_ext_midi_l);

    pserial.hostport_tf = new JTextField("8000", 3);
    pserial.hostport_tf.addActionListener(this);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, pserial.hostport_tf, -6, SpringLayout.NORTH, hostport_ext_midi_l);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, pserial.hostport_tf, -1, SpringLayout.NORTH, hostport_ext_midi_l);
    osc_ext_midi_sl.putConstraint(SpringLayout.WEST, pserial.hostport_tf, 10, SpringLayout.EAST, hostport_ext_midi_l);
    osc_ext_midi_p.add(pserial.hostport_tf);

    JLabel listenport_ext_midi_l = new JLabel("Listen Port :");
    osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, listenport_ext_midi_l, 65, SpringLayout.NORTH, osc_ext_midi_p);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_ext_midi_sl.putConstraint(SpringLayout.WEST, listenport_ext_midi_l, 38, SpringLayout.WEST, osc_ext_midi_p);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_ext_midi_sl.putConstraint(SpringLayout.WEST, listenport_ext_midi_l, 32, SpringLayout.WEST, osc_ext_midi_p);
    osc_ext_midi_p.add(listenport_ext_midi_l);

    pserial.listenport_tf = new JTextField("8080", 3);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, pserial.listenport_tf, -6, SpringLayout.NORTH, listenport_ext_midi_l);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, pserial.listenport_tf, -1, SpringLayout.NORTH, listenport_ext_midi_l);
    osc_ext_midi_sl.putConstraint(SpringLayout.WEST, pserial.listenport_tf, 10, SpringLayout.EAST, listenport_ext_midi_l);
    osc_ext_midi_p.add(pserial.listenport_tf);

    JLabel midioutput_ext_midi_l = new JLabel("MIDI Output :");
    osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, midioutput_ext_midi_l, 35, SpringLayout.NORTH, osc_ext_midi_p);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_ext_midi_sl.putConstraint(SpringLayout.WEST, midioutput_ext_midi_l, 197, SpringLayout.WEST, osc_ext_midi_p);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_ext_midi_sl.putConstraint(SpringLayout.WEST, midioutput_ext_midi_l, 179, SpringLayout.WEST, osc_ext_midi_p);
    osc_ext_midi_p.add(midioutput_ext_midi_l);
    pserial.midioutput_cb = new JComboBox(pserial.getMidiOutputList());
    pserial.midioutput_cb.setActionCommand("MidiOutChanged");
    pserial.midioutput_cb.addActionListener(this);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      pserial.midioutput_cb.setPreferredSize(new Dimension(200, 30));
    osc_ext_midi_sl.putConstraint(SpringLayout.NORTH, pserial.midioutput_cb, 20, SpringLayout.NORTH, midioutput_ext_midi_l);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      osc_ext_midi_sl.putConstraint(SpringLayout.WEST, pserial.midioutput_cb, -90, SpringLayout.EAST, midioutput_ext_midi_l);
    else if(System.getProperty("os.name").startsWith("Windows"))
      osc_ext_midi_sl.putConstraint(SpringLayout.WEST, pserial.midioutput_cb, -70, SpringLayout.EAST, midioutput_ext_midi_l);
    osc_ext_midi_p.add(pserial.midioutput_cb);

    //Device Settings
    JPanel ds_p = new JPanel();
    SpringLayout ds_sl = new SpringLayout();
    ds_p.setLayout(ds_sl);
    SoftBevelBorder ds_inborder = new SoftBevelBorder(SoftBevelBorder.LOWERED);
    TitledBorder ds_outborder = new TitledBorder(ds_inborder, "Device Settings", TitledBorder.LEFT, TitledBorder.ABOVE_TOP);
    ds_p.setPreferredSize(new Dimension(430, 380));
    ds_p.setBorder(ds_outborder);
    sl.putConstraint(SpringLayout.NORTH, ds_p, 180, SpringLayout.NORTH, c);
    sl.putConstraint(SpringLayout.WEST, ds_p, 10, SpringLayout.WEST, c);
    c.add(ds_p);

    JLabel device_l = new JLabel("Device :");
    ds_sl.putConstraint(SpringLayout.NORTH, device_l, 10, SpringLayout.NORTH, ds_p);
    ds_sl.putConstraint(SpringLayout.WEST, device_l, 22, SpringLayout.WEST, ds_p);
    ds_p.add(device_l);
    pserial.device_cb = new JComboBox(pserial.getDeviceList());
    pserial.device_cb.setActionCommand("DeviceChanged");
    pserial.device_cb.addActionListener(this);
    ds_sl.putConstraint(SpringLayout.NORTH, pserial.device_cb, -4, SpringLayout.NORTH, device_l);
    ds_sl.putConstraint(SpringLayout.WEST, pserial.device_cb, 10, SpringLayout.EAST, device_l);
    ds_p.add(pserial.device_cb);

    JLabel cable_l = new JLabel("Cable Orientation :");
    ds_sl.putConstraint(SpringLayout.NORTH, cable_l, 40, SpringLayout.NORTH, ds_p);
    ds_sl.putConstraint(SpringLayout.WEST, cable_l, 22, SpringLayout.WEST, ds_p);
    ds_p.add(cable_l);
    String[] cable_str = {"left", "right", "up", "down"};
    pserial.cable_cb = new JComboBox(cable_str);
    pserial.cable_cb.setActionCommand("CableChanged");
    pserial.cable_cb.addActionListener(this);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      ds_sl.putConstraint(SpringLayout.NORTH, pserial.cable_cb, -4, SpringLayout.NORTH, cable_l);
    else if(System.getProperty("os.name").startsWith("Windows"))
      ds_sl.putConstraint(SpringLayout.NORTH, pserial.cable_cb, -3, SpringLayout.NORTH, cable_l);
    ds_sl.putConstraint(SpringLayout.WEST, pserial.cable_cb, 10, SpringLayout.EAST, cable_l);
    ds_p.add(pserial.cable_cb);

    JLabel intensity_l = new JLabel("Intensity :");
    ds_sl.putConstraint(SpringLayout.NORTH, intensity_l, 72, SpringLayout.NORTH, ds_p);
    ds_sl.putConstraint(SpringLayout.WEST, intensity_l, 22, SpringLayout.WEST, ds_p);
    ds_p.add(intensity_l);
    SpinnerNumberModel intensity_m = new SpinnerNumberModel(15, 0, 15, 1);
    pserial.intensity_s = new JSpinner(intensity_m);
    JSpinner.NumberEditor intensity_edit = new JSpinner.NumberEditor(pserial.intensity_s);
    pserial.intensity_s.setEditor(intensity_edit);
    JFormattedTextField intensity_text = intensity_edit.getTextField();
    intensity_text.setEditable(false);
    pserial.intensity_s.addChangeListener(this);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      pserial.intensity_s.setPreferredSize(new Dimension(50, 22));
    if(System.getProperty("os.name").startsWith("Windows"))
      pserial.intensity_s.setPreferredSize(new Dimension(50, 24));
    ds_sl.putConstraint(SpringLayout.NORTH, pserial.intensity_s, -2, SpringLayout.NORTH, intensity_l);
    ds_sl.putConstraint(SpringLayout.WEST, pserial.intensity_s, 10, SpringLayout.EAST, intensity_l);
    ds_p.add(pserial.intensity_s);

    pserial.led_clear_b = new JButton("LED Clear");
    pserial.led_clear_b.addActionListener(this);
    ds_sl.putConstraint(SpringLayout.NORTH, pserial.led_clear_b, -2, SpringLayout.NORTH, pserial.intensity_s);
    ds_sl.putConstraint(SpringLayout.WEST, pserial.led_clear_b, 30, SpringLayout.EAST, pserial.intensity_s);
    ds_p.add(pserial.led_clear_b);

    pserial.led_test_b = new JButton("LED Test On");
    pserial.led_test_b.addActionListener(this);
    ds_sl.putConstraint(SpringLayout.NORTH, pserial.led_test_b, 0, SpringLayout.NORTH, pserial.led_clear_b);
    ds_sl.putConstraint(SpringLayout.WEST, pserial.led_test_b, 10, SpringLayout.EAST, pserial.led_clear_b);
    ds_p.add(pserial.led_test_b);

    JPanel dsps_p = new JPanel();
    SpringLayout dsps_sl = new SpringLayout();
    dsps_p.setLayout(dsps_sl);
    SoftBevelBorder dsps_inborder = new SoftBevelBorder(SoftBevelBorder.LOWERED);
    TitledBorder dsps_outborder = new TitledBorder(dsps_inborder, "Device-Specific Protocol Settings", TitledBorder.LEFT, TitledBorder.ABOVE_TOP);
    dsps_p.setPreferredSize(new Dimension(395, 130));
    dsps_p.setBorder(dsps_outborder);
    ds_sl.putConstraint(SpringLayout.NORTH, dsps_p, 100, SpringLayout.NORTH, ds_p);
    ds_sl.putConstraint(SpringLayout.WEST, dsps_p, 10, SpringLayout.WEST, ds_p);
    ds_p.add(dsps_p);

    JLabel prefix_l = new JLabel("Address Pattern Prefix :");
    dsps_sl.putConstraint(SpringLayout.NORTH, prefix_l, 10, SpringLayout.NORTH, dsps_p);
    dsps_sl.putConstraint(SpringLayout.WEST, prefix_l, 65, SpringLayout.WEST, dsps_p);
    dsps_p.add(prefix_l);
    pserial.prefix_tf = new JTextField("/test", 5);
    pserial.prefix_tf.addActionListener(this);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      dsps_sl.putConstraint(SpringLayout.NORTH, pserial.prefix_tf, -4, SpringLayout.NORTH, prefix_l);
    if(System.getProperty("os.name").startsWith("Windows"))
      dsps_sl.putConstraint(SpringLayout.NORTH, pserial.prefix_tf, 0, SpringLayout.NORTH, prefix_l);
    dsps_sl.putConstraint(SpringLayout.WEST, pserial.prefix_tf, 10, SpringLayout.EAST, prefix_l);
    dsps_p.add(pserial.prefix_tf);

    JLabel startcolumn_l = new JLabel("Starting Column :");
    dsps_sl.putConstraint(SpringLayout.NORTH, startcolumn_l, 40, SpringLayout.NORTH, dsps_p);
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      dsps_sl.putConstraint(SpringLayout.WEST, startcolumn_l, 102, SpringLayout.WEST, dsps_p);
    if(System.getProperty("os.name").startsWith("Windows"))
      dsps_sl.putConstraint(SpringLayout.WEST, startcolumn_l, 104, SpringLayout.WEST, dsps_p);
    dsps_p.add(startcolumn_l);
    SpinnerNumberModel startcolumn_m = new SpinnerNumberModel(0, 0, null, 1);
    pserial.startcolumn_s = new JSpinner(startcolumn_m);
    JSpinner.NumberEditor startcolumn_edit = new JSpinner.NumberEditor(pserial.startcolumn_s);
    pserial.startcolumn_s.setEditor(startcolumn_edit);
    JFormattedTextField startcolumn_text = startcolumn_edit.getTextField();
    startcolumn_text.setEditable(false);
    pserial.startcolumn_s.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0)))
            pserial.setCurrentStartingColumn(0, (Integer)pserial.startcolumn_s.getValue());
          else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1)))
            pserial.setCurrentStartingColumn(1, (Integer)pserial.startcolumn_s.getValue());
        }
      });
    pserial.startcolumn_s.setPreferredSize(new Dimension(50, 22));
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      dsps_sl.putConstraint(SpringLayout.NORTH, pserial.startcolumn_s, -4, SpringLayout.NORTH, startcolumn_l);
    if(System.getProperty("os.name").startsWith("Windows"))
      dsps_sl.putConstraint(SpringLayout.NORTH, pserial.startcolumn_s, 0, SpringLayout.NORTH, startcolumn_l);
    dsps_sl.putConstraint(SpringLayout.WEST, pserial.startcolumn_s, 10, SpringLayout.EAST, startcolumn_l);
    dsps_p.add(pserial.startcolumn_s);

    JLabel startrow_l = new JLabel("Starting Row :");
    if(System.getProperty("os.name").startsWith("Mac OS X")) {
      dsps_sl.putConstraint(SpringLayout.NORTH, startrow_l, 70, SpringLayout.NORTH, dsps_p);
      dsps_sl.putConstraint(SpringLayout.WEST, startrow_l, 125, SpringLayout.WEST, dsps_p);
    }
    if(System.getProperty("os.name").startsWith("Windows")) {
      dsps_sl.putConstraint(SpringLayout.NORTH, startrow_l, 72, SpringLayout.NORTH, dsps_p);
      dsps_sl.putConstraint(SpringLayout.WEST, startrow_l, 122, SpringLayout.WEST, dsps_p);
    }
    dsps_p.add(startrow_l);
    SpinnerNumberModel startrow_m = new SpinnerNumberModel(0, 0, null, 1);
    pserial.startrow_s = new JSpinner(startrow_m);
    JSpinner.NumberEditor startrow_edit = new JSpinner.NumberEditor(pserial.startrow_s);
    pserial.startrow_s.setEditor(startrow_edit);
    JFormattedTextField startrow_text = startrow_edit.getTextField();
    startrow_text.setEditable(false);
    pserial.startrow_s.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0)))
            pserial.setCurrentStartingRow(0, (Integer)pserial.startrow_s.getValue());
          else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1)))
            pserial.setCurrentStartingRow(1, (Integer)pserial.startrow_s.getValue());
        }
      });
    pserial.startrow_s.setPreferredSize(new Dimension(50, 22));
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      dsps_sl.putConstraint(SpringLayout.NORTH, pserial.startrow_s, -4, SpringLayout.NORTH, startrow_l);
    if(System.getProperty("os.name").startsWith("Windows"))
      dsps_sl.putConstraint(SpringLayout.NORTH, pserial.startrow_s, 0, SpringLayout.NORTH, startrow_l);
    dsps_sl.putConstraint(SpringLayout.WEST, pserial.startrow_s, 10, SpringLayout.EAST, startrow_l);
    dsps_p.add(pserial.startrow_s);

    JTabbedPane ais_tab = new JTabbedPane();
    if(System.getProperty("os.name").startsWith("Mac OS X"))
      ais_tab.setPreferredSize(new Dimension(395, 115));
    else if(System.getProperty("os.name").startsWith("Windows"))
      ais_tab.setPreferredSize(new Dimension(395, 105));
    ds_sl.putConstraint(SpringLayout.NORTH, ais_tab, 240, SpringLayout.NORTH, ds_p);
    ds_sl.putConstraint(SpringLayout.WEST, ais_tab, 10, SpringLayout.WEST, ds_p);
    ds_p.add(ais_tab);

    JPanel aie_p = new JPanel();
    SpringLayout aie_sl = new SpringLayout();
    aie_p.setLayout(aie_sl);
    aie_p.setPreferredSize(new Dimension(395, 40));
    aie_sl.putConstraint(SpringLayout.NORTH, aie_p, 0, SpringLayout.NORTH, ais_tab);
    aie_sl.putConstraint(SpringLayout.WEST, aie_p, 0, SpringLayout.WEST, ais_tab);
    for(int i = 0; i < 7; i++) {
      pserial.adc_ck[i] = new JCheckBox(" adc " + i);
      pserial.adc_ck[i].addActionListener(this);
      if(i < 4) {
        aie_sl.putConstraint(SpringLayout.NORTH, pserial.adc_ck[i], 10, SpringLayout.NORTH, aie_p);
        aie_sl.putConstraint(SpringLayout.WEST, pserial.adc_ck[i], 10 + (90 * i), SpringLayout.WEST, aie_p);
      }
      else {
        aie_sl.putConstraint(SpringLayout.NORTH, pserial.adc_ck[i], 40, SpringLayout.NORTH, aie_p);
        aie_sl.putConstraint(SpringLayout.WEST, pserial.adc_ck[i], 10 + (90 * (i - 4)), SpringLayout.WEST, aie_p);
      }
      aie_p.add(pserial.adc_ck[i]);
    }
    ais_tab.addTab("Analog In. Enable", aie_p);

    JPanel ait_p = new JPanel();
    SpringLayout ait_sl = new SpringLayout();
    ait_p.setLayout(ait_sl);
    ait_p.setPreferredSize(new Dimension(395, 80));
    ait_sl.putConstraint(SpringLayout.NORTH, ait_p, 0, SpringLayout.NORTH, ais_tab);
    ait_sl.putConstraint(SpringLayout.WEST, ait_p, 0, SpringLayout.WEST, ais_tab);
    for(int i = 0; i < 7; i++) {
      String[] type_name = {"I/F", "C/F", "M/A"};
      pserial.adc_cmb0[i] = new JComboBox(type_name);
      pserial.adc_cmb0[i].setActionCommand("InputType" + i);
      pserial.adc_cmb0[i].setEnabled(false);
      pserial.adc_cmb0[i].addActionListener(this);
      if(i < 4) {
        ait_sl.putConstraint(SpringLayout.NORTH, pserial.adc_cmb0[i], 10, SpringLayout.NORTH, ait_p);
        ait_sl.putConstraint(SpringLayout.WEST, pserial.adc_cmb0[i], 10 + (90 * i), SpringLayout.WEST, ait_p);
      }
      else {
        ait_sl.putConstraint(SpringLayout.NORTH, pserial.adc_cmb0[i], 40, SpringLayout.NORTH, ait_p);
        ait_sl.putConstraint(SpringLayout.WEST, pserial.adc_cmb0[i], 10 + (90 * (i - 4)), SpringLayout.WEST, ait_p);
      }
      ait_p.add(pserial.adc_cmb0[i]);
    }
    ais_tab.addTab("Input Type", ait_p);

    JPanel aic_p = new JPanel();
    SpringLayout aic_sl = new SpringLayout();
    aic_p.setLayout(aic_sl);
    aic_p.setPreferredSize(new Dimension(395, 80));
    aic_sl.putConstraint(SpringLayout.NORTH, aic_p, 0, SpringLayout.NORTH, ais_tab);
    aic_sl.putConstraint(SpringLayout.WEST, aic_p, 0, SpringLayout.WEST, ais_tab);
    for(int i = 0; i < 7; i++) {
      String[] curve_name = {"C.1", "C.2", "C.3", "C.4", "C.5"};
      pserial.adc_cmb1[i] = new JComboBox(curve_name);
      pserial.adc_cmb1[i].setEnabled(false);
      pserial.adc_cmb1[i].setSelectedIndex(2);
      pserial.adc_cmb1[i].addActionListener(this);
      if(i < 4) {
        aic_sl.putConstraint(SpringLayout.NORTH, pserial.adc_cmb1[i], 10, SpringLayout.NORTH, aic_p);
        aic_sl.putConstraint(SpringLayout.WEST, pserial.adc_cmb1[i], 10 + (90 * i), SpringLayout.WEST, aic_p);
      }
      else {
        aic_sl.putConstraint(SpringLayout.NORTH, pserial.adc_cmb1[i], 40, SpringLayout.NORTH, aic_p);
        aic_sl.putConstraint(SpringLayout.WEST, pserial.adc_cmb1[i], 10 + (90 * (i - 4)), SpringLayout.WEST, aic_p);
      }
      aic_p.add(pserial.adc_cmb1[i]);
    }
    ais_tab.addTab("Fader Curve", aic_p);

    //Firmware Update
    JPanel fu_p = new JPanel();
    SpringLayout fu_sl = new SpringLayout();
    fu_p.setLayout(fu_sl);
    SoftBevelBorder fu_inborder = new SoftBevelBorder(SoftBevelBorder.LOWERED);
    TitledBorder fu_outborder = new TitledBorder(fu_inborder, "Firmware Update", TitledBorder.LEFT, TitledBorder.ABOVE_TOP);
    fu_p.setPreferredSize(new Dimension(430, 95));
    fu_p.setBorder(fu_outborder);
    sl.putConstraint(SpringLayout.NORTH, fu_p, 570, SpringLayout.NORTH, c);
    sl.putConstraint(SpringLayout.WEST, fu_p, 10, SpringLayout.WEST, c);
    c.add(fu_p);

    JLabel hex_l = new JLabel("HEX :");
    fu_sl.putConstraint(SpringLayout.NORTH, hex_l, 10, SpringLayout.NORTH, fu_p);
    fu_sl.putConstraint(SpringLayout.WEST, hex_l, 38, SpringLayout.WEST, fu_p);
    fu_p.add(hex_l);
    pserial.hex_tf = new JTextField("", 10);
    fu_sl.putConstraint(SpringLayout.NORTH, pserial.hex_tf, -4, SpringLayout.NORTH, hex_l);
    fu_sl.putConstraint(SpringLayout.WEST, pserial.hex_tf, 10, SpringLayout.EAST, hex_l);
    fu_p.add(pserial.hex_tf);
    pserial.hex_b = new JButton("Select");
    pserial.hex_b.addActionListener(this);
    fu_sl.putConstraint(SpringLayout.NORTH, pserial.hex_b, -2, SpringLayout.NORTH, pserial.hex_tf);
    fu_sl.putConstraint(SpringLayout.WEST, pserial.hex_b, 10, SpringLayout.EAST, pserial.hex_tf);
    fu_p.add(pserial.hex_b);
    pserial.update_b = new JButton("Update");
    pserial.update_b.addActionListener(this);
    fu_sl.putConstraint(SpringLayout.NORTH, pserial.update_b, 0, SpringLayout.NORTH, pserial.hex_b);
    fu_sl.putConstraint(SpringLayout.WEST, pserial.update_b, 8, SpringLayout.EAST, pserial.hex_b);
    fu_p.add(pserial.update_b);
    pserial.update_b.setEnabled(false);
    pserial.update_pb = new JProgressBar(0, 100);
    fu_sl.putConstraint(SpringLayout.NORTH, pserial.update_pb, 32, SpringLayout.NORTH, fu_p);
    fu_sl.putConstraint(SpringLayout.WEST, pserial.update_pb, 40, SpringLayout.WEST, fu_p);
    fu_p.add(pserial.update_pb);
    pserial.update_pb.setPreferredSize(new Dimension(350, 30));
    timer = new Timer(1, this);
  }

  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if(cmd == null)
      cmd = "timer";
    else if(cmd.equals(pserial.prefix_tf.getText()))
      cmd = "Prefix";
    else if(cmd.equals(pserial.hostaddress_tf.getText()))
      cmd = "HostAddress";
    else if(cmd.equals(pserial.listenport_tf.getText()))
      cmd = "ListenPort";
    else if(cmd.equals(pserial.hostport_tf.getText()))
      cmd = "HostPort";

    if(cmd.equals("DeviceChanged")) {
      if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0)))
        pserial.changeDeviceSettings(0);
      else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1)))
        pserial.changeDeviceSettings(1);
    }
    else if(cmd.equals("ProtocolChanged")) {
      if(((String)pserial.protocol_cb.getSelectedItem()).equals("Open Sound Control")) {
        psd_cl.show(psd_p, "osc");
        if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0)))
          pserial.setCurrentProtocol(0, "Open Sound Control");
        else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1)))
          pserial.setCurrentProtocol(1, "Open Sound Control");
      }
      else if(((String)pserial.protocol_cb.getSelectedItem()).equals("MIDI")) {
        psd_cl.show(psd_p, "midi");
        if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0)))
          pserial.setCurrentProtocol(0, "MIDI");
        else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1)))
          pserial.setCurrentProtocol(1, "MIDI");
      }
      else if(((String)pserial.protocol_cb.getSelectedItem()).equals("OSC/MIDI(ext.)")) {
        psd_cl.show(psd_p, "osc-ext-midi");
        if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0)))
          pserial.setCurrentProtocol(0, "OSC/MIDI(ext.)");
        else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1)))
          pserial.setCurrentProtocol(1, "OSC/MIDI(ext.)");
      }
      else if(((String)pserial.protocol_cb.getSelectedItem()).equals("OSC(LEDs)/MIDI(Pads)")) {
        psd_cl.show(psd_p, "midi");
        if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0)))
          pserial.setCurrentProtocol(0, "OSC(LEDs)/MIDI(Pads)");
        else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1)))
          pserial.setCurrentProtocol(1, "OSC(LEDs)/MIDI(Pads)");
      }
    }
    else if(cmd.equals("MidiInChanged")) {
      if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0))) {
        pserial.setCurrentMidiIn(0, pserial.midiinput_cb.getSelectedIndex());
        pserial.openMIDIPort(0);
      }
      else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1))) {
        pserial.setCurrentMidiIn(1, pserial.midiinput_cb.getSelectedIndex());
        pserial.openMIDIPort(1);
      }
    }
    else if(cmd.equals("MidiOutChanged")) {
      if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0))) {
        pserial.setCurrentMidiOut(0, pserial.midioutput_cb.getSelectedIndex());
        pserial.openMIDIPort(0);
      }
      else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1))) {
        pserial.setCurrentMidiOut(1, pserial.midioutput_cb.getSelectedIndex());
        pserial.openMIDIPort(1);
      }
    }
    else if(cmd.equals("CableChanged")) {
      if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0)))
        pserial.setCurrentCable(0, (String)pserial.cable_cb.getSelectedItem());
      else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1)))
        pserial.setCurrentCable(1, (String)pserial.cable_cb.getSelectedItem());
    }
    else if(cmd.equals("LED Clear")) {
      int idx = pserial.device_cb.getSelectedIndex();
      for(int i = 0; i < 8; i++) {
        String str =new String("lr " + i + " " + 0 + (char)0x0D);
        if(pserial.checkPortState(idx))
          pserial.sendDataToSerial(idx, str);
      }
    }
    else if(cmd.equals("LED Test On")) {
      //sy int idx = pserial.device_cb.getSelectedIndex();
      String str =new String("test 1" + (char)0x0D);

      if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0))) {
        if(pserial.checkPortState(0))
          pserial.sendDataToSerial(0, str);
      }
      else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1))) {
        if(pserial.checkPortState(1))
          pserial.sendDataToSerial(1, str);
      }
/*
      if(pserial.checkPortState(idx))
        pserial.sendDataToSerial(idx, str);
*/
      pserial.led_test_b.setText("LED Test Off");
    }
    else if(cmd.equals("LED Test Off")) {
      //sy int idx = pserial.device_cb.getSelectedIndex();
      String str =new String("test 0" + (char)0x0D);

      if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0))) {
        if(pserial.checkPortState(0))
          pserial.sendDataToSerial(0, str);
      }
      else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1))) {
        if(pserial.checkPortState(1))
          pserial.sendDataToSerial(1, str);
      }
      pserial.led_test_b.setText("LED Test On");
    }
    else if(cmd.equals("Detail..."))
      mdf.setVisible(true);
    else if(cmd.equals("Select")) {
      JFileChooser fc = new JFileChooser();
      int selected = fc.showOpenDialog(this);
      if (selected == JFileChooser.APPROVE_OPTION) {
        hex_f = fc.getSelectedFile();
        pserial.hex_tf.setText(hex_f.getName());
        pserial.update_b.setEnabled(true);
      }
    }
    else if(cmd.equals("Update")) {
      try
      {
        hex_fr = new FileReader(hex_f);
        size = 0;
        count = 0;
        bar = 0;
        while(hex_fr.read() != -1)
          size++;
        timer.start();
        hex_fr = new FileReader(hex_f);
      }
      catch(IOException ioe){}
    }
    else if(cmd.equals("Prefix"))
      pserial.initOSCListener();
    else if(cmd.equals("HostAddress") || cmd.equals("HostPort")) {
      pserial.setOSCHostInfo();
    }
    else if(cmd.equals(" adc 0") || cmd.equals(" adc 1") || cmd.equals(" adc 2") || cmd.equals(" adc 3") ||
            cmd.equals(" adc 4") || cmd.equals(" adc 5") || cmd.equals(" adc 6")) {
      int adc_id = Integer.parseInt(cmd.substring(5, 6));
      boolean b = pserial.adc_ck[adc_id].isSelected();
      String str;
      if(b) {
        str =new String("ae " + adc_id + " " + 1 + (char)0x0D);
        pserial.adc_cmb0[adc_id].setEnabled(true);
        if(pserial.adc_cmb0[adc_id].getSelectedIndex() < 2)
          pserial.adc_cmb1[adc_id].setEnabled(true);
      }
      else {
        str =new String("ae " + adc_id + " " + 0 + (char)0x0D);
        pserial.adc_cmb0[adc_id].setEnabled(false);
        pserial.adc_cmb1[adc_id].setEnabled(false);
      }
      
      if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0))) {
        pserial.setAdcEnable(0, adc_id, b);
        pserial.sendDataToSerial(0, str);
      }
      else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1))) {
        pserial.setAdcEnable(1, adc_id, b);
        pserial.sendDataToSerial(1, str);
      }
    }
    else if(cmd.equals("InputType0") || cmd.equals("InputType1") || cmd.equals("InputType2") || cmd.equals("InputType3") ||
            cmd.equals("InputType4") || cmd.equals("InputType5") || cmd.equals("InputType6")) {
      int adc_id = Integer.parseInt(cmd.substring(9, 10));
      int idx = pserial.adc_cmb0[adc_id].getSelectedIndex();
      if(idx < 2) {
        pserial.adc_cmb1[adc_id].setEnabled(true);
      }
      else {
        pserial.adc_cmb1[adc_id].setEnabled(false);
      }
    }
    else if(cmd.equals("timer")) {
      try {
        if((ch = hex_fr.read()) != -1) {
          bar = (int)(((double)count / (double)size) * 100);
          pserial.update_pb.setValue(bar);
          count++;
          pserial.sendDataToSerial(0, ch);
        }
        if(ch == -1 || ch == 59) {
          pserial.update_pb.setValue(0);
          if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0)))
            pserial.closeSerialPort(0);
          else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1)))
            pserial.closeSerialPort(1);
          hex_fr.close();
          timer.stop();
        }
      } catch(IOException ioe) {}
    }
  }

  public void stateChanged(ChangeEvent e) {
    if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(0))) {
      if((Integer)pserial.intensity_s.getValue() != pserial.getCurrentIntensity(0)) {
        pserial.setCurrentIntensity(0, (Integer)pserial.intensity_s.getValue());
        String str =new String("i " + pserial.getCurrentIntensity(0) + (char)0x0D);
        if(pserial.checkPortState(0))
          pserial.sendDataToSerial(0, str);
      }
      //sy pserial.starting_column[0] = (Integer)pserial.startcolumn_s.getValue();
      //sy pserial.starting_row[0] = (Integer)pserial.startrow_s.getValue();
    }
    else if(((String)pserial.device_cb.getSelectedItem()).equals(pserial.getCurrentDevice(1))) {
      if((Integer)pserial.intensity_s.getValue() != pserial.getCurrentIntensity(1)) {
        pserial.setCurrentIntensity(1, (Integer)pserial.intensity_s.getValue());
        String str =new String("i " + pserial.getCurrentIntensity(1) + (char)0x0D);
        if(pserial.checkPortState(1))
          pserial.sendDataToSerial(1, str);
      }
      //sy pserial.starting_column[1] = (Integer)pserial.startcolumn_s.getValue();
      //sy pserial.starting_row[1] = (Integer)pserial.startrow_s.getValue();
    }
  }

  public class MidiDetailFrame extends JFrame implements ActionListener {
    File save_f, load_f;
    JPanel mididetail_p;
    JButton save, load;
    MidiPadConfPanel[][] mpcp = new MidiPadConfPanel[16][8];

    MidiDetailFrame() {
      super("MIDI Detail Setting...");
    }
    public void init() {
      SpringLayout sl = new SpringLayout();
      mididetail_p = new JPanel();
      mididetail_p.setPreferredSize(new Dimension(1800, 570));
      mididetail_p.setLayout(sl);
      //sy c.setLayout(sl);

      prev_index = 0;
      para_change_flag = false;
      String[] type_name = {"Channel", "Velocity (Note On)", "Velocity (Note Off)"};
      pserial.midiparameter_cb = new JComboBox(type_name);
      pserial.midiparameter_cb.setActionCommand("TypeChanged");
      pserial.midiparameter_cb.addActionListener(this);
      sl.putConstraint(SpringLayout.WEST, pserial.midiparameter_cb, 10, SpringLayout.WEST, mididetail_p);
      sl.putConstraint(SpringLayout.NORTH, pserial.midiparameter_cb, 20, SpringLayout.NORTH, mididetail_p);
      mididetail_p.add(pserial.midiparameter_cb);

      save = new JButton("Save As...");
      save.addActionListener(this);
      sl.putConstraint(SpringLayout.WEST, save, 10, SpringLayout.EAST, pserial.midiparameter_cb);
      sl.putConstraint(SpringLayout.NORTH, save, 0, SpringLayout.NORTH, pserial.midiparameter_cb);
      mididetail_p.add(save);

      load = new JButton("Load...");
      load.addActionListener(this);
      sl.putConstraint(SpringLayout.WEST, load, 10, SpringLayout.EAST, save);
      sl.putConstraint(SpringLayout.NORTH, load, 0, SpringLayout.NORTH, save);
      mididetail_p.add(load);

      for(int j = 0; j < mpcp[0].length; j++) {
        for(int i = 0; i < mpcp.length; i++) {
          mpcp[i][j] = new MidiPadConfPanel(i, j);
          sl.putConstraint(SpringLayout.WEST, mpcp[i][j], (110 * i) + 10, SpringLayout.WEST, mididetail_p);
          sl.putConstraint(SpringLayout.NORTH, mpcp[i][j], (60 * j) + 60, SpringLayout.NORTH, mididetail_p);
          mididetail_p.add(mpcp[i][j]);
          pserial.setMidiNoteChannel(i, j, 0);
          pserial.setMidiNoteOnVelocity(i, j, 127);
          pserial.setMidiNoteOffVelocity(i, j, 0);
        }
      }
      Container c = getContentPane();
      JScrollPane sp = new JScrollPane(mididetail_p);
      c.add(sp);
    }

    public void setHalfVisible() {
      for(int j = 0; j < mpcp[0].length; j++)
        for(int i = 0; i < mpcp.length; i++)
          if(i > 7)
            mpcp[i][j].setVisible(false);
    }

    public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();

      if(cmd.equals("TypeChanged")) {
        para_change_flag = true;
        String index_name = (String)pserial.midiparameter_cb.getSelectedItem();
        int index = pserial.midiparameter_cb.getSelectedIndex();

        if(index != prev_index)
          for(int j = 0; j < mpcp[0].length; j++)
            for(int i = 0; i < mpcp.length; i++)
              switch(prev_index) {
              case 0:
                pserial.setMidiNoteChannel(i, j, (Integer)mpcp[i][j].value.getValue() - 1);
                break;
              case 1:
                pserial.setMidiNoteOnVelocity(i, j, (Integer)mpcp[i][j].value.getValue());
                break;
              case 2:
                pserial.setMidiNoteOffVelocity(i, j, (Integer)mpcp[i][j].value.getValue());
                break;
              }

        for(int j = 0; j < mpcp[0].length; j++) {
          for(int i = 0; i < mpcp.length; i++) {
            switch(index) {
            case 0:
              mpcp[i][j].setSliderRange(1, 16);
              mpcp[i][j].value.setValue(pserial.getMidiNoteChannel(i, j));
              break;
            case 1:
              mpcp[i][j].setSliderRange(0, 127);
              mpcp[i][j].value.setValue(pserial.getMidiNoteOnVelocity(i, j));
              break;
            case 2:
              mpcp[i][j].setSliderRange(0, 127);
              mpcp[i][j].value.setValue(pserial.getMidiNoteOffVelocity(i, j));
              break;
            }
          }
        }
        prev_index = index;
        para_change_flag = false;
      }
      else if(cmd.equals("Save As...")) {
        JFileChooser fc = new JFileChooser();
        int selected = fc.showSaveDialog(this);
        if (selected == JFileChooser.APPROVE_OPTION) {
          save_f = fc.getSelectedFile();
        }
        try {
          FileWriter fw = new FileWriter(save_f);
          BufferedWriter bw = new BufferedWriter(fw);
          for(int k = 0; k < 3; k++) {
            switch(k) {
            case 0:
              bw.write("//MIDI Channel" + System.getProperty("line.separator"));
              for(int j = 0; j < mpcp[0].length; j++) {
                String line = "";
                for(int i = 0; i < mpcp.length - 1; i++) {
                  line += (Integer.toString(pserial.getMidiNoteChannel(i, j)) + " ");
                }
                line += (Integer.toString(pserial.getMidiNoteChannel(mpcp.length - 1, j)));
                line += System.getProperty("line.separator");
                bw.write(line);
              }
              break;
            case 1:
              bw.write("//Note On Velocity" + System.getProperty("line.separator"));
              for(int j = 0; j < mpcp[0].length; j++) {
                String line = "";
                for(int i = 0; i < mpcp.length - 1; i++) {
                  line += (Integer.toString(pserial.getMidiNoteOnVelocity(i, j)) + " ");
                }
                line += (Integer.toString(pserial.getMidiNoteOnVelocity(mpcp.length - 1, j)));
                line += System.getProperty("line.separator");
                bw.write(line);
              }
              break;
            case 2:
              bw.write("//Note Off Velocity" + System.getProperty("line.separator"));
              for(int j = 0; j < mpcp[0].length; j++) {
                String line = "";
                for(int i = 0; i < mpcp.length - 1; i++) {
                  line += (Integer.toString(pserial.getMidiNoteOffVelocity(i, j)) + " ");
                }
                line += (Integer.toString(pserial.getMidiNoteOffVelocity(mpcp.length - 1, j)));
                line += System.getProperty("line.separator");
                bw.write(line);
              }
              break;
            }
            if(k != 3)
              bw.write(System.getProperty("line.separator"));
          }
          bw.flush();
          bw.close();
        }
        catch(IOException ioe){}
      }
      else if(cmd.equals("Load...")) {
        JFileChooser fc = new JFileChooser();
        int selected = fc.showOpenDialog(this);
        if(selected == JFileChooser.APPROVE_OPTION) {
          load_f = fc.getSelectedFile();
        }
        try {
          FileReader fr = new FileReader(load_f);
          BufferedReader br = new BufferedReader(fr);
          String line = "";
          int x = 0, y = 0, index = -1;
          while((line = br.readLine()) != null) {
            if(line.indexOf("//") != -1 || line.indexOf(System.getProperty("line.separator")) == 0) {
              y = 0;
              index++;
            }
            else {
              java.util.StringTokenizer st = new java.util.StringTokenizer(line);
              x = 0;
              while(st.hasMoreTokens()) {
                switch(index) {
                case 0:
                  pserial.setMidiNoteChannel(x, y, Integer.valueOf(st.nextToken()) - 1);
                  break;
                case 1:
                  pserial.setMidiNoteOnVelocity(x, y, Integer.valueOf(st.nextToken()));
                  break;
                case 2:
                  pserial.setMidiNoteOffVelocity(x, y, Integer.valueOf(st.nextToken()));
                  break;
                }
                x++;
              }
              y++;
            }
          }
          int k = pserial.midiparameter_cb.getSelectedIndex();
          for(int j = 0; j < mpcp[0].length; j++) {
            for(int i = 0; i < mpcp.length; i++) {
              switch(k) {
              case 0:
                mpcp[i][j].value.setValue(pserial.getMidiNoteChannel(i, j));
                break;
              case 1:
                mpcp[i][j].value.setValue(pserial.getMidiNoteOnVelocity(i, j));
                break;
              case 2:
                mpcp[i][j].value.setValue(pserial.getMidiNoteOffVelocity(i, j));
                break;
              }
            }
          }
        }
        catch(IOException ioe){}
      }
    }
  }

  private class MidiPadConfPanel extends JPanel implements ChangeListener
  {
    SpinnerNumberModel snm;
    JSpinner value;
    JSlider slider;
    int lattice_x, lattice_y;

    MidiPadConfPanel(int x, int y) {
      lattice_x = x;
      lattice_y = y;
      SpringLayout mpcp_sl = new SpringLayout();
      setLayout(mpcp_sl);
      setPreferredSize(new Dimension(110, 50));

      snm = new SpinnerNumberModel(1, 1, 16, 1);
      value = new JSpinner(snm);
      JSpinner.NumberEditor ne = new JSpinner.NumberEditor(value);
      value.setEditor(ne);
      JFormattedTextField ftf = ne.getTextField();
      ftf.setEditable(false);
      value.addChangeListener(this);
      value.setPreferredSize(new Dimension(100, 22));
      mpcp_sl.putConstraint(SpringLayout.WEST, value, 0, SpringLayout.WEST, this); 
      mpcp_sl.putConstraint(SpringLayout.NORTH, value, 0, SpringLayout.NORTH, this);
      add(value);

      slider = new JSlider();
      slider.setValue(1);
      slider.setMinimum(1);
      slider.setMaximum(16); 
      slider.addChangeListener(this);
      slider.setPreferredSize(new Dimension(116, 25));
      mpcp_sl.putConstraint(SpringLayout.WEST, slider, -6, SpringLayout.WEST, this);
      mpcp_sl.putConstraint(SpringLayout.NORTH, slider, 30, SpringLayout.NORTH, this);
      add(slider);
    }

    void setSliderRange(int min, int max) {
      snm.setMinimum(min);
      snm.setMaximum(max);
      slider.setMinimum(min);
      slider.setMaximum(max);
    }

    public void stateChanged(ChangeEvent e) {
      if(e.getSource() == value)
        slider.setValue((Integer)value.getValue());
      else if(e.getSource() == slider)
        value.setValue(slider.getValue());
      if(para_change_flag == false) {
        int index = pserial.midiparameter_cb.getSelectedIndex();
        switch(index) {
        case 0:
          pserial.setMidiNoteChannel(lattice_x, lattice_y, (Integer)value.getValue() - 1);
          break;
        case 1:
          pserial.setMidiNoteOnVelocity(lattice_x, lattice_y, (Integer)value.getValue());
          break;
        case 2:
          pserial.setMidiNoteOffVelocity(lattice_x, lattice_y, (Integer)value.getValue());
          break;
        }
      }
    }
  }
}