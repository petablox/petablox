package conf_analyzer.stubs;

import java.net.InetAddress;

import rice.p2p.commonapi.*;

public class PastryStub {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
//      org.mpisws.p2p.pki.x509.CAToolImpl.main(args);
//      org.mpisws.p2p.testing.filetransfer.EncryptedFileTest.main(args);
//      org.mpisws.p2p.testing.filetransfer.ProfileFileTest.main(args);
      org.mpisws.p2p.testing.transportlayer.MagicNumberTest.main(args);
//      org.mpisws.p2p.testing.transportlayer.RapidRebind.main(args);
//      org.mpisws.p2p.testing.transportlayer.SSLTest.main(args);
//      org.mpisws.p2p.testing.transportlayer.peerreview.AuthenticatorStoreTest.main(args);
//      org.mpisws.p2p.testing.transportlayer.peerreview.PRInconsistent1.main(args);
//      org.mpisws.p2p.testing.transportlayer.peerreview.PRNonconform1.main(args);
//      org.mpisws.p2p.testing.transportlayer.peerreview.PRNonconform2.main(args);
      org.mpisws.p2p.testing.transportlayer.peerreview.PRRegressionTest.main(args);
      org.mpisws.p2p.testing.transportlayer.replay.Recorder.main(args);
//      org.mpisws.p2p.testing.transportlayer.replay.Replayer.main(args);
      rice.environment.processing.simple.SimpleProcessor.main(args); //IS THIS SLOW?
      rice.p2p.multiring.RingCertificate.main(args);
//      rice.p2p.multiring.testing.MultiringRegrTest.main(args);
//      rice.p2p.past.testing.DistPastTest.main(args);
      rice.p2p.past.testing.PastRegrTest.main(args);
      rice.p2p.past.testing.RawPastRegrTest.main(args);
      rice.p2p.scribe.testing.ScribeRegrTest.main(args);
      rice.p2p.splitstream.testing.SplitStreamRegrTest.main(args);
      rice.p2p.util.BloomFilter.main(args); //SLOW?
      rice.pastry.Id.main(args);
//      rice.pastry.direct.proximitygenerators.SphereNetworkProximityGenerator.main(args);
      rice.pastry.standard.StandardAddress.main(args); //SLOW?
      rice.pastry.testing.DirectPastryPingTest.main(args);
      rice.pastry.testing.DirectPastryRegrTest.main(args);
      rice.pastry.testing.DistHelloWorld.main(args);
      rice.pastry.testing.PartitionHandlerTest.main(args);
      rice.pastry.testing.rendezvous.DistTutorialInternet.main(args);
      rice.persistence.testing.GlacierPersistentStorageTest.main(args);
      rice.persistence.testing.MemoryStorageTest.main(args);
      rice.tutorial.lookup.LookupServiceTest.main(args);
      rice.tutorial.transportdirect.DistTutorial.main(args); //SLOW?
      rice.tutorial.transportlayer.DistTutorial.main(args); //SLOW?
      
      Class<rice.p2p.commonapi.Application> ac = (Class<Application>) Class.forName(args[0]);
      Class<rice.p2p.commonapi.Id> id_c = (Class<Id>) Class.forName(args[0]);
      Class<Message> m_c = (Class<Message>) Class.forName(args[0]);
      Class<RouteMessage> rm_c = (Class<RouteMessage>) Class.forName(args[0]);
      Class<NodeHandle> nh_c = (Class<NodeHandle>) Class.forName(args[0]);
      Class<rice.pastry.socket.nat.NATHandler> nhandle_c = (Class<rice.pastry.socket.nat.NATHandler>)
      		Class.forName(args[0]);
      Class<rice.p2p.aggregation.AggregationImpl> ai_c = (Class<rice.p2p.aggregation.AggregationImpl>)
  		Class.forName(args[0]);
      
      Class<rice.p2p.glacier.v2.GlacierImpl> gi_c = (Class<rice.p2p.glacier.v2.GlacierImpl>)
  		Class.forName(args[0]);
      
      
      Class<rice.p2p.past.gc.GCPast> gcpast_c = (Class<rice.p2p.past.gc.GCPast>)
  		Class.forName(args[0]);

      rice.p2p.commonapi.Application a = ac.newInstance();
      Id id = id_c.newInstance(); 
      Message me = m_c.newInstance(); 
      RouteMessage rm = rm_c.newInstance();
      NodeHandle nh = nh_c.newInstance();
      rice.pastry.socket.nat.NATHandler nhandle = nhandle_c.newInstance();

      nhandle.findAvailableFireWallPort(0, 0, 0, "");
      nhandle.findFireWall(InetAddress.getLocalHost());
      nhandle.getFireWallExternalAddress();
      nhandle.openFireWallPort(0, 0, "");

      a.deliver(id, me);
      a.forward(rm);
      a.update(nh, false);
      
//      rice.p2p.past.gc.GCPast gcpast = new rice.p2p.past.gc.GCPastImpl(null, null, 0, "", null, 0);
//      gcpast_c.newInstance();
      
      /*
      rice.p2p.glacier.v2.GlacierImpl gi = gi_c.newInstance();
      gi.forward(rm);
      //LOTS MORE!
      gi.getReplicationFactor();
      gi.neighborSeen(id, 0);*/
      
//      rice.p2p.aggregation.AggregationImpl ai = ai_c.newInstance();
//      ai.
      
      
		} catch (Exception e) {
		}
	}
}
