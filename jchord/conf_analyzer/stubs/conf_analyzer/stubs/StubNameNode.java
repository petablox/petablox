/*This code is automatically generated for use as a stub/test harness
 * for the configuration debug tools.  */
package conf_analyzer.stubs;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.net.ScriptBasedMapping;

public class StubNameNode {
	
  public static NameNode createNameNode(String argv[], 
      Configuration conf) throws Exception {
  	NameNode nn = NameNode.createNameNode(argv, conf);
  	exercise(nn);
  	return nn;
  }
	
  public static void exercise(org.apache.hadoop.hdfs.server.namenode.NameNode inst) throws Exception {
    inst.getProtocolVersion (null,0);
    inst.getNamesystem ();
    inst.join ();
    inst.stop ();
    inst.getBlocks (null,0);
    inst.getBlockLocations (null,0,0);
    inst.create (null,null,null,false,(short) 0,0);
    inst.append (null,null);
    inst.setReplication (null,(short) 0);
    inst.setPermission (null,null);
    inst.setOwner (null,null,null);
    inst.addBlock (null,null);
    inst.abandonBlock (null,null,null);
    inst.complete (null,null);
    inst.reportBadBlocks (null);
    inst.nextGenerationStamp (null);
    inst.commitBlockSynchronization (null,0,0,false,false,null);
    inst.getPreferredBlockSize (null);
    inst.rename (null,null);
    inst.delete (null);
    inst.delete (null,false);
    inst.mkdirs (null,null);
    inst.renewLease (null);
    inst.getListing (null);
    inst.getFileInfo (null);
    inst.getStats ();
    inst.getDatanodeReport (null);
    inst.setSafeMode (null);
    inst.isInSafeMode ();
    inst.saveNamespace ();
    inst.refreshNodes ();
    inst.getEditLogSize ();
    inst.rollEditLog ();
    inst.rollFsImage ();
    inst.finalizeUpgrade ();
    inst.distributedUpgradeProgress (null);
    inst.metaSave (null);
    inst.getContentSummary (null);
    inst.setQuota (null,0,0);
    inst.fsync (null,null);
    inst.setTimes (null,0,0);
    inst.register ( new DatanodeRegistration());
    inst.sendHeartbeat (null,0,0,0,0,0);
    inst.blockReport (null,null);
    inst.blockReceived (null,null,null);
    inst.errorReport (null,0,null);
    inst.versionRequest ();
    inst.processUpgradeCommand (null);
    inst.verifyRequest (null);
    inst.verifyVersion (0);
    inst.getFsImageName ();
    inst.getFSImage ();
    inst.getFsImageNameCheckpoint ();
    inst.getNameNodeAddress ();
    inst.getHttpAddress ();
    inst.refreshServiceAcl ();
    
    ScriptBasedMapping t = new ScriptBasedMapping();
    t.setConf(new Configuration());
    t.resolve(new ModelList<String>());
  }
}
