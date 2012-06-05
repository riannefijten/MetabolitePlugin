package chemical;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
import org.openscience.cdk.io.SMILESReader;
import org.openscience.cdk.layout.StructureDiagramGenerator;


public class Chemical {
	public static String Inchi(){
	String cid = "methane";
	HttpClient httpclient = new DefaultHttpClient();
	HttpGet getInchi = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" + cid + "/stdinchikey");
	HttpResponse response = null;
	String inchiInfo = null;
	try {
		response = httpclient.execute(getInchi);
		HttpEntity entity = response.getEntity();
		inchiInfo = "InChI key of " + cid + ": " + (EntityUtils.toString(entity));
	} catch (ClientProtocolException e) {
		inchiInfo = "Exception occured. Request failed.";
	} catch (IOException e) {
		inchiInfo = "Exception occured. Request failed.";
	}
	return inchiInfo;
	}
	
	public static void main(String[] args){
		JFrame frame = new JFrame();
		JPanel panel = new JPanel();
		JLabel label = new JLabel(Inchi());
		panel.add(label);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
}
