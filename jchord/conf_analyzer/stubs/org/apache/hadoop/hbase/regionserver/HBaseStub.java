package org.apache.hadoop.hbase.regionserver;

public class HBaseStub {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			org.apache.hadoop.hbase.LocalHBaseCluster.main(args);
			org.apache.hadoop.hbase.regionserver.HRegionServer.main(args);
			org.apache.hadoop.hbase.regionserver.HRegion.main(args);
			org.apache.hadoop.hbase.regionserver.MemStore.main(args);
			org.apache.hadoop.hbase.regionserver.ShutdownHook.main(args);
			org.apache.hadoop.hbase.thrift.ThriftServer.main(args);
			org.apache.hadoop.hbase.rest.Main.main(args);
//			org.apache.hadoop.hbase.io.BatchUpdate.main(args);
			org.apache.hadoop.hbase.master.HMaster.main(args);
//			org.apache.hadoop.hbase.util.Migrate.main(args);
			org.apache.hadoop.hbase.zookeeper.HQuorumPeer.main(args);
			org.apache.hadoop.hbase.zookeeper.ZKServerTool.main(args);
			
		} catch(Exception e) {}
	}

}
