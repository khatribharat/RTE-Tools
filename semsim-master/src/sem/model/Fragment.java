import java.util.Map;
import java.util.ArrayList;
import java.util.Set;

import sem.graph.Graph;
import sem.graph.Edge;
import sem.util.Index;

class Enumerator {

	private List<Edge> edges;
	private List<List<Edge> > completeEdgeReps;
	private Index nodeIndex;
	
	public Enumerator(List<Edge> edges, Index nodeIndex)
	{
		this.edges = edges;
		this.nodeIndex = nodeIndex;
		completeEdgeReps = new ArrayList<List<Edge> >();
		init();
	}

	private void init()
	{	
		List<Edge> left = new ArrayList<Edge>(edges);
		List<Edge> selected = new ArrayList<Edge>();
		enumerate(left, selected);
	}

	/* Checks whether a given set of edges is 'COMPLETE' or not. Not every edges representation can represent a node subgraph. We need to filter out
	 * the edges representations which cannot represent any node subgraphs. 
	 * Let A = the edges representation represents a dependency graph.
	 *     B = the nodes (part of any edge of the edges representation) must be 'FULLY' connected.
	 * Now, A -> B <=> B' -> A' where X' denotes the complement of X. Hence, a violation of B would imply a violation of A. But if B holds true, then 
	 * we can find atleast one node subgraph (one with the set of nodes we're working on) representated by our edges representation.
	 */	
	private boolean checkCompleteEdgeRep(List<Edge> selected)
	{
		/* First find the set of nodes contained within these edges. */
		Set<Integer> nodeSet = new HashSet<Integer>();
		List<Edge> completeEdgeSet = new ArrayList<Edge>();
		for (Iterator it = selected.iterator(); it.hasNext();)
		{
			Edge e = (Edge) it.next();
			nodeSet.add(nodeIndex.getId(e.getHead().getLabel()));
			nodeSet.add(nodeIndex.getId(e.getDep().getLabel()));
		}			
		/* nodeSet contains the set of nodes associated with any of the edges of the edges representation. */ 			
		/* Now, we need to check if these nodes are fully connected or not (the validity of 'B'). */
		for (Iterator it = edges.iterator(); it.hasNext();)
		{
			Edge e = (Edge) it.next();
			Integer e1 = nodeIndex.getId(e.getHead().getLabel());
			Integer e2 = nodeIndex.getId(e.getDep().getLabel());
			if (nodeSet.contains(e1) && nodeSet.contains(e2))
				completeEdgeSet.add(e);
		}			
		if (completeEdgeSet.size() == selected.size())
			return true;
		return false;
	}

	private void putCompleteEdgeRep(List<Edge> selected)
	{
		this.completeEdgeReps.add(selected);
	}
	
	public List<List<Edge> > getCompleteEdgeReps()
	{
		return this.completeEdgeReps;
	}

	private void enumerate(List<Edge> left, List<Edge> selected)
	{
		if (left.size() == 0)
		{
			if (checkCompleteEdgeRep(selected))
				putCompleteEdgeRep(selected);	
			return;
		}

		Edge e = left[left.size()-1];
		left.remove(left.size()-1);
		
		enumerate(left, selected);
		
		selected.add(e);
		enumerate(left, selected);

		/* restoring the states of left and selected before returning to the parent function. */
		left.add(e);	
	}
}

public class Fragment {

/* featureCountMap stores the # of times a feature occurs with a fragment in the corpus. */
private Map<Integer, Integer> featureCountMap;
private Index featureIndex;
private Index nodeIndex;

public Fragment(Index featureIndex, Index nodeIndex) {
	featureCountMap = new HashMap<Intger, Integer>();
	this.featureIndex = featureIndex;
	this.nodeIndex = nodeIndex;
}
	
/* Finds the features of a fragment and updates the 'featureCountMap' appropriately. */ 
private void putStats(List<Edge> completeEdgeRep, List<Edge> edges)
{
	/* 'completeEdgeRep' represents a fragment i.e. a CONNECTED NODE SUBGRAPH */
	Set<Integer> nodeSet = new HashSet<Integer>();
	for (Iterator it = completeEdgeRep.iterator(); it.hasNext();)
	{
		Edge e = (Edge) it.next();
		nodeSet.add(nodeIndex.getId(e.getHead().getLabel()));			
		nodeSet.add(nodeIndex.getId(e.getDep().getLabel()));			
	}
	/* Every 'completeEdgeRep' represents a UNIQUE connected dependency graph. */
	for(Iterator it = edges.iterator(); it.hasNext();)
	{
		Edge e = (Edge) it.next();
		Integer e1 = nodeIndex.getId(e.getHead().getLabel());
		Integer e2 = nodeIndex.getId(e.getDep().getLabel());
		if ( (nodeSet.contains(e1) && nodeSet.contains(e2)) || !(nodeSet.contains(e1) || nodeSet.contains(e2)) )
			continue;
		else
		{
			/* This edge is a feature of this fragment. */
			Integer edgeId = edgeIndex.getId(e.getLabel());
			String featureLabel = edgeId + "," + e2;
			Integer featureId = featureIndex.getId(featureLabel);
			if (featureCountMap.containsKey(featureId))
				featureCountMap.put(featureId, featureCountMap.get(featureId)+1);
			else
				featureCountMap.put(featureId,1);		
		}
	}
}


/* A fragment is any (weakly) connnected subgraph of a dependency graph. Weak connectivity is defined as follows: for every node (i) there exists
 * a node (j) such that either (i) -> (j) or (j) -> (i). We need to enumerate all the fragments of all the dependency graphs within the corpus in
 * order to compute P(*,f), which requires us to calculate the # of times a particular feature 'f' occurs with any fragment within the corpus. 
 * DEFINITION: A fragment is any connected subgraph of a directed dependency graph containing one or more words and the grammatical relations between them.
 * We won't however consider fragments that span across sentences or documents (even if they actually satisfy the definition of a fragment) since such
 * fragments might not serve any purpose as far as their semantic meanings are concerned.
 * We can enumerate the set of all CONNECTED node subgraphs through enumerating the set of 'COMPLETE' edge representations. Every connected subgraph has 
 * a 'COMPLETE' edges representation. And given a 'COMPLETE' edges representation, we have a UNIQUE connected subgraph. The notion of 'COMPLETE' edges 
 * representation arises from the question of whether every edges representation has a corresponding node subgraph or not. It occurs that only 'COMPLETE' 
 * edges representations have a corresponding node subgraph.
 * No connected node subgraph could be left since every such graph has a 'COMPLETE' edges representation and all such edges representations are enumerated
 * where each of them produces a UNIQUE connected node subgraph.
 */

private void buildStats(Map<Integer, List<Graph> > sentenceGraphMap)
{
	for (iterator it = sentenceGraphMap.entrySet().iterator; it.hasNext();)
	{
		Map.Entry e = (Map.Entry) it.next();
		/* We, at present, only consider a single valid parse of each sentence in the corpus. Hence, the index '0' in the following statement. */
		Graph g = e.getValue().get(0);
		List<Edge> edges = g.getEdges();
		/* We need to enumerate all the 'COMPLETE' edges representations. */
		Enumerator enumerator = new Enumerator(edges, nodeIndex);				
		List<List<Edge> > completeEdgeReps = enumerator.getCompleteEdgeReps();
		for (Iterator iu = completeEdgeReps.iterator(); iu.hasNext();)
		{
			List<Edge> completeEdgeRep = (List) iu.next();
			putStats(completEdgeRep, edges);
		}  
	}	
}	

}
