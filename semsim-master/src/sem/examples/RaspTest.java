package sem.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.File;

import sem.exception.GraphFormatException;
import sem.exception.SemModelException;
import sem.graph.Graph;
import sem.grapheditor.GraphEditor;
import sem.grapheditor.LowerCaseGraphEditor;
import sem.grapheditor.NumTagsGraphEditor;
import sem.graphreader.GraphReader;
import sem.graphreader.RaspGraphReader;
import sem.model.SemModel;
import sem.model.SemModelAux;
import sem.model.VectorSpace;
import sem.util.Index;
import sem.util.FragmentProcessor;
/* class RaspTest is used to set up a framework for our fragment entailment engine to work on. */
public class RaspTest {

/* @arg[0] contains the filename of the file containing the parsed sentences from the corpus.
 * @arg[1] contains the filename of the file containing the parsed text fragments between which a similarity score is required.
 */

public static void main(String[] args) {
		try {
			// First, let's build the model
			// Using the SemGraph library to read in the dependency graphs
			/* args[0] contains the parsed sentences of a single document; we can extend the example to take in more than one document in
			 * order to build a model of the entire corpus. */
			// Creating a new empty model
			if (args.length < 2)
			{	
				System.out.println("Usage: sem.examples.RaspTest <parsed corpus file> <parsed input fragments>");
				System.exit(0);
			}
			File f = new File(args[0]);
			if (!f.exists())	
			{
				System.out.println("The file \"" + args[0] + "\" doesn't exist.");
				System.exit(0);
			}	
			
			SemModel semModel = new SemModel(false);
		
			// @Khatri Creating a new empty auxiliary model.
			SemModelAux semModelAux = new SemModelAux();
		
			GraphReader reader = new RaspGraphReader(args[0], false, semModelAux);
			// @Khatri

			// initializing some graph editors. They can be used to clean up the graphs, but are not required.
			ArrayList<GraphEditor> graphEditors = new ArrayList<GraphEditor>(Arrays.asList(new LowerCaseGraphEditor(), new NumTagsGraphEditor()));
			// Adding all the graphs to the model
			while(reader.hasNext()){
				Graph graph = reader.next();
				for(GraphEditor graphEditor : graphEditors)
					graphEditor.edit(graph);
				semModel.add(graph);
			}
			reader.close();
			// @Khatri
			System.out.println("Begin building semModelAux stats.");
			semModelAux.buildModelStats(semModel.getNodeIndex());
			System.out.println("Done building semModelAux stats.");
			// @Khatri
			
			/* Now, we move on to find the extrinsic similarity between the two text fragments. */
			FragmentProcessor fragmentProcessor = new FragmentProcessor(semModelAux);

			// We construct a new vector space, using the PMI weighting scheme. The PMI_LIM scheme discards features that occur only once. 
                        VectorSpace vectorSpace = new VectorSpace(semModel, VectorSpace.WEIGHT_PMI_LIM, true); 

			Fragment fragment = new Fragment(vectorSpace.getFeatureIndex(), semModel.getNodeIndex());
			/* Build the feature count stats for the feature weighting schemes. */
			fragment.buildStats(semModelAux.getSentenceGraphsMap());

			System.out.println("Begin finding matching sentences.");
			List<List <Integer> > sentenceMatchesList = fragmentProcessor.getSentenceMatches(args[1], semModel.getNodeIndex());
			System.out.println("Done finding matching sentences.");

			/* Now that we have the list of probable sentence Matches, we need to find the exact sentence matches using the 'getSentenceMatches'
			 * function of the fragmentProcessor class. */		
			
			
		} catch (GraphFormatException e) {
			e.printLine();
			e.printStackTrace();
		} catch (SemModelException e) {
			e.printStackTrace();
		}
	}
}
