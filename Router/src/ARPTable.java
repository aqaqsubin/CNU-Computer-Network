
public class ARPTable {

	ARPLayer arpLayer1;
	ARPLayer arpLayer2;
	ApplicationLayer app;

	ARPTable() {
		this.arpLayer1 = null;
		this.arpLayer2 = null;
	}

	ARPTable(ARPLayer arpLayer1, ARPLayer arpLayer2, ApplicationLayer app) {
		this.arpLayer1 = arpLayer1;
		this.arpLayer2 = arpLayer2;
		this.app = app;

	}

	public void setARPLayer(ARPLayer arpLayer1, ARPLayer arpLayer2) {
		this.arpLayer1 = arpLayer1;
		this.arpLayer2 = arpLayer2;
	}

	public  void updateARPCacheTable() {
		app.TotalArea.setText("");
		if (arpLayer1 != null && arpLayer2 != null) {

			arpLayer1.updateCacheTable();
			arpLayer2.updateCacheTable();
		}

	}
	public void addEntry(String ipAddress, Object[] value, String portName) {
		if (value[2].equals("Incomplete")) {
            app.TotalArea.append("       " + ipAddress + "\t" + "??????????????\t incomplete"+portName+"\n");
         } else {
            byte[] maxAddr = (byte[]) value[1];
            String macAddress = String.format("%X:", maxAddr[0]) + String.format("%X:", maxAddr[1])
                  + String.format("%X:", maxAddr[2]) + String.format("%X:", maxAddr[3])
                  + String.format("%X:", maxAddr[4]) + String.format("%X", maxAddr[5]);
            
            System.out.println(ipAddress +" : "+macAddress);
            app.TotalArea.append("       " + ipAddress + "\t" + macAddress + "\t complete"+portName+"\n");
         }
		return;
	}

}
