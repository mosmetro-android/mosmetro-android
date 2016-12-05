package pw.thedrhax.mosmetro.authenticator.networks;

import android.content.Context;

public class MT_FREE extends MosMetro {
	public static String SSID = "MT_FREE";

	public MT_FREE(Context context) {
		super(context);
	}
	
	@Override
	public String getSSID() {
		return "MT_FREE";
	}
}