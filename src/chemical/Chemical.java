package chemical;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;


public class Chemical {
	public static void Pubchem(){
		String cid = "5793";
		String pubChemInchi = null;
		try {
		
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet pubChemRequest = new HttpGet("http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid="
				+ cid + "5793&loc=ec_rcs");
		
		HttpResponse response = null;
		response = httpclient.execute(pubChemRequest);
		
		HttpEntity entity = response.getEntity();
		pubChemInchi = EntityUtils.toString(entity);
		
	} catch (ClientProtocolException ClientException) {
		System.out.println(ClientException.getMessage());
		ClientException.printStackTrace();
	} catch (IOException IoException) {
		System.out.println(IoException.getMessage());
		IoException.printStackTrace();
	} catch (Throwable throwable) {
		  System.out.println(throwable.getMessage());
		  throwable.printStackTrace();
	}
		System.out.println(pubChemInchi);
		
	}
}
