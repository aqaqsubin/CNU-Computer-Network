
import java.util.ArrayList;

public class EthernetLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public final static int HEARER_SIZE = 14;
	private static byte[] arp_mac_dstaddr = null;

	public byte[] chat_file_dstaddr;
	public byte[] ex_ethernetaddr = new byte[6];

	private class _ETHERNET_ADDR {
		private byte[] addr = new byte[6];

		public _ETHERNET_ADDR() {
			this.addr[0] = (byte) 0x00;
			this.addr[1] = (byte) 0x00;
			this.addr[2] = (byte) 0x00;
			this.addr[3] = (byte) 0x00;
			this.addr[4] = (byte) 0x00;
			this.addr[5] = (byte) 0x00;
		}
	}

	private class _ETHERNET_HEADER {
		_ETHERNET_ADDR enet_dstaddr;
		_ETHERNET_ADDR enet_srcaddr;
		byte[] enet_type;
		byte[] enet_data;

		public _ETHERNET_HEADER() {
			this.enet_dstaddr = new _ETHERNET_ADDR();
			this.enet_srcaddr = new _ETHERNET_ADDR();
			this.enet_type = new byte[2];
			this.enet_data = null;
		}
	}

	_ETHERNET_HEADER m_sHeader = new _ETHERNET_HEADER();

	public EthernetLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;

	}

	public void SetEnetSrcAddress(byte[] srcAddress) {
		// TODO Auto-generated method stub
		m_sHeader.enet_srcaddr.addr[0] = srcAddress[0];
		m_sHeader.enet_srcaddr.addr[1] = srcAddress[1];
		m_sHeader.enet_srcaddr.addr[2] = srcAddress[2];
		m_sHeader.enet_srcaddr.addr[3] = srcAddress[3];
		m_sHeader.enet_srcaddr.addr[4] = srcAddress[4];
		m_sHeader.enet_srcaddr.addr[5] = srcAddress[5];

	}
	public byte[] getEnetSrcAddress() {
		// TODO Auto-generated method stub
		byte[] returnByte = new byte[6];
		System.arraycopy(m_sHeader.enet_srcaddr.addr, 0, returnByte, 0, 6);
		return returnByte;
	}

	public void SetEnetDstAddress(byte[] dstAddress) {
		// TODO Auto-generated method stub
		m_sHeader.enet_dstaddr.addr[0] = dstAddress[0];
		m_sHeader.enet_dstaddr.addr[1] = dstAddress[1];
		m_sHeader.enet_dstaddr.addr[2] = dstAddress[2];
		m_sHeader.enet_dstaddr.addr[3] = dstAddress[3];
		m_sHeader.enet_dstaddr.addr[4] = dstAddress[4];
		m_sHeader.enet_dstaddr.addr[5] = dstAddress[5];

	}

	public static void SetMacAddrDstAddr(byte[] dstAddress) {
		arp_mac_dstaddr = dstAddress;
	}

	public byte[] ObjToByteDATA(_ETHERNET_HEADER Header, byte[] input, int length) {
		byte[] buf = new byte[length + HEARER_SIZE];

		buf[0] = Header.enet_dstaddr.addr[0];
		buf[1] = Header.enet_dstaddr.addr[1];
		buf[2] = Header.enet_dstaddr.addr[2];
		buf[3] = Header.enet_dstaddr.addr[3];
		buf[4] = Header.enet_dstaddr.addr[4];
		buf[5] = Header.enet_dstaddr.addr[5];
		buf[6] = Header.enet_srcaddr.addr[0];
		buf[7] = Header.enet_srcaddr.addr[1];
		buf[8] = Header.enet_srcaddr.addr[2];
		buf[9] = Header.enet_srcaddr.addr[3];
		buf[10] = Header.enet_srcaddr.addr[4];
		buf[11] = Header.enet_srcaddr.addr[5];
		buf[12] = Header.enet_type[0];
		buf[13] = Header.enet_type[1];

		for (int i = 0; i < length; i++) {
			buf[HEARER_SIZE + i] = input[i];
		}
		return buf;
	}

	public boolean Send(byte[] input, int length, byte[] identifier) {

		m_sHeader.enet_data = input;
		if (m_sHeader.enet_data.length > 1500)
			return false;
		// Get from IP Layer
		if (identifier!=null) {
			m_sHeader.enet_data = input;
			m_sHeader.enet_type[0] = (byte) 0x08;
			m_sHeader.enet_type[1] = (byte) 0x00;

			m_sHeader.enet_dstaddr.addr = identifier;

			byte[] frame = ObjToByteDATA(m_sHeader, input, length);

			((NILayer)GetUnderLayer()).Send(frame, length + HEARER_SIZE);

		} 
		// Get from ARP Layer
		else {

			System.out.println();
			m_sHeader.enet_data = input;
			m_sHeader.enet_type[0] = (byte) 0x08;
			m_sHeader.enet_type[1] = (byte) 0x06;

			if (input[6] == 0x00 && input[7] == 0x01) {
				System.out.println("ARP Broadcast");

				m_sHeader.enet_dstaddr.addr[0] = (byte) 0xff;
				m_sHeader.enet_dstaddr.addr[1] = (byte) 0xff;
				m_sHeader.enet_dstaddr.addr[2] = (byte) 0xff;
				m_sHeader.enet_dstaddr.addr[3] = (byte) 0xff;
				m_sHeader.enet_dstaddr.addr[4] = (byte) 0xff;
				m_sHeader.enet_dstaddr.addr[5] = (byte) 0xff;


			}
			if (input[6] == 0x00 && input[7] == 0x02) {
				
				System.out.println("ARP Reply");
				m_sHeader.enet_dstaddr.addr[0] = input[18];
				m_sHeader.enet_dstaddr.addr[1] = input[19];
				m_sHeader.enet_dstaddr.addr[2] = input[20];
				m_sHeader.enet_dstaddr.addr[3] = input[21];
				m_sHeader.enet_dstaddr.addr[4] = input[22];
				m_sHeader.enet_dstaddr.addr[5] = input[23];

				String macAddress1 = String.format("%X:", input[18]) + String.format("%X:", input[19])
						+ String.format("%X:", input[20]) + String.format("%X:", input[21])
						+ String.format("%X:", input[22]) + String.format("%X", input[23]);
				System.out.println("ARP Reply to : "+macAddress1);
				
			}

			byte[] frame = ObjToByteDATA(m_sHeader, input, length);
			((NILayer)GetUnderLayer()).Send(frame, length + HEARER_SIZE);
			
		}
		return true;
	}

	public byte[] RemoveCappHeader(byte[] input, int length) {

		byte[] rebuf = new byte[length - HEARER_SIZE];
		m_sHeader.enet_data = new byte[length - HEARER_SIZE];

		for (int i = 0; i < length - HEARER_SIZE; i++) {
			m_sHeader.enet_data[i] = input[HEARER_SIZE + i];
			rebuf[i] = input[HEARER_SIZE + i];
		}
		return rebuf;
	}

	public boolean Receive(byte[] input) {
		byte[] data;
		data = RemoveCappHeader(input, input.length);

		if (!srcme_Addr(input)) {
			if (bro_Addr(input) || dstme_Addr(input)) {
				
				System.out.println("packet");
				if (input[12] == 0x08 && input[13] == 0x00) {
					// Sending the IP Layer
					((IPLayer)this.GetUpperLayer(1)).Receive(data);
				}
				else {
					String srcIpAddressToString = (data[14] & 0xFF) + "." + (data[15] & 0xFF) + "."
			              + (data[16] & 0xFF) + "." + (data[17] & 0xFF);
					((ARPLayer)this.GetUpperLayer(0)).Receive(data, getEnetSrcAddress());
				}
				return true;
			}
		}
		return true;
	}

	public boolean dstme_Addr(byte[] add) {
		for (int i = 0; i < 6; i++) {
			if (add[i] != m_sHeader.enet_srcaddr.addr[i])
				return false;
		}
		return true;
	}

	public boolean srcme_Addr(byte[] add) {
		for (int i = 0; i < 6; i++) {
			if (add[i + 6] != m_sHeader.enet_srcaddr.addr[i])
				return false;
		}
		return true;
	}

	public boolean dst_you(byte[] add) {
		for (int i = 0; i < 6; i++) {
			if (add[i + 6] != ((IPLayer)GetUpperLayer(1)).chatDST_mac[i]) {
				return false;
			}
		}
		return true;
	}

	public boolean bro_Addr(byte[] add) {
		for (int i = 0; i < 6; i++) {
			if (add[i] != (byte) 0xff) {
				return false;
			}	
		}
		return true;
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		if (pUnderLayer == null)
			return;
		p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
		// nUpperLayerCount++;
	}

	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}

	@Override
	public BaseLayer GetUnderLayer(int nindex) {
		return null;
	}
}
