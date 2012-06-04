package chemical;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.IChemObjectReader.Mode;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.layout.StructureDiagramGenerator;


public class Chemical {
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		String name = "aspirin";
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet getInchi = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" + name + "/stdinchikey");
		HttpResponse response = httpclient.execute(getInchi);
		HttpEntity entity = response.getEntity();
		String inchiInfo = "InChI key of " + name + ": " + EntityUtils.toString(entity);
		System.out.println(inchiInfo);
		
		HttpGet getInchi2 = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" + name + "/smiles");
		HttpResponse response2 = httpclient.execute(getInchi2);
		HttpEntity entity2 = response2.getEntity();
		String inchiInfo2 = "Image:" + EntityUtils.toString(entity2);
		System.out.println(inchiInfo2);
		
	
		

	}
}