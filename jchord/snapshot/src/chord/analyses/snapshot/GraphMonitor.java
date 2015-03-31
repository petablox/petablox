/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import chord.util.Utils;

import chord.project.Config;

public interface GraphMonitor {
  public void finish();
  public void addNode(int a, String label);
  public void deleteEdge(int a, int b, String label);
  public void addEdge(int a, int b, String label);
  public void setNodeLabel(int a, String label);
  public void setNodeColor(int a, String color);
  public void setNodeColor(int a, int color);
}

class SerializingGraphMonitor implements GraphMonitor {
  PrintWriter out;
  int max;
  int n = 0;
  public SerializingGraphMonitor(String path, int max) {
    this.out = Utils.openOut(path);
    this.max = max;
  } 
  boolean c() {
    if (n >= max) return false;
    n++;
    return true;
  }
  public void finish() { out.close(); }
  public void addNode(int a, String label) { if (c()) out.printf("n %s%s\n", a, label == null ? "" : " "+label); }
  public void deleteEdge(int a, int b, String label) { if (c()) out.printf("-e %s %s%s\n", a, b, label == null ? "" : " "+label); }
  public void addEdge(int a, int b, String label) { if (c()) out.printf("e %s %s%s\n", a, b, label == null ? "" : " "+label); }
  public void setNodeLabel(int a, String label) { if (c()) out.printf("nl %s %s\n", a, label); }
  public void setNodeColor(int a, String color) { if (c()) out.printf("nc %s %s\n", a, color); }
  public void setNodeColor(int a, int color) { }
}

/*class UbiGraphMonitor implements GraphMonitor {
  UbigraphClient graph = new UbigraphClient();
  TIntIntHashMap nodes = new TIntIntHashMap();
  TLongIntHashMap edges = new TLongIntHashMap();

  String[] colors;

  long encode(int a, int b) { return a*1000000000+b; }

  public UbiGraphMonitor() {
    graph.clear();
    Random random = new Random(1);
    colors = new String[1000];
    for (int i = 0; i < colors.length; i++)
      colors[i] = String.format("#%06x", random.nextInt(1<<(4*6)));
    graph.setEdgeStyleAttribute(0, "arrow", "true"); // Default
  }
  public void finish() { }
  public void addNode(int a, String label) {
    int v = graph.newVertex();
    if (label != null) graph.setVertexAttribute(v, "label", label);
    nodes.put(a, v);
  }
  public void setNodeLabel(int a, String label) {
    graph.setVertexAttribute(nodes.get(a), "label", label);
  }
  public void setNodeColor(int a, int color) {
    graph.setVertexAttribute(nodes.get(a), "color", colors[color%colors.length]);
  }
  public void setNodeColor(int a, String color) {
    graph.setVertexAttribute(nodes.get(a), "color", color);
  }
  public void deleteEdge(int a, int b) {
    int e = edges.remove(encode(a, b));
    graph.removeEdge(e);
  }
  public void addEdge(int a, int b, String label) {
    if (a == b) return; // Ubigraph doesn't support self-loops
    int e = graph.newEdge(nodes.get(a), nodes.get(b));
    //if (label != null) graph.setEdgeAttribute(e, "label", label); // Can't do this now - need to create edge style
    edges.put(encode(a, b), e);
  }
}*/
