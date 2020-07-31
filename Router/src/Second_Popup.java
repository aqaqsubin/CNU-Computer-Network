import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.jnetpcap.PcapIf;

public class Second_Popup extends JFrame {
	JTextArea inputProtocol;
	JTextArea inputMac;
	JComboBox<String> selectHost;
	JLabel lbl_Device;
	JLabel lbl_protocol;
	JLabel lbl_mac;
	Container contentPane;
	String[] hostsName = {"Host B","Host C","Host D"};
	String host = hostsName[0];
	
	JComboBox<String> selectInterface;
	JLabel lbl_Interface;
	String[] interfaceName = { "Port1", "Port2" };
	String interface0 = interfaceName[0];
	int proxySize =0;

	public Second_Popup(HashMap<String, Object[] > proxyTable,JTextArea proxyArea) {

		setTitle("Proxy ARP Entry");
		setSize(450, 350);
		setLocation(1200, 300);
		getContentPane().setLayout(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		contentPane = new JPanel();
		((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);



		lbl_Interface = new JLabel("Interface");
		lbl_Interface.setBounds(50, 40, 90, 30);
		contentPane.add(lbl_Interface);

		selectInterface = new JComboBox<String>(interfaceName);
		selectInterface.setBounds(150, 40, 180, 30);
		selectInterface.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				interface0 = interfaceName[selectInterface.getSelectedIndex()];
			}
		});
		contentPane.add(selectInterface);
		

		lbl_protocol = new JLabel("IP �ּ�");
		lbl_protocol.setBounds(50, 90, 90, 30);
		inputProtocol = new JTextArea();
		inputProtocol.setBounds(150, 90, 180, 30);
		inputProtocol.setEnabled(true);

		contentPane.add(lbl_protocol);
		contentPane.add(inputProtocol);

		lbl_mac = new JLabel("Ethernet �ּ�");
		lbl_mac.setBounds(50, 140, 90, 30);
		inputMac = new JTextArea();
		inputMac.setBounds(150, 140, 180, 30);
		inputMac.setEnabled(true);

		contentPane.add(lbl_mac);
		contentPane.add(inputMac);




		JButton btnOk = new JButton("Ok");
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!inputProtocol.getText().equals("") && !inputMac.getText().equals("")) {
					String interface_Num = interface0;
					
						String hostName = host;
						StringTokenizer st = new StringTokenizer(inputProtocol.getText(), ".");

						byte[] ipAddress = new byte[4];
						for (int i = 0; i < 4; i++) {
							String ss = st.nextToken();
							int s = Integer.parseInt(ss);
							ipAddress[i] = (byte) (s & 0xFF);
						}

						st = new StringTokenizer(inputMac.getText(), ":");
						byte[] macAddress = new byte[6];
						for (int i = 0; i < 6; i++) {
							String ss = st.nextToken();
							int s = Integer.parseInt(ss, 16);
							macAddress[i] = (byte) (s & 0xFF);
						}
						Object[] value = new Object[3];
						value[0] = interface_Num;
						value[1] = macAddress;
						value[2] = ipAddress;

						proxyTable.put(inputProtocol.getText(), value);

					

					String printResult ="";

					for(Iterator iterator = proxyTable.keySet().iterator(); iterator.hasNext();) {
						String keyIP = (String)iterator.next();
						Object[] obj = proxyTable.get(keyIP);

						byte[] mac = (byte[])proxyTable.get(keyIP)[1];
						String ip_String =keyIP;
						String mac_String ="";


						for(int j=0;j<5;j++) mac_String = mac_String + String.format("%X:",mac[j]);
						mac_String = mac_String + String.format("%X",mac[5]);

						printResult = printResult+ip_String+"\t    "+mac_String+"\t    "+obj[0]+"\n";
					}
				
					proxySize = proxyTable.size();
					System.out.println(proxyTable.size()+"  "+printResult);
					proxyArea.setText(printResult);
					dispose();

				}

			}
		});

		btnOk.setBounds(130, 250, 80, 30);
		getContentPane().add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		btnCancel.setBounds(220, 250, 80, 30);
		getContentPane().add(btnCancel);
		setVisible(true);
	}
}
