package sem.util;

import java.util.*;

import sem.exception.GraphFormatException;
import sem.graph.Graph;
import sem.graph.Node;
import sem.graphreader.GraphReader;
import sem.graphreader.RaspGraphReader;
import sem.grapheditor.GraphEditor;
import sem.grapheditor.LowerCaseGraphEditor;
import sem.grapheditor.NumTagsGraphEditor;
import sem.util.Index;
import sem.util.Gadgets;
import sem.model.SemModelAux;
/* This class serves serves as an interface to the core for finding entailment any two text fragments. */

/* The following is essentialy a 'struct' and doesn't have a 'behavior' associated with itself. */

class Match {

	Integer sentenceId;
	List<List<Edge>> matchedFragmentList;
	
	public Match(Integer sentenceId, List<List<Edge>> matchedFragmentList)
	{
		this.sentenceId = sentenceId;
		this.matchedFragmentList = matchedFragmentList;
	}	
}

class BucketGroup
{
	public Map<Integer, List<Integer> > bucketMap;
	public Map<Integer, List<Integer> > bucketSelectionsMap;
	public Map<Integer, Integer> countMap;
	public List<Edge> edges;
	public List<Edge> fragmentEdges;
	public List<List<Edge>> matchedFragmentList;

	public BucketGroup(Map<Integer, List<Integer> > bucketMap, Map<Integer, List<Integer> > bucketSelectionsMap, Map<Integer, Integer> countMap, List<List<Edge>> matchedFragmentList, List<Edge> edges, List<Edge> fragmentEdges) {
		this.countMap = countMap;
		this.bucketMap = bucketMap;
		this.bucketSelectionsMap = bucketSelectionsMap;
		this.edges = edges;
		this.fragmentEdges = fragmentEdges;
		this.matchedFragmentList = matchedFragmentList;
	}
}

public class FragmentProcessor
{
	
	private SemModelAux semModelAux;
	
	public FragmentProcessor(SemModelAux semModelAux)
	{
		this.semModelAux = semModelAux;
	}       	
	
	private Map<Integer, Graph> getDependencyGraphs(String filename)
	{
		Map<Integer, Graph> dependencyGraphsMap = new HashMap<Integer, Graph>();
		try
		{	
			GraphReader reader = new RaspGraphReader(filename, false);
			ArrayList<GraphEditor> graphEditors = new ArrayList<GraphEditor>(Arrays.asList(new LowerCaseGraphEditor(), new NumTagsGraphEditor()));
			while (reader.hasNext())
			{
				Integer sentenceId = null;
				Graph graph = reader.next(sentenceId);
				for(GraphEditor graphEditor : graphEditors)
					graphEditor.edit(graph);
				dependencyGraphs.put(sentenceId, graph);
			}		
			reader.close();	
		} catch (GraphFormatException e) {
			e.printLine();
			e.printStackTrace();
		}
		
		return dependencyGraphsMap;
	}	
		
	
	/* Returns the smallest list of all the sentenceId lists corresponding to the nodeIds contained within the text fragment. */
	private List<Integer> getRepSentenceMatches(Map<Integer, Integer> wordDistCrit)
	{
		int repSize = -1;
		List<Integer> repSentenceMatches = null;
		for (Iterator it = wordDistCrit.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry pair = (Map.Entry) it.next();
			Integer nodeId = (Integer) pair.getKey();
			Integer nodeCount = (Integer) pair.getValue();
			List<Integer> sentenceMatches = semModelAux.getSentenceMatches(nodeId, nodeCount);
			Integer curSize = sentenceMatches.size();
			if (repSize < 0 || repSize > curSize)
			{	
				repSize = curSize;
				repSentenceMatches = sentenceMatches;
			}
		}
		return repSentenceMatches;
	}	

	/* @filename consists of text fragments parsed by RASP. */
	private Map<Integer, List<Integer>> getProbSentenceMatches(String filename, Index nodeIndex)
	{
		Map<Integer, List<Integer>> probSentenceMatchesMap = new HashMap<Integer, List<Integer>>();
		/* This map is also needed in getSentenceMatches() function. */
		Map<Integer, Graph> dependencyGraphsMap = getDependencyGraphs(filename);
		for (Iterator it = dependencyGraphsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			Integer sentenceId = e.getKey();
			Graph graph = e.getValue();
			
			/* Step 1 */
			Map<Integer, Integer> wordDistCrit = Gadgets.getWordDist(graph, nodeIndex);

			/* Step 2 */
			if (wordDistCrit != null)
			{	
				List<Integer> repSentenceMatches = getRepSentenceMatches(wordDistCrit);
			
				/* Step 3 */ 
				/* Find the probable sentences which might contain a given text fragment as a subgraph of themselves. */
				/* 'probSentenceMatches' gives the sentence ids of all the sentences in the corpus which fulfill the node criteria
				 * required for them to contain the current text fragment as a subgraph of themselves. */
				List<Integer> probSentenceMatches = semModelAux.getIntersection(repSentenceMatches, wordDistCrit);
				probSentenceMatchesMap.put(sentenceId, probSentenceMatches);
			}
		}	
		/* Now, we need to perform exact matching between the dependency graphs of the text fragment and the dependency graphs of 
		 * probSentenceMatches. */
		return probSentenceMatchesMap;
	}

	private boolean isConnected(BucketGroup buckets) {
		Map<Integer, List<Integer> > nodeSelectionsMap = buckets.bucketSelectionsMap;
		List<Edge> edges = buckets.edges;
		/* We have to check if every selected node is involved in an 'edge' of the graph or not. */
		Set<Integer> connectedNodeSet = new HashSet<Integer>(); 
		for (Iterator it = edges.iterator(); it.hasNext();) {
			Edge e = (Edge) it.next();
			/*TODO getHeadId() and getDepId() */
			int headLoc = e.getHeadLoc();
			int depLoc = e.getDepLoc();
			int headId = nodeIndex.getId(e.getHead().getLabel());
			int depId = nodeIndex.getId(e.getDep().getLabel());
			if (nodeSelectionsMap.containsKey(headId) && nodeSelectionsMap.containsKey(depId))  {
				if (nodeSelectionsMap.get(headId).contains(headLoc) && nodeSelectionsMap.get(depId).contains(depLoc)) {
					connectedNodeSet.add(headLoc);
					connectedNodeSet.add(depLoc);
				}	
			}	
		}
		int nodeCount = 0;
		for (Iterator it = nodeSelectionsMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			nodeCount += e.getValue().size();
		}	
		if (nodeCount == connectedNodeSet.size())
			return true;
		return false;
	}

	private List<Edge> getEdgeMatches(List<Edge> edges, Edge e)
	{
		int headId = nodeIndex.getId(e.getHead().getLabel());	
		int depId = nodeIndex.getId(e.getDep().getLabel());
		List<Edge> eMatches = new ArrayList<Edge>();
		for (i = 0; i < edges.size(); i++)
		{
			Edge eMatch = edges.get(i);
			int headIdMatch = nodeIndex.getId(eMatch.getHead().getLabel());	
			int depIdMatch = nodeIndex.getId(eMatch.getDep().getLabel());
			if ((headId == headIdMatch) && (depId == depIdMatch) && (e.getLabel() == eMatch.getLabel()))
				eMatches.add(eMatch);
		}
		return eMatches;	
	}


	/* The 'isExactMatch' method is a recursive procedure. It recurses over all possible matchings of nodes until it finds any one match that 
	 * satisfies the subgraph problem. 
	 */
	
	/* TODO Include this check in the caller	if (edges.size() == fragmentEdges.size() && fragmentEdges.size() > 0)
								if (isExactMatch(edges, fragmentEdges, matchingsMap))
								{}							
	Make sure to have 'nodeIndex' available for this function. 	
	*/
	/* A 'true' return value propagates right up the root whereas a 'false' return value results in the adoption of a new search path until all the 
	 * possible paths are explored. 
	 * [explanation] The matchings already done during the exploration of a path should be "respected" for the left portion of the particular
	 * in the search tree. If we find any 'satisfactory' matching during such an exploration, then the 'fragment' graph is ensured to contain
	 * "sufficient"(but may be more) material for a RE-LABELING of the parent graph. If the edges of the 'fragment' graph also exhaust in the
	 * process, then we find an isomorphism between the two graphs.
	 * */
	private boolean isExactMatch(List<Edge> edges, List<Edge> fragmentEdges, Map<Integer, Integer> matchingsMap) {
		/* Exact matches would be done with the help of edges. */
		if (edges.size() == 0) {				
			if (fragmentEdges.size() == 0)
				return true;
			return false;
		}	
		/* Pick the first edge to match. */
		Edge e = edges.get(0);
		/* List of matching edges. */
		List<Edge> eMatches = getEdgeMatches(fragmentEdges, e);	
		if (eMatches.size() != 0)
		{
			/* Check if any of the 'ends' of the edge 'e' already has a mapping in the 'matchingsMap'. */
			int headLoc = e.getHeadLoc();
			int depLoc = e.getDepLoc();
			boolean headRes = false, depRes = false;
			if ( (headRes = matchingsMap.containsKey(headLoc)) && (depRes = matchingsMap.containsKey(depLoc)) )
			{	/* We have a 'constraint' for matching this edge. */
				int headMatch = matchingsMap.get(headLoc);
				int depMatch = matchingsMap.get(depLoc);
				for (j = 0; j < eMatches.size(); j++)
				{
					Edge eMatch = eMatches.get(j);
					if ((eMatch.getHeadLoc() == headMatch) && (eMatch.getDepLoc() == depMatch))
					{
						edges.remove(0);
						fragmentEdges.remove(eMatch);
						boolean result = isExactMatch(edges, fragmentEdges, matchingsMap);
						edges.add(0,e);
						fragmentEdges.add(eMatch);
						return result;
					}
				}
				return false;	
			}	
			else if (headRes || depRes)
			{
				if (headRes)
				{
					int headMatch = matchingsMap.get(headLoc);
					edges.remove(0);
					for (j = 0; j < eMatches.size(); j++)
					{
						Edge eMatch = eMatches.get(j);
						if (eMatch.getHeadLoc() == headMatch)
						{
							int depMatch = eMatch.getDepLoc();
							matchingsMap.put(depLoc, depMatch);
							fragmentEdges.remove(eMatch);
							if (isExactMatch(edges, fragmentEdges, matchingsMap))
								return true;
							fragmentEdges.add(eMatch);
							matchingsMap.remove(depLoc);
						}	
					}
					edges.add(0,e);
					return false;	
				}                                                                                                                                                        else
				{
					int depMatch = matchingsMap.get(depLoc);
					edges.remove(0);
					for (j = 0; j < eMatches.size(); j++)
					{
						Edge eMatch = eMatches.get(j);
						if (eMatch.getDepLoc() == depMatch)
						{
							int headMatch = eMatch.getHeadLoc();
							matchingsMap.put(headLoc, headMatch);
							fragmentEdges.remove(eMatch);
							if (isExactMatch(edges, fragmentEdges, matchingsMap))
								return true;
							fragmentEdges.add(eMatch);
							matchingsMap.remove(headLoc);
						}	
					}
					edges.add(0,e);
					return false;	
				}	
			}	
			else	/* no constraint on matching the edge 'e'. */
			{	
				edges.remove(0);
				for (j = 0; j < eMatches.size(); j++)
				{
					Edge eMatch = eMatches.get(j);
					matchingsMap.put(e.getHeadLoc(), eMatch.getHeadLoc());
					matchingsMap.put(e.getDepLoc(), eMatch.getDepLoc());
					fragmentEdges.remove(eMatch);
					if (isExactMatch(edges, fragmentEdges, matchingsMap))
						return true;
					fragmentEdges.add(eMatch);
					matchingsMap.remove(e.getHeadLoc());
					matchingsMap.remove(e.getDepLoc());
				}
				edges.add(0,e);
				return false;	
			 }
		}
		else
			return false;
	}	

	/* TODO nodeIndex is used in this function, but not included in the argument list. */
	private List<Edge> getEdges(List<Edge> edges, Map<Integer, List<Integer>> nodeSelectionsMap)
	{
		edgeSelectionList = new ArrayList<Edge>();
		for (Iterator it = edges.iterator(); it.hasNext();)
		{
			Edge e = (Edge) it.next();
			int headId = nodeIndex.getId(e.getHead().getLabel());
			int depId = nodeIndex.getId(e.getDep().getLabel());
			if (nodeSelectionsMap.containsKey(headId) && nodeSelectionsMap.containsKey(depId))
			{
				List<Integer> headMatchList =  nodeSelectionsMap.get(headId);
				List<Integer> depMatchList =  nodeSelectionsMap.get(depId);
				if (headMatchList.contains(e.getHeadLoc()) && depMatchList.contains(e.getDepLoc()))
					edgeSelectionList.add(e);
			}	
		}	
		return edgeSelectionList;
	}	

	private void selectFromBucket(List<Integer> list, List<Integer> selectionsList, int count, int startPos, BucketGroup buckets, Iterator nextBucket) {
		if (count == 0) {
			if (nextBucket.hasNext())
				newBucket(buckets, nextBucket);
			else {	
				/* We have a 'combination' now. */
				if (isConnected(buckets))
				{	List<Edge> nodeSelectionEdges = getEdges(buckets.edges, buckets.nodeSelectionsMap);
					Map<Integer, Integer> matchingsMap = new HashMap<Integer, Integer>();
					if (isExactMatch(nodeSelectionEdges, buckets.fragmentEdges, matchingsMap))
						buckets.matchedFragmentList.add(nodeSelectionEdges);
				}		
			}	
		}	
		
		for (int i = startPos; i < list.size(); i++) {
			Integer x = list.get(i);
			selectionsList.add(0, x);
			selectFromBucket(list, selectionsList, count-1, i+1, buckets, nextBucket);
			selectionsList.remove(0);
		}	
	
	}

	private void newBucket(BucketGroup buckets, Iterator nextBucket)
	{
		Map.Entry e = (Map.Entry) nextBucket.next();
		Integer bucketKey = e.getKey();
		int count = buckets.countMap.get(bucketKey);	
		/* We're entering a new bucket; make sure that the 'selections' list for this bucket is empty. */
		List<Integer> selectionsList;
		if (buckets.bucketSelectionsMap.containsKey(bucketKey))
			selectionsList = buckets.bucketSelectionsMap.get(bucketKey);
		else
		{
			selectionsList = new LinkedList<Integer>();
			buckets.bucketSelectionsMap.put(bucketKey, selectionsList);
		}

		selectionsList.clear();	// clear the list.
		List<Integer> list = buckets.bucketMap.get(bucketKey);
		selectFromBucket(list, selectionsList, count, 0, buckets, nextBucket);
	}	

	/* Checks whether 'fragment' is a subgraph of 'sentence' or not. */
	private boolean isFragment(Graph fragment, Graph sentence, List<List<Edge>> matchedFragmentList)
	{
		Map<Integer, List<Integer>> commonWordDistMap = Gadgets.getCommonWordDist(sentence, fragment, nodeIndex);
		Map<Integer, Integer> countMap = Gadgets.getWordDist(fragment, nodeIndex);
		Map<Integer, List<Integer>> nodeSelectionsMap = new HashMap<Integer, List<Integer>>();
		BucketGroup buckets = new BucketGroup(commonWordDistMap, countMap, nodeSelectionsMap, matchedFragmentList, sentence.getEdges(), fragment.getEdges());
		Iterator nextBucket = commonWordDistMap.entrySet().iterator();
		if (nextBucket.hasNext())	
			newBucket(buckets, nextBucket);		
		if (buckets.matchedFragmentList.size() != 0)
			return true;	
		return false;
	}	

	public Map<Integer, List<Match> > getSentenceMatches(String filename, Index nodeIndex)
	{
		/* TODO Modify getProbSentenceMatches() and printProbMatches() functions for the new return types. */	
		Map<Integer, List<Integer>> probSentenceMatchesMap = getProbSentenceMatches(filename, nodeIndex);
		/* The exact matchings would be done using a brute-force method using all possible options for all possible nodes. */
		Map<Integer, List<Match> > exactSentenceMatchesMap = new HashMap<Integer, List<Match> >();
		for (Iterator it = probSentenceMatchesMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry e = (Map.Entry) it.next();
			int fragmentId = e.getKey();
			Graph fragment = fragmentGraphsMap.get(fragmentId);
			List<Integer> probSentenceMatches = e.getValue();
			for (Iterator iu = probSentenceMatches.iterator(); iu.hasNext();)
			{
				Integer sentenceId = (Integer) iu.next();
				Graph sentence = sentenceGraphsMap.get(sentenceId);
				List<List<Edge>> matchedFragmentList = new ArrayList<List<Edge>>(); 
				if (isFragment(fragment, sentence, matchedFragmentList))
				{
					Match match = new Match(sentenceId, matchedFragmentList);
					if (exactSentenceMatchesMap.containsKey(fragmentId))
						exactSentenceMatchesMap.get(fragmentId).add(match);
					else
					{
						List<Match> exactSentenceMatches = new ArrayList<Match>();
						exactSentenceMatches.add(match);
						exactSentenceMatchesMap.put(fragmentId, exactSentenceMatches);
					}	
				}	
			}	
		
		}			
		return exactSentenceMatchesMap;
	}

}	
