// This code computes the single source shortest paths in a graph represented 
// by the adjacency list by using the Belman-Ford algorithm. The source 
// node is taken as the first command line parameter.
//
// 1. Save this file as BellmanFord.java
// 2. Compile it with javac BellmanFord.java
// 3. Run with java BellmanFord src < bf.dat (or your own file name) 

import java.util.*;
import java.io.File;
import java.io.IOException;

public class BellmanFord
{
  private static class Vertex   // aux. class for the graph vertices
  {
    String label;               // vertex label
    int pred;                   // link to pred. in DFS forest
    double d;                   // the d-value
    public Vertex(String label) // constructor
    {
      this.label = label;       // set up the vertex label
      pred = -1;                // pred = nil
    }
  }

  private static class Arc      // aux. class for for storing edge weights
  {                             //  (to be used in adjacency list)
    int vertex;                 // incoming node index
    double weight;              // edge weight
    public Arc(int v, double w) // constructor
    {
      vertex = v;
      weight = w;
    }
  }

  private static Vertex[] vertices;  // array of graph vertices
  private static Arc[][] adjList;    // storage for adjacency list
  private static int source;         // index of the source vertex

  public static void main(String[] args) throws IOException
  {
    readGraph(args);                 // read graph encoding
    printAdjList("Adjacency list of G", adjList);

    if (bf(source))                  // run the Belman-Ford method
      printShortestPaths(source);
    else
      System.out.println("G contains a negative weight loop");
  }

  public static boolean bf(int s)    // Bellman-Ford shortest path algo
  {
    initSingleSource(s);

    for (int i=1; i<vertices.length; i++)
      for (int u=0; u<adjList.length; u++)
        for (int v=0; v<adjList[u].length; v++)
          relax(u, adjList[u][v].vertex, adjList[u][v].weight);

    for (int u=0; u<adjList.length; u++)
      for (int v=0; v<adjList[u].length; v++)
        if (vertices[adjList[u][v].vertex].d > 
            vertices[u].d + adjList[u][v].weight)
          return(false);

    return(true);
  }

  public static void initSingleSource(int s)
  {
    for (int v=0; v<vertices.length; v++)
    {
      vertices[v].d = Double.MAX_VALUE;      // d = infinity
      vertices[v].pred = -1;                 // pred = nil
    }
    vertices[s].d = 0;
  }

  public static void relax(int u, int v, double w)
  {
    if (vertices[v].d > vertices[u].d + w)
    {
      vertices[v].d = vertices[u].d + w;
      vertices[v].pred = u;
    }
  }

  public static void printShortestPaths(int s)
  {
    System.out.println("\nShortest paths from " + vertices[s].label + ":");
    for (int v=0; v<vertices.length; v++)
    {
      Stack<Integer> stack = new Stack<Integer>();
      stack.push(new Integer(v)); // pushing the vertices on the path v -> s   
      for (int u=vertices[v].pred; u >= 0; u=vertices[u].pred)
        stack.push(new Integer(u));

      int i = stack.pop().intValue();               // source vertex (=s)
      System.out.print(vertices[i].label);          // print source vertex
      int length = 0;                               // path length s -> v
      if (v == s) System.out.print(" -> " + vertices[i].label);
      else                                          // v is not the source
      while (! stack.isEmpty())                     // do for the whole path
      {
        int j = stack.pop().intValue();             // vertex on the path
        System.out.print(" -> " +vertices[j].label);// print it 
        int k = 0;                                  // find the weight of
        for ( ; adjList[i][k].vertex != j; k++);    // the edge i -> j
        length += adjList[i][k].weight;             // update the path length
        i = j;
      }
      System.out.println(" length=" + length);      // print the path length
    }
  }

  // this auxilary method prints the adjacency matrix of the graph
  public static void printAdjList(String s, Arc[][] adj)
  {
    System.out.println(s);
    for (int i=0; i<adj.length; i++)
    {
      System.out.print(vertices[i].label + ": ");
      for (int j=0; j<adj[i].length; j++)
        System.out.print(vertices[adj[i][j].vertex].label + "/" +
          adj[i][j].weight + " ");
      System.out.println();
    }
  }

  // this method reads graph encoding from file
  public static void readGraph(String[] args) throws IOException
  {
    if (args.length != 2)
    {
      System.out.println("No graph and source node specified");
      System.exit(0);
    }
    Scanner inFile = new Scanner(new File(args[0]));

    HashMap<String, Integer> nodeMap =
       new HashMap<String, Integer>(); // storage for vertex labels
    ArrayList<Vertex> nodeList =
       new ArrayList<Vertex>();        // storage for vertex objects
    String input = "";                 // storage for the entire file data
    int index = 0;
    String s = "";
    while (inFile.hasNextLine())       // read all file records
    {
      s = inFile.nextLine();           // read a text line from file
      if (s.length() > 0 && s.charAt(0) != '#') // ignore comments
      {
        StringTokenizer sTk = new StringTokenizer(s);
        String label = sTk.nextToken(); // vertex label
        if (! nodeMap.containsKey(label)) // check for a new vertex in hash
        {
          Vertex v = new Vertex(label);   // create the vertex object
          nodeMap.put(label, new Integer(index)); // assign to it an index
          nodeList.add(v);                        // put it into node list
          index++;
          input = input + "#" + s;     // append the input line
        }
        else                           // this vertex is already processed
        {
          System.out.println("Multiple declaration of vertex: " + label);
          System.exit(0);
        }
      }
    }

    if (nodeMap.containsKey(args[1]))
      source = nodeMap.get(args[1]).intValue();
    else
    {
      System.out.println("Non-existing source node " + args[1]);
      System.exit(0);
    }

    // converting list into array
    vertices = (Vertex[]) nodeList.toArray(new Vertex[nodeList.size()]);

    // computing the adjacency list
    adjList = new Arc[vertices.length][];  // we already know # of vertices
    StringTokenizer sTk = new StringTokenizer(input, "#");
    for (int i=0; i<vertices.length; i++)
    {
      StringTokenizer sTk1 = new StringTokenizer(sTk.nextToken());
      adjList[i] = new Arc[(sTk1.countTokens()-1)/2];
      s = sTk1.nextToken();
      for (int j=0; j<adjList[i].length; j++)
      {
        s = sTk1.nextToken();
        double weight = 0;
        try
        {
          weight = Double.parseDouble(sTk1.nextToken());  //edge wgt
        }
        catch(NumberFormatException nfe)
        {
          System.out.println("Wrong label/weight combination in the " +
               "adjacency list of " + vertices[i].label);
          System.exit(0);
        }
        catch(NoSuchElementException nfe)
        {
          System.out.println("Wrong adjacency list for node " + 
                             vertices[i].label);
          System.exit(0);
        }

        if (nodeMap.containsKey(s))
        {
          int ind = nodeMap.get(s).intValue();
          adjList[i][j] = new Arc(ind, weight);
        }
        else
        {
          System.out.println("Undeclared vertex: " + s);
          System.exit(0);
        } 
      }
    }
  }
}
