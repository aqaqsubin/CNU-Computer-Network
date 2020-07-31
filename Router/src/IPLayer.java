import java.util.ArrayList;


public class IPLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public int nUnderLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUnderLayerIP = new ArrayList<BaseLayer>();
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public final static int IPHEADER = 20;

	byte[] chatDST_mac = new byte[6];
	byte[] arpDST_mac = new byte[6];
	byte[] chatDST_ip = new byte[4];
	byte[] arpDST_ip = new byte[4];

	public BaseLayer friendIPLayer;
	public RoutingTable routingTable;

	Receive_Thread receiveThread = null;
	Thread receiveStartThread = null;


	String portName;
	byte[] srcMacAddress;



	private class _IPLayer_HEADER {
		byte[] ip_versionLen;   // ip version -> IPv4 : 4
		byte[] ip_serviceType;   // type of service
		byte[] ip_packetLen;   // total packet length
		byte[] ip_datagramID;   // datagram id
		byte[] ip_offset;      // fragment offset
		byte[] ip_ttl;         // time to live in gateway hops
		byte[] ip_protocol;      // IP protocol
		byte[] ip_cksum;      // header checksum
		byte[] ip_srcaddr;      // IP address of source
		byte[] ip_dstaddr;      // IP address of destination
		byte[] ip_data;         // data

		public _IPLayer_HEADER(){
			this.ip_versionLen = new byte[1];
			this.ip_serviceType = new byte[1];
			this.ip_packetLen = new byte[2];
			this.ip_datagramID = new byte[2];
			this.ip_offset = new byte[2];
			this.ip_ttl = new byte[1];
			this.ip_protocol = new byte[1];
			this.ip_cksum = new byte[2];
			this.ip_srcaddr = new byte[4];
			this.ip_dstaddr= new byte[4];
			this.ip_data = null;      
		}
	}

	_IPLayer_HEADER m_sHeader = new _IPLayer_HEADER();

	public IPLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		m_sHeader = new _IPLayer_HEADER(); 
	}

	public void friendIPset( BaseLayer friendIPLayer ) {
		this.friendIPLayer = friendIPLayer;
	}

	public BaseLayer friendIPget() {
		return this.friendIPLayer;
	}
	public void setRouter(RoutingTable routingTable) {
		this.routingTable = routingTable;
	}
	public void setPort(byte[] srcMacAddress, String portName) {
		this.srcMacAddress = srcMacAddress;
		this.portName = portName;
	}


	public void SetIPSrcAddress(byte[] srcAddress) {
		// TODO Auto-generated method stub
		m_sHeader.ip_srcaddr[0]= srcAddress[0];
		m_sHeader.ip_srcaddr[1]= srcAddress[1];
		m_sHeader.ip_srcaddr[2]= srcAddress[2];
		m_sHeader.ip_srcaddr[3]= srcAddress[3];

	}

	public void SetIPDstAddress(byte[] dstAddress) {
		// TODO Auto-generated method stub
		m_sHeader.ip_dstaddr[0]= dstAddress[0];
		m_sHeader.ip_dstaddr[1]= dstAddress[1];
		m_sHeader.ip_dstaddr[2]= dstAddress[2];
		m_sHeader.ip_dstaddr[3]= dstAddress[3];

	}

	public byte[] ObjToByte(_IPLayer_HEADER Header, byte[] input, int length) {
		byte[] buf = new byte[length + IPHEADER];

		buf[0] = Header.ip_versionLen[0];
		buf[1] = Header.ip_serviceType[0];
		buf[2] = Header.ip_packetLen[0];
		buf[3] = Header.ip_packetLen[1];
		buf[4] = Header.ip_datagramID[0];
		buf[5] = Header.ip_datagramID[1];
		buf[6] = Header.ip_offset[0];
		buf[7] = Header.ip_offset[1];
		buf[8] = Header.ip_ttl[0];
		buf[9] = Header.ip_protocol[0];
		buf[10] = Header.ip_cksum[0];
		buf[11] = Header.ip_cksum[1];
		for (int i = 0; i < 4; i++) {
			buf[12 + i] = Header.ip_srcaddr[i];
			buf[16 + i] = Header.ip_dstaddr[i];
		}
		for (int i = 0; i < length; i++) {
			buf[IPHEADER + i] = input[i];
		}
		return buf;
	}


	public boolean Send(byte[] input, int length) {


		if((input[2]==(byte)0x20 && input[3]==(byte)0x80) || (input[2]==(byte)0x20 && input[3]==(byte)0x90) ) {
			m_sHeader.ip_offset[0] = 0x00;
			m_sHeader.ip_offset[1] = 0x03;

			byte[] bytes = ObjToByte(m_sHeader,input,length);
			((EthernetLayer)this.GetUnderLayer(1)).Send(bytes,length+IPHEADER,this);
			return true;

		}
		else {
			byte[] opcode = new byte[2];
			opcode[0] = (byte)0x00;
			opcode[1] = (byte)0x01;
			byte[] bytes = ObjToByte(m_sHeader,input,length);
			((ARPLayer)this.GetUnderLayer(0)).Send(m_sHeader.ip_srcaddr,m_sHeader.ip_dstaddr,new byte[6],new byte[6],opcode);

			return true;
		}
	}


	public byte[] RemoveCappHeader(byte[] input, int length) {

		byte[] remvHeader = new byte[length-20];
		for(int i=0;i<length-20;i++) {
			remvHeader[i] = input[i+20];
		}
		return remvHeader;// �����ϼ��� �ʿ��Ͻø�
	}

	public synchronized boolean Receive(byte[] input) {
		
		      byte[] data = RemoveCappHeader(input, input.length);
		
		      if (srcme_Addr(input)) {
		         return false;
		      }
		      if (dstme_Addr(input)) {
		         this.GetUpperLayer(0).Receive(data);
		         return true;
		      } else {
		         byte[] destIP = new byte[4];
		         System.arraycopy(input, 16, destIP, 0, 4);
		         Object[] value = routingTable.findEntry(destIP);
		         if (value != null) {
		            /* flag*/
		            byte[] netAdd;
		            if ((boolean) value[4]) {
		               netAdd = (byte[]) value[2];
		            } else {
		               netAdd = (byte[]) destIP;
		            }

		            byte[] opcode = new byte[2];
		            opcode[0] = (byte) 0x00;
		            opcode[1] = (byte) 0x01;
		
     
		            if(((String)value[6]).equals(portName)) {
		               ((ARPLayer) this.GetUnderLayer(0)).SendforARP(input, (String) value[6], m_sHeader.ip_srcaddr,
		                     netAdd, srcMacAddress, new byte[6], opcode);
		            }else {
		            	IPLayer friend = (IPLayer)this.friendIPLayer;
		            	((ARPLayer) friend.GetUnderLayer(0)).SendforARP(input, (String) value[6], friend.m_sHeader.ip_srcaddr,
		                        netAdd, friend.srcMacAddress, new byte[6], opcode);
		            }
		            
		         }
		      }
		      return false;
		
	}
	public boolean dstme_Addr(byte[] add) {
		for(int i = 0;i<4;i++) {
			if(add[i+16]!=m_sHeader.ip_srcaddr[i]) return false;
		}

		return true;
	}
	public boolean srcme_Addr(byte[] add) {
		for(int i = 0;i<4;i++) {
			if(add[i+12]!=m_sHeader.ip_srcaddr[i]) return false;
		}

		return true;
	}


	class Receive_Thread implements Runnable {
		
		byte[] input;

		ARPLayer arp;
		IPLayer friend ;
		
		public Receive_Thread(byte[] input,IPLayer friend,ARPLayer arp) {
			this.input = input;
			this.friend =friend;
			this.arp = arp;
		}

		@Override
		public void run() {
			//	      System.out.println("IP receive input length : " + input.length);
			byte[] data = RemoveCappHeader(input, input.length);

			if (srcme_Addr(input)) {
				return ;
			}
			if (dstme_Addr(input)) {
				return ;
			} else {
				byte[] destIP = new byte[4];
				System.arraycopy(input, 16, destIP, 0, 4);
				Object[] value = routingTable.findEntry(destIP);
				if (value != null) {
					/* flag Ȯ�� */
					byte[] netAdd;
					if ((boolean) value[4]) {
						netAdd = (byte[]) value[2];
					} else {
						netAdd = (byte[]) destIP;
					}
					/* ��Ʈ��ũ �ּ� -> arp */
					byte[] opcode = new byte[2];
					opcode[0] = (byte) 0x00;
					opcode[1] = (byte) 0x01;

					if(((String)value[6]).equals(portName)) {
						((ARPLayer) arp).SendforARP(input, (String) value[6], m_sHeader.ip_srcaddr,
								netAdd, srcMacAddress, new byte[6], opcode);
					}else {
						((ARPLayer) friend.GetUnderLayer(0)).SendforARP(input, (String) value[6], friend.m_sHeader.ip_srcaddr,
								netAdd, friend.srcMacAddress, new byte[6], opcode);
					}

				}
			}
			return ;
		}
	}

	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
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
		this.p_aUnderLayerIP.add(nUnderLayerCount++, pUnderLayer);
	}

	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
		// nUpperLayerCount++;

	}
	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		//      System.out.println("ip "+pUULayer.GetLayerName());
		this.SetUpperLayer(pUULayer);
		this.SetUnderLayer(pUULayer);
	}
	@Override
	public BaseLayer GetUnderLayer(int nindex) {
		if (nindex < 0 || nindex > nUnderLayerCount || nUnderLayerCount < 0)
			return null;
		return p_aUnderLayerIP.get(nindex);
	}
}