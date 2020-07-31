import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.JOptionPane;

import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

public class ARPLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public int nUnderLayerCount = 0;
	public String pLayerName = null;
	public ArrayList<BaseLayer> p_aUnderLayerARP = new ArrayList<BaseLayer>();
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	public ARPTable arpTable;

	HashMap<String, Object[]> cacheTable = new HashMap<String, Object[]>();
	HashMap<String, Object[]> proxyTable = new HashMap<String, Object[]>();
	HashMap<String, ArrayList<Object[]>> waitfor = new HashMap<String, ArrayList<Object[]>>();
	Receive_Thread receiveThread = null;
	Thread sendStartThread = null;
	Thread receiveStartThread = null;
	ApplicationLayer app;

	private byte[] ipAddress_port;
	Cache_Timeout thread = null;
	String portName;

	public final static int ARPHEADER = 28;

	// inner class for dealing with Mac address
	private class _ARP_MAC_ADDR {
		private byte[] addr = new byte[6];

		public _ARP_MAC_ADDR() {
			this.addr[0] = (byte) 0x00;
			this.addr[1] = (byte) 0x00;
			this.addr[2] = (byte) 0x00;
			this.addr[3] = (byte) 0x00;
			this.addr[4] = (byte) 0x00;
			this.addr[5] = (byte) 0x00;
		}
	}

	// inner class for dealing with Protocol address
	private class _ARP_PROTOCOL_ADDR {
		private byte[] addr = new byte[4];

		public _ARP_PROTOCOL_ADDR() {
			this.addr[0] = (byte) 0x00;
			this.addr[1] = (byte) 0x00;
			this.addr[2] = (byte) 0x00;
			this.addr[3] = (byte) 0x00;
		}
	}

	private class _ARP_HEADER {
		byte[] arp_hwType;
		byte[] arp_protoAddrType;
		byte[] arp_hwAddrLength;
		byte[] arp_protoAddrLength;
		byte[] arp_opcode;
		_ARP_MAC_ADDR _arp_mac_srcaddr;
		_ARP_PROTOCOL_ADDR _arp_protocol_srcaddr;
		_ARP_MAC_ADDR _arp_mac_dstaddr;
		_ARP_PROTOCOL_ADDR _arp_protocol_dstaddr; // first sending, it's empty.

		public _ARP_HEADER() {
			arp_hwType = new byte[2];
			arp_protoAddrType = new byte[2];
			arp_hwAddrLength = new byte[1];
			arp_protoAddrLength = new byte[1];
			arp_opcode = new byte[2];
			_arp_mac_srcaddr = new _ARP_MAC_ADDR();
			_arp_protocol_srcaddr = new _ARP_PROTOCOL_ADDR();
			_arp_mac_dstaddr = new _ARP_MAC_ADDR();
			_arp_protocol_dstaddr = new _ARP_PROTOCOL_ADDR();
		}
	}

	_ARP_HEADER m_sHeader = new _ARP_HEADER();

	public ARPLayer(String pName, ARPTable arpTable) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		if (pName.equals("ARP"))
			portName = "Port1";
		else
			portName = "Port2";

		if (thread == null) {
			thread = new Cache_Timeout(this.cacheTable, 30, 50);
			Thread obj = new Thread(thread);
			obj.start();
		}
		this.arpTable = arpTable;
	}

	public byte[] ObjToByte(_ARP_HEADER m_sHeader) {
		byte[] buf = new byte[ARPHEADER];

		buf[0] = m_sHeader.arp_hwType[0];
		buf[1] = m_sHeader.arp_hwType[1];
		buf[2] = m_sHeader.arp_protoAddrType[0];
		buf[3] = m_sHeader.arp_protoAddrType[1];
		buf[4] = m_sHeader.arp_hwAddrLength[0];
		buf[5] = m_sHeader.arp_protoAddrLength[0];
		buf[6] = m_sHeader.arp_opcode[0];
		buf[7] = m_sHeader.arp_opcode[1];
		for (int i = 0; i < 6; i++) {
			buf[i + 8] = m_sHeader._arp_mac_srcaddr.addr[i];
			buf[i + 18] = m_sHeader._arp_mac_dstaddr.addr[i];
		}
		for (int i = 0; i < 4; i++) {
			buf[i + 14] = m_sHeader._arp_protocol_srcaddr.addr[i];
			buf[i + 24] = m_sHeader._arp_protocol_dstaddr.addr[i];
		}

		return buf;
	}


	public boolean Send(byte[] arp_protocol_srcaddr, byte[] arp_protocol_dstaddr, byte[] mac_srcaddr,
			byte[] mac_dstaddr, byte[] arp_opcode) {
		
      Object[] value = new Object[4];

      String srcIpAddressToString = (arp_protocol_srcaddr[0] & 0xFF) + "." + (arp_protocol_srcaddr[1] & 0xFF) + "."
              + (arp_protocol_srcaddr[2] & 0xFF) + "." + (arp_protocol_srcaddr[3] & 0xFF);
      String dstIpAddressToString = (arp_protocol_dstaddr[0] & 0xFF) + "." + (arp_protocol_dstaddr[1] & 0xFF) + "."
            + (arp_protocol_dstaddr[2] & 0xFF) + "." + (arp_protocol_dstaddr[3] & 0xFF);


      String macAddress1 = String.format("%X:", mac_srcaddr[0]) + String.format("%X:", mac_srcaddr[1])
		+ String.format("%X:", mac_srcaddr[2]) + String.format("%X:", mac_srcaddr[3])
		+ String.format("%X:", mac_srcaddr[4]) + String.format("%X", mac_srcaddr[5]);
	
      String macAddress2 = String.format("%X:", mac_dstaddr[0]) + String.format("%X:", mac_dstaddr[1])
		+ String.format("%X:", mac_dstaddr[2]) + String.format("%X:", mac_dstaddr[3])
		+ String.format("%X:", mac_dstaddr[4]) + String.format("%X", mac_dstaddr[5]);
      
      
      if (arp_opcode[0] == (byte) 0x00 && arp_opcode[1] == (byte) 0x01) {

         if (cacheTable.containsKey(dstIpAddressToString))
            return true;

         value[0] = cacheTable.size(); // ARP-request Send ("Incomplete")
         value[1] = mac_dstaddr;
         value[2] = "Incomplete";
         value[3] = System.currentTimeMillis();

         cacheTable.put(dstIpAddressToString, value);
         arpTable.addEntry(dstIpAddressToString,value,portName);
         
         m_sHeader._arp_mac_dstaddr.addr = mac_dstaddr;

         m_sHeader._arp_protocol_srcaddr.addr = arp_protocol_srcaddr;
         m_sHeader._arp_protocol_dstaddr.addr = arp_protocol_dstaddr;
         m_sHeader._arp_mac_srcaddr.addr = mac_srcaddr;

      } else {
    	 m_sHeader._arp_mac_dstaddr.addr = mac_dstaddr;

         m_sHeader._arp_protocol_srcaddr.addr = arp_protocol_srcaddr;
         m_sHeader._arp_protocol_dstaddr.addr = arp_protocol_dstaddr;
         m_sHeader._arp_mac_srcaddr.addr = mac_srcaddr;

      }

      m_sHeader.arp_hwType[0] = (byte) 0x00; // Ethernet
      m_sHeader.arp_hwType[1] = (byte) 0x01;

      m_sHeader.arp_protoAddrType[0] = (byte) 0x08; // IP
      m_sHeader.arp_protoAddrType[1] = (byte) 0x00;

      m_sHeader.arp_hwAddrLength[0] = 6;
      m_sHeader.arp_protoAddrLength[0] = 4;

      m_sHeader.arp_opcode = arp_opcode;

      byte[] bytes = ObjToByte(m_sHeader);
     
      ((EthernetLayer)this.GetUnderLayer(0)).Send(bytes, bytes.length,null);
      
      return true;
	}

	public boolean Receive(byte[] input, byte[] mac_srcaddress) {
		receiveThread = new Receive_Thread(input, mac_srcaddress, ((EthernetLayer) GetUnderLayer(0)),this,arpTable);
		receiveStartThread = new Thread(receiveThread);
		receiveStartThread.start();
		return false;
	}

	public void updateCacheTable() {
		if (app.exist) {

			Set keyS = cacheTable.keySet();

			for (Iterator iterator = keyS.iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				Object[] value = (Object[]) cacheTable.get(key);

				if (value[2].equals("Incomplete")) {
					app.TotalArea
							.append("       " + key + "\t" + "??????????????\t incomplete" + portName + "\n");
				} else {
					byte[] maxAddr = (byte[]) value[1];
					String macAddress = String.format("%X:", maxAddr[0]) + String.format("%X:", maxAddr[1])
							+ String.format("%X:", maxAddr[2]) + String.format("%X:", maxAddr[3])
							+ String.format("%X:", maxAddr[4]) + String.format("%X", maxAddr[5]);

					app.TotalArea
							.append("       " + key + "\t" + macAddress + "\t complete" + portName + "\n");
				}
			}
		}
	}

	public boolean SendforARP(byte[] message, String port, byte[] arp_protocol_srcaddr, byte[] arp_protocol_dstaddr,
			byte[] mac_srcaddr, byte[] mac_dstaddr, byte[] arp_opcode) {

		String ipAddressToString = (arp_protocol_dstaddr[0] & 0xFF) + "." + (arp_protocol_dstaddr[1] & 0xFF) + "."
				+ (arp_protocol_dstaddr[2] & 0xFF) + "." + (arp_protocol_dstaddr[3] & 0xFF);      

		if (cacheTable.containsKey(ipAddressToString) && (cacheTable.get(ipAddressToString)[2].equals("Complete"))) {

			byte[] realAddress = (byte[]) (cacheTable.get(ipAddressToString)[1]);
			((EthernetLayer) this.GetUnderLayer(0)).Send(message, message.length, realAddress);
			
			return true;
		}
		Object[] value = new Object[2];
		value[0] = message;
		value[1] = port;

		if (waitfor.containsKey(ipAddressToString))
			waitfor.get(ipAddressToString).add(value);
		else {
			ArrayList<Object[]> objList = new ArrayList<Object[]>();
			objList.add(value);
			waitfor.put(ipAddressToString, objList);
		}
		
		Send(arp_protocol_srcaddr, arp_protocol_dstaddr, mac_srcaddr, mac_dstaddr, arp_opcode);

		return true;
	}

	/******************************************************************/

	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		// TODO Auto-generated method stub

		return null;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		// TODO Auto-generated method stub
		if (pUnderLayer == null) {
			return;
		}
		this.p_aUnderLayerARP.add(nUnderLayerCount++, pUnderLayer);
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		this.SetUnderLayer(pUULayer);
	}


	public void SetIPAddrSrcAddr(byte[] srcaddr) {
		this.ipAddress_port = srcaddr;
	}

	public void SetARPTable(ARPTable arpTable, ApplicationLayer app) {
		this.arpTable = arpTable;
		this.app = app;
	}

	@Override
	public BaseLayer GetUnderLayer(int nindex) {
		if (nindex < 0 || nindex > nUnderLayerCount || nUnderLayerCount < 0)
			return null;
		return p_aUnderLayerARP.get(nindex);
	}

	class Cache_Timeout implements Runnable {
		HashMap<String, Object[]> cacheTable;
		int incompleteTimeLimit;
		int completeTimeLimit;

		public Cache_Timeout(HashMap<String, Object[]> cacheTable, int incompleteTimeLimit, int completeTimeLimit) {
			this.cacheTable = cacheTable;
			this.incompleteTimeLimit = incompleteTimeLimit;
			this.completeTimeLimit = completeTimeLimit;
		}

		@Override
		public void run() {
			while (true) {
				if (arpTable != null) {
					Set keyS = this.cacheTable.keySet();
					ArrayList<String> deleteKey = new ArrayList<String>();
					for (Iterator iterator = keyS.iterator(); iterator.hasNext();) {
						String key = null;
						if ((key = (String) iterator.next()) != null) {
							Object[] value = (Object[]) this.cacheTable.get(key);
							if (value[2].equals("Incomplete")) {
								if ((System.currentTimeMillis() - (long) value[3]) / 60000 >= incompleteTimeLimit) {
									deleteKey.add(key);
								}
							} else {
								if ((System.currentTimeMillis() - (long) value[3]) / 60000 >= completeTimeLimit) {
									deleteKey.add(key);
								}
							}
						}
					}
					for (int i = 0; i < deleteKey.size(); i++)
						this.cacheTable.remove(deleteKey.get(i));
					if (deleteKey.size() != 0)
						arpTable.updateARPCacheTable();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		}
	}


	class Receive_Thread implements Runnable {
		byte[] input;
		byte[] mac_srcaddress;
		EthernetLayer ethernet;
		ARPLayer arp;
		ARPTable arpTableInThread;

		public Receive_Thread(byte[] input, byte[] mac_srcaddress,EthernetLayer ethernet,ARPLayer arp, ARPTable arpTableInThread) {
			this.input = input;
			this.mac_srcaddress=mac_srcaddress;
			this.ethernet =ethernet;
			this.arp = arp;
			this.arpTableInThread = arpTableInThread;
		}

		@Override
		public void run() {
			byte[] message = input;

			Object[] value = new Object[4];
			byte[] dstIP = new byte[4];
			byte[] dstMac = new byte[6];
			byte[] targetIP = new byte[4];

			System.arraycopy(message, 14, dstIP, 0, 4);
			System.arraycopy(message, 8, dstMac, 0, 6);
			System.arraycopy(message, 24, targetIP, 0, 4);

			String ipAddressToString = (dstIP[0] & 0xFF) + "." + (dstIP[1] & 0xFF) + "." + (dstIP[2] & 0xFF) + "."
					+ (dstIP[3] & 0xFF);
			String targetIpAddressToString = (targetIP[0] & 0xFF) + "." + (targetIP[1] & 0xFF) + "."
					+ (targetIP[2] & 0xFF) + "." + (targetIP[3] & 0xFF);

			if (message[6] == (byte) 0x00 && message[7] == (byte) 0x01) { // ARP-request Receive ("Complete")

				byte[] newOp = new byte[2];
				newOp[0] = (byte) 0x00;
				newOp[1] = (byte) 0x02;

				if (!cacheTable.containsKey(ipAddressToString)) {

					value[0] = cacheTable.size();
					value[1] = dstMac;
					value[2] = "Complete";
					value[3] = System.currentTimeMillis();

					cacheTable.put(ipAddressToString, value);
					arpTableInThread.addEntry(ipAddressToString, value, portName);
				} else {

					value[0] = cacheTable.get(ipAddressToString)[0];
					value[1] = dstMac; // mac address
					value[2] = cacheTable.get(ipAddressToString)[2];
					value[3] = System.currentTimeMillis();

					cacheTable.put(ipAddressToString, value);
				}
				if (proxyTable.containsKey(targetIpAddressToString))
					return;

				newOp = new byte[2];
				newOp[0] = (byte) 0x00;
				newOp[1] = (byte) 0x02;

				boolean checkPort = false;

				if (!proxyTable.containsKey(targetIpAddressToString)) {
					for (int i = 0; i < 4; i++) {
						if (message[i + 24] != ipAddress_port[i])
							return;
					}
					((ARPLayer)arp).Send(ipAddress_port, dstIP, mac_srcaddress, dstMac, newOp);
					return;
				}

			} else if (message[6] == (byte) 0x00 && message[7] == (byte) 0x02) { // ARP-reply Receive ("Incomplete" ->

				if (waitfor.containsKey(ipAddressToString)) {
					ArrayList<Object[]> list = waitfor.get(ipAddressToString);
					if (!list.isEmpty()) {
						Object[] toSendObj = (Object[]) list.get(0);

						((EthernetLayer) ethernet).Send((byte[]) toSendObj[0],
								((byte[]) toSendObj[0]).length, dstMac);
						if (list.size() == 1)
							waitfor.remove(ipAddressToString);
						else
							list.remove(0);
					}

				}

				if (cacheTable.containsKey(ipAddressToString)) {

					value[0] = cacheTable.get(ipAddressToString)[0];
					value[1] = dstMac;
					value[2] = "Complete";
					value[3] = System.currentTimeMillis();
					cacheTable.replace(ipAddressToString, value);

					arpTable.updateARPCacheTable();
				} else {
					value[0] = cacheTable.size();
					value[1] = dstMac;
					value[2] = "Complete";
					value[3] = System.currentTimeMillis();
					cacheTable.put(ipAddressToString, value);

					arpTable.addEntry(ipAddressToString, value, portName);
				}

			}
		}
	}
}