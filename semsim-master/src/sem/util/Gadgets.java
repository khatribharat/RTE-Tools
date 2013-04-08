package sem.util;

import java.util.*;
import java.io.IOException;

import sem.graph.Graph;
import sem.graph.Node;
import sem.util.Index;

public class Gadgets
{
	public static Map<Integer, Integer> getWordDist(Graph g, Index nodeIndex)
	{
		Map<Integer, Integer> wordDistMap = new HashMap<Integer, Integer>();
		List<Node> nodeList = g.getNodes();
		for (Iterator it = nodeList.iterator(); it.hasNext(); )
		{
			Node node = (Node)it.next();
			/* Check if such a node exists in the corpus; in case it doesn't, then the fragment given doesn't have a match in the corpus and extrinisc similarity cannot be calculated. */
			Integer nodeId = nodeIndex.getId(node.getLabel());
			if (nodeId != null)
			{
				if (wordDistMap.containsKey(nodeId))
				{
					int value = wordDistMap.get(nodeId);
					wordDistMap.put(nodeId, value+1);
				}	
				else
					wordDistMap.put(nodeId,1);
			}
			else
			{	
				System.out.println("Gadgets.java: The node " + node.getLabel() + " doesn't exist in the node index of the corpus.");
				System.out.println("Gadgets.java: Returning 'null' value for the given text fragment.");
				//printNodeIndex(nodeIndex);
				return null;					
			}	
		}
		return wordDistMap;	
	}	

	public static void printNodeIndex(Index nodeIndex)
	{
		int inChar;
		HashMap<String, Integer> idMap = nodeIndex.getIdMap();
		for (Iterator it = idMap.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry entry = (Map.Entry) it.next();
			System.out.println(entry.getKey() + " => " + entry.getValue());
			try
			{
				inChar = System.in.read();
			} catch (IOException e) {
				System.out.println("Error reading input.");
			}	
		}	
	}

//	public static Map<Integer, List<Integer> > getCommonNodeDist(Graph parent, Graph subgraph, Index nodeIndex)	
//	{
//		Map<Integer, Integer> subgraphWordDistMap = getWordDist(subgraph, nodeIndex);
//		ArrayList<Node> nodes = parent.getNodes();
//		Map<Integer, List<Integer> > commonWordDistMap = new HashMap<Integer, List<Integer> >();
//		for (int i = 0; i < nodes.size(); i++)
//		{
//			Node node = nodes.get(i);
//			int index = nodeIndex.getId(node.getLabel());
//			if (subgraphWordDistMap.containsKey(index))
//			{
//				if (commonWordDistMap.containsKey(index))
//					List<Integer> matchIndicesList = commonWordDistMap.get(index);
//				else
//				{
//					List<Integer> matchIndicesList = new ArrayList<Integer>();
//					commonWordDistMap.put(index, matchIndicesList);
//				}
//				matchIndicesList.add(i);
//			}	
//			
//		}	
//		return commonWordDistMap;
//	}	
}
