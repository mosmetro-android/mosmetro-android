package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;

public class MT_Free extends MosMetro {
	public static String SSID = "MT_Free";

	public MT_Free(Context context) {
		super(context);
	}
	
	@Override
	public String getSSID() {
		return "MT_Free";
	}
}